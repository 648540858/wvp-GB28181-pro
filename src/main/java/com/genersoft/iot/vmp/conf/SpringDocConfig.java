package com.genersoft.iot.vmp.conf;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.core.annotation.Order;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author lin
 */
@Configuration
@Order(1)
public class SpringDocConfig {

    @Value("${doc.enabled: true}")
    private boolean enable;

    @Bean
    public OpenAPI springShopOpenApi() {
        Contact contact = new Contact();
        contact.setName("pan");
        contact.setEmail("648540858@qq.com");
        return new OpenAPI()
                .info(new Info().title("WVP-PRO 接口文档")
                        .contact(contact)
                        .description("开箱即用的28181协议视频平台")
                        .version("v2.0")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")));
    }

    /**
     * 添加分组
     * @return
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("1. 全部")
                .packagesToScan("com.genersoft.iot.vmp.vmanager")
                .build();
    }

    @Bean
    public GroupedOpenApi publicApi2() {
        return GroupedOpenApi.builder()
                .group("2. 国标28181")
                .packagesToScan("com.genersoft.iot.vmp.vmanager.gb28181")
                .build();
    }

    @Bean
    public GroupedOpenApi publicApi3() {
        return GroupedOpenApi.builder()
                .group("3. 拉流转发")
                .packagesToScan("com.genersoft.iot.vmp.vmanager.streamProxy")
                .build();
    }

    @Bean
    public GroupedOpenApi publicApi4() {
        return GroupedOpenApi.builder()
                .group("4. 推流管理")
                .packagesToScan("com.genersoft.iot.vmp.vmanager.streamPush")
                .build();
    }

    @Bean
    public GroupedOpenApi publicApi5() {
        return GroupedOpenApi.builder()
                .group("4. 服务管理")
                .packagesToScan("com.genersoft.iot.vmp.vmanager.server")
                .build();
    }

    @Bean
    public GroupedOpenApi publicApi6() {
        return GroupedOpenApi.builder()
                .group("5. 用户管理")
                .packagesToScan("com.genersoft.iot.vmp.vmanager.user")
                .build();
    }
}
