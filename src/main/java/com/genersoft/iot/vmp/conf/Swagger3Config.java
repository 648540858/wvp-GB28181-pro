package com.genersoft.iot.vmp.conf;

import io.swagger.annotations.ApiOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.builders.RequestParameterBuilder;
import springfox.documentation.schema.ScalarType;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class Swagger3Config {

    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .groupName("全部")
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.genersoft.iot.vmp.vmanager"))
                .paths(PathSelectors.any())
                .build()
                .pathMapping("/");
    }
    @Bean
    public Docket createRestGBApi() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .groupName("国标")
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.genersoft.iot.vmp.vmanager.gb28181"))
                .paths(PathSelectors.any())
                .build()
                .pathMapping("/");
    }
    @Bean
    public Docket createRestStreamProxyApi() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .groupName("拉流转发")
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.genersoft.iot.vmp.vmanager.streamProxy"))
                .paths(PathSelectors.any())
                .build()
                .pathMapping("/");
    }
    @Bean
    public Docket createRestStreamPushApi() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .groupName("推流管理")
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.genersoft.iot.vmp.vmanager.streamPush"))
                .paths(PathSelectors.any())
                .build()
                .pathMapping("/");
    }


    @Bean
    public Docket createServerApi() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .groupName("服务管理")
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.genersoft.iot.vmp.vmanager.server"))
                .paths(PathSelectors.any())
                .build()
                .pathMapping("/");
    }
    @Bean
    public Docket createUserApi() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .groupName("用户管理")
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.genersoft.iot.vmp.vmanager.user"))
                .paths(PathSelectors.any())
                .build()
                .pathMapping("/");
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("WVP-PRO 接口文档")
                .description("更多请咨询服务开发者(18010473990@@163.com)。")
                .contact(new Contact("panlinlin", "http://www.ruiyeclub.cn", "ruiyeclub@foxmail.com"))
                .version("2.0")
                .build();
    }
}
