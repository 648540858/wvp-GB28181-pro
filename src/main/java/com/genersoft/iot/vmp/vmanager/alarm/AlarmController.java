package com.genersoft.iot.vmp.vmanager.alarm;

import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.service.IAlarmService;
import com.genersoft.iot.vmp.service.bean.AlarmType;
import com.genersoft.iot.vmp.service.bean.Alarm;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

@Tag(name = "报警管理接口")
@Slf4j
@RestController
@RequestMapping("/api/alarm")
@RequiredArgsConstructor
public class AlarmController {

    private final IAlarmService alarmService;

    @ResponseBody
    @GetMapping("/list")
    @Operation(summary = "分页查询报警列表", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页查询数量", required = true)
    @Parameter(name = "alarmType", description = "报警类型列表，多个类型用逗号分隔")
    @Parameter(name = "beginTime", description = "开始时间，格式：yyyy-MM-dd HH:mm:ss")
    @Parameter(name = "endTime", description = "结束时间，格式：yyyy-MM-dd HH:mm:ss")
    public PageInfo<Alarm> list(@RequestParam Integer page,
                                @RequestParam Integer count,
                                @RequestParam(required = false) List<AlarmType> alarmType,
                                @RequestParam(required = false) String beginTime,
                                @RequestParam(required = false) String endTime) {
        return alarmService.getAlarms(page, count, alarmType, beginTime, endTime);
    }

    @ResponseBody
    @DeleteMapping("/delete")
    @Operation(summary = "删除报警信息", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "ids", description = "报警ID列表", required = true)
    public void delete(@RequestBody List<Long> ids) {
        alarmService.deleteAlarmInfo(ids);
    }

    @ResponseBody
    @DeleteMapping("/clear")
    @Operation(summary = "按筛选条件清空报警信息", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "alarmType", description = "报警类型列表，为空则不限类型")
    @Parameter(name = "beginTime", description = "开始时间，格式：yyyy-MM-dd HH:mm:ss")
    @Parameter(name = "endTime", description = "结束时间，格式：yyyy-MM-dd HH:mm:ss")
    public int clear(@RequestParam(required = false) List<AlarmType> alarmType,
                     @RequestParam(required = false) String beginTime,
                     @RequestParam(required = false) String endTime) {
        return alarmService.clearAlarmsByCondition(alarmType, beginTime, endTime);
    }

    @GetMapping("/snap/{id}")
    @Operation(summary = "获取报警快照图片")
    @Parameter(name = "id", description = "报警ID", required = true)
    public void snap(HttpServletResponse resp, @PathVariable Long id) {
        String snapPath = alarmService.getAlarmSnapById(id);
        if (snapPath == null || snapPath.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        try {
            File file = new File(snapPath);
            if (!file.exists()) {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }
            try (InputStream in = Files.newInputStream(file.toPath())) {
                resp.setContentType(MediaType.IMAGE_JPEG_VALUE);
                IOUtils.copy(in, resp.getOutputStream());
            }
        } catch (IOException e) {
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }
}
