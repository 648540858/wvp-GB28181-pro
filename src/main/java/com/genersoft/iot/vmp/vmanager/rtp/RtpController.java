package com.genersoft.iot.vmp.vmanager.rtp;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.VersionInfo;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.*;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("rawtypes")
@Tag(name = "第三方服务对接")

@RestController
@RequestMapping("/api/rtp")
public class RtpController {

    @Autowired
    private ZlmHttpHookSubscribe zlmHttpHookSubscribe;

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
    private IStreamPushService pushService;


    @Autowired
    private IStreamProxyService proxyService;


    @Value("${server.port}")
    private int serverPort;


    @Autowired
    private IRedisCatchStorage redisCatchStorage;


    @GetMapping(value = "/openRtpServer")
    @ResponseBody
    @Operation(summary = "开启收流和获取发流信息")
    @Parameter(name = "isSend", description = "是否发送，false时同时只开启收流", required = true)
    @Parameter(name = "callId", description = "整个过程的唯一标识", required = true)
    @Parameter(name = "ssrc", description = "来源流的SSRC", required = false)
    @Parameter(name = "hasAudio", description = "是否", required = false)
    @Parameter(name = "stream", description = "形成的流的ID", required = true)
    @Parameter(name = "tcpMode", description = "收流模式， 0为UDP， 1为TCP被动", required = true)
    public void openRtpServer(Boolean isSend, String ssrc, String callId, Boolean hasAudio, String stream, Integer tcpMode) {
        MediaServerItem mediaServerItem = mediaServerService.getMediaServerForMinimumLoad(null);
        if (mediaServerItem == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(),"没有可用的MediaServer");
        }
    }

    @GetMapping(value = "/sendRTP")
    @ResponseBody
    @Operation(summary = "发送流")

    @Parameter(name = "ssrc", description = "发送流的SSRC", required = true)
    @Parameter(name = "ip", description = "目标IP", required = true)
    @Parameter(name = "port", description = "目标端口", required = true)
    @Parameter(name = "app", description = "待发送应用名", required = true)
    @Parameter(name = "stream", description = "待发送流Id", required = true)
    @Parameter(name = "callId", description = "整个过程的唯一标识", required = true)
    @Parameter(name = "onlyAudio", description = "是否只有音频", required = true)
    public void sendRTP(String ssrc, String ip, Integer port, String app, String stream, String callId, Boolean onlyAudio) {

    }

}
