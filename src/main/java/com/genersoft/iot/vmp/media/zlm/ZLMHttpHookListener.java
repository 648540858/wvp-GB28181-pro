package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.media.bean.ResultForOnPublish;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.media.*;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaSendRtpStoppedEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.zlm.dto.ZLMServerConfig;
import com.genersoft.iot.vmp.media.zlm.dto.hook.*;
import com.genersoft.iot.vmp.media.zlm.event.HookZlmServerKeepaliveEvent;
import com.genersoft.iot.vmp.media.zlm.event.HookZlmServerStartEvent;
import com.genersoft.iot.vmp.service.*;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @description:针对 ZLMediaServer的hook事件监听
 * @author: swwheihei
 * @date: 2020年5月8日 上午10:46:48
 */
@RestController
@RequestMapping("/index/hook")
public class ZLMHttpHookListener {

    private final static Logger logger = LoggerFactory.getLogger(ZLMHttpHookListener.class);

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private ISIPCommanderForPlatform commanderFroPlatform;

    @Autowired
    private AudioBroadcastManager audioBroadcastManager;

    @Autowired
    private IPlayService playService;

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;


    @Autowired
    private IRedisRpcService redisRpcService;

    @Autowired
    private IInviteStreamService inviteStreamService;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private IStreamProxyService streamProxyService;

    @Autowired
    private DeferredResultHolder resultHolder;

    @Autowired
    private IMediaService mediaService;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private HookSubscribe subscribe;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IUserService userService;

    @Autowired
    private ICloudRecordService cloudRecordService;

    @Autowired
    private VideoStreamSessionManager sessionManager;

    @Autowired
    private SSRCFactory ssrcFactory;

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private IStreamPushService streamPushService;

    /**
     * 服务器定时上报时间，上报间隔可配置，默认10s上报一次
     */
    @ResponseBody
    @PostMapping(value = "/on_server_keepalive", produces = "application/json;charset=UTF-8")
    public HookResult onServerKeepalive(@RequestBody OnServerKeepaliveHookParam param) {
        try {
            HookZlmServerKeepaliveEvent event = new HookZlmServerKeepaliveEvent(this);
            MediaServer mediaServerItem = mediaServerService.getOne(param.getMediaServerId());
            if (mediaServerItem != null) {
                event.setMediaServerItem(mediaServerItem);
                applicationEventPublisher.publishEvent(event);
            }
        }catch (Exception e) {
            logger.info("[ZLM-HOOK-心跳] 发送通知失败 ", e);
        }
        return HookResult.SUCCESS();
    }

    /**
     * 播放器鉴权事件，rtsp/rtmp/http-flv/ws-flv/hls的播放都将触发此鉴权事件。
     */
    @ResponseBody
    @PostMapping(value = "/on_play", produces = "application/json;charset=UTF-8")
    public HookResult onPlay(@RequestBody OnPlayHookParam param) {

        Map<String, String> paramMap = urlParamToMap(param.getParams());
        // 对于播放流进行鉴权
        boolean authenticateResult = mediaService.authenticatePlay(param.getApp(), param.getStream(), paramMap.get("callId"));
        if (!authenticateResult) {
            logger.info("[ZLM HOOK] 播放鉴权 失败：{}->{}", param.getMediaServerId(), param);
            return new HookResult(401, "Unauthorized");
        }
        logger.info("[ZLM HOOK] 播放鉴权成功：{}->{}", param.getMediaServerId(), param);
        return HookResult.SUCCESS();
    }

