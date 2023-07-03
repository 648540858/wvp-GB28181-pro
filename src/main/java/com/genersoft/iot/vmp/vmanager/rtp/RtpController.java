package com.genersoft.iot.vmp.vmanager.rtp;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.VersionInfo;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.media.zlm.ZLMServerFactory;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeFactory;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForRtpServerTimeout;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IDeviceChannelService;
import com.genersoft.iot.vmp.service.IDeviceService;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("rawtypes")
@Tag(name = "第三方服务对接")

@RestController
@RequestMapping("/api/rtp")
public class RtpController {

    @Autowired
    private ZLMServerFactory zlmServerFactory;

    private final static Logger logger = LoggerFactory.getLogger(RtpController.class);

    @Autowired
    private ZlmHttpHookSubscribe hookSubscribe;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private VersionInfo versionInfo;

    @Autowired
    private SipConfig sipConfig;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IDeviceChannelService channelService;

    @Autowired
    private DynamicTask dynamicTask;


    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;


    @Value("${server.port}")
    private int serverPort;


    @Autowired
    private IRedisCatchStorage redisCatchStorage;


    @GetMapping(value = "/receive/open")
    @ResponseBody
    @Operation(summary = "开启收流和获取发流信息")
    @Parameter(name = "isSend", description = "是否发送，false时只开启收流， true同时返回推流信息", required = true)
    @Parameter(name = "callId", description = "整个过程的唯一标识，为了与后续接口关联", required = true)
    @Parameter(name = "ssrc", description = "来源流的SSRC，不传则不校验来源ssrc", required = false)
    @Parameter(name = "stream", description = "形成的流的ID", required = true)
    @Parameter(name = "tcpMode", description = "收流模式， 0为UDP， 1为TCP被动", required = true)
    @Parameter(name = "callBack", description = "回调地址，如果收流超时会通道回调通知，回调为get请求，参数为callId", required = true)
    public OtherRtpSendInfo openRtpServer(Boolean isSend, String ssrc, String callId, String stream, Integer tcpMode, String callBack) {

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
        int ssrcInt = 0;
        if (ssrc != null) {
            try {
                ssrcInt = Integer.parseInt(ssrc);
            }catch (NumberFormatException e) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(),"ssrc格式错误");
            }

        }
        int localPort = zlmServerFactory.createRTPServer(mediaServerItem, stream, ssrcInt, null, false, tcpMode);
        // 注册回调如果rtp收流超时则通过回调发送通知
        if (callBack != null) {
            HookSubscribeForRtpServerTimeout hookSubscribeForRtpServerTimeout = HookSubscribeFactory.on_rtp_server_timeout(ssrc, null, mediaServerItem.getId());
            // 订阅 zlm启动事件, 新的zlm也会从这里进入系统
            hookSubscribe.addSubscribe(hookSubscribeForRtpServerTimeout,
                    (mediaServerItemInUse, response)->{
                        if (stream.equals(response.getString("stream_id"))) {
                            logger.info("[开启收流和获取发流信息] 等待收流超时 callId->{}, 发送回调", callId);
                            OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
                            OkHttpClient client = httpClientBuilder.build();
                            String url = callBack + "?callId="  + callId;
                            Request request = new Request.Builder().get().url(url).build();
                            try {
                                client.newCall(request).execute();
                            } catch (IOException e) {
                                logger.error("[开启收流和获取发流信息] 等待收流超时 callId->{}, 发送回调失败", callId, e);
                            }
                        }
                    });
        }
        OtherRtpSendInfo otherRtpSendInfo = new OtherRtpSendInfo();
        otherRtpSendInfo.setReceiveIp(mediaServerItem.getSdpIp());
        otherRtpSendInfo.setReceivePort(localPort);
        otherRtpSendInfo.setCallId(callId);
        otherRtpSendInfo.setStream(stream);
        if (isSend != null && isSend) {
            String key = VideoManagerConstants.WVP_OTHER_SEND_RTP_INFO + userSetting.getServerId() + callId;
            // 预创建发流信息
            int port = zlmServerFactory.keepPort(mediaServerItem, callId, 0, ssrc1 -> {
                return redisTemplate.opsForValue().get(key) != null;
            });

            // 将信息写入redis中，以备后用
            redisTemplate.opsForValue().set(key, otherRtpSendInfo);
            // 设置超时任务，超时未使用，则自动移除，并关闭端口保持, 默认五分钟
            dynamicTask.startDelay(key, ()->{
                logger.info("[第三方服务对接->开启收流和获取发流信息] 端口保持超时 callId->{}", callId);
                redisTemplate.delete(key);
                zlmServerFactory.releasePort(mediaServerItem, callId);
            }, 300000);
            otherRtpSendInfo.setIp(mediaServerItem.getSdpIp());
            otherRtpSendInfo.setPort(port);
            logger.info("[开启收流和获取发流信息] 结果，callId->{}， {}", callId, otherRtpSendInfo);
        }
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
    }

    @GetMapping(value = "/send/start")
    @ResponseBody
    @Operation(summary = "发送流")
    @Parameter(name = "ssrc", description = "发送流的SSRC", required = true)
    @Parameter(name = "ip", description = "目标IP", required = true)
    @Parameter(name = "port", description = "目标端口", required = true)
    @Parameter(name = "app", description = "待发送应用名", required = true)
    @Parameter(name = "stream", description = "待发送流Id", required = true)
    @Parameter(name = "callId", description = "整个过程的唯一标识，不传则使用随机端口发流", required = true)
    @Parameter(name = "onlyAudio", description = "是否只有音频", required = true)
    @Parameter(name = "isUdp", description = "是否为UDP", required = true)
    @Parameter(name = "streamType", description = "流类型，1为es流，2为ps流， 默认es流", required = false)
    public void sendRTP(String ssrc, String ip, Integer port, String app, String stream, String callId, Boolean onlyAudio, Boolean isUdp, Integer streamType) {
        logger.info("[第三方服务对接->发送流] ssrc->{}, ip->{}, port->{}, app->{}, stream->{}, callId->{}, onlyAudio->{}, streamType->{}",
                ssrc, ip, port, app, stream, callId, onlyAudio, streamType == 1? "ES":"PS");
        MediaServerItem mediaServerItem = mediaServerService.getDefaultMediaServer();
        String key = VideoManagerConstants.WVP_OTHER_SEND_RTP_INFO + userSetting.getServerId() + callId;
        OtherRtpSendInfo sendInfo = (OtherRtpSendInfo)redisTemplate.opsForValue().get(key);
        if (sendInfo != null) {
            zlmServerFactory.releasePort(mediaServerItem, sendInfo.getCallId());
        }else {
            sendInfo = new OtherRtpSendInfo();
        }
        sendInfo.setPushApp(app);
        sendInfo.setPushStream(stream);
        sendInfo.setPushSSRC(ssrc);

        Map<String, Object> param = new HashMap<>(12);
        param.put("vhost","__defaultVhost__");
        param.put("app",app);
        param.put("stream",stream);
        param.put("ssrc", ssrc);

        param.put("dst_url",ip);
        param.put("dst_port", port);
        String is_Udp = isUdp ? "1" : "0";
        param.put("is_udp", is_Udp);
        param.put("src_port", sendInfo.getPort());
        param.put("use_ps", streamType==2 ? "1" : "0");
        param.put("only_audio", onlyAudio ? "1" : "0");

        JSONObject jsonObject = zlmServerFactory.startSendRtpStream(mediaServerItem, param);
        if (jsonObject.getInteger("code") == 0) {
            logger.info("[第三方服务对接->发送流] 发流成功，callId->{}", callId);
            redisTemplate.opsForValue().set(key, sendInfo);
        }else {
            redisTemplate.delete(key);
            logger.info("[第三方服务对接->发送流] 发流失败，callId->{}, {}", callId, jsonObject.getString("msg"));
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "[发流失败] " + jsonObject.getString("msg"));
        }
    }



    @GetMapping(value = "/send/stop")
    @ResponseBody
    @Operation(summary = "关闭发送流")
    @Parameter(name = "callId", description = "整个过程的唯一标识，不传则使用随机端口发流", required = true)
    public void closeSendRTP(String callId) {
        logger.info("[第三方服务对接->关闭发送流] callId->{}", callId);
        String key = VideoManagerConstants.WVP_OTHER_SEND_RTP_INFO + userSetting.getServerId() + callId;
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
    }

}
