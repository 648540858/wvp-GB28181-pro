package com.genersoft.iot.vmp.vmanager.recordPlan;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.service.bean.RecordPlan;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "录制计划")
@Slf4j
@RestController
@RequestMapping("/api/record/plan")
public class RecordPlanController {

    @ResponseBody
    @PostMapping("/add")
    @Operation(summary = "添加录制计划", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "channelId", description = "通道ID", required = true)
    @Parameter(name = "deviceDbId", description = "国标设备ID", required = true)
    @Parameter(name = "planList", description = "录制计划, 为空则清空计划", required = false)
    public void openRtpServer(@RequestParam(required = false) Integer channelId, @RequestParam(required = false) Integer deviceDbId, @RequestParam(required = false) List<RecordPlan> planList

    ) {
        if (channelId == null && deviceDbId == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "通道ID和国标设备ID不可都为NULL");
        }



    }
}
