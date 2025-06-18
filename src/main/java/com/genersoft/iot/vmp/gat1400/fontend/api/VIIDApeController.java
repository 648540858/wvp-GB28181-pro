package com.genersoft.iot.vmp.gat1400.fontend.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.genersoft.iot.vmp.gat1400.backend.task.action.KeepaliveAction;
import com.genersoft.iot.vmp.gat1400.fontend.domain.APEDeviceQuery;
import com.genersoft.iot.vmp.gat1400.framework.domain.core.BaseResponse;
import com.genersoft.iot.vmp.gat1400.framework.domain.core.SearchDataResponse;
import com.genersoft.iot.vmp.gat1400.framework.domain.core.SimpleDataResponse;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.APEDevice;
import com.genersoft.iot.vmp.gat1400.framework.service.APEDeviceService;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

import javax.annotation.Resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(tags = {"视图库卡口设备"})
@RestController
public class VIIDApeController {

    @Resource
    APEDeviceService service;

    @ApiOperation(value = "视图库APE设备-分页列表")
    @GetMapping("/api/viid/ape/device/page")
    public SearchDataResponse<APEDevice> page(APEDeviceQuery request) {
        Page<APEDevice> page = service.page(request);
        return new SearchDataResponse<>(page.getRecords(), page.getTotal());
    }

    @ApiOperation(value = "视图库APE设备-详情")
    @GetMapping("/api/viid/ape/device/{id}")
    public SimpleDataResponse<APEDevice> getInfo(@PathVariable("id") String id) {
        return new SimpleDataResponse<>(service.getById(id));
    }

    @ApiOperation(value = "视图库APE设备-新增")
    @PostMapping("/api/viid/ape/device")
    public BaseResponse add(@RequestBody APEDevice request) {
        if (StringUtils.isBlank(request.getOwnerApsId())) {
            //如果没有填写所属采集系统则分配到当前视图库下
            request.setOwnerApsId(KeepaliveAction.CURRENT_SERVER_ID);
        }
        return BaseResponse.withBoolean(service.saveDevice(request));
    }

    @ApiOperation(value = "视图库APE设备-修改")
    @PutMapping("/api/viid/ape/device")
    public BaseResponse edit(@RequestBody APEDevice request) {
        if (StringUtils.isBlank(request.getOwnerApsId())) {
            //如果没有填写所属采集系统则分配到当前视图库下
            request.setOwnerApsId(KeepaliveAction.CURRENT_SERVER_ID);
        }
        return BaseResponse.withBoolean(service.updateDevice(request));
    }

    @ApiOperation(value = "视图库APE设备-删除")
    @DeleteMapping("/api/viid/ape/device/{ids}")
    public BaseResponse remove(@PathVariable String[] ids) {
        boolean res = service.removeByIds(Arrays.asList(ids));
        return BaseResponse.withBoolean(res);
    }
}
