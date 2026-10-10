package com.genersoft.iot.vmp.vmanager.setting;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.service.IApplicationRestartService;
import com.genersoft.iot.vmp.service.ISettingService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.ProfileInfo;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@SuppressWarnings("rawtypes")
@Tag(name = "系统设置接口")
@Slf4j
@RestController
@RequestMapping("/api/setting")
public class SettingController {

    @Autowired
    private ISettingService settingService;

    @Autowired
    private IApplicationRestartService applicationRestartService;

    @ResponseBody
    @GetMapping("/profiles")
    @Operation(summary = "查询配置文件列表", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public List<ProfileInfo> queryProfiles() {
        return settingService.queryProfiles();
    }

    @ResponseBody
    @GetMapping("/profile/content")
    @Operation(summary = "查看配置文件内容", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "profileId", description = "配置文件ID", required = true)
    public WVPResult<String> profileContent(@RequestParam String profileId) {
        String content = settingService.readProfileContent(profileId);
        return WVPResult.success(content);
    }

    @ResponseBody
    @PostMapping("/profile/switch")
    @Operation(summary = "切换配置文件", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "profileId", description = "配置文件ID, 即 application-{id}.yml 中的 {id}", required = true)
    public WVPResult<String> switchProfile(@RequestParam String profileId) {
        String path = settingService.switchProfile(profileId);
        return WVPResult.success(path, "配置文件已切换, 重启服务后生效");
    }

    @ResponseBody
    @PostMapping("/restart")
    @Operation(summary = "重启服务", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public WVPResult<String> restart() {
        boolean success = applicationRestartService.restart();
        if (!success) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "触发重启失败, 请检查重启脚本是否存在于jar同级目录");
        }
        return WVPResult.success(null, "服务将在数秒后重启, 请稍后刷新页面");
    }
}
