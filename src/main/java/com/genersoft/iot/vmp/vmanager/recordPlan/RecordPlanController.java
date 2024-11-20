package com.genersoft.iot.vmp.vmanager.recordPlan;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.service.IRecordPlanService;
import com.genersoft.iot.vmp.service.bean.RecordPlan;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.recordPlan.bean.RecordPlanParam;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "录制计划")
@Slf4j
@RestController
@RequestMapping("/api/record/plan")
public class RecordPlanController {

    @Autowired
    private IRecordPlanService recordPlanService;

    @Autowired
    private IDeviceChannelService deviceChannelService;


    @ResponseBody
    @PostMapping("/add")
    @Operation(summary = "添加录制计划", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "plan", description = "计划", required = true)
    public void add(@RequestBody RecordPlan plan) {
        if (plan.getPlanItemList() == null || plan.getPlanItemList().isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "添加录制计划时，录制计划不可为空");
        }
        recordPlanService.add(plan);
    }

    @ResponseBody
    @PostMapping("/linke")
    @Operation(summary = "通道关联录制计划", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "param", description = "通道关联录制计划", required = false)
    public void linke(@RequestBody RecordPlanParam param) {
        if (param.getChannelId() == null && param.getDeviceDbId() == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "通道ID和国标设备ID不可都为NULL");
        }
        List<Integer> channelIds = new ArrayList<>();
        if (param.getChannelId() != null) {
            channelIds.add(param.getChannelId());
        }else {
            List<Integer> chanelIdList = deviceChannelService.queryChaneIdListByDeviceDbId(param.getDeviceDbId());
            if (chanelIdList == null || chanelIdList.isEmpty()) {
                channelIds = chanelIdList;
            }
        }
        recordPlanService.linke(channelIds, param.getPlanId());
    }

    @ResponseBody
    @GetMapping("/get")
    @Operation(summary = "查询录制计划", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "planId", description = "计划ID", required = true)
    public RecordPlan get(Integer planId) {
        if (planId == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "计划ID不可为NULL");
        }
        return recordPlanService.get(planId);
    }

    @ResponseBody
    @GetMapping("/query")
    @Operation(summary = "查询录制计划列表", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "query", description = "检索内容", required = false)
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页查询数量", required = true)
    public PageInfo<RecordPlan> query(@RequestParam(required = false) String query, @RequestParam Integer page, @RequestParam Integer count) {
        if (query != null && ObjectUtils.isEmpty(query.trim())) {
            query = null;
        }
        return recordPlanService.query(page, count, query);
    }

    @ResponseBody
    @PostMapping("/edit")
    @Operation(summary = "编辑录制计划", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "plan", description = "计划", required = true)
    public void edit(@RequestBody RecordPlan plan) {
        if (plan == null || plan.getId() == 0) {
            throw new ControllerException(ErrorCode.ERROR400);
        }
        recordPlanService.update(plan);
    }

    @ResponseBody
    @DeleteMapping("/delete")
    @Operation(summary = "删除录制计划", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "planId", description = "计划ID", required = true)
    public void delete(Integer planId) {
        if (planId == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "计划IDID不可为NULL");
        }
        recordPlanService.delete(planId);
    }

}
