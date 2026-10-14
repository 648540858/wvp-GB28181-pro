package com.genersoft.iot.vmp.vmanager.setting;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.service.IApplicationRestartService;
import com.genersoft.iot.vmp.service.ISettingService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.JtServerConfig;
import com.genersoft.iot.vmp.vmanager.bean.ProfileInfo;
import com.genersoft.iot.vmp.vmanager.bean.SipServerConfig;
import com.genersoft.iot.vmp.vmanager.bean.UserSettingConfig;
import com.genersoft.iot.vmp.vmanager.bean.UserSettingSaveResult;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.genersoft.iot.vmp.vmanager.bean.WebServerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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

    @ResponseBody
    @GetMapping("/web-server")
    @Operation(summary = "查询WEB服务配置", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public WebServerConfig queryWebServerConfig() {
        return settingService.queryWebServerConfig();
    }

    @ResponseBody
    @PostMapping("/web-server")
    @Operation(summary = "保存WEB服务配置", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public WVPResult<String> saveWebServerConfig(@RequestBody WebServerConfig config) {
        String path = settingService.saveWebServerConfig(config);
        return WVPResult.success(path, "配置已保存, 重启服务后生效");
    }

    @ResponseBody
    @GetMapping("/sip-server")
    @Operation(summary = "查询国标服务配置", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public SipServerConfig querySipServerConfig() {
        return settingService.querySipServerConfig();
    }

    @ResponseBody
    @PostMapping("/sip-server")
    @Operation(summary = "保存国标服务配置", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public WVPResult<String> saveSipServerConfig(@RequestBody SipServerConfig config) {
        String path = settingService.saveSipServerConfig(config);
        return WVPResult.success(path, "配置已保存, 重启服务后生效");
    }

    @ResponseBody
    @GetMapping("/jt-server")
    @Operation(summary = "查询部标服务配置", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public JtServerConfig queryJtServerConfig() {
        return settingService.queryJtServerConfig();
    }

    @ResponseBody
    @PostMapping("/jt-server")
    @Operation(summary = "保存部标服务配置", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public WVPResult<String> saveJtServerConfig(@RequestBody JtServerConfig config) {
        String path = settingService.saveJtServerConfig(config);
        return WVPResult.success(path, "配置已保存, 重启服务后生效");
    }

    @ResponseBody
    @GetMapping("/user-setting")
    @Operation(summary = "查询平台策略配置", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public UserSettingConfig queryUserSettingConfig() {
        return settingService.queryUserSettingConfig();
    }

    @ResponseBody
    @PostMapping("/user-setting")
    @Operation(summary = "保存平台策略配置", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public WVPResult<UserSettingSaveResult> saveUserSettingConfig(@RequestBody UserSettingConfig config) {
        UserSettingSaveResult result = settingService.saveUserSettingConfig(config);
        String message = result.isRestartRequired() ? "配置已保存，部分配置需重启后生效" : "配置已保存并已生效";
        return WVPResult.success(result, message);
    }

    @ResponseBody
    @GetMapping("/alarm-types")
    @Operation(summary = "查询报警订阅类型列表", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public List<Map<String, String>> queryAlarmTypes() {
        return settingService.queryAlarmTypes();
    }
}
