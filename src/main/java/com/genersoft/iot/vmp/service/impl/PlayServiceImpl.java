package com.genersoft.iot.vmp.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.genersoft.iot.vmp.common.*;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.exception.ServiceException;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
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
import com.genersoft.iot.vmp.media.zlm.SendRtpPortManager;
import com.genersoft.iot.vmp.service.IDeviceChannelService;
import com.genersoft.iot.vmp.service.IDeviceService;
import com.genersoft.iot.vmp.service.IInviteStreamService;
import com.genersoft.iot.vmp.service.IPlayService;
import com.genersoft.iot.vmp.service.bean.DownloadFileInfo;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.CloudRecordUtils;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.AudioBroadcastResult;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.gb28181.play.bean.AudioBroadcastEvent;
import gov.nist.javax.sip.message.SIPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.sdp.*;
import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.header.CallIdHeader;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

@SuppressWarnings(value = {"rawtypes", "unchecked"})
@Service
@DS("master")
public class PlayServiceImpl implements IPlayService {

    private final static Logger logger = LoggerFactory.getLogger(PlayServiceImpl.class);

    @Autowired
    private IVideoManagerStorage storager;

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
    private SendRtpPortManager sendRtpPortManager;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private VideoStreamSessionManager streamSession;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IDeviceChannelService channelService;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private ISIPCommanderForPlatform commanderForPlatform;

    @Autowired
    private SSRCFactory ssrcFactory;

