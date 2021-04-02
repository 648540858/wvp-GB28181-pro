package com.genersoft.iot.vmp.vmanager.streamPush;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.vmanager.media.MediaController;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@CrossOrigin
@RequestMapping(value = "/api/push")
public class StreamPushController {

    private final static Logger logger = LoggerFactory.getLogger(StreamPushController.class);

    @Autowired
    private IStreamPushService streamPushService;

    @RequestMapping(value = "/list")
    @ResponseBody
    public PageInfo<StreamPushItem> list(@RequestParam(required = false)Integer page,
                                         @RequestParam(required = false)Integer count,
                                         @RequestParam(required = false)String q,
                                         @RequestParam(required = false)Boolean online ){

        PageInfo<StreamPushItem> pushList = streamPushService.getPushList(page - 1, page - 1 + count);
        return pushList;
    }

    @RequestMapping(value = "/saveToGB")
    @ResponseBody
    public Object saveToGB(@RequestBody GbStream stream){
        if (streamPushService.saveToGB(stream)){
            return "success";
        }else {
            return "fail";
        }
    }

    @RequestMapping(value = "/removeFormGB")
    @ResponseBody
    public Object removeFormGB(@RequestBody GbStream stream){
        if (streamPushService.removeFromGB(stream)){
            return "success";
        }else {
            return "fail";
        }
    }
}
