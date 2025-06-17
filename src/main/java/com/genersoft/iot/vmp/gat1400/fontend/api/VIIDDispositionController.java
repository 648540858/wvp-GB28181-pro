package com.genersoft.iot.vmp.gat1400.fontend.api;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.genersoft.iot.vmp.gat1400.fontend.DictContextHolder;
import com.genersoft.iot.vmp.gat1400.fontend.domain.DispositionQuery;
import com.genersoft.iot.vmp.gat1400.framework.domain.core.SearchDataResponse;
import com.genersoft.iot.vmp.gat1400.framework.domain.core.SimpleDataResponse;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.ResponseStatusObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDDisposition;
import com.genersoft.iot.vmp.gat1400.framework.domain.vo.VIIDResponseStatusObject;
import com.genersoft.iot.vmp.gat1400.framework.service.IVIIDDispositionService;
import com.genersoft.iot.vmp.gat1400.utils.StructCodec;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.ResponseStatusListObject;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javax.annotation.Resource;

import io.swagger.annotations.ApiOperation;

/**
 * 布控接口
 */
@RestController
public class VIIDDispositionController {

    @Resource
    IVIIDDispositionService dispositionService;

    @ApiOperation(value = "布控-分页列表")
    @GetMapping("/api/viid/disposition/page")
    public SearchDataResponse<VIIDDisposition> page(DispositionQuery request) {
        QueryWrapper<VIIDDisposition> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(StringUtils.isNotBlank(request.getDeviceId()),
                VIIDDisposition::getServerId, request.getDeviceId());
        wrapper.lambda().orderByDesc(VIIDDisposition::getId);
        Page<VIIDDisposition> page = dispositionService.page(request.pageable(), wrapper);
        for (VIIDDisposition record : page.getRecords()) {
            DictContextHolder.setDictValue("DispositionCategoryType", record::getDispositionCategory, record::setDispositionCategory);
        }
        return new SearchDataResponse<>(page.getRecords(), page.getTotal());
    }

    @ApiOperation(value = "布控-详情")
    @GetMapping("/api/viid/disposition/{id}")
    public SimpleDataResponse<VIIDDisposition> getInfo(@PathVariable("id") String id) {
        return new SimpleDataResponse<>(dispositionService.getById(id));
    }

    @ApiOperation(value = "布控-新增")
    @PostMapping("/api/viid/disposition")
    public VIIDResponseStatusObject add(@RequestBody VIIDDisposition request) {
        request.setDispositionId(StructCodec.randomDispositionId());
        return dispositionService.createRemote(request);
    }

    @ApiOperation(value = "布控-修改")
    @PutMapping("/api/viid/disposition")
    public VIIDResponseStatusObject edit(@RequestBody VIIDDisposition request) {
        return dispositionService.updateRemote(request);
    }

    @ApiOperation(value = "布控-撤控")
    @PutMapping("/api/viid/disposition/revoke")
    public VIIDResponseStatusObject revoke(@RequestParam("dispositionId") String dispositionId) {
        return dispositionService.revokeRemote(dispositionId);
    }

    @ApiOperation(value = "布控-删除")
    @DeleteMapping("/api/viid/disposition/{ids}")
    public VIIDResponseStatusObject remove(@PathVariable String[] ids) {
        ResponseStatusObject[] statusObjects = Stream.of(ids)
                .map(dispositionService::deleteRemote)
                .filter(Objects::nonNull)
                .map(VIIDResponseStatusObject::getResponseStatusListObject)
                .map(ResponseStatusListObject::getResponseStatusObject)
                .flatMap(List::stream)
                .toArray(ResponseStatusObject[]::new);
        return VIIDResponseStatusObject.from(statusObjects);
    }
}
