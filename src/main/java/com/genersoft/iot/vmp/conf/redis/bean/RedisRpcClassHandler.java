package com.genersoft.iot.vmp.conf.redis.bean;

import lombok.Data;

import java.lang.reflect.Method;

@Data
public class RedisRpcClassHandler {

    private Class<?> objectClass;
    private Method method;

    public RedisRpcClassHandler(Class<?> objectClass, Method method) {
        this.objectClass = objectClass;
        this.method = method;
    }
}
