package com.genersoft.iot.vmp.service.redisMsg.dto;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisRpcController {
    /**
     * 请求路径
     */
    String value() default "";
}
