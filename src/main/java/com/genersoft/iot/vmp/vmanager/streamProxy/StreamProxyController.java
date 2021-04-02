package com.genersoft.iot.vmp.vmanager.streamProxy;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.service.IStreamProxyService;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * 拉流代理接口
 */
@Controller
@CrossOrigin
@RequestMapping(value = "/api/proxy")
public class StreamProxyController {

    private final static Logger logger = LoggerFactory.getLogger(StreamProxyController.class);

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IStreamProxyService streamProxyService;


    @RequestMapping(value = "/list")
    @ResponseBody
    public PageInfo<StreamProxyItem> list(@RequestParam(required = false)Integer page,
                                          @RequestParam(required = false)Integer count,
                                          @RequestParam(required = false)String q,
                                          @RequestParam(required = false)Boolean online ){

        return streamProxyService.getAll(page, count);
    }

    @RequestMapping(value = "/save")
    @ResponseBody
    public Object save(@RequestBody StreamProxyItem param){
        logger.info("添加代理： " + JSONObject.toJSONString(param));
        streamProxyService.save(param);
        return "success";
    }

    @RequestMapping(value = "/del")
    @ResponseBody
    public Object del(String app, String stream){
        logger.info("移除代理： " + app + "/" + stream);
        streamProxyService.del(app, stream);
        return "success";
    }

    @RequestMapping(value = "/start")
    @ResponseBody
    public Object start(String app, String stream){
        logger.info("启用代理： " + app + "/" + stream);
        boolean result = streamProxyService.start(app, stream);
        return "success";
    }

    @RequestMapping(value = "/stop")
    @ResponseBody
    public Object stop(String app, String stream){
        logger.info("停用代理： " + app + "/" + stream);
        boolean result = streamProxyService.stop(app, stream);
        return "success";
    }
}
