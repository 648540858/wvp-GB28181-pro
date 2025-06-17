package com.genersoft.iot.vmp.gat1400.fontend.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.genersoft.iot.vmp.gat1400.fontend.DictContextHolder;
import com.genersoft.iot.vmp.gat1400.fontend.domain.DataSelectOption;
import com.genersoft.iot.vmp.gat1400.fontend.domain.TollgateQuery;
import com.genersoft.iot.vmp.gat1400.framework.domain.core.BaseResponse;
import com.genersoft.iot.vmp.gat1400.framework.domain.core.SearchDataResponse;
import com.genersoft.iot.vmp.gat1400.framework.domain.core.SimpleDataResponse;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.TollgateDevice;
import com.genersoft.iot.vmp.gat1400.framework.service.TollgateDeviceService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(tags = {"视图库卡口设备"})
@RestController
public class VIIDTollgateController {

    @Resource
    TollgateDeviceService service;

    @ApiOperation(value = "视图库卡口设备-分页列表")
    @GetMapping("/api/viid/tollgate/page")
    public SearchDataResponse<TollgateDevice> page(TollgateQuery request) {
        Page<TollgateDevice> page = service.page(request);
        for (TollgateDevice record : page.getRecords()) {
            DictContextHolder.setDictValue("tollgateCatType", record::getTollgateCat, record::setTollgateCat);
            DictContextHolder.setDictValue("tollgateUsageType", record::getTollgateUsage, record::setTollgateUsage);
        }
        return new SearchDataResponse<>(page.getRecords(), page.getTotal());
    }

    @ApiOperation(value = "视图库卡口设备-详情")
    @GetMapping("/api/viid/tollgate/{id}")
    public SimpleDataResponse<TollgateDevice> getInfo(@PathVariable String id) {
        return new SimpleDataResponse<>(service.getById(id));
    }

    @ApiOperation(value = "视图库卡口设备-列表")
    @GetMapping("/api/viid/tollgate/device/options")
    public SearchDataResponse<DataSelectOption> list(TollgateQuery request) {
        request.setPageNum(1);
        request.setPageSize(500);
        Page<TollgateDevice> page = service.page(request);
        List<DataSelectOption> collect = page.getRecords().stream()
                .map(ele -> DataSelectOption.from(ele.getTollgateId(), ele.getName()))
                .collect(Collectors.toList());
        return new SearchDataResponse<>(collect, page.getTotal());
    }

    @ApiOperation(value = "视图库卡口设备-新增")
    @PostMapping("/api/viid/tollgate")
    public BaseResponse addTollgate(@RequestBody TollgateDevice request) {
        return BaseResponse.withBoolean(service.saveTollgate(request));
    }

    @ApiOperation(value = "视图库卡口设备-更新")
    @PutMapping("/api/viid/tollgate")
    public BaseResponse updateTollgate(@RequestBody TollgateDevice request) {
        return BaseResponse.withBoolean(service.updateTollgate(request));
    }

    @ApiOperation(value = "视图库卡口设备-删除")
    @DeleteMapping("/api/viid/tollgate/device/{ids}")
    public BaseResponse remove(@PathVariable String[] ids) {
        boolean res = service.removeByIds(Arrays.asList(ids));
        return BaseResponse.withBoolean(res);
    }
}
