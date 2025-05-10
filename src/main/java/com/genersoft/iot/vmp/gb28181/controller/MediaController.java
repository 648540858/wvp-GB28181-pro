package com.genersoft.iot.vmp.gb28181.controller;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.conf.security.SecurityUtils;
import com.genersoft.iot.vmp.conf.security.dto.LoginUser;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;


@Tag(name  = "媒体流相关")
@RestController
@Slf4j
@RequestMapping(value = "/api/media")
public class MediaController {

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IStreamProxyService streamProxyService;

    @Autowired
    private IMediaServerService mediaServerService;


    /**
     * 根据应用名和流id获取播放地址
     * @param app 应用名
     * @param stream 流id
     * @return
     */
    @Operation(summary = "根据应用名和流id获取播放地址", security = @SecurityRequirement(name = JwtUtils.HEADER))
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
            log.info("使用{}作为返回流的ip", localAddr);
            streamInfo = mediaServerService.getStreamInfoByAppAndStreamWithCheck(app, stream, mediaServerId, localAddr, authority);
        }else {
            streamInfo = mediaServerService.getStreamInfoByAppAndStreamWithCheck(app, stream, mediaServerId, authority);
        }

        if (streamInfo != null){
            return new StreamContent(streamInfo);
        }else {
            //获取流失败，重启拉流后重试一次
            streamProxyService.stopByAppAndStream(app,stream);
            boolean start = streamProxyService.startByAppAndStream(app, stream);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("[线程休眠失败]， {}", e.getMessage());
            }
            if (useSourceIpAsStreamIp != null && useSourceIpAsStreamIp) {
                String host = request.getHeader("Host");
                String localAddr = host.split(":")[0];
                log.info("使用{}作为返回流的ip", localAddr);
                streamInfo = mediaServerService.getStreamInfoByAppAndStreamWithCheck(app, stream, mediaServerId, localAddr, authority);
            }else {
                streamInfo = mediaServerService.getStreamInfoByAppAndStreamWithCheck(app, stream, mediaServerId, authority);
            }
            if (streamInfo != null){
                return new StreamContent(streamInfo);
            }else {
                throw new ControllerException(ErrorCode.ERROR100);
            }
        }
    }
    /**
     * 获取推流播放地址
     * @param app 应用名
     * @param stream 流id
     * @return
     */
    @GetMapping(value = "/getPlayUrl")
    @ResponseBody
    @Operation(summary = "获取推流播放地址", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "app", description = "应用名", required = true)
    @Parameter(name = "stream", description = "流id", required = true)
    @Parameter(name = "mediaServerId", description = "媒体服务器id")
    public StreamContent getPlayUrl(@RequestParam String app, @RequestParam String stream,
                                    @RequestParam(required = false) String mediaServerId){
        boolean authority = false;
        // 是否登陆用户, 登陆用户返回完整信息
        LoginUser userInfo = SecurityUtils.getUserInfo();
        if (userInfo!= null) {
            authority = true;
        }
        StreamInfo streamInfo = mediaServerService.getStreamInfoByAppAndStreamWithCheck(app, stream, mediaServerId, authority);
        if (streamInfo == null){
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "获取播放地址失败");
        }
        return new StreamContent(streamInfo);
    }
}
