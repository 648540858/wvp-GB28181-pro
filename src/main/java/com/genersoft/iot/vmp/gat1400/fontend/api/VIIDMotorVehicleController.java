package com.genersoft.iot.vmp.gat1400.fontend.api;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

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

import cz.data.viid.fe.DictContextHolder;
import cz.data.viid.fe.domain.MotorVehicleQuery;
import cz.data.viid.framework.domain.core.BaseResponse;
import cz.data.viid.framework.domain.core.SearchDataResponse;
import cz.data.viid.framework.domain.core.SimpleDataResponse;
import cz.data.viid.framework.domain.entity.VIIDMotorVehicle;
import cz.data.viid.framework.service.VIIDMotorVehicleService;
import cz.data.viid.utils.DateUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(tags = {"机动车"})
@RestController
public class VIIDMotorVehicleController {

    @Resource
    VIIDMotorVehicleService service;

    @ApiOperation(value = "机动车-分页列表")
    @GetMapping("/api/viid/motorvehicles/page")
    public SearchDataResponse<VIIDMotorVehicle> page(MotorVehicleQuery request) {
        QueryWrapper<VIIDMotorVehicle> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(StringUtils.isNotBlank(request.getDeviceId()),
                VIIDMotorVehicle::getDeviceId, request.getDeviceId());
        wrapper.lambda().between(StringUtils.isNotBlank(request.getStartTime())
                        && StringUtils.isNotBlank(request.getEndTime()),
                VIIDMotorVehicle::getDataTime, request.getStartTime(), request.getEndTime()
        );
        wrapper.lambda().orderByDesc(VIIDMotorVehicle::getId);
        Page<VIIDMotorVehicle> page = service.page(request.pageable(), wrapper);
        for (VIIDMotorVehicle record : page.getRecords()) {
            String format = DateUtil.viidDateFormat(record.getPassTime());
            record.setPassTime(format);
            format = DateUtil.viidDateFormat(record.getAppearTime());
            record.setAppearTime(format);
            format = DateUtil.viidDateFormat(record.getDisappearTime());
            record.setDisappearTime(format);
            DictContextHolder.setDictValue("color", record::getVehicleColor, record::setVehicleColor);
            DictContextHolder.setDictValue("color", record::getPlateColor, record::setPlateColor);
            DictContextHolder.setDictValue("plateClassType", record::getPlateClass, record::setPlateClass);
            DictContextHolder.setDictValue("vehicleBrandType", record::getVehicleBrand, record::setVehicleBrand);
            DictContextHolder.setDictValue("directionType", record::getDirection, record::setDirection);
        }
        return new SearchDataResponse<>(page.getRecords(), page.getTotal());
    }

    @ApiOperation(value = "机动车-详情")
    @GetMapping("/api/viid/motorvehicles/{id}")
    public SimpleDataResponse<VIIDMotorVehicle> getInfo(@PathVariable("id") String id) {
        return new SimpleDataResponse<>(service.getById(id));
    }

    @ApiOperation(value = "机动车-新增")
    @PostMapping("/api/viid/motorvehicles")
    public BaseResponse add(@RequestBody VIIDMotorVehicle request) {
        return BaseResponse.withBoolean(service.save(request));
    }

    @ApiOperation(value = "机动车-修改")
    @PutMapping("/api/viid/motorvehicles")
    public BaseResponse edit(@RequestBody VIIDMotorVehicle request) {
        return BaseResponse.withBoolean(service.updateById(request));
    }

    @ApiOperation(value = "机动车-删除")
    @DeleteMapping("/api/viid/motorvehicles/{ids}")
    public BaseResponse remove(@PathVariable String[] ids) {
        boolean res = service.removeByIds(Arrays.asList(ids));
        return BaseResponse.withBoolean(res);
    }
}
