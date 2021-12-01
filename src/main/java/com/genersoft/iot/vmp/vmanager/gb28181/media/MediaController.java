package com.genersoft.iot.vmp.vmanager.gb28181.media;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
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


@Api(tags = "媒体流相关")
@Controller
@CrossOrigin
@RequestMapping(value = "/api/media")
public class MediaController {

    private final static Logger logger = LoggerFactory.getLogger(MediaController.class);

    @Autowired
    private IVideoManagerStorager storager;

    @Autowired
    private IStreamPushService streamPushService;

    @Autowired
    private IMediaService mediaService;


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
    public WVPResult<StreamInfo> getStreamInfoByAppAndStream(@RequestParam String app, @RequestParam String stream, @RequestParam(required = false) String mediaServerId){
        StreamInfo streamInfoByAppAndStreamWithCheck = mediaService.getStreamInfoByAppAndStreamWithCheck(app, stream, mediaServerId);
        WVPResult<StreamInfo> result = new WVPResult<>();
        if (streamInfoByAppAndStreamWithCheck != null){
            result.setCode(0);
            result.setMsg("scccess");
            result.setData(streamInfoByAppAndStreamWithCheck);
        }else {
            result.setCode(-1);
            result.setMsg("fail");
        }
        return result;
    }



}
