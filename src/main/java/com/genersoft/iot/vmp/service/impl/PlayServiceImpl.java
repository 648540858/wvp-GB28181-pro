package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson2.JSONArray;
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
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.media.zlm.AssistRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeFactory;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForStreamChange;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.*;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
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
    private ZLMRTPServerFactory zlmrtpServerFactory;

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

                Boolean ready = zlmrtpServerFactory.isStreamReady(mediaInfo, "rtp", streamId);
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
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(mediaServerItem, streamId, null, device.isSsrcCheck(),  false, 0, false, device.getStreamModeForParam());
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


    @Override
    public void play(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo, Device device, String channelId,
                     ErrorCallback<Object> callback) {

        if (mediaServerItem == null || ssrcInfo == null) {
            callback.run(InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getCode(),
                    InviteErrorCode.ERROR_FOR_PARAMETER_ERROR.getMsg(),
                    null);
            return;
        }
        logger.info("\r\n" +
                " [点播开始] \r\n" +
                "deviceId  : {}, \r\n" +
                "channelId : {},\r\n" +
                "收流端口    : {}, \r\n" +
                "收流模式    : {}, \r\n" +
                "SSRC      : {}, \r\n" +
                "SSRC校验   ：{}",
                device.getDeviceId(),
                channelId,
                ssrcInfo.getPort(),
                device.getStreamMode(),
                ssrcInfo.getSsrc(),
                device.isSsrcCheck());

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
        InviteInfo inviteInfo = InviteInfo.getinviteInfo(device.getDeviceId(), channelId, ssrcInfo.getStream(), ssrcInfo,
                mediaServerItem.getSdpIp(), ssrcInfo.getPort(), device.getStreamMode(), InviteSessionType.PLAY,
                InviteSessionStatus.ready);
        inviteStreamService.updateInviteInfo(inviteInfo);
        // 超时处理
        String timeOutTaskKey = UUID.randomUUID().toString();
        dynamicTask.startDelay(timeOutTaskKey, () -> {
            // 执行超时任务时查询是否已经成功，成功了则不执行超时任务，防止超时任务取消失败的情况
            InviteInfo inviteInfoForTimeOut = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, device.getDeviceId(), channelId);
            if (inviteInfoForTimeOut == null || inviteInfoForTimeOut.getStreamInfo() == null) {
                logger.info("[点播超时] 收流超时 deviceId: {}, channelId: {}，端口：{}, SSRC: {}", device.getDeviceId(), channelId, ssrcInfo.getPort(), ssrcInfo.getSsrc());
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
            cmder.playStreamCmd(mediaServerItem, ssrcInfo, device, channelId, (MediaServerItem mediaServerItemInuse, JSONObject response) -> {
                logger.info("收到订阅消息： " + response.toJSONString());
                dynamicTask.stop(timeOutTaskKey);
                // hook响应
                StreamInfo streamInfo = onPublishHandlerForPlay(mediaServerItemInuse, response, device.getDeviceId(), channelId);
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
                logger.info("[点播成功] deviceId: {}, channelId: {}", device.getDeviceId(), channelId);
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
                String ssrcInResponse = SipUtils.getSsrcFromSdp(contentString);

                // 检查是否有y字段
                if (ssrcInResponse != null) {
                    // 查询到ssrc不一致且开启了ssrc校验则需要针对处理
                    if (ssrcInfo.getSsrc().equals(ssrcInResponse)) {
                        if (device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE")) {
                            try {
                                Gb28181Sdp gb28181Sdp = SipUtils.parseSDP(contentString);
                                SessionDescription sdp = gb28181Sdp.getBaseSdb();
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
                            inviteInfo.setStream(stream);
                            subscribe.addSubscribe(hookSubscribe, (MediaServerItem mediaServerItemInUse, JSONObject response) -> {
                                logger.info("[ZLM HOOK] ssrc修正后收到订阅消息： " + response.toJSONString());
                                dynamicTask.stop(timeOutTaskKey);
                                // hook响应
                                StreamInfo streamInfo = onPublishHandlerForPlay(mediaServerItemInUse, response, device.getDeviceId(), channelId);
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

                            streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());

                            callback.run(InviteErrorCode.ERROR_FOR_RESET_SSRC.getCode(),
                                    "下级自定义了ssrc,重新设置收流信息失败", null);
                            inviteStreamService.call(InviteSessionType.PLAY, device.getDeviceId(), channelId, null,
                                    InviteErrorCode.ERROR_FOR_RESET_SSRC.getCode(),
                                    "下级自定义了ssrc,重新设置收流信息失败", null);

                        }else {
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

    private StreamInfo onPublishHandlerForPlay(MediaServerItem mediaServerItem, JSONObject response, String deviceId, String channelId) {
        StreamInfo streamInfo = onPublishHandler(mediaServerItem, response, deviceId, channelId);
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

    private StreamInfo onPublishHandlerForPlayback(MediaServerItem mediaServerItem, JSONObject response, String deviceId, String channelId, String startTime, String endTime) {

        StreamInfo streamInfo = onPublishHandler(mediaServerItem, response, deviceId, channelId);
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
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(newMediaServerItem, null, null, device.isSsrcCheck(),  true, 0, false, device.getStreamModeForParam());
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
        InviteInfo inviteInfo = InviteInfo.getinviteInfo(device.getDeviceId(), channelId, ssrcInfo.getStream(), ssrcInfo,
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

        ZlmHttpHookSubscribe.Event hookEvent = (mediaServerItemInuse, jsonObject) -> {
            logger.info("收到回放订阅消息： " + jsonObject);
            dynamicTask.stop(playBackTimeOutTaskKey);
            StreamInfo streamInfo = onPublishHandlerForPlayback(mediaServerItemInuse, jsonObject, deviceId, channelId, startTime, endTime);
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
                        String ssrcInResponse = SipUtils.getSsrcFromSdp(contentString);

                        // 检查是否有y字段
                        if (ssrcInResponse != null) {
                            // 查询到ssrc不一致且开启了ssrc校验则需要针对处理
                            if (ssrcInfo.getSsrc().equals(ssrcInResponse)) {
                                if (device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE")) {
                                    try {
                                        Gb28181Sdp gb28181Sdp = SipUtils.parseSDP(contentString);
                                        SessionDescription sdp = gb28181Sdp.getBaseSdb();
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
                                    inviteInfo.setStream(stream);
                                    subscribe.addSubscribe(hookSubscribe, (MediaServerItem mediaServerItemInUse, JSONObject response) -> {
                                        logger.info("[ZLM HOOK] ssrc修正后收到订阅消息： " + response.toJSONString());
                                        dynamicTask.stop(playBackTimeOutTaskKey);
                                        // hook响应
                                        hookEvent.response(mediaServerItemInUse, response);
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

                                    streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());

                                    callback.run(InviteErrorCode.ERROR_FOR_RESET_SSRC.getCode(),
                                            "下级自定义了ssrc,重新设置收流信息失败", null);

                                }else {
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
        InviteInfo inviteInfo = InviteInfo.getinviteInfo(device.getDeviceId(), channelId, ssrcInfo.getStream(), ssrcInfo,
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
        ZlmHttpHookSubscribe.Event hookEvent = (mediaServerItemInuse, jsonObject) -> {
            logger.info("[录像下载]收到订阅消息： " + jsonObject);
            dynamicTask.stop(downLoadTimeOutTaskKey);
            StreamInfo streamInfo = onPublishHandlerForDownload(mediaServerItemInuse, jsonObject, deviceId, channelId, startTime, endTime);
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
                        String ssrcInResponse = SipUtils.getSsrcFromSdp(contentString);
                        // 检查是否有y字段
                        if (ssrcInResponse != null) {
                            // 查询到ssrc不一致且开启了ssrc校验则需要针对处理
                            if (ssrcInfo.getSsrc().equals(ssrcInResponse)) {
                                if (device.getStreamMode().equalsIgnoreCase("TCP-ACTIVE")) {
                                    try {
                                        Gb28181Sdp gb28181Sdp = SipUtils.parseSDP(contentString);
                                        SessionDescription sdp = gb28181Sdp.getBaseSdb();
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
                                    hookSubscribe.getContent().put("stream", String.format("%08x", Integer.parseInt(ssrcInResponse)).toUpperCase());
                                    subscribe.addSubscribe(hookSubscribe, (MediaServerItem mediaServerItemInUse, JSONObject response) -> {
                                        logger.info("[ZLM HOOK] ssrc修正后收到订阅消息： " + response.toJSONString());
                                        dynamicTask.stop(downLoadTimeOutTaskKey);
                                        hookEvent.response(mediaServerItemInUse, response);
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

                                    streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());

                                    callback.run(InviteErrorCode.ERROR_FOR_RESET_SSRC.getCode(),
                                            "下级自定义了ssrc,重新设置收流信息失败", null);

                                }else {
                                    ssrcInfo.setSsrc(ssrcInResponse);
                                    inviteInfo.setSsrcInfo(ssrcInfo);
                                    inviteInfo.setStream(ssrcInfo.getStream());
                                }
                            }else {
                                logger.info("[录像下载] 收到invite 200, 下级自定义了ssrc, 但是当前模式无需修正");
                            }
                        }
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

    private StreamInfo onPublishHandlerForDownload(MediaServerItem mediaServerItemInuse, JSONObject response, String deviceId, String channelId, String startTime, String endTime) {
        StreamInfo streamInfo = onPublishHandler(mediaServerItemInuse, response, deviceId, channelId);
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


    public StreamInfo onPublishHandler(MediaServerItem mediaServerItem, JSONObject resonse, String deviceId, String channelId) {
        String streamId = resonse.getString("stream");
        JSONArray tracks = resonse.getJSONArray("tracks");
        StreamInfo streamInfo = mediaService.getStreamInfoByAppAndStream(mediaServerItem, "rtp", streamId, tracks, null);
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
                String filePath = path + File.separator + fileName;
                File snapFile = new File(path + File.separator + fileName);
                if (snapFile.exists()) {
                    errorCallback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), filePath);
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
