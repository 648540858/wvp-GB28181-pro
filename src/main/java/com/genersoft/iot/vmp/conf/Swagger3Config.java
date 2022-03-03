package com.genersoft.iot.vmp.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@Configuration
public class Swagger3Config {

    @Value("${swagger-ui.enabled: true}")
    private boolean enable;

    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .groupName("1. 全部")
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.genersoft.iot.vmp.vmanager"))
                .paths(PathSelectors.any())
                .build()
                .pathMapping("/")
                .enable(enable);
    }
    @Bean
    public Docket createRestGBApi() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .groupName("2. 国标28181")
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.genersoft.iot.vmp.vmanager.gb28181"))
                .paths(PathSelectors.any())
                .build()
                .pathMapping("/")
                .enable(enable);
    }

    @Bean
    public Docket createRestONVIFApi() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .groupName("3. ONVIF")
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.genersoft.iot.vmp.vmanager.onvif"))
                .paths(PathSelectors.any())
                .build()
                .pathMapping("/")
                .enable(enable);
    }

    @Bean
    public Docket createRestStreamProxyApi() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .groupName("4. 拉流转发")
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.genersoft.iot.vmp.vmanager.streamProxy"))
                .paths(PathSelectors.any())
                .build()
                .pathMapping("/")
                .enable(enable);
    }
    @Bean
    public Docket createRestStreamPushApi() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .groupName("5. 推流管理")
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.genersoft.iot.vmp.vmanager.streamPush"))
                .paths(PathSelectors.any())
                .build()
                .pathMapping("/")
                .enable(enable);
    }


    @Bean
    public Docket createServerApi() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .groupName("6. 服务管理")
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.genersoft.iot.vmp.vmanager.server"))
                .paths(PathSelectors.any())
                .build()
                .pathMapping("/")
                .enable(enable);
    }
    @Bean
    public Docket createUserApi() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .groupName("7. 用户管理")
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.genersoft.iot.vmp.vmanager.user"))
                .paths(PathSelectors.any())
                .build()
                .pathMapping("/")
                .enable(enable);
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("WVP-PRO 接口文档")
                .description("更多请咨询服务开发者(https://github.com/648540858/wvp-GB28181-pro)。")
                .contact(new Contact("648540858", "648540858", "648540858@qq.com"))
                .version("2.0")
                .build();
    }
}
