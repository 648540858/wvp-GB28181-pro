package com.genersoft.iot.vmp.service.redisMsg.dto;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisRpcMapping {
    /**
     * 请求路径
     */
    String value() default "";
}
