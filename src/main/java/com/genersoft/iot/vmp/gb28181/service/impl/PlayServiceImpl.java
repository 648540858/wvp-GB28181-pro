package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.*;
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
import com.genersoft.iot.vmp.utils.CloudRecordUtils;
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
import java.util.UUID;
import java.util.Vector;

@SuppressWarnings(value = {"rawtypes", "unchecked"})
@Slf4j
@Service("playService")
public class PlayServiceImpl implements IPlayService {

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
    @Async("taskExecutor")
    @org.springframework.context.event.EventListener
    public void onApplicationEvent(MediaArrivalEvent event) {
        if ("broadcast".equals(event.getApp()) || "talk".equals(event.getApp())) {
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
                    if ("broadcast".equals(event.getApp())) {
                        if (audioBroadcastManager.exit(channel.getId())) {
                            stopAudioBroadcast(device, channel);
                        }
                        // 开启语音对讲通道
                        try {
                            audioBroadcastCmd(device, channel, event.getMediaServer(),
                                    event.getApp(), event.getStream(), 60, false, (msg) -> {
                                        log.info("[语音对讲] 通道建立成功, device: {}, channel: {}", deviceId, channelId);
                                    });
                        } catch (InvalidArgumentException | ParseException | SipException e) {
                            log.error("[命令发送失败] 语音对讲: {}", e.getMessage());
                        }
                    }else if ("talk".equals(event.getApp())) {
                        // 开启语音对讲通道
                        talkCmd(device, channel, event.getMediaServer(), event.getStream(), (msg) -> {
                            log.info("[语音对讲] 通道建立成功, device: {}, channel: {}", deviceId, channelId);
                        });
                    }
                }
            }
        }


    }

    /**
     * 流离开的处理
     */
    @Async("taskExecutor")
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

        if ("broadcast".equals(event.getApp()) || "talk".equals(event.getApp())) {
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
                    if ("broadcast".equals(event.getApp())) {
                        stopAudioBroadcast(device, channel);
                    }else if ("talk".equals(event.getApp())) {
                        stopTalk(device, channel, false);
                    }
                }
            }
        }else if ("rtp".equals(event.getApp())) {
            // 释放ssrc
            InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(null, event.getStream());
            if (inviteInfo != null && inviteInfo.getStatus() == InviteSessionStatus.ok
                    && inviteInfo.getStreamInfo() != null && inviteInfo.getSsrcInfo() != null) {
                // 发送bye
                stop(inviteInfo);
            }

        }
    }

    /**
     * 流未找到的处理
     */
    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(MediaNotFoundEvent event) {
        if (!"rtp".equals(event.getApp())) {
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

    private SSRCInfo play(MediaServer mediaServerItem, Device device, DeviceChannel channel, String ssrc, Boolean record,
                          ErrorCallback<StreamInfo> callback) {
        if (mediaServerItem == null ) {
            if (callback != null) {
                callback.run(InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getCode(),
                        InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getMsg(),
                        null);
            }
            return null;
        }

        InviteInfo inviteInfoInCatch = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
        if (inviteInfoInCatch != null ) {
            if (inviteInfoInCatch.getStreamInfo() == null) {
                // 释放生成的ssrc，使用上一次申请的322

                ssrcFactory.releaseSsrc(mediaServerItem.getId(), ssrc);
                // 点播发起了但是尚未成功, 仅注册回调等待结果即可
                inviteStreamService.once(InviteSessionType.PLAY, channel.getId(), null, callback);
                log.info("[点播开始] 已经请求中，等待结果， deviceId: {}, channelId({}): {}", device.getDeviceId(), channel.getDeviceId(), channel.getId());
                return inviteInfoInCatch.getSsrcInfo();
            }else {
                StreamInfo streamInfo = inviteInfoInCatch.getStreamInfo();
                String streamId = streamInfo.getStream();
                if (streamId == null) {
                    callback.run(InviteErrorCode.ERROR_FOR_CATCH_DATA.getCode(), "点播失败， redis缓存streamId等于null", null);
                    inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
                            InviteErrorCode.ERROR_FOR_CATCH_DATA.getCode(),
                            "点播失败， redis缓存streamId等于null",
                            null);
                    return inviteInfoInCatch.getSsrcInfo();
                }
                MediaServer mediaInfo = streamInfo.getMediaServer();
                Boolean ready = mediaServerService.isStreamReady(mediaInfo, "rtp", streamId);
                if (ready != null && ready) {
                    if(callback != null) {
                        callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
                    }
                    inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
                            InviteErrorCode.SUCCESS.getCode(),
                            InviteErrorCode.SUCCESS.getMsg(),
                            streamInfo);
                    log.info("[点播已存在] 直接返回， deviceId: {}, channelId: {}", device.getDeviceId(), channel.getDeviceId());
                    return inviteInfoInCatch.getSsrcInfo();
                }else {
                    // 点播发起了但是尚未成功, 仅注册回调等待结果即可
                    inviteStreamService.once(InviteSessionType.PLAY, channel.getId(), null, callback);
                    deviceChannelService.stopPlay(channel.getId());
                    inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
                }
            }
        }

        String streamId = String.format("%s_%s", device.getDeviceId(), channel.getDeviceId());
        int tcpMode = device.getStreamMode().equals("TCP-ACTIVE")? 2: (device.getStreamMode().equals("TCP-PASSIVE")? 1:0);
        RTPServerParam rtpServerParam = new RTPServerParam();
        rtpServerParam.setMediaServerItem(mediaServerItem);
        rtpServerParam.setStreamId(streamId);
        rtpServerParam.setPresetSsrc(ssrc);
        rtpServerParam.setSsrcCheck(device.isSsrcCheck());
        rtpServerParam.setPlayback(false);
        rtpServerParam.setPort(0);
        rtpServerParam.setTcpMode(tcpMode);
        rtpServerParam.setOnlyAuto(false);
        rtpServerParam.setDisableAudio(!channel.isHasAudio());

        SSRCInfo ssrcInfo = receiveRtpServerService.openRTPServer(rtpServerParam, (code, msg, result) -> {

            if (code == InviteErrorCode.SUCCESS.getCode() && result != null && result.getHookData() != null) {
                // hook响应
                StreamInfo streamInfo = onPublishHandlerForPlay(result.getHookData().getMediaServer(), result.getHookData().getMediaInfo(), device, channel);
                if (streamInfo == null){
                    if (callback != null) {
                        callback.run(InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getCode(),
                                InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getMsg(), null);
                    }
                    inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
                            InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getCode(),
                            InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getMsg(), null);
                    return;
                }
                if (callback != null) {
                    callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
                }
                inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
                        InviteErrorCode.SUCCESS.getCode(),
                        InviteErrorCode.SUCCESS.getMsg(),
                        streamInfo);

                log.info("[点播成功] deviceId: {}, channelId:{}, 码流类型：{}", device.getDeviceId(), channel.getDeviceId(),
                        channel.getStreamIdentification());
                snapOnPlay(result.getHookData().getMediaServer(), device.getDeviceId(), channel.getDeviceId(), streamId);
            }else {
                if (callback != null) {
                    callback.run(code, msg, null);
                }
                inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null, code, msg, null);
                inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
                SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByStream("rtp", streamId);
                if (ssrcTransaction != null) {
                    try {
                        cmder.streamByeCmd(device, channel.getDeviceId(),"rtp", streamId, null, null);
                    } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
                        log.error("[点播超时]， 发送BYE失败 {}", e.getMessage());
                    } finally {
                        sessionManager.removeByStream("rtp", streamId);
                    }
                }
            }
        });
        if (ssrcInfo == null || ssrcInfo.getPort() <= 0) {
            log.info("[点播端口/SSRC]获取失败，deviceId={},channelId={},ssrcInfo={}", device.getDeviceId(), channel.getDeviceId(), ssrcInfo);
            callback.run(InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(), "获取端口或者ssrc失败", null);
            inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
                    InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(),
                    InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getMsg(),
                    null);
            return null;
        }
        log.info("[点播开始] deviceId: {}, channelId({}): {},码流类型：{}, 收流端口： {}, 码流：{}, 收流模式：{}, SSRC: {}, SSRC校验：{}",
                device.getDeviceId(), channel.getDeviceId(), channel.getId(), channel.getStreamIdentification(), ssrcInfo.getPort(), ssrcInfo.getStream(),
                device.getStreamMode(), ssrcInfo.getSsrc(), device.isSsrcCheck());

        // 初始化redis中的invite消息状态
        InviteInfo inviteInfo = InviteInfo.getInviteInfo(device.getDeviceId(), channel.getId(), ssrcInfo.getStream(), ssrcInfo, mediaServerItem.getId(),
                mediaServerItem.getSdpIp(), ssrcInfo.getPort(), device.getStreamMode(), InviteSessionType.PLAY,
                InviteSessionStatus.ready, userSetting.getRecordSip());
        if (record != null) {
            inviteInfo.setRecord(record);
        }else {
            inviteInfo.setRecord(userSetting.getRecordSip());
        }

        inviteStreamService.updateInviteInfo(inviteInfo);

        try {
            cmder.playStreamCmd(mediaServerItem, ssrcInfo, device, channel, (eventResult) -> {
                // 处理收到200ok后的TCP主动连接以及SSRC不一致的问题
                InviteOKHandler(eventResult, ssrcInfo, mediaServerItem, device, channel, callback, inviteInfo, InviteSessionType.PLAY);
            }, (event) -> {
                log.info("[点播失败]{}:{} deviceId: {}, channelId:{}",event.statusCode, event.msg, device.getDeviceId(), channel.getDeviceId());
                receiveRtpServerService.closeRTPServer(mediaServerItem, ssrcInfo);

                sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());
                if (callback != null) {
                    callback.run(event.statusCode, event.msg, null);
                }
                inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
                        event.statusCode, event.msg, null);

                inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
            }, userSetting.getPlayTimeout().longValue());
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 点播消息: {}", e.getMessage());
            receiveRtpServerService.closeRTPServer(mediaServerItem, ssrcInfo);
            sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());
            if (callback != null) {
                callback.run(InviteErrorCode.ERROR_FOR_SIP_SENDING_FAILED.getCode(),
                        InviteErrorCode.ERROR_FOR_SIP_SENDING_FAILED.getMsg(), null);
            }
            inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
                    InviteErrorCode.ERROR_FOR_SIP_SENDING_FAILED.getCode(),
                    InviteErrorCode.ERROR_FOR_SIP_SENDING_FAILED.getMsg(), null);

            inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
        }
        return ssrcInfo;
    }


    private void talk(MediaServer mediaServerItem, Device device, DeviceChannel channel, String stream,
                      HookSubscribe.Event hookEvent, SipSubscribe.Event errorEvent,
                      Runnable timeoutCallback, AudioBroadcastEvent audioEvent) {

        String playSsrc = ssrcFactory.getPlaySsrc(mediaServerItem.getId());

        if (playSsrc == null) {
            audioEvent.call("ssrc已经用尽");
            return;
        }
        SendRtpInfo sendRtpInfo;
        try {
            sendRtpInfo = sendRtpServerService.createSendRtpInfo(mediaServerItem, null, null, playSsrc, device.getDeviceId(), "talk", stream,
                    channel.getId(), true, false);
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
                mediaServerService.releaseSsrc(mediaServerItem.getId(), sendRtpInfo.getSsrc());
                sessionManager.removeByStream(sendRtpInfo.getApp(), sendRtpInfo.getStream());
            }
        }, userSetting.getPlayTimeout());

        try {
            Integer localPort = mediaServerService.startSendRtpPassive(mediaServerItem, sendRtpInfo, userSetting.getPlayTimeout() * 1000);
            if (localPort == null || localPort <= 0) {
                timeoutCallback.run();
                mediaServerService.releaseSsrc(mediaServerItem.getId(), sendRtpInfo.getSsrc());
                sessionManager.removeByStream(sendRtpInfo.getApp(), sendRtpInfo.getStream());
                return;
            }
            sendRtpInfo.setPort(localPort);
        }catch (ControllerException e) {
            mediaServerService.releaseSsrc(mediaServerItem.getId(), sendRtpInfo.getSsrc());
            log.info("[语音对讲]失败 deviceId: {}, channelId: {}", device.getDeviceId(), channel.getDeviceId());
            audioEvent.call("失败, " + e.getMessage());
            // 查看是否已经建立了通道，存在则发送bye
            stopTalk(device, channel);
        }


        // 查看设备是否已经在推流
        try {
            cmder.talkStreamCmd(mediaServerItem, sendRtpInfo, device, channel, callId, (hookData) -> {
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

                        SsrcTransaction ssrcTransaction = SsrcTransaction.buildForDevice(device.getDeviceId(), sendRtpInfo.getChannelId(), "talk", sendRtpInfo.getApp(),
                                sendRtpInfo.getStream(), sendRtpInfo.getSsrc(), sendRtpInfo.getMediaServerId(),
                                response, InviteSessionType.TALK);

                        sessionManager.put(ssrcTransaction);
                    } else {
                        log.error("[语音对讲]收到的消息错误，response不是SIPResponse");
                    }
                } else {
                    log.error("[语音对讲]收到的消息错误，event不是ResponseEvent");
                }

            }, (event) -> {
                dynamicTask.stop(timeOutTaskKey);
                mediaServerService.closeRTPServer(mediaServerItem, sendRtpInfo.getStream());
                // 释放ssrc
                mediaServerService.releaseSsrc(mediaServerItem.getId(), sendRtpInfo.getSsrc());
                sessionManager.removeByStream(sendRtpInfo.getApp(), sendRtpInfo.getStream());
                errorEvent.response(event);
            }, userSetting.getPlayTimeout().longValue());
        } catch (InvalidArgumentException | SipException | ParseException e) {

            log.error("[命令发送失败] 对讲消息: {}", e.getMessage());
            dynamicTask.stop(timeOutTaskKey);
            mediaServerService.closeRTPServer(mediaServerItem, sendRtpInfo.getStream());
            // 释放ssrc
            mediaServerService.releaseSsrc(mediaServerItem.getId(), sendRtpInfo.getSsrc());

            sessionManager.removeByStream(sendRtpInfo.getApp(), sendRtpInfo.getStream());
            SipSubscribe.EventResult eventResult = new SipSubscribe.EventResult();
            eventResult.type = SipSubscribe.EventResultType.cmdSendFailEvent;
            eventResult.statusCode = -1;
            eventResult.msg = "命令发送失败";
            errorEvent.response(eventResult);
        }
