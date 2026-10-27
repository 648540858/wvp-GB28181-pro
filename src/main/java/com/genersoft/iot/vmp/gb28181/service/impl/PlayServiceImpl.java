package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.*;
import com.genersoft.iot.vmp.common.enums.MediaStreamUtil;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.exception.ServiceException;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.controller.bean.AudioBroadcastEvent;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.service.*;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.gb28181.session.SendSsrcFactory;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.bean.RecordInfo;
import com.genersoft.iot.vmp.media.event.hook.Hook;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.hook.HookType;
import com.genersoft.iot.vmp.media.event.media.MediaArrivalEvent;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.media.event.media.MediaNotFoundEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.service.ICloudRecordService;
import com.genersoft.iot.vmp.service.IReceiveRtpServerService;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.service.bean.*;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcPlayService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.AudioBroadcastResult;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import javax.sdp.*;
import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Response;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.Vector;

@SuppressWarnings(value = {"rawtypes", "unchecked"})
@Slf4j
@Service("playService")
public class PlayServiceImpl implements IPlayService {

    // RPC 会将同一设备路由到所属 WVP；固定条带锁只串行同名回放或下载的本机创建、停止与回调。
    private final Object[] playbackLifecycleLocks = java.util.stream.IntStream.range(0, 64)
            .mapToObj(index -> new Object()).toArray();
    private final java.util.concurrent.ConcurrentMap<String, Object> playbackAttempts =
            new java.util.concurrent.ConcurrentHashMap<>();
    // 下载完成前保留本次身份；固定重启时间槽避免同秒 MP4 事件被挂到下一轮下载。
    private final java.util.concurrent.ConcurrentMap<String, Object> downloadAttempts = new java.util.concurrent.ConcurrentHashMap<>();
    private final long[] downloadRestartAfter = new long[64];

    /**
     * 录像下载会话在流离开后的保留窗口（毫秒）。
     *
     * <p>ZLM 停止推流时会先发 on_stream_changed(regist=false)，再回调 on_record_mp4，
     * 而下载地址只在 on_record_mp4 钩子里写入 InviteInfo。因此流离开时不能立即删除下载会话，
     * 需要保留到钩子执行完成；这里给一个足够宽的窗口，到期由 InviteStreamServiceImpl 的定时清理回收。</p>
     */
    private static final long DOWNLOAD_INVITE_KEEP_MS = 15 * 60 * 1000L;

    @Autowired
    private ISIPCommander cmder;

    @Autowired
    private AudioBroadcastManager audioBroadcastManager;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private ISIPCommanderForPlatform sipCommanderFroPlatform;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IInviteStreamService inviteStreamService;

    @Autowired
    private HookSubscribe subscribe;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private SipInviteSessionManager sessionManager;

    @Autowired
    private IDialogTerminationService dialogTerminationService;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private ISIPCommanderForPlatform commanderForPlatform;

    @Autowired
    private SSRCFactory ssrcFactory;

    @Autowired
    private SendSsrcFactory sendSsrcFactory;

    @Autowired
    private IPlatformService platformService;

    @Autowired
    private IGbChannelService channelService;

    @Autowired
    private ISendRtpServerService sendRtpServerService;

    @Autowired
    private IReceiveRtpServerService receiveRtpServerService;

    @Autowired
    private ICloudRecordService cloudRecordService;

    @Autowired
    private IRedisRpcPlayService redisRpcPlayService;

    /**
     * 流到来的处理
     */
    @Async
    @EventListener
    public void onApplicationEvent(MediaArrivalEvent event) {
        if (MediaStreamUtil.GB28181_BROADCAST.equals(event.getApp()) || MediaStreamUtil.GB28181_TALK.equals(event.getApp())) {
            if (event.getStream().indexOf("_") > 0) {
                String[] streamArray = event.getStream().split("_");
                if (streamArray.length == 2) {
                    String deviceId = streamArray[0];
                    String channelId = streamArray[1];
                    Device device = deviceService.getDeviceByDeviceId(deviceId);
                    DeviceChannel channel = deviceChannelService.getOneForSource(deviceId, channelId);
                    if (device == null) {
                        log.info("[语音对讲/喊话] 未找到设备：{}", deviceId);
                        return;
                    }
                    if (channel == null) {
                        log.info("[语音对讲/喊话] 未找到通道：{}", channelId);
                        return;
                    }
                    if (MediaStreamUtil.GB28181_BROADCAST.equals(event.getApp())) {
                        if (audioBroadcastManager.exit(channel.getId())) {
                            stopAudioBroadcast(device, channel);
                        }
                        // 开启语音对讲通道
                        try {
                            audioBroadcastCmd(device, channel, event.getMediaServer(),
                                    event.getApp(), event.getStream(), 60, false, (msg) -> log.info("[语音喊话] 通道建立成功, device: {}, channel: {}", deviceId, channelId));
                        } catch (InvalidArgumentException | ParseException | SipException e) {
                            log.error("[命令发送失败] 语音对讲: {}", e.getMessage());
                        }
                    }else if (MediaStreamUtil.GB28181_TALK.equals(event.getApp())) {
                        // 开启语音对讲通道
                        talkCmd(device, channel, event.getMediaServer(), event.getStream(), (msg) -> log.info("[语音对讲] 通道建立成功, device: {}, channel: {}", deviceId, channelId));
                    }
                }
            }
        }


    }

    /**
     * 流离开的处理
     */
    @Async
    @EventListener
    public void onApplicationEvent(MediaDepartureEvent event) {
        List<SendRtpInfo> sendRtpInfos = sendRtpServerService.queryByStream(event.getStream());
        if (!sendRtpInfos.isEmpty()) {
            for (SendRtpInfo sendRtpInfo : sendRtpInfos) {
                if (sendRtpInfo != null && sendRtpInfo.isSendToPlatform() && sendRtpInfo.getApp().equals(event.getApp())) {
                    String platformId = sendRtpInfo.getTargetId();
                    Device device = deviceService.getDeviceByDeviceId(platformId);
                    DeviceChannel channel = deviceChannelService.getOneById(sendRtpInfo.getChannelId());
                    try {
                        if (device != null && channel != null) {
                            cmder.streamByeCmd(device, channel.getDeviceId(), event.getApp(), event.getStream(), sendRtpInfo.getCallId(), null);
                            if (sendRtpInfo.getPlayType().equals(InviteStreamType.BROADCAST)
                                    || sendRtpInfo.getPlayType().equals(InviteStreamType.TALK)) {
                                AudioBroadcastCatch audioBroadcastCatch = audioBroadcastManager.get(channel.getId());
                                if (audioBroadcastCatch != null) {
                                    // 来自上级平台的停止对讲
                                    log.info("[停止对讲] 来自上级，平台：{}, 通道：{}", sendRtpInfo.getTargetId(), sendRtpInfo.getChannelId());
                                    audioBroadcastManager.del(sendRtpInfo.getChannelId());
                                }
                            }
                        }
                    } catch (SipException | InvalidArgumentException | ParseException |
                             SsrcTransactionNotFoundException e) {
                        log.error("[命令发送失败] 发送BYE: {}", e.getMessage());
                    }
                }
            }
        }

        if (MediaStreamUtil.GB28181_BROADCAST.equals(event.getApp()) || MediaStreamUtil.GB28181_TALK.equals(event.getApp())) {
            if (event.getStream().indexOf("_") > 0) {
                String[] streamArray = event.getStream().split("_");
                if (streamArray.length == 2) {
                    String deviceId = streamArray[0];
                    String channelId = streamArray[1];
                    Device device = deviceService.getDeviceByDeviceId(deviceId);
                    if (device == null) {
                        log.info("[语音对讲/喊话] 未找到设备：{}", deviceId);
                        return;
                    }
                    DeviceChannel channel = deviceChannelService.getOneForSource(deviceId, channelId);
                    if (channel == null) {
                        log.info("[语音对讲/喊话] 未找到通道：{}", channelId);
                        return;
                    }
                    if (MediaStreamUtil.GB28181_BROADCAST.equals(event.getApp())) {
                        stopAudioBroadcast(device, channel);
                    }else if (MediaStreamUtil.GB28181_TALK.equals(event.getApp())) {
                        stopTalk(device, channel, false);
                    }
                }
            }
        }else if (MediaStreamUtil.isGB28181(event.getApp(), event.getStream())) {
            synchronized (playbackLifecycleLock(event.getStream())) {
            InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStreamPreferPlay(event.getStream());
            if (inviteInfo != null) {
                if (inviteStreamService.isStaleStreamDeparture(inviteInfo, event.getCreateStamp())) {
                    log.info("[流离开] 流实例创建时间早于当前会话，判定为旧会话的迟到注销事件，跳过BYE: stream={}, deviceId={}, channelId={}",
                            event.getStream(), inviteInfo.getDeviceId(), inviteInfo.getChannelId());
                    return;
                }
                if (inviteInfo.getType() == InviteSessionType.PLAYBACK) {
                    // 注销事件没有精确的请求身份；正在注册或已有同名媒体时，不允许旧事件停止新回放。
                    if (inviteInfo.getStreamInfo() == null) {
                        return;
                    }
                    List<StreamInfo> currentMedia = mediaServerService.getMediaList(
                            inviteInfo.getStreamInfo().getMediaServer(), event.getApp(), event.getStream(), null);
                    if (currentMedia == null || !currentMedia.isEmpty()) {
                        return;
                    }
                    playbackAttempts.remove(event.getStream());
                }
                if (inviteInfo.getType() == InviteSessionType.DOWNLOAD) {
                    // 旧注销也可能晚于停止重下；只回收已确认没有媒体的本轮下载。
                    if (inviteInfo.getStreamInfo() == null) {
                        return;
                    }
                    List<StreamInfo> currentMedia = mediaServerService.getMediaList(
                            inviteInfo.getStreamInfo().getMediaServer(), event.getApp(), event.getStream(), null);
                    if (currentMedia == null || !currentMedia.isEmpty()) {
                        return;
                    }
                }
                Device device = deviceService.getDeviceByDeviceId(inviteInfo.getDeviceId());
                DeviceChannel channel = deviceChannelService.getOneForSourceById(inviteInfo.getChannelId());
                if (device != null && channel != null) {
                    dialogTerminationService.stopDeviceByStream(device, channel,
                            event.getApp(), event.getStream());
                }
                if (isRetainedDownloadSession(inviteInfo)) {
                    // 录像下载：保留会话信息，等待 on_record_mp4 钩子写入下载地址。
                    // 提前删除会导致钩子取不到 InviteInfo，前端查询下载进度时拿不到 downLoadFilePath。
                    inviteStreamService.setInviteInfoCleanTime(inviteInfo, DOWNLOAD_INVITE_KEEP_MS);
                    log.info("[流离开] 录像下载会话保留 {} 分钟，等待写入下载地址: stream={}, deviceId={}, channelId={}",
                            DOWNLOAD_INVITE_KEEP_MS / (60 * 1000), event.getStream(),
                            inviteInfo.getDeviceId(), inviteInfo.getChannelId());
                } else {
                    inviteStreamService.removeInviteInfo(inviteInfo);
                }
                if (inviteInfo.getType() == InviteSessionType.PLAY) {
                    deviceChannelService.stopPlay(inviteInfo.getChannelId());
                }
                if (inviteInfo.getStreamInfo() != null) {
                    receiveRtpServerService.closeRTPServer(inviteInfo.getStreamInfo().getMediaServer(),
                            MediaStreamUtil.RTP_APP, event.getStream());
                }
            } else {
                if (playbackAttempts.containsKey(event.getStream())) {
                    return;
                }
                // InviteInfo 可能已被清理，仍按流名解析设备/通道，清理残留事务
                String[] streamArray = event.getStream().split("_");
                if (streamArray.length >= 2) {
                    Device device = deviceService.getDeviceByDeviceId(streamArray[0]);
                    DeviceChannel channel = deviceChannelService.getOneForSource(streamArray[0], streamArray[1]);
                    if (device != null && channel != null) {
                        dialogTerminationService.stopDeviceByStream(device, channel,
                                event.getApp(), event.getStream());
                    }
                }
            }
            }
        }
    }

    /**
     * 是否为「流已建立、需要保留会话信息」的录像下载会话。
     *
     * <p>只有已经发布过流（streamInfo 不为空）的下载才有下载地址需要在随后的 on_record_mp4 钩子里写入；
     * 未建立流就失败的下载没有地址可写，维持原有的立即清理。</p>
     */
    private boolean isRetainedDownloadSession(InviteInfo inviteInfo) {
        return inviteInfo.getType() == InviteSessionType.DOWNLOAD && inviteInfo.getStreamInfo() != null;
    }

