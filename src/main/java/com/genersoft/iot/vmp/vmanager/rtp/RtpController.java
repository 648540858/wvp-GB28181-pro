package com.genersoft.iot.vmp.vmanager.rtp;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.media.zlm.SendRtpPortManager;
import com.genersoft.iot.vmp.media.zlm.ZLMServerFactory;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeFactory;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForRtpServerTimeout;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForStreamChange;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnRtpServerTimeoutHookParam;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.OtherRtpSendInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("rawtypes")
@Tag(name = "第三方服务对接")

@RestController
@RequestMapping("/api/rtp")
public class RtpController {

    @Autowired
    private ZLMServerFactory zlmServerFactory;

    @Autowired
    private SendRtpPortManager sendRtpPortManager;

    private final static Logger logger = LoggerFactory.getLogger(RtpController.class);

    @Autowired
    private ZlmHttpHookSubscribe hookSubscribe;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;


    @GetMapping(value = "/receive/open")
    @ResponseBody
    @Operation(summary = "开启收流和获取发流信息")
    @Parameter(name = "isSend", description = "是否发送，false时只开启收流， true同时返回推流信息", required = true)
    @Parameter(name = "callId", description = "整个过程的唯一标识，为了与后续接口关联", required = true)
    @Parameter(name = "ssrc", description = "来源流的SSRC，不传则不校验来源ssrc", required = false)
    @Parameter(name = "stream", description = "形成的流的ID", required = true)
    @Parameter(name = "tcpMode", description = "收流模式， 0为UDP， 1为TCP被动", required = true)
    @Parameter(name = "callBack", description = "回调地址，如果收流超时会通道回调通知，回调为get请求，参数为callId", required = true)
    public OtherRtpSendInfo openRtpServer(Boolean isSend, @RequestParam(required = false)String ssrc, String callId, String stream, Integer tcpMode, String callBack) {

        logger.info("[第三方服务对接->开启收流和获取发流信息] isSend->{}, ssrc->{}, callId->{}, stream->{}, tcpMode->{}, callBack->{}",
                isSend, ssrc, callId, stream, tcpMode==0?"UDP":"TCP被动", callBack);

        MediaServerItem mediaServerItem = mediaServerService.getDefaultMediaServer();
        if (mediaServerItem == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(),"没有可用的MediaServer");
        }
        if (stream == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(),"stream参数不可为空");
        }
        if (isSend != null && isSend && callId == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(),"isSend为true时，CallID不能为空");
        }
        long ssrcInt = 0;
        if (ssrc != null) {
            try {
                ssrcInt = Long.parseLong(ssrc);
            }catch (NumberFormatException e) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(),"ssrc格式错误");
            }
        }
        String receiveKey = VideoManagerConstants.WVP_OTHER_RECEIVE_RTP_INFO + userSetting.getServerId() + "_" + callId + "_"  + stream;
        int localPortForVideo = zlmServerFactory.createRTPServer(mediaServerItem, stream, ssrcInt, null, false, tcpMode);
        int localPortForAudio = zlmServerFactory.createRTPServer(mediaServerItem, stream + "_a" , ssrcInt, null, false, tcpMode);
        if (localPortForVideo == 0 || localPortForAudio == 0) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "获取端口失败");
        }
        // 注册回调如果rtp收流超时则通过回调发送通知
        if (callBack != null) {
            HookSubscribeForRtpServerTimeout hookSubscribeForRtpServerTimeout = HookSubscribeFactory.on_rtp_server_timeout(stream, String.valueOf(ssrcInt), mediaServerItem.getId());
            // 订阅 zlm启动事件, 新的zlm也会从这里进入系统
            hookSubscribe.addSubscribe(hookSubscribeForRtpServerTimeout,
                    (mediaServerItemInUse, hookParam)->{
                        OnRtpServerTimeoutHookParam serverTimeoutHookParam = (OnRtpServerTimeoutHookParam) hookParam;
                        if (stream.equals(serverTimeoutHookParam.getStream_id())) {
                            logger.info("[开启收流和获取发流信息] 等待收流超时 callId->{}, 发送回调", callId);
                            OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
                            OkHttpClient client = httpClientBuilder.build();
                            String url = callBack + "?callId="  + callId;
                            Request request = new Request.Builder().get().url(url).build();
                            try {
                                client.newCall(request).execute();
                            } catch (IOException e) {
                                logger.error("[第三方服务对接->开启收流和获取发流信息] 等待收流超时 callId->{}, 发送回调失败", callId, e);
                            }
                            hookSubscribe.removeSubscribe(hookSubscribeForRtpServerTimeout);
                        }
                    });
        }
        String key = VideoManagerConstants.WVP_OTHER_SEND_RTP_INFO + userSetting.getServerId() + "_"  + callId;
        OtherRtpSendInfo otherRtpSendInfo = new OtherRtpSendInfo();
        otherRtpSendInfo.setReceiveIp(mediaServerItem.getSdpIp());
        otherRtpSendInfo.setReceivePortForVideo(localPortForVideo);
        otherRtpSendInfo.setReceivePortForAudio(localPortForAudio);
        otherRtpSendInfo.setCallId(callId);
        otherRtpSendInfo.setStream(stream);

        // 将信息写入redis中，以备后用
        redisTemplate.opsForValue().set(receiveKey, otherRtpSendInfo);
        if (isSend != null && isSend) {
            // 预创建发流信息
            int portForVideo = sendRtpPortManager.getNextPort(mediaServerItem);
            int portForAudio = sendRtpPortManager.getNextPort(mediaServerItem);

            otherRtpSendInfo.setSendLocalIp(mediaServerItem.getSdpIp());
            otherRtpSendInfo.setSendLocalPortForVideo(portForVideo);
            otherRtpSendInfo.setSendLocalPortForAudio(portForAudio);
            // 将信息写入redis中，以备后用
            redisTemplate.opsForValue().set(key, otherRtpSendInfo, 300, TimeUnit.SECONDS);
            logger.info("[第三方服务对接->开启收流和获取发流信息] 结果，callId->{}， {}", callId, otherRtpSendInfo);
        }
        // 将信息写入redis中，以备后用
        redisTemplate.opsForValue().set(key, otherRtpSendInfo, 300, TimeUnit.SECONDS);
        return otherRtpSendInfo;
    }

    @GetMapping(value = "/receive/close")
    @ResponseBody
    @Operation(summary = "关闭收流")
    @Parameter(name = "stream", description = "流的ID", required = true)
    public void closeRtpServer(String stream) {
        logger.info("[第三方服务对接->关闭收流] stream->{}", stream);
        MediaServerItem mediaServerItem = mediaServerService.getDefaultMediaServer();
        zlmServerFactory.closeRtpServer(mediaServerItem,stream);
        zlmServerFactory.closeRtpServer(mediaServerItem,stream + "_a");
        String receiveKey = VideoManagerConstants.WVP_OTHER_RECEIVE_RTP_INFO + userSetting.getServerId() + "_*_"  + stream;
        List<Object> scan = RedisUtil.scan(redisTemplate, receiveKey);
        if (scan.size() > 0) {
            for (Object key : scan) {
                // 将信息写入redis中，以备后用
                redisTemplate.delete(key);
            }
        }
    }

    @GetMapping(value = "/send/start")
    @ResponseBody
    @Operation(summary = "发送流")
    @Parameter(name = "ssrc", description = "发送流的SSRC", required = true)
    @Parameter(name = "dstIpForAudio", description = "目标音频收流IP", required = false)
    @Parameter(name = "dstIpForVideo", description = "目标视频收流IP", required = false)
    @Parameter(name = "dstPortForAudio", description = "目标音频收流端口", required = false)
    @Parameter(name = "dstPortForVideo", description = "目标视频收流端口", required = false)
    @Parameter(name = "app", description = "待发送应用名", required = true)
    @Parameter(name = "stream", description = "待发送流Id", required = true)
    @Parameter(name = "callId", description = "整个过程的唯一标识，不传则使用随机端口发流", required = true)
    @Parameter(name = "isUdp", description = "是否为UDP", required = true)
    @Parameter(name = "ptForAudio", description = "rtp的音频pt", required = false)
    @Parameter(name = "ptForVideo", description = "rtp的视频pt", required = false)
    public void sendRTP(String ssrc,
                        @RequestParam(required = false)String dstIpForAudio,
                        @RequestParam(required = false)String dstIpForVideo,
                        @RequestParam(required = false)Integer dstPortForAudio,
                        @RequestParam(required = false)Integer dstPortForVideo,
                        String app,
                        String stream,
                        String callId,
                        Boolean isUdp,
                        @RequestParam(required = false)Integer ptForAudio,
                        @RequestParam(required = false)Integer ptForVideo
        ) {
        logger.info("[第三方服务对接->发送流] " +
                        "ssrc->{}, \r\n" +
                        "dstIpForAudio->{}, \n" +
                        "dstIpForAudio->{}, \n" +
                        "dstPortForAudio->{},  \n" +
                        "dstPortForVideo->{}, \n" +
                        "app->{}, \n" +
                        "stream->{}, \n" +
                        "callId->{}, \n" +
                        "ptForAudio->{}, \n" +
                        "ptForVideo->{}",
                        ssrc,
                        dstIpForAudio,
                        dstIpForVideo,
                        dstPortForAudio,
                        dstPortForVideo,
                        app,
                        stream,
                        callId,
                        ptForAudio,
                        ptForVideo);
        if (!((dstPortForAudio > 0 && !ObjectUtils.isEmpty(dstPortForAudio) || (dstPortForVideo > 0 && !ObjectUtils.isEmpty(dstIpForVideo))))) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "至少应该存在一组音频或视频发送参数");
        }
        MediaServerItem mediaServerItem = mediaServerService.getDefaultMediaServer();
        String key = VideoManagerConstants.WVP_OTHER_SEND_RTP_INFO + userSetting.getServerId() + "_"  + callId;
        OtherRtpSendInfo sendInfo = (OtherRtpSendInfo)redisTemplate.opsForValue().get(key);
        if (sendInfo == null) {
            sendInfo = new OtherRtpSendInfo();
        }
        sendInfo.setPushApp(app);
        sendInfo.setPushStream(stream);
        sendInfo.setPushSSRC(ssrc);

        Map<String, Object> paramForAudio;
        Map<String, Object> paramForVideo;
        if (!ObjectUtils.isEmpty(dstIpForAudio) && dstPortForAudio > 0) {
            paramForAudio = new HashMap<>();
            paramForAudio.put("vhost","__defaultVhost__");
            paramForAudio.put("app",app);
            paramForAudio.put("stream",stream);
            paramForAudio.put("ssrc", ssrc);

            paramForAudio.put("dst_url", dstIpForAudio);
            paramForAudio.put("dst_port", dstPortForAudio);
            String is_Udp = isUdp ? "1" : "0";
            paramForAudio.put("is_udp", is_Udp);
            paramForAudio.put("src_port", sendInfo.getSendLocalPortForAudio());
            paramForAudio.put("only_audio", "1");
            if (ptForAudio != null) {
                paramForAudio.put("pt", ptForAudio);
            }

        } else {
            paramForAudio = null;
        }
        if (!ObjectUtils.isEmpty(dstIpForVideo) && dstPortForVideo > 0) {
            paramForVideo = new HashMap<>();
            paramForVideo.put("vhost","__defaultVhost__");
            paramForVideo.put("app",app);
            paramForVideo.put("stream",stream);
            paramForVideo.put("ssrc", ssrc);

            paramForVideo.put("dst_url", dstIpForVideo);
            paramForVideo.put("dst_port", dstPortForVideo);
            String is_Udp = isUdp ? "1" : "0";
            paramForVideo.put("is_udp", is_Udp);
            paramForVideo.put("src_port", sendInfo.getSendLocalPortForVideo());
            paramForVideo.put("only_audio", "0");
            if (ptForVideo != null) {
                paramForVideo.put("pt", ptForVideo);
            }

        } else {
            paramForVideo = null;
        }

        Boolean streamReady = zlmServerFactory.isStreamReady(mediaServerItem, app, stream);
        if (streamReady) {
            if (paramForVideo != null) {
                JSONObject jsonObject = zlmServerFactory.startSendRtpStream(mediaServerItem, paramForVideo);
                if (jsonObject.getInteger("code") == 0) {
                    logger.info("[第三方服务对接->发送流] 视频流发流成功，callId->{}，param->{}", callId, paramForVideo);
                    redisTemplate.opsForValue().set(key, sendInfo);
                }else {
                    redisTemplate.delete(key);
                    logger.info("[第三方服务对接->发送流] 视频流发流失败，callId->{}, {}", callId, jsonObject.getString("msg"));
                    throw new ControllerException(ErrorCode.ERROR100.getCode(), "[视频流发流失败] " + jsonObject.getString("msg"));
                }
            }
            if(paramForAudio != null) {
                JSONObject jsonObject = zlmServerFactory.startSendRtpStream(mediaServerItem, paramForAudio);
                if (jsonObject.getInteger("code") == 0) {
                    logger.info("[第三方服务对接->发送流] 音频流发流成功，callId->{}，param->{}", callId, paramForAudio);
                    redisTemplate.opsForValue().set(key, sendInfo);
                }else {
                    redisTemplate.delete(key);
                    logger.info("[第三方服务对接->发送流] 音频流发流失败，callId->{}, {}", callId, jsonObject.getString("msg"));
                    throw new ControllerException(ErrorCode.ERROR100.getCode(), "[音频流发流失败] " + jsonObject.getString("msg"));
                }
            }
        }else {
            logger.info("[第三方服务对接->发送流] 流不存在，等待流上线，callId->{}", callId);
            String uuid = UUID.randomUUID().toString();
            HookSubscribeForStreamChange hookSubscribeForStreamChange = HookSubscribeFactory.on_stream_changed(app, stream, true, "rtsp", mediaServerItem.getId());
            dynamicTask.startDelay(uuid, ()->{
                logger.info("[第三方服务对接->发送流] 等待流上线超时 callId->{}", callId);
                redisTemplate.delete(key);
                hookSubscribe.removeSubscribe(hookSubscribeForStreamChange);
            }, 10000);

            // 订阅 zlm启动事件, 新的zlm也会从这里进入系统
            OtherRtpSendInfo finalSendInfo = sendInfo;
            hookSubscribe.removeSubscribe(hookSubscribeForStreamChange);
            hookSubscribe.addSubscribe(hookSubscribeForStreamChange,
                    (mediaServerItemInUse, response)->{
                        dynamicTask.stop(uuid);
                        logger.info("[第三方服务对接->发送流] 流上线，开始发流 callId->{}", callId);
                        try {
                            Thread.sleep(400);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        if (paramForVideo != null) {
                            JSONObject jsonObject = zlmServerFactory.startSendRtpStream(mediaServerItem, paramForVideo);
                            if (jsonObject.getInteger("code") == 0) {
                                logger.info("[第三方服务对接->发送流] 视频流发流成功，callId->{}，param->{}", callId, paramForVideo);
                                redisTemplate.opsForValue().set(key, finalSendInfo);
                            }else {
                                redisTemplate.delete(key);
                                logger.info("[第三方服务对接->发送流] 视频流发流失败，callId->{}, {}", callId, jsonObject.getString("msg"));
                                throw new ControllerException(ErrorCode.ERROR100.getCode(), "[视频流发流失败] " + jsonObject.getString("msg"));
                            }
                        }
                        if(paramForAudio != null) {
                            JSONObject jsonObject = zlmServerFactory.startSendRtpStream(mediaServerItem, paramForAudio);
                            if (jsonObject.getInteger("code") == 0) {
                                logger.info("[第三方服务对接->发送流] 音频流发流成功，callId->{}，param->{}", callId, paramForAudio);
                                redisTemplate.opsForValue().set(key, finalSendInfo);
                            }else {
                                redisTemplate.delete(key);
                                logger.info("[第三方服务对接->发送流] 音频流发流失败，callId->{}, {}", callId, jsonObject.getString("msg"));
                                throw new ControllerException(ErrorCode.ERROR100.getCode(), "[音频流发流失败] " + jsonObject.getString("msg"));
                            }
                        }
                        hookSubscribe.removeSubscribe(hookSubscribeForStreamChange);
                    });
        }
    }

    @GetMapping(value = "/send/stop")
    @ResponseBody
    @Operation(summary = "关闭发送流")
    @Parameter(name = "callId", description = "整个过程的唯一标识，不传则使用随机端口发流", required = true)
    public void closeSendRTP(String callId) {
        logger.info("[第三方服务对接->关闭发送流] callId->{}", callId);
        String key = VideoManagerConstants.WVP_OTHER_SEND_RTP_INFO + userSetting.getServerId() + "_"  + callId;
        OtherRtpSendInfo sendInfo = (OtherRtpSendInfo)redisTemplate.opsForValue().get(key);
        if (sendInfo == null){
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未开启发流");
        }
        Map<String, Object> param = new HashMap<>();
        param.put("vhost","__defaultVhost__");
        param.put("app",sendInfo.getPushApp());
        param.put("stream",sendInfo.getPushStream());
        param.put("ssrc",sendInfo.getPushSSRC());
        MediaServerItem mediaServerItem = mediaServerService.getDefaultMediaServer();
        Boolean result = zlmServerFactory.stopSendRtpStream(mediaServerItem, param);
        if (!result) {
            logger.info("[第三方服务对接->关闭发送流] 失败 callId->{}", callId);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "停止发流失败");
        }else {
            logger.info("[第三方服务对接->关闭发送流] 成功 callId->{}", callId);
        }
        redisTemplate.delete(key);
    }

}
