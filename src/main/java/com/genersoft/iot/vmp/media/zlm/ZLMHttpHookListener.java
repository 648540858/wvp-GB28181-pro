package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.media.zlm.dto.*;
import com.genersoft.iot.vmp.media.zlm.dto.HookType;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.media.zlm.dto.hook.*;
import com.genersoft.iot.vmp.service.*;
import com.genersoft.iot.vmp.service.bean.MessageForPushChannel;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.OtherPsSendInfo;
import com.genersoft.iot.vmp.vmanager.bean.OtherRtpSendInfo;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletRequest;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    private ZLMServerFactory zlmServerFactory;

    @Autowired
    private IPlayService playService;

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IStreamSendManager streamSendManager;

    @Autowired
    private IInviteStreamService inviteStreamService;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private IStreamProxyService streamProxyService;

    @Autowired
    private IStreamPushService streamPushService;

    @Autowired
    private DeferredResultHolder resultHolder;

    @Autowired
    private IMediaService mediaService;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private ZLMMediaListManager zlmMediaListManager;

    @Autowired
    private ZlmHttpHookSubscribe subscribe;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IUserService userService;

    @Autowired
    private ICloudRecordService cloudRecordService;

    @Autowired
    private VideoStreamSessionManager sessionManager;

    @Autowired
    private AssistRESTfulUtils assistRESTfulUtils;

    @Autowired
    private SSRCFactory ssrcFactory;

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    /**
     * 服务器定时上报时间，上报间隔可配置，默认10s上报一次
     */
    @ResponseBody

    @PostMapping(value = "/on_server_keepalive", produces = "application/json;charset=UTF-8")
    public HookResult onServerKeepalive(@RequestBody OnServerKeepaliveHookParam param) {


        taskExecutor.execute(() -> {
            List<ZlmHttpHookSubscribe.Event> subscribes = this.subscribe.getSubscribes(HookType.on_server_keepalive);
            if (subscribes != null && subscribes.size() > 0) {
                for (ZlmHttpHookSubscribe.Event subscribe : subscribes) {
                    subscribe.response(null, param);
                }
            }
        });
        mediaServerService.updateMediaServerKeepalive(param.getMediaServerId(), param.getData());

        return HookResult.SUCCESS();
    }

    /**
     * 播放器鉴权事件，rtsp/rtmp/http-flv/ws-flv/hls的播放都将触发此鉴权事件。
     */
    @ResponseBody

    @PostMapping(value = "/on_play", produces = "application/json;charset=UTF-8")
    public HookResult onPlay(@RequestBody OnPlayHookParam param) {
        if (logger.isDebugEnabled()) {
            logger.debug("[ZLM HOOK] 播放鉴权：{}->{}" + param.getMediaServerId(), param);
        }
        String mediaServerId = param.getMediaServerId();

        taskExecutor.execute(() -> {
            JSONObject json = (JSONObject) JSON.toJSON(param);
            ZlmHttpHookSubscribe.Event subscribe = this.subscribe.sendNotify(HookType.on_play, json);
            if (subscribe != null) {
                MediaServerItem mediaInfo = mediaServerService.getOne(mediaServerId);
                if (mediaInfo != null) {
                    subscribe.response(mediaInfo, param);
                }
            }
        });
        if (!"rtp".equals(param.getApp())) {
            Map<String, String> paramMap = urlParamToMap(param.getParams());
            StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(param.getApp(), param.getStream());
            if (streamAuthorityInfo != null && streamAuthorityInfo.getCallId() != null && !streamAuthorityInfo.getCallId().equals(paramMap.get("callId"))) {
                return new HookResult(401, "Unauthorized");
            }
        }

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

        String mediaServerId = json.getString("mediaServerId");
        MediaServerItem mediaInfo = mediaServerService.getOne(mediaServerId);
        if (mediaInfo == null) {
            return new HookResultForOnPublish(200, "success");
        }
        // 推流鉴权的处理
        if (!"rtp".equals(param.getApp())) {
            StreamProxy stream = streamProxyService.getStreamProxyByAppAndStream(param.getApp(), param.getStream());
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
                if (mediaInfo != null) {
                    subscribe.response(mediaInfo, param);
                } else {
                    new HookResultForOnPublish(1, "zlm not register");
                }
            }
        });

        // 是否录像
        if ("rtp".equals(param.getApp())) {
            result.setEnable_mp4(userSetting.getRecordSip());
        } else {
            result.setEnable_mp4(userSetting.isRecordPushLive());
        }
        // 国标流
        if ("rtp".equals(param.getApp())) {

            InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(null, param.getStream());

            // 单端口模式下修改流 ID
            if (!mediaInfo.isRtpEnable() && inviteInfo == null) {
                String ssrc = String.format("%010d", Long.parseLong(param.getStream(), 16));
                inviteInfo = inviteStreamService.getInviteInfoBySSRC(ssrc);
                if (inviteInfo != null) {
                    result.setStream_replace(inviteInfo.getStream());
                    logger.info("[ZLM HOOK] 推流鉴权 stream: {} 替换为 {}", param.getStream(), inviteInfo.getStream());
                }
            }

            // 设置音频信息及录制信息
            List<SsrcTransaction> ssrcTransactionForAll = sessionManager.getSsrcTransactionForAll(null, null, null, param.getStream());
            if (ssrcTransactionForAll != null && ssrcTransactionForAll.size() == 1) {

                // 为录制国标模拟一个鉴权信息, 方便后续写入录像文件时使用
                StreamAuthorityInfo streamAuthorityInfo = StreamAuthorityInfo.getInstanceByHook(param);
                streamAuthorityInfo.setApp(param.getApp());
                streamAuthorityInfo.setStream(ssrcTransactionForAll.get(0).getStream());
                streamAuthorityInfo.setCallId(ssrcTransactionForAll.get(0).getSipTransactionInfo().getCallId());

                redisCatchStorage.updateStreamAuthorityInfo(param.getApp(), ssrcTransactionForAll.get(0).getStream(), streamAuthorityInfo);

                String deviceId = ssrcTransactionForAll.get(0).getDeviceId();
                String channelId = ssrcTransactionForAll.get(0).getChannelId();
                DeviceChannel deviceChannel = storager.queryChannel(deviceId, channelId);
                if (deviceChannel != null) {
                    result.setEnable_audio(deviceChannel.isHasAudio());
                }
                // 如果是录像下载就设置视频间隔十秒
                if (ssrcTransactionForAll.get(0).getType() == InviteSessionType.DOWNLOAD) {
                    // 获取录像的总时长，然后设置为这个视频的时长
                    InviteInfo inviteInfoForDownload = inviteStreamService.getInviteInfo(InviteSessionType.DOWNLOAD, deviceId, channelId, param.getStream());
                    if (inviteInfoForDownload != null && inviteInfoForDownload.getStreamInfo() != null) {
                        String startTime = inviteInfoForDownload.getStreamInfo().getStartTime();
                        String endTime = inviteInfoForDownload.getStreamInfo().getEndTime();
                        long difference = DateUtil.getDifference(startTime, endTime) / 1000;
                        result.setMp4_max_second((int) difference);
                        result.setEnable_mp4(true);
                        // 设置为2保证得到的mp4的时长是正常的
                        result.setModify_stamp(2);
                    }
                }
                // 如果是talk对讲，则默认获取声音
                if (ssrcTransactionForAll.get(0).getType() == InviteSessionType.TALK) {
                    result.setEnable_audio(true);
                }
            }
        } else if (param.getApp().equals("broadcast")) {
            result.setEnable_audio(true);
        } else if (param.getApp().equals("talk")) {
            result.setEnable_audio(true);
        }
        if (param.getApp().equalsIgnoreCase("rtp")) {
            String receiveKey = VideoManagerConstants.WVP_OTHER_RECEIVE_RTP_INFO + userSetting.getServerId() + "_" + param.getStream();
            OtherRtpSendInfo otherRtpSendInfo = (OtherRtpSendInfo) redisTemplate.opsForValue().get(receiveKey);

            String receiveKeyForPS = VideoManagerConstants.WVP_OTHER_RECEIVE_PS_INFO + userSetting.getServerId() + "_" + param.getStream();
            OtherPsSendInfo otherPsSendInfo = (OtherPsSendInfo) redisTemplate.opsForValue().get(receiveKeyForPS);
            if (otherRtpSendInfo != null || otherPsSendInfo != null) {
                result.setEnable_mp4(true);
            }
        }
        logger.info("[ZLM HOOK] 推流鉴权 响应：{}->{} \r\n{}", param.getMediaServerId(), param, result);
        return result;
    }


    /**
     * rtsp/rtmp流注册或注销时触发此事件；此事件对回复不敏感。
     */
    @ResponseBody
    @PostMapping(value = "/on_stream_changed", produces = "application/json;charset=UTF-8")
    public HookResult onStreamChanged(@RequestBody OnStreamChangedHookParam param) {

        if (param.isRegist()) {
            logger.info("[ZLM HOOK] 流注册, {}->{}->{}->{}/{}", param.getMediaServerId(), param.getSchema(),
                    OriginType.values()[param.getOriginType()].getType(), param.getApp(), param.getStream());
        } else {
            logger.info("[ZLM HOOK] 流注销, {}->{}->{}->{}/{}", param.getMediaServerId(), param.getSchema(),
                    OriginType.values()[param.getOriginType()].getType(), param.getApp(), param.getStream());
        }

        JSONObject json = (JSONObject) JSON.toJSON(param);
        taskExecutor.execute(() -> {
            // 发送hook订阅通知
            ZlmHttpHookSubscribe.Event subscribe = this.subscribe.sendNotify(HookType.on_stream_changed, json);
            MediaServerItem mediaInfo = mediaServerService.getOne(param.getMediaServerId());
            if (mediaInfo == null) {
                logger.info("[ZLM HOOK] 流变化未找到ZLM, {}", param.getMediaServerId());
                return;
            }
            if (subscribe != null) {
                subscribe.response(mediaInfo, param);
            }

            List<StreamMediaTrack> tracks = param.getTracks();
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

                streamProxyService.updatePullingStatus(param.isRegist(), param.getApp(), param.getStream());
                if ("rtp".equals(param.getApp())) {
                    if (!param.isRegist()) {
                        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(null, param.getStream());
                        if (inviteInfo != null && (inviteInfo.getType() == InviteSessionType.PLAY || inviteInfo.getType() == InviteSessionType.PLAYBACK)) {
                            inviteStreamService.removeInviteInfo(inviteInfo);
                            storager.stopPlay(inviteInfo.getDeviceId(), inviteInfo.getChannelId());
                        }
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
                    String type;
                    if (param.getOriginType() == 0) {
                        // 源类型为unknown，则主动查询类型
                        type = mediaService.getStreamType(param.getApp(), param.getStream());
                    }else {
                        type = OriginType.values()[param.getOriginType()].getType();
                    }
                    if (type != null) {
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
                                    streamPushService.offline(param.getApp(), param.getStream());
                                }
                            }
                        }
                        // 设置拉流代理拉流状态
                        streamProxyService.updatePullingStatus(param.isRegist(), param.getApp(), param.getStream());
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
                if (!param.isRegist()) {
                    List<SendRtpItem> sendRtpItems = streamSendManager.getByAppAndStream(param.getApp(), param.getStream());
                    if (sendRtpItems != null && !sendRtpItems.isEmpty()) {
                        for (SendRtpItem sendRtpItem : sendRtpItems) {
                            if (sendRtpItem != null && sendRtpItem.getApp().equals(param.getApp())) {
                                String platformId = sendRtpItem.getDestId();
                                ParentPlatform platform = storager.queryParentPlatByServerGBId(platformId);
                                Device device = deviceService.getDevice(platformId);

                                try {
                                    if (platform != null) {
                                        commanderFroPlatform.streamByeCmd(platform, sendRtpItem);
                                        streamSendManager.remove(sendRtpItem);
                                    } else {
                                        cmder.streamByeCmd(device, sendRtpItem.getChannelId(), param.getStream(), sendRtpItem.getCallId());
                                        if (sendRtpItem.getPlayType().equals(InviteStreamType.BROADCAST)
                                                || sendRtpItem.getPlayType().equals(InviteStreamType.TALK)) {
                                            AudioBroadcastCatch audioBroadcastCatch = audioBroadcastManager.get(sendRtpItem.getDestId(), sendRtpItem.getChannelId());
                                            if (audioBroadcastCatch != null) {
                                                // 来自上级平台的停止对讲
                                                logger.info("[停止对讲] 来自上级，平台：{}, 通道：{}", sendRtpItem.getDestId(), sendRtpItem.getChannelId());
                                                audioBroadcastManager.del(sendRtpItem.getDestId(), sendRtpItem.getChannelId());
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
                List<SendRtpItem> sendRtpItems = streamSendManager.getByByChanelId(inviteInfo.getChannelId());
                if (sendRtpItems != null && !sendRtpItems.isEmpty()) {
                    for (SendRtpItem sendRtpItem : sendRtpItems) {
                        ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(sendRtpItem.getDestId());
                        try {
                            commanderFroPlatform.streamByeCmd(parentPlatform, sendRtpItem.getCallId());
                        } catch (SipException | InvalidArgumentException | ParseException e) {
                            logger.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
                        }
                        streamSendManager.remove(sendRtpItem);
                        if (InviteStreamType.PUSH == sendRtpItem.getPlayType()) {
                            MessageForPushChannel messageForPushChannel = MessageForPushChannel.getInstance(0,
                                    sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getChannelId(),
                                    sendRtpItem.getDestId(), parentPlatform.getName(), userSetting.getServerId(), sendRtpItem.getMediaServerId());
                            messageForPushChannel.setPlatFormIndex(parentPlatform.getId());
                            redisCatchStorage.sendPlatformStopPlayMsg(messageForPushChannel);
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

                inviteStreamService.removeInviteInfo(inviteInfo.getType(), inviteInfo.getDeviceId(),
                        inviteInfo.getChannelId(), inviteInfo.getStream());
                storager.stopPlay(inviteInfo.getDeviceId(), inviteInfo.getChannelId());
                return ret;
            }
            List<SendRtpItem> sendRtpItemList = streamSendManager.getByAppAndStream(param.getApp(), param.getStream());
            if (sendRtpItemList != null && !sendRtpItemList.isEmpty()) {
                for (SendRtpItem sendRtpItem : sendRtpItemList) {
                    if (sendRtpItem != null && "talk".equals(sendRtpItem.getApp())) {
                        ret.put("close", false);
                        return ret;
                    }
                }
            }
        } else if ("talk".equals(param.getApp()) || "broadcast".equals(param.getApp())) {
            ret.put("close", false);
        } else {
            // 非国标流 推流/拉流代理
            // 拉流代理
            StreamProxy streamProxyItem = streamProxyService.getStreamProxyByAppAndStream(param.getApp(), param.getStream());
            if (streamProxyItem != null) {
                if (streamProxyItem.isEnableRemoveNoneReader()) {
                    // 无人观看自动移除
                    ret.put("close", true);
                    streamProxyService.removeProxy(streamProxyItem.getId());
                    logger.info("[{}/{}]<-[{}] 拉流代理无人观看已经移除", param.getApp(), param.getStream(), streamProxyItem.getUrl());
                } else if (streamProxyItem.isEnableDisableNoneReader()) {
                    // 无人观看停用
                    ret.put("close", true);
                    // 修改数据
                    streamProxyService.stop(param.getApp(), param.getStream(), null);
                } else {
                    // 无人观看不做处理
                    ret.put("close", false);
                }
                return ret;
            }
        }
        return ret;
    }

    /**
     * 流未找到事件，用户可以在此事件触发时，立即去拉流，这样可以实现按需拉流；此事件对回复不敏感。
     */
    @ResponseBody
    @PostMapping(value = "/on_stream_not_found", produces = "application/json;charset=UTF-8")
    public DeferredResult<HookResult> onStreamNotFound(@RequestBody OnStreamNotFoundHookParam param) {
        logger.info("[ZLM HOOK] 流未找到：{}->{}->{}/{}", param.getMediaServerId(), param.getSchema(), param.getApp(), param.getStream());

        DeferredResult<HookResult> defaultResult = new DeferredResult<>();

        MediaServerItem mediaInfo = mediaServerService.getOne(param.getMediaServerId());
        if (!userSetting.isAutoApplyPlay() || mediaInfo == null) {
            defaultResult.setResult(new HookResult(ErrorCode.ERROR404.getCode(), ErrorCode.ERROR404.getMsg()));
            return defaultResult;
        }

        if ("rtp".equals(param.getApp())) {
            String[] s = param.getStream().split("_");
            if ((s.length != 2 && s.length != 4)) {
                defaultResult.setResult(HookResult.SUCCESS());
                return defaultResult;
            }
            String deviceId = s[0];
            String channelId = s[1];
            Device device = redisCatchStorage.getDevice(deviceId);
            if (device == null || !device.isOnLine()) {
                defaultResult.setResult(new HookResult(ErrorCode.ERROR404.getCode(), ErrorCode.ERROR404.getMsg()));
                return defaultResult;
            }
            DeviceChannel deviceChannel = storager.queryChannel(deviceId, channelId);
            if (deviceChannel == null) {
                defaultResult.setResult(new HookResult(ErrorCode.ERROR404.getCode(), ErrorCode.ERROR404.getMsg()));
                return defaultResult;
            }
            if (s.length == 2) {
                logger.info("[ZLM HOOK] 预览流未找到, 发起自动点播：{}->{}->{}/{}", param.getMediaServerId(), param.getSchema(), param.getApp(), param.getStream());

                RequestMessage msg = new RequestMessage();
                String key = DeferredResultHolder.CALLBACK_CMD_PLAY + deviceId + channelId;
                boolean exist = resultHolder.exist(key, null);
                msg.setKey(key);
                String uuid = UUID.randomUUID().toString();
                msg.setId(uuid);
                DeferredResult<HookResult> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());

                result.onTimeout(() -> {
                    logger.info("[ZLM HOOK] 预览流自动点播, 等待超时");
                    msg.setData(new HookResult(ErrorCode.ERROR100.getCode(), "点播超时"));
                    resultHolder.invokeAllResult(msg);
                    inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, deviceId, channelId);
                    storager.stopPlay(deviceId, channelId);
                });

                resultHolder.put(key, uuid, result);

                if (!exist) {
                    playService.play(mediaInfo, deviceId, channelId, null, (code, message, data) -> {
                        msg.setData(new HookResult(code, message));
                        resultHolder.invokeResult(msg);
                    });
                }
                return result;
            } else if (s.length == 4) {
                // 此时为录像回放， 录像回放格式为> 设备ID_通道ID_开始时间_结束时间
                String startTimeStr = s[2];
                String endTimeStr = s[3];
                if (startTimeStr == null || endTimeStr == null || startTimeStr.length() != 14 || endTimeStr.length() != 14) {
                    defaultResult.setResult(HookResult.SUCCESS());
                    return defaultResult;
                }
                String startTime = DateUtil.urlToyyyy_MM_dd_HH_mm_ss(startTimeStr);
                String endTime = DateUtil.urlToyyyy_MM_dd_HH_mm_ss(endTimeStr);
                logger.info("[ZLM HOOK] 回放流未找到, 发起自动点播：{}->{}->{}/{}-{}-{}",
                        param.getMediaServerId(), param.getSchema(),
                        param.getApp(), param.getStream(),
                        startTime, endTime
                );
                RequestMessage msg = new RequestMessage();
                String key = DeferredResultHolder.CALLBACK_CMD_PLAYBACK + deviceId + channelId;
                boolean exist = resultHolder.exist(key, null);
                msg.setKey(key);
                String uuid = UUID.randomUUID().toString();
                msg.setId(uuid);
                DeferredResult<HookResult> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());

                result.onTimeout(() -> {
                    logger.info("[ZLM HOOK] 回放流自动点播, 等待超时");
                    // 释放rtpserver
                    msg.setData(new HookResult(ErrorCode.ERROR100.getCode(), "点播超时"));
                    resultHolder.invokeResult(msg);
                });

                resultHolder.put(key, uuid, result);

                if (!exist) {
                    SSRCInfo ssrcInfo = mediaServerService.openRTPServer(mediaInfo, param.getStream(), null,
                            device.isSsrcCheck(), true, 0, false, false, device.getStreamModeForParam());
                    playService.playBack(mediaInfo, ssrcInfo, deviceId, channelId, startTime, endTime, (code, message, data) -> {
                        msg.setData(new HookResult(code, message));
                        resultHolder.invokeResult(msg);
                    });
                }
                return result;
            } else {
                defaultResult.setResult(HookResult.SUCCESS());
                return defaultResult;
            }

        } else {
            // 拉流代理
            StreamProxy streamProxyByAppAndStream = streamProxyService.getStreamProxyByAppAndStream(param.getApp(), param.getStream());
            if (streamProxyByAppAndStream != null && streamProxyByAppAndStream.isEnableDisableNoneReader()) {
                streamProxyService.start(param.getApp(), param.getStream(), null);
            }
            DeferredResult<HookResult> result = new DeferredResult<>();
            result.setResult(HookResult.SUCCESS());
            return result;
        }
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
        taskExecutor.execute(() -> {
            List<ZlmHttpHookSubscribe.Event> subscribes = this.subscribe.getSubscribes(HookType.on_server_started);
            if (subscribes != null && subscribes.size() > 0) {
                for (ZlmHttpHookSubscribe.Event subscribe : subscribes) {
                    subscribe.response(null, zlmServerConfig);
                }
            }
            mediaServerService.zlmServerOnline(zlmServerConfig);
        });

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
            List<SendRtpItem> sendRtpItems = streamSendManager.getByAppAndStream(param.getApp(), param.getStream());
            if (sendRtpItems != null && !sendRtpItems.isEmpty()) {
                for (SendRtpItem sendRtpItem : sendRtpItems) {
                    ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(sendRtpItem.getDestId());
                    ssrcFactory.releaseSsrc(sendRtpItem.getMediaServerId(), sendRtpItem.getSsrc());
                    try {
                        commanderFroPlatform.streamByeCmd(parentPlatform, sendRtpItem.getCallId());
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        logger.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
                    }
                    streamSendManager.remove(sendRtpItem);
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
    public HookResult onRtpServerTimeout(HttpServletRequest request, @RequestBody OnRtpServerTimeoutHookParam
            param) {
        logger.info("[ZLM HOOK] rtpServer收流超时：{}->{}({})", param.getMediaServerId(), param.getStream_id(), param.getSsrc());

        taskExecutor.execute(() -> {
            JSONObject json = (JSONObject) JSON.toJSON(param);
            List<ZlmHttpHookSubscribe.Event> subscribes = this.subscribe.getSubscribes(HookType.on_rtp_server_timeout);
            if (subscribes != null && !subscribes.isEmpty()) {
                for (ZlmHttpHookSubscribe.Event subscribe : subscribes) {
                    subscribe.response(null, param);
                }
            }
        });

        return HookResult.SUCCESS();
    }

    /**
     * 录像完成事件
     */
    @ResponseBody
    @PostMapping(value = "/on_record_mp4", produces = "application/json;charset=UTF-8")
    public HookResult onRecordMp4(HttpServletRequest request, @RequestBody OnRecordMp4HookParam param) {
        logger.info("[ZLM HOOK] 录像完成事件：{}->{}", param.getMediaServerId(), param.getFile_path());

        taskExecutor.execute(() -> {
            List<ZlmHttpHookSubscribe.Event> subscribes = this.subscribe.getSubscribes(HookType.on_record_mp4);
            if (subscribes != null && !subscribes.isEmpty()) {
                for (ZlmHttpHookSubscribe.Event subscribe : subscribes) {
                    subscribe.response(null, param);
                }
            }
            cloudRecordService.addRecord(param);

        });

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