    /**
     * 流到来的处理
     */
    @Async("taskExecutor")
    @org.springframework.context.event.EventListener
    public void onApplicationEvent(MediaArrivalEvent event) {
        if ("broadcast".equals(event.getApp())) {
            if (event.getStream().indexOf("_") > 0) {
                String[] streamArray = event.getStream().split("_");
                if (streamArray.length == 2) {
                    String deviceId = streamArray[0];
                    String channelId = streamArray[1];
                    Device device = deviceService.getDevice(deviceId);
                    if (device == null) {
                        logger.info("[语音对讲/喊话] 未找到设备：{}", deviceId);
                        return;
                    }
                    if ("broadcast".equals(event.getApp())) {
                        if (audioBroadcastManager.exit(deviceId, channelId)) {
                            stopAudioBroadcast(deviceId, channelId);
                        }
                        // 开启语音对讲通道
                        try {
                            audioBroadcastCmd(device, channelId, event.getMediaServer(),
                                    event.getApp(), event.getStream(), 60, false, (msg) -> {
                                        logger.info("[语音对讲] 通道建立成功, device: {}, channel: {}", deviceId, channelId);
                                    });
                        } catch (InvalidArgumentException | ParseException | SipException e) {
                            logger.error("[命令发送失败] 语音对讲: {}", e.getMessage());
                        }
                    }else if ("talk".equals(event.getApp())) {
                        // 开启语音对讲通道
                        talkCmd(device, channelId, event.getMediaServer(), event.getStream(), (msg) -> {
                            logger.info("[语音对讲] 通道建立成功, device: {}, channel: {}", deviceId, channelId);
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
        List<SendRtpItem> sendRtpItems = redisCatchStorage.querySendRTPServerByStream(event.getStream());
        if (!sendRtpItems.isEmpty()) {
            for (SendRtpItem sendRtpItem : sendRtpItems) {
                if (sendRtpItem != null && sendRtpItem.getApp().equals(event.getApp())) {
                    String platformId = sendRtpItem.getPlatformId();
                    Device device = deviceService.getDevice(platformId);
                    try {
                        if (device != null) {
                            cmder.streamByeCmd(device, sendRtpItem.getChannelId(), event.getStream(), sendRtpItem.getCallId());
                            if (sendRtpItem.getPlayType().equals(InviteStreamType.BROADCAST)
                                    || sendRtpItem.getPlayType().equals(InviteStreamType.TALK)) {
                                AudioBroadcastCatch audioBroadcastCatch = audioBroadcastManager.get(sendRtpItem.getDeviceId(), sendRtpItem.getChannelId());
                                if (audioBroadcastCatch != null) {
                                    // 来自上级平台的停止对讲
                                    logger.info("[停止对讲] 来自上级，平台：{}, 通道：{}", sendRtpItem.getDeviceId(), sendRtpItem.getChannelId());
                                    audioBroadcastManager.del(sendRtpItem.getDeviceId(), sendRtpItem.getChannelId());
                                }
                            }
                        }
                    } catch (SipException | InvalidArgumentException | ParseException |
                             SsrcTransactionNotFoundException e) {
                        logger.error("[命令发送失败] 发送BYE: {}", e.getMessage());
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
                    Device device = deviceService.getDevice(deviceId);
                    if (device == null) {
                        logger.info("[语音对讲/喊话] 未找到设备：{}", deviceId);
                        return;
                    }
                    if ("broadcast".equals(event.getApp())) {
                        stopAudioBroadcast(deviceId, channelId);
                    }else if ("talk".equals(event.getApp())) {
                        stopTalk(device, channelId, false);
                    }
                }
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
        DeviceChannel deviceChannel = storager.queryChannel(deviceId, channelId);
        if (deviceChannel == null) {
            return;
        }
        if (s.length == 2) {
            logger.info("[ZLM HOOK] 预览流未找到, 发起自动点播：{}->{}->{}/{}", event.getMediaServer().getId(), event.getSchema(), event.getApp(), event.getStream());
            play(event.getMediaServer(), deviceId, channelId, null, null);
        } else if (s.length == 4) {
            // 此时为录像回放， 录像回放格式为> 设备ID_通道ID_开始时间_结束时间
            String startTimeStr = s[2];
            String endTimeStr = s[3];
            if (startTimeStr == null || endTimeStr == null || startTimeStr.length() != 14 || endTimeStr.length() != 14) {
                return;
            }
            String startTime = DateUtil.urlToyyyy_MM_dd_HH_mm_ss(startTimeStr);
            String endTime = DateUtil.urlToyyyy_MM_dd_HH_mm_ss(endTimeStr);
            logger.info("[ZLM HOOK] 回放流未找到, 发起自动点播：{}->{}->{}/{}-{}-{}",
                    event.getMediaServer().getId(), event.getSchema(),
                    event.getApp(), event.getStream(),
                    startTime, endTime
            );

            SSRCInfo ssrcInfo = mediaServerService.openRTPServer(event.getMediaServer(), event.getStream(), null,
                    device.isSsrcCheck(), true, 0, false, !deviceChannel.getHasAudio(), false, device.getStreamModeForParam());
            playBack(event.getMediaServer(), ssrcInfo, deviceId, channelId, startTime, endTime, null);
        }
    }


    @Override
    public SSRCInfo play(MediaServer mediaServerItem, String deviceId, String channelId, String ssrc, ErrorCallback<Object> callback) {
        if (mediaServerItem == null) {
            logger.warn("[点播] 未找到可用的zlm deviceId: {},channelId:{}", deviceId, channelId);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到可用的zlm");
        }
        Device device = redisCatchStorage.getDevice(deviceId);
        if (device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE") && !mediaServerItem.isRtpEnable()) {
            logger.warn("[点播] 单端口收流时不支持TCP主动方式收流 deviceId: {},channelId:{}", deviceId, channelId);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "单端口收流时不支持TCP主动方式收流");
        }
        DeviceChannel channel = channelService.getOne(deviceId, channelId);
        if (channel == null) {
            logger.warn("[点播] 未找到通道 deviceId: {},channelId:{}", deviceId, channelId);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到通道");
        }
        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, deviceId, channelId);
        if (inviteInfo != null ) {
            if (inviteInfo.getStreamInfo() == null) {
                // 释放生成的ssrc，使用上一次申请的
                ssrcFactory.releaseSsrc(mediaServerItem.getId(), ssrc);
                // 点播发起了但是尚未成功, 仅注册回调等待结果即可
                inviteStreamService.once(InviteSessionType.PLAY, deviceId, channelId, null, callback);
                logger.info("[点播开始] 已经请求中，等待结果， deviceId: {}, channelId: {}", device.getDeviceId(), channelId);
                return inviteInfo.getSsrcInfo();
            }else {
                StreamInfo streamInfo = inviteInfo.getStreamInfo();
                String streamId = streamInfo.getStream();
                if (streamId == null) {
                    callback.run(InviteErrorCode.ERROR_FOR_CATCH_DATA.getCode(), "点播失败， redis缓存streamId等于null", null);
                    inviteStreamService.call(InviteSessionType.PLAY, device.getDeviceId(), channelId, null,
                            InviteErrorCode.ERROR_FOR_CATCH_DATA.getCode(),
                            "点播失败， redis缓存streamId等于null",
                            null);
                    return inviteInfo.getSsrcInfo();
                }
                String mediaServerId = streamInfo.getMediaServerId();
                MediaServer mediaInfo = mediaServerService.getOne(mediaServerId);
                Boolean ready = mediaServerService.isStreamReady(mediaInfo, "rtp", streamId);
                if (ready != null && ready) {
                    callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
                    inviteStreamService.call(InviteSessionType.PLAY, device.getDeviceId(), channelId, null,
                            InviteErrorCode.SUCCESS.getCode(),
                            InviteErrorCode.SUCCESS.getMsg(),
                            streamInfo);
                    logger.info("[点播已存在] 直接返回， deviceId: {}, channelId: {}", device.getDeviceId(), channelId);
                    return inviteInfo.getSsrcInfo();
                }else {
                    // 点播发起了但是尚未成功, 仅注册回调等待结果即可
                    inviteStreamService.once(InviteSessionType.PLAY, deviceId, channelId, null, callback);
                    storager.stopPlay(streamInfo.getDeviceID(), streamInfo.getChannelId());
                    inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, deviceId, channelId);
                }
            }
        }
        String streamId = String.format("%s_%s", device.getDeviceId(), channelId);
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(mediaServerItem, streamId, ssrc, device.isSsrcCheck(),  false, 0, false, !channel.getHasAudio(), false, device.getStreamModeForParam());
        if (ssrcInfo == null) {
            callback.run(InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(), InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getMsg(), null);
            inviteStreamService.call(InviteSessionType.PLAY, device.getDeviceId(), channelId, null,
                    InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(),
                    InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getMsg(),
                    null);
            return null;
        }
        play(mediaServerItem, ssrcInfo, device, channel, callback);
        return ssrcInfo;
    }

    private void talk(MediaServer mediaServerItem, Device device, String channelId, String stream,
                      HookSubscribe.Event hookEvent, SipSubscribe.Event errorEvent,
                      Runnable timeoutCallback, AudioBroadcastEvent audioEvent) {

        String playSsrc = ssrcFactory.getPlaySsrc(mediaServerItem.getId());

        if (playSsrc == null) {
            audioEvent.call("ssrc已经用尽");
            return;
        }
        SendRtpItem sendRtpItem = new SendRtpItem();
        sendRtpItem.setApp("talk");
        sendRtpItem.setStream(stream);
        sendRtpItem.setSsrc(playSsrc);
        sendRtpItem.setDeviceId(device.getDeviceId());
        sendRtpItem.setPlatformId(device.getDeviceId());
        sendRtpItem.setChannelId(channelId);
        sendRtpItem.setRtcp(false);
        sendRtpItem.setMediaServerId(mediaServerItem.getId());
        sendRtpItem.setOnlyAudio(true);
        sendRtpItem.setPlayType(InviteStreamType.TALK);
        sendRtpItem.setPt(8);
        sendRtpItem.setStatus(1);
        sendRtpItem.setTcpActive(false);
        sendRtpItem.setTcp(true);
        sendRtpItem.setUsePs(false);
        sendRtpItem.setReceiveStream(stream + "_talk");

        String callId = SipUtils.getNewCallId();
        int port = sendRtpPortManager.getNextPort(mediaServerItem);
        //端口获取失败的ssrcInfo 没有必要发送点播指令
        if (port <= 0) {
            logger.info("[语音对讲] 端口分配异常，deviceId={},channelId={}", device.getDeviceId(), channelId);
            audioEvent.call("端口分配异常");
            return;
        }
        sendRtpItem.setLocalPort(port);
        sendRtpItem.setPort(port);
        logger.info("[语音对讲]开始 deviceId: {}, channelId: {},收流端口： {}, 收流模式：{}, SSRC: {}, SSRC校验：{}", device.getDeviceId(), channelId, sendRtpItem.getLocalPort(), device.getStreamMode(), sendRtpItem.getSsrc(), false);
        // 超时处理
        String timeOutTaskKey = UUID.randomUUID().toString();
        dynamicTask.startDelay(timeOutTaskKey, () -> {

            logger.info("[语音对讲] 收流超时 deviceId: {}, channelId: {}，端口：{}, SSRC: {}", device.getDeviceId(), channelId, sendRtpItem.getPort(), sendRtpItem.getSsrc());
            timeoutCallback.run();
            // 点播超时回复BYE 同时释放ssrc以及此次点播的资源
            try {
                cmder.streamByeCmd(device, channelId, sendRtpItem.getStream(), null);
            } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
                logger.error("[语音对讲]超时， 发送BYE失败 {}", e.getMessage());
            } finally {
                timeoutCallback.run();
                mediaServerService.releaseSsrc(mediaServerItem.getId(), sendRtpItem.getSsrc());
                streamSession.remove(device.getDeviceId(), channelId, sendRtpItem.getStream());
            }
        }, userSetting.getPlayTimeout());

        try {
            mediaServerService.startSendRtpPassive(mediaServerItem, null, sendRtpItem, userSetting.getPlayTimeout() * 1000);
        }catch (ControllerException e) {
            mediaServerService.releaseSsrc(mediaServerItem.getId(), sendRtpItem.getSsrc());
            logger.info("[语音对讲]失败 deviceId: {}, channelId: {}", device.getDeviceId(), channelId);
            audioEvent.call("失败, " + e.getMessage());
            // 查看是否已经建立了通道，存在则发送bye
            stopTalk(device, channelId);
        }


        // 查看设备是否已经在推流
        try {
            cmder.talkStreamCmd(mediaServerItem, sendRtpItem, device, channelId, callId, (hookData) -> {
                logger.info("[语音对讲] 流已生成， 开始推流： " + hookData);
                dynamicTask.stop(timeOutTaskKey);
                // TODO 暂不做处理
            }, (hookData) -> {
                logger.info("[语音对讲] 设备开始推流： " + hookData);
                dynamicTask.stop(timeOutTaskKey);

            }, (event) -> {
                dynamicTask.stop(timeOutTaskKey);

                if (event.event instanceof ResponseEvent) {
                    ResponseEvent responseEvent = (ResponseEvent) event.event;
                    if (responseEvent.getResponse() instanceof SIPResponse) {
                        SIPResponse response = (SIPResponse) responseEvent.getResponse();
                        sendRtpItem.setFromTag(response.getFromTag());
                        sendRtpItem.setToTag(response.getToTag());
                        sendRtpItem.setCallId(response.getCallIdHeader().getCallId());
                        redisCatchStorage.updateSendRTPSever(sendRtpItem);

                        streamSession.put(device.getDeviceId(), channelId, "talk",
                                sendRtpItem.getStream(), sendRtpItem.getSsrc(), sendRtpItem.getMediaServerId(),
                                response, InviteSessionType.TALK);
                    } else {
                        logger.error("[语音对讲]收到的消息错误，response不是SIPResponse");
                    }
                } else {
                    logger.error("[语音对讲]收到的消息错误，event不是ResponseEvent");
                }

            }, (event) -> {
                dynamicTask.stop(timeOutTaskKey);
                mediaServerService.closeRTPServer(mediaServerItem, sendRtpItem.getStream());
                // 释放ssrc
                mediaServerService.releaseSsrc(mediaServerItem.getId(), sendRtpItem.getSsrc());
                streamSession.remove(device.getDeviceId(), channelId, sendRtpItem.getStream());
                errorEvent.response(event);
            });
        } catch (InvalidArgumentException | SipException | ParseException e) {

            logger.error("[命令发送失败] 对讲消息: {}", e.getMessage());
            dynamicTask.stop(timeOutTaskKey);
            mediaServerService.closeRTPServer(mediaServerItem, sendRtpItem.getStream());
            // 释放ssrc
            mediaServerService.releaseSsrc(mediaServerItem.getId(), sendRtpItem.getSsrc());

            streamSession.remove(device.getDeviceId(), channelId, sendRtpItem.getStream());
            SipSubscribe.EventResult eventResult = new SipSubscribe.EventResult();
            eventResult.type = SipSubscribe.EventResultType.cmdSendFailEvent;
            eventResult.statusCode = -1;
            eventResult.msg = "命令发送失败";
            errorEvent.response(eventResult);
        }
//        }

    }



    @Override
    public void play(MediaServer mediaServerItem, SSRCInfo ssrcInfo, Device device, DeviceChannel channel,
                     ErrorCallback<Object> callback) {

        if (mediaServerItem == null || ssrcInfo == null) {
            callback.run(InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getCode(),
                    InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getMsg(),
                    null);
            return;
        }
        logger.info("[点播开始] deviceId: {}, channelId: {},码流类型：{}, 收流端口： {}, 码流：{}, 收流模式：{}, SSRC: {}, SSRC校验：{}",
                device.getDeviceId(), channel.getChannelId(), channel.getStreamIdentification(), ssrcInfo.getPort(), ssrcInfo.getStream(),
                device.getStreamMode(), ssrcInfo.getSsrc(), device.isSsrcCheck());
        //端口获取失败的ssrcInfo 没有必要发送点播指令
        if (ssrcInfo.getPort() <= 0) {
            logger.info("[点播端口分配异常]，deviceId={},channelId={},ssrcInfo={}", device.getDeviceId(), channel.getChannelId(), ssrcInfo);
            // 释放ssrc
            mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
            streamSession.remove(device.getDeviceId(), channel.getChannelId(), ssrcInfo.getStream());

            callback.run(InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(), "点播端口分配异常", null);
            inviteStreamService.call(InviteSessionType.PLAY, device.getDeviceId(), channel.getChannelId(), null,
                    InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(), "点播端口分配异常", null);
            return;
        }

        // 初始化redis中的invite消息状态
        InviteInfo inviteInfo = InviteInfo.getInviteInfo(device.getDeviceId(), channel.getChannelId(), ssrcInfo.getStream(), ssrcInfo,
                mediaServerItem.getSdpIp(), ssrcInfo.getPort(), device.getStreamMode(), InviteSessionType.PLAY,
                InviteSessionStatus.ready);
        inviteStreamService.updateInviteInfo(inviteInfo);
        // 超时处理
        String timeOutTaskKey = UUID.randomUUID().toString();
        dynamicTask.startDelay(timeOutTaskKey, () -> {
            // 执行超时任务时查询是否已经成功，成功了则不执行超时任务，防止超时任务取消失败的情况
            InviteInfo inviteInfoForTimeOut = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, device.getDeviceId(), channel.getChannelId());
            if (inviteInfoForTimeOut == null || inviteInfoForTimeOut.getStreamInfo() == null) {
                logger.info("[点播超时] 收流超时 deviceId: {}, channelId: {},码流：{}，端口：{}, SSRC: {}",
                        device.getDeviceId(), channel.getChannelId(), channel.getStreamIdentification(),
                        ssrcInfo.getPort(), ssrcInfo.getSsrc());

                callback.run(InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getCode(), InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getMsg(), null);
                inviteStreamService.call(InviteSessionType.PLAY, device.getDeviceId(), channel.getChannelId(), null,
                        InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getCode(), InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getMsg(), null);
                inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, device.getDeviceId(), channel.getChannelId());

                try {
                    cmder.streamByeCmd(device, channel.getChannelId(), ssrcInfo.getStream(), null);
                } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
                    logger.error("[点播超时]， 发送BYE失败 {}", e.getMessage());
                } finally {
                    mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                    mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
                    streamSession.remove(device.getDeviceId(), channel.getChannelId(), ssrcInfo.getStream());
                    mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
                    // 取消订阅消息监听
                    subscribe.removeSubscribe(Hook.getInstance(HookType.on_media_arrival, "rtp", ssrcInfo.getStream(), mediaServerItem.getId()));
                }
            }else {
                logger.info("[点播超时] 收流超时 deviceId: {}, channelId: {},码流：{}，端口：{}, SSRC: {}",
                        device.getDeviceId(), channel.getChannelId(), channel.getStreamIdentification(),
                        ssrcInfo.getPort(), ssrcInfo.getSsrc());

                mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());

                mediaServerService.closeRTPServer(mediaServerItem.getId(), ssrcInfo.getStream());
                streamSession.remove(device.getDeviceId(), channel.getChannelId(), ssrcInfo.getStream());
            }
        }, userSetting.getPlayTimeout());

        try {
            cmder.playStreamCmd(mediaServerItem, ssrcInfo, device, channel, (hookData ) -> {
                logger.info("收到订阅消息： " + hookData);
                dynamicTask.stop(timeOutTaskKey);
                // hook响应
                StreamInfo streamInfo = onPublishHandlerForPlay(hookData.getMediaServer(), hookData.getMediaInfo(), device.getDeviceId(), channel.getChannelId());
                if (streamInfo == null){
                    callback.run(InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getCode(),
                            InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getMsg(), null);
                    inviteStreamService.call(InviteSessionType.PLAY, device.getDeviceId(), channel.getChannelId(), null,
                            InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getCode(),
                            InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getMsg(), null);
                    return;
                }
                callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
                inviteStreamService.call(InviteSessionType.PLAY, device.getDeviceId(), channel.getChannelId(), null,
                        InviteErrorCode.SUCCESS.getCode(),
                        InviteErrorCode.SUCCESS.getMsg(),
                        streamInfo);
                logger.info("[点播成功] deviceId: {}, channelId:{}, 码流类型：{}", device.getDeviceId(), channel.getChannelId(),
                        channel.getStreamIdentification());
                snapOnPlay(hookData.getMediaServer(), device.getDeviceId(), channel.getChannelId(), ssrcInfo.getStream());
            }, (eventResult) -> {
                // 处理收到200ok后的TCP主动连接以及SSRC不一致的问题
                InviteOKHandler(eventResult, ssrcInfo, mediaServerItem, device, channel.getChannelId(),
                        timeOutTaskKey, callback, inviteInfo, InviteSessionType.PLAY);
            }, (event) -> {
                logger.info("[点播失败] deviceId: {}, channelId:{}, {}: {}", device.getDeviceId(), channel.getChannelId(), event.statusCode, event.msg);
                dynamicTask.stop(timeOutTaskKey);
                mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
                // 释放ssrc
                mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());

                streamSession.remove(device.getDeviceId(), channel.getChannelId(), ssrcInfo.getStream());

                callback.run(event.statusCode, event.msg, null);
                inviteStreamService.call(InviteSessionType.PLAY, device.getDeviceId(), channel.getChannelId(), null,
                        InviteErrorCode.ERROR_FOR_RESET_SSRC.getCode(),
                        String.format("点播失败， 错误码： %s, %s", event.statusCode, event.msg), null);

                inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, device.getDeviceId(), channel.getChannelId());
            });
        } catch (InvalidArgumentException | SipException | ParseException e) {

            logger.error("[命令发送失败] 点播消息: {}", e.getMessage());
            dynamicTask.stop(timeOutTaskKey);
            mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
            // 释放ssrc
            mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());

