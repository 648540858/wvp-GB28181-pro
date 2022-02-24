package com.genersoft.iot.vmp.conf.druid;

import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * druid监控支持注解
 *
 * @author
 * {@link DruidConfiguration} druid监控页面安全配置支持
 * {@link ServletComponentScan} druid监控页面需要扫描servlet
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({
        DruidConfiguration.class,
})
@ServletComponentScan
public @interface EnableDruidSupport {
}