    /**
     * 流未找到的处理
     */
    @Async
    @EventListener
    public void onApplicationEvent(MediaNotFoundEvent event) {
        if (!MediaStreamUtil.isGB28181(event.getApp(), event.getStream())) {
            return;
        }
        String[] s = event.getStream().split("_");
        if ((s.length != 2 && s.length != 4)) {
            return;
        }
        String deviceId = s[0];
        String channelId = s[1];
        Device device = redisCatchStorage.getDevice(deviceId);
        if (device == null || !device.isOnLine()) {
            return;
        }
        DeviceChannel deviceChannel = deviceChannelService.getOne(deviceId, channelId);
        if (deviceChannel == null) {
            return;
        }
        if (s.length == 2) {
            log.info("[ZLM HOOK] 预览流未找到, 发起自动点播：{}->{}->{}/{}", event.getMediaServer().getId(), event.getSchema(), event.getApp(), event.getStream());
            play(event.getMediaServer(), deviceId, channelId, null, (code, msg, data) -> {});
        } else if (s.length == 4) {
            // 此时为录像回放， 录像回放格式为> 设备ID_通道ID_开始时间_结束时间
            String startTimeStr = s[2];
            String endTimeStr = s[3];
            if (startTimeStr == null || endTimeStr == null || startTimeStr.length() != 14 || endTimeStr.length() != 14) {
                return;
            }
            String startTime = DateUtil.urlToyyyy_MM_dd_HH_mm_ss(startTimeStr);
            String endTime = DateUtil.urlToyyyy_MM_dd_HH_mm_ss(endTimeStr);
            log.info("[ZLM HOOK] 回放流未找到, 发起自动点播：{}->{}->{}/{}-{}-{}",
                    event.getMediaServer().getId(), event.getSchema(),
                    event.getApp(), event.getStream(),
                    startTime, endTime
            );

            playBack(event.getMediaServer(), device, deviceChannel, startTime, endTime, (code, msg, data) -> {});
        }
    }

    @Override
    public void play(Device device, DeviceChannel channel, ErrorCallback<StreamInfo> callback) {

        // 判断设备是否属于当前平台, 如果不属于则发起自动调用
        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcPlayService.play(device.getServerId(), channel.getId(), callback);
            return;
        }
        MediaServer mediaServerItem = getNewMediaServerItem(device);
        if (mediaServerItem == null) {
            log.warn("[点播] 未找到可用的zlm deviceId: {},channelId:{}", device.getDeviceId(), channel.getDeviceId());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到可用的zlm");
        }
        play(mediaServerItem, device, channel, null, userSetting.getRecordSip(), callback);
    }

    @Override
    public SSRCInfo play(MediaServer mediaServerItem, String deviceId, String channelId, String ssrc, ErrorCallback<StreamInfo> callback) {
        if (mediaServerItem == null) {
            log.warn("[点播] 未找到可用的zlm deviceId: {},channelId:{}", deviceId, channelId);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到可用的zlm");
        }
        Device device = redisCatchStorage.getDevice(deviceId);
        if (device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE") && !mediaServerItem.isRtpEnable()) {
            log.warn("[点播] 单端口收流时不支持TCP主动方式收流 deviceId: {},channelId:{}", deviceId, channelId);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "单端口收流时不支持TCP主动方式收流");
        }
        DeviceChannel channel = deviceChannelService.getOneForSource(deviceId, channelId);
        if (channel == null) {
            log.warn("[点播] 未找到通道 deviceId: {},channelId:{}", deviceId, channelId);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到通道");
        }

        return play(mediaServerItem, device, channel, ssrc, userSetting.getRecordSip(), callback);
    }

    private SSRCInfo play(MediaServer mediaServer, Device device, DeviceChannel channel, String ssrc, Boolean record,
                          ErrorCallback<StreamInfo> callback) {
        if (mediaServer == null ) {
            if (callback != null) {
                callback.run(InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getCode(),
                        InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getMsg(),
                        null);
            }
            return null;
        }

        InviteInfo inviteInfoInCatch = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
        if (inviteInfoInCatch != null && inviteInfoInCatch.getStreamInfo() != null) {
            StreamInfo streamInfo = inviteInfoInCatch.getStreamInfo();
            String streamId = streamInfo.getStream();
            if (streamId != null) {
                MediaServer mediaInfo = streamInfo.getMediaServer();
                Boolean ready = mediaServerService.isStreamReady(mediaInfo, MediaStreamUtil.RTP_APP, streamId);
                if (ready != null && ready) {
                    SsrcTransaction currentTransaction = sessionManager.getSsrcTransactionByStream(
                            MediaStreamUtil.RTP_APP, streamId);
                    if (currentTransaction != null && currentTransaction.getSipTransactionInfo() != null) {
                        if(callback != null) {
                            callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
                        }
                        inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
                                InviteErrorCode.SUCCESS.getCode(),
                                InviteErrorCode.SUCCESS.getMsg(),
                                streamInfo);
                        log.info("[点播已存在] 直接返回， 设备编号: {}, 通道编号: {}", device.getDeviceId(), channel.getDeviceId());
                        return inviteInfoInCatch.getSsrcInfo();
                    }
                    log.warn("[点播] 流已存在但缺少 SIP 事务，清理后重新点播， 设备编号: {}, 通道编号: {}, 流：{}",
                            device.getDeviceId(), channel.getDeviceId(), streamId);
                } else {
                    // 流不存在，清理异常状态后重新点播
                    log.warn("[点播] 已存在点播信息但流不存在，重新点播， 设备编号: {}, 通道编号: {}", device.getDeviceId(), channel.getDeviceId());
                }
                receiveRtpServerService.closeRTPServer(mediaInfo, MediaStreamUtil.RTP_APP, streamId);
                deviceChannelService.stopPlay(channel.getId());
                inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
            }else {
                // 缓存中流ID为空，清理异常数据后重新点播
                log.warn("[点播] 缓存中streamId为null，清理异常数据后重新点播， 设备编号: {}, 通道编号: {}", device.getDeviceId(), channel.getDeviceId());
                inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
            }
        }

        // 原子认领点播发起权，并发请求只有一个能发起invite，其余等待结果
        boolean first = inviteStreamService.onceAndFirst(InviteSessionType.PLAY, channel.getId(), null, callback);
        if (!first) {
            log.info("[点播开始] 已有请求在途，等待结果， deviceId: {}, channelId({}): {}", device.getDeviceId(), channel.getDeviceId(), channel.getId());
            InviteInfo inviteInfoWaiting = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
            return inviteInfoWaiting != null ? inviteInfoWaiting.getSsrcInfo() : null;
        }
        // 认领成功后二次确认，避免发起者刚完成(已写入redis)时本线程重复发起invite
        InviteInfo inviteInfoForConfirm = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
        if (inviteInfoForConfirm != null && inviteInfoForConfirm.getStreamInfo() != null
                && inviteInfoForConfirm.getStreamInfo().getStream() != null
                && Boolean.TRUE.equals(mediaServerService.isStreamReady(inviteInfoForConfirm.getStreamInfo().getMediaServer(),
                        MediaStreamUtil.RTP_APP, inviteInfoForConfirm.getStreamInfo().getStream()))) {
            SsrcTransaction currentTransaction = sessionManager.getSsrcTransactionByStream(
                    MediaStreamUtil.RTP_APP, inviteInfoForConfirm.getStreamInfo().getStream());
            if (currentTransaction != null && currentTransaction.getSipTransactionInfo() != null) {
                inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
                        InviteErrorCode.SUCCESS.getCode(),
                        InviteErrorCode.SUCCESS.getMsg(),
                        inviteInfoForConfirm.getStreamInfo());
                return inviteInfoForConfirm.getSsrcInfo();
            }
        }

        String streamId = String.format("%s_%s", device.getDeviceId(), channel.getDeviceId());

        try {
            SSRCInfo ssrcInfo = receiveRtpServerService.openGbRTPServerForPlay(mediaServer, device, channel, ssrc,
                    record != null ? record : userSetting.getRecordSip(),
                    (code, msg, result) -> {
                try {
                    if (code == InviteErrorCode.SUCCESS.getCode() && result != null && result.getHookData() != null) {
                        // hook 响应
                        StreamInfo streamInfo = onPublishHandlerForPlay(result.getHookData().getMediaServer(), result.getHookData().getMediaInfo(), device, channel);
                        if (streamInfo == null){
                            inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
                                    InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getCode(),
                                    InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getMsg(), null);
                            inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
                            return;
                        }
                        inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
                                InviteErrorCode.SUCCESS.getCode(),
                                InviteErrorCode.SUCCESS.getMsg(),
                                streamInfo);

                        log.info("[点播成功] 设备编号: {}, 通道编号:{}, 码流类型：{}", device.getDeviceId(), channel.getDeviceId(),
                                channel.getStreamIdentification());
                        snapOnPlay(result.getHookData().getMediaServer(), device.getDeviceId(), channel.getDeviceId(), streamId);
                    }else {
                        inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null, code, msg, null);
                        inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
                        SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByStream(MediaStreamUtil.RTP_APP, streamId);
                        if (ssrcTransaction != null) {
                            try {
                                cmder.streamByeCmd(device, channel.getDeviceId(), MediaStreamUtil.RTP_APP, streamId, null, null);
                            } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
                                log.error("[点播超时]， 发送BYE失败 {}", e.getMessage());
                            }
                        }
                    }
                }catch (Exception e) {
                    log.error("[点播] 回调处理异常，释放点播请求， deviceId: {}, channelId: {}", device.getDeviceId(), channel.getDeviceId(), e);
                    inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
                            InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getCode(),
                            InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getMsg(), null);
                    inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
                }
            });
            if (ssrcInfo == null || ssrcInfo.getPort() <= 0) {
                log.info("[点播端口/SSRC]获取失败，设备编号：{}, 通道编号：{},ssrcInfo；{}", device.getDeviceId(), channel.getDeviceId(), ssrcInfo);
                inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
                        InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(),
                        InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getMsg(),
                        null);
                return null;
            }
            String sdpIp = !ObjectUtils.isEmpty(device.getSdpIp()) ? device.getSdpIp() : mediaServer.getSdpIp();
            log.info("[点播开始] 设备编号: {}, 通道编号: {}, 收流地址： {}:{}, 流ID：{}, 收流模式：{}, SSRC: {}, SSRC校验：{}",
                    device.getDeviceId(), channel.getDeviceId(), sdpIp, ssrcInfo.getPort(), ssrcInfo.getStream(), device.getStreamMode(),
                    ssrcInfo.getSsrc(), device.isSsrcCheck());

            // 初始化redis中的invite消息状态
            InviteInfo inviteInfo = InviteInfo.getInviteInfo(device.getDeviceId(), channel.getId(), ssrcInfo.getStream(), ssrcInfo, mediaServer.getId(),
                    mediaServer.getSdpIp(), ssrcInfo.getPort(), device.getStreamMode(), InviteSessionType.PLAY,
                    InviteSessionStatus.ready);
            inviteInfo.setCreateTime(System.currentTimeMillis());
            inviteStreamService.updateInviteInfo(inviteInfo);

            try {
                cmder.playStreamCmd(mediaServer, ssrcInfo, device, channel, (eventResult) -> {
                    try {
                        // 处理收到200ok后的TCP主动连接以及SSRC不一致的问题
                        InviteOKHandler(eventResult, ssrcInfo, mediaServer, device, channel, callback, inviteInfo, InviteSessionType.PLAY);
                    }catch (Exception e) {
                        log.error("[点播] 200OK处理异常，释放点播请求， deviceId: {}, channelId: {}", device.getDeviceId(), channel.getDeviceId(), e);
                        inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
                                InviteErrorCode.FAIL.getCode(), "点播200OK处理异常", null);
                    }
                }, (event) -> {
                    log.info("[点播失败]{}:{} deviceId: {}, channelId:{}",event.statusCode, event.msg, device.getDeviceId(), channel.getDeviceId());
                    receiveRtpServerService.closeRTPServer(mediaServer, ssrcInfo.getApp(), ssrcInfo.getStream());

                    sessionManager.removeByStreamIfSsrc(ssrcInfo.getApp(), ssrcInfo.getStream(), ssrcInfo.getSsrc());
                    inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
                            event.statusCode, event.msg, null);

                    inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
                }, userSetting.getPlayTimeout().longValue());
            } catch (InvalidArgumentException | SipException | ParseException e) {
                log.error("[命令发送失败] 点播消息: {}", e.getMessage());
                receiveRtpServerService.closeRTPServer(mediaServer, ssrcInfo.getApp(), ssrcInfo.getStream());
                inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
                        InviteErrorCode.ERROR_FOR_SIP_SENDING_FAILED.getCode(),
                        InviteErrorCode.ERROR_FOR_SIP_SENDING_FAILED.getMsg(), null);

                inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
            }
            return ssrcInfo;
        } catch (Throwable e) {
            log.error("[点播] 未知异常，释放点播请求， deviceId: {}, channelId: {}", device.getDeviceId(), channel.getDeviceId(), e);
            inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
                    InviteErrorCode.ERROR_FOR_SIP_SENDING_FAILED.getCode(),
                    InviteErrorCode.ERROR_FOR_SIP_SENDING_FAILED.getMsg(), null);
            return null;
        }
    }


    private void talk(MediaServer mediaServerItem, Device device, DeviceChannel channel, String stream,
                      SipSubscribe.Event errorEvent, Runnable timeoutCallback, AudioBroadcastEvent audioEvent) {

        String ySsrc = ssrcFactory.getPlaySsrc(mediaServerItem);

        if (ySsrc == null) {
            audioEvent.call("ssrc已经用尽");
            return;
        }
        String sendSsrc = sendSsrcFactory.getSendSsrc("0");
        SendRtpInfo sendRtpInfo;
        try {
            sendRtpInfo = sendRtpServerService.createSendRtpInfo(mediaServerItem, null, null, sendSsrc, device.getDeviceId(), MediaStreamUtil.GB28181_TALK, stream,
                    channel.getId(), true, false);
            if (sendRtpInfo == null) {
                audioEvent.call("获取发流端口失败");
                return;
            }
            sendRtpInfo.setPlayType(InviteStreamType.TALK);
        }catch (PlayException e) {
            log.info("[语音对讲]开始 获取发流端口失败 deviceId: {}, channelId: {},", device.getDeviceId(), channel.getDeviceId());
            return;
        }

        sendRtpInfo.setOnlyAudio(true);
        sendRtpInfo.setPt(8);
        sendRtpInfo.setStatus(1);
        sendRtpInfo.setTcpActive(false);
        sendRtpInfo.setUsePs(false);
        sendRtpInfo.setReceiveStream(stream + "_talk");

        String callId = SipUtils.getNewCallId();
        log.info("[语音对讲]开始 deviceId: {}, channelId: {},收流端口： {}, 收流模式：{}, SSRC: {}, SSRC校验：{}", device.getDeviceId(), channel.getDeviceId(), sendRtpInfo.getLocalPort(), device.getStreamMode(), sendRtpInfo.getSsrc(), false);
        // 超时处理
        String timeOutTaskKey = UUID.randomUUID().toString();
        dynamicTask.startDelay(timeOutTaskKey, () -> {

            log.info("[语音对讲] 收流超时 deviceId: {}, channelId: {}，端口：{}, SSRC: {}", device.getDeviceId(), channel.getDeviceId(), sendRtpInfo.getPort(), sendRtpInfo.getSsrc());
            timeoutCallback.run();
            // 点播超时回复BYE 同时释放ssrc以及此次点播的资源
            try {
                cmder.streamByeCmd(device, channel.getDeviceId(), null,  null, callId, null);
            } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
                log.error("[语音对讲]超时， 发送BYE失败 {}", e.getMessage());
            } finally {
                timeoutCallback.run();
            }
        }, userSetting.getPlayTimeout());

        try {
            Integer localPort = mediaServerService.startSendRtpPassive(mediaServerItem, sendRtpInfo, userSetting.getPlayTimeout() * 1000);
            if (localPort == null || localPort <= 0) {
                timeoutCallback.run();
                sessionManager.removeByStreamIfSsrc(sendRtpInfo.getApp(), sendRtpInfo.getStream(),
                        sendRtpInfo.getSsrc());
                return;
            }
            sendRtpInfo.setPort(localPort);
            // 增加鉴权信息
            receiveRtpServerService.addAuthenticateInfoForGb28181Talk(mediaServerItem, sendRtpInfo.getStream());

        }catch (ControllerException e) {
            log.info("[语音对讲]失败 deviceId: {}, channelId: {}", device.getDeviceId(), channel.getDeviceId());
            audioEvent.call("失败, " + e.getMessage());
            // 查看是否已经建立了通道，存在则发送bye
            stopTalk(device, channel);
        }


        // 查看设备是否已经在推流
        try {
            cmder.talkStreamCmd(mediaServerItem, sendRtpInfo, ySsrc, device, channel, callId, (hookData) -> {
                log.info("[语音对讲] 流已生成， 开始推流： " + hookData);
                dynamicTask.stop(timeOutTaskKey);
                // TODO 暂不做处理
            }, (hookData) -> {
                log.info("[语音对讲] 设备开始推流： " + hookData);
                dynamicTask.stop(timeOutTaskKey);

            }, (event) -> {
                dynamicTask.stop(timeOutTaskKey);

                if (event.event instanceof ResponseEvent) {
                    ResponseEvent responseEvent = (ResponseEvent) event.event;
                    if (responseEvent.getResponse() instanceof SIPResponse) {
                        SIPResponse response = (SIPResponse) responseEvent.getResponse();
                        sendRtpInfo.setFromTag(response.getFromTag());
                        sendRtpInfo.setToTag(response.getToTag());
                        sendRtpInfo.setCallId(response.getCallIdHeader().getCallId());
                        sendRtpServerService.update(sendRtpInfo);

                        // SIP 事务由 SIPCommander.talkStreamCmd 统一注册，这里不再重复写入。
                    } else {
                        log.error("[语音对讲]收到的消息错误，response不是SIPResponse");
                    }
                } else {
                    log.error("[语音对讲]收到的消息错误，event不是ResponseEvent");
                }

            }, (event) -> {
                dynamicTask.stop(timeOutTaskKey);
                receiveRtpServerService.closeRTPServer(mediaServerItem, sendRtpInfo.getApp(), sendRtpInfo.getStream());
                sessionManager.removeByStreamIfSsrc(sendRtpInfo.getApp(), sendRtpInfo.getStream(),
                        sendRtpInfo.getSsrc());
                errorEvent.response(event);
            }, userSetting.getPlayTimeout().longValue());
        } catch (InvalidArgumentException | SipException | ParseException e) {

            log.error("[命令发送失败] 对讲消息: {}", e.getMessage());
            dynamicTask.stop(timeOutTaskKey);
            receiveRtpServerService.closeRTPServer(mediaServerItem, sendRtpInfo.getApp(), sendRtpInfo.getStream());
            sessionManager.removeByStreamIfSsrc(sendRtpInfo.getApp(), sendRtpInfo.getStream(),
                    sendRtpInfo.getSsrc());
            SipSubscribe.EventResult eventResult = new SipSubscribe.EventResult();
            eventResult.type = SipSubscribe.EventResultType.cmdSendFailEvent;
            eventResult.statusCode = -1;
            eventResult.msg = "命令发送失败";
            errorEvent.response(eventResult);
        }