    /**
     * rtsp/rtmp/rtp推流鉴权事件。
     */
    @ResponseBody
    @PostMapping(value = "/on_publish", produces = "application/json;charset=UTF-8")
    public HookResultForOnPublish onPublish(@RequestBody OnPublishHookParam param) {

        JSONObject json = (JSONObject) JSON.toJSON(param);

        logger.info("[ZLM HOOK]推流鉴权：{}->{}", param.getMediaServerId(), param);
        // TODO 加快处理速度

        String mediaServerId = json.getString("mediaServerId");
        MediaServer mediaServer = mediaServerService.getOne(mediaServerId);
        if (mediaServer == null) {
            return new HookResultForOnPublish(0, "success");
        }
        // 推流鉴权的处理
        if (!"rtp".equals(param.getApp())) {
            StreamProxyItem stream = streamProxyService.getStreamProxyByAppAndStream(param.getApp(), param.getStream());
            if (stream != null) {
                HookResultForOnPublish result = HookResultForOnPublish.SUCCESS();
                result.setEnable_audio(stream.isEnableAudio());
                result.setEnable_mp4(stream.isEnableMp4());
                return result;
            }
            if (userSetting.getPushAuthority()) {
                // 推流鉴权
                if (param.getParams() == null) {
                    logger.info("推流鉴权失败： 缺少必要参数：sign=md5(user表的pushKey)");
                    return new HookResultForOnPublish(401, "Unauthorized");
                }
                Map<String, String> paramMap = urlParamToMap(param.getParams());
                String sign = paramMap.get("sign");
                if (sign == null) {
                    logger.info("推流鉴权失败： 缺少必要参数：sign=md5(user表的pushKey)");
                    return new HookResultForOnPublish(401, "Unauthorized");
                }
                // 推流自定义播放鉴权码
                String callId = paramMap.get("callId");
                // 鉴权配置
                boolean hasAuthority = userService.checkPushAuthority(callId, sign);
                if (!hasAuthority) {
                    logger.info("推流鉴权失败： sign 无权限: callId={}. sign={}", callId, sign);
                    return new HookResultForOnPublish(401, "Unauthorized");
                }
                StreamAuthorityInfo streamAuthorityInfo = StreamAuthorityInfo.getInstanceByHook(param);
                streamAuthorityInfo.setCallId(callId);
                streamAuthorityInfo.setSign(sign);
                // 鉴权通过
                redisCatchStorage.updateStreamAuthorityInfo(param.getApp(), param.getStream(), streamAuthorityInfo);
            }
        } else {
            zlmMediaListManager.sendStreamEvent(param.getApp(), param.getStream(), param.getMediaServerId());
        }


        HookResultForOnPublish result = HookResultForOnPublish.SUCCESS();
        result.setEnable_audio(true);
        taskExecutor.execute(() -> {
            ZlmHttpHookSubscribe.Event subscribe = this.subscribe.sendNotify(HookType.on_publish, json);
            if (subscribe != null) {
                subscribe.response(mediaInfo, param);
            }
        });

        ResultForOnPublish resultForOnPublish = mediaService.authenticatePublish(mediaServer, param.getApp(), param.getStream(), param.getParams());
        if (resultForOnPublish != null) {
            HookResultForOnPublish successResult = HookResultForOnPublish.getInstance(resultForOnPublish);
            logger.info("[ZLM HOOK]推流鉴权 响应：{}->{}->>>>{}", param.getMediaServerId(), param, successResult);
            return successResult;
        }else {
            HookResultForOnPublish fail = HookResultForOnPublish.Fail();
            logger.info("[ZLM HOOK]推流鉴权 响应：{}->{}->>>>{}", param.getMediaServerId(), param, fail);
            return fail;
        }
    }


