package com.genersoft.iot.vmp;

import com.genersoft.iot.vmp.conf.DataSourceDefaults;
import com.genersoft.iot.vmp.jt1078.util.ClassUtil;
import com.genersoft.iot.vmp.utils.GitUtil;
import com.genersoft.iot.vmp.utils.SpringBeanFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;    
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 启动类
 */
@ServletComponentScan("com.genersoft.iot.vmp.conf")
@SpringBootApplication
@EnableScheduling
@EnableAsync(proxyTargetClass = true)
@EnableCaching
@Slf4j
public class VManageBootstrap extends SpringBootServletInitializer {

	private static String[] args;
	private static ConfigurableApplicationContext context;
	public static void main(String[] args) {
		VManageBootstrap.args = args;
		VManageBootstrap.context = createApplication().run(args);
		ClassUtil.context = VManageBootstrap.context;
		GitUtil gitUtil = SpringBeanFactory.getBean("gitUtil");
		if (gitUtil == null) {
			log.info("获取版本信息失败");
		}else {
			log.info("构建版本： {}", gitUtil.getBuildVersion());
			log.info("构建时间： {}", gitUtil.getBuildDate());
			log.info("GIT信息： 分支: {}, ID: {},  时间: {}", gitUtil.getBranch(), gitUtil.getCommitIdShort(), gitUtil.getCommitTime());
		}

	}
	// 项目重启
	public static void restart() {
		context.close();
		VManageBootstrap.context = createApplication().run(args);
	}

	private static SpringApplication createApplication() {
		SpringApplication application = new SpringApplication(VManageBootstrap.class);
		application.addListeners(DataSourceDefaults.ENVIRONMENT_LISTENER);
		application.addInitializers(VManageBootstrap::excludeRedisAutoConfigIfNoRedis);
		// 默认启用虚拟线程，profile/命令行仍可覆盖
		application.setDefaultProperties(Map.of("spring.threads.virtual.enabled", true));
		return application;
	}

	/**
	 * 未配置 spring.data.redis.host（内存模式）时排除 Redis 自动装配，
	 * 避免启动时创建无用的 RedisConnectionFactory 并尝试连接 Redis。
	 */
	private static void excludeRedisAutoConfigIfNoRedis(ConfigurableApplicationContext context) {
		String host = context.getEnvironment().getProperty("spring.data.redis.host");
		if (host != null && !host.isBlank()) {
			// redis 模式，保留自动装配
			return;
		}
		Map<String, Object> props = new HashMap<>();
		props.put("spring.autoconfigure.exclude",
				new String[]{"org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"});
		context.getEnvironment().getPropertySources()
				.addFirst(new MapPropertySource("conditionalRedisAutoConfigExclude", props));
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(VManageBootstrap.class)
				.listeners(DataSourceDefaults.ENVIRONMENT_LISTENER)
				.initializers(VManageBootstrap::excludeRedisAutoConfigIfNoRedis)
				.properties("spring.threads.virtual.enabled=true");
	}

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		super.onStartup(servletContext);

		servletContext.setSessionTrackingModes(
				Collections.singleton(SessionTrackingMode.COOKIE)
		);
		SessionCookieConfig sessionCookieConfig = servletContext.getSessionCookieConfig();
		sessionCookieConfig.setHttpOnly(true);
	}
}
