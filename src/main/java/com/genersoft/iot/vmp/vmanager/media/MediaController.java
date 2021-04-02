package com.genersoft.iot.vmp.vmanager.media;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.gb28181.bean.PlatformGbStream;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.service.IStreamProxyService;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


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




    @RequestMapping(value = "/getStreamInfoByAppAndStream")
    @ResponseBody
    public StreamInfo getStreamInfoByAppAndStream(String app, String stream){
        return mediaService.getStreamInfoByAppAndStreamWithCheck(app, stream);
    }



}
