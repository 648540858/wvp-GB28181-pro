package com.genersoft.iot.vmp.gat1400.fontend.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import cz.data.viid.fe.DictContextHolder;
import cz.data.viid.fe.domain.LaneQuery;
import cz.data.viid.framework.domain.core.BaseResponse;
import cz.data.viid.framework.domain.core.SearchDataResponse;
import cz.data.viid.framework.domain.core.SimpleDataResponse;
import cz.data.viid.framework.domain.entity.Lane;
import cz.data.viid.framework.domain.entity.TollgateDevice;
import cz.data.viid.framework.service.LaneService;
import cz.data.viid.framework.service.TollgateDeviceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(tags = {"web车道API"})
@RestController
public class LaneController {

    @Resource
    LaneService service;
    @Resource
    TollgateDeviceService tollgateDeviceService;

    @ApiOperation(value = "卡口车道-分页列表")
    @GetMapping("/api/viid/lane/page")
    public SearchDataResponse<Lane> page(LaneQuery request) {
        Page<Lane> page = service.pageData(request);
        Map<String, TollgateDevice> buffer = new HashMap<>();
        for (Lane laneObject : page.getRecords()) {
            TollgateDevice tollgateDevice = buffer.computeIfAbsent(laneObject.getTollgateId(), tollgateDeviceService::getById);
            if (tollgateDevice != null) {
                laneObject.setTollgateId(tollgateDevice.getName());
            }
            DictContextHolder.setDictValue("directionType", laneObject::getDirection, laneObject::setDirection);
        }
        return new SearchDataResponse<>(page.getRecords(), page.getTotal());
    }

    @ApiOperation(value = "卡口车道-详情")
    @GetMapping("/api/viid/lane/{id}")
    public SimpleDataResponse<Lane> getInfo(@PathVariable String id) {
        return new SimpleDataResponse<>(service.getById(id));
    }

    @ApiOperation(value = "卡口车道-新增")
    @PostMapping("/api/viid/lane")
    public BaseResponse addLane(@RequestBody @Validated Lane request) {
        if (service.getData(request.getTollgateId(), request.getLaneId()) == null) {
            boolean saved = service.saveData(request);
            return BaseResponse.withBoolean(saved);
        } else {
            String message = String.format("已存在: 卡口编号[%s]和车道ID[%s]", request.getTollgateId(), request.getLaneId());
            return BaseResponse.error(500, message);
        }
    }

    @ApiOperation(value = "卡口车道-更新")
    @PutMapping("/api/viid/lane")
    public BaseResponse updateLane(@RequestBody @Validated Lane request) {
        boolean updated = service.updateData(request);
        return BaseResponse.withBoolean(updated);
    }

    @ApiOperation(value = "卡口车道-删除")
    @DeleteMapping("/api/viid/lane/{ids}")
    public BaseResponse remove(@PathVariable String[] ids) {
        service.removeData(ids);
        return BaseResponse.success();
    }
}