//        }

    }

    private void tcpActiveHandler(Device device, DeviceChannel channel, String contentString,
                                  MediaServer mediaServerItem, SSRCInfo ssrcInfo, ErrorCallback<StreamInfo> callback){
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
            Boolean result = mediaServerService.connectRtpServer(mediaServerItem, sdp.getConnection().getAddress(), port, ssrcInfo.getStream());
            log.info("[TCP主动连接对方] 结果： {}" , result);
            if (!result) {
                // 主动连接失败，结束流程， 清理数据
                receiveRtpServerService.closeRTPServer(mediaServerItem, ssrcInfo);
                sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());
                callback.run(InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getCode(),
                        InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getMsg(), null);
                inviteStreamService.call(InviteSessionType.BROADCAST, channel.getId(), null,
                        InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getCode(),
                        InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getMsg(), null);
            }
        } catch (SdpException e) {
            log.error("[TCP主动连接对方] deviceId: {}, channelId: {}, 解析200OK的SDP信息失败", device.getDeviceId(), channel.getDeviceId(), e);
            receiveRtpServerService.closeRTPServer(mediaServerItem, ssrcInfo);

            sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());

            callback.run(InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getCode(),
                    InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getMsg(), null);
            inviteStreamService.call(InviteSessionType.BROADCAST, channel.getId(), null,
                    InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getCode(),
                    InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getMsg(), null);
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
        String streamUrl;
        if (mediaServerItemInuse.getRtspPort() != 0) {
            streamUrl = String.format("rtsp://127.0.0.1:%s/%s/%s", mediaServerItemInuse.getRtspPort(), "rtp", stream);
        } else {
            streamUrl = String.format("http://127.0.0.1:%s/%s/%s.live.mp4", mediaServerItemInuse.getHttpPort(), "rtp", stream);
        }
        String path = "snap";
        String fileName = deviceId + "_" + channelId + ".jpg";
        // 请求截图
        log.info("[请求截图]: " + fileName);
        mediaServerService.getSnap(mediaServerItemInuse, streamUrl, 15, 1, path, fileName);
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

    private void playBack(MediaServer mediaServerItem,
                         Device device, DeviceChannel channel, String startTime,
                         String endTime, ErrorCallback<StreamInfo> callback) {

        String startTimeStr = startTime.replace("-", "")
                .replace(":", "")
                .replace(" ", "");
        String endTimeTimeStr = endTime.replace("-", "")
                .replace(":", "")
                .replace(" ", "");

        String stream = device.getDeviceId() + "_" + channel.getDeviceId() + "_" + startTimeStr + "_" + endTimeTimeStr;
        int tcpMode = device.getStreamMode().equals("TCP-ACTIVE")? 2: (device.getStreamMode().equals("TCP-PASSIVE")? 1:0);

        RTPServerParam rtpServerParam = new RTPServerParam();
        rtpServerParam.setMediaServerItem(mediaServerItem);
        rtpServerParam.setStreamId(stream);
        rtpServerParam.setSsrcCheck(device.isSsrcCheck());
        rtpServerParam.setPlayback(true);
        rtpServerParam.setPort(0);
        rtpServerParam.setTcpMode(tcpMode);
        rtpServerParam.setOnlyAuto(false);
        rtpServerParam.setDisableAudio(!channel.isHasAudio());
        SSRCInfo ssrcInfo = receiveRtpServerService.openRTPServer(rtpServerParam, (code, msg, result) -> {
            if (code == InviteErrorCode.SUCCESS.getCode() && result != null && result.getHookData() != null) {
                // hook响应
                StreamInfo streamInfo = onPublishHandlerForPlayback(result.getHookData().getMediaServer(), result.getHookData().getMediaInfo(), device, channel, startTime, endTime);
                if (streamInfo == null) {
                    log.warn("设备回放API调用失败！");
                    callback.run(InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getCode(),
                            InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getMsg(), null);
                    return;
                }
                callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
                log.info("[录像回放] 成功 deviceId: {}, channelId: {},  开始时间: {}, 结束时间： {}", device.getDeviceId(), channel.getGbDeviceId(), startTime, endTime);
            }else {
                if (callback != null) {
                    callback.run(code, msg, null);
                }
                inviteStreamService.call(InviteSessionType.PLAYBACK, channel.getId(), null, code, msg, null);
                inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAYBACK, channel.getId());
                SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByStream("rtp", stream);
                if (ssrcTransaction != null) {
                    try {
                        cmder.streamByeCmd(device, channel.getDeviceId(),"rtp",  stream, null, null);
                    } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
                        log.error("[录像回放] 发送BYE失败 {}", e.getMessage());
                    } finally {
                        sessionManager.removeByStream("rtp", stream);
                    }
                }
            }
        });
        if (ssrcInfo == null || ssrcInfo.getPort() <= 0) {
            log.info("[回放端口/SSRC]获取失败，deviceId={},channelId={},ssrcInfo={}", device.getDeviceId(), channel.getDeviceId(), ssrcInfo);
            if (callback != null) {
                callback.run(InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(), "获取端口或者ssrc失败", null);
            }
            inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
                    InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(),
                    InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getMsg(),
                    null);
            return;
        }

        log.info("[录像回放] deviceId: {}, channelId: {}, 开始时间: {}, 结束时间： {}, 收流端口：{}, 收流模式：{}, SSRC: {}, SSRC校验：{}",
                device.getDeviceId(), channel.getGbDeviceId(), startTime, endTime, ssrcInfo.getPort(), device.getStreamMode(),
                ssrcInfo.getSsrc(), device.isSsrcCheck());
        // 初始化redis中的invite消息状态
        InviteInfo inviteInfo = InviteInfo.getInviteInfo(device.getDeviceId(), channel.getId(), ssrcInfo.getStream(), ssrcInfo, mediaServerItem.getId(),
                mediaServerItem.getSdpIp(), ssrcInfo.getPort(), device.getStreamMode(), InviteSessionType.PLAYBACK,
                InviteSessionStatus.ready, userSetting.getRecordSip());
        inviteStreamService.updateInviteInfo(inviteInfo);

        try {
            cmder.playbackStreamCmd(mediaServerItem, ssrcInfo, device, channel, startTime, endTime,
                    eventResult -> {
                        // 处理收到200ok后的TCP主动连接以及SSRC不一致的问题
                        InviteOKHandler(eventResult, ssrcInfo, mediaServerItem, device, channel,
                                callback, inviteInfo, InviteSessionType.PLAYBACK);
                    }, eventResult -> {
                        log.info("[录像回放] 失败，{} {}", eventResult.statusCode, eventResult.msg);
                        if (callback != null) {
                            callback.run(eventResult.statusCode, eventResult.msg, null);
                        }

                        receiveRtpServerService.closeRTPServer(mediaServerItem, ssrcInfo);
                        sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());
                        inviteStreamService.removeInviteInfo(inviteInfo);
                    }, userSetting.getPlayTimeout().longValue());
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 录像回放: {}", e.getMessage());
            if (callback != null) {
                callback.run(InviteErrorCode.FAIL.getCode(), e.getMessage(), null);
            }
            receiveRtpServerService.closeRTPServer(mediaServerItem, ssrcInfo);
            sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());
            inviteStreamService.removeInviteInfo(inviteInfo);
        }
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
                    tcpActiveHandler(device, channel, contentString, mediaServerItem, ssrcInfo, callback);
                }
            }else {
                // 单端口
                if (device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE")) {
                    log.warn("[Invite 200OK] 单端口收流模式不支持tcp主动模式收流");
                }

            }
        }else {
            log.info("[Invite 200OK] 收到invite 200, 发现下级自定义了ssrc: {}", ssrcInResponse);
            // ssrc 不一致
            if (mediaServerItem.isRtpEnable()) {
                // 多端口
                if (device.isSsrcCheck()) {
                    // ssrc检验
                    // 更新ssrc
                    log.info("[Invite 200OK] SSRC修正 {}->{}", ssrcInfo.getSsrc(), ssrcInResponse);
                    // 释放ssrc
                    mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                    Boolean result = mediaServerService.updateRtpServerSSRC(mediaServerItem, ssrcInfo.getStream(), ssrcInResponse);
                    if (!result) {
                        try {
                            log.warn("[Invite 200OK] 更新ssrc失败，停止点播 {}/{}", device.getDeviceId(), channel.getDeviceId());
                            cmder.streamByeCmd(device, channel.getDeviceId(), ssrcInfo.getApp(), ssrcInfo.getStream(), null, null);
                        } catch (InvalidArgumentException | SipException | ParseException | SsrcTransactionNotFoundException e) {
                            log.error("[命令发送失败] 停止播放， 发送BYE: {}", e.getMessage());
                        }

                        // 释放ssrc
                        mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());

                        sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());

                        callback.run(InviteErrorCode.ERROR_FOR_RESET_SSRC.getCode(),
                                "下级自定义了ssrc,重新设置收流信息失败", null);
                        inviteStreamService.call(inviteSessionType, channel.getId(), null,
                                InviteErrorCode.ERROR_FOR_RESET_SSRC.getCode(),
                                "下级自定义了ssrc,重新设置收流信息失败", null);

                    }else {
                        ssrcInfo.setSsrc(ssrcInResponse);
                        inviteInfo.setSsrcInfo(ssrcInfo);
                        inviteInfo.setStream(ssrcInfo.getStream());
                        if (device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE")) {
                            if (mediaServerItem.isRtpEnable()) {
                                tcpActiveHandler(device, channel, contentString, mediaServerItem,  ssrcInfo, callback);
                            }else {
                                log.warn("[Invite 200OK] 单端口收流模式不支持tcp主动模式收流");
                            }
                        }
                        inviteStreamService.updateInviteInfo(inviteInfo);
                    }
                }
            }else {
                if (ssrcInResponse != null) {
                    // 单端口
                    // 重新订阅流上线
                    SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByStream("rtp", inviteInfo.getStream());
                    sessionManager.removeByStream("rtp", inviteInfo.getStream());
                    inviteStreamService.updateInviteInfoForSSRC(inviteInfo, ssrcInResponse);
                    ssrcTransaction.setDeviceId(device.getDeviceId());
                    ssrcTransaction.setChannelId(ssrcTransaction.getChannelId());
                    ssrcTransaction.setCallId(ssrcTransaction.getCallId());
                    ssrcTransaction.setSsrc(ssrcInResponse);
                    ssrcTransaction.setApp("rtp");
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


    private void download(MediaServer mediaServerItem, Device device, DeviceChannel channel, String startTime, String endTime, int downloadSpeed, ErrorCallback<StreamInfo> callback) {
        if (mediaServerItem == null ) {
            callback.run(InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getCode(),
                    InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getMsg(),
                    null);
            return;
        }

        int tcpMode = device.getStreamMode().equals("TCP-ACTIVE")? 2: (device.getStreamMode().equals("TCP-PASSIVE")? 1:0);
        // 录像下载不使用固定流地址，固定流地址会导致如果开始时间与结束时间一致时文件错误的叠加在一起
        RTPServerParam rtpServerParam = new RTPServerParam();
        rtpServerParam.setMediaServerItem(mediaServerItem);
        rtpServerParam.setSsrcCheck(device.isSsrcCheck());
        rtpServerParam.setPlayback(true);
        rtpServerParam.setPort(0);
        rtpServerParam.setTcpMode(tcpMode);
        rtpServerParam.setOnlyAuto(false);
        rtpServerParam.setDisableAudio(!channel.isHasAudio());
        SSRCInfo ssrcInfo = receiveRtpServerService.openRTPServer(rtpServerParam, (code, msg, result) -> {
            if (code == InviteErrorCode.SUCCESS.getCode() && result != null && result.getHookData() != null) {
                // hook响应
                StreamInfo streamInfo = onPublishHandlerForDownload(mediaServerItem, result.getHookData().getMediaInfo(), device, channel, startTime, endTime);
                if (streamInfo == null) {
                    log.warn("[录像下载] 获取流地址信息失败");
                    callback.run(InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getCode(),
                            InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getMsg(), null);
                    return;
                }
                callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
                log.info("[录像下载] 调用成功 deviceId: {}, channelId: {},  开始时间: {}, 结束时间： {}", device.getDeviceId(), channel, startTime, endTime);
            }else {
                if (callback != null) {
                    callback.run(code, msg, null);
                }
                inviteStreamService.call(InviteSessionType.DOWNLOAD, channel.getId(), null, code, msg, null);
                inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.DOWNLOAD, channel.getId());
                if (result != null && result.getSsrcInfo() != null) {
                    SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByStream(result.getSsrcInfo().getApp(), result.getSsrcInfo().getStream());
                    if (ssrcTransaction != null) {
                        try {
                            cmder.streamByeCmd(device, channel.getDeviceId(), ssrcTransaction.getApp(), ssrcTransaction.getStream(), null, null);
                        } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
                            log.error("[录像下载] 发送BYE失败 {}", e.getMessage());
                        } finally {
                            sessionManager.removeByStream(ssrcTransaction.getApp(), ssrcTransaction.getStream());
                        }
                    }
                }
            }
        });
        if (ssrcInfo == null || ssrcInfo.getPort() <= 0) {
            log.info("[录像下载端口/SSRC]获取失败，deviceId={},channelId={},ssrcInfo={}", device.getDeviceId(), channel.getDeviceId(), ssrcInfo);
            if (callback != null) {
                callback.run(InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(), "获取端口或者ssrc失败", null);
            }
            inviteStreamService.call(InviteSessionType.PLAY, channel.getId(), null,
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
        InviteInfo inviteInfo = InviteInfo.getInviteInfo(device.getDeviceId(), channel.getId(), ssrcInfo.getStream(), ssrcInfo, mediaServerItem.getId(),
                mediaServerItem.getSdpIp(), ssrcInfo.getPort(), device.getStreamMode(), InviteSessionType.DOWNLOAD,
                InviteSessionStatus.ready, true);
        inviteInfo.setStartTime(startTime);
        inviteInfo.setEndTime(endTime);

        inviteStreamService.updateInviteInfo(inviteInfo);
        try {
            cmder.downloadStreamCmd(mediaServerItem, ssrcInfo, device, channel, startTime, endTime, downloadSpeed,
                    eventResult -> {
                        // 对方返回错误
                        callback.run(InviteErrorCode.FAIL.getCode(), String.format("录像下载失败， 错误码： %s, %s", eventResult.statusCode, eventResult.msg), null);
                        receiveRtpServerService.closeRTPServer(mediaServerItem, ssrcInfo);
                        sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());
                        inviteStreamService.removeInviteInfo(inviteInfo);
                    }, eventResult ->{
                        // 处理收到200ok后的TCP主动连接以及SSRC不一致的问题
                        InviteOKHandler(eventResult, ssrcInfo, mediaServerItem, device, channel,
                                 callback, inviteInfo, InviteSessionType.DOWNLOAD);

                        // 注册录像回调事件，录像下载结束后写入下载地址
                        HookSubscribe.Event hookEventForRecord = (hookData) -> {
                            log.info("[录像下载] 收到录像写入磁盘消息： ， {}/{}-{}",
                                    inviteInfo.getDeviceId(), inviteInfo.getChannelId(), ssrcInfo.getStream());
                            log.info("[录像下载] 收到录像写入磁盘消息内容： " + hookData);
                            RecordInfo recordInfo = hookData.getRecordInfo();
                            String filePath = recordInfo.getFilePath();
                            DownloadFileInfo downloadFileInfo = CloudRecordUtils.getDownloadFilePath(mediaServerItem, filePath);
                            InviteInfo inviteInfoForNew = inviteStreamService.getInviteInfo(inviteInfo.getType()
                                    , inviteInfo.getChannelId(), inviteInfo.getStream());
                            if (inviteInfoForNew != null && inviteInfoForNew.getStreamInfo() != null) {
                                inviteInfoForNew.getStreamInfo().setDownLoadFilePath(downloadFileInfo);
                                // 不可以马上移除会导致后续接口拿不到下载地址
                                inviteStreamService.updateInviteInfo(inviteInfoForNew, 60*15L);
                            }
                        };
                        Hook hook = Hook.getInstance(HookType.on_record_mp4, "rtp", ssrcInfo.getStream(), mediaServerItem.getId());
                        // 设置过期时间，下载失败时自动处理订阅数据
                        hook.setExpireTime(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
                        subscribe.addSubscribe(hook, hookEventForRecord);
                    }, userSetting.getPlayTimeout().longValue());
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 录像下载: {}", e.getMessage());
            callback.run(InviteErrorCode.FAIL.getCode(),e.getMessage(), null);
            receiveRtpServerService.closeRTPServer(mediaServerItem, ssrcInfo);
            sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());
            inviteStreamService.removeInviteInfo(inviteInfo);
        }
    }

    @Override
    public StreamInfo getDownLoadInfo(Device device, DeviceChannel channel, String stream) {


        InviteInfo inviteInfo = inviteStreamService.getInviteInfo(InviteSessionType.DOWNLOAD, channel.getId(), stream);
        if (inviteInfo == null) {
            String app = "rtp";
            StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(app, stream);
            if (streamAuthorityInfo != null) {
                List<CloudRecordItem> allList = cloudRecordService.getAllList(null, app, stream, null, null, null, streamAuthorityInfo.getCallId(), null);
                if (allList.isEmpty()) {
                    log.warn("[获取下载进度] 未查询到录像下载的信息 {}/{}-{}", device.getDeviceId(), channel.getDeviceId(), stream);
                    return null;
                }
                String filePath = allList.get(0).getFilePath();
                if (filePath == null) {
                    log.warn("[获取下载进度] 未查询到录像下载的文件路径 {}/{}-{}", device.getDeviceId(), channel.getDeviceId(), stream);
                    return null;
                }
                String mediaServerId = allList.get(0).getMediaServerId();
                MediaServer mediaServer = mediaServerService.getOne(mediaServerId);
                if (mediaServer == null) {
                    log.warn("[获取下载进度] 未查询到录像下载的节点信息 {}/{}-{}", device.getDeviceId(), channel.getDeviceId(), stream);
                    return null;
                }
                log.warn("[获取下载进度] 发现下载已经结束，直接从数据库获取到文件 {}/{}-{}", device.getDeviceId(), channel.getDeviceId(), stream);
                DownloadFileInfo downloadFileInfo = CloudRecordUtils.getDownloadFilePath(mediaServer, filePath);
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

        if (inviteInfo.getStreamInfo().getProgress() == 1) {
            return inviteInfo.getStreamInfo();
        }

        // 获取当前已下载时长
        MediaServer mediaServerItem = inviteInfo.getStreamInfo().getMediaServer();
        if (mediaServerItem == null) {
            log.warn("[获取下载进度] 查询录像信息时发现节点不存在");
            return null;
        }
        String app = "rtp";
        Long duration  = mediaServerService.updateDownloadProcess(mediaServerItem, app, stream);
        if (duration == null || duration == 0) {
            inviteInfo.getStreamInfo().setProgress(0);
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
        StreamInfo streamInfo = mediaServerService.getStreamInfoByAppAndStream(mediaServerItem, "rtp", mediaInfo.getStream(), mediaInfo, null);
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
        // 处理正在观看的国标设备
        List<SsrcTransaction> allSsrc = sessionManager.getAll();
        if (allSsrc.size() > 0) {
            for (SsrcTransaction ssrcTransaction : allSsrc) {
                if (ssrcTransaction.getMediaServerId().equals(mediaServer.getId())) {
                    Device device = deviceService.getDeviceByDeviceId(ssrcTransaction.getDeviceId());
                    if (device == null) {
                        continue;
                    }
                    DeviceChannel deviceChannel = deviceChannelService.getOneById(ssrcTransaction.getChannelId());
                    if (deviceChannel == null) {
                        continue;
                    }
                    try {
                        cmder.streamByeCmd(device, deviceChannel.getDeviceId(), ssrcTransaction.getApp(),
                                ssrcTransaction.getStream(), null, null);
                    } catch (InvalidArgumentException | ParseException | SipException |
                             SsrcTransactionNotFoundException e) {
                        log.error("[zlm离线]为正在使用此zlm的设备， 发送BYE失败 {}", e.getMessage());
                    }
                }
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
        String app = broadcastMode?"broadcast":"talk";
        String stream = device.getDeviceId() + "_" + deviceChannel.getDeviceId();
        AudioBroadcastResult audioBroadcastResult = new AudioBroadcastResult();
        audioBroadcastResult.setApp(app);
        audioBroadcastResult.setStream(stream);
        audioBroadcastResult.setStreamInfo(new StreamContent(mediaServerService.getStreamInfoByAppAndStream(mediaServerItem, app, stream, null, null, null, false)));
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
    public void pauseRtp(String streamId) throws ServiceException, InvalidArgumentException, ParseException, SipException {

        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(InviteSessionType.PLAYBACK, streamId);
        if (null == inviteInfo || inviteInfo.getStreamInfo() == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "streamId不存在");
        }
        Device device = deviceService.getDeviceByDeviceId(inviteInfo.getDeviceId());
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "设备不存在");
        }
        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcPlayService.pauseRtp(device.getServerId(), streamId);
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
    public void resumeRtp(String streamId) throws ServiceException, InvalidArgumentException, ParseException, SipException {
        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(InviteSessionType.PLAYBACK, streamId);
        if (null == inviteInfo || inviteInfo.getStreamInfo() == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "streamId不存在");
        }
        Device device = deviceService.getDeviceByDeviceId(inviteInfo.getDeviceId());
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "设备不存在");
        }
        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcPlayService.resumeRtp(device.getServerId(), streamId);
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
            Boolean streamReady = mediaServerService.isStreamReady(mediaServer, "rtp", sendRtpInfo.getReceiveStream());
            if (streamReady) {
                log.warn("[语音对讲] 进行中： {}", channel.getDeviceId());
                event.call("语音对讲进行中");
                return;
            } else {
                stopTalk(device, channel);
            }
        }

        talk(mediaServerItem, device, channel, stream, (hookData) -> {
            log.info("[语音对讲] 收到设备发来的流");
        }, eventResult -> {
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
        if (sendRtpInfo == null) {
            log.info("[语音对讲] 停止失败， 未找到发送信息，可能已经停止");
            return;
        }
        // 停止向设备推流
        String mediaServerId = sendRtpInfo.getMediaServerId();
        if (mediaServerId == null) {
            return;
        }

        MediaServer mediaServer = mediaServerService.getOne(mediaServerId);

        if (streamIsReady == null || streamIsReady) {
            mediaServerService.stopSendRtp(mediaServer, sendRtpInfo.getApp(), sendRtpInfo.getStream(), sendRtpInfo.getSsrc());
        }

        ssrcFactory.releaseSsrc(mediaServerId, sendRtpInfo.getSsrc());

        SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByStream(sendRtpInfo.getApp(), sendRtpInfo.getStream());
        if (ssrcTransaction != null) {
            try {
                cmder.streamByeCmd(device, channel.getDeviceId(), sendRtpInfo.getApp(), sendRtpInfo.getStream(), null, null);
            } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException  e) {
                log.info("[语音对讲] 停止消息发送失败，可能已经停止");
            }
        }
        sendRtpServerService.deleteByChannel(channel.getId(), device.getDeviceId());
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
                MediaServer mediaServerItemInuse = inviteInfo.getStreamInfo().getMediaServer();
                String streamUrl;
                if (mediaServerItemInuse.getRtspPort() != 0) {
                    streamUrl = String.format("rtsp://127.0.0.1:%s/%s/%s", mediaServerItemInuse.getRtspPort(), "rtp",  inviteInfo.getStreamInfo().getStream());
                }else {
                    streamUrl = String.format("http://127.0.0.1:%s/%s/%s.live.mp4", mediaServerItemInuse.getHttpPort(), "rtp",  inviteInfo.getStreamInfo().getStream());
                }
                String path = "snap";
                // 请求截图
                log.info("[请求截图]: " + fileName);
                mediaServerService.getSnap(mediaServerItemInuse, streamUrl, 15, 1, path, fileName);
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
    public void stop(InviteSessionType type, Device device, DeviceChannel channel, String stream) {
        if (!userSetting.getServerId().equals(device.getServerId())) {
            redisRpcPlayService.stop(device.getServerId(), type,  channel.getId(), stream);
        }else {
            InviteInfo inviteInfo = inviteStreamService.getInviteInfo(type, channel.getId(), stream);
            if (inviteInfo == null) {
                if (type == InviteSessionType.PLAY) {
                    deviceChannelService.stopPlay(channel.getId());
                }
                return;
            }
            inviteStreamService.removeInviteInfo(inviteInfo);
            if (InviteSessionStatus.ok == inviteInfo.getStatus()) {
                try {
                    log.info("[停止点播/回放/下载] {}/{}", device.getDeviceId(), channel.getDeviceId());
                    cmder.streamByeCmd(device, channel.getDeviceId(), "rtp", inviteInfo.getStream(), null, null);
                } catch (InvalidArgumentException | SipException | ParseException | SsrcTransactionNotFoundException e) {
                    log.error("[命令发送失败] 停止点播/回放/下载， 发送BYE: {}", e.getMessage());
                    throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
                }
            }

            if (inviteInfo.getType() == InviteSessionType.PLAY) {
                deviceChannelService.stopPlay(channel.getId());
            }
            if (inviteInfo.getStreamInfo() != null) {
                receiveRtpServerService.closeRTPServer(inviteInfo.getStreamInfo().getMediaServer(), inviteInfo.getSsrcInfo());
            }
        }
    }

    @Override
    public void stop(InviteInfo inviteInfo) {
        Assert.notNull(inviteInfo, "参数异常");
        DeviceChannel channel = deviceChannelService.getOneForSourceById(inviteInfo.getChannelId());
        if (channel == null) {
            log.warn("[停止点播] 发现通道不存在");
            return;
        }
        Device device = deviceService.getDevice(channel.getDataDeviceId());
        if (device == null) {
            log.warn("[停止点播] 发现设备不存在");
            return;
        }
        inviteStreamService.removeInviteInfo(inviteInfo);
        if (InviteSessionStatus.ok == inviteInfo.getStatus()) {
            try {
                log.info("[停止点播/回放/下载] {}/{}", device.getDeviceId(), channel.getDeviceId());
                cmder.streamByeCmd(device, channel.getDeviceId(), "rtp", inviteInfo.getStream(), null, null);
            } catch (InvalidArgumentException | SipException | ParseException | SsrcTransactionNotFoundException e) {
                log.warn("[命令发送失败] 停止点播/回放/下载， 发送BYE: {}", e.getMessage());
            }
        }

        if (inviteInfo.getType() == InviteSessionType.PLAY) {
            deviceChannelService.stopPlay(channel.getId());
        }
        if (inviteInfo.getStreamInfo() != null) {
            receiveRtpServerService.closeRTPServer(inviteInfo.getStreamInfo().getMediaServer(), inviteInfo.getSsrcInfo());
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