    /**
     * rtsp/rtmp流注册或注销时触发此事件；此事件对回复不敏感。
     */
    @ResponseBody
    @PostMapping(value = "/on_stream_changed", produces = "application/json;charset=UTF-8")
    public HookResult onStreamChanged(@RequestBody OnStreamChangedHookParam param) {

        MediaServer mediaServer = mediaServerService.getOne(param.getMediaServerId());
        if (mediaServer == null) {
            return HookResult.SUCCESS();
        }

        if (param.isRegist()) {
            logger.info("[ZLM HOOK] 流注册, {}->{}->{}/{}", param.getMediaServerId(), param.getSchema(), param.getApp(), param.getStream());
            MediaArrivalEvent mediaArrivalEvent = MediaArrivalEvent.getInstance(this, param, mediaServer);
            applicationEventPublisher.publishEvent(mediaArrivalEvent);
        } else {
            logger.info("[ZLM HOOK] 流注销, {}->{}->{}/{}", param.getMediaServerId(), param.getSchema(), param.getApp(), param.getStream());
            MediaDepartureEvent mediaDepartureEvent = MediaDepartureEvent.getInstance(this, param, mediaServer);
            applicationEventPublisher.publishEvent(mediaDepartureEvent);
        }

        JSONObject json = (JSONObject) JSON.toJSON(param);
        taskExecutor.execute(() -> {
            ZlmHttpHookSubscribe.Event subscribe = this.subscribe.sendNotify(HookType.on_stream_changed, json);
            MediaServerItem mediaInfo = mediaServerService.getOne(param.getMediaServerId());
            if (mediaInfo == null) {
                logger.info("[ZLM HOOK] 流变化未找到ZLM, {}", param.getMediaServerId());
                return;
            }
            if (subscribe != null) {
                subscribe.response(mediaInfo, param);
            }

            List<OnStreamChangedHookParam.MediaTrack> tracks = param.getTracks();
            // TODO 重构此处逻辑
            if (param.isRegist()) {
                // 处理流注册的鉴权信息， 流注销这里不再删除鉴权信息，下次来了新的鉴权信息会对就的进行覆盖
                if (param.getOriginType() == OriginType.RTMP_PUSH.ordinal()
                        || param.getOriginType() == OriginType.RTSP_PUSH.ordinal()
                        || param.getOriginType() == OriginType.RTC_PUSH.ordinal()) {
                    StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(param.getApp(), param.getStream());
                    if (streamAuthorityInfo == null) {
                        streamAuthorityInfo = StreamAuthorityInfo.getInstanceByHook(param);
                    } else {
                        streamAuthorityInfo.setOriginType(param.getOriginType());
                        streamAuthorityInfo.setOriginTypeStr(param.getOriginTypeStr());
                    }
                    redisCatchStorage.updateStreamAuthorityInfo(param.getApp(), param.getStream(), streamAuthorityInfo);
                }
            }
            if ("rtsp".equals(param.getSchema())) {
                logger.info("流变化：注册->{}, app->{}, stream->{}", param.isRegist(), param.getApp(), param.getStream());
                if (param.isRegist()) {
                    mediaServerService.addCount(param.getMediaServerId());
                } else {
                    mediaServerService.removeCount(param.getMediaServerId());
                }

                int updateStatusResult = streamProxyService.updateStatus(param.isRegist(), param.getApp(), param.getStream());
                if (updateStatusResult > 0) {

                }

                if ("rtp".equals(param.getApp()) && !param.isRegist()) {
                    InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(null, param.getStream());
                    if (inviteInfo != null && (inviteInfo.getType() == InviteSessionType.PLAY || inviteInfo.getType() == InviteSessionType.PLAYBACK)) {
                        inviteStreamService.removeInviteInfo(inviteInfo);
                        storager.stopPlay(inviteInfo.getDeviceId(), inviteInfo.getChannelId());
                    }
                } else if ("broadcast".equals(param.getApp())) {
                    // 语音对讲推流  stream需要满足格式deviceId_channelId
                    if (param.getStream().indexOf("_") > 0) {
                        String[] streamArray = param.getStream().split("_");
                        if (streamArray.length == 2) {
                            String deviceId = streamArray[0];
                            String channelId = streamArray[1];
                            Device device = deviceService.getDevice(deviceId);
                            if (device != null) {
                                if (param.isRegist()) {
                                    if (audioBroadcastManager.exit(deviceId, channelId)) {
                                        playService.stopAudioBroadcast(deviceId, channelId);
                                    }
                                    // 开启语音对讲通道
                                    try {
                                        playService.audioBroadcastCmd(device, channelId, mediaInfo, param.getApp(), param.getStream(), 60, false, (msg) -> {
                                            logger.info("[语音对讲] 通道建立成功, device: {}, channel: {}", deviceId, channelId);
                                        });
                                    } catch (InvalidArgumentException | ParseException | SipException e) {
                                        logger.error("[命令发送失败] 语音对讲: {}", e.getMessage());
                                    }
                                } else {
                                    // 流注销
                                    playService.stopAudioBroadcast(deviceId, channelId);
                                }
                            } else {
                                logger.info("[语音对讲] 未找到设备：{}", deviceId);
                            }
                        }
                    }
                } else if ("talk".equals(param.getApp())) {
                    // 语音对讲推流  stream需要满足格式deviceId_channelId
                    if (param.getStream().indexOf("_") > 0) {
                        String[] streamArray = param.getStream().split("_");
                        if (streamArray.length == 2) {
                            String deviceId = streamArray[0];
                            String channelId = streamArray[1];
                            Device device = deviceService.getDevice(deviceId);
                            if (device != null) {
                                if (param.isRegist()) {
                                    if (audioBroadcastManager.exit(deviceId, channelId)) {
                                        playService.stopAudioBroadcast(deviceId, channelId);
                                    }
                                    // 开启语音对讲通道
                                    playService.talkCmd(device, channelId, mediaInfo, param.getStream(), (msg) -> {
                                        logger.info("[语音对讲] 通道建立成功, device: {}, channel: {}", deviceId, channelId);
                                    });
                                } else {
                                    // 流注销
                                    playService.stopTalk(device, channelId, param.isRegist());
                                }
                            } else {
                                logger.info("[语音对讲] 未找到设备：{}", deviceId);
                            }
                        }
                    }

                } else {
                    if (!"rtp".equals(param.getApp())) {
                        String type = OriginType.values()[param.getOriginType()].getType();
                        if (param.isRegist()) {
                            StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(
                                    param.getApp(), param.getStream());
                            String callId = null;
                            if (streamAuthorityInfo != null) {
                                callId = streamAuthorityInfo.getCallId();
                            }
                            StreamInfo streamInfoByAppAndStream = mediaService.getStreamInfoByAppAndStream(mediaInfo,
                                    param.getApp(), param.getStream(), tracks, callId);
                            param.setStreamInfo(new StreamContent(streamInfoByAppAndStream));
                            redisCatchStorage.addStream(mediaInfo, type, param.getApp(), param.getStream(), param);
                            if (param.getOriginType() == OriginType.RTSP_PUSH.ordinal()
                                    || param.getOriginType() == OriginType.RTMP_PUSH.ordinal()
                                    || param.getOriginType() == OriginType.RTC_PUSH.ordinal()) {
                                param.setSeverId(userSetting.getServerId());
                                zlmMediaListManager.addPush(param);

                                // 冗余数据，自己系统中自用
                                redisCatchStorage.addPushListItem(param.getApp(), param.getStream(), param);
                            }
                        } else {
                            // 兼容流注销时类型从redis记录获取
                            OnStreamChangedHookParam onStreamChangedHookParam = redisCatchStorage.getStreamInfo(
                                    param.getApp(), param.getStream(), param.getMediaServerId());
                            if (onStreamChangedHookParam != null) {
                                type = OriginType.values()[onStreamChangedHookParam.getOriginType()].getType();
                                redisCatchStorage.removeStream(mediaInfo.getId(), type, param.getApp(), param.getStream());
                                if ("PUSH".equalsIgnoreCase(type)) {
                                    // 冗余数据，自己系统中自用
                                    redisCatchStorage.removePushListItem(param.getApp(), param.getStream(), param.getMediaServerId());
                                }
                            }
                            GbStream gbStream = storager.getGbStream(param.getApp(), param.getStream());
                            if (gbStream != null) {
//									eventPublisher.catalogEventPublishForStream(null, gbStream, CatalogEvent.OFF);
                            }
                            zlmMediaListManager.removeMedia(param.getApp(), param.getStream());
                        }
                        GbStream gbStream = storager.getGbStream(param.getApp(), param.getStream());
                        if (gbStream != null) {
                            if (userSetting.isUsePushingAsStatus()) {
                                eventPublisher.catalogEventPublishForStream(null, gbStream, param.isRegist() ? CatalogEvent.ON : CatalogEvent.OFF);
                            }
                        }
                        if (type != null) {
                            // 发送流变化redis消息
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("serverId", userSetting.getServerId());
                            jsonObject.put("app", param.getApp());
                            jsonObject.put("stream", param.getStream());
                            jsonObject.put("register", param.isRegist());
                            jsonObject.put("mediaServerId", param.getMediaServerId());
                            redisCatchStorage.sendStreamChangeMsg(type, jsonObject);
                        }
                    }
                }
                if (!param.isRegist()) {
                    List<SendRtpItem> sendRtpItems = redisCatchStorage.querySendRTPServerByStream(param.getStream());
                    if (!sendRtpItems.isEmpty()) {
                        for (SendRtpItem sendRtpItem : sendRtpItems) {
                            if (sendRtpItem == null) {
                                continue;
                            }

                            if (sendRtpItem.getApp().equals(param.getApp())) {
                                logger.info(sendRtpItem.toString());
                                if (userSetting.getServerId().equals(sendRtpItem.getServerId())) {
                                    MessageForPushChannel messageForPushChannel = MessageForPushChannel.getInstance(0,
                                            sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getChannelId(),
                                            sendRtpItem.getPlatformId(), null, userSetting.getServerId(), param.getMediaServerId());
                                    // 通知其他wvp停止发流
                                    redisCatchStorage.sendPushStreamClose(messageForPushChannel);
                                }else {
                                    String platformId = sendRtpItem.getPlatformId();
                                    ParentPlatform platform = storager.queryParentPlatByServerGBId(platformId);
                                    Device device = deviceService.getDevice(platformId);

                                    try {
                                        if (platform != null) {
                                            commanderFroPlatform.streamByeCmd(platform, sendRtpItem);
                                            redisCatchStorage.deleteSendRTPServer(platformId, sendRtpItem.getChannelId(),
                                                    sendRtpItem.getCallId(), sendRtpItem.getStream());
                                        } else {
                                            cmder.streamByeCmd(device, sendRtpItem.getChannelId(), param.getStream(), sendRtpItem.getCallId());
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
                    }
                }
            }
        });
        return HookResult.SUCCESS();
    }

    /**
     * 流无人观看时事件，用户可以通过此事件选择是否关闭无人看的流。
     */
    @ResponseBody
    @PostMapping(value = "/on_stream_none_reader", produces = "application/json;charset=UTF-8")
    public JSONObject onStreamNoneReader(@RequestBody OnStreamNoneReaderHookParam param) {

        logger.info("[ZLM HOOK]流无人观看：{}->{}->{}/{}", param.getMediaServerId(), param.getSchema(),
                param.getApp(), param.getStream());
        JSONObject ret = new JSONObject();
        ret.put("code", 0);
        // 国标类型的流
        if ("rtp".equals(param.getApp())) {
            ret.put("close", userSetting.getStreamOnDemand());
            // 国标流， 点播/录像回放/录像下载
            InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(null, param.getStream());
            // 点播
            if (inviteInfo != null) {
                // 录像下载
                if (inviteInfo.getType() == InviteSessionType.DOWNLOAD) {
                    ret.put("close", false);
                    return ret;
                }
                // 收到无人观看说明流也没有在往上级推送
                if (redisCatchStorage.isChannelSendingRTP(inviteInfo.getChannelId())) {
                    List<SendRtpItem> sendRtpItems = redisCatchStorage.querySendRTPServerByChannelId(
                            inviteInfo.getChannelId());
                    if (!sendRtpItems.isEmpty()) {
                        for (SendRtpItem sendRtpItem : sendRtpItems) {
                            ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(sendRtpItem.getPlatformId());
                            try {
                                commanderFroPlatform.streamByeCmd(parentPlatform, sendRtpItem.getCallId());
                            } catch (SipException | InvalidArgumentException | ParseException e) {
                                logger.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
                            }
                            redisCatchStorage.deleteSendRTPServer(parentPlatform.getServerGBId(), sendRtpItem.getChannelId(),
                                    sendRtpItem.getCallId(), sendRtpItem.getStream());
                            if (InviteStreamType.PUSH == sendRtpItem.getPlayType()) {
                                MessageForPushChannel messageForPushChannel = MessageForPushChannel.getInstance(0,
                                        sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getChannelId(),
                                        sendRtpItem.getPlatformId(), parentPlatform.getName(), userSetting.getServerId(), sendRtpItem.getMediaServerId());
                                messageForPushChannel.setPlatFormIndex(parentPlatform.getId());
                                redisCatchStorage.sendPlatformStopPlayMsg(messageForPushChannel);
                            }
                        }
                    }
                }
                Device device = deviceService.getDevice(inviteInfo.getDeviceId());
                if (device != null) {
                    try {
                        // 多查询一次防止已经被处理了
                        InviteInfo info = inviteStreamService.getInviteInfo(inviteInfo.getType(),
                                inviteInfo.getDeviceId(), inviteInfo.getChannelId(), inviteInfo.getStream());
                        if (info != null) {
                            cmder.streamByeCmd(device, inviteInfo.getChannelId(),
                                    inviteInfo.getStream(), null);
                        } else {
                            logger.info("[无人观看] 未找到设备的点播信息： {}， 流：{}", inviteInfo.getDeviceId(), param.getStream());
                        }
                    } catch (InvalidArgumentException | ParseException | SipException |
                             SsrcTransactionNotFoundException e) {
                        logger.error("[无人观看]点播， 发送BYE失败 {}", e.getMessage());
                    }
                } else {
                    logger.info("[无人观看] 未找到设备： {}，流：{}", inviteInfo.getDeviceId(), param.getStream());
                }

        boolean close = mediaService.closeStreamOnNoneReader(param.getMediaServerId(), param.getApp(), param.getStream(), param.getSchema());
        ret.put("code", close);
        return ret;
    }

    /**
     * 流未找到事件，用户可以在此事件触发时，立即去拉流，这样可以实现按需拉流；此事件对回复不敏感。
     */
    @ResponseBody
    @PostMapping(value = "/on_stream_not_found", produces = "application/json;charset=UTF-8")
    public HookResult onStreamNotFound(@RequestBody OnStreamNotFoundHookParam param) {
        logger.info("[ZLM HOOK] 流未找到：{}->{}->{}/{}", param.getMediaServerId(), param.getSchema(), param.getApp(), param.getStream());


        MediaServer mediaServer = mediaServerService.getOne(param.getMediaServerId());
        if (!userSetting.isAutoApplyPlay() || mediaServer == null) {
            return HookResult.SUCCESS();
        }
        MediaNotFoundEvent mediaNotFoundEvent = MediaNotFoundEvent.getInstance(this, param, mediaServer);
        applicationEventPublisher.publishEvent(mediaNotFoundEvent);
        return HookResult.SUCCESS();
    }

    /**
     * 服务器启动事件，可以用于监听服务器崩溃重启；此事件对回复不敏感。
     */
    @ResponseBody
    @PostMapping(value = "/on_server_started", produces = "application/json;charset=UTF-8")
    public HookResult onServerStarted(HttpServletRequest request, @RequestBody JSONObject jsonObject) {

        jsonObject.put("ip", request.getRemoteAddr());
        ZLMServerConfig zlmServerConfig = JSON.to(ZLMServerConfig.class, jsonObject);
        zlmServerConfig.setIp(request.getRemoteAddr());
        logger.info("[ZLM HOOK] zlm 启动 " + zlmServerConfig.getGeneralMediaServerId());
        try {
            HookZlmServerStartEvent event = new HookZlmServerStartEvent(this);
            MediaServer mediaServerItem = mediaServerService.getOne(zlmServerConfig.getMediaServerId());
            if (mediaServerItem != null) {
                event.setMediaServerItem(mediaServerItem);
                applicationEventPublisher.publishEvent(event);
            }
        }catch (Exception e) {
            logger.info("[ZLM-HOOK-ZLM启动] 发送通知失败 ", e);
        }

        return HookResult.SUCCESS();
    }

    /**
     * 发送rtp(startSendRtp)被动关闭时回调
     */
    @ResponseBody
    @PostMapping(value = "/on_send_rtp_stopped", produces = "application/json;charset=UTF-8")
    public HookResult onSendRtpStopped(HttpServletRequest request, @RequestBody OnSendRtpStoppedHookParam param) {

        logger.info("[ZLM HOOK] rtp发送关闭：{}->{}/{}", param.getMediaServerId(), param.getApp(), param.getStream());

        // 查找对应的上级推流，发送停止
        if (!"rtp".equals(param.getApp())) {
            return HookResult.SUCCESS();
        }
        taskExecutor.execute(() -> {
            List<SendRtpItem> sendRtpItems = redisCatchStorage.querySendRTPServerByStream(param.getStream());
            if (sendRtpItems.size() > 0) {
                for (SendRtpItem sendRtpItem : sendRtpItems) {
                    ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(sendRtpItem.getPlatformId());
                    ssrcFactory.releaseSsrc(sendRtpItem.getMediaServerId(), sendRtpItem.getSsrc());
                    try {
                        commanderFroPlatform.streamByeCmd(parentPlatform, sendRtpItem.getCallId());
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        logger.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
                    }
                    redisCatchStorage.deleteSendRTPServer(parentPlatform.getServerGBId(), sendRtpItem.getChannelId(),
                            sendRtpItem.getCallId(), sendRtpItem.getStream());
                }
            }
        });

        return HookResult.SUCCESS();
    }

    /**
     * rtpServer收流超时
     */
    @ResponseBody
    @PostMapping(value = "/on_rtp_server_timeout", produces = "application/json;charset=UTF-8")
    public HookResult onRtpServerTimeout(@RequestBody OnRtpServerTimeoutHookParam
            param) {
        logger.info("[ZLM HOOK] rtpServer收流超时：{}->{}({})", param.getMediaServerId(), param.getStream_id(), param.getSsrc());

        try {
            MediaRtpServerTimeoutEvent event = new MediaRtpServerTimeoutEvent(this);
            MediaServer mediaServerItem = mediaServerService.getOne(param.getMediaServerId());
            if (mediaServerItem != null) {
                event.setMediaServer(mediaServerItem);
                event.setApp("rtp");
                applicationEventPublisher.publishEvent(event);
            }
        }catch (Exception e) {
            logger.info("[ZLM-HOOK-rtpServer收流超时] 发送通知失败 ", e);
        }

        return HookResult.SUCCESS();
    }

    /**
     * 录像完成事件
     */
    @ResponseBody
    @PostMapping(value = "/on_record_mp4", produces = "application/json;charset=UTF-8")
    public HookResult onRecordMp4(HttpServletRequest request, @RequestBody OnRecordMp4HookParam param) {
        logger.info("[ZLM HOOK] 录像完成事件：{}->{}", param.getMediaServerId(), param.getFile_path());

        try {
            MediaServer mediaServerItem = mediaServerService.getOne(param.getMediaServerId());
            if (mediaServerItem != null) {
                MediaRecordMp4Event event = MediaRecordMp4Event.getInstance(this, param, mediaServerItem);
                event.setMediaServer(mediaServerItem);
                applicationEventPublisher.publishEvent(event);
            }
        }catch (Exception e) {
            logger.info("[ZLM-HOOK-rtpServer收流超时] 发送通知失败 ", e);
        }

        return HookResult.SUCCESS();
    }

    private Map<String, String> urlParamToMap(String params) {
        HashMap<String, String> map = new HashMap<>();
        if (ObjectUtils.isEmpty(params)) {
            return map;
        }
        String[] paramsArray = params.split("&");
        if (paramsArray.length == 0) {
            return map;
        }
        for (String param : paramsArray) {
            String[] paramArray = param.split("=");
            if (paramArray.length == 2) {
                map.put(paramArray[0], paramArray[1]);
            }
        }
        return map;
    }
}
