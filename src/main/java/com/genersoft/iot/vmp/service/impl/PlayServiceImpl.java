package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionStatus;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.exception.ServiceException;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.media.zlm.*;
import com.genersoft.iot.vmp.media.zlm.AssistRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.ZLMServerFactory;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeFactory;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForStreamChange;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.hook.HookParam;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;
import com.genersoft.iot.vmp.service.*;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.service.bean.RequestPushStreamMsg;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.service.redisMsg.RedisGbPlayMsgListener;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.AudioBroadcastResult;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.gb28181.play.bean.AudioBroadcastEvent;
import gov.nist.javax.sip.message.SIPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
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
import java.util.*;

@SuppressWarnings(value = {"rawtypes", "unchecked"})
@Service
public class PlayServiceImpl implements IPlayService {

    private final static Logger logger = LoggerFactory.getLogger(PlayServiceImpl.class);

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private AudioBroadcastManager audioBroadcastManager;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private ISIPCommanderForPlatform sipCommanderFroPlatform;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private ZLMServerFactory zlmServerFactory;

    @Autowired
    private IInviteStreamService inviteStreamService;

    @Autowired
    private SendRtpPortManager sendRtpPortManager;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private ZLMServerFactory zlmserverfactory;

    @Autowired
    private AssistRESTfulUtils assistRESTfulUtils;

    @Autowired
    private IMediaService mediaService;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private VideoStreamSessionManager streamSession;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private SipConfig sipConfig;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private ZlmHttpHookSubscribe subscribe;

    @Autowired
    private ISIPCommanderForPlatform commanderForPlatform;


    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private RedisGbPlayMsgListener redisGbPlayMsgListener;

    @Autowired
    private ZlmHttpHookSubscribe hookSubscribe;

    @Autowired
    private SSRCFactory ssrcFactory;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;


