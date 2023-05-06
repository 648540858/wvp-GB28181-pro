package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson2.JSON;
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
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.media.zlm.AssistRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeFactory;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForStreamChange;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.*;
import com.genersoft.iot.vmp.service.bean.*;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
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
    public SSRCInfo play(MediaServerItem mediaServerItem, String deviceId, String channelId, InviteErrorCallback<Object> callback) {
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
                     InviteErrorCallback<Object> callback) {

        logger.info("[点播开始] deviceId: {}, channelId: {},收流端口：{}, 收流模式：{}, SSRC: {}, SSRC校验：{}", device.getDeviceId(), channelId, ssrcInfo.getPort(), device.getStreamMode(), ssrcInfo.getSsrc(), device.isSsrcCheck());

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
                        if (!ssrcFactory.checkSsrc(mediaServerItem.getId(),ssrcInResponse)) {
                            // ssrc 不可用
                            logger.info("[点播消息] SSRC修正时发现ssrc不可使用 {}->{}", ssrcInfo.getSsrc(), ssrcInResponse);
                            // 释放ssrc
                            ssrcFactory.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                            streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());

                            callback.run(InviteErrorCode.ERROR_FOR_SSRC_UNAVAILABLE.getCode(),
                                    InviteErrorCode.ERROR_FOR_SSRC_UNAVAILABLE.getMsg(), null);
                            inviteStreamService.call(InviteSessionType.PLAY, device.getDeviceId(), channelId, null,
                                    InviteErrorCode.ERROR_FOR_SSRC_UNAVAILABLE.getCode(),
                                    InviteErrorCode.ERROR_FOR_SSRC_UNAVAILABLE.getMsg(), null);

                            return;
                        }
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
                                        InviteErrorCode.SUCCESS.getMsg(), null);
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
                                logger.warn("[停止点播] {}/{}", device.getDeviceId(), channelId);
                                cmder.streamByeCmd(device, channelId, ssrcInfo.getStream(), null, null);
                            } catch (InvalidArgumentException | SipException | ParseException | SsrcTransactionNotFoundException e) {
                                logger.error("[命令发送失败] 停止点播， 发送BYE: {}", e.getMessage());
                                throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
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

    private void onPublishHandlerForPlayback(MediaServerItem mediaServerItem, JSONObject response, String deviceId, String channelId, PlayBackCallback playBackCallback) {

        StreamInfo streamInfo = onPublishHandler(mediaServerItem, response, deviceId, channelId);
        PlayBackResult<StreamInfo> playBackResult = new PlayBackResult<>();
        if (streamInfo != null) {
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

            playBackResult.setCode(ErrorCode.SUCCESS.getCode());
            playBackResult.setMsg(ErrorCode.SUCCESS.getMsg());
            playBackResult.setData(streamInfo);
            playBackCallback.call(playBackResult);
        } else {
            logger.warn("录像回放调用失败！");
            playBackResult.setCode(ErrorCode.ERROR100.getCode());
            playBackResult.setMsg("录像回放调用失败！");
            playBackCallback.call(playBackResult);
        }
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
                                                          String endTime, InviteStreamCallback inviteStreamCallback,
                                                          PlayBackCallback callback) {
        Device device = storager.queryVideoDevice(deviceId);
        if (device == null) {
            return;
        }
        MediaServerItem newMediaServerItem = getNewMediaServerItem(device);
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(newMediaServerItem, null, null, device.isSsrcCheck(),  true, 0, false, device.getStreamModeForParam());
        playBack(newMediaServerItem, ssrcInfo, deviceId, channelId, startTime, endTime, inviteStreamCallback, callback);
    }

    @Override
    public void playBack(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo,
                                                          String deviceId, String channelId, String startTime,
                                                          String endTime, InviteStreamCallback infoCallBack,
                                                          PlayBackCallback playBackCallback) {
        if (mediaServerItem == null || ssrcInfo == null) {
            return;
        }

        Device device = storager.queryVideoDevice(deviceId);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "设备： " + deviceId + "不存在");
        }
        logger.info("[回放消息] deviceId: {}, channelId: {},收流端口：{}, 收流模式：{}, SSRC: {}, SSRC校验：{}", device.getDeviceId(), channelId, ssrcInfo.getPort(), device.getStreamMode(), ssrcInfo.getSsrc(), device.isSsrcCheck());
        PlayBackResult<StreamInfo> playBackResult = new PlayBackResult<>();
        String playBackTimeOutTaskKey = UUID.randomUUID().toString();
        dynamicTask.startDelay(playBackTimeOutTaskKey, () -> {
            logger.warn(String.format("设备回放超时，deviceId：%s ，channelId：%s", deviceId, channelId));
            playBackResult.setCode(ErrorCode.ERROR100.getCode());
            playBackResult.setMsg("回放超时");

            try {
                cmder.streamByeCmd(device, channelId, ssrcInfo.getStream(), null);
            } catch (InvalidArgumentException | ParseException | SipException e) {
                logger.error("[录像流]回放超时 发送BYE失败 {}", e.getMessage());
            } catch (SsrcTransactionNotFoundException e) {
                // 点播超时回复BYE 同时释放ssrc以及此次点播的资源
                mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
                streamSession.remove(deviceId, channelId, ssrcInfo.getStream());
            }
            // 回复之前所有的点播请求
            playBackCallback.call(playBackResult);
        }, userSetting.getPlayTimeout());

        SipSubscribe.Event errorEvent = event -> {
            dynamicTask.stop(playBackTimeOutTaskKey);
            playBackResult.setCode(ErrorCode.ERROR100.getCode());
            playBackResult.setMsg(String.format("回放失败， 错误码： %s, %s", event.statusCode, event.msg));
            playBackResult.setEvent(event);
            playBackCallback.call(playBackResult);
            streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());
        };

        InviteStreamCallback hookEvent = (InviteStreamInfo inviteStreamInfo) -> {
            logger.info("收到回放订阅消息： " + inviteStreamInfo.getResponse().toJSONString());
            dynamicTask.stop(playBackTimeOutTaskKey);
            StreamInfo streamInfo = onPublishHandler(inviteStreamInfo.getMediaServerItem(), inviteStreamInfo.getResponse(), deviceId, channelId);
            if (streamInfo == null) {
                logger.warn("设备回放API调用失败！");
                playBackResult.setCode(ErrorCode.ERROR100.getCode());
                playBackResult.setMsg("设备回放API调用失败！");
                playBackCallback.call(playBackResult);
                return;
            }
            redisCatchStorage.startPlayback(streamInfo, inviteStreamInfo.getCallId());

            playBackResult.setCode(ErrorCode.SUCCESS.getCode());
            playBackResult.setMsg(ErrorCode.SUCCESS.getMsg());
            playBackResult.setData(streamInfo);
            playBackResult.setMediaServerItem(inviteStreamInfo.getMediaServerItem());
            playBackResult.setResponse(inviteStreamInfo.getResponse());
            playBackCallback.call(playBackResult);
        };

        try {
            cmder.playbackStreamCmd(mediaServerItem, ssrcInfo, device, channelId, startTime, endTime, infoCallBack,
                    hookEvent, eventResult -> {
                        if (eventResult.type == SipSubscribe.EventResultType.response) {
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
                                    return;
                                }
                                logger.info("[回放消息] 收到invite 200, 发现下级自定义了ssrc: {}", ssrcInResponse);
                                if (!mediaServerItem.isRtpEnable() || device.isSsrcCheck()) {
                                    logger.info("[回放消息] SSRC修正 {}->{}", ssrcInfo.getSsrc(), ssrcInResponse);

                                    if (!ssrcFactory.checkSsrc(mediaServerItem.getId(),ssrcInResponse)) {
                                        // ssrc 不可用
                                        // 释放ssrc
                                        dynamicTask.stop(playBackTimeOutTaskKey);
                                        mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                                        streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());
                                        eventResult.msg = "下级自定义了ssrc,但是此ssrc不可用";
                                        eventResult.statusCode = 400;
                                        errorEvent.response(eventResult);
                                        return;
                                    }

                                    // 单端口模式streamId也有变化，需要重新设置监听
                                    if (!mediaServerItem.isRtpEnable()) {
                                        // 添加订阅
                                        HookSubscribeForStreamChange hookSubscribe = HookSubscribeFactory.on_stream_changed("rtp", ssrcInfo.getStream(), true, "rtsp", mediaServerItem.getId());
                                        subscribe.removeSubscribe(hookSubscribe);
                                        hookSubscribe.getContent().put("stream", String.format("%08x", Integer.parseInt(ssrcInResponse)).toUpperCase());
                                        subscribe.addSubscribe(hookSubscribe, (MediaServerItem mediaServerItemInUse, JSONObject response) -> {
                                            logger.info("[ZLM HOOK] ssrc修正后收到订阅消息： " + response.toJSONString());
                                            dynamicTask.stop(playBackTimeOutTaskKey);
                                            // hook响应
                                            onPublishHandlerForPlayback(mediaServerItemInUse, response, device.getDeviceId(), channelId, playBackCallback);
                                            hookEvent.call(new InviteStreamInfo(mediaServerItem, null, eventResult.callId, "rtp", ssrcInfo.getStream()));
                                        });
                                    }

                                    // 关闭rtp server
                                    mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream(), result->{
                                        if (result) {
                                            // 重新开启ssrc server
                                            mediaServerService.openRTPServer(mediaServerItem, ssrcInfo.getStream(), ssrcInResponse, device.isSsrcCheck(), true, ssrcInfo.getPort(), true, device.getStreamModeForParam());
                                        }else {
                                            try {
                                                logger.warn("[回放消息]停止 {}/{}", device.getDeviceId(), channelId);
                                                cmder.streamByeCmd(device, channelId, ssrcInfo.getStream(), null, null);
                                            } catch (InvalidArgumentException | SipException | ParseException | SsrcTransactionNotFoundException e) {
                                                logger.error("[命令发送失败] 停止点播 停止， 发送BYE: {}", e.getMessage());
                                                throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
                                            }

                                            dynamicTask.stop(playBackTimeOutTaskKey);
                                            // 释放ssrc
                                            mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                                            streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());
                                            errorEvent.response(eventResult);
                                            eventResult.msg = "下级自定义了ssrc,重新设置收流信息失败";
                                            eventResult.statusCode = 500;
                                            errorEvent.response(eventResult);
                                        }
                                    });
                                }
                            }
                        }

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
    public void download(String deviceId, String channelId, String startTime, String endTime, int downloadSpeed, InviteStreamCallback infoCallBack, PlayBackCallback playBackCallback) {
        Device device = storager.queryVideoDevice(deviceId);
        if (device == null) {
            return;
        }
        MediaServerItem newMediaServerItem = getNewMediaServerItemHasAssist(device);
        if (newMediaServerItem == null) {
            PlayBackResult<StreamInfo> downloadResult = new PlayBackResult<>();
            downloadResult.setCode(ErrorCode.ERROR100.getCode());
            downloadResult.setMsg("未找到assist服务");
            playBackCallback.call(downloadResult);
            return;
        }
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(newMediaServerItem, null, null, device.isSsrcCheck(),  true, 0, false, device.getStreamModeForParam());
        download(newMediaServerItem, ssrcInfo, deviceId, channelId, startTime, endTime, downloadSpeed, infoCallBack, playBackCallback);
    }


    @Override
    public void download(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo, String deviceId, String channelId, String startTime, String endTime, int downloadSpeed, InviteStreamCallback infoCallBack, PlayBackCallback hookCallBack) {
        if (mediaServerItem == null || ssrcInfo == null) {
            return;
        }

        Device device = storager.queryVideoDevice(deviceId);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "设备：" + deviceId + "不存在");
        }
        PlayBackResult<StreamInfo> downloadResult = new PlayBackResult<>();
        logger.info("[录像下载] deviceId: {}, channelId: {},收流端口：{}, 收流模式：{}, SSRC: {}, SSRC校验：{}", device.getDeviceId(), channelId, ssrcInfo.getPort(), device.getStreamMode(), ssrcInfo.getSsrc(), device.isSsrcCheck());
        String downLoadTimeOutTaskKey = UUID.randomUUID().toString();
        dynamicTask.startDelay(downLoadTimeOutTaskKey, () -> {
            logger.warn(String.format("录像下载请求超时，deviceId：%s ，channelId：%s", deviceId, channelId));
            downloadResult.setCode(ErrorCode.ERROR100.getCode());
            downloadResult.setMsg("录像下载请求超时");
            hookCallBack.call(downloadResult);

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
            downloadResult.setCode(ErrorCode.ERROR100.getCode());
            downloadResult.setMsg(String.format("录像下载失败， 错误码： %s, %s", event.statusCode, event.msg));
            downloadResult.setEvent(event);
            hookCallBack.call(downloadResult);
            streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());
        };
        InviteStreamCallback hookEvent = (InviteStreamInfo inviteStreamInfo) -> {
            logger.info("[录像下载]收到订阅消息： " + inviteStreamInfo.getCallId());
            dynamicTask.stop(downLoadTimeOutTaskKey);
            StreamInfo streamInfo = onPublishHandler(inviteStreamInfo.getMediaServerItem(), inviteStreamInfo.getResponse(), deviceId, channelId);
            streamInfo.setStartTime(startTime);
            streamInfo.setEndTime(endTime);
            redisCatchStorage.startDownload(streamInfo, inviteStreamInfo.getCallId());
            downloadResult.setCode(ErrorCode.SUCCESS.getCode());
            downloadResult.setMsg(ErrorCode.SUCCESS.getMsg());
            downloadResult.setData(streamInfo);
            downloadResult.setMediaServerItem(inviteStreamInfo.getMediaServerItem());
            downloadResult.setResponse(inviteStreamInfo.getResponse());
            hookCallBack.call(downloadResult);
        };
        try {
            cmder.downloadStreamCmd(mediaServerItem, ssrcInfo, device, channelId, startTime, endTime, downloadSpeed, infoCallBack,
                    hookEvent, errorEvent, eventResult ->
                    {
                        if (eventResult.type == SipSubscribe.EventResultType.response) {
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
                                    return;
                                }
                                logger.info("[录像下载] 收到invite 200, 发现下级自定义了ssrc: {}", ssrcInResponse);
                                if (!mediaServerItem.isRtpEnable() || device.isSsrcCheck()) {
                                    logger.info("[录像下载] SSRC修正 {}->{}", ssrcInfo.getSsrc(), ssrcInResponse);

                                    if (!ssrcFactory.checkSsrc(mediaServerItem.getId(),ssrcInResponse)) {
                                        // ssrc 不可用
                                        // 释放ssrc
                                        mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                                        streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());
                                        eventResult.msg = "下级自定义了ssrc,但是此ssrc不可用";
                                        eventResult.statusCode = 400;
                                        errorEvent.response(eventResult);
                                        return;
                                    }

                                    // 单端口模式streamId也有变化，需要重新设置监听
                                    if (!mediaServerItem.isRtpEnable()) {
                                        // 添加订阅
                                        HookSubscribeForStreamChange hookSubscribe = HookSubscribeFactory.on_stream_changed("rtp", ssrcInfo.getStream(), true, "rtsp", mediaServerItem.getId());
                                        subscribe.removeSubscribe(hookSubscribe);
                                        hookSubscribe.getContent().put("stream", String.format("%08x", Integer.parseInt(ssrcInResponse)).toUpperCase());
                                        subscribe.addSubscribe(hookSubscribe, (MediaServerItem mediaServerItemInUse, JSONObject response) -> {
                                            logger.info("[ZLM HOOK] ssrc修正后收到订阅消息： " + response.toJSONString());
                                            dynamicTask.stop(downLoadTimeOutTaskKey);
                                            // hook响应
                                            onPublishHandlerForPlayback(mediaServerItemInUse, response, device.getDeviceId(), channelId, hookCallBack);
                                            hookEvent.call(new InviteStreamInfo(mediaServerItem, null, eventResult.callId, "rtp", ssrcInfo.getStream()));
                                        });
                                    }

                                    // 关闭rtp server
                                    mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream(), result->{
                                        if (result) {
                                            // 重新开启ssrc server
                                            mediaServerService.openRTPServer(mediaServerItem, ssrcInfo.getStream(), ssrcInResponse, device.isSsrcCheck(), true, ssrcInfo.getPort(), true, device.getStreamModeForParam());
                                        }else {
                                            try {
                                                logger.warn("[录像下载] 停止{}/{}", device.getDeviceId(), channelId);
                                                cmder.streamByeCmd(device, channelId, ssrcInfo.getStream(), null, null);
                                            } catch (InvalidArgumentException | SipException | ParseException | SsrcTransactionNotFoundException e) {
                                                logger.error("[命令发送失败] 录像下载停止， 发送BYE: {}", e.getMessage());
                                                throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
                                            }

                                            dynamicTask.stop(downLoadTimeOutTaskKey);
                                            // 释放ssrc
                                            mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());

                                            streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());
                                            eventResult.msg = "下级自定义了ssrc,重新设置收流信息失败";
                                            eventResult.statusCode = 500;
                                            errorEvent.response(eventResult);
                                        }
                                    });
                                }
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
        StreamInfo streamInfo = redisCatchStorage.queryDownload(deviceId, channelId, stream, null);
        if (streamInfo != null) {
            if (streamInfo.getProgress() == 1) {
                return streamInfo;
            }

            // 获取当前已下载时长
            String mediaServerId = streamInfo.getMediaServerId();
            MediaServerItem mediaServerItem = mediaServerService.getOne(mediaServerId);
            if (mediaServerItem == null) {
                logger.warn("查询录像信息时发现节点已离线");
                return null;
            }
            if (mediaServerItem.getRecordAssistPort() > 0) {
                JSONObject jsonObject = assistRESTfulUtils.fileDuration(mediaServerItem, streamInfo.getApp(), streamInfo.getStream(), null);
                if (jsonObject == null) {
                    throw new ControllerException(ErrorCode.ERROR100.getCode(), "连接Assist服务失败");
                }
                if (jsonObject.getInteger("code") == 0) {
                    long duration = jsonObject.getLong("data");

                    if (duration == 0) {
                        streamInfo.setProgress(0);
                    } else {
                        String startTime = streamInfo.getStartTime();
                        String endTime = streamInfo.getEndTime();
                        long start = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime);
                        long end = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime);

                        BigDecimal currentCount = new BigDecimal(duration / 1000);
                        BigDecimal totalCount = new BigDecimal(end - start);
                        BigDecimal divide = currentCount.divide(totalCount, 2, RoundingMode.HALF_UP);
                        double process = divide.doubleValue();
                        streamInfo.setProgress(process);
                    }
                }
            }
        }
        return streamInfo;
    }

    private void onPublishHandlerForDownload(InviteStreamInfo inviteStreamInfo, String deviceId, String channelId, String uuid) {
        RequestMessage msg = new RequestMessage();
        msg.setKey(DeferredResultHolder.CALLBACK_CMD_DOWNLOAD + deviceId + channelId);
        msg.setId(uuid);
        StreamInfo streamInfo = onPublishHandler(inviteStreamInfo.getMediaServerItem(), inviteStreamInfo.getResponse(), deviceId, channelId);
        if (streamInfo != null) {
            redisCatchStorage.startDownload(streamInfo, inviteStreamInfo.getCallId());
            msg.setData(JSON.toJSONString(streamInfo));
            resultHolder.invokeResult(msg);
        } else {
            logger.warn("设备预览API调用失败！");
            msg.setData(WVPResult.fail(ErrorCode.ERROR100.getCode(), "设备预览API调用失败！"));
            resultHolder.invokeResult(msg);
        }
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
        String key = redisCatchStorage.queryPlaybackForKey(null, null, streamId, null);
        StreamInfo streamInfo = redisCatchStorage.queryPlayback(null, null, streamId, null);
        if (null == streamInfo) {
            logger.warn("streamId不存在!");
            throw new ServiceException("streamId不存在");
        }
        streamInfo.setPause(true);
        redisTemplate.opsForValue().set(key, streamInfo);
        MediaServerItem mediaServerItem = mediaServerService.getOne(streamInfo.getMediaServerId());
        if (null == mediaServerItem) {
            logger.warn("mediaServer 不存在!");
            throw new ServiceException("mediaServer不存在");
        }
        // zlm 暂停RTP超时检查
        JSONObject jsonObject = zlmresTfulUtils.pauseRtpCheck(mediaServerItem, streamId);
        if (jsonObject == null || jsonObject.getInteger("code") != 0) {
            throw new ServiceException("暂停RTP接收失败");
        }
        Device device = storager.queryVideoDevice(streamInfo.getDeviceID());
        cmder.playPauseCmd(device, streamInfo);
    }

    @Override
    public void resumeRtp(String streamId) throws ServiceException, InvalidArgumentException, ParseException, SipException {
        String key = redisCatchStorage.queryPlaybackForKey(null, null, streamId, null);
        StreamInfo streamInfo = redisCatchStorage.queryPlayback(null, null, streamId, null);
        if (null == streamInfo) {
            logger.warn("streamId不存在!");
            throw new ServiceException("streamId不存在");
        }
        streamInfo.setPause(false);
        redisTemplate.opsForValue().set(key, streamInfo);
        MediaServerItem mediaServerItem = mediaServerService.getOne(streamInfo.getMediaServerId());
        if (null == mediaServerItem) {
            logger.warn("mediaServer 不存在!");
            throw new ServiceException("mediaServer不存在");
        }
        // zlm 暂停RTP超时检查
        JSONObject jsonObject = zlmresTfulUtils.resumeRtpCheck(mediaServerItem, streamId);
        if (jsonObject == null || jsonObject.getInteger("code") != 0) {
            throw new ServiceException("继续RTP接收失败");
        }
        Device device = storager.queryVideoDevice(streamInfo.getDeviceID());
        cmder.playResumeCmd(device, streamInfo);
    }
}
