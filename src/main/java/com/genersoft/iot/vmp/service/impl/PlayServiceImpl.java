package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionStatus;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.exception.ServiceException;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
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
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import gov.nist.javax.sip.message.SIPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.sdp.*;
import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

@SuppressWarnings(value = {"rawtypes", "unchecked"})
@Service
public class PlayServiceImpl implements IPlayService {

    private final static Logger logger = LoggerFactory.getLogger(PlayServiceImpl.class);

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private SIPCommanderFroPlatform sipCommanderFroPlatform;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IInviteStreamService inviteStreamService;

    @Autowired
    private DeferredResultHolder resultHolder;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private ZLMServerFactory zlmServerFactory;

    @Autowired
    private AssistRESTfulUtils assistRESTfulUtils;

    @Autowired
    private IMediaService mediaService;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private VideoStreamSessionManager streamSession;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private ZlmHttpHookSubscribe subscribe;


    @Override
    public SSRCInfo play(MediaServerItem mediaServerItem, String deviceId, String channelId, String ssrc, ErrorCallback<Object> callback) {
        if (mediaServerItem == null) {
            logger.warn("[点播] 未找到可用的zlm deviceId: {},channelId:{}", deviceId, channelId);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到可用的zlm");
        }

        Device device = redisCatchStorage.getDevice(deviceId);
        if (device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE") && !mediaServerItem.isRtpEnable()) {
            logger.warn("[点播] 单端口收流时不支持TCP主动方式收流 deviceId: {},channelId:{}", deviceId, channelId);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "单端口收流时不支持TCP主动方式收流");
        }
        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, deviceId, channelId);
        if (inviteInfo != null ) {
            if (inviteInfo.getStreamInfo() == null) {
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
                MediaServerItem mediaInfo = mediaServerService.getOne(mediaServerId);

                Boolean ready = zlmServerFactory.isStreamReady(mediaInfo, "rtp", streamId);
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
        String streamId = String.format("%s_%s", device.getDeviceId(), channelId);;
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(mediaServerItem, streamId, ssrc, device.isSsrcCheck(),  false, 0, false, device.getStreamModeForParam());
        if (ssrcInfo == null) {
            callback.run(InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(), InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getMsg(), null);
            inviteStreamService.call(InviteSessionType.PLAY, device.getDeviceId(), channelId, null,
                    InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(),
                    InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getMsg(),
                    null);
            return null;
        }
        play(mediaServerItem, ssrcInfo, device, channelId, callback);
        return ssrcInfo;
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
        logger.info("[点播开始] deviceId: {}, channelId: {},码流类型：{}, 收流端口： {}, STREAM：{}, 收流模式：{}, SSRC: {}, SSRC校验：{}",
                device.getDeviceId(), channelId, device.isSwitchPrimarySubStream() ? "辅码流" : "主码流", ssrcInfo.getPort(), ssrcInfo.getStream(),
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

                callback.run(InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getCode(), InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getMsg(), null);
                inviteStreamService.call(InviteSessionType.PLAY, device.getDeviceId(), channelId, null,
                        InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getCode(), InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getMsg(), null);
                inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, device.getDeviceId(), channelId);

                try {
                    cmder.streamByeCmd(device, channelId, ssrcInfo.getStream(), null);
                } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
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
                logger.info("[点播成功] deviceId: {}, channelId:{}, 码流类型：{}", device.getDeviceId(), channelId,
                        device.isSwitchPrimarySubStream() ? "辅码流" : "主码流");
                snapOnPlay(mediaServerItemInuse, device.getDeviceId(), channelId, ssrcInfo.getStream());
            }, (eventResult) -> {
                // 处理收到200ok后的TCP主动连接以及SSRC不一致的问题
                InviteOKHandler(eventResult, ssrcInfo, mediaServerItem, device, channelId,
                        timeOutTaskKey, callback, inviteInfo, InviteSessionType.PLAY);
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

    private void tcpActiveHandler(Device device, String channelId, String contentString,
                                  MediaServerItem mediaServerItem,
                                  String timeOutTaskKey, SSRCInfo ssrcInfo, ErrorCallback<Object> callback){
        if (!device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE")) {
            return;
        }
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
            logger.info("[TCP主动连接对方] deviceId: {}, channelId: {}, 连接对方的地址：{}:{}, 收流模式：{}, SSRC: {}, SSRC校验：{}", device.getDeviceId(), channelId, sdp.getConnection().getAddress(), port, device.getStreamMode(), ssrcInfo.getSsrc(), device.isSsrcCheck());
            JSONObject jsonObject = zlmresTfulUtils.connectRtpServer(mediaServerItem, sdp.getConnection().getAddress(), port, ssrcInfo.getStream());
            logger.info("[TCP主动连接对方] 结果： {}", jsonObject);
        } catch (SdpException e) {
            logger.error("[TCP主动连接对方] deviceId: {}, channelId: {}, 解析200OK的SDP信息失败", device.getDeviceId(), channelId, e);
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

    /**
     * 点播成功时调用截图.
     *
     * @param mediaServerItemInuse media
     * @param deviceId             设备 ID
     * @param channelId            通道 ID
     * @param stream               ssrc
     */
    private void snapOnPlay(MediaServerItem mediaServerItemInuse, String deviceId, String channelId, String stream) {
        String streamUrl;
        if (mediaServerItemInuse.getRtspPort() != 0) {
            streamUrl = String.format("rtsp://127.0.0.1:%s/%s/%s", mediaServerItemInuse.getRtspPort(), "rtp", stream);
        } else {
            streamUrl = String.format("http://127.0.0.1:%s/%s/%s.live.mp4", mediaServerItemInuse.getHttpPort(), "rtp", stream);
        }
        String path = "snap";
        String fileName = deviceId + "_" + channelId + ".jpg";
        // 请求截图
        logger.info("[请求截图]: " + fileName);
        zlmresTfulUtils.getSnap(mediaServerItemInuse, streamUrl, 15, 1, path, fileName);
    }

    private StreamInfo onPublishHandlerForPlay(MediaServerItem mediaServerItem, HookParam hookParam, String deviceId, String channelId) {
        StreamInfo streamInfo = null;
        Device device = redisCatchStorage.getDevice(deviceId);
        OnStreamChangedHookParam streamChangedHookParam = (OnStreamChangedHookParam)hookParam;
        streamInfo = onPublishHandler(mediaServerItem, streamChangedHookParam, deviceId, channelId);
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
            logger.warn("[录像回放] 未找到设备 deviceId: {},channelId:{}", deviceId, channelId);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到设备：" + deviceId);
        }

        MediaServerItem newMediaServerItem = getNewMediaServerItem(device);
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
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(newMediaServerItem, stream, null, device.isSsrcCheck(),  true, 0, false, device.getStreamModeForParam());
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


    private void InviteOKHandler(SipSubscribe.EventResult eventResult, SSRCInfo ssrcInfo, MediaServerItem mediaServerItem,
                                 Device device, String channelId, String timeOutTaskKey, ErrorCallback<Object> callback,
                                 InviteInfo inviteInfo, InviteSessionType inviteSessionType){
        inviteInfo.setStatus(InviteSessionStatus.ok);
        ResponseEvent responseEvent = (ResponseEvent) eventResult.event;
        String contentString = new String(responseEvent.getResponse().getRawContent());
        String ssrcInResponse = SipUtils.getSsrcFromSdp(contentString);
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
        MediaServerItem newMediaServerItem = getNewMediaServerItemHasAssist(device);
        if (newMediaServerItem == null) {
            callback.run(InviteErrorCode.ERROR_FOR_ASSIST_NOT_READY.getCode(),
                    InviteErrorCode.ERROR_FOR_ASSIST_NOT_READY.getMsg(),
                    null);
            return;
        }
        // 录像下载不使用固定流地址，固定流地址会导致如果开始时间与结束时间一致时文件错误的叠加在一起
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(newMediaServerItem, null, null, device.isSsrcCheck(),  true, 0, false, device.getStreamModeForParam());
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
                        // 处理收到200ok后的TCP主动连接以及SSRC不一致的问题
                        InviteOKHandler(eventResult, ssrcInfo, mediaServerItem, device, channelId,
                                downLoadTimeOutTaskKey, callback, inviteInfo, InviteSessionType.DOWNLOAD);
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

}