            streamSession.remove(device.getDeviceId(), channel.getChannelId(), ssrcInfo.getStream());

            callback.run(InviteErrorCode.ERROR_FOR_SIP_SENDING_FAILED.getCode(),
                    InviteErrorCode.ERROR_FOR_SIP_SENDING_FAILED.getMsg(), null);
            inviteStreamService.call(InviteSessionType.PLAY, device.getDeviceId(), channel.getChannelId(), null,
                    InviteErrorCode.ERROR_FOR_SIP_SENDING_FAILED.getCode(),
                    InviteErrorCode.ERROR_FOR_SIP_SENDING_FAILED.getMsg(), null);

            inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, device.getDeviceId(), channel.getChannelId());
        }
    }

    private void tcpActiveHandler(Device device, String channelId, String contentString,
                                  MediaServer mediaServerItem,
                                  String timeOutTaskKey, SSRCInfo ssrcInfo, ErrorCallback<Object> callback){
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
            logger.info("[TCP主动连接对方] deviceId: {}, channelId: {}, 连接对方的地址：{}:{}, 收流模式：{}, SSRC: {}, SSRC校验：{}", device.getDeviceId(), channelId, sdp.getConnection().getAddress(), port, device.getStreamMode(), ssrcInfo.getSsrc(), device.isSsrcCheck());
            Boolean result = mediaServerService.connectRtpServer(mediaServerItem, sdp.getConnection().getAddress(), port, ssrcInfo.getStream());
            logger.info("[TCP主动连接对方] 结果： {}" , result);
            if (!result) {
                // 主动连接失败，结束流程， 清理数据
                dynamicTask.stop(timeOutTaskKey);
                mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
                // 释放ssrc
                mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());

                streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());

                callback.run(InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getCode(),
                        InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getMsg(), null);
                inviteStreamService.call(InviteSessionType.BROADCAST, device.getDeviceId(), channelId, null,
                        InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getCode(),
                        InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getMsg(), null);
            }
        } catch (SdpException e) {
            logger.error("[TCP主动连接对方] deviceId: {}, channelId: {}, 解析200OK的SDP信息失败", device.getDeviceId(), channelId, e);
            dynamicTask.stop(timeOutTaskKey);
            mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
            // 释放ssrc
            mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());

            streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());

            callback.run(InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getCode(),
                    InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getMsg(), null);
            inviteStreamService.call(InviteSessionType.BROADCAST, device.getDeviceId(), channelId, null,
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
        String path = "snap";
        String fileName = deviceId + "_" + channelId + ".jpg";
        // 请求截图
        logger.info("[请求截图]: " + fileName);
        mediaServerService.getSnap(mediaServerItemInuse, "rtp", stream,15, 1, path, fileName);
    }

    public StreamInfo onPublishHandlerForPlay(MediaServer mediaServerItem, MediaInfo mediaInfo, String deviceId, String channelId) {
        StreamInfo streamInfo = null;
        Device device = redisCatchStorage.getDevice(deviceId);
        streamInfo = onPublishHandler(mediaServerItem, mediaInfo, deviceId, channelId);
        if (streamInfo != null) {
            DeviceChannel deviceChannel = storager.queryChannel(deviceId, channelId);
            if (deviceChannel != null) {
                deviceChannel.setStreamId(streamInfo.getStream());
                storager.startPlay(deviceId, channelId, streamInfo.getStream());
            }
            InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, deviceId, channelId);
            if (inviteInfo != null) {
                inviteInfo.setStatus(InviteSessionStatus.ok);
                inviteInfo.setStreamInfo(streamInfo);
                inviteStreamService.updateInviteInfo(inviteInfo);
            }
        }
        return streamInfo;

    }

    private StreamInfo onPublishHandlerForPlayback(MediaServer mediaServerItem, MediaInfo mediaInfo, String deviceId, String channelId, String startTime, String endTime) {
        StreamInfo streamInfo = onPublishHandler(mediaServerItem, mediaInfo, deviceId, channelId);
        if (streamInfo != null) {
            streamInfo.setStartTime(startTime);
            streamInfo.setEndTime(endTime);
            DeviceChannel deviceChannel = storager.queryChannel(deviceId, channelId);
            if (deviceChannel != null) {
                deviceChannel.setStreamId(streamInfo.getStream());
                storager.startPlay(deviceId, channelId, streamInfo.getStream());
            }
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
            logger.warn("点播时未找到可使用的ZLM...");
        }
        return mediaServerItem;
    }

    @Override
    public void playBack(String deviceId, String channelId, String startTime,
                         String endTime, ErrorCallback<Object> callback) {
        Device device = storager.queryVideoDevice(deviceId);
        if (device == null) {
            logger.warn("[录像回放] 未找到设备 deviceId: {},channelId:{}", deviceId, channelId);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到设备：" + deviceId);
        }

        DeviceChannel channel = channelService.getOne(deviceId, channelId);
        if (channel == null) {
            logger.warn("[录像回放] 未找到通道 deviceId: {},channelId:{}", deviceId, channelId);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到通道：" + channelId);
        }

        MediaServer newMediaServerItem = getNewMediaServerItem(device);
        if (device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE") && ! newMediaServerItem.isRtpEnable()) {
            logger.warn("[录像回放] 单端口收流时不支持TCP主动方式收流 deviceId: {},channelId:{}", deviceId, channelId);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "单端口收流时不支持TCP主动方式收流");
        }
        String startTimeStr = startTime.replace("-", "")
                .replace(":", "")
                .replace(" ", "");
        String endTimeTimeStr = endTime.replace("-", "")
                .replace(":", "")
                .replace(" ", "");
        String stream = deviceId + "_" + channelId + "_" + startTimeStr + "_" + endTimeTimeStr;
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(newMediaServerItem, stream, null, device.isSsrcCheck(),  true, 0, false,  !channel.getHasAudio(),  false, device.getStreamModeForParam());
        playBack(newMediaServerItem, ssrcInfo, deviceId, channelId, startTime, endTime, callback);
    }

    @Override
    public void playBack(MediaServer mediaServerItem, SSRCInfo ssrcInfo,
                         String deviceId, String channelId, String startTime,
                         String endTime, ErrorCallback<Object> callback) {
        if (mediaServerItem == null || ssrcInfo == null) {
            callback.run(InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getCode(),
                    InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getMsg(),
                    null);
            return;
        }

        Device device = storager.queryVideoDevice(deviceId);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "设备： " + deviceId + "不存在");
        }
        logger.info("[录像回放] deviceId: {}, channelId: {}, 开始时间: {}, 结束时间： {}, 收流端口：{}, 收流模式：{}, SSRC: {}, SSRC校验：{}",
                device.getDeviceId(), channelId, startTime, endTime, ssrcInfo.getPort(), device.getStreamMode(),
                ssrcInfo.getSsrc(), device.isSsrcCheck());
        // 初始化redis中的invite消息状态
        InviteInfo inviteInfo = InviteInfo.getInviteInfo(device.getDeviceId(), channelId, ssrcInfo.getStream(), ssrcInfo,
                mediaServerItem.getSdpIp(), ssrcInfo.getPort(), device.getStreamMode(), InviteSessionType.PLAYBACK,
                InviteSessionStatus.ready);
        inviteStreamService.updateInviteInfo(inviteInfo);
        String playBackTimeOutTaskKey = UUID.randomUUID().toString();
        dynamicTask.startDelay(playBackTimeOutTaskKey, () -> {
            logger.warn("[录像回放] 超时，deviceId：{} ，channelId：{}", deviceId, channelId);
            inviteStreamService.removeInviteInfo(inviteInfo);
            callback.run(InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getCode(), InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getMsg(), null);

            try {
                cmder.streamByeCmd(device, channelId, ssrcInfo.getStream(), null);
            } catch (InvalidArgumentException | ParseException | SipException e) {
                logger.error("[录像回放] 超时 发送BYE失败 {}", e.getMessage());
            } catch (SsrcTransactionNotFoundException e) {
                // 点播超时回复BYE 同时释放ssrc以及此次点播的资源
                mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
                streamSession.remove(deviceId, channelId, ssrcInfo.getStream());
            }
        }, userSetting.getPlayTimeout());

        SipSubscribe.Event errorEvent = event -> {
            logger.info("[录像回放] 失败，{} {}", event.statusCode, event.msg);
            dynamicTask.stop(playBackTimeOutTaskKey);
            callback.run(InviteErrorCode.ERROR_FOR_SIGNALLING_ERROR.getCode(),
                    String.format("回放失败， 错误码： %s, %s", event.statusCode, event.msg), null);
            mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
            mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
            streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());
            inviteStreamService.removeInviteInfo(inviteInfo);
        };

        HookSubscribe.Event hookEvent = (hookData) -> {
            logger.info("收到回放订阅消息： " + hookData);
            dynamicTask.stop(playBackTimeOutTaskKey);
            StreamInfo streamInfo = onPublishHandlerForPlayback(hookData.getMediaServer(), hookData.getMediaInfo(), deviceId, channelId, startTime, endTime);
            if (streamInfo == null) {
                logger.warn("设备回放API调用失败！");
                callback.run(InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getCode(),
                        InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getMsg(), null);
                return;
            }
            callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
            logger.info("[录像回放] 成功 deviceId: {}, channelId: {},  开始时间: {}, 结束时间： {}", device.getDeviceId(), channelId, startTime, endTime);
        };

        try {
            cmder.playbackStreamCmd(mediaServerItem, ssrcInfo, device, channelId, startTime, endTime,
                    hookEvent, eventResult -> {
                        // 处理收到200ok后的TCP主动连接以及SSRC不一致的问题
                        InviteOKHandler(eventResult, ssrcInfo, mediaServerItem, device, channelId,
                                playBackTimeOutTaskKey, callback, inviteInfo, InviteSessionType.PLAYBACK);
                    }, errorEvent);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            logger.error("[命令发送失败] 录像回放: {}", e.getMessage());

            SipSubscribe.EventResult eventResult = new SipSubscribe.EventResult();
            eventResult.type = SipSubscribe.EventResultType.cmdSendFailEvent;
            eventResult.statusCode = -1;
            eventResult.msg = "命令发送失败";
            errorEvent.response(eventResult);
        }
    }


    private void InviteOKHandler(SipSubscribe.EventResult eventResult, SSRCInfo ssrcInfo, MediaServer mediaServerItem,
                                 Device device, String channelId, String timeOutTaskKey, ErrorCallback<Object> callback,
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
                    tcpActiveHandler(device, channelId, contentString, mediaServerItem, timeOutTaskKey, ssrcInfo, callback);
                }
            }else {
                // 单端口
                if (device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE")) {
                    logger.warn("[Invite 200OK] 单端口收流模式不支持tcp主动模式收流");
                }

            }
        }else {
            logger.info("[Invite 200OK] 收到invite 200, 发现下级自定义了ssrc: {}", ssrcInResponse);
            // ssrc 不一致
            if (mediaServerItem.isRtpEnable()) {
                // 多端口
                if (device.isSsrcCheck()) {
                    // ssrc检验
                    // 更新ssrc
                    logger.info("[Invite 200OK] SSRC修正 {}->{}", ssrcInfo.getSsrc(), ssrcInResponse);
                    // 释放ssrc
                    mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                    Boolean result = mediaServerService.updateRtpServerSSRC(mediaServerItem, ssrcInfo.getStream(), ssrcInResponse);
                    if (!result) {
                        try {
                            logger.warn("[Invite 200OK] 更新ssrc失败，停止点播 {}/{}", device.getDeviceId(), channelId);
                            cmder.streamByeCmd(device, channelId, ssrcInfo.getStream(), null, null);
                        } catch (InvalidArgumentException | SipException | ParseException | SsrcTransactionNotFoundException e) {
                            logger.error("[命令发送失败] 停止播放， 发送BYE: {}", e.getMessage());
                        }

                        dynamicTask.stop(timeOutTaskKey);
                        // 释放ssrc
                        mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());

                        streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());

                        callback.run(InviteErrorCode.ERROR_FOR_RESET_SSRC.getCode(),
                                "下级自定义了ssrc,重新设置收流信息失败", null);
                        inviteStreamService.call(inviteSessionType, device.getDeviceId(), channelId, null,
                                InviteErrorCode.ERROR_FOR_RESET_SSRC.getCode(),
                                "下级自定义了ssrc,重新设置收流信息失败", null);

                    }else {
                        ssrcInfo.setSsrc(ssrcInResponse);
                        inviteInfo.setSsrcInfo(ssrcInfo);
                        inviteInfo.setStream(ssrcInfo.getStream());
                        if (device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE")) {
                            if (mediaServerItem.isRtpEnable()) {
                                tcpActiveHandler(device, channelId, contentString, mediaServerItem, timeOutTaskKey, ssrcInfo, callback);
                            }else {
                                logger.warn("[Invite 200OK] 单端口收流模式不支持tcp主动模式收流");
                            }
                        }
                        inviteStreamService.updateInviteInfo(inviteInfo);
                    }
                }
            }else {
                if (ssrcInResponse != null) {
                    // 单端口
                    // 重新订阅流上线
                    SsrcTransaction ssrcTransaction = streamSession.getSsrcTransaction(inviteInfo.getDeviceId(),
                            inviteInfo.getChannelId(), null, inviteInfo.getStream());
                    streamSession.remove(inviteInfo.getDeviceId(),
                            inviteInfo.getChannelId(), inviteInfo.getStream());
                    inviteStreamService.updateInviteInfoForSSRC(inviteInfo, ssrcInResponse);
                    streamSession.put(device.getDeviceId(), channelId, ssrcTransaction.getCallId(),
                            inviteInfo.getStream(), ssrcInResponse, mediaServerItem.getId(), (SIPResponse) responseEvent.getResponse(), inviteSessionType);
                }
            }
        }
    }




    @Override
    public void download(String deviceId, String channelId, String startTime, String endTime, int downloadSpeed, ErrorCallback<Object> callback) {
        Device device = storager.queryVideoDevice(deviceId);
        if (device == null) {
            return;
        }
        DeviceChannel channel = channelService.getOne(deviceId, channelId);
        if (channel == null) {
            return;
        }
        MediaServer newMediaServerItem = this.getNewMediaServerItem(device);
        if (newMediaServerItem == null) {
            callback.run(InviteErrorCode.ERROR_FOR_ASSIST_NOT_READY.getCode(),
                    InviteErrorCode.ERROR_FOR_ASSIST_NOT_READY.getMsg(),
                    null);
            return;
        }
        // 录像下载不使用固定流地址，固定流地址会导致如果开始时间与结束时间一致时文件错误的叠加在一起
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(newMediaServerItem, null, null, device.isSsrcCheck(),  true, 0, false,!channel.getHasAudio(), false, device.getStreamModeForParam());
        download(newMediaServerItem, ssrcInfo, deviceId, channelId, startTime, endTime, downloadSpeed, callback);
    }


    @Override
    public void download(MediaServer mediaServerItem, SSRCInfo ssrcInfo, String deviceId, String channelId, String startTime, String endTime, int downloadSpeed, ErrorCallback<Object> callback) {
        if (mediaServerItem == null || ssrcInfo == null) {
            callback.run(InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getCode(),
                    InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getMsg(),
                    null);
            return;
        }
        Device device = storager.queryVideoDevice(deviceId);
        if (device == null) {
            callback.run(InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getCode(),
                    "设备：" + deviceId + "不存在",
                    null);
            return;
        }
        logger.info("[录像下载] deviceId: {}, channelId: {}, 下载速度：{}, 收流端口：{}, 收流模式：{}, SSRC: {}, SSRC校验：{}", device.getDeviceId(), channelId, downloadSpeed, ssrcInfo.getPort(), device.getStreamMode(), ssrcInfo.getSsrc(), device.isSsrcCheck());
        // 初始化redis中的invite消息状态
        InviteInfo inviteInfo = InviteInfo.getInviteInfo(device.getDeviceId(), channelId, ssrcInfo.getStream(), ssrcInfo,
                mediaServerItem.getSdpIp(), ssrcInfo.getPort(), device.getStreamMode(), InviteSessionType.DOWNLOAD,
                InviteSessionStatus.ready);
        inviteStreamService.updateInviteInfo(inviteInfo);
        String downLoadTimeOutTaskKey = UUID.randomUUID().toString();
        dynamicTask.startDelay(downLoadTimeOutTaskKey, () -> {
            logger.warn(String.format("录像下载请求超时，deviceId：%s ，channelId：%s", deviceId, channelId));
            inviteStreamService.removeInviteInfo(inviteInfo);
            callback.run(InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getCode(),
                    InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getMsg(), null);

            // 点播超时回复BYE 同时释放ssrc以及此次点播的资源
            try {
                cmder.streamByeCmd(device, channelId, ssrcInfo.getStream(), null);
            } catch (InvalidArgumentException | ParseException | SipException e) {
                logger.error("[录像流]录像下载请求超时， 发送BYE失败 {}", e.getMessage());
            } catch (SsrcTransactionNotFoundException e) {
                mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
                streamSession.remove(deviceId, channelId, ssrcInfo.getStream());
            }
        }, userSetting.getPlayTimeout());

        SipSubscribe.Event errorEvent = event -> {
            dynamicTask.stop(downLoadTimeOutTaskKey);
            callback.run(InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getCode(),
                    String.format("录像下载失败， 错误码： %s, %s", event.statusCode, event.msg), null);
            streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());
            inviteStreamService.removeInviteInfo(inviteInfo);
        };
        HookSubscribe.Event hookEvent = (hookData) -> {
            logger.info("[录像下载]收到订阅消息： " + hookData);
            dynamicTask.stop(downLoadTimeOutTaskKey);
            StreamInfo streamInfo = onPublishHandlerForDownload(hookData.getMediaServer(), hookData.getMediaInfo(), deviceId, channelId, startTime, endTime);
            if (streamInfo == null) {
                logger.warn("[录像下载] 获取流地址信息失败");
                callback.run(InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getCode(),
                        InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getMsg(), null);
                return;
            }
            callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
            logger.info("[录像下载] 调用成功 deviceId: {}, channelId: {},  开始时间: {}, 结束时间： {}", device.getDeviceId(), channelId, startTime, endTime);
        };
        try {
            cmder.downloadStreamCmd(mediaServerItem, ssrcInfo, device, channelId, startTime, endTime, downloadSpeed,
                    hookEvent, errorEvent, eventResult ->{
                        // 处理收到200ok后的TCP主动连接以及SSRC不一致的问题
                        InviteOKHandler(eventResult, ssrcInfo, mediaServerItem, device, channelId,
                                downLoadTimeOutTaskKey, callback, inviteInfo, InviteSessionType.DOWNLOAD);

                        // 注册录像回调事件，录像下载结束后写入下载地址
                        HookSubscribe.Event hookEventForRecord = (hookData) -> {
                            logger.info("[录像下载] 收到录像写入磁盘消息： ， {}/{}-{}",
                                    inviteInfo.getDeviceId(), inviteInfo.getChannelId(), ssrcInfo.getStream());
                            logger.info("[录像下载] 收到录像写入磁盘消息内容： " + hookData);
                            RecordInfo recordInfo = hookData.getRecordInfo();
                            String filePath = recordInfo.getFilePath();
                            DownloadFileInfo downloadFileInfo = CloudRecordUtils.getDownloadFilePath(mediaServerItem, filePath);
                            InviteInfo inviteInfoForNew = inviteStreamService.getInviteInfo(inviteInfo.getType(), inviteInfo.getDeviceId()
                                    , inviteInfo.getChannelId(), inviteInfo.getStream());
                            inviteInfoForNew.getStreamInfo().setDownLoadFilePath(downloadFileInfo);
                            inviteStreamService.updateInviteInfo(inviteInfoForNew);
                        };
                        Hook hook = Hook.getInstance(HookType.on_record_mp4, "rtp", ssrcInfo.getStream(), mediaServerItem.getId());
                        // 设置过期时间，下载失败时自动处理订阅数据
                        hook.setExpireTime(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
                        subscribe.addSubscribe(hook, hookEventForRecord);
                    });
        } catch (InvalidArgumentException | SipException | ParseException e) {
            logger.error("[命令发送失败] 录像下载: {}", e.getMessage());

            SipSubscribe.EventResult eventResult = new SipSubscribe.EventResult();
            eventResult.type = SipSubscribe.EventResultType.cmdSendFailEvent;
            eventResult.statusCode = -1;
            eventResult.msg = "命令发送失败";
            errorEvent.response(eventResult);
        }
    }

    @Override
    public StreamInfo getDownLoadInfo(String deviceId, String channelId, String stream) {
        InviteInfo inviteInfo = inviteStreamService.getInviteInfo(InviteSessionType.DOWNLOAD, deviceId, channelId, stream);
        if (inviteInfo == null || inviteInfo.getStreamInfo() == null) {
            logger.warn("[获取下载进度] 未查询到录像下载的信息");
            return null;
        }

        if (inviteInfo.getStreamInfo().getProgress() == 1) {
            return inviteInfo.getStreamInfo();
        }

        // 获取当前已下载时长
        String mediaServerId = inviteInfo.getStreamInfo().getMediaServerId();
        MediaServer mediaServerItem = mediaServerService.getOne(mediaServerId);
        if (mediaServerItem == null) {
            logger.warn("[获取下载进度] 查询录像信息时发现节点不存在");
            return null;
        }
        SsrcTransaction ssrcTransaction = streamSession.getSsrcTransaction(deviceId, channelId, null, stream);

        if (ssrcTransaction == null) {
            logger.warn("[获取下载进度] 下载已结束");
            return null;
        }
        String app = "rtp";

        MediaInfo mediaInfo = mediaServerService.getMediaInfo(mediaServerItem, app, stream);
        if (mediaInfo == null) {
            logger.warn("[获取下载进度] 查询进度失败, 节点Id： {}， {}/{}", mediaServerId, app, stream);
            return null;
        }
        if (mediaInfo.getDuration() == null || mediaInfo.getDuration() == 0) {
            inviteInfo.getStreamInfo().setProgress(0);
        } else {
            String startTime = inviteInfo.getStreamInfo().getStartTime();
            String endTime = inviteInfo.getStreamInfo().getEndTime();
            // 此时start和end单位是秒
            long start = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime);
            long end = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime);

            BigDecimal currentCount = new BigDecimal(mediaInfo.getDuration());
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

    private StreamInfo onPublishHandlerForDownload(MediaServer mediaServerItemInuse, MediaInfo mediaInfo, String deviceId, String channelId, String startTime, String endTime) {
        StreamInfo streamInfo = onPublishHandler(mediaServerItemInuse, mediaInfo, deviceId, channelId);
        if (streamInfo != null) {
            streamInfo.setProgress(0);
            streamInfo.setStartTime(startTime);
            streamInfo.setEndTime(endTime);
            InviteInfo inviteInfo = inviteStreamService.getInviteInfo(InviteSessionType.DOWNLOAD, deviceId, channelId, streamInfo.getStream());
            if (inviteInfo != null) {
                logger.info("[录像下载] 更新invite消息中的stream信息");
                inviteInfo.setStatus(InviteSessionStatus.ok);
                inviteInfo.setStreamInfo(streamInfo);
                inviteStreamService.updateInviteInfo(inviteInfo);
            }
        }
        return streamInfo;
    }


    public StreamInfo onPublishHandler(MediaServer mediaServerItem, MediaInfo mediaInfo, String deviceId, String channelId) {
        StreamInfo streamInfo = mediaServerService.getStreamInfoByAppAndStream(mediaServerItem, "rtp", mediaInfo.getStream(), mediaInfo, null);
        streamInfo.setDeviceID(deviceId);
        streamInfo.setChannelId(channelId);
        return streamInfo;
    }


    @Override
    public void zlmServerOffline(String mediaServerId) {
        // 处理正在向上推流的上级平台
        List<SendRtpItem> sendRtpItems = redisCatchStorage.querySendRTPServer(null);
        if (sendRtpItems.size() > 0) {
            for (SendRtpItem sendRtpItem : sendRtpItems) {
                if (sendRtpItem.getMediaServerId().equals(mediaServerId)) {
                    ParentPlatform platform = storager.queryParentPlatByServerGBId(sendRtpItem.getPlatformId());
                    try {
                        sipCommanderFroPlatform.streamByeCmd(platform, sendRtpItem.getCallId());
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        logger.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
                    }
                }
            }
        }
        // 处理正在观看的国标设备
        List<SsrcTransaction> allSsrc = streamSession.getAllSsrc();
        if (allSsrc.size() > 0) {
            for (SsrcTransaction ssrcTransaction : allSsrc) {
                if (ssrcTransaction.getMediaServerId().equals(mediaServerId)) {
                    Device device = deviceService.getDevice(ssrcTransaction.getDeviceId());
                    if (device == null) {
                        continue;
                    }
                    try {
                        cmder.streamByeCmd(device, ssrcTransaction.getChannelId(),
                                ssrcTransaction.getStream(), null);
                    } catch (InvalidArgumentException | ParseException | SipException |
                             SsrcTransactionNotFoundException e) {
                        logger.error("[zlm离线]为正在使用此zlm的设备， 发送BYE失败 {}", e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public AudioBroadcastResult audioBroadcast(Device device, String channelId, Boolean broadcastMode) {
        // TODO 必须多端口模式才支持语音喊话鹤语音对讲
        if (device == null || channelId == null) {
            return null;
        }
        logger.info("[语音喊话] device： {}, channel: {}", device.getDeviceId(), channelId);
        DeviceChannel deviceChannel = storager.queryChannel(device.getDeviceId(), channelId);
        if (deviceChannel == null) {
            logger.warn("开启语音广播的时候未找到通道： {}", channelId);
            return null;
        }
        MediaServer mediaServerItem = mediaServerService.getMediaServerForMinimumLoad(null);
        if (broadcastMode == null) {
            broadcastMode = true;
        }
        String app = broadcastMode?"broadcast":"talk";
        String stream = device.getDeviceId() + "_" + channelId;
        AudioBroadcastResult audioBroadcastResult = new AudioBroadcastResult();
        audioBroadcastResult.setApp(app);
        audioBroadcastResult.setStream(stream);
        audioBroadcastResult.setStreamInfo(new StreamContent(mediaServerService.getStreamInfoByAppAndStream(mediaServerItem, app, stream, null, null, null, false)));
        audioBroadcastResult.setCodec("G.711");
        return audioBroadcastResult;
    }

    @Override
    public boolean audioBroadcastCmd(Device device, String channelId, MediaServer mediaServerItem, String app, String stream, int timeout, boolean isFromPlatform, AudioBroadcastEvent event) throws InvalidArgumentException, ParseException, SipException {
        if (device == null || channelId == null) {
            return false;
        }
        logger.info("[语音喊话] device： {}, channel: {}", device.getDeviceId(), channelId);
        DeviceChannel deviceChannel = storager.queryChannel(device.getDeviceId(), channelId);
        if (deviceChannel == null) {
            logger.warn("开启语音广播的时候未找到通道： {}", channelId);
            event.call("开启语音广播的时候未找到通道");
            return false;
        }
        // 查询通道使用状态
        if (audioBroadcastManager.exit(device.getDeviceId(), channelId)) {
            SendRtpItem sendRtpItem = redisCatchStorage.querySendRTPServer(device.getDeviceId(), channelId, null, null);
            if (sendRtpItem != null && sendRtpItem.isOnlyAudio()) {
                // 查询流是否存在，不存在则认为是异常状态
                Boolean streamReady = mediaServerService.isStreamReady(mediaServerItem, sendRtpItem.getApp(), sendRtpItem.getStream());
                if (streamReady) {
                    logger.warn("语音广播已经开启： {}", channelId);
                    event.call("语音广播已经开启");
                    return false;
                } else {
                    stopAudioBroadcast(device.getDeviceId(), channelId);
                }
            }
        }

        // 发送通知
        cmder.audioBroadcastCmd(device, channelId, eventResultForOk -> {
            // 发送成功
            AudioBroadcastCatch audioBroadcastCatch = new AudioBroadcastCatch(device.getDeviceId(), channelId, mediaServerItem, app, stream, event, AudioBroadcastCatchStatus.Ready, isFromPlatform);
            audioBroadcastManager.update(audioBroadcastCatch);
            // 等待invite消息， 超时则结束
            String key = VideoManagerConstants.BROADCAST_WAITE_INVITE +  device.getDeviceId();
            if (!SipUtils.isFrontEnd(device.getDeviceId())) {
                key += audioBroadcastCatch.getChannelId();
            }
            dynamicTask.startDelay(key, ()->{
                logger.info("[语音广播]等待invite消息超时：{}/{}", device.getDeviceId(), channelId);
                stopAudioBroadcast(device.getDeviceId(), channelId);
            }, 10*1000);
        }, eventResultForError -> {
            // 发送失败
            logger.error("语音广播发送失败： {}:{}", channelId, eventResultForError.msg);
            event.call("语音广播发送失败");
            stopAudioBroadcast(device.getDeviceId(), channelId);
        });
        return true;
    }

    @Override
    public boolean audioBroadcastInUse(Device device, String channelId) {
        if (audioBroadcastManager.exit(device.getDeviceId(), channelId)) {
            SendRtpItem sendRtpItem = redisCatchStorage.querySendRTPServer(device.getDeviceId(), channelId, null, null);
            if (sendRtpItem != null && sendRtpItem.isOnlyAudio()) {
                // 查询流是否存在，不存在则认为是异常状态
                MediaServer mediaServerServiceOne = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                Boolean streamReady = mediaServerService.isStreamReady(mediaServerServiceOne, sendRtpItem.getApp(), sendRtpItem.getStream());
                if (streamReady) {
                    logger.warn("语音广播通道使用中： {}", channelId);
                    return true;
                }
            }
        }
        return false;
    }


    @Override
    public void stopAudioBroadcast(String deviceId, String channelId) {
        logger.info("[停止对讲] 设备：{}, 通道：{}", deviceId, channelId);
        List<AudioBroadcastCatch> audioBroadcastCatchList = new ArrayList<>();
        if (channelId == null) {
            audioBroadcastCatchList.addAll(audioBroadcastManager.get(deviceId));
        } else {
            audioBroadcastCatchList.add(audioBroadcastManager.get(deviceId, channelId));
        }
        if (audioBroadcastCatchList.size() > 0) {
            for (AudioBroadcastCatch audioBroadcastCatch : audioBroadcastCatchList) {
                Device device = deviceService.getDevice(deviceId);
                if (device == null || audioBroadcastCatch == null) {
                    return;
                }
                SendRtpItem sendRtpItem = redisCatchStorage.querySendRTPServer(deviceId, audioBroadcastCatch.getChannelId(), null, null);
                if (sendRtpItem != null) {
                    redisCatchStorage.deleteSendRTPServer(deviceId, sendRtpItem.getChannelId(), null, null);
                    MediaServer mediaServer = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                    mediaServerService.stopSendRtp(mediaServer, sendRtpItem.getApp(), sendRtpItem.getStream(), null);
                    try {
                        cmder.streamByeCmdForDeviceInvite(device, sendRtpItem.getChannelId(), audioBroadcastCatch.getSipTransactionInfo(), null);
                    } catch (InvalidArgumentException | ParseException | SipException |
                             SsrcTransactionNotFoundException e) {
                        logger.error("[消息发送失败] 发送语音喊话BYE失败");
                    }
                }

                audioBroadcastManager.del(deviceId, channelId);
            }
        }
    }


    @Override
    public void zlmServerOnline(String mediaServerId) {
        // TODO 查找之前的点播，流如果不存在则给下级发送bye
//        MediaServerItem mediaServerItem = mediaServerService.getOne(mediaServerId);
//        zlmresTfulUtils.getMediaList(mediaServerItem, (mediaList ->{
//            Integer code = mediaList.getInteger("code");
//            if (code == 0) {
//                JSONArray data = mediaList.getJSONArray("data");
//                if (data == null || data.size() == 0) {
//                    zlmServerOffline(mediaServerId);
//                }else {
//                    Map<String, JSONObject> mediaListMap = new HashMap<>();
//                    for (int i = 0; i < data.size(); i++) {
//                        JSONObject json = data.getJSONObject(i);
//                        String app = json.getString("app");
//                        if ("rtp".equals(app)) {
//                            String stream = json.getString("stream");
//                            if (mediaListMap.get(stream) != null) {
//                                continue;
//                            }
//                            mediaListMap.put(stream, json);
//                            // 处理正在观看的国标设备
//                            List<SsrcTransaction> ssrcTransactions = streamSession.getSsrcTransactionForAll(null, null, null, stream);
//                            if (ssrcTransactions.size() > 0) {
//                                for (SsrcTransaction ssrcTransaction : ssrcTransactions) {
//                                    if(ssrcTransaction.getMediaServerId().equals(mediaServerId)) {
//                                        cmder.streamByeCmd(ssrcTransaction.getDeviceId(), ssrcTransaction.getChannelId(),
//                                                ssrcTransaction.getStream(), null);
//                                    }
//                                }
//                            }
//                        }
//                    }
//                    if (mediaListMap.size() > 0 ) {
//                        // 处理正在向上推流的上级平台
//                        List<SendRtpItem> sendRtpItems = redisCatchStorage.querySendRTPServer(null);
//                        if (sendRtpItems.size() > 0) {
//                            for (SendRtpItem sendRtpItem : sendRtpItems) {
//                                if (sendRtpItem.getMediaServerId().equals(mediaServerId)) {
//                                    if (mediaListMap.get(sendRtpItem.getStreamId()) == null) {
//                                        ParentPlatform platform = storager.queryPlatformByServerGBId(sendRtpItem.getPlatformId());
//                                        sipCommanderFroPlatform.streamByeCmd(platform, sendRtpItem.getCallId());
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }));
    }

    @Override
    public void pauseRtp(String streamId) throws ServiceException, InvalidArgumentException, ParseException, SipException {
        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(InviteSessionType.PLAYBACK, streamId);
        if (null == inviteInfo || inviteInfo.getStreamInfo() == null) {
            logger.warn("streamId不存在!");
            throw new ServiceException("streamId不存在");
        }
        inviteInfo.getStreamInfo().setPause(true);
        inviteStreamService.updateInviteInfo(inviteInfo);
        MediaServer mediaServerItem = mediaServerService.getOne(inviteInfo.getStreamInfo().getMediaServerId());
        if (null == mediaServerItem) {
            logger.warn("mediaServer 不存在!");
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
        Device device = storager.queryVideoDevice(inviteInfo.getDeviceId());
        cmder.playPauseCmd(device, inviteInfo.getStreamInfo());
    }

    @Override
    public void resumeRtp(String streamId) throws ServiceException, InvalidArgumentException, ParseException, SipException {
        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(InviteSessionType.PLAYBACK, streamId);
        if (null == inviteInfo || inviteInfo.getStreamInfo() == null) {
            logger.warn("streamId不存在!");
            throw new ServiceException("streamId不存在");
        }
        inviteInfo.getStreamInfo().setPause(false);
        inviteStreamService.updateInviteInfo(inviteInfo);
        MediaServer mediaServerItem = mediaServerService.getOne(inviteInfo.getStreamInfo().getMediaServerId());
        if (null == mediaServerItem) {
            logger.warn("mediaServer 不存在!");
            throw new ServiceException("mediaServer不存在");
        }
        // zlm 暂停RTP超时检查
        // 使用zlm中的流ID
        String streamKey = inviteInfo.getStream();
        if (!mediaServerItem.isRtpEnable()) {
            streamKey = Long.toHexString(Long.parseLong(inviteInfo.getSsrcInfo().getSsrc())).toUpperCase();
        }
        boolean result = mediaServerService.resumeRtpCheck(mediaServerItem, streamKey);
        if (!result) {
            throw new ServiceException("继续RTP接收失败");
        }
        Device device = storager.queryVideoDevice(inviteInfo.getDeviceId());
        cmder.playResumeCmd(device, inviteInfo.getStreamInfo());
    }

    @Override
    public void startPushStream(SendRtpItem sendRtpItem, SIPResponse sipResponse, ParentPlatform platform, CallIdHeader callIdHeader) {
        // 开始发流
        MediaServer mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());

        if (mediaInfo != null) {
            try {
                if (sendRtpItem.isTcpActive()) {
                    mediaServerService.startSendRtpPassive(mediaInfo, platform, sendRtpItem, null);
                } else {
                    mediaServerService.startSendRtp(mediaInfo, platform, sendRtpItem);
                }
            }catch (ControllerException e) {
                logger.error("RTP推流失败: {}", e.getMessage());
                startSendRtpStreamFailHand(sendRtpItem, platform, callIdHeader);
                return;
            }

            logger.info("RTP推流成功[ {}/{} ]，{}, ", sendRtpItem.getApp(), sendRtpItem.getStream(),
                    sendRtpItem.isTcpActive()?"被动发流": sendRtpItem.getIp() + ":" + sendRtpItem.getPort());

        }
    }

    @Override
    public void startSendRtpStreamFailHand(SendRtpItem sendRtpItem, ParentPlatform platform, CallIdHeader callIdHeader) {
        if (sendRtpItem.isOnlyAudio()) {
            Device device = deviceService.getDevice(sendRtpItem.getDeviceId());
            AudioBroadcastCatch audioBroadcastCatch = audioBroadcastManager.get(sendRtpItem.getDeviceId(), sendRtpItem.getChannelId());
            if (audioBroadcastCatch != null) {
                try {
                    cmder.streamByeCmd(device, sendRtpItem.getChannelId(), audioBroadcastCatch.getSipTransactionInfo(), null);
                } catch (SipException | ParseException | InvalidArgumentException |
                         SsrcTransactionNotFoundException exception) {
                    logger.error("[命令发送失败] 停止语音对讲: {}", exception.getMessage());
                }
            }
        } else {
            if (platform != null) {
                // 向上级平台
                try {
                    commanderForPlatform.streamByeCmd(platform, callIdHeader.getCallId());
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    logger.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
                }
            }

        }
    }

    @Override
    public void talkCmd(Device device, String channelId, MediaServer mediaServerItem, String stream, AudioBroadcastEvent event) {
        if (device == null || channelId == null) {
            return;
        }
        // TODO 必须多端口模式才支持语音喊话鹤语音对讲
        logger.info("[语音对讲] device： {}, channel: {}", device.getDeviceId(), channelId);
        DeviceChannel deviceChannel = storager.queryChannel(device.getDeviceId(), channelId);
        if (deviceChannel == null) {
            logger.warn("开启语音对讲的时候未找到通道： {}", channelId);
            event.call("开启语音对讲的时候未找到通道");
            return;
        }
        // 查询通道使用状态
        if (audioBroadcastManager.exit(device.getDeviceId(), channelId)) {
            SendRtpItem sendRtpItem = redisCatchStorage.querySendRTPServer(device.getDeviceId(), channelId, null, null);
            if (sendRtpItem != null && sendRtpItem.isOnlyAudio()) {
                // 查询流是否存在，不存在则认为是异常状态
                MediaServer mediaServer = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                Boolean streamReady = mediaServerService.isStreamReady(mediaServer, sendRtpItem.getApp(), sendRtpItem.getStream());
                if (streamReady) {
                    logger.warn("[语音对讲] 正在语音广播，无法开启语音通话： {}", channelId);
                    event.call("正在语音广播");
                    return;
                } else {
                    stopAudioBroadcast(device.getDeviceId(), channelId);
                }
            }
        }

        SendRtpItem sendRtpItem = redisCatchStorage.querySendRTPServer(device.getDeviceId(), channelId, stream, null);
        if (sendRtpItem != null) {
            MediaServer mediaServer = mediaServerService.getOne(sendRtpItem.getMediaServerId());
            Boolean streamReady = mediaServerService.isStreamReady(mediaServer, "rtp", sendRtpItem.getReceiveStream());
            if (streamReady) {
                logger.warn("[语音对讲] 进行中： {}", channelId);
                event.call("语音对讲进行中");
                return;
            } else {
                stopTalk(device, channelId);
            }
        }

        talk(mediaServerItem, device, channelId, stream, (hookData) -> {
            logger.info("[语音对讲] 收到设备发来的流");
        }, eventResult -> {
            logger.warn("[语音对讲] 失败，{}/{}, 错误码 {} {}", device.getDeviceId(), channelId, eventResult.statusCode, eventResult.msg);
            event.call("失败，错误码 " + eventResult.statusCode + ", " + eventResult.msg);
        }, () -> {
            logger.warn("[语音对讲] 失败，{}/{} 超时", device.getDeviceId(), channelId);
            event.call("失败，超时 ");
            stopTalk(device, channelId);
        }, errorMsg -> {
            logger.warn("[语音对讲] 失败，{}/{} {}", device.getDeviceId(), channelId, errorMsg);
            event.call(errorMsg);
            stopTalk(device, channelId);
        });
    }

    private void stopTalk(Device device, String channelId) {
        stopTalk(device, channelId, null);
    }

    @Override
    public void stopTalk(Device device, String channelId, Boolean streamIsReady) {
        logger.info("[语音对讲] 停止， {}/{}", device.getDeviceId(), channelId);
        SendRtpItem sendRtpItem = redisCatchStorage.querySendRTPServer(device.getDeviceId(), channelId, null, null);
        if (sendRtpItem == null) {
            logger.info("[语音对讲] 停止失败， 未找到发送信息，可能已经停止");
            return;
        }
        // 停止向设备推流
        String mediaServerId = sendRtpItem.getMediaServerId();
        if (mediaServerId == null) {
            return;
        }

        MediaServer mediaServer = mediaServerService.getOne(mediaServerId);

        if (streamIsReady == null || streamIsReady) {
            mediaServerService.stopSendRtp(mediaServer, sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getSsrc());
        }

        ssrcFactory.releaseSsrc(mediaServerId, sendRtpItem.getSsrc());

        SsrcTransaction ssrcTransaction = streamSession.getSsrcTransaction(device.getDeviceId(), channelId, null, sendRtpItem.getStream());
        if (ssrcTransaction != null) {
            try {
                cmder.streamByeCmd(device, channelId, sendRtpItem.getStream(), null);
            } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException  e) {
                logger.info("[语音对讲] 停止消息发送失败，可能已经停止");
            }
        }
        redisCatchStorage.deleteSendRTPServer(device.getDeviceId(), channelId,null, null);
    }

    @Override
    public void getSnap(String deviceId, String channelId, String fileName, ErrorCallback errorCallback) {
        Device device = deviceService.getDevice(deviceId);
        if (device == null) {
            errorCallback.run(InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getCode(), InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getMsg(), null);
            return;
        }
        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, deviceId, channelId);
        if (inviteInfo != null) {
            if (inviteInfo.getStreamInfo() != null) {
                // 已存在线直接截图
                MediaServer mediaServerItemInuse = mediaServerService.getOne(inviteInfo.getStreamInfo().getMediaServerId());
                String path = "snap";
                // 请求截图
                logger.info("[请求截图]: " + fileName);
                mediaServerService.getSnap(mediaServerItemInuse, "rtp", inviteInfo.getStreamInfo().getStream(), 15, 1, path, fileName);
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
                InviteInfo inviteInfoForPlay = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, deviceId, channelId);
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
    public void stopPlay(Device device, String channelId) {
        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, device.getDeviceId(), channelId);
        if (inviteInfo == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "点播未找到");
        }
        if (InviteSessionStatus.ok == inviteInfo.getStatus()) {
            try {
                logger.info("[停止点播] {}/{}", device.getDeviceId(), channelId);
                cmder.streamByeCmd(device, channelId, inviteInfo.getStream(), null, null);
            } catch (InvalidArgumentException | SipException | ParseException | SsrcTransactionNotFoundException e) {
                logger.error("[命令发送失败] 停止点播， 发送BYE: {}", e.getMessage());
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
            }
        }
        inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, device.getDeviceId(), channelId);
        storager.stopPlay(device.getDeviceId(), channelId);
        channelService.stopPlay(device.getDeviceId(), channelId);
        if (inviteInfo.getStreamInfo() != null) {
            mediaServerService.closeRTPServer(inviteInfo.getStreamInfo().getMediaServerId(), inviteInfo.getStream());
        }
    }
}
