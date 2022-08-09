package com.genersoft.iot.vmp;

import java.util.logging.LogManager;

import com.genersoft.iot.vmp.conf.druid.EnableDruidSupport;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import springfox.documentation.oas.annotations.EnableOpenApi;

/**
 * 启动类
 */
@ServletComponentScan("com.genersoft.iot.vmp.conf")
@SpringBootApplication
@EnableScheduling
@EnableOpenApi
@EnableDruidSupport
public class VManageBootstrap extends LogManager {
	private static String[] args;
	private static ConfigurableApplicationContext context;
	public static void main(String[] args) {
		VManageBootstrap.args = args;
		VManageBootstrap.context = SpringApplication.run(VManageBootstrap.class, args);
	}
	// 项目重启
	public static void restart() {
		context.close();
		VManageBootstrap.context = SpringApplication.run(VManageBootstrap.class, args);
	}
	

}