    @Override
    public SSRCInfo play(MediaServerItem mediaServerItem, String deviceId, String channelId, ErrorCallback<Object> callback) {
        if (mediaServerItem == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到可用的zlm");
        }

        Device device = redisCatchStorage.getDevice(deviceId);
        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, deviceId, channelId);
        if (inviteInfo != null ) {
            if (inviteInfo.getStreamInfo() == null) {
                // 点播发起了但是尚未成功, 仅注册回调等待结果即可
                inviteStreamService.once(InviteSessionType.PLAY, deviceId, channelId, null, callback);
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
                MediaServerItem mediaInfo = mediaServerService.getOne(mediaServerId);

                Boolean ready = zlmserverfactory.isStreamReady(mediaInfo, "rtp", streamId);
                if (ready != null && ready) {
                    callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
                    inviteStreamService.call(InviteSessionType.PLAY, device.getDeviceId(), channelId, null,
                            InviteErrorCode.SUCCESS.getCode(),
                            InviteErrorCode.SUCCESS.getMsg(),
                            streamInfo);
                    return inviteInfo.getSsrcInfo();
                }else {
                    // 点播发起了但是尚未成功, 仅注册回调等待结果即可
                    inviteStreamService.once(InviteSessionType.PLAY, deviceId, channelId, null, callback);
                    storager.stopPlay(streamInfo.getDeviceID(), streamInfo.getChannelId());
                    inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, deviceId, channelId);
                }
            }
        }
        String streamId = null;
        if (mediaServerItem.isRtpEnable()) {
            streamId = String.format("%s_%s", device.getDeviceId(), channelId);
        }
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(mediaServerItem, streamId, null, device.isSsrcCheck(),  false, 0, false, false,device.getStreamModeForParam());
        if (ssrcInfo == null) {
            callback.run(InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(), InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getMsg(), null);
            inviteStreamService.call(InviteSessionType.PLAY, device.getDeviceId(), channelId, null,
                    InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(),
                    InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getMsg(),
                    null);
            return null;
        }
        // TODO 记录点播的状态
        play(mediaServerItem, ssrcInfo, device, channelId, callback);
        return ssrcInfo;
    }

    private void talk(MediaServerItem mediaServerItem, Device device, String channelId, String stream,
                      ZlmHttpHookSubscribe.Event hookEvent, SipSubscribe.Event errorEvent,
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
        int port = sendRtpPortManager.getNextPort(mediaServerItem.getId());
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

        Map<String, Object> param = new HashMap<>(12);
        param.put("vhost","__defaultVhost__");
        param.put("app", sendRtpItem.getApp());
        param.put("stream", sendRtpItem.getStream());
        param.put("ssrc", sendRtpItem.getSsrc());
        param.put("src_port", sendRtpItem.getLocalPort());
        param.put("pt", sendRtpItem.getPt());
        param.put("use_ps", sendRtpItem.isUsePs() ? "1" : "0");
        param.put("only_audio", sendRtpItem.isOnlyAudio() ? "1" : "0");
        param.put("is_udp", sendRtpItem.isTcp() ? "0" : "1");
        param.put("recv_stream_id", sendRtpItem.getReceiveStream());
        param.put("close_delay_ms", userSetting.getPlayTimeout() * 1000);

        zlmServerFactory.startSendRtpPassive(mediaServerItem, param, jsonObject -> {
            if (jsonObject == null || jsonObject.getInteger("code") != 0 ) {
                mediaServerService.releaseSsrc(mediaServerItem.getId(), sendRtpItem.getSsrc());
                logger.info("[语音对讲]失败 deviceId: {}, channelId: {}", device.getDeviceId(), channelId);
                audioEvent.call("失败, " + jsonObject.getString("msg"));
                // 查看是否已经建立了通道，存在则发送bye
                stopTalk(device, channelId);
            }
        });


        // 查看设备是否已经在推流
        try {
            cmder.talkStreamCmd(mediaServerItem, sendRtpItem, device, channelId, callId, (mediaServerItemInuse, hookParam) -> {
                logger.info("[语音对讲] 流已生成， 开始推流： " + hookParam);
                dynamicTask.stop(timeOutTaskKey);
                // TODO 暂不做处理
            }, (mediaServerItemInuse, hookParam) -> {
                logger.info("[语音对讲] 设备开始推流： " + hookParam);
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
    public void play(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo, Device device, String channelId,
                     ErrorCallback<Object> callback) {

        if (mediaServerItem == null || ssrcInfo == null) {
            callback.run(InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getCode(),
                    InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getMsg(),
                    null);
            return;
        }
        logger.info("[点播开始] deviceId: {}, channelId: {},码流类型：{}, 收流端口： {}, 收流模式：{}, SSRC: {}, SSRC校验：{}",
                device.getDeviceId(), channelId, device.isSwitchPrimarySubStream() ? "辅码流" : "主码流", ssrcInfo.getPort(),
                device.getStreamMode(), ssrcInfo.getSsrc(), device.isSsrcCheck());
        //端口获取失败的ssrcInfo 没有必要发送点播指令
        if (ssrcInfo.getPort() <= 0) {
            logger.info("[点播端口分配异常]，deviceId={},channelId={},ssrcInfo={}", device.getDeviceId(), channelId, ssrcInfo);
            // 释放ssrc
            mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
            streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());

            callback.run(InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(), "点播端口分配异常", null);
            inviteStreamService.call(InviteSessionType.PLAY, device.getDeviceId(), channelId, null,
                    InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(), "点播端口分配异常", null);
            return;
        }

        // 初始化redis中的invite消息状态
        InviteInfo inviteInfo = InviteInfo.getInviteInfo(device.getDeviceId(), channelId, ssrcInfo.getStream(), ssrcInfo,
                mediaServerItem.getSdpIp(), ssrcInfo.getPort(), device.getStreamMode(), InviteSessionType.PLAY,
                InviteSessionStatus.ready);
        inviteInfo.setSubStream(device.isSwitchPrimarySubStream());
        inviteStreamService.updateInviteInfo(inviteInfo);
        // 超时处理
        String timeOutTaskKey = UUID.randomUUID().toString();
        dynamicTask.startDelay(timeOutTaskKey, () -> {
            // 执行超时任务时查询是否已经成功，成功了则不执行超时任务，防止超时任务取消失败的情况
            InviteInfo inviteInfoForTimeOut = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, device.getDeviceId(), channelId);
            if (inviteInfoForTimeOut == null || inviteInfoForTimeOut.getStreamInfo() == null) {
                logger.info("[点播超时] 收流超时 deviceId: {}, channelId: {},码流类型：{}，端口：{}, SSRC: {}",
                        device.getDeviceId(), channelId, device.isSwitchPrimarySubStream() ? "辅码流" : "主码流",
                        ssrcInfo.getPort(), ssrcInfo.getSsrc());

                // 点播超时回复BYE 同时释放ssrc以及此次点播的资源
//                InviteInfo inviteInfoForTimeout = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.play, device.getDeviceId(), channelId);
//                if (inviteInfoForTimeout == null) {
//                    return;
//                }
//                if (InviteSessionStatus.ok == inviteInfoForTimeout.getStatus() ) {
//                    // TODO 发送bye
//                }else {
//                    // TODO 发送cancel
//                }
                callback.run(InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getCode(), InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getMsg(), null);
                inviteStreamService.call(InviteSessionType.PLAY, device.getDeviceId(), channelId, null,
                        InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getCode(), InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getMsg(), null);
                inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, device.getDeviceId(), channelId);

                try {
                    cmder.streamByeCmd(device, channelId, ssrcInfo.getStream(), null);
                } catch (InvalidArgumentException | ParseException | SipException |
                         SsrcTransactionNotFoundException e) {
                    logger.error("[点播超时]， 发送BYE失败 {}", e.getMessage());
                } finally {
                    mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                    mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
                    streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());
                    mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
                    // 取消订阅消息监听
                    HookSubscribeForStreamChange hookSubscribe = HookSubscribeFactory.on_stream_changed("rtp", ssrcInfo.getStream(), true, "rtsp", mediaServerItem.getId());
                    subscribe.removeSubscribe(hookSubscribe);
                }
            }
        }, userSetting.getPlayTimeout());

        try {
            cmder.playStreamCmd(mediaServerItem, ssrcInfo, device, channelId, (mediaServerItemInuse, hookParam ) -> {
                logger.info("收到订阅消息： " + hookParam);
                dynamicTask.stop(timeOutTaskKey);
                // hook响应
                StreamInfo streamInfo = onPublishHandlerForPlay(mediaServerItemInuse, hookParam, device.getDeviceId(), channelId);
                if (streamInfo == null){
                    callback.run(InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getCode(),
                            InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getMsg(), null);
                    inviteStreamService.call(InviteSessionType.PLAY, device.getDeviceId(), channelId, null,
                            InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getCode(),
                            InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getMsg(), null);
                    return;
                }
                callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
                inviteStreamService.call(InviteSessionType.PLAY, device.getDeviceId(), channelId, null,
                        InviteErrorCode.SUCCESS.getCode(),
                        InviteErrorCode.SUCCESS.getMsg(),
                        streamInfo);
                logger.info("[点播成功] deviceId: {}, channelId:{}, 码流类型：{}", device.getDeviceId(),
                        device.isSwitchPrimarySubStream() ? "辅码流" : "主码流");
                String streamUrl;
                if (mediaServerItemInuse.getRtspPort() != 0) {
                    streamUrl = String.format("rtsp://127.0.0.1:%s/%s/%s", mediaServerItemInuse.getRtspPort(), "rtp",  ssrcInfo.getStream());
                }else {
                    streamUrl = String.format("http://127.0.0.1:%s/%s/%s.live.mp4", mediaServerItemInuse.getHttpPort(), "rtp",  ssrcInfo.getStream());
                }
                String path = "snap";
                String fileName = device.getDeviceId() + "_" + channelId + ".jpg";
                // 请求截图
                logger.info("[请求截图]: " + fileName);
                zlmresTfulUtils.getSnap(mediaServerItemInuse, streamUrl, 15, 1, path, fileName);

            }, (event) -> {
                inviteInfo.setStatus(InviteSessionStatus.ok);

                ResponseEvent responseEvent = (ResponseEvent) event.event;
                String contentString = new String(responseEvent.getResponse().getRawContent());
                // 获取ssrc
                int ssrcIndex = contentString.indexOf("y=");
                // 检查是否有y字段
                if (ssrcIndex >= 0) {
                    //ssrc规定长度为10字节，不取余下长度以避免后续还有“f=”字段 TODO 后续对不规范的非10位ssrc兼容
                    String ssrcInResponse = contentString.substring(ssrcIndex + 2, ssrcIndex + 12).trim();
                    // 查询到ssrc不一致且开启了ssrc校验则需要针对处理
                    if (ssrcInfo.getSsrc().equals(ssrcInResponse)) {
                        if (device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE")) {
                            String substring = contentString.substring(0, contentString.indexOf("y="));
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
                                logger.info("[点播-TCP主动连接对方] deviceId: {}, channelId: {}, 连接对方的地址：{}:{}, 收流模式：{}, SSRC: {}, SSRC校验：{}", device.getDeviceId(), channelId, sdp.getConnection().getAddress(), port, device.getStreamMode(), ssrcInfo.getSsrc(), device.isSsrcCheck());
                                JSONObject jsonObject = zlmresTfulUtils.connectRtpServer(mediaServerItem, sdp.getConnection().getAddress(), port, ssrcInfo.getStream());
                                logger.info("[点播-TCP主动连接对方] 结果： {}", jsonObject);
                            } catch (SdpException e) {
                                logger.error("[点播-TCP主动连接对方] deviceId: {}, channelId: {}, 解析200OK的SDP信息失败", device.getDeviceId(), channelId, e);
                                dynamicTask.stop(timeOutTaskKey);
                                mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
                                // 释放ssrc
                                mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());

                                streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());

                                callback.run(InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getCode(),
                                        InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getMsg(), null);
                                inviteStreamService.call(InviteSessionType.PLAY, device.getDeviceId(), channelId, null,
                                        InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getCode(),
                                        InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getMsg(), null);
                            }
                        }
                        return;
                    }
                    logger.info("[点播消息] 收到invite 200, 发现下级自定义了ssrc: {}", ssrcInResponse);
                    if (!mediaServerItem.isRtpEnable() || device.isSsrcCheck()) {
                        logger.info("[点播消息] SSRC修正 {}->{}", ssrcInfo.getSsrc(), ssrcInResponse);
                        // 释放ssrc
                        mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                        // 单端口模式streamId也有变化，重新设置监听即可
                        if (!mediaServerItem.isRtpEnable()) {
                            // 添加订阅
                            HookSubscribeForStreamChange hookSubscribe = HookSubscribeFactory.on_stream_changed("rtp", ssrcInfo.getStream(), true, "rtsp", mediaServerItem.getId());
                            subscribe.removeSubscribe(hookSubscribe);
                            String stream = String.format("%08x", Integer.parseInt(ssrcInResponse)).toUpperCase();
                            hookSubscribe.getContent().put("stream", stream);
                            inviteStreamService.updateInviteInfoForStream(inviteInfo, stream);
                            subscribe.addSubscribe(hookSubscribe, (mediaServerItemInUse, hookParam) -> {
                                logger.info("[ZLM HOOK] ssrc修正后收到订阅消息： " + hookParam);
                                dynamicTask.stop(timeOutTaskKey);
                                // hook响应
                                StreamInfo streamInfo = onPublishHandlerForPlay(mediaServerItemInUse, hookParam, device.getDeviceId(), channelId);
                                if (streamInfo == null){
                                    callback.run(InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getCode(),
                                            InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getMsg(), null);
                                    inviteStreamService.call(InviteSessionType.PLAY, device.getDeviceId(), channelId, null,
                                            InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getCode(),
                                            InviteErrorCode.ERROR_FOR_STREAM_PARSING_EXCEPTIONS.getMsg(), null);
                                    return;
                                }
                                callback.run(InviteErrorCode.SUCCESS.getCode(),
                                        InviteErrorCode.SUCCESS.getMsg(), streamInfo);
                                inviteStreamService.call(InviteSessionType.PLAY, device.getDeviceId(), channelId, null,
                                        InviteErrorCode.SUCCESS.getCode(),
                                        InviteErrorCode.SUCCESS.getMsg(),
                                        streamInfo);
                            });
                            return;
                        }

                        // 更新ssrc
                        Boolean result = mediaServerService.updateRtpServerSSRC(mediaServerItem, ssrcInfo.getStream(), ssrcInResponse);
                        if (!result) {
                            try {
                                logger.warn("[点播] 更新ssrc失败，停止点播 {}/{}", device.getDeviceId(), channelId);
                                cmder.streamByeCmd(device, channelId, ssrcInfo.getStream(), null, null);
                            } catch (InvalidArgumentException | SipException | ParseException | SsrcTransactionNotFoundException e) {
                                logger.error("[命令发送失败] 停止点播， 发送BYE: {}", e.getMessage());
                            }

                            dynamicTask.stop(timeOutTaskKey);
                            // 释放ssrc
                            mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());

                            streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());

                            callback.run(InviteErrorCode.ERROR_FOR_RESET_SSRC.getCode(),
                                    "下级自定义了ssrc,重新设置收流信息失败", null);
                            inviteStreamService.call(InviteSessionType.PLAY, device.getDeviceId(), channelId, null,
                                    InviteErrorCode.ERROR_FOR_RESET_SSRC.getCode(),
                                    "下级自定义了ssrc,重新设置收流信息失败", null);

                        }else {
                            if (ssrcInfo.getStream()!= null && !ssrcInfo.getStream().equals(inviteInfo.getStream())) {
                                inviteStreamService.removeInviteInfo(inviteInfo);
                            }
                            ssrcInfo.setSsrc(ssrcInResponse);
                            inviteInfo.setSsrcInfo(ssrcInfo);
                            inviteInfo.setStream(ssrcInfo.getStream());
                        }
                    }else {
                        logger.info("[点播消息] 收到invite 200, 下级自定义了ssrc, 但是当前模式无需修正");
                    }
                }
                inviteStreamService.updateInviteInfo(inviteInfo);
            }, (event) -> {
                dynamicTask.stop(timeOutTaskKey);
                mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
                // 释放ssrc
                mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());

                streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());

                callback.run(InviteErrorCode.ERROR_FOR_SIGNALLING_ERROR.getCode(),
                        String.format("点播失败， 错误码： %s, %s", event.statusCode, event.msg), null);
                inviteStreamService.call(InviteSessionType.PLAY, device.getDeviceId(), channelId, null,
                        InviteErrorCode.ERROR_FOR_RESET_SSRC.getCode(),
                        String.format("点播失败， 错误码： %s, %s", event.statusCode, event.msg), null);

                inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, device.getDeviceId(), channelId);
            });
        } catch (InvalidArgumentException | SipException | ParseException e) {

            logger.error("[命令发送失败] 点播消息: {}", e.getMessage());
            dynamicTask.stop(timeOutTaskKey);
            mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
            // 释放ssrc
            mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());

            streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());

            callback.run(InviteErrorCode.ERROR_FOR_SIP_SENDING_FAILED.getCode(),
                    InviteErrorCode.ERROR_FOR_SIP_SENDING_FAILED.getMsg(), null);
            inviteStreamService.call(InviteSessionType.PLAY, device.getDeviceId(), channelId, null,
                    InviteErrorCode.ERROR_FOR_SIP_SENDING_FAILED.getCode(),
                    InviteErrorCode.ERROR_FOR_SIP_SENDING_FAILED.getMsg(), null);

            inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, device.getDeviceId(), channelId);
        }
    }

    @Override
    public StreamInfo onPublishHandlerForPlay(MediaServerItem mediaServerItem, HookParam hookParam, String deviceId, String channelId) {
        OnStreamChangedHookParam streamChangedHookParam = (OnStreamChangedHookParam) hookParam;
        StreamInfo streamInfo = onPublishHandler(mediaServerItem, streamChangedHookParam, deviceId, channelId);
        Device device = redisCatchStorage.getDevice(deviceId);
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

    private StreamInfo onPublishHandlerForPlayback(MediaServerItem mediaServerItem, HookParam param, String deviceId, String channelId, String startTime, String endTime) {
        OnStreamChangedHookParam streamChangedHookParam = (OnStreamChangedHookParam) param;
        StreamInfo streamInfo = onPublishHandler(mediaServerItem, streamChangedHookParam, deviceId, channelId);
        if (streamInfo != null) {
            streamInfo.setStartTime(startTime);
            streamInfo.setEndTime(endTime);
            DeviceChannel deviceChannel = storager.queryChannel(deviceId, channelId);
            if (deviceChannel != null) {
                deviceChannel.setStreamId(streamInfo.getStream());
                storager.startPlay(deviceId, channelId, streamInfo.getStream());
            }
            InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAYBACK, deviceId, channelId);
            if (inviteInfo != null) {
                inviteInfo.setStatus(InviteSessionStatus.ok);

                inviteInfo.setStreamInfo(streamInfo);
                inviteStreamService.updateInviteInfo(inviteInfo);
            }

        }
        return streamInfo;
    }

    @Override
    public MediaServerItem getNewMediaServerItem(Device device) {
        if (device == null) {
            return null;
        }
        MediaServerItem mediaServerItem;
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
    public MediaServerItem getNewMediaServerItemHasAssist(Device device) {
        if (device == null) {
            return null;
        }
        MediaServerItem mediaServerItem;
        if (ObjectUtils.isEmpty(device.getMediaServerId()) || "auto".equals(device.getMediaServerId())) {
            mediaServerItem = mediaServerService.getMediaServerForMinimumLoad(true);
        } else {
            mediaServerItem = mediaServerService.getOne(device.getMediaServerId());
        }
        if (mediaServerItem == null) {
            logger.warn("[获取可用的ZLM节点]未找到可使用的ZLM...");
        }
        return mediaServerItem;
    }

    @Override
    public void playBack(String deviceId, String channelId, String startTime,
                                                          String endTime, ErrorCallback<Object> callback) {
        Device device = storager.queryVideoDevice(deviceId);
        if (device == null) {
            return;
        }
        MediaServerItem newMediaServerItem = getNewMediaServerItem(device);
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(newMediaServerItem, null, null, device.isSsrcCheck(),  true, 0, false,false, device.getStreamModeForParam());
        playBack(newMediaServerItem, ssrcInfo, deviceId, channelId, startTime, endTime, callback);
    }

    @Override
    public void playBack(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo,
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

        ZlmHttpHookSubscribe.Event hookEvent = (mediaServerItemInuse, hookParam) -> {
            logger.info("收到回放订阅消息： " + hookParam);
            dynamicTask.stop(playBackTimeOutTaskKey);
            StreamInfo streamInfo = onPublishHandlerForPlayback(mediaServerItemInuse, hookParam, deviceId, channelId, startTime, endTime);
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
                        inviteInfo.setStatus(InviteSessionStatus.ok);
                        ResponseEvent responseEvent = (ResponseEvent) eventResult.event;
                        String contentString = new String(responseEvent.getResponse().getRawContent());
                        // 获取ssrc
                        int ssrcIndex = contentString.indexOf("y=");
                        // 检查是否有y字段
                        if (ssrcIndex >= 0) {
                            //ssrc规定长度为10字节，不取余下长度以避免后续还有“f=”字段 TODO 后续对不规范的非10位ssrc兼容
                            String ssrcInResponse = contentString.substring(ssrcIndex + 2, ssrcIndex + 12);
                            // 查询到ssrc不一致且开启了ssrc校验则需要针对处理
                            if (ssrcInfo.getSsrc().equals(ssrcInResponse)) {
                                if (device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE")) {
                                    String substring = contentString.substring(0, contentString.indexOf("y="));
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
                                        logger.info("[录像回放-TCP主动连接对方] deviceId: {}, channelId: {}, 连接对方的地址：{}:{}, 收流模式：{}, SSRC: {}, SSRC校验：{}", device.getDeviceId(), channelId, sdp.getConnection().getAddress(), port, device.getStreamMode(), ssrcInfo.getSsrc(), device.isSsrcCheck());
                                        JSONObject jsonObject = zlmresTfulUtils.connectRtpServer(mediaServerItem, sdp.getConnection().getAddress(), port, ssrcInfo.getStream());
                                        logger.info("[录像回放-TCP主动连接对方] 结果： {}", jsonObject);
                                    } catch (SdpException e) {
                                        logger.error("[录像回放-TCP主动连接对方] deviceId: {}, channelId: {}, 解析200OK的SDP信息失败", device.getDeviceId(), channelId, e);
                                        dynamicTask.stop(playBackTimeOutTaskKey);
                                        mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
                                        // 释放ssrc
                                        mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());

                                        streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());

                                        callback.run(InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getCode(),
                                                InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getMsg(), null);
                                        inviteStreamService.call(InviteSessionType.PLAY, device.getDeviceId(), channelId, null,
                                                InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getCode(),
                                                InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getMsg(), null);
                                    }
                                }
                                return;
                            }
                            logger.info("[录像回放] 收到invite 200, 发现下级自定义了ssrc: {}", ssrcInResponse);
                            if (!mediaServerItem.isRtpEnable() || device.isSsrcCheck()) {
                                logger.info("[录像回放] SSRC修正 {}->{}", ssrcInfo.getSsrc(), ssrcInResponse);

                                // 释放ssrc
                                mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());

                                // 单端口模式streamId也有变化，需要重新设置监听
                                if (!mediaServerItem.isRtpEnable()) {
                                    // 添加订阅
                                    HookSubscribeForStreamChange hookSubscribe = HookSubscribeFactory.on_stream_changed("rtp", ssrcInfo.getStream(), true, "rtsp", mediaServerItem.getId());
                                    subscribe.removeSubscribe(hookSubscribe);
                                    String stream = String.format("%08x", Integer.parseInt(ssrcInResponse)).toUpperCase();
                                    hookSubscribe.getContent().put("stream", stream);
                                    inviteStreamService.updateInviteInfoForStream(inviteInfo, stream);
                                    subscribe.addSubscribe(hookSubscribe, (mediaServerItemInUse, hookParam) -> {
                                        logger.info("[ZLM HOOK] ssrc修正后收到订阅消息： " + hookParam);
                                        dynamicTask.stop(playBackTimeOutTaskKey);
                                        // hook响应
                                        hookEvent.response(mediaServerItemInUse, hookParam);
                                    });
                                }
                                // 更新ssrc
                                Boolean result = mediaServerService.updateRtpServerSSRC(mediaServerItem, ssrcInfo.getStream(), ssrcInResponse);
                                if (!result) {
                                    try {
                                        logger.warn("[录像回放] 更新ssrc失败，停止录像回放 {}/{}", device.getDeviceId(), channelId);
                                        cmder.streamByeCmd(device, channelId, ssrcInfo.getStream(), null, null);
                                    } catch (InvalidArgumentException | SipException | ParseException | SsrcTransactionNotFoundException e) {
                                        logger.error("[命令发送失败] 停止点播， 发送BYE: {}", e.getMessage());

                                    }

                                    dynamicTask.stop(playBackTimeOutTaskKey);
                                    // 释放ssrc
                                    mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());

                                    streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());

                                    callback.run(InviteErrorCode.ERROR_FOR_RESET_SSRC.getCode(),
                                            "下级自定义了ssrc,重新设置收流信息失败", null);

                                }else {
                                    if (ssrcInfo.getStream()!= null && !ssrcInfo.getStream().equals(inviteInfo.getStream())) {
                                        inviteStreamService.removeInviteInfo(inviteInfo);
                                    }

                                    ssrcInfo.setSsrc(ssrcInResponse);
                                    inviteInfo.setSsrcInfo(ssrcInfo);
                                    inviteInfo.setStream(ssrcInfo.getStream());
                                }
                            }else {
                                logger.info("[点播消息] 收到invite 200, 下级自定义了ssrc, 但是当前模式无需修正");
                            }
                        }
                        inviteStreamService.updateInviteInfo(inviteInfo);
                    }, errorEvent);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            logger.error("[命令发送失败] 回放: {}", e.getMessage());

            SipSubscribe.EventResult eventResult = new SipSubscribe.EventResult();
            eventResult.type = SipSubscribe.EventResultType.cmdSendFailEvent;
            eventResult.statusCode = -1;
            eventResult.msg = "命令发送失败";
            errorEvent.response(eventResult);
        }
    }


    @Override
    public void download(String deviceId, String channelId, String startTime, String endTime, int downloadSpeed, ErrorCallback<Object> callback) {
        Device device = storager.queryVideoDevice(deviceId);
        if (device == null) {
            return;
        }
        MediaServerItem newMediaServerItem = getNewMediaServerItemHasAssist(device);
        if (newMediaServerItem == null) {
            callback.run(InviteErrorCode.ERROR_FOR_ASSIST_NOT_READY.getCode(),
                    InviteErrorCode.ERROR_FOR_ASSIST_NOT_READY.getMsg(),
                    null);
            return;
        }
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(newMediaServerItem, null, null, device.isSsrcCheck(),  true, 0, false,false, device.getStreamModeForParam());
        download(newMediaServerItem, ssrcInfo, deviceId, channelId, startTime, endTime, downloadSpeed, callback);
    }


    @Override
    public void download(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo, String deviceId, String channelId, String startTime, String endTime, int downloadSpeed, ErrorCallback<Object> callback) {
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
        ZlmHttpHookSubscribe.Event hookEvent = (mediaServerItemInuse, hookParam) -> {
            logger.info("[录像下载]收到订阅消息： " + hookParam);
            dynamicTask.stop(downLoadTimeOutTaskKey);
            StreamInfo streamInfo = onPublishHandlerForDownload(mediaServerItemInuse, hookParam, deviceId, channelId, startTime, endTime);
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
                        inviteInfo.setStatus(InviteSessionStatus.ok);
                        ResponseEvent responseEvent = (ResponseEvent) eventResult.event;
                        String contentString = new String(responseEvent.getResponse().getRawContent());
                        // 获取ssrc
                        int ssrcIndex = contentString.indexOf("y=");
                        // 检查是否有y字段
                        if (ssrcIndex >= 0) {
                            //ssrc规定长度为10字节，不取余下长度以避免后续还有“f=”字段 TODO 后续对不规范的非10位ssrc兼容
                            String ssrcInResponse = contentString.substring(ssrcIndex + 2, ssrcIndex + 12);
                            // 查询到ssrc不一致且开启了ssrc校验则需要针对处理
                            if (ssrcInfo.getSsrc().equals(ssrcInResponse)) {
                                if (device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE")) {
                                    String substring = contentString.substring(0, contentString.indexOf("y="));
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
                                        logger.info("[录像下载-TCP主动连接对方] deviceId: {}, channelId: {}, 连接对方的地址：{}:{}, 收流模式：{}, SSRC: {}, SSRC校验：{}", device.getDeviceId(), channelId, sdp.getConnection().getAddress(), port, device.getStreamMode(), ssrcInfo.getSsrc(), device.isSsrcCheck());
                                        JSONObject jsonObject = zlmresTfulUtils.connectRtpServer(mediaServerItem, sdp.getConnection().getAddress(), port, ssrcInfo.getStream());
                                        logger.info("[录像下载-TCP主动连接对方] 结果： {}", jsonObject);
                                    } catch (SdpException e) {
                                        logger.error("[录像下载-TCP主动连接对方] deviceId: {}, channelId: {}, 解析200OK的SDP信息失败", device.getDeviceId(), channelId, e);
                                        dynamicTask.stop(downLoadTimeOutTaskKey);
                                        mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
                                        // 释放ssrc
                                        mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());

                                        streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());

                                        callback.run(InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getCode(),
                                                InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getMsg(), null);
                                        inviteStreamService.call(InviteSessionType.PLAY, device.getDeviceId(), channelId, null,
                                                InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getCode(),
                                                InviteErrorCode.ERROR_FOR_SDP_PARSING_EXCEPTIONS.getMsg(), null);
                                    }
                                }
                                return;
                            }
                            logger.info("[录像下载] 收到invite 200, 发现下级自定义了ssrc: {}", ssrcInResponse);
                            if (!mediaServerItem.isRtpEnable() || device.isSsrcCheck()) {
                                logger.info("[录像下载] SSRC修正 {}->{}", ssrcInfo.getSsrc(), ssrcInResponse);

                                // 释放ssrc
                                mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());

                                // 单端口模式streamId也有变化，需要重新设置监听
                                if (!mediaServerItem.isRtpEnable()) {
                                    // 添加订阅
                                    HookSubscribeForStreamChange hookSubscribe = HookSubscribeFactory.on_stream_changed("rtp", ssrcInfo.getStream(), true, "rtsp", mediaServerItem.getId());
                                    subscribe.removeSubscribe(hookSubscribe);
                                    String stream = String.format("%08x", Integer.parseInt(ssrcInResponse)).toUpperCase();
                                    hookSubscribe.getContent().put("stream", stream);
                                    inviteStreamService.updateInviteInfoForStream(inviteInfo, stream);
                                    subscribe.addSubscribe(hookSubscribe, (mediaServerItemInUse, hookParam) -> {
                                        logger.info("[ZLM HOOK] ssrc修正后收到订阅消息： " + hookParam);
                                        dynamicTask.stop(downLoadTimeOutTaskKey);
                                        hookEvent.response(mediaServerItemInUse, hookParam);
                                    });
                                }

                                // 更新ssrc
                                Boolean result = mediaServerService.updateRtpServerSSRC(mediaServerItem, ssrcInfo.getStream(), ssrcInResponse);
                                if (!result) {
                                    try {
                                        logger.warn("[录像下载] 更新ssrc失败，停止录像回放 {}/{}", device.getDeviceId(), channelId);
                                        cmder.streamByeCmd(device, channelId, ssrcInfo.getStream(), null, null);
                                    } catch (InvalidArgumentException | SipException | ParseException | SsrcTransactionNotFoundException e) {
                                        logger.error("[命令发送失败] 停止点播， 发送BYE: {}", e.getMessage());
                                    }

                                    dynamicTask.stop(downLoadTimeOutTaskKey);
                                    // 释放ssrc
                                    mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());

                                    streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());

                                    callback.run(InviteErrorCode.ERROR_FOR_RESET_SSRC.getCode(),
                                            "下级自定义了ssrc,重新设置收流信息失败", null);

                                }else {
                                    if (ssrcInfo.getStream()!= null && !ssrcInfo.getStream().equals(inviteInfo.getStream())) {
                                        inviteStreamService.removeInviteInfo(inviteInfo);
                                    }
                                    ssrcInfo.setSsrc(ssrcInResponse);
                                    inviteInfo.setSsrcInfo(ssrcInfo);
                                    inviteInfo.setStream(ssrcInfo.getStream());
                                }
                            }else {
                                logger.info("[录像下载] 收到invite 200, 下级自定义了ssrc, 但是当前模式无需修正");
                            }
                        }
                        inviteStreamService.updateInviteInfo(inviteInfo);
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

        if (inviteInfo != null && inviteInfo.getStreamInfo() != null) {
            if (inviteInfo.getStreamInfo().getProgress() == 1) {
                return inviteInfo.getStreamInfo();
            }

            // 获取当前已下载时长
            String mediaServerId = inviteInfo.getStreamInfo().getMediaServerId();
            MediaServerItem mediaServerItem = mediaServerService.getOne(mediaServerId);
            if (mediaServerItem == null) {
                logger.warn("查询录像信息时发现节点已离线");
                return null;
            }
            if (mediaServerItem.getRecordAssistPort() > 0) {
                JSONObject jsonObject = assistRESTfulUtils.fileDuration(mediaServerItem, inviteInfo.getStreamInfo().getApp(), inviteInfo.getStreamInfo().getStream(), null);
                if (jsonObject == null) {
                    throw new ControllerException(ErrorCode.ERROR100.getCode(), "连接Assist服务失败");
                }
                if (jsonObject.getInteger("code") == 0) {
                    long duration = jsonObject.getLong("data");

                    if (duration == 0) {
                        inviteInfo.getStreamInfo().setProgress(0);
                    } else {
                        String startTime = inviteInfo.getStreamInfo().getStartTime();
                        String endTime = inviteInfo.getStreamInfo().getEndTime();
                        long start = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime);
                        long end = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime);

                        BigDecimal currentCount = new BigDecimal(duration / 1000);
                        BigDecimal totalCount = new BigDecimal(end - start);
                        BigDecimal divide = currentCount.divide(totalCount, 2, RoundingMode.HALF_UP);
                        double process = divide.doubleValue();
                        inviteInfo.getStreamInfo().setProgress(process);
                    }
                    inviteStreamService.updateInviteInfo(inviteInfo);
                }
            }
            return inviteInfo.getStreamInfo();
        }
        return null;
    }

    private StreamInfo onPublishHandlerForDownload(MediaServerItem mediaServerItemInuse, HookParam hookParam, String deviceId, String channelId, String startTime, String endTime) {
        OnStreamChangedHookParam streamChangedHookParam = (OnStreamChangedHookParam) hookParam;
        StreamInfo streamInfo = onPublishHandler(mediaServerItemInuse, streamChangedHookParam, deviceId, channelId);
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


    public StreamInfo onPublishHandler(MediaServerItem mediaServerItem, OnStreamChangedHookParam hookParam, String deviceId, String channelId) {
        StreamInfo streamInfo = mediaService.getStreamInfoByAppAndStream(mediaServerItem, "rtp", hookParam.getStream(), hookParam.getTracks(), null);
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
        MediaServerItem mediaServerItem = mediaServerService.getMediaServerForMinimumLoad(null);
        if (broadcastMode == null) {
            broadcastMode = true;
        }
        String app = broadcastMode?"broadcast":"talk";
        String stream = device.getDeviceId() + "_" + channelId;
        AudioBroadcastResult audioBroadcastResult = new AudioBroadcastResult();
        audioBroadcastResult.setApp(app);
        audioBroadcastResult.setStream(stream);
        audioBroadcastResult.setStreamInfo(new StreamContent(mediaService.getStreamInfoByAppAndStream(mediaServerItem, app, stream, null, null, null, false)));
        audioBroadcastResult.setCodec("G.711");
        return audioBroadcastResult;
    }

    @Override
    public boolean audioBroadcastCmd(Device device, String channelId, MediaServerItem mediaServerItem, String app, String stream, int timeout, boolean isFromPlatform, AudioBroadcastEvent event) throws InvalidArgumentException, ParseException, SipException {
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
                Boolean streamReady = zlmServerFactory.isStreamReady(mediaServerItem, sendRtpItem.getApp(), sendRtpItem.getStream());
                if (streamReady) {
                    logger.warn("语音广播已经开启： {}", channelId);
                    event.call("语音广播已经开启");
                    return false;
                } else {
                    stopAudioBroadcast(device.getDeviceId(), channelId);
                }
            }
        }
        SendRtpItem sendRtpItem = redisCatchStorage.querySendRTPServer(device.getDeviceId(), channelId, null, null);
        if (sendRtpItem != null) {
            MediaServerItem mediaServer = mediaServerService.getOne(sendRtpItem.getMediaServerId());
            Boolean streamReady = zlmServerFactory.isStreamReady(mediaServer, "rtp", sendRtpItem.getReceiveStream());
            if (streamReady) {
                logger.warn("[语音对讲] 进行中： {}", channelId);
                event.call("语音对讲进行中");
                return false;
            } else {
                stopTalk(device, channelId);
            }
        }

        // 发送通知
        cmder.audioBroadcastCmd(device, channelId, eventResultForOk -> {
            // 发送成功
            AudioBroadcastCatch audioBroadcastCatch = new AudioBroadcastCatch(device.getDeviceId(), channelId, mediaServerItem, app, stream, event, AudioBroadcastCatchStatus.Ready, isFromPlatform);
            audioBroadcastManager.update(audioBroadcastCatch);
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
                MediaServerItem mediaServerServiceOne = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                Boolean streamReady = zlmServerFactory.isStreamReady(mediaServerServiceOne, sendRtpItem.getApp(), sendRtpItem.getStream());
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
                    MediaServerItem mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                    Map<String, Object> param = new HashMap<>();
                    param.put("vhost", "__defaultVhost__");
                    param.put("app", sendRtpItem.getApp());
                    param.put("stream", sendRtpItem.getStream());
                    zlmresTfulUtils.stopSendRtp(mediaInfo, param);
                    try {
                        cmder.streamByeCmd(device, sendRtpItem.getChannelId(), audioBroadcastCatch.getSipTransactionInfo(), null);
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
        MediaServerItem mediaServerItem = mediaServerService.getOne(inviteInfo.getStreamInfo().getMediaServerId());
        if (null == mediaServerItem) {
            logger.warn("mediaServer 不存在!");
            throw new ServiceException("mediaServer不存在");
        }
        // zlm 暂停RTP超时检查
        JSONObject jsonObject = zlmresTfulUtils.pauseRtpCheck(mediaServerItem, streamId);
        if (jsonObject == null || jsonObject.getInteger("code") != 0) {
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
        MediaServerItem mediaServerItem = mediaServerService.getOne(inviteInfo.getStreamInfo().getMediaServerId());
        if (null == mediaServerItem) {
            logger.warn("mediaServer 不存在!");
            throw new ServiceException("mediaServer不存在");
        }
        // zlm 暂停RTP超时检查
        JSONObject jsonObject = zlmresTfulUtils.resumeRtpCheck(mediaServerItem, streamId);
        if (jsonObject == null || jsonObject.getInteger("code") != 0) {
            throw new ServiceException("继续RTP接收失败");
        }
        Device device = storager.queryVideoDevice(inviteInfo.getDeviceId());
        cmder.playResumeCmd(device, inviteInfo.getStreamInfo());
    }

    @Override
    public void startPushStream(SendRtpItem sendRtpItem, SIPResponse sipResponse, ParentPlatform platform, CallIdHeader callIdHeader) {
        // 开始发流
        String is_Udp = sendRtpItem.isTcp() ? "0" : "1";
        MediaServerItem mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
        logger.info("[开始推流] rtp/{}, 目标={}:{}，SSRC={}, RTCP={}", sendRtpItem.getStream(),
                sendRtpItem.getIp(), sendRtpItem.getPort(), sendRtpItem.getSsrc(), sendRtpItem.isRtcp());
        Map<String, Object> param = new HashMap<>(12);
        param.put("vhost", "__defaultVhost__");
        param.put("app", sendRtpItem.getApp());
        param.put("stream", sendRtpItem.getStream());
        param.put("ssrc", sendRtpItem.getSsrc());
        param.put("src_port", sendRtpItem.getLocalPort());
        param.put("pt", sendRtpItem.getPt());
        param.put("use_ps", sendRtpItem.isUsePs() ? "1" : "0");
        param.put("only_audio", sendRtpItem.isOnlyAudio() ? "1" : "0");
        param.put("is_udp", is_Udp);
        if (!sendRtpItem.isTcp()) {
            // udp模式下开启rtcp保活
            param.put("udp_rtcp_timeout", sendRtpItem.isRtcp() ? "1" : "0");
        }

        if (mediaInfo == null) {
            RequestPushStreamMsg requestPushStreamMsg = RequestPushStreamMsg.getInstance(
                    sendRtpItem.getMediaServerId(), sendRtpItem.getApp(), sendRtpItem.getStream(),
                    sendRtpItem.getIp(), sendRtpItem.getPort(), sendRtpItem.getSsrc(), sendRtpItem.isTcp(),
                    sendRtpItem.getLocalPort(), sendRtpItem.getPt(), sendRtpItem.isUsePs(), sendRtpItem.isOnlyAudio());
            redisGbPlayMsgListener.sendMsgForStartSendRtpStream(sendRtpItem.getServerId(), requestPushStreamMsg, json -> {
                startSendRtpStreamHand(sendRtpItem, platform, json, param, callIdHeader);
            });
        } else {
            // 如果是严格模式，需要关闭端口占用
            JSONObject startSendRtpStreamResult = null;
            if (sendRtpItem.getLocalPort() != 0) {
                if (sendRtpItem.isTcpActive()) {
                    startSendRtpStreamResult = zlmServerFactory.startSendRtpPassive(mediaInfo, param);
                } else {
                    param.put("dst_url", sendRtpItem.getIp());
                    param.put("dst_port", sendRtpItem.getPort());
                    startSendRtpStreamResult = zlmServerFactory.startSendRtpStream(mediaInfo, param);
                }
            } else {
                if (sendRtpItem.isTcpActive()) {
                    startSendRtpStreamResult = zlmServerFactory.startSendRtpPassive(mediaInfo, param);
                } else {
                    param.put("dst_url", sendRtpItem.getIp());
                    param.put("dst_port", sendRtpItem.getPort());
                    startSendRtpStreamResult = zlmServerFactory.startSendRtpStream(mediaInfo, param);
                }
            }
            if (startSendRtpStreamResult != null) {
                startSendRtpStreamHand(sendRtpItem, platform, startSendRtpStreamResult, param, callIdHeader);
            }
        }
    }

    @Override
    public void startSendRtpStreamHand(SendRtpItem sendRtpItem, Object correlationInfo,
                                       JSONObject jsonObject, Map<String, Object> param, CallIdHeader callIdHeader) {
        if (jsonObject == null) {
            logger.error("RTP推流失败: 请检查ZLM服务");
        } else if (jsonObject.getInteger("code") == 0) {
            logger.info("调用ZLM推流接口, 结果： {}", jsonObject);
            logger.info("RTP推流成功[ {}/{} ]，{}->{}, ", param.get("app"), param.get("stream"), jsonObject.getString("local_port"),
                    sendRtpItem.isTcpActive()?"被动发流": param.get("dst_url") + ":" + param.get("dst_port"));
        } else {
            logger.error("RTP推流失败: {}, 参数：{}", jsonObject.getString("msg"), JSONObject.toJSONString(param));
            if (sendRtpItem.isOnlyAudio()) {
                Device device = deviceService.getDevice(sendRtpItem.getDeviceId());
                AudioBroadcastCatch audioBroadcastCatch = audioBroadcastManager.get(sendRtpItem.getDeviceId(), sendRtpItem.getChannelId());
                if (audioBroadcastCatch != null) {
                    try {
                        cmder.streamByeCmd(device, sendRtpItem.getChannelId(), audioBroadcastCatch.getSipTransactionInfo(), null);
                    } catch (SipException | ParseException | InvalidArgumentException |
                             SsrcTransactionNotFoundException e) {
                        logger.error("[命令发送失败] 停止语音对讲: {}", e.getMessage());
                    }
                }
            } else {
                // 向上级平台
                if (correlationInfo instanceof ParentPlatform) {
                    try {
                        ParentPlatform parentPlatform = (ParentPlatform)correlationInfo;
                        commanderForPlatform.streamByeCmd(parentPlatform, callIdHeader.getCallId());
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        logger.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public void talkCmd(Device device, String channelId, MediaServerItem mediaServerItem, String stream, AudioBroadcastEvent event) {
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
                MediaServerItem mediaServer = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                Boolean streamReady = zlmServerFactory.isStreamReady(mediaServer, sendRtpItem.getApp(), sendRtpItem.getStream());
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
            MediaServerItem mediaServer = mediaServerService.getOne(sendRtpItem.getMediaServerId());
            Boolean streamReady = zlmServerFactory.isStreamReady(mediaServer, "rtp", sendRtpItem.getReceiveStream());
            if (streamReady) {
                logger.warn("[语音对讲] 进行中： {}", channelId);
                event.call("语音对讲进行中");
                return;
            } else {
                stopTalk(device, channelId);
            }
        }

        talk(mediaServerItem, device, channelId, stream, (mediaServerItem1, hookParam) -> {
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

        MediaServerItem mediaServer = mediaServerService.getOne(mediaServerId);

        if (streamIsReady == null || streamIsReady) {
            Map<String, Object> param = new HashMap<>();
            param.put("vhost", "__defaultVhost__");
            param.put("app", sendRtpItem.getApp());
            param.put("stream", sendRtpItem.getStream());
            param.put("ssrc", sendRtpItem.getSsrc());
            zlmServerFactory.stopSendRtpStream(mediaServer, param);
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
                MediaServerItem mediaServerItemInuse = mediaServerService.getOne(inviteInfo.getStreamInfo().getMediaServerId());
                String streamUrl;
                if (mediaServerItemInuse.getRtspPort() != 0) {
                    streamUrl = String.format("rtsp://127.0.0.1:%s/%s/%s", mediaServerItemInuse.getRtspPort(), "rtp",  inviteInfo.getStreamInfo().getStream());
                }else {
                    streamUrl = String.format("http://127.0.0.1:%s/%s/%s.live.mp4", mediaServerItemInuse.getHttpPort(), "rtp",  inviteInfo.getStreamInfo().getStream());
                }
                String path = "snap";
                // 请求截图
                logger.info("[请求截图]: " + fileName);
                zlmresTfulUtils.getSnap(mediaServerItemInuse, streamUrl, 15, 1, path, fileName);
                File snapFile = new File(path + File.separator + fileName);
                if (snapFile.exists()) {
                    errorCallback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), snapFile.getAbsoluteFile());
                }else {
                    errorCallback.run(InviteErrorCode.FAIL.getCode(), InviteErrorCode.FAIL.getMsg(), null);
                }
                return;
            }
        }

        MediaServerItem newMediaServerItem = getNewMediaServerItem(device);
        play(newMediaServerItem, deviceId, channelId, (code, msg, data)->{
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

}
