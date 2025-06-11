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
import cz.data.viid.fe.domain.PersonQuery;
import cz.data.viid.framework.domain.core.BaseResponse;
import cz.data.viid.framework.domain.core.SearchDataResponse;
import cz.data.viid.framework.domain.core.SimpleDataResponse;
import cz.data.viid.framework.domain.entity.VIIDPerson;
import cz.data.viid.framework.service.VIIDPersonService;
import cz.data.viid.utils.DateUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(tags = {"人员"})
@RestController
public class VIIDPersonController {

    @Resource
    VIIDPersonService service;

    @ApiOperation(value = "人员-分页列表")
    @GetMapping("/api/viid/persons/page")
    public SearchDataResponse<VIIDPerson> page(PersonQuery request) {
        QueryWrapper<VIIDPerson> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(StringUtils.isNotBlank(request.getDeviceId()),
                VIIDPerson::getDeviceId, request.getDeviceId());
        wrapper.lambda().between(StringUtils.isNotBlank(request.getStartTime())
                        && StringUtils.isNotBlank(request.getEndTime()),
                VIIDPerson::getDataTime, request.getStartTime(), request.getEndTime()
        );
        wrapper.lambda().orderByDesc(VIIDPerson::getId);
        Page<VIIDPerson> page = service.page(request.pageable(), wrapper);
        for (VIIDPerson record : page.getRecords()) {
            String format = DateUtil.viidDateFormat(record.getPersonAppearTime());
            record.setPersonAppearTime(format);
            format = DateUtil.viidDateFormat(record.getPersonDisAppearTime());
            record.setPersonDisAppearTime(format);
            DictContextHolder.setDictValue("gender", record::getGenderCode, record::setGenderCode);
        }
        return new SearchDataResponse<>(page.getRecords(), page.getTotal());
    }

    @ApiOperation(value = "人员-详情")
    @GetMapping("/api/viid/persons/{id}")
    public SimpleDataResponse<VIIDPerson> getInfo(@PathVariable("id") String id) {
        return new SimpleDataResponse<>(service.getById(id));
    }

    @ApiOperation(value = "人员-新增")
    @PostMapping("/api/viid/persons")
    public BaseResponse add(@RequestBody VIIDPerson request) {
        return BaseResponse.withBoolean(service.save(request));
    }

    @ApiOperation(value = "人员-修改")
    @PutMapping("/api/viid/persons")
    public BaseResponse edit(@RequestBody VIIDPerson request) {
        return BaseResponse.withBoolean(service.updateById(request));
    }

    @ApiOperation(value = "人员-删除")
    @DeleteMapping("/api/viid/persons/{ids}")
    public BaseResponse remove(@PathVariable String[] ids) {
        boolean res = service.removeByIds(Arrays.asList(ids));
        return BaseResponse.withBoolean(res);
    }

}
