package com.genersoft.iot.vmp.vmanager.gb28181.platformGbStream;

import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.service.IGbStreamService;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "级联平台关联视频流")
@CrossOrigin
@RestController
@RequestMapping("/api/platform_gb_stream")
public class PlatformGbStreamController {

    private final static Logger logger = LoggerFactory.getLogger(PlatformGbStreamController.class);

    @Autowired
    private IGbStreamService gbStreamService;

    @Autowired
    private IVideoManagerStorager storager;

    @ApiOperation("分页查询级联平台关联的视频流")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", value = "当前页", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "count", value = "每页条数", dataTypeClass = Integer.class),
    })
    @GetMapping(value = "/list")
    @ResponseBody
    public PageInfo<GbStream> list(@RequestParam(required = false)Integer page,
                                   @RequestParam(required = false)Integer count){

        return gbStreamService.getAll(page, count);
    }


}
