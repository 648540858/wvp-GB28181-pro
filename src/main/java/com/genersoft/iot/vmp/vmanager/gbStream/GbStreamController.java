package com.genersoft.iot.vmp.vmanager.gbStream;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.vmanager.gbStream.bean.GbStreamParam;
import com.genersoft.iot.vmp.vmanager.platform.bean.UpdateChannelParam;
import com.genersoft.iot.vmp.vmanager.service.IGbStreamService;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/gbStream")
public class GbStreamController {

    private final static Logger logger = LoggerFactory.getLogger(GbStreamController.class);

    @Autowired
    private IGbStreamService gbStreamService;

    @Autowired
    private IVideoManagerStorager storager;


    @RequestMapping(value = "/list")
    @ResponseBody
    public PageInfo<GbStream> list(@RequestParam(required = false)Integer page,
                                   @RequestParam(required = false)Integer count){

        return gbStreamService.getAll(page, count);
    }


    @RequestMapping(value = "/del")
    @ResponseBody
    public Object del(@RequestBody GbStreamParam gbStreamParam){
        System.out.println(2222);
        System.out.println(gbStreamParam.getGbStreams().size());
        if (gbStreamService.delPlatformInfo(gbStreamParam.getGbStreams())) {
            return "success";
        }else {
            return "fail";
        }

    }

    @RequestMapping(value = "/add")
    @ResponseBody
    public Object add(@RequestBody GbStreamParam gbStreamParam){
        System.out.println(3333);
        System.out.println(gbStreamParam.getGbStreams().size());
        if (gbStreamService.addPlatformInfo(gbStreamParam.getGbStreams(), gbStreamParam.getPlatformId())) {
            return "success";
        }else {
            return "fail";
        }
    }
}
