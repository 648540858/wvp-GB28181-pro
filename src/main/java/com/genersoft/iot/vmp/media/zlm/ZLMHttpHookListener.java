package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.bean.ResultForOnPublish;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.media.*;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaSendRtpStoppedEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.dto.ZLMServerConfig;
import com.genersoft.iot.vmp.media.zlm.dto.hook.*;
import com.genersoft.iot.vmp.media.zlm.event.HookZlmServerKeepaliveEvent;
import com.genersoft.iot.vmp.media.zlm.event.HookZlmServerStartEvent;
import com.genersoft.iot.vmp.service.*;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushService;
import com.genersoft.iot.vmp.utils.MediaServerUtils;
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

        Map<String, String> paramMap = MediaServerUtils.urlParamToMap(param.getParams());
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
//    @ResponseBody
//    @PostMapping(value = "/on_stream_changed", produces = "application/json;charset=UTF-8")
//    public HookResult onStreamChanged(@RequestBody JSONObject param) {
//        System.out.println(11);
//        return HookResult.SUCCESS();
//    }
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
        if (!ObjectUtils.isEmpty(mediaServer.getTranscodeSuffix())
                && !"null".equalsIgnoreCase(mediaServer.getTranscodeSuffix())
                && param.getStream().endsWith(mediaServer.getTranscodeSuffix())  ) {
            return HookResult.SUCCESS();
        }
        if (param.getSchema().equalsIgnoreCase("rtsp")) {
            if (param.isRegist()) {
                logger.info("[ZLM HOOK] 流注册, {}->{}->{}/{}", param.getMediaServerId(), param.getSchema(), param.getApp(), param.getStream());
                MediaArrivalEvent mediaArrivalEvent = MediaArrivalEvent.getInstance(this, param, mediaServer);
                applicationEventPublisher.publishEvent(mediaArrivalEvent);
            } else {
                logger.info("[ZLM HOOK] 流注销, {}->{}->{}/{}", param.getMediaServerId(), param.getSchema(), param.getApp(), param.getStream());
                MediaDepartureEvent mediaDepartureEvent = MediaDepartureEvent.getInstance(this, param, mediaServer);
                applicationEventPublisher.publishEvent(mediaDepartureEvent);
            }
        }

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

        MediaServer mediaInfo = mediaServerService.getOne(param.getMediaServerId());
        if (mediaInfo == null) {
            JSONObject ret = new JSONObject();
            ret.put("code", 0);
            return ret;
        }
        if (!ObjectUtils.isEmpty(mediaInfo.getTranscodeSuffix())
                && !"null".equalsIgnoreCase(mediaInfo.getTranscodeSuffix())
                && param.getStream().endsWith(mediaInfo.getTranscodeSuffix())  ) {
            param.setStream(param.getStream().substring(0, param.getStream().lastIndexOf(mediaInfo.getTranscodeSuffix()) -1 ));
        }

        JSONObject ret = new JSONObject();
        boolean close = mediaService.closeStreamOnNoneReader(param.getMediaServerId(), param.getApp(), param.getStream(), param.getSchema());
        ret.put("code", 0);
        ret.put("close", close);
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
        try {
            MediaSendRtpStoppedEvent event = new MediaSendRtpStoppedEvent(this);
            MediaServer mediaServerItem = mediaServerService.getOne(param.getMediaServerId());
            if (mediaServerItem != null) {
                event.setMediaServer(mediaServerItem);
                applicationEventPublisher.publishEvent(event);
            }
        }catch (Exception e) {
            logger.info("[ZLM-HOOK-rtp发送关闭] 发送通知失败 ", e);
        }

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
}
