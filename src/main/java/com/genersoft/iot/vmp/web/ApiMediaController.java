package com.genersoft.iot.vmp.web;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.RealVideo;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 兼容LiveGBS的API：系统接口
 */
@Controller
@CrossOrigin
@RequestMapping(value = "/api/v1/media")
public class ApiMediaController {

    private final static Logger logger = LoggerFactory.getLogger(ApiMediaController.class);

    @Autowired
    private IRedisCatchStorage redisCatchStorage;


    @RequestMapping(value = "/list")
    @ResponseBody
    public JSONObject list( @RequestParam(required = false)Integer start,
                            @RequestParam(required = false)Integer limit,
                            @RequestParam(required = false)String q,
                            @RequestParam(required = false)Boolean online ){

        List<Object> mediaList = redisCatchStorage.getMediaList(start - 1, start - 1 + limit);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", 0);
        jsonObject.put("data", mediaList);
        return jsonObject;
    }
}
