package com.genersoft.iot.vmp.vmanager.region;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.Gb28181CodeType;
import com.genersoft.iot.vmp.service.IRegionService;
import com.genersoft.iot.vmp.service.bean.Region;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    @GetMapping("/list")
    public PageInfo<Region> query(
            @RequestParam(required = false) String query,
            @RequestParam(required = true) int page,
            @RequestParam(required = true) int count
    ){
        return regionService.query(query, page, count);
    }

    @Operation(summary = "更新区域")
    @Parameter(name = "region", description = "Region", required = true)
    @ResponseBody
    @PostMapping("/update")
    public void update(@RequestBody Region region){
        regionService.update(region);
    }

    @Operation(summary = "删除区域")
    @Parameter(name = "regionDeviceId", description = "区域编码", required = true)
    @ResponseBody
    @GetMapping("/delete")
    public void delete(String regionDeviceId){
        assert regionDeviceId != null;
        boolean result = regionService.deleteByDeviceId(regionDeviceId);
        if (!result) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "移除失败");
        }
    }

    @Operation(summary = "分页区域子节点")
    @Parameter(name = "regionParentId", description = "行政区划父节点编号", required = true)
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页查询数量", required = true)
    @ResponseBody
    @GetMapping("/child/list")
    public PageInfo<Region> queryChildGroupList(
            @RequestParam(required = true) String regionParentId,
            @RequestParam(required = true) int page,
            @RequestParam(required = true) int count
    ){
        if (ObjectUtils.isEmpty(regionParentId.trim())) {
            regionParentId = null;
        }
        return regionService.queryChildGroupList(regionParentId, page, count);
    }

    @Operation(summary = "获取所属的行政区划下的行政区划")
    @Parameter(name = "parent", description = "所属的行政区划", required = false)
    @ResponseBody
    @GetMapping("/base/child/list")
    public List<Region> getAllChild(@RequestParam(required = false) String parent){
        if (ObjectUtils.isEmpty(parent.trim())) {
            parent = null;
        }
        return regionService.getAllChild(parent);
    }
}
