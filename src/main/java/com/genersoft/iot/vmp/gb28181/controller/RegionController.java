package com.genersoft.iot.vmp.gb28181.controller;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.Region;
import com.genersoft.iot.vmp.gb28181.bean.RegionTree;
import com.genersoft.iot.vmp.gb28181.service.IRegionService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "区域管理")
@RestController
@RequestMapping("/api/region")
public class RegionController {

    private final static Logger logger = LoggerFactory.getLogger(RegionController.class);

    @Autowired
    private IRegionService regionService;

    @Operation(summary = "添加区域")
    @Parameter(name = "region", description = "Region", required = true)
    @ResponseBody
    @PostMapping("/add")
    public void add(@RequestBody Region region){
        regionService.add(region);
    }

    @Operation(summary = "查询区域")
    @Parameter(name = "query", description = "要搜索的内容", required = true)
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页查询数量", required = true)
    @ResponseBody
    @GetMapping("/page/list")
    public PageInfo<Region> query(
            @RequestParam(required = false) String query,
            @RequestParam(required = true) int page,
            @RequestParam(required = true) int count
    ){
        return regionService.query(query, page, count);
    }

    @Operation(summary = "查询区域")
    @Parameter(name = "query", description = "要搜索的内容", required = true)
    @Parameter(name = "parent", description = "所属行政区划编号", required = true)
    @ResponseBody
    @GetMapping("/tree/list")
    public List<RegionTree> queryForTree(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer parent,
            @RequestParam(required = false) Boolean hasChannel
    ){
        if (ObjectUtils.isEmpty(query)) {
            query = null;
        }
        return regionService.queryForTree(query, parent, hasChannel);
    }

    @Operation(summary = "更新区域")
    @Parameter(name = "region", description = "Region", required = true)
    @ResponseBody
    @PostMapping("/update")
    public void update(@RequestBody Region region){
        regionService.update(region);
    }

    @Operation(summary = "删除区域")
    @Parameter(name = "id", description = "区域ID", required = true)
    @ResponseBody
    @DeleteMapping("/delete")
    public void delete(Integer id){
        Assert.notNull(id, "区域ID需要存在");
        boolean result = regionService.deleteByDeviceId(id);
        if (!result) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "移除失败");
        }
    }

    @Operation(summary = "根据区域Id查询区域")
    @Parameter(name = "regionDeviceId", description = "行政区划节点编号", required = true)
    @ResponseBody
    @GetMapping("/one")
    public Region queryRegionByDeviceId(
            @RequestParam(required = true) String regionDeviceId
    ){
        if (ObjectUtils.isEmpty(regionDeviceId.trim())) {
            throw new ControllerException(ErrorCode.ERROR400);
        }
        return regionService.queryRegionByDeviceId(regionDeviceId);
    }

    @Operation(summary = "获取所属的行政区划下的行政区划")
    @Parameter(name = "parent", description = "所属的行政区划", required = false)
    @ResponseBody
    @GetMapping("/base/child/list")
    public List<Region> getAllChild(@RequestParam(required = false) String parent){
        if (ObjectUtils.isEmpty(parent)) {
            parent = null;
        }
        return regionService.getAllChild(parent);
    }

    @Operation(summary = "获取所属的行政区划下的行政区划")
    @Parameter(name = "deviceId", description = "当前的行政区划", required = false)
    @ResponseBody
    @GetMapping("/path")
    public List<Region> getPath(String deviceId){
        return regionService.getPath(deviceId);
    }

    @Operation(summary = "从通道中同步行政区划")
    @ResponseBody
    @GetMapping("/sync")
    public void sync(){
        regionService.syncFromChannel();
    }

    @Operation(summary = "根据行政区划编号从文件中查询层级和描述")
    @ResponseBody
    @GetMapping("/description")
    public String getDescription(String civilCode){
        return regionService.getDescription(civilCode);
    }

    @Operation(summary = "根据行政区划编号从文件中查询层级并添加")
    @ResponseBody
    @GetMapping("/addByCivilCode")
    public void addByCivilCode(String civilCode){
        regionService.addByCivilCode(civilCode);
    }


}
