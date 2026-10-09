package com.genersoft.iot.vmp.conf;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.DefaultPropertiesPropertySource;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 数据源默认值：
 * <ul>
 *   <li>配置文件中未配置 spring.datasource 时，使用内置 H2 文件数据库，默认值与 application-standalone.yml 保持一致；</li>
 *   <li>已配置 spring.datasource.url 时完全不介入，不关心配置是否完整。</li>
 * </ul>
 * 监听器在配置文件加载完成后执行，使用与 setDefaultProperties 相同的最低优先级属性源。
 */
@Slf4j
public final class DataSourceDefaults {

    /**
     * 与 application-standalone.yml 一致的默认数据源配置
     */
    public static final Map<String, Object> PROPERTIES;

    static {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("spring.datasource.driver-class-name", "org.h2.Driver");
        defaults.put("spring.datasource.url", "jdbc:h2:file:./data/wvp;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE");
        defaults.put("spring.datasource.username", "sa");
        defaults.put("spring.datasource.password", "");
        // 文件型 H2 不属于 Spring Boot 的 embedded 数据库，需显式指定建表/初始化脚本
        defaults.put("spring.sql.init.mode", "always");
        defaults.put("spring.sql.init.schema-locations", "classpath:db/h2-schema.sql");
        defaults.put("spring.sql.init.data-locations", "classpath:db/h2-data.sql");
        defaults.put("pagehelper.helper-dialect", "h2");
        PROPERTIES = Collections.unmodifiableMap(defaults);
    }

    /**
     * 配置文件加载完成后执行：未配置 spring.datasource.url 时注入默认 H2 数据源
     */
    public static final ApplicationListener<ApplicationEnvironmentPreparedEvent> ENVIRONMENT_LISTENER = event -> {
        ConfigurableEnvironment environment = event.getEnvironment();
        if (environment.containsProperty("spring.datasource.url")) {
            return;
        }
        DefaultPropertiesPropertySource.addOrMerge(PROPERTIES, environment.getPropertySources());
        log.info("已启用默认H2数据库。");
    };

    private DataSourceDefaults() {
    }
}
