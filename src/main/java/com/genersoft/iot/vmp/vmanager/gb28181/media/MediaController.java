package com.genersoft.iot.vmp.vmanager.gb28181.media;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
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
    })
    @RequestMapping(value = "/getStreamInfoByAppAndStream")
    @ResponseBody
    public StreamInfo getStreamInfoByAppAndStream(String app, String stream){
        return mediaService.getStreamInfoByAppAndStreamWithCheck(app, stream);
    }



}
