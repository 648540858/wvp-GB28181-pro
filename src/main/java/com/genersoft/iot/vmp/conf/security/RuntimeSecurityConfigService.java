package com.genersoft.iot.vmp.conf.security;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.security.dto.RuntimeSecurityConfig;
import com.genersoft.iot.vmp.storager.dao.SecurityConfigMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RuntimeSecurityConfigService {

    static final String REDIS_KEY_PREFIX = "VMP_RUNTIME_SECURITY_CONFIG:";

    private final UserSetting userSetting;

    private final StringRedisTemplate redisTemplate;

    private final SecurityConfigMapper securityConfigMapper;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private volatile RuntimeSecurityConfig runtimeConfig;

    public RuntimeSecurityConfigService(UserSetting userSetting, StringRedisTemplate redisTemplate,
                                        SecurityConfigMapper securityConfigMapper) {
        this.userSetting = userSetting;
        this.redisTemplate = redisTemplate;
        this.securityConfigMapper = securityConfigMapper;
    }

    @PostConstruct
    public void initialize() {
        RuntimeSecurityConfig config = fromUserSetting();
        try {
            securityConfigMapper.createTableIfNotExists();
            String persisted = securityConfigMapper.getConfig(serverId());
            if (StringUtils.hasText(persisted)) {
                config = parse(persisted, config);
                try {
                    persistDatabase(config);
                } catch (Exception exception) {
                    log.warn("补全数据库安全配置失败，继续使用已恢复的配置: {}", exception.getMessage());
                }
            } else {
                config = loadRedis(config);
                persistDatabase(config);
            }
        } catch (Exception exception) {
            log.error("读取数据库安全配置失败，继续使用 Redis 或启动配置", exception);
            config = loadRedis(config);
        }
        syncRedis(config, false);
        apply(config);
    }

    public RuntimeSecurityConfig getConfig() {
        RuntimeSecurityConfig config = runtimeConfig;
        if (config == null) {
            synchronized (this) {
                if (runtimeConfig == null) {
                    apply(fromUserSetting());
                }
                config = runtimeConfig;
            }
        }
        return copy(config);
    }

    @Transactional(rollbackFor = Exception.class)
    public synchronized RuntimeSecurityConfig update(RuntimeSecurityConfig source) {
        RuntimeSecurityConfig config = normalize(source);
        persistDatabase(config);
        syncRedis(config, true);
        apply(config);
        return copy(config);
    }

    public boolean isAuthenticationExcluded(String requestUri) {
        if (!StringUtils.hasText(requestUri)) {
            return false;
        }
        for (String pattern : getConfig().getInterfaceAuthenticationExcludes()) {
            if (pathMatcher.match(pattern, requestUri)) {
                return true;
            }
        }
        return false;
    }

    private RuntimeSecurityConfig normalize(RuntimeSecurityConfig source) {
        if (source == null) {
            throw new IllegalArgumentException("安全配置不能为空");
        }
        if (source.getInterfaceAuthentication() == null
                || source.getDocEnabled() == null
                || source.getContentTypeOptionsEnabled() == null) {
            throw new IllegalArgumentException("安全配置开关不能为空");
        }
        Long loginTimeout = source.getLoginTimeoutMinutes();
        if (loginTimeout == null || loginTimeout < 0 || loginTimeout > 999) {
            throw new IllegalArgumentException("登录超时时间必须在 0 到 999 分钟之间");
        }

        RuntimeSecurityConfig result = new RuntimeSecurityConfig();
        result.setInterfaceAuthentication(source.getInterfaceAuthentication());
        result.setLoginTimeoutMinutes(loginTimeout);
        result.setDocEnabled(source.getDocEnabled());
        result.setContentTypeOptionsEnabled(source.getContentTypeOptionsEnabled());
        result.setInterfaceAuthenticationExcludes(normalizeExcludes(source.getInterfaceAuthenticationExcludes()));
        result.setAllowedOrigins(normalizeOrigins(source.getAllowedOrigins()));
        return result;
    }

    private List<String> normalizeExcludes(List<String> values) {
        List<String> result = normalizeList(values, 100, 256, "鉴权例外接口");
        for (String value : result) {
            if (!value.startsWith("/")) {
                throw new IllegalArgumentException("鉴权例外接口必须以 / 开头");
            }
            if ("/**".equals(value) || "/*".equals(value)) {
                throw new IllegalArgumentException("鉴权例外接口不能匹配全部请求");
            }
        }
        return result;
    }

    private List<String> normalizeOrigins(List<String> values) {
        List<String> result = normalizeList(values, 50, 256, "跨域来源");
        List<String> normalizedOrigins = new ArrayList<>();
        for (String value : result) {
            URI uri;
            try {
                uri = URI.create(value);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("跨域来源格式错误: " + value);
            }
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            String path = uri.getRawPath();
            if (!("http".equals(scheme) || "https".equals(scheme))
                    || !StringUtils.hasText(uri.getHost())
                    || StringUtils.hasText(uri.getUserInfo())
                    || (StringUtils.hasText(path) && !"/".equals(path))
                    || StringUtils.hasText(uri.getRawQuery())
                    || StringUtils.hasText(uri.getRawFragment())) {
                throw new IllegalArgumentException("跨域来源必须是 http(s)://主机[:端口]: " + value);
            }
            normalizedOrigins.add(scheme + "://" + uri.getRawAuthority());
        }
        return normalizedOrigins.stream().distinct().collect(Collectors.toList());
    }

    private List<String> normalizeList(List<String> values, int maxSize, int maxLength, String name) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        if (result.size() > maxSize) {
            throw new IllegalArgumentException(name + "最多允许 " + maxSize + " 项");
        }
        if (result.stream().anyMatch(value -> value.length() > maxLength)) {
            throw new IllegalArgumentException(name + "单项长度不能超过 " + maxLength + " 个字符");
        }
        return result;
    }

    private RuntimeSecurityConfig fromUserSetting() {
        RuntimeSecurityConfig config = new RuntimeSecurityConfig();
        config.setInterfaceAuthentication(Boolean.TRUE.equals(userSetting.getInterfaceAuthentication()));
        config.setInterfaceAuthenticationExcludes(copyList(userSetting.getInterfaceAuthenticationExcludes()));
        config.setLoginTimeoutMinutes(userSetting.getLoginTimeout());
        config.setAllowedOrigins(copyList(userSetting.getAllowedOrigins()));
        config.setDocEnabled(Boolean.TRUE.equals(userSetting.getDocEnable()));
        config.setContentTypeOptionsEnabled(Boolean.TRUE.equals(userSetting.getContentTypeOptionsEnabled()));
        return normalize(config);
    }

    private void apply(RuntimeSecurityConfig config) {
        RuntimeSecurityConfig snapshot = copy(config);
        userSetting.setInterfaceAuthentication(snapshot.getInterfaceAuthentication());
        userSetting.setInterfaceAuthenticationExcludes(copyList(snapshot.getInterfaceAuthenticationExcludes()));
        userSetting.setLoginTimeout(snapshot.getLoginTimeoutMinutes());
        userSetting.setAllowedOrigins(copyList(snapshot.getAllowedOrigins()));
        userSetting.setDocEnable(snapshot.getDocEnabled());
        userSetting.setContentTypeOptionsEnabled(snapshot.getContentTypeOptionsEnabled());
        runtimeConfig = snapshot;
    }

    private RuntimeSecurityConfig copy(RuntimeSecurityConfig source) {
        RuntimeSecurityConfig result = new RuntimeSecurityConfig();
        result.setInterfaceAuthentication(source.getInterfaceAuthentication());
        result.setInterfaceAuthenticationExcludes(copyList(source.getInterfaceAuthenticationExcludes()));
        result.setLoginTimeoutMinutes(source.getLoginTimeoutMinutes());
        result.setAllowedOrigins(copyList(source.getAllowedOrigins()));
        result.setDocEnabled(source.getDocEnabled());
        result.setContentTypeOptionsEnabled(source.getContentTypeOptionsEnabled());
        return result;
    }

    private List<String> copyList(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    private RuntimeSecurityConfig parse(String value, RuntimeSecurityConfig defaults) {
        JSONObject object = JSON.parseObject(value);
        RuntimeSecurityConfig source = JSON.parseObject(value, RuntimeSecurityConfig.class);
        RuntimeSecurityConfig result = copy(defaults);
        if (object.containsKey("interfaceAuthentication") && source.getInterfaceAuthentication() != null) {
            result.setInterfaceAuthentication(source.getInterfaceAuthentication());
        }
        if (object.containsKey("interfaceAuthenticationExcludes")
                && source.getInterfaceAuthenticationExcludes() != null) {
            result.setInterfaceAuthenticationExcludes(source.getInterfaceAuthenticationExcludes());
        }
        if (object.containsKey("loginTimeoutMinutes") && source.getLoginTimeoutMinutes() != null) {
            result.setLoginTimeoutMinutes(source.getLoginTimeoutMinutes());
        } else if (object.getLong("jwtExpirationMinutes") != null) {
            result.setLoginTimeoutMinutes(object.getLong("jwtExpirationMinutes"));
        }
        if (object.containsKey("allowedOrigins") && source.getAllowedOrigins() != null) {
            result.setAllowedOrigins(source.getAllowedOrigins());
        }
        if (object.containsKey("docEnabled") && source.getDocEnabled() != null) {
            result.setDocEnabled(source.getDocEnabled());
        }
        if (object.containsKey("contentTypeOptionsEnabled") && source.getContentTypeOptionsEnabled() != null) {
            result.setContentTypeOptionsEnabled(source.getContentTypeOptionsEnabled());
        }
        return normalize(result);
    }

    private RuntimeSecurityConfig loadRedis(RuntimeSecurityConfig fallback) {
        try {
            String persisted = redisTemplate.opsForValue().get(redisKey());
            return StringUtils.hasText(persisted) ? parse(persisted, fallback) : fallback;
        } catch (Exception exception) {
            log.warn("读取 Redis 安全配置失败，继续使用当前配置: {}", exception.getMessage());
            return fallback;
        }
    }

    private void persistDatabase(RuntimeSecurityConfig config) {
        String json = JSON.toJSONString(config);
        String updateTime = Instant.now().toString();
        if (securityConfigMapper.update(serverId(), json, updateTime) == 0) {
            securityConfigMapper.insert(serverId(), json, updateTime);
        }
    }

    private void syncRedis(RuntimeSecurityConfig config, boolean required) {
        try {
            redisTemplate.opsForValue().set(redisKey(), JSON.toJSONString(config));
        } catch (RuntimeException exception) {
            if (required) {
                throw exception;
            }
            log.warn("同步安全配置到 Redis 失败: {}", exception.getMessage());
        }
    }

    private String redisKey() {
        return REDIS_KEY_PREFIX + serverId();
    }

    private String serverId() {
        return userSetting.getServerId();
    }
}
