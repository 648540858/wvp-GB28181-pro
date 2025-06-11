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
import cz.data.viid.fe.domain.FaceQuery;
import cz.data.viid.framework.domain.core.BaseResponse;
import cz.data.viid.framework.domain.core.SearchDataResponse;
import cz.data.viid.framework.domain.core.SimpleDataResponse;
import cz.data.viid.framework.domain.entity.VIIDFace;
import cz.data.viid.framework.service.VIIDFaceService;
import cz.data.viid.utils.DateUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(tags = {"人脸"})
@RestController
public class VIIDFaceController {

    @Resource
    VIIDFaceService service;

    @ApiOperation(value = "人脸-分页列表")
    @GetMapping("/api/viid/faces/page")
    public SearchDataResponse<VIIDFace> page(FaceQuery request) {
        QueryWrapper<VIIDFace> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(StringUtils.isNotBlank(request.getDeviceId()),
                VIIDFace::getDeviceId, request.getDeviceId());
        wrapper.lambda().between(StringUtils.isNotBlank(request.getStartTime())
                        && StringUtils.isNotBlank(request.getEndTime()),
                VIIDFace::getDataTime, request.getStartTime(), request.getEndTime()
        );
        wrapper.lambda().orderByDesc(VIIDFace::getId);
        Page<VIIDFace> page = service.page(request.pageable(), wrapper);
        for (VIIDFace record : page.getRecords()) {
            String format = DateUtil.viidDateFormat(record.getFaceAppearTime());
            record.setFaceAppearTime(format);
            format = DateUtil.viidDateFormat(record.getFaceDisAppearTime());
            record.setFaceDisAppearTime(format);
            DictContextHolder.setDictValue("gender", record::getGenderCode, record::setGenderCode);
        }
        return new SearchDataResponse<>(page.getRecords(), page.getTotal());
    }

    @ApiOperation(value = "人脸-详情")
    @GetMapping("/api/viid/faces/{id}")
    public SimpleDataResponse<VIIDFace> getInfo(@PathVariable("id") String id) {
        return new SimpleDataResponse<>(service.getById(id));
    }

    @ApiOperation(value = "人脸-新增")
    @PostMapping("/api/viid/faces")
    public BaseResponse add(@RequestBody VIIDFace request) {
        return BaseResponse.withBoolean(service.save(request));
    }

    @ApiOperation(value = "人脸-修改")
    @PutMapping("/api/viid/faces")
    public BaseResponse edit(@RequestBody VIIDFace request) {
        return BaseResponse.withBoolean(service.updateById(request));
    }

    @ApiOperation(value = "人脸-删除")
    @DeleteMapping("/api/viid/faces/{ids}")
    public BaseResponse remove(@PathVariable String[] ids) {
        boolean res = service.removeByIds(Arrays.asList(ids));
        return BaseResponse.withBoolean(res);
    }
}
