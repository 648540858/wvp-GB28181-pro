package com.genersoft.iot.vmp.vmanager.gb28181.media;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.security.SecurityUtils;
import com.genersoft.iot.vmp.conf.security.dto.LoginUser;
import com.genersoft.iot.vmp.media.zlm.dto.OnPublishHookParam;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IStreamProxyService;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;


@Api(tags = "媒体流相关")
@Controller
@CrossOrigin
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
    @ApiOperation("根据应用名和流id获取播放地址")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "app", value = "应用名", dataTypeClass = String.class),
            @ApiImplicitParam(name = "stream", value = "流id", dataTypeClass = String.class),
            @ApiImplicitParam(name = "mediaServerId", value = "媒体服务器id", dataTypeClass = String.class, required = false),
    })
    @GetMapping(value = "/stream_info_by_app_and_stream")
    @ResponseBody
    public WVPResult<StreamInfo> getStreamInfoByAppAndStream(HttpServletRequest request, @RequestParam String app,
                                                             @RequestParam String stream,
                                                             @RequestParam(required = false) String mediaServerId,
                                                             @RequestParam(required = false) String callId,
                                                             @RequestParam(required = false) Boolean useSourceIpAsStreamIp){
        boolean authority = false;
        if (callId != null) {
            // 权限校验
            StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(app, stream);
            if (streamAuthorityInfo.getCallId().equals(callId)) {
                authority = true;
            }else {
                WVPResult<StreamInfo> result = new WVPResult<>();
                result.setCode(401);
                result.setMsg("fail");
                return result;
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

        WVPResult<StreamInfo> result = new WVPResult<>();
        if (streamInfo != null){
            result.setCode(0);
            result.setMsg("scccess");
            result.setData(streamInfo);
        }else {
            //获取流失败，重启拉流后重试一次
            streamProxyService.stop(app,stream);
            boolean start = streamProxyService.start(app, stream);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
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
                result.setCode(0);
                result.setMsg("scccess");
                result.setData(streamInfo);
            }else {
                result.setCode(-1);
                result.setMsg("fail");
            }
        }
        return result;
    }
}
