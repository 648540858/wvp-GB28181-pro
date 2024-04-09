package com.genersoft.iot.vmp.media.abl;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.media.abl.bean.hook.*;
import com.genersoft.iot.vmp.media.abl.event.HookAblServerKeepaliveEvent;
import com.genersoft.iot.vmp.media.abl.event.HookAblServerStartEvent;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.bean.ResultForOnPublish;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.media.MediaArrivalEvent;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.media.event.media.MediaNotFoundEvent;
import com.genersoft.iot.vmp.media.event.media.MediaRtpServerTimeoutEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.ZLMMediaListManager;
import com.genersoft.iot.vmp.media.zlm.dto.hook.HookResult;
import com.genersoft.iot.vmp.media.zlm.dto.hook.HookResultForOnPublish;
import com.genersoft.iot.vmp.service.*;
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
 * ABL 的hook事件监听
 */
@RestController
@RequestMapping("/index/hook/abl")
public class ABLHttpHookListener {

    private final static Logger logger = LoggerFactory.getLogger(ABLHttpHookListener.class);

    @Autowired
    private ABLRESTfulUtils ablresTfulUtils;

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
    private ZLMMediaListManager zlmMediaListManager;

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

    /**
     * 服务器定时上报时间，上报间隔可配置，默认10s上报一次
     */
    @ResponseBody
    @PostMapping(value = "/on_server_keepalive", produces = "application/json;charset=UTF-8")
    public HookResult onServerKeepalive(@RequestBody OnServerKeepaliveABLHookParam param) {
        try {
            HookAblServerKeepaliveEvent event = new HookAblServerKeepaliveEvent(this);
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
    public HookResult onPlay(@RequestBody OnPlayABLHookParam param) {

        MediaServer mediaServer = mediaServerService.getOne(param.getMediaServerId());
        if (mediaServer == null) {
            return new HookResultForOnPublish(0, "success");
        }

        Map<String, String> paramMap = urlParamToMap(param.getParams());
        // 对于播放流进行鉴权
        boolean authenticateResult = mediaService.authenticatePlay(param.getApp(), param.getStream(), paramMap.get("callId"));
        if (!authenticateResult) {
            logger.info("[ABL HOOK] 播放鉴权 失败：{}->{}", param.getMediaServerId(), param);
            ablresTfulUtils.closeStreams(mediaServer, param.getApp(), param.getStream());

        }
        logger.info("[ABL HOOK] 播放鉴权成功：{}->{}", param.getMediaServerId(), param);
        return HookResult.SUCCESS();
    }

    /**
     * rtsp/rtmp/rtp推流鉴权事件。
     */
    @ResponseBody
    @PostMapping(value = "/on_publish", produces = "application/json;charset=UTF-8")
    public HookResult onPublish(@RequestBody OnPublishABLHookParam param) {


        logger.info("[ABL HOOK] 推流鉴权：{}->{}", param.getMediaServerId(), param);
        // TODO 加快处理速度

        MediaServer mediaServer = mediaServerService.getOne(param.getMediaServerId());
        if (mediaServer == null) {
            return new HookResultForOnPublish(0, "success");
        }

        ResultForOnPublish resultForOnPublish = mediaService.authenticatePublish(mediaServer, param.getApp(), param.getStream(), param.getParams());
        if (resultForOnPublish == null) {
            logger.info("[ABL HOOK]推流鉴权 拒绝 响应：{}->{}", param.getMediaServerId(), param);
            ablresTfulUtils.closeStreams(mediaServer, param.getApp(), param.getStream());
        }
        return HookResult.SUCCESS();
    }

    /**
     * 如果某一个码流进行MP4录像（enable_mp4=1），会触发录像进度通知事件
     */
    @ResponseBody
    @PostMapping(value = "/on_record_progress", produces = "application/json;charset=UTF-8")
    public HookResult onRecordProgress(@RequestBody OnRecordProgressABLHookParam param) {


        logger.info("[ABL HOOK] 录像进度通知：{}->{}/{}->{}/{}", param.getMediaServerId(), param.getApp(), param.getStream(), param.getCurrentFileDuration(), param.getTotalVideoDuration());

        // TODO 这里用来做录像进度
//        MediaServer mediaServer = mediaServerService.getOne(param.getMediaServerId());
//        if (mediaServer == null) {
//            return new HookResultForOnPublish(0, "success");
//        }
//
//        ResultForOnPublish resultForOnPublish = mediaService.authenticatePublish(mediaServer, param.getApp(), param.getStream(), param.getParams());
//        if (resultForOnPublish == null) {
//            logger.info("[ABL HOOK]推流鉴权 拒绝 响应：{}->{}", param.getMediaServerId(), param);
//            ablresTfulUtils.closeStreams(mediaServer, param.getApp(), param.getStream());
//        }
        return HookResult.SUCCESS();
    }

    /**
     * 当代理拉流、国标接入等等 码流不到达时会发出 码流不到达的事件通知
     */
    @ResponseBody
    @PostMapping(value = "/on_stream_not_arrive", produces = "application/json;charset=UTF-8")
    public HookResult onStreamNotArrive(@RequestBody ABLHookParam param) {


        logger.info("[ABL HOOK] 码流不到达通知：{}->{}/{}", param.getMediaServerId(), param.getApp(), param.getStream());
        try {
            if ("rtp".equals(param.getApp())) {
                return HookResult.SUCCESS();
            }
            MediaRtpServerTimeoutEvent event = new MediaRtpServerTimeoutEvent(this);
            MediaServer mediaServerItem = mediaServerService.getOne(param.getMediaServerId());
            if (mediaServerItem != null) {
                event.setMediaServer(mediaServerItem);
                event.setApp("rtp");
                applicationEventPublisher.publishEvent(event);
            }
        }catch (Exception e) {
            logger.info("[ABL-HOOK-码流不到达通知] 发送通知失败 ", e);
        }

        return HookResult.SUCCESS();
    }

    /**
     * 如果某一个码流进行MP4录像（enable_mp4=1），当某个MP4文件被删除会触发该事件通知
     */
    @ResponseBody
    @PostMapping(value = "/on_delete_record_mp4", produces = "application/json;charset=UTF-8")
    public HookResult onDeleteRecordMp4(@RequestBody OnRecordMp4ABLHookParam param) {


        logger.info("[ABL HOOK] MP4文件被删除通知：{}->{}/{}", param.getMediaServerId(), param.getApp(), param.getStream());


        return HookResult.SUCCESS();
    }


    /**
     * rtsp/rtmp流注册或注销时触发此事件；此事件对回复不敏感。
     */
    @ResponseBody
    @PostMapping(value = "/on_stream_arrive", produces = "application/json;charset=UTF-8")
    public HookResult onStreamArrive(@RequestBody OnStreamArriveABLHookParam param) {

        MediaServer mediaServer = mediaServerService.getOne(param.getMediaServerId());
        if (mediaServer == null) {
            return HookResult.SUCCESS();
        }

        logger.info("[ABL HOOK] 码流到达, {}->{}/{}", param.getMediaServerId(), param.getApp(), param.getStream());
        MediaArrivalEvent mediaArrivalEvent = MediaArrivalEvent.getInstance(this, param, mediaServer);
        applicationEventPublisher.publishEvent(mediaArrivalEvent);
        return HookResult.SUCCESS();
    }

    /**
     * 流无人观看时事件，用户可以通过此事件选择是否关闭无人看的流。
     */
    @ResponseBody
    @PostMapping(value = "/on_stream_none_reader", produces = "application/json;charset=UTF-8")
    public JSONObject onStreamNoneReader(@RequestBody ABLHookParam param) {

        logger.info("[ZLM HOOK]流无人观看：{}->{}/{}", param.getMediaServerId(),
                param.getApp(), param.getStream());
        JSONObject ret = new JSONObject();

        boolean close = mediaService.closeStreamOnNoneReader(param.getMediaServerId(), param.getApp(), param.getStream(), null);
        ret.put("code", close);
        return ret;
    }

    /**
     * 当播放一个url，如果不存在时，会发出一个消息通知
     */
    @ResponseBody
    @PostMapping(value = "/on_stream_not_found", produces = "application/json;charset=UTF-8")
    public HookResult onStreamNotFound(@RequestBody ABLHookParam param) {
        logger.info("[ABL HOOK] 流未找到：{}->{}/{}", param.getMediaServerId(), param.getApp(), param.getStream());


        MediaServer mediaServer = mediaServerService.getOne(param.getMediaServerId());
        if (!userSetting.isAutoApplyPlay() || mediaServer == null) {
            return HookResult.SUCCESS();
        }
        MediaNotFoundEvent mediaNotFoundEvent = MediaNotFoundEvent.getInstance(this, param, mediaServer);
        applicationEventPublisher.publishEvent(mediaNotFoundEvent);
        return HookResult.SUCCESS();
    }

    /**
     * ABLMediaServer启动时会发送上线通知
     */
    @ResponseBody
    @PostMapping(value = "/on_server_started", produces = "application/json;charset=UTF-8")
    public HookResult onServerStarted(HttpServletRequest request, @RequestBody OnServerStaredABLHookParam param) {

        logger.info("[ABL HOOK] 启动 " + param.getMediaServerId());
        try {
            HookAblServerStartEvent event = new HookAblServerStartEvent(this);
            MediaServer mediaServerItem = mediaServerService.getOne(param.getMediaServerId());
            if (mediaServerItem != null) {
                event.setMediaServerItem(mediaServerItem);
                applicationEventPublisher.publishEvent(event);
            }
        }catch (Exception e) {
            logger.info("[ABL-HOOK-启动] 发送通知失败 ", e);
        }

        return HookResult.SUCCESS();
    }

    /**
     * TODO 发送rtp(startSendRtp)被动关闭时回调
     */
//    @ResponseBody
//    @PostMapping(value = "/on_send_rtp_stopped", produces = "application/json;charset=UTF-8")
//    public HookResult onSendRtpStopped(HttpServletRequest request, @RequestBody OnSendRtpStoppedHookParam param) {
//
//        logger.info("[ZLM HOOK] rtp发送关闭：{}->{}/{}", param.getMediaServerId(), param.getApp(), param.getStream());
//
//        // 查找对应的上级推流，发送停止
//        if (!"rtp".equals(param.getApp())) {
//            return HookResult.SUCCESS();
//        }
//        try {
//            MediaSendRtpStoppedEvent event = new MediaSendRtpStoppedEvent(this);
//            MediaServer mediaServerItem = mediaServerService.getOne(param.getMediaServerId());
//            if (mediaServerItem != null) {
//                event.setMediaServer(mediaServerItem);
//                applicationEventPublisher.publishEvent(event);
//            }
//        }catch (Exception e) {
//            logger.info("[ZLM-HOOK-rtp发送关闭] 发送通知失败 ", e);
//        }
//
//        return HookResult.SUCCESS();
//    }

    /**
     * TODO 录像完成事件
     */
    @ResponseBody
    @PostMapping(value = "/on_record_mp4", produces = "application/json;charset=UTF-8")
    public HookResult onRecordMp4(HttpServletRequest request, @RequestBody OnRecordMp4ABLHookParam param) {
        logger.info("[ABL HOOK] 录像完成事件：{}->{}", param.getMediaServerId(), param.getFileName());

//        try {
//            MediaServer mediaServerItem = mediaServerService.getOne(param.getMediaServerId());
//            if (mediaServerItem != null) {
//                MediaRecordMp4Event event = MediaRecordMp4Event.getInstance(this, param, mediaServerItem);
//                event.setMediaServer(mediaServerItem);
//                applicationEventPublisher.publishEvent(event);
//            }
//        }catch (Exception e) {
//            logger.info("[ZLM-HOOK-rtpServer收流超时] 发送通知失败 ", e);
//        }

        return HookResult.SUCCESS();
    }

    /**
     * 当某一路码流断开时会发送通知
     */
    @ResponseBody
    @PostMapping(value = "/on_stream_disconnect", produces = "application/json;charset=UTF-8")
    public HookResult onRecordMp4(HttpServletRequest request, @RequestBody ABLHookParam param) {
        logger.info("[ABL HOOK] 码流断开事件, {}->{}/{}", param.getMediaServerId(), param.getApp(), param.getStream());

        MediaServer mediaServer = mediaServerService.getOne(param.getMediaServerId());
        if (mediaServer == null) {
            return HookResult.SUCCESS();
        }

        MediaDepartureEvent mediaDepartureEvent = MediaDepartureEvent.getInstance(this, param, mediaServer);
        applicationEventPublisher.publishEvent(mediaDepartureEvent);

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
