package com.genersoft.iot.vmp.vmanager.security;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.conf.security.RuntimeSecurityConfigService;
import com.genersoft.iot.vmp.conf.security.SecurityUtils;
import com.genersoft.iot.vmp.conf.security.dto.LoginUser;
import com.genersoft.iot.vmp.conf.security.dto.RuntimeSecurityConfig;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.SecurityConfigInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Tag(name = "安全配置")
@Slf4j
@RestController
@RequestMapping("/api/server/security")
public class SecurityConfigController {

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private Environment environment;

    @Autowired
    private RuntimeSecurityConfigService runtimeSecurityConfigService;

    @GetMapping("/config")
    @Operation(summary = "获取当前安全配置信息", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public SecurityConfigInfo getConfig() {
        RuntimeSecurityConfig runtimeConfig = runtimeSecurityConfigService.getConfig();
        SecurityConfigInfo result = new SecurityConfigInfo();
        List<String> allowedOrigins = safeList(runtimeConfig.getAllowedOrigins());
        boolean httpsEnabled = environment.getProperty("server.ssl.enabled", Boolean.class, false);
        boolean fastjson1Present = ClassUtils.isPresent("com.alibaba.fastjson.JSON",
                SecurityConfigController.class.getClassLoader());

        result.setInterfaceAuthentication(runtimeConfig.getInterfaceAuthentication());
        result.setInterfaceAuthenticationExcludes(safeList(runtimeConfig.getInterfaceAuthenticationExcludes()));
        result.setLoginTimeoutMinutes(runtimeConfig.getLoginTimeoutMinutes());
        result.setJwtExpirationMinutes(runtimeConfig.getLoginTimeoutMinutes());
        result.setAccessTokenHeader(JwtUtils.getHeader());
        result.setApiKeyHeader(JwtUtils.getApiKeyHeader());
        result.setPasswordEncoder("BCrypt");
        result.setSessionCreationPolicy("ALWAYS");
        result.setCsrfEnabled(false);
        result.setContentTypeOptionsEnabled(runtimeConfig.getContentTypeOptionsEnabled());
        result.setAllowedOrigins(allowedOrigins);
        result.setAllowAllOrigins(allowedOrigins.isEmpty());
        result.setDocEnabled(runtimeConfig.getDocEnabled());
        result.setHttpsEnabled(httpsEnabled);
        result.setServerPort(environment.getProperty("server.port", Integer.class, 8080));
        result.setJwkFile(StringUtils.hasText(userSetting.getJwkFile())
                ? userSetting.getJwkFile() : "config/jwk.json（默认外部文件）");
        result.setSipCacheServerConnections(userSetting.isSipCacheServerConnections());
        result.setJsonLibrary("Fastjson2");
        String fastjson2Version = JSON.class.getPackage().getImplementationVersion();
        result.setJsonLibraryVersion(fastjson2Version == null ? "未知" : fastjson2Version);
        result.setFastjson1Present(fastjson1Present);
        result.setFastjsonRiskStatus(fastjson1Present
                ? "检测到 Fastjson 1.x，请立即移除 1.2.68 至 1.2.83 版本"
                : "未检测到 Fastjson 1.x，当前运行时使用 Fastjson2");
        result.setWarnings(buildWarnings(result));
        return result;
    }

    @PutMapping("/config")
    @Operation(summary = "更新运行时安全配置", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public SecurityConfigInfo updateConfig(@RequestBody RuntimeSecurityConfig config, HttpServletResponse response) {
        LoginUser loginUser = assertAdmin();
        RuntimeSecurityConfig updatedConfig;
        try {
            updatedConfig = runtimeSecurityConfigService.update(config);
        } catch (IllegalArgumentException exception) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), exception.getMessage());
        } catch (Exception exception) {
            log.error("保存运行时安全配置失败", exception);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "安全配置保存失败");
        }
        Long loginTimeout = updatedConfig.getLoginTimeoutMinutes();
        String renewedToken = JwtUtils.createToken(loginUser.getUsername(), loginTimeout == 0 ? null : loginTimeout);
        if (!StringUtils.hasText(renewedToken)) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "登录令牌刷新失败，请重新登录");
        }
        response.setHeader(JwtUtils.getHeader(), renewedToken);
        return getConfig();
    }

    private LoginUser assertAdmin() {
        LoginUser loginUser = SecurityUtils.getUserInfo();
        if (loginUser == null || loginUser.getRole() == null || loginUser.getRole().getId() != 1) {
            throw new ControllerException(ErrorCode.ERROR403);
        }
        return loginUser;
    }

    private List<String> buildWarnings(SecurityConfigInfo config) {
        List<String> warnings = new ArrayList<>();
        if (Boolean.TRUE.equals(config.getFastjson1Present())) {
            warnings.add("运行时类路径存在 Fastjson 1.x，可能受到远程代码执行漏洞影响");
        }
        if (!Boolean.TRUE.equals(config.getInterfaceAuthentication())) {
            warnings.add("接口鉴权已关闭，除明确匿名接口外建议保持开启");
        }
        if (Boolean.TRUE.equals(config.getAllowAllOrigins())) {
            warnings.add("跨域来源未限制，当前允许任意来源携带凭据访问");
        }
        if (!Boolean.TRUE.equals(config.getHttpsEnabled())) {
            warnings.add("HTTPS 未启用，登录凭据和访问令牌可能以明文传输");
        }
        if (Boolean.TRUE.equals(config.getDocEnabled())) {
            warnings.add("接口文档页面已开启，生产环境建议关闭");
        }
        if (!Boolean.TRUE.equals(config.getCsrfEnabled())) {
            warnings.add("CSRF 防护已关闭，请确保写接口仅接受不会被浏览器自动携带的访问令牌");
        }
        if (!Boolean.TRUE.equals(config.getContentTypeOptionsEnabled())) {
            warnings.add("X-Content-Type-Options 安全响应头未启用");
        }
        if (Boolean.TRUE.equals(config.getSipCacheServerConnections())) {
            warnings.add("SIP 服务端连接缓存已开启，高并发恶意注册场景可能增加拒绝服务风险");
        }
        return warnings;
    }

    private List<String> safeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(values);
    }
}
