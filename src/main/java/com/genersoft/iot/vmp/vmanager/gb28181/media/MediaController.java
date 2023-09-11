package com.genersoft.iot.vmp.vmanager.gb28181.media;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.SecurityUtils;
import com.genersoft.iot.vmp.conf.security.dto.LoginUser;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.service.IStreamProxyService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;


@Tag(name  = "媒体流相关")
@Controller

@RequestMapping(value = "/api/media")
public class MediaController {

    private final static Logger logger = LoggerFactory.getLogger(MediaController.class);

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IMediaService mediaService;
    @Autowired
    private IStreamProxyService streamProxyService;


    /**
     * 根据应用名和流id获取播放地址
     * @param app 应用名
     * @param stream 流id
     * @return
     */
    @Operation(summary = "根据应用名和流id获取播放地址")
    @Parameter(name = "app", description = "应用名", required = true)
    @Parameter(name = "stream", description = "流id", required = true)
    @Parameter(name = "mediaServerId", description = "媒体服务器id")
    @Parameter(name = "callId", description = "推流时携带的自定义鉴权ID")
    @Parameter(name = "useSourceIpAsStreamIp", description = "是否使用请求IP作为返回的地址IP")
    @GetMapping(value = "/stream_info_by_app_and_stream")
    @ResponseBody
    public StreamContent getStreamInfoByAppAndStream(HttpServletRequest request, @RequestParam String app,
                                                     @RequestParam String stream,
                                                     @RequestParam(required = false) String mediaServerId,
                                                     @RequestParam(required = false) String callId,
                                                     @RequestParam(required = false) Boolean useSourceIpAsStreamIp){
        boolean authority = false;
        if (callId != null) {
            // 权限校验
            StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(app, stream);
            if (streamAuthorityInfo != null
                    && streamAuthorityInfo.getCallId() != null
                    && streamAuthorityInfo.getCallId().equals(callId)) {
                authority = true;
            }else {
                throw new ControllerException(ErrorCode.ERROR400.getCode(), "获取播放地址鉴权失败");
            }
        }else {
            // 是否登陆用户, 登陆用户返回完整信息
            LoginUser userInfo = SecurityUtils.getUserInfo();
            if (userInfo!= null) {
                authority = true;
            }
        }

        StreamInfo streamInfo;

        if (useSourceIpAsStreamIp != null && useSourceIpAsStreamIp) {
            String host = request.getHeader("Host");
            String localAddr = host.split(":")[0];
            logger.info("使用{}作为返回流的ip", localAddr);
            streamInfo = mediaService.getStreamInfoByAppAndStreamWithCheck(app, stream, mediaServerId, localAddr, authority);
        }else {
            streamInfo = mediaService.getStreamInfoByAppAndStreamWithCheck(app, stream, mediaServerId, authority);
        }

        if (streamInfo != null){
            return  new StreamContent(streamInfo);
        }else {
            //获取流失败，重启拉流后重试一次
            streamProxyService.stop(app,stream);
            boolean start = streamProxyService.start(app, stream);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error("[线程休眠失败]， {}", e.getMessage());
            }
            if (useSourceIpAsStreamIp != null && useSourceIpAsStreamIp) {
                String host = request.getHeader("Host");
                String localAddr = host.split(":")[0];
                logger.info("使用{}作为返回流的ip", localAddr);
                streamInfo = mediaService.getStreamInfoByAppAndStreamWithCheck(app, stream, mediaServerId, localAddr, authority);
            }else {
                streamInfo = mediaService.getStreamInfoByAppAndStreamWithCheck(app, stream, mediaServerId, authority);
            }
            if (streamInfo != null){
                return new StreamContent(streamInfo);
            }else {
                throw new ControllerException(ErrorCode.ERROR100);
            }
        }
    }
}
