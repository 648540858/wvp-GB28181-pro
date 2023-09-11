package com.genersoft.iot.vmp.vmanager.ps;

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
import com.genersoft.iot.vmp.vmanager.bean.OtherPsSendInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("rawtypes")
@Tag(name = "第三方PS服务对接")

@RestController
@RequestMapping("/api/ps")
public class PsController {

    private final static Logger logger = LoggerFactory.getLogger(PsController.class);

    @Autowired
    private ZLMServerFactory zlmServerFactory;

    @Autowired
    private ZlmHttpHookSubscribe hookSubscribe;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private SendRtpPortManager sendRtpPortManager;

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
    public OtherPsSendInfo openRtpServer(Boolean isSend, @RequestParam(required = false)String ssrc, String callId, String stream, Integer tcpMode, String callBack) {

        logger.info("[第三方PS服务对接->开启收流和获取发流信息] isSend->{}, ssrc->{}, callId->{}, stream->{}, tcpMode->{}, callBack->{}",
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
        String receiveKey = VideoManagerConstants.WVP_OTHER_RECEIVE_PS_INFO + userSetting.getServerId() + "_" + callId + "_"  + stream;
        int localPort = zlmServerFactory.createRTPServer(mediaServerItem, stream, ssrcInt, null, false, tcpMode);
        if (localPort == 0) {
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
                            logger.info("[第三方PS服务对接->开启收流和获取发流信息] 等待收流超时 callId->{}, 发送回调", callId);
                            // 将信息写入redis中，以备后用
                            redisTemplate.delete(receiveKey);
                            OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
                            OkHttpClient client = httpClientBuilder.build();
                            String url = callBack + "?callId="  + callId;
                            Request request = new Request.Builder().get().url(url).build();
                            try {
                                client.newCall(request).execute();
                            } catch (IOException e) {
                                logger.error("[第三方PS服务对接->开启收流和获取发流信息] 等待收流超时 callId->{}, 发送回调失败", callId, e);
                            }
                            hookSubscribe.removeSubscribe(hookSubscribeForRtpServerTimeout);
                        }
                    });
        }
        OtherPsSendInfo otherPsSendInfo = new OtherPsSendInfo();
        otherPsSendInfo.setReceiveIp(mediaServerItem.getSdpIp());
        otherPsSendInfo.setReceivePort(localPort);
        otherPsSendInfo.setCallId(callId);
        otherPsSendInfo.setStream(stream);

        // 将信息写入redis中，以备后用
        redisTemplate.opsForValue().set(receiveKey, otherPsSendInfo);
        if (isSend != null && isSend) {
            String key = VideoManagerConstants.WVP_OTHER_SEND_PS_INFO + userSetting.getServerId() + "_"  + callId;
            // 预创建发流信息
            int port = sendRtpPortManager.getNextPort(mediaServerItem);

            otherPsSendInfo.setSendLocalIp(mediaServerItem.getSdpIp());
            otherPsSendInfo.setSendLocalPort(port);
            // 将信息写入redis中，以备后用
            redisTemplate.opsForValue().set(key, otherPsSendInfo, 300, TimeUnit.SECONDS);
            logger.info("[第三方PS服务对接->开启收流和获取发流信息] 结果，callId->{}， {}", callId, otherPsSendInfo);
        }
        return otherPsSendInfo;
    }

    @GetMapping(value = "/receive/close")
    @ResponseBody
    @Operation(summary = "关闭收流")
    @Parameter(name = "stream", description = "流的ID", required = true)
    public void closeRtpServer(String stream) {
        logger.info("[第三方PS服务对接->关闭收流] stream->{}", stream);
        MediaServerItem mediaServerItem = mediaServerService.getDefaultMediaServer();
        zlmServerFactory.closeRtpServer(mediaServerItem,stream);
        String receiveKey = VideoManagerConstants.WVP_OTHER_RECEIVE_PS_INFO + userSetting.getServerId() + "_*_"  + stream;
        List<Object> scan = RedisUtil.scan(redisTemplate, receiveKey);
        if (!scan.isEmpty()) {
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
    @Parameter(name = "dstIp", description = "目标收流IP", required = true)
    @Parameter(name = "dstPort", description = "目标收流端口", required = true)
    @Parameter(name = "app", description = "待发送应用名", required = true)
    @Parameter(name = "stream", description = "待发送流Id", required = true)
    @Parameter(name = "callId", description = "整个过程的唯一标识，不传则使用随机端口发流", required = true)
    @Parameter(name = "isUdp", description = "是否为UDP", required = true)
    public void sendRTP(String ssrc,
                        String dstIp,
                        Integer dstPort,
                        String app,
                        String stream,
                        String callId,
                        Boolean isUdp
        ) {
        logger.info("[第三方PS服务对接->发送流] " +
                        "ssrc->{}, \r\n" +
                        "dstIp->{}, \n" +
                        "dstPort->{},  \n" +
                        "app->{}, \n" +
                        "stream->{}, \n" +
                        "callId->{} \n",
                        ssrc,
                        dstIp,
                        dstPort,
                        app,
                        stream,
                        callId);
        MediaServerItem mediaServerItem = mediaServerService.getDefaultMediaServer();
        String key = VideoManagerConstants.WVP_OTHER_SEND_PS_INFO + userSetting.getServerId() + "_"  + callId;
        OtherPsSendInfo sendInfo = (OtherPsSendInfo)redisTemplate.opsForValue().get(key);
        if (sendInfo == null) {
            sendInfo = new OtherPsSendInfo();
        }
        sendInfo.setPushApp(app);
        sendInfo.setPushStream(stream);
        sendInfo.setPushSSRC(ssrc);

        Map<String, Object> param;


        param = new HashMap<>();
        param.put("vhost","__defaultVhost__");
        param.put("app",app);
        param.put("stream",stream);
        param.put("ssrc", ssrc);

        param.put("dst_url", dstIp);
        param.put("dst_port", dstPort);
        String is_Udp = isUdp ? "1" : "0";
        param.put("is_udp", is_Udp);
        param.put("src_port", sendInfo.getSendLocalPort());


        Boolean streamReady = zlmServerFactory.isStreamReady(mediaServerItem, app, stream);
        if (streamReady) {
            JSONObject jsonObject = zlmServerFactory.startSendRtpStream(mediaServerItem, param);
            if (jsonObject.getInteger("code") == 0) {
                logger.info("[第三方PS服务对接->发送流] 视频流发流成功，callId->{}，param->{}", callId, param);
                redisTemplate.opsForValue().set(key, sendInfo);
            }else {
                redisTemplate.delete(key);
                logger.info("[第三方PS服务对接->发送流] 视频流发流失败，callId->{}, {}", callId, jsonObject.getString("msg"));
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "[视频流发流失败] " + jsonObject.getString("msg"));
            }
        }else {
            logger.info("[第三方PS服务对接->发送流] 流不存在，等待流上线，callId->{}", callId);
            String uuid = UUID.randomUUID().toString();
            HookSubscribeForStreamChange hookSubscribeForStreamChange = HookSubscribeFactory.on_stream_changed(app, stream, true, "rtsp", mediaServerItem.getId());
            dynamicTask.startDelay(uuid, ()->{
                logger.info("[第三方PS服务对接->发送流] 等待流上线超时 callId->{}", callId);
                redisTemplate.delete(key);
                hookSubscribe.removeSubscribe(hookSubscribeForStreamChange);
            }, 10000);

            // 订阅 zlm启动事件, 新的zlm也会从这里进入系统
            OtherPsSendInfo finalSendInfo = sendInfo;
            hookSubscribe.removeSubscribe(hookSubscribeForStreamChange);
            hookSubscribe.addSubscribe(hookSubscribeForStreamChange,
                    (mediaServerItemInUse, response)->{
                        dynamicTask.stop(uuid);
                        logger.info("[第三方PS服务对接->发送流] 流上线，开始发流 callId->{}", callId);
                        try {
                            Thread.sleep(400);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        JSONObject jsonObject = zlmServerFactory.startSendRtpStream(mediaServerItem, param);
                        if (jsonObject.getInteger("code") == 0) {
                            logger.info("[第三方PS服务对接->发送流] 视频流发流成功，callId->{}，param->{}", callId, param);
                            redisTemplate.opsForValue().set(key, finalSendInfo);
                        }else {
                            redisTemplate.delete(key);
                            logger.info("[第三方PS服务对接->发送流] 视频流发流失败，callId->{}, {}", callId, jsonObject.getString("msg"));
                            throw new ControllerException(ErrorCode.ERROR100.getCode(), "[视频流发流失败] " + jsonObject.getString("msg"));
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
        logger.info("[第三方PS服务对接->关闭发送流] callId->{}", callId);
        String key = VideoManagerConstants.WVP_OTHER_SEND_PS_INFO + userSetting.getServerId() + "_"  + callId;
        OtherPsSendInfo sendInfo = (OtherPsSendInfo)redisTemplate.opsForValue().get(key);
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
            logger.info("[第三方PS服务对接->关闭发送流] 失败 callId->{}", callId);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "停止发流失败");
        }else {
            logger.info("[第三方PS服务对接->关闭发送流] 成功 callId->{}", callId);
        }
        redisTemplate.delete(key);
    }


    @GetMapping(value = "/getTestPort")
    @ResponseBody
    public int getTestPort() {
        MediaServerItem defaultMediaServer = mediaServerService.getDefaultMediaServer();

//        for (int i = 0; i <300; i++) {
//            new Thread(() -> {
//                int nextPort = sendRtpPortManager.getNextPort(defaultMediaServer);
//                try {
//                    Thread.sleep((int)Math.random()*10);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//                System.out.println(nextPort);
//            }).start();
//        }

        return sendRtpPortManager.getNextPort(defaultMediaServer);
    }
}
