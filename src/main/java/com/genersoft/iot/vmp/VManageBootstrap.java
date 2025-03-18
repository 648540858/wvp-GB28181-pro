package com.genersoft.iot.vmp;

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
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import java.util.Collections;

/**
 * 启动类
 */
@ServletComponentScan("com.genersoft.iot.vmp.conf")
@SpringBootApplication
@EnableScheduling
@EnableCaching
@Slf4j
public class VManageBootstrap extends SpringBootServletInitializer {

	private static String[] args;
	private static ConfigurableApplicationContext context;
	public static void main(String[] args) {
		VManageBootstrap.args = args;
		VManageBootstrap.context = SpringApplication.run(VManageBootstrap.class, args);
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
		VManageBootstrap.context = SpringApplication.run(VManageBootstrap.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(VManageBootstrap.class);
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
