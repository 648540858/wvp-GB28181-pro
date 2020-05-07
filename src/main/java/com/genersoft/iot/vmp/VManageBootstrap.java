package com.genersoft.iot.vmp;

import java.util.logging.LogManager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//@EnableEurekaClient
//@EnableTransactionManagement
//@EnableFeignClients(basePackages = { "com.genersoft.iot.vmp", "org.integrain" })
//@ServletComponentScan("com.genersoft.iot.vmp")
@EnableAutoConfiguration
public class VManageBootstrap extends LogManager {
	public static void main(String[] args) {
		SpringApplication.run(VManageBootstrap.class, args);
	}

}
