package com.genersoft.iot.vmp.service.redisMsg.dto;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface RedisRpcController {
    /**
     * 请求路径
     */
    String value() default "";
}
