package com.genersoft.iot.vmp.vmanager.media;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.vmanager.service.IMediaService;
import com.genersoft.iot.vmp.vmanager.service.IStreamProxyService;
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
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IStreamProxyService streamProxyService;

    @Autowired
    private IMediaService mediaService;


    @RequestMapping(value = "/list")
    @ResponseBody
    public JSONObject list( @RequestParam(required = false)Integer page,
                            @RequestParam(required = false)Integer count,
                            @RequestParam(required = false)String q,
                            @RequestParam(required = false)Boolean online ){

        JSONObject jsonObject = redisCatchStorage.getMediaList(page - 1, page - 1 + count);
        return jsonObject;
    }

    @RequestMapping(value = "/getStreamInfoByAppAndStream")
    @ResponseBody
    public StreamInfo getStreamInfoByAppAndStream(String app, String stream){
        return mediaService.getStreamInfoByAppAndStreamWithCheck(app, stream);
    }




}