//        }

    }

    private void tcpActiveHandler(Device device, DeviceChannel channel, String contentString,
                                  MediaServer mediaServerItem, SSRCInfo ssrcInfo, ErrorCallback<StreamInfo> callback,
                                  InviteSessionType inviteSessionType, InviteInfo inviteInfo){
        if (!device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE")) {
            return;
        }

        String substring;
        if (contentString.indexOf("y=") > 0) {
            substring = contentString.substring(0, contentString.indexOf("y="));
        }else {
            substring = contentString;
        }
        try {
            SessionDescription sdp = SdpFactory.getInstance().createSessionDescription(substring);
            int port = -1;
            Vector mediaDescriptions = sdp.getMediaDescriptions(true);
            for (Object description : mediaDescriptions) {
                MediaDescription mediaDescription = (MediaDescription) description;
                Media media = mediaDescription.getMedia();

                Vector mediaFormats = media.getMediaFormats(false);
                if (mediaFormats.contains("96")) {
                    port = media.getMediaPort();
                    break;
                }
            }
            log.info("[TCP主动连接对方] deviceId: {}, channelId: {}, 连接对方的地址：{}:{}, 收流模式：{}, SSRC: {}, SSRC校验：{}", device.getDeviceId(), channel.getDeviceId(), sdp.getConnection().getAddress(), port, device.getStreamMode(), ssrcInfo.getSsrc(), device.isSsrcCheck());
            Boolean result = mediaServerService.connectRtpServer(mediaServerItem, sdp.getConnection().getAddress(), port, ssrcInfo.getApp(), ssrcInfo.getStream());
            log.info("[TCP主动连接对方] 结果： {}" , result);
            if (!result) {
                // 主动连接失败，结束流程， 清理数据
                receiveRtpServerService.closeRTPServer(mediaServerItem, ssrcInfo.getApp(), ssrcInfo.getStream());
                sessionManager.removeByStreamIfSsrc(ssrcInfo.getApp(), ssrcInfo.getStream(), ssrcInfo.getSsrc());
                inviteStreamService.call(inviteSessionType, channel.getId(), getCallStream(inviteSessionType, inviteInfo),
                        InviteErrorCode.ERROR_FOR_TCP_ACTIVE_CONNECTION_REFUSED_ERROR.getCode(),
                        InviteErrorCode.ERROR_FOR_TCP_ACTIVE_CONNECTION_REFUSED_ERROR.getMsg(), null);
                inviteStreamService.removeInviteInfo(inviteInfo);
            }
        } catch (SdpException e) {
            log.error("[TCP主动连接对方] deviceId: {}, channelId: {}, 解析200OK的SDP信息失败", device.getDeviceId(), channel.getDeviceId(), e);
            receiveRtpServerService.closeRTPServer(mediaServerItem, ssrcInfo.getApp(), ssrcInfo.getStream());

            sessionManager.removeByStreamIfSsrc(ssrcInfo.getApp(), ssrcInfo.getStream(), ssrcInfo.getSsrc());

            inviteStreamService.call(inviteSessionType, channel.getId(), getCallStream(inviteSessionType, inviteInfo),
                    InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getCode(),
                    InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getMsg(), null);
            inviteStreamService.removeInviteInfo(inviteInfo);
        }
    }

    /**
     * 点播成功时调用截图.
     *
     * @param mediaServerItemInuse media
     * @param deviceId             设备 ID
     * @param channelId            通道 ID
     * @param stream               ssrc
     */
    private void snapOnPlay(MediaServer mediaServerItemInuse, String deviceId, String channelId, String stream) {
        String path = "snap";
        String fileName = deviceId + "_" + channelId + ".jpg";
        // 请求截图
        log.info("[请求截图]: " + fileName);
        mediaServerService.getSnap(mediaServerItemInuse, MediaStreamUtil.RTP_APP, stream, 15, 1, path, fileName);
    }

    public StreamInfo onPublishHandlerForPlay(MediaServer mediaServerItem, MediaInfo mediaInfo, Device device, DeviceChannel channel) {
        StreamInfo streamInfo = null;
        streamInfo = onPublishHandler(mediaServerItem, mediaInfo, device, channel);
        if (streamInfo != null) {
            deviceChannelService.startPlay(channel.getId(), streamInfo.getStream());
            InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
            if (inviteInfo != null) {
                inviteInfo.setStatus(InviteSessionStatus.ok);
                inviteInfo.setStreamInfo(streamInfo);
                inviteStreamService.updateInviteInfo(inviteInfo);
            }
        }
        return streamInfo;

    }

    private StreamInfo onPublishHandlerForPlayback(MediaServer mediaServerItem, MediaInfo mediaInfo, Device device,
                                                   DeviceChannel channel, String startTime, String endTime) {
        StreamInfo streamInfo = onPublishHandler(mediaServerItem, mediaInfo, device, channel);
        if (streamInfo != null) {
            streamInfo.setStartTime(startTime);
            streamInfo.setEndTime(endTime);
            InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(InviteSessionType.PLAYBACK, mediaInfo.getStream());
            if (inviteInfo != null) {
                inviteInfo.setStatus(InviteSessionStatus.ok);
                inviteInfo.setStreamInfo(streamInfo);
                inviteStreamService.updateInviteInfo(inviteInfo);
            }
        }
        return streamInfo;
    }

    /** 按设备绑定或媒体节点负载选择可用的收流节点。 */
    @Override
    public MediaServer getNewMediaServerItem(Device device) {
        if (device == null) {
            return null;
        }
        MediaServer mediaServerItem;
        if (ObjectUtils.isEmpty(device.getMediaServerId()) || "auto".equals(device.getMediaServerId())) {
            mediaServerItem = mediaServerService.getMediaServerForMinimumLoad(null);
        } else {
            mediaServerItem = mediaServerService.getOne(device.getMediaServerId());
        }
        if (mediaServerItem == null) {
            log.warn("点播时未找到可使用的ZLM...");
        }
        return mediaServerItem;
    }

    /** 根据设备归属选择本机收流或转发原有共享回放请求。 */
    @Override
    public void playBack(Device device, DeviceChannel channel, String startTime,
                         String endTime, ErrorCallback<StreamInfo> callback) {
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "设备不存在");
        }
        if (channel == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "通道不存在");
        }
        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcPlayService.playback(device.getServerId(), channel.getId(), startTime, endTime, callback);
            return;
        }

        MediaServer newMediaServerItem = getNewMediaServerItem(device);
        if (newMediaServerItem == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到可用的节点");
        }
        if (device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE") && ! newMediaServerItem.isRtpEnable()) {
            log.warn("[录像回放] 单端口收流时不支持TCP主动方式收流 deviceId: {},channelId:{}", device.getDeviceId(), channel.getDeviceId());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "单端口收流时不支持TCP主动方式收流");
        }

        playBack(newMediaServerItem, device, channel, startTime, endTime, callback);
    }

    /**
     * 合并同区间回放请求，并在复用缓存或重新发起 INVITE 前核对媒体节点的真实状态。
     */
    private void playBack(MediaServer mediaServer,
                         Device device, DeviceChannel channel, String startTime,
                         String endTime, ErrorCallback<StreamInfo> callback) {

        String stream = receiveRtpServerService.getPlaybackStream(device, channel, startTime, endTime);
        synchronized (playbackLifecycleLock(stream)) {
            startPlayback(mediaServer, device, channel, startTime, endTime, stream, callback);
        }
    }

    /** 在同名回放的临界区内完成认领和创建，回调也必须验证本轮身份后才能改变状态。 */
    private void startPlayback(MediaServer mediaServer, Device device, DeviceChannel channel,
                               String startTime, String endTime, String stream, ErrorCallback<StreamInfo> callback) {
        // 原子认领回放发起权，并发请求只有一个能发起invite，其余等待结果
        boolean first = inviteStreamService.onceAndFirst(InviteSessionType.PLAYBACK, channel.getId(), stream, callback);
        if (!first) {
            log.info("[录像回放] 已有请求在途，等待结果， deviceId: {}, channelId({}): {}, 开始时间: {}, 结束时间： {}",
                    device.getDeviceId(), channel.getDeviceId(), channel.getId(), startTime, endTime);
            return;
        }

        Object attempt = new Object();
        playbackAttempts.put(stream, attempt);
        try {
            InviteInfo previous = inviteStreamService.getInviteInfo(InviteSessionType.PLAYBACK, channel.getId(), stream);
            if (previous != null) {
                MediaServer previousMedia = previous.getStreamInfo() != null
                        ? previous.getStreamInfo().getMediaServer() : mediaServerService.getOne(previous.getMediaServerId());
                if (previousMedia == null) {
                    throw new IllegalStateException("回放缓存对应的媒体节点不存在");
                }
                List<StreamInfo> previousStreams = mediaServerService.getMediaList(previousMedia,
                        MediaStreamUtil.RTP_APP, stream, null);
                if (previousStreams == null) {
                    // 查询失败不能当成流已结束，否则会破坏仍在使用的共享回放。
                    throw new IllegalStateException("无法确认回放媒体状态");
                }
                if (previous.getStreamInfo() != null && InviteSessionStatus.ok.equals(previous.getStatus())
                        && !previousStreams.isEmpty()) {
                    playbackAttempts.remove(stream, attempt);
                    inviteStreamService.call(InviteSessionType.PLAYBACK, channel.getId(), stream,
                            InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), previous.getStreamInfo());
                    log.info("[录像回放] 媒体仍在线，复用会话，deviceId: {}, channelId: {}, stream: {}",
                            device.getDeviceId(), channel.getDeviceId(), stream);
                    return;
                }
                if (!previousStreams.isEmpty()) {
                    // 会话资料不完整时不能擅自关闭正在被其他客户端使用的媒体。
                    throw new IllegalStateException("回放会话状态与媒体状态不一致");
                }
                // 只清理已确认没有媒体的旧会话，由所属回放临界区保护，清理后才允许同名新会话启动。
                dialogTerminationService.stopDeviceByStream(device, channel, MediaStreamUtil.RTP_APP, stream);
                receiveRtpServerService.closeRTPServer(previousMedia, MediaStreamUtil.RTP_APP, stream);
                inviteStreamService.removeInviteInfo(previous);
            }
            if (!waitForPlaybackStreamClosed(mediaServer, stream)) {
                throw new IllegalStateException("旧回放媒体尚未释放");
            }
            ErrorCallback<OpenRTPServerResult> receiveCallback = (code, msg, result) -> {
                synchronized (playbackLifecycleLock(stream)) {
                    if (playbackAttempts.get(stream) != attempt) {
                        return;
                    }
                    if (code == InviteErrorCode.SUCCESS.getCode() && result != null && result.getHookData() != null) {
                        StreamInfo streamInfo = onPublishHandlerForPlayback(result.getHookData().getMediaServer(), result.getHookData().getMediaInfo(), device, channel, startTime, endTime);
                        if (streamInfo != null) {
                            inviteStreamService.call(InviteSessionType.PLAYBACK, channel.getId(), stream,
                                    InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
                            log.info("[录像回放] 成功 deviceId: {}, channelId: {}, 开始时间: {}, 结束时间: {}",
                                    device.getDeviceId(), channel.getDeviceId(), startTime, endTime);
                            return;
                        }
                        code = InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getCode();
                        msg = InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getMsg();
                    }
                    // 先终止旧资源，再释放等待者；回调可同步触发下一次播放，顺序不能倒置。
                    dialogTerminationService.stopDeviceByStream(device, channel, MediaStreamUtil.RTP_APP, stream);
                    receiveRtpServerService.closeRTPServer(mediaServer, MediaStreamUtil.RTP_APP, stream);
                    inviteStreamService.removeInviteInfo(InviteSessionType.PLAYBACK, channel.getId(), stream);
                    playbackAttempts.remove(stream, attempt);
                    inviteStreamService.call(InviteSessionType.PLAYBACK, channel.getId(), stream, code, msg, null);
                }
            };
            SSRCInfo ssrcInfo = receiveRtpServerService.openGbRTPServerForPlayback(
                    mediaServer, device, channel, startTime, endTime, receiveCallback);
            if (ssrcInfo == null || ssrcInfo.getPort() <= 0) {
                log.info("[回放端口/SSRC]获取失败，deviceId={},channelId={},ssrcInfo={}", device.getDeviceId(), channel.getDeviceId(), ssrcInfo);
                playbackAttempts.remove(stream, attempt);
                inviteStreamService.call(InviteSessionType.PLAYBACK, channel.getId(), stream,
                        InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(),
                        InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getMsg(),
                        null);
                return;
            }

            log.info("[录像回放] deviceId: {}, channelId: {}, 开始时间: {}, 结束时间： {}, 收流端口：{}, 收流模式：{}, SSRC: {}, SSRC校验：{}",
                    device.getDeviceId(), channel.getDeviceId(), startTime, endTime, ssrcInfo.getPort(), device.getStreamMode(),
                    ssrcInfo.getSsrc(), device.isSsrcCheck());
            // 初始化redis中的invite消息状态
            InviteInfo inviteInfo = InviteInfo.getInviteInfo(device.getDeviceId(), channel.getId(), ssrcInfo.getStream(), ssrcInfo, mediaServer.getId(),
                    mediaServer.getSdpIp(), ssrcInfo.getPort(), device.getStreamMode(), InviteSessionType.PLAYBACK,
                    InviteSessionStatus.ready);
            inviteInfo.setCreateTime(System.currentTimeMillis());
            inviteStreamService.updateInviteInfo(inviteInfo);

            try {
                cmder.playbackStreamCmd(mediaServer, ssrcInfo, device, channel, startTime, endTime,
                        eventResult -> {
                            synchronized (playbackLifecycleLock(stream)) {
                                if (playbackAttempts.get(stream) == attempt) {
                                    InviteOKHandler(eventResult, ssrcInfo, mediaServer, device, channel,
                                            callback, inviteInfo, InviteSessionType.PLAYBACK);
                                }
                            }
                        }, eventResult -> {
                            synchronized (playbackLifecycleLock(stream)) {
                                if (playbackAttempts.get(stream) != attempt) {
                                    return;
                                }
                                log.info("[录像回放] 失败，{} {}", eventResult.statusCode, eventResult.msg);
                                receiveRtpServerService.closeRTPServer(mediaServer, ssrcInfo.getApp(), ssrcInfo.getStream());
                                sessionManager.removeByStreamIfSsrc(ssrcInfo.getApp(), ssrcInfo.getStream(), ssrcInfo.getSsrc());
                                inviteStreamService.removeInviteInfo(inviteInfo);
                                playbackAttempts.remove(stream, attempt);
                                inviteStreamService.call(InviteSessionType.PLAYBACK, channel.getId(), stream,
                                        eventResult.statusCode, eventResult.msg, null);
                            }
                        }, userSetting.getPlayTimeout().longValue(), registration -> {
                            synchronized (playbackLifecycleLock(stream)) {
                                if (playbackAttempts.get(stream) != attempt) {
                                    return false;
                                }
                                // SIP 事务注册会替换同名索引，必须和上层成功回调共享同一个身份门禁。
                                registration.run();
                                return true;
                            }
                        });
            } catch (InvalidArgumentException | SipException | ParseException e) {
                log.error("[命令发送失败] 录像回放: {}", e.getMessage());
                receiveRtpServerService.closeRTPServer(mediaServer, ssrcInfo.getApp(), ssrcInfo.getStream());
                inviteStreamService.removeInviteInfo(inviteInfo);
                playbackAttempts.remove(stream, attempt);
                inviteStreamService.call(InviteSessionType.PLAYBACK, channel.getId(), stream,
                        InviteErrorCode.FAIL.getCode(), InviteErrorCode.FAIL.getMsg(), null);
            }
        } catch (Throwable e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            playbackAttempts.remove(stream, attempt);
            log.error("[录像回放] 未知异常，释放回放请求， deviceId: {}, channelId({}): {}", device.getDeviceId(), channel.getDeviceId(), channel.getId(), e);
            inviteStreamService.call(InviteSessionType.PLAYBACK, channel.getId(), stream,
                    InviteErrorCode.FAIL.getCode(), "录像回放异常", null);
        }
    }


    /** 返回同名回放共享的本机锁，避免无限增长的按流锁表或字符串池。 */
    private Object playbackLifecycleLock(String stream) {
        return playbackLifecycleLocks[Math.floorMod(stream.hashCode(), playbackLifecycleLocks.length)];
    }

    /**
     * 在有限时间内等待同名媒体释放，避免停止接口刚返回就再次 INVITE 撞上旧媒体。
     * 查询失败立即退出且不建流；仅观察本次录像流，不关闭没有会话归属的媒体。
     */
    private boolean waitForPlaybackStreamClosed(MediaServer mediaServer, String stream) throws InterruptedException {
        long waitMillis = Math.min(Math.max(userSetting.getPlayTimeout(), 0), 5000);
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(waitMillis);
        while (true) {
            List<StreamInfo> streams = mediaServerService.getMediaList(mediaServer, MediaStreamUtil.RTP_APP, stream, null);
            if (streams == null) {
                return false;
            }
            if (streams.isEmpty()) {
                return true;
            }
            if (System.nanoTime() >= deadline) {
                return false;
            }
            Thread.sleep(50);
        }
    }

    /** 释放已注册或仍在等待注册的会话接收器，避免取消后留下超时任务和同名媒体。 */
    private void closeInviteReceiver(InviteInfo inviteInfo) {
        if (inviteInfo.getType() == InviteSessionType.DOWNLOAD) {
            downloadAttempts.remove(inviteInfo.getStream());
            subscribe.removeSubscribe(Hook.getInstance(HookType.on_record_mp4, MediaStreamUtil.RTP_APP,
                    inviteInfo.getStream(), inviteInfo.getMediaServerId()));
            downloadRestartAfter[Math.floorMod(inviteInfo.getStream().hashCode(), downloadRestartAfter.length)] =
                    (System.currentTimeMillis() / 1000 + 1) * 1000;
        }
        if (inviteInfo.getType() == InviteSessionType.PLAYBACK) {
            playbackAttempts.remove(inviteInfo.getStream());
        }
        MediaServer mediaServer = inviteInfo.getStreamInfo() != null
                ? inviteInfo.getStreamInfo().getMediaServer() : mediaServerService.getOne(inviteInfo.getMediaServerId());
        if (mediaServer != null) {
            receiveRtpServerService.closeRTPServer(mediaServer, MediaStreamUtil.RTP_APP, inviteInfo.getStream());
        }
        if (inviteInfo.getType() == InviteSessionType.DOWNLOAD) {
            inviteStreamService.call(InviteSessionType.DOWNLOAD, inviteInfo.getChannelId(), inviteInfo.getStream(),
                    InviteErrorCode.ERROR_FOR_FINISH.getCode(), InviteErrorCode.ERROR_FOR_FINISH.getMsg(), null);
        }
        if (inviteInfo.getType() == InviteSessionType.PLAYBACK) {
            inviteStreamService.call(InviteSessionType.PLAYBACK, inviteInfo.getChannelId(), inviteInfo.getStream(),
                    InviteErrorCode.ERROR_FOR_FINISH.getCode(), InviteErrorCode.ERROR_FOR_FINISH.getMsg(), null);
        }
    }


    private String getCallStream(InviteSessionType inviteSessionType, InviteInfo inviteInfo) {
        // 点播(PLAY)认领时stream为null，回放/下载认领时stream为确定性的时间段key
        if (inviteSessionType == InviteSessionType.PLAY) {
            return null;
        }
        return inviteInfo.getStream();
    }

    private void InviteOKHandler(SipSubscribe.EventResult eventResult, SSRCInfo ssrcInfo, MediaServer mediaServerItem,
                                 Device device, DeviceChannel channel, ErrorCallback<StreamInfo> callback,
                                 InviteInfo inviteInfo, InviteSessionType inviteSessionType){
        inviteInfo.setStatus(InviteSessionStatus.ok);
        ResponseEvent responseEvent = (ResponseEvent) eventResult.event;
        String contentString = new String(responseEvent.getResponse().getRawContent());
        String ssrcInResponse = SipUtils.getSsrcFromSdp(contentString);
        // 兼容回复的消息中缺少ssrc(y字段)的情况
        if (ssrcInResponse == null) {
            ssrcInResponse = ssrcInfo.getSsrc();
        }
        if (ssrcInfo.getSsrc().equals(ssrcInResponse)) {
            // ssrc 一致
            if (mediaServerItem.isRtpEnable()) {
                // 多端口
                if (device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE")) {
                    tcpActiveHandler(device, channel, contentString, mediaServerItem, ssrcInfo, callback, inviteSessionType, inviteInfo);
                }
            }else {
                // 单端口
                if (device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE")) {
                    log.warn("[Invite 200OK] 单端口收流模式不支持tcp主动模式收流");
                }

            }
        }else {
            log.info("[Invite 200OK] 收到invite 200, 发现下级自定义了ssrc: {}", ssrcInResponse);
            String oldStreamId = String.format("%08x", Long.parseLong(ssrcInfo.getSsrc())).toUpperCase();
            String newStreamId = String.format("%08x", Long.parseLong(ssrcInResponse)).toUpperCase();
            if (!mediaServerItem.isRtpEnable()) { // 多端口时按照端口绑定了stream，即使stream与ssrc不一致，也不会影响
                receiveRtpServerService.refreshAuthenticateInfo(oldStreamId, newStreamId);
            }
            // ssrc 不一致
            if (mediaServerItem.isRtpEnable()) {
                // 多端口
                if (device.isSsrcCheck()) {
                    // ssrc检验
                    // 更新ssrc
                    log.info("[Invite 200OK] SSRC修正 {}->{}", ssrcInfo.getSsrc(), ssrcInResponse);
                    Boolean result = mediaServerService.updateRtpServerSSRC(mediaServerItem, ssrcInfo.getApp(), ssrcInfo.getStream(), ssrcInResponse);
                    if (!result) {
                        try {
                            log.warn("[Invite 200OK] 更新ssrc失败，停止点播 {}/{}", device.getDeviceId(), channel.getDeviceId());
                            cmder.streamByeCmd(device, channel.getDeviceId(), ssrcInfo.getApp(), ssrcInfo.getStream(), null, null);
                        } catch (InvalidArgumentException | SipException | ParseException | SsrcTransactionNotFoundException e) {
                            log.error("[命令发送失败] 停止播放， 发送BYE: {}", e.getMessage());
                        }

                        inviteStreamService.call(inviteSessionType, channel.getId(), getCallStream(inviteSessionType, inviteInfo),
                                InviteErrorCode.ERROR_FOR_RESET_SSRC.getCode(),
                                "下级自定义了ssrc,重新设置收流信息失败", null);
                        inviteStreamService.removeInviteInfo(inviteInfo);

                    }else {
                        ssrcInfo.setSsrc(ssrcInResponse);
                        inviteInfo.setSsrcInfo(ssrcInfo);
                        inviteInfo.setStream(ssrcInfo.getStream());
                        if (device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE")) {
                            if (mediaServerItem.isRtpEnable()) {
                                tcpActiveHandler(device, channel, contentString, mediaServerItem,  ssrcInfo, callback, inviteSessionType, inviteInfo);
                            }else {
                                log.warn("[Invite 200OK] 单端口收流模式不支持tcp主动模式收流");
                            }
                        }
                        inviteStreamService.updateInviteInfo(inviteInfo);
                    }
                } else {
                    ssrcInfo.setSsrc(ssrcInResponse);
                    inviteInfo.setSsrcInfo(ssrcInfo);
                    inviteInfo.setStream(ssrcInfo.getStream());
                    if (device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE")) {
                        if (mediaServerItem.isRtpEnable()) {
                            tcpActiveHandler(device, channel, contentString, mediaServerItem,  ssrcInfo, callback, inviteSessionType, inviteInfo);
                        }else {
                            log.warn("[Invite 200OK] 单端口收流模式不支持tcp主动模式收流");
                        }
                    }
                    inviteStreamService.updateInviteInfo(inviteInfo);
                }
            }else {
                if (ssrcInResponse != null) {
                    // 单端口
                    // 重新订阅流上线
                    SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByStream(MediaStreamUtil.RTP_APP, inviteInfo.getStream());
                    if (ssrcTransaction == null) {
                        return;
                    }
                    inviteStreamService.updateInviteInfoForSSRC(inviteInfo, ssrcInResponse);
                    ssrcTransaction.setDeviceId(device.getDeviceId());
                    ssrcTransaction.setChannelId(ssrcTransaction.getChannelId());
                    ssrcTransaction.setCallId(ssrcTransaction.getCallId());
                    ssrcTransaction.setSsrc(ssrcInResponse);
                    ssrcTransaction.setApp(MediaStreamUtil.RTP_APP);
                    ssrcTransaction.setStream(inviteInfo.getStream());
                    ssrcTransaction.setMediaServerId(mediaServerItem.getId());
                    ssrcTransaction.setSipTransactionInfo(new SipTransactionInfo((SIPResponse) responseEvent.getResponse()));
                    ssrcTransaction.setType(inviteSessionType);

                    sessionManager.put(ssrcTransaction);
                }
            }
        }
    }

    @Override
    public void download(Device device, DeviceChannel channel, String startTime, String endTime, int downloadSpeed, ErrorCallback<StreamInfo> callback) {

        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcPlayService.download(device.getServerId(), channel.getId(), startTime, endTime, downloadSpeed, callback);
            return;
        }

        MediaServer newMediaServerItem = this.getNewMediaServerItem(device);
        if (newMediaServerItem == null) {
            callback.run(InviteErrorCode.ERROR_FOR_ASSIST_NOT_READY.getCode(),
                    InviteErrorCode.ERROR_FOR_ASSIST_NOT_READY.getMsg(),
                    null);
            return;
        }

        download(newMediaServerItem, device, channel, startTime, endTime, downloadSpeed, callback);
    }


    // 在同流临界区内启动下载，与自然注销、成品通知和主动停止保持同一归属。
    private void download(MediaServer mediaServer, Device device, DeviceChannel channel, String startTime, String endTime, int downloadSpeed, ErrorCallback<StreamInfo> callback) {
        if (mediaServer == null ) {
            callback.run(InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getCode(),
                    InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getMsg(),
                    null);
            return;
        }

        String stream = receiveRtpServerService.getPlaybackStream(device, channel, startTime, endTime);

        synchronized (playbackLifecycleLock(stream)) {
            startDownload(mediaServer, device, channel, startTime, endTime, downloadSpeed, stream, callback);
        }
    }

    // 认领单次下载并保护异步回调，旧请求只能清理自己的结果和媒体。
    private void startDownload(MediaServer mediaServer, Device device, DeviceChannel channel, String startTime,
                               String endTime, int downloadSpeed, String stream, ErrorCallback<StreamInfo> callback) {
        // 原子认领下载发起权，并发请求只有一个能发起invite，其余等待结果
        boolean first = inviteStreamService.onceAndFirst(InviteSessionType.DOWNLOAD, channel.getId(), stream, callback);
        if (!first) {
            log.info("[录像下载] 已有请求在途，等待结果， deviceId: {}, channelId({}): {}, 开始时间: {}, 结束时间： {}",
                    device.getDeviceId(), channel.getDeviceId(), channel.getId(), startTime, endTime);
            return;
        }

        // 认领成功后二次确认，下载刚完成(已写入redis)时本线程直接复用，避免重复发起invite
        InviteInfo inviteInfoForConfirm = inviteStreamService.getInviteInfo(InviteSessionType.DOWNLOAD, channel.getId(), stream);
        if (inviteInfoForConfirm != null && inviteInfoForConfirm.getStreamInfo() != null
                && InviteSessionStatus.ok.equals(inviteInfoForConfirm.getStatus())) {
            inviteStreamService.call(InviteSessionType.DOWNLOAD, channel.getId(), stream,
                    InviteErrorCode.SUCCESS.getCode(),
                    InviteErrorCode.SUCCESS.getMsg(),
                    inviteInfoForConfirm.getStreamInfo());
            log.info("[录像下载] 已存在，直接返回， deviceId: {}, channelId({}): {}, 开始时间: {}, 结束时间： {}",
                    device.getDeviceId(), channel.getDeviceId(), channel.getId(), startTime, endTime);
            return;
        }

        Object attempt = new Object();
        downloadAttempts.put(stream, attempt);
        try {
            long restartDelay = downloadRestartAfter[Math.floorMod(stream.hashCode(), downloadRestartAfter.length)] - System.currentTimeMillis();
            if (restartDelay > 0) {
                Thread.sleep(Math.min(restartDelay, 1000));
            }
            SSRCInfo ssrcInfo = receiveRtpServerService.openGbRTPServerForDownload(mediaServer, device, channel, startTime, endTime, (code, msg, result) -> {
                synchronized (playbackLifecycleLock(stream)) {
                    if (downloadAttempts.get(stream) != attempt) {
                        return;
                    }
                    if (code == InviteErrorCode.SUCCESS.getCode() && result != null && result.getHookData() != null) {
                        StreamInfo streamInfo = onPublishHandlerForDownload(mediaServer, result.getHookData().getMediaInfo(), device, channel, startTime, endTime);
                        if (streamInfo != null) {
                            inviteStreamService.call(InviteSessionType.DOWNLOAD, channel.getId(), stream,
                                    InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
                            log.info("[录像下载] 调用成功 deviceId: {}, channelId: {}, 开始时间: {}, 结束时间: {}",
                                    device.getDeviceId(), channel.getDeviceId(), startTime, endTime);
                            return;
                        }
                        code = InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getCode();
                        msg = InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getMsg();
                    }
                    failDownload(mediaServer, device, channel, stream, attempt, code, msg);
                }
            });
            if (ssrcInfo == null || ssrcInfo.getPort() <= 0) {
                downloadAttempts.remove(stream, attempt);
                log.info("[录像下载端口/SSRC]获取失败，deviceId={},channelId={},ssrcInfo={}", device.getDeviceId(), channel.getDeviceId(), ssrcInfo);
                inviteStreamService.call(InviteSessionType.DOWNLOAD, channel.getId(), stream,
                        InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(),
                        InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getMsg(),
                        null);
                return;
            }
            log.info("[录像下载] deviceId: {}, channelId: {}, 开始时间： {}, 结束时间： {}， 下载速度：{}, 收流端口：{}, 收流模式：{}, SSRC: {}({}), SSRC校验：{}",
                    device.getDeviceId(), channel.getDeviceId(), startTime, endTime, downloadSpeed, ssrcInfo.getPort(), device.getStreamMode(),
                    ssrcInfo.getSsrc(), String.format("%08x", Long.parseLong(ssrcInfo.getSsrc())).toUpperCase(),
                    device.isSsrcCheck());

            // 初始化redis中的invite消息状态
            InviteInfo inviteInfo = InviteInfo.getInviteInfo(device.getDeviceId(), channel.getId(), ssrcInfo.getStream(), ssrcInfo, mediaServer.getId(),
                    mediaServer.getSdpIp(), ssrcInfo.getPort(), device.getStreamMode(), InviteSessionType.DOWNLOAD,
                    InviteSessionStatus.ready);
            inviteInfo.setCreateTime(System.currentTimeMillis());
            inviteStreamService.updateInviteInfo(inviteInfo);
            try {
                cmder.downloadStreamCmd(mediaServer, ssrcInfo, device, channel, startTime, endTime, downloadSpeed,
                        eventResult -> {
                            synchronized (playbackLifecycleLock(stream)) {
                                failDownload(mediaServer, device, channel, stream, attempt,
                                        InviteErrorCode.FAIL.getCode(), InviteErrorCode.FAIL.getMsg());
                            }
                        }, eventResult ->{
                            // 处理收到200ok后的TCP主动连接以及SSRC不一致的问题
                            InviteOKHandler(eventResult, ssrcInfo, mediaServer, device, channel,
                                     callback, inviteInfo, InviteSessionType.DOWNLOAD);

                            // 注册录像回调事件，录像下载结束后写入下载地址
                            HookSubscribe.Event hookEventForRecord = hookData -> {
                                synchronized (playbackLifecycleLock(stream)) {
                                    if (downloadAttempts.get(stream) != attempt) {
                                        return;
                                    }
                                    InviteInfo current = inviteStreamService.getInviteInfo(InviteSessionType.DOWNLOAD, channel.getId(), stream);
                                    RecordInfo record = hookData.getRecordInfo();
                                    if (current == null || current.getStreamInfo() == null || record == null || hookData.getMediaServer() == null
                                            || !Objects.equals(current.getCreateTime(), inviteInfo.getCreateTime())
                                            || !Objects.equals(mediaServer.getId(), hookData.getMediaServer().getId())
                                            || !Objects.equals(stream, record.getStream())
                                            || !MediaStreamUtil.RTP_APP.equals(record.getApp())
                                            // ZLM 的录制起始时间只有秒精度，允许本轮创建所在秒。
                                            || record.getStartTime() / 1000 < inviteInfo.getCreateTime() / 1000) {
                                        return;
                                    }
                                    DownloadFileInfo path = mediaServerService.getDownloadFilePath(mediaServer, record);
                                    current.getStreamInfo().setDownLoadFilePath(path);
                                    current.getStreamInfo().setProgress(1.0);
                                    inviteStreamService.updateInviteInfo(current, java.util.concurrent.TimeUnit.MINUTES.toMillis(15));
                                    downloadAttempts.remove(stream, attempt);
                                    subscribe.removeSubscribe(Hook.getInstance(HookType.on_record_mp4, MediaStreamUtil.RTP_APP, stream, mediaServer.getId()));
                                }
                            };
                            Hook hook = Hook.getInstance(HookType.on_record_mp4, MediaStreamUtil.RTP_APP, ssrcInfo.getStream(), mediaServer.getId());
                            // 设置过期时间，下载失败时自动处理订阅数据
                            hook.setExpireTime(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
                            subscribe.addSubscribe(hook, hookEventForRecord);
                        }, userSetting.getPlayTimeout().longValue(), task -> {
                            synchronized (playbackLifecycleLock(stream)) {
                                if (downloadAttempts.get(stream) != attempt) {
                                    return false;
                                }
                                task.run();
                                return true;
                            }
                        });
            } catch (InvalidArgumentException | SipException | ParseException e) {
                log.error("[命令发送失败] 录像下载: {}", e.getMessage());
                failDownload(mediaServer, device, channel, stream, attempt,
                        InviteErrorCode.FAIL.getCode(), InviteErrorCode.FAIL.getMsg());
            }
        } catch (Throwable e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("[录像下载] 未知异常，释放下载请求， deviceId: {}, channelId({}): {}", device.getDeviceId(), channel.getDeviceId(), channel.getId(), e);
            failDownload(mediaServer, device, channel, stream, attempt,
                    InviteErrorCode.FAIL.getCode(), InviteErrorCode.FAIL.getMsg());
        }
    }

    // 仅清理本次失败下载的资源，释放认领后新请求才能开始，旧失败不能影响下一轮。
    private void failDownload(MediaServer mediaServer, Device device, DeviceChannel channel, String stream,
                              Object attempt, int code, String msg) {
        if (downloadAttempts.get(stream) != attempt) {
            return;
        }
        dialogTerminationService.stopDeviceByStream(device, channel, MediaStreamUtil.RTP_APP, stream);
        receiveRtpServerService.closeRTPServer(mediaServer, MediaStreamUtil.RTP_APP, stream);
        subscribe.removeSubscribe(Hook.getInstance(HookType.on_record_mp4, MediaStreamUtil.RTP_APP, stream, mediaServer.getId()));
        inviteStreamService.removeInviteInfo(InviteSessionType.DOWNLOAD, channel.getId(), stream);
        downloadAttempts.remove(stream, attempt);
        downloadRestartAfter[Math.floorMod(stream.hashCode(), downloadRestartAfter.length)] =
                (System.currentTimeMillis() / 1000 + 1) * 1000;
        inviteStreamService.call(InviteSessionType.DOWNLOAD, channel.getId(), stream, code, msg, null);
    }

    // 串行读取下载进度与 MP4 完成写入，防止旧进度轮询覆盖已经完成的文件路径。
    @Override
    public StreamInfo getDownLoadInfo(Device device, DeviceChannel channel, String stream) {
        synchronized (playbackLifecycleLock(stream)) {
            return readDownloadInfo(device, channel, stream);
        }
    }

    // 在同流临界区内读取缓存或既有数据库兜底，保持进度与成品状态一致。
    private StreamInfo readDownloadInfo(Device device, DeviceChannel channel, String stream) {
        InviteInfo inviteInfo = inviteStreamService.getInviteInfo(InviteSessionType.DOWNLOAD, channel.getId(), stream);
        if (inviteInfo == null) {
            String app = MediaStreamUtil.RTP_APP;
            StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(app, stream);
            if (streamAuthorityInfo != null) {
                List<CloudRecordItem> allList = cloudRecordService.getAllList(null, app, stream, null, null, null, streamAuthorityInfo.getCallId(), null);
                if (allList.isEmpty()) {
                    log.warn("[获取下载进度] 未查询到录像下载的信息 {}/{}-{}", device.getDeviceId(), channel.getDeviceId(), stream);
                    return null;
                }

                String mediaServerId = allList.get(0).getMediaServerId();
                MediaServer mediaServer = mediaServerService.getOne(mediaServerId);
                if (mediaServer == null) {
                    log.warn("[获取下载进度] 未查询到录像下载的节点信息 {}/{}-{}", device.getDeviceId(), channel.getDeviceId(), stream);
                    return null;
                }
                log.warn("[获取下载进度] 发现下载已经结束，直接从数据库获取到文件 {}/{}-{}", device.getDeviceId(), channel.getDeviceId(), stream);
                DownloadFileInfo downloadFileInfo = mediaServerService.getDownloadFilePath(mediaServer, RecordInfo.getInstance(allList.get(0)));
                StreamInfo streamInfo = new StreamInfo();
                streamInfo.setDownLoadFilePath(downloadFileInfo);
                streamInfo.setApp(app);
                streamInfo.setStream(stream);
                streamInfo.setServerId(mediaServerId);
                streamInfo.setProgress(1.0);
                return streamInfo;
            }
        }

        if (inviteInfo == null || inviteInfo.getStreamInfo() == null) {
            log.warn("[获取下载进度] 未查询到录像下载的信息 {}/{}-{}", device.getDeviceId(), channel.getDeviceId(), stream);
            return null;
        }

        if (inviteInfo.getStreamInfo().getProgress() == 1
                || inviteInfo.getStreamInfo().getDownLoadFilePath() != null) {
            // 下载已经结束（进度到 100% 或已拿到下载地址），直接返回缓存中的结果，
            // 不再向媒体节点查询进度，避免流注销后进度被重置
            return inviteInfo.getStreamInfo();
        }

        // 获取当前已下载时长
        MediaServer mediaServerItem = inviteInfo.getStreamInfo().getMediaServer();
        if (mediaServerItem == null) {
            log.warn("[获取下载进度] 查询录像信息时发现节点不存在");
            return null;
        }
        String app = MediaStreamUtil.RTP_APP;
        Long duration  = mediaServerService.updateDownloadProcess(mediaServerItem, app, stream);
        if (duration == null || duration == 0) {
            // 流已注销时媒体节点查不到进度，此时保留上一次的进度，不回落为 0，
            // 避免「流刚离开、on_record_mp4 还没写入下载地址」这段时间进度从 100% 跳回 0%
            log.debug("[获取下载进度] 媒体节点未返回进度，保留上次进度: stream={}, progress={}",
                    stream, inviteInfo.getStreamInfo().getProgress());
        } else {
            String startTime = inviteInfo.getStreamInfo().getStartTime();
            String endTime = inviteInfo.getStreamInfo().getEndTime();
            // 此时start和end单位是秒
            long start = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime);
            long end = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime);

            BigDecimal currentCount = new BigDecimal(duration);
            BigDecimal totalCount = new BigDecimal((end - start) * 1000);
            BigDecimal divide = currentCount.divide(totalCount, 2, RoundingMode.HALF_UP);
            double process = divide.doubleValue();
            if (process > 0.999) {
                process = 1.0;
            }
            inviteInfo.getStreamInfo().setProgress(process);
        }
        inviteStreamService.updateInviteInfo(inviteInfo);
        return inviteInfo.getStreamInfo();
    }

    private StreamInfo onPublishHandlerForDownload(MediaServer mediaServerItemInuse, MediaInfo mediaInfo, Device device, DeviceChannel channel, String startTime, String endTime) {
        StreamInfo streamInfo = onPublishHandler(mediaServerItemInuse, mediaInfo, device, channel);
        if (streamInfo != null) {
            streamInfo.setProgress(0);
            streamInfo.setStartTime(startTime);
            streamInfo.setEndTime(endTime);
            InviteInfo inviteInfo = inviteStreamService.getInviteInfo(InviteSessionType.DOWNLOAD, channel.getId(), streamInfo.getStream());
            if (inviteInfo != null) {
                log.info("[录像下载] 更新invite消息中的stream信息");
                inviteInfo.setStatus(InviteSessionStatus.ok);
                inviteInfo.setStreamInfo(streamInfo);
                inviteStreamService.updateInviteInfo(inviteInfo);
            }
        }
        return streamInfo;
    }


    public StreamInfo onPublishHandler(MediaServer mediaServerItem, MediaInfo mediaInfo, Device device, DeviceChannel channel) {
        StreamInfo streamInfo = mediaServerService.getStreamInfoByAppAndStream(mediaServerItem, MediaStreamUtil.RTP_APP, mediaInfo.getStream(), mediaInfo, null);
        streamInfo.setDeviceId(device.getDeviceId());
        streamInfo.setChannelId(channel.getId());
        return streamInfo;
    }


    @Override
    public void zlmServerOffline(MediaServer mediaServer) {
        // 处理正在向上推流的上级平台
        List<SendRtpInfo> sendRtpInfos = sendRtpServerService.queryAll();
        if (!sendRtpInfos.isEmpty()) {
            for (SendRtpInfo sendRtpInfo : sendRtpInfos) {
                if (sendRtpInfo.getMediaServerId().equals(mediaServer.getId()) && sendRtpInfo.isSendToPlatform()) {
                    Platform platform = platformService.queryPlatformByServerGBId(sendRtpInfo.getTargetId());
                    CommonGBChannel channel = channelService.getOne(sendRtpInfo.getChannelId());
                    try {
                        sipCommanderFroPlatform.streamByeCmd(platform, sendRtpInfo, channel);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        log.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
                    }
                }
            }
        }
        // 处理正在观看的国标设备/平台事务
        List<SsrcTransaction> allSsrc = sessionManager.getAll();
        for (SsrcTransaction ssrcTransaction : allSsrc) {
            if (!Objects.equals(mediaServer.getId(), ssrcTransaction.getMediaServerId())) {
                continue;
            }
            if (ssrcTransaction.getSipTransactionInfo() == null) {
                continue;
            }
            try {
                if (ssrcTransaction.getPlatformId() != null) {
                    Platform platform = platformService.queryPlatformByServerGBId(ssrcTransaction.getPlatformId());
                    CommonGBChannel channel = ssrcTransaction.getChannelId() == null
                            ? null : channelService.getOne(ssrcTransaction.getChannelId());
                    if (platform == null || channel == null) {
                        continue;
                    }
                    sipCommanderFroPlatform.streamByeCmd(platform, channel,
                            ssrcTransaction.getSipTransactionInfo(), null);
                } else {
                    Device device = deviceService.getDeviceByDeviceId(ssrcTransaction.getDeviceId());
                    if (device == null) {
                        continue;
                    }
                    DeviceChannel deviceChannel = deviceChannelService.getOneById(ssrcTransaction.getChannelId());
                    if (deviceChannel == null) {
                        continue;
                    }
                    cmder.streamByeCmd(device, deviceChannel.getDeviceId(),
                            ssrcTransaction.getSipTransactionInfo(), null);
                }
                sessionManager.removeByCallId(ssrcTransaction.getCallId());
            } catch (Exception e) {
                log.error("[zlm离线] 发送BYE失败, callId={}", ssrcTransaction.getCallId(), e);
            }
        }
    }

    @Override
    public AudioBroadcastResult audioBroadcast(String deviceId, String channelDeviceId, Boolean broadcastMode) {

        Device device = deviceService.getDeviceByDeviceId(deviceId);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "未找到设备： " + deviceId);
        }
        DeviceChannel deviceChannel = deviceChannelService.getOne(deviceId, channelDeviceId);
        if (deviceChannel == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "未找到通道： " + channelDeviceId);
        }

        if (!userSetting.getServerId().equals(device.getServerId())) {
            return redisRpcPlayService.audioBroadcast(device.getServerId(), deviceId, channelDeviceId, broadcastMode);
        }
        log.info("[语音喊话] device： {}, channel: {}", device.getDeviceId(), deviceChannel.getDeviceId());
        MediaServer mediaServerItem = mediaServerService.getMediaServerForMinimumLoad(null);
        if (broadcastMode == null) {
            broadcastMode = true;
        }
        String app = broadcastMode ? MediaStreamUtil.GB28181_BROADCAST : MediaStreamUtil.GB28181_TALK;
        String stream = device.getDeviceId() + "_" + deviceChannel.getDeviceId();
        AudioBroadcastResult audioBroadcastResult = new AudioBroadcastResult();
        audioBroadcastResult.setApp(app);
        audioBroadcastResult.setStream(stream);
        audioBroadcastResult.setStreamInfo(new StreamContent(mediaServerService.getStreamInfoByAppAndStream(mediaServerItem, app, stream, null, null, null, false)));
        if (!broadcastMode) {
            audioBroadcastResult.setPlayStreamInfo(new StreamContent(
                    mediaServerService.getStreamInfoByAppAndStream(mediaServerItem,
                            MediaStreamUtil.GB28181_TALK, stream + "_talk",
                            null, null, null, true)));
        }
        audioBroadcastResult.setCodec("G.711");
        return audioBroadcastResult;
    }

    @Override
    public boolean audioBroadcastCmd(Device device, DeviceChannel deviceChannel, MediaServer mediaServerItem, String app, String stream, int timeout, boolean isFromPlatform, AudioBroadcastEvent event) throws InvalidArgumentException, ParseException, SipException {
        Assert.notNull(device, "设备不存在");
        Assert.notNull(deviceChannel, "通道不存在");
        log.info("[语音喊话] device： {}, channel: {}", device.getDeviceId(), deviceChannel.getDeviceId());
        // 查询通道使用状态
        if (audioBroadcastManager.exit(deviceChannel.getId())) {
            SendRtpInfo sendRtpInfo = sendRtpServerService.queryByChannelId(deviceChannel.getId(), device.getDeviceId());
            if (sendRtpInfo != null && sendRtpInfo.isOnlyAudio()) {
                // 查询流是否存在，不存在则认为是异常状态
                Boolean streamReady = mediaServerService.isStreamReady(mediaServerItem, sendRtpInfo.getApp(), sendRtpInfo.getStream());
                if (streamReady) {
                    log.warn("语音广播已经开启： {}", deviceChannel.getDeviceId());
                    event.call("语音广播已经开启");
                    return false;
                } else {
                    stopAudioBroadcast(device, deviceChannel);
                }
            }
        }

        // 发送通知
        cmder.audioBroadcastCmd(device, deviceChannel.getDeviceId(), eventResultForOk -> {
            // 发送成功
            AudioBroadcastCatch audioBroadcastCatch = new AudioBroadcastCatch(device.getDeviceId(), deviceChannel.getId(), mediaServerItem, app, stream, event, AudioBroadcastCatchStatus.Ready, isFromPlatform);
            audioBroadcastManager.update(audioBroadcastCatch);
            // 等待invite消息， 超时则结束
            String key = VideoManagerConstants.BROADCAST_WAITE_INVITE +  device.getDeviceId();
            if (!SipUtils.isFrontEnd(device.getDeviceId())) {
                key += audioBroadcastCatch.getChannelId();
            }
            dynamicTask.startDelay(key, ()->{
                log.info("[语音广播]等待invite消息超时：{}/{}", device.getDeviceId(), deviceChannel.getDeviceId());
                stopAudioBroadcast(device, deviceChannel);
            }, 10*1000);
        }, eventResultForError -> {
            // 发送失败
            log.error("语音广播发送失败： {}:{}", deviceChannel.getDeviceId(), eventResultForError.msg);
            event.call("语音广播发送失败");
            stopAudioBroadcast(device, deviceChannel);
        });
        return true;
    }

    @Override
    public boolean audioBroadcastInUse(Device device, DeviceChannel channel) {
        if (audioBroadcastManager.exit(channel.getId())) {
            SendRtpInfo sendRtpInfo = sendRtpServerService.queryByChannelId(channel.getId(), device.getDeviceId());
            if (sendRtpInfo != null && sendRtpInfo.isOnlyAudio()) {
                // 查询流是否存在，不存在则认为是异常状态
                MediaServer mediaServerServiceOne = mediaServerService.getOne(sendRtpInfo.getMediaServerId());
                Boolean streamReady = mediaServerService.isStreamReady(mediaServerServiceOne, sendRtpInfo.getApp(), sendRtpInfo.getStream());
                if (streamReady) {
                    log.warn("语音广播通道使用中： {}", channel.getDeviceId());
                    return true;
                }
            }
        }
        return false;
    }


    @Override
    public void stopAudioBroadcast(Device device, DeviceChannel channel) {
        log.info("[停止对讲] 设备：{}, 通道：{}", device.getDeviceId(), channel.getDeviceId());
        List<AudioBroadcastCatch> audioBroadcastCatchList = new ArrayList<>();
        if (channel == null) {
            audioBroadcastCatchList.addAll(audioBroadcastManager.getByDeviceId(device.getDeviceId()));
        } else {
            audioBroadcastCatchList.addAll(audioBroadcastManager.getByDeviceId(device.getDeviceId()));
        }
        if (!audioBroadcastCatchList.isEmpty()) {
            for (AudioBroadcastCatch audioBroadcastCatch : audioBroadcastCatchList) {
                if (audioBroadcastCatch == null) {
                    continue;
                }
                SendRtpInfo sendRtpInfo = sendRtpServerService.queryByChannelId(channel.getId(), device.getDeviceId());
                if (sendRtpInfo != null) {
                    sendRtpServerService.delete(sendRtpInfo);
                    MediaServer mediaServer = mediaServerService.getOne(sendRtpInfo.getMediaServerId());
                    mediaServerService.stopSendRtp(mediaServer, sendRtpInfo.getApp(), sendRtpInfo.getStream(), null);
                    try {
                        cmder.streamByeCmdForDeviceInvite(device, channel.getDeviceId(), audioBroadcastCatch.getSipTransactionInfo(), null);
                    } catch (InvalidArgumentException | ParseException | SipException |
                             SsrcTransactionNotFoundException e) {
                        log.error("[消息发送失败] 发送语音喊话BYE失败");
                    }
                }

                audioBroadcastManager.del(channel.getId());
            }
        }
    }

    @Override
    public void zlmServerOnline(MediaServer mediaServer) {
        // 获取
        List<InviteInfo> inviteInfoList = inviteStreamService.getAllInviteInfo();
        if (inviteInfoList.isEmpty()) {
            return;
        }

        List<String> rtpServerList = mediaServerService.listRtpServer(mediaServer);
        if (rtpServerList.isEmpty()) {
            return;
        }
        for (InviteInfo inviteInfo : inviteInfoList) {
            if (!rtpServerList.contains(inviteInfo.getStream())){
                inviteStreamService.removeInviteInfo(inviteInfo);
            }
        }
    }

    @Override
    public void playbackPause(String streamId) throws ServiceException, InvalidArgumentException, ParseException, SipException {

        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(InviteSessionType.PLAYBACK, streamId);
        if (null == inviteInfo || inviteInfo.getStreamInfo() == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "streamId不存在");
        }
        Device device = deviceService.getDeviceByDeviceId(inviteInfo.getDeviceId());
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "设备不存在");
        }
        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcPlayService.playbackPause(device.getServerId(), streamId);
            return;
        }

        inviteInfo.getStreamInfo().setPause(true);
        inviteStreamService.updateInviteInfo(inviteInfo);
        MediaServer mediaServerItem = inviteInfo.getStreamInfo().getMediaServer();
        if (null == mediaServerItem) {
            log.warn("mediaServer 不存在!");
            throw new ServiceException("mediaServer不存在");
        }
        // zlm 暂停RTP超时检查
        // 使用zlm中的流ID
        String streamKey = inviteInfo.getStream();
        if (!mediaServerItem.isRtpEnable()) {
            streamKey = Long.toHexString(Long.parseLong(inviteInfo.getSsrcInfo().getSsrc())).toUpperCase();
        }
        Boolean result = mediaServerService.pauseRtpCheck(mediaServerItem, streamKey);
        if (!result) {
            throw new ServiceException("暂停RTP接收失败");
        }

        DeviceChannel channel = deviceChannelService.getOneById(inviteInfo.getChannelId());
        cmder.playPauseCmd(device, channel, inviteInfo.getStreamInfo());
    }

    @Override
    public void playbackResume(String streamId) throws ServiceException, InvalidArgumentException, ParseException, SipException {
        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(InviteSessionType.PLAYBACK, streamId);
        if (null == inviteInfo || inviteInfo.getStreamInfo() == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "streamId不存在");
        }
        Device device = deviceService.getDeviceByDeviceId(inviteInfo.getDeviceId());
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "设备不存在");
        }
        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcPlayService.playbackResume(device.getServerId(), streamId);
            return;
        }

        inviteInfo.getStreamInfo().setPause(false);
        inviteStreamService.updateInviteInfo(inviteInfo);
        MediaServer mediaServerItem = inviteInfo.getStreamInfo().getMediaServer();
        if (null == mediaServerItem) {
            log.warn("mediaServer 不存在!");
            throw new ServiceException("mediaServer不存在");
        }
        // 使用zlm中的流ID
        String streamKey = inviteInfo.getStream();
        if (!mediaServerItem.isRtpEnable()) {
            streamKey = Long.toHexString(Long.parseLong(inviteInfo.getSsrcInfo().getSsrc())).toUpperCase();
        }
        boolean result = mediaServerService.resumeRtpCheck(mediaServerItem, streamKey);
        if (!result) {
            throw new ServiceException("继续RTP接收失败");
        }
        DeviceChannel channel = deviceChannelService.getOneById(inviteInfo.getChannelId());
        cmder.playResumeCmd(device, channel, inviteInfo.getStreamInfo());
    }

    @Override
    public void playbackSeek(String streamId, long seekTime) throws InvalidArgumentException, ParseException, SipException {
        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(InviteSessionType.PLAYBACK, streamId);

        if (null == inviteInfo || inviteInfo.getStreamInfo() == null) {
            log.warn("streamId不存在!");
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "streamId不存在");
        }
        Device device = deviceService.getDeviceByDeviceId(inviteInfo.getDeviceId());
        DeviceChannel channel = deviceChannelService.getOneById(inviteInfo.getChannelId());
        cmder.playSeekCmd(device, channel, inviteInfo.getStreamInfo(), seekTime);
    }

    @Override
    public void playbackSpeed(String streamId, double speed) throws InvalidArgumentException, ParseException, SipException {
        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(InviteSessionType.PLAYBACK, streamId);

        if (null == inviteInfo || inviteInfo.getStreamInfo() == null) {
            log.warn("streamId不存在!");
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "streamId不存在");
        }
        Device device = deviceService.getDeviceByDeviceId(inviteInfo.getDeviceId());
        DeviceChannel channel = deviceChannelService.getOneById(inviteInfo.getChannelId());
        cmder.playSpeedCmd(device, channel, inviteInfo.getStreamInfo(), speed);
    }

    @Override
    public void startPushStream(SendRtpInfo sendRtpInfo, DeviceChannel channel, SIPResponse sipResponse, Platform platform, CallIdHeader callIdHeader) {
        // 开始发流
        MediaServer mediaInfo = mediaServerService.getOne(sendRtpInfo.getMediaServerId());

        if (mediaInfo != null) {
            try {
                if (sendRtpInfo.isTcpActive()) {
                    mediaServerService.startSendRtpPassive(mediaInfo, sendRtpInfo, null);
                } else {
                    mediaServerService.startSendRtp(mediaInfo, sendRtpInfo);
                }
                redisCatchStorage.sendPlatformStartPlayMsg(sendRtpInfo, channel, platform);
            }catch (ControllerException e) {
                log.error("RTP推流失败: {}", e.getMessage());
                startSendRtpStreamFailHand(sendRtpInfo, platform, callIdHeader);
                return;
            }

            log.info("RTP推流成功[ {}/{} ]，{}, ", sendRtpInfo.getApp(), sendRtpInfo.getStream(),
                    sendRtpInfo.isTcpActive()?"被动发流": sendRtpInfo.getIp() + ":" + sendRtpInfo.getPort());

        }
    }

    @Override
    public void startSendRtpStreamFailHand(SendRtpInfo sendRtpInfo, Platform platform, CallIdHeader callIdHeader) {
        if (sendRtpInfo.isOnlyAudio()) {
            Device device = deviceService.getDeviceByDeviceId(sendRtpInfo.getTargetId());
            DeviceChannel deviceChannel = deviceChannelService.getOneById(sendRtpInfo.getChannelId());
            AudioBroadcastCatch audioBroadcastCatch = audioBroadcastManager.get(sendRtpInfo.getChannelId());
            if (audioBroadcastCatch != null) {
                try {
                    cmder.streamByeCmd(device, deviceChannel.getDeviceId(), audioBroadcastCatch.getSipTransactionInfo(), null);
                } catch (SipException | ParseException | InvalidArgumentException |
                         SsrcTransactionNotFoundException exception) {
                    log.error("[命令发送失败] 停止语音对讲: {}", exception.getMessage());
                }
            }
        } else {
            if (platform != null) {
                // 向上级平台
                CommonGBChannel channel = channelService.getOne(sendRtpInfo.getChannelId());
                try {
                    commanderForPlatform.streamByeCmd(platform, sendRtpInfo, channel);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
                }
            }

        }
    }

    @Override
    public void talkCmd(Device device, DeviceChannel channel, MediaServer mediaServerItem, String stream, AudioBroadcastEvent event) {
        if (device == null || channel == null) {
            return;
        }
        // TODO 必须多端口模式才支持语音喊话鹤语音对讲
        log.info("[语音对讲] device： {}, channel: {}", device.getDeviceId(), channel.getDeviceId());
        // 查询通道使用状态
        if (audioBroadcastManager.exit(channel.getId())) {
            SendRtpInfo sendRtpInfo = sendRtpServerService.queryByChannelId(channel.getId(), device.getDeviceId());
            if (sendRtpInfo != null && sendRtpInfo.isOnlyAudio()) {
                // 查询流是否存在，不存在则认为是异常状态
                MediaServer mediaServer = mediaServerService.getOne(sendRtpInfo.getMediaServerId());
                Boolean streamReady = mediaServerService.isStreamReady(mediaServer, sendRtpInfo.getApp(), sendRtpInfo.getStream());
                if (streamReady) {
                    log.warn("[语音对讲] 正在语音广播，无法开启语音通话： {}", channel.getDeviceId());
                    event.call("正在语音广播");
                    return;
                } else {
                    stopAudioBroadcast(device, channel);
                }
            }
        }

        SendRtpInfo sendRtpInfo = sendRtpServerService.queryByChannelId(channel.getId(), device.getDeviceId());
        if (sendRtpInfo != null) {
            MediaServer mediaServer = mediaServerService.getOne(sendRtpInfo.getMediaServerId());
            Boolean streamReady = mediaServerService.isStreamReady(mediaServer, MediaStreamUtil.GB28181_TALK, sendRtpInfo.getReceiveStream());
            if (streamReady) {
                log.warn("[语音对讲] 进行中： {}", channel.getDeviceId());
                event.call("语音对讲进行中");
                return;
            } else {
                stopTalk(device, channel);
            }
        }

        talk(mediaServerItem, device, channel, stream, eventResult -> {
            log.warn("[语音对讲] 失败，{}/{}, 错误码 {} {}", device.getDeviceId(), channel.getDeviceId(), eventResult.statusCode, eventResult.msg);
            event.call("失败，错误码 " + eventResult.statusCode + ", " + eventResult.msg);
        }, () -> {
            log.warn("[语音对讲] 失败，{}/{} 超时", device.getDeviceId(), channel.getDeviceId());
            event.call("失败，超时 ");
            stopTalk(device, channel);
        }, errorMsg -> {
            log.warn("[语音对讲] 失败，{}/{} {}", device.getDeviceId(), channel.getDeviceId(), errorMsg);
            event.call(errorMsg);
            stopTalk(device, channel);
        });
    }

    private void stopTalk(Device device, DeviceChannel channel) {
        stopTalk(device, channel, null);
    }

    @Override
    public void stopTalk(Device device, DeviceChannel channel, Boolean streamIsReady) {
        log.info("[语音对讲] 停止， {}/{}", device.getDeviceId(), channel.getDeviceId());
        SendRtpInfo sendRtpInfo = sendRtpServerService.queryByChannelId(channel.getId(), device.getDeviceId());
        if (sendRtpInfo != null) {
            String mediaServerId = sendRtpInfo.getMediaServerId();
            if (mediaServerId != null) {
                MediaServer mediaServer = mediaServerService.getOne(mediaServerId);
                if (streamIsReady == null || streamIsReady) {
                    mediaServerService.stopSendRtp(mediaServer, sendRtpInfo.getApp(),
                            sendRtpInfo.getStream(), sendRtpInfo.getSsrc());
                }
            }
        } else {
            log.info("[语音对讲] 未找到发送信息，继续清理残留 SIP 事务");
        }

        dialogTerminationService.stopDevice(device, channel, InviteSessionType.TALK,
                sendRtpInfo == null ? null : sendRtpInfo.getStream());

        if (sendRtpInfo != null) {
            sendRtpServerService.deleteByChannel(channel.getId(), device.getDeviceId());
        }
    }

    @Override
    public void getSnap(String deviceId, String channelId, String fileName, ErrorCallback errorCallback) {
        Device device = deviceService.getDeviceByDeviceId(deviceId);
        Assert.notNull(device, "设备不存在");
        DeviceChannel channel = deviceChannelService.getOne(deviceId, channelId);
        Assert.notNull(channel, "通道不存在");
        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
        if (inviteInfo != null) {
            if (inviteInfo.getStreamInfo() != null) {
                // 已存在线直接截图
                MediaServer mediaServer = inviteInfo.getStreamInfo().getMediaServer();
                String path = "snap";
                // 请求截图
                log.info("[请求截图]: " + fileName);
                mediaServerService.getSnap(mediaServer, MediaStreamUtil.RTP_APP,  inviteInfo.getStreamInfo().getStream(), 15, 1, path, fileName);
                File snapFile = new File(path + File.separator + fileName);
                if (snapFile.exists()) {
                    errorCallback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), snapFile.getAbsoluteFile());
                }else {
                    errorCallback.run(InviteErrorCode.FAIL.getCode(), InviteErrorCode.FAIL.getMsg(), null);
                }
                return;
            }
        }

        MediaServer newMediaServerItem = getNewMediaServerItem(device);
        play(newMediaServerItem, deviceId, channelId, null, (code, msg, data)->{
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                InviteInfo inviteInfoForPlay = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
                if (inviteInfoForPlay != null && inviteInfoForPlay.getStreamInfo() != null) {
                    getSnap(deviceId, channelId, fileName, errorCallback);
                }else {
                    errorCallback.run(InviteErrorCode.FAIL.getCode(), InviteErrorCode.FAIL.getMsg(), null);
                }
            }else {
                errorCallback.run(InviteErrorCode.FAIL.getCode(), InviteErrorCode.FAIL.getMsg(), null);
            }
        });
    }

    @Override
    public void getSnap(CommonGBChannel channel, ErrorCallback<byte[]> errorCallback) {
        // 2016协议不支持直接获取国标通道的抓图， 只能通过点播的方式获取
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            log.warn("[快照] 未找到通道{}的设备信息", channel);
            errorCallback.run(InviteErrorCode.FAIL.getCode(), "未找到设备信息", null);
            return;
        }
        DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
        if (deviceChannel == null) {
            log.warn("[快照] 未找到通道{}的设备信息", channel);
            errorCallback.run(InviteErrorCode.FAIL.getCode(), "未找到原始通道", null);
            return;
        }

        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getGbId());
        if (inviteInfo != null) {
            if (inviteInfo.getStreamInfo() != null) {
                // 已存在线直接截图
                MediaServer mediaServer = inviteInfo.getStreamInfo().getMediaServer();
                String path = "snap";
                // 请求截图
                log.info("[请求截图]: 返回byte数组" );
                byte[] snapByteArray = mediaServerService.getSnap(mediaServer, MediaStreamUtil.RTP_APP,  inviteInfo.getStreamInfo().getStream(), 15, 1, path, null);
                if (snapByteArray != null) {
                    errorCallback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), snapByteArray);
                }else {
                    errorCallback.run(InviteErrorCode.FAIL.getCode(), InviteErrorCode.FAIL.getMsg(), null);
                }
                return;
            }
        }

        play(device, deviceChannel, (code, msg, data)->{
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                InviteInfo inviteInfoForPlay = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getGbId());
                if (inviteInfoForPlay != null && inviteInfoForPlay.getStreamInfo() != null) {
                    byte[] snapByteArray = mediaServerService.getSnap(data.getMediaServer(), MediaStreamUtil.RTP_APP,  data.getStream(), 15, 1, null, null);
                    errorCallback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), snapByteArray);
                }else {
                    errorCallback.run(InviteErrorCode.FAIL.getCode(), InviteErrorCode.FAIL.getMsg(), null);
                }
            }else {
                errorCallback.run(InviteErrorCode.FAIL.getCode(), InviteErrorCode.FAIL.getMsg(), null);
            }
        });
    }



    /** 停止指定会话，终止 SIP 对话并释放尚未完成注册的媒体接收器。 */
    @Override
    public void stop(InviteSessionType type, Device device, DeviceChannel channel, String stream) {
        if ((type == InviteSessionType.PLAYBACK || type == InviteSessionType.DOWNLOAD) && stream != null) {
            synchronized (playbackLifecycleLock(stream)) {
                stopSession(type, device, channel, stream);
            }
        } else {
            stopSession(type, device, channel, stream);
        }
    }

    /** 在同流临界区内终止回放或下载；其他点播类型沿用原有停止行为。 */
    private void stopSession(InviteSessionType type, Device device, DeviceChannel channel, String stream) {
        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcPlayService.stop(device.getServerId(), type,  channel.getId(), stream);
        }else {
            log.info("[停止点播/回放/下载] {}/{}", device.getDeviceId(), channel.getDeviceId());
            InviteInfo inviteInfo = inviteStreamService.getInviteInfo(type, channel.getId(), stream);
            int failed = dialogTerminationService.stopDevice(device, channel, type, stream);
            if (inviteInfo != null) {
                inviteStreamService.removeInviteInfo(inviteInfo);
            }
            if (type == InviteSessionType.PLAY) {
                deviceChannelService.stopPlay(channel.getId());
            }
            if (inviteInfo != null) {
                closeInviteReceiver(inviteInfo);
            }
            if (failed > 0) {
                log.warn("[停止点播/回放/下载] {}/{} 有 {} 个 BYE 发送失败", device.getDeviceId(),
                        channel.getDeviceId(), failed);
            }
        }
    }

    /** 仅按当前会话身份停止点播，过期清理请求不得影响新会话。 */
    @Override
    public void stop(InviteInfo inviteInfo) {
        Assert.notNull(inviteInfo, "参数异常");
        if (inviteInfo.getType() == InviteSessionType.PLAYBACK || inviteInfo.getType() == InviteSessionType.DOWNLOAD) {
            synchronized (playbackLifecycleLock(inviteInfo.getStream())) {
                stopSession(inviteInfo);
            }
        } else {
            stopSession(inviteInfo);
        }
    }

    /** 只在当前缓存仍属于请求会话时执行终止，避免旧清理任务跨过新会话。 */
    private void stopSession(InviteInfo inviteInfo) {
        DeviceChannel channel = deviceChannelService.getOneForSourceById(inviteInfo.getChannelId());
        if (channel == null) {
            log.warn("[停止点播] 发现通道不存在");
            inviteStreamService.removeInviteInfo(inviteInfo);
            return;
        }
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            log.warn("[停止点播] 发现设备不存在");
            inviteStreamService.removeInviteInfo(inviteInfo);
            return;
        }

        InviteInfo current = inviteStreamService.getInviteInfo(inviteInfo.getType(),
                inviteInfo.getChannelId(), inviteInfo.getStream());
        if (current != null
                && inviteInfo.getCreateTime() != null
                && current.getCreateTime() != null
                && !Objects.equals(current.getCreateTime(), inviteInfo.getCreateTime())) {
            log.warn("[停止点播] 忽略过期请求, stream={}", inviteInfo.getStream());
            return;
        }

        int failed = dialogTerminationService.stopDevice(device, channel,
                inviteInfo.getType(), inviteInfo.getStream());
        inviteStreamService.removeInviteInfo(inviteInfo);

        if (inviteInfo.getType() == InviteSessionType.PLAY) {
            deviceChannelService.stopPlay(channel.getId());
        }
        closeInviteReceiver(inviteInfo);
        if (failed > 0) {
            log.warn("[停止点播/回放/下载] {}/{} 有 {} 个 BYE 发送失败",
                    device.getDeviceId(), channel.getDeviceId(), failed);
        }
    }

    @Override
    public void play(CommonGBChannel channel, Boolean record, ErrorCallback<StreamInfo> callback) {
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            log.warn("[点播] 未找到通道{}的设备信息", channel);
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
        DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());

        MediaServer mediaServerItem = getNewMediaServerItem(device);
        if (mediaServerItem == null) {
            log.warn("[点播] 未找到可用的zlm deviceId: {},channelId:{}", device.getDeviceId(), deviceChannel.getDeviceId());
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到可用的zlm");
        }
        play(mediaServerItem, device, deviceChannel, null, record, callback);

    }

    @Override
    public void stopPlay(InviteSessionType inviteSessionType, CommonGBChannel channel) {
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            log.warn("[停止播放] 未找到通道{}的设备信息", channel);
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
        DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
        String stream = String.format("%s_%s", device.getDeviceId(), deviceChannel.getDeviceId());
        stop(inviteSessionType, device, deviceChannel, stream);
    }

    @Override
    public void stop(InviteSessionType inviteSessionType, CommonGBChannel channel, String stream) {
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            log.warn("[停止播放] 未找到通道{}的设备信息", channel);
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
        DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(channel.getGbId());
        stop(inviteSessionType, device, deviceChannel, stream);
    }

    @Override
    public void playBack(CommonGBChannel channel, Long startTime, Long stopTime, ErrorCallback<StreamInfo> callback) {
        if (startTime == null || stopTime == null) {
            throw new PlayException(Response.BAD_REQUEST, "bad request");
        }
        // 国标通道
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            log.warn("[点播] 未找到通道{}的设备信息", channel);
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
        DeviceChannel deviceChannel = deviceChannelService.getOneById(channel.getGbId());
        if (deviceChannel == null) {
            log.warn("[点播] 未找到通道{}", channel.getGbDeviceId());
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
        String startTimeStr = DateUtil.timestampTo_yyyy_MM_dd_HH_mm_ss(startTime);
        String stopTimeStr = DateUtil.timestampTo_yyyy_MM_dd_HH_mm_ss(stopTime);
        playBack(device, deviceChannel, startTimeStr, stopTimeStr, callback);
    }

    @Override
    public void download(CommonGBChannel channel, Long startTime, Long stopTime, Integer downloadSpeed, ErrorCallback<StreamInfo> callback) {
        if (startTime == null || stopTime == null || downloadSpeed == null) {
            throw new PlayException(Response.BAD_REQUEST, "bad request");
        }
        // 国标通道
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            log.warn("[点播] 未找到通道{}的设备信息", channel);
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
        DeviceChannel deviceChannel = deviceChannelService.getOneById(channel.getGbId());
        if (deviceChannel == null) {
            log.warn("[点播] 未找到通道{}", channel.getGbDeviceId());
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
        String startTimeStr = DateUtil.timestampTo_yyyy_MM_dd_HH_mm_ss(startTime);
        String stopTimeStr = DateUtil.timestampTo_yyyy_MM_dd_HH_mm_ss(stopTime);
        download(device, deviceChannel, startTimeStr, stopTimeStr, downloadSpeed, callback);

    }
}
