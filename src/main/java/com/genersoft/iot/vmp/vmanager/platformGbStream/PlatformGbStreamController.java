package com.genersoft.iot.vmp.vmanager.platformGbStream;

import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.gb28181.bean.PlatformGbStream;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.vmanager.service.IGbStreamService;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api")
public class PlatformGbStreamController {

    private final static Logger logger = LoggerFactory.getLogger(PlatformGbStreamController.class);

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


}
