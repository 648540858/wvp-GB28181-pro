package com.genersoft.iot.vmp.gat1400.fontend.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

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

import cz.data.viid.fe.domain.DataSelectOption;
import cz.data.viid.fe.domain.ServerQuery;
import cz.data.viid.framework.domain.core.BaseResponse;
import cz.data.viid.framework.domain.core.SearchDataResponse;
import cz.data.viid.framework.domain.core.SimpleDataResponse;
import cz.data.viid.framework.domain.entity.VIIDServer;
import cz.data.viid.framework.service.VIIDServerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(tags = {"视图库"})
@RestController
public class VIIDServerController {

    @Resource
    VIIDServerService viidServerService;

    @ApiOperation(value = "视图库-分页列表")
    @GetMapping("/api/viid/server/page")
    public SearchDataResponse<VIIDServer> page(ServerQuery request) {
        request.setExcludeSelf(true);
        Page<VIIDServer> page = viidServerService.page(request);
        return new SearchDataResponse<>(page.getRecords(), page.getTotal());
    }

    @ApiOperation(value = "视图库-下拉框选项")
    @GetMapping("/api/viid/server/options")
    public SearchDataResponse<DataSelectOption> serverOptions(ServerQuery request) {
        request.setPageNum(1);
        request.setPageSize(1000);
        Page<VIIDServer> page = viidServerService.list(request);
        List<DataSelectOption> collect = page.getRecords().stream()
                .map(ele -> DataSelectOption.from(ele.getServerId(), ele.getServerName(), ele.getCategory()))
                .collect(Collectors.toList());
        return new SearchDataResponse<>(collect, page.getTotal());
    }

    @ApiOperation(value = "视图库-详情")
    @GetMapping("/api/viid/server/{id}")
    public SimpleDataResponse<VIIDServer> get(@PathVariable String id) {
        return new SimpleDataResponse<>(viidServerService.getById(id));
    }

    @ApiOperation(value = "视图库-插入更新")
    @PostMapping("/api/viid/server/upsert")
    public BaseResponse upsert(@RequestBody VIIDServer request) {
        return BaseResponse.withBoolean(viidServerService.upsert(request));
    }

    @ApiOperation(value = "视图库-删除")
    @DeleteMapping("/api/viid/server/{ids}")
    public BaseResponse remove(@PathVariable String[] ids) {
        boolean res = viidServerService.removeByIds(Arrays.asList(ids));
        Arrays.asList(ids).forEach(viidServerService::changeDomain);
        return BaseResponse.withBoolean(res);
    }

    @ApiOperation(value = "视图库-启用状态变更")
    @PutMapping("/api/viid/server/change/enable")
    public BaseResponse changeServerEnable(@RequestBody VIIDServer request) {
        VIIDServer server = new VIIDServer();
        server.setServerId(request.getServerId());
        server.setEnabled(request.getEnabled());
        boolean b = viidServerService.updateById(server);
        if (b) {
            viidServerService.changeDomain(server.getServerId());
        }
        return BaseResponse.withBoolean(b);
    }

    @ApiOperation(value = "视图库-双向保活状态变更")
    @PutMapping("/api/viid/server/change/keepalive")
    public BaseResponse changeServerKeepalive(@RequestBody VIIDServer request) {
        VIIDServer server = new VIIDServer();
        server.setServerId(request.getServerId());
        server.setKeepalive(request.getKeepalive());
        boolean b = viidServerService.updateById(server);
        if (b) {
            viidServerService.changeDomain(server.getServerId());
        }
        return BaseResponse.withBoolean(b);
    }

    @ApiOperation(value = "视图库-当前节点详情")
    @GetMapping("/api/viid/server/me")
    public SimpleDataResponse<VIIDServer> me() {
        return new SimpleDataResponse<>(viidServerService.getCurrentServer());
    }

    @ApiOperation(value = "视图库-更新当前节点")
    @PostMapping("/api/viid/server/me")
    public BaseResponse me(@RequestBody VIIDServer request) {
        boolean res = viidServerService.maintenance(request);
        return BaseResponse.withBoolean(res);
    }
}
