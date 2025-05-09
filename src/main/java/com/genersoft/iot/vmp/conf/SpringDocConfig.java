package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.conf.security.JwtUtils;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(value = "user-settings.doc-enable", havingValue = "true", matchIfMissing = true)
public class SpringDocConfig {

    @Value("${doc.enabled: true}")
    private boolean enable;

    @Bean
    public OpenAPI springShopOpenApi() {
        Contact contact = new Contact();
        contact.setName("pan");
        contact.setEmail("648540858@qq.com");

        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(JwtUtils.HEADER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .bearerFormat("JWT")))
                .info(new Info().title("WVP-PRO 接口文档")
                        .contact(contact)
                        .description("开箱即用的28181协议视频平台。 <br/>" +
                                "1. 打开http://127.0.0.1:18080/doc.html#/1.%20全部/用户管理/login_1" +
                                " 登录成功后返回AccessToken。 <br/>" +
                                "2. 填写到AccessToken到参数值 http://127.0.0.1:18080/doc.html#/Authorize/1.%20全部  <br/>" +
                                "后续接口就可以直接测试了")
                        .version("v3.1.0")
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
                .packagesToScan("com.genersoft.iot.vmp")
                .build();
    }

    @Bean
    public GroupedOpenApi publicApi2() {
        return GroupedOpenApi.builder()
                .group("2. 国标28181")
                .packagesToScan("com.genersoft.iot.vmp.gb28181")
                .build();
    }

    @Bean
    public GroupedOpenApi publicApi3() {
        return GroupedOpenApi.builder()
                .group("3. 拉流转发")
                .packagesToScan("com.genersoft.iot.vmp.streamProxy")
                .build();
    }

    @Bean
    public GroupedOpenApi publicApi4() {
        return GroupedOpenApi.builder()
                .group("4. 推流管理")
                .packagesToScan("com.genersoft.iot.vmp.streamPush")
                .build();
    }

    @Bean
    public GroupedOpenApi publicApi5() {
        return GroupedOpenApi.builder()
                .group("4. 服务管理")
                .packagesToScan("com.genersoft.iot.vmp.server")
                .build();
    }

    @Bean
    public GroupedOpenApi publicApi6() {
        return GroupedOpenApi.builder()
                .group("5. 用户管理")
                .packagesToScan("com.genersoft.iot.vmp.user")
                .build();
    }
}
