package com.genersoft.iot.vmp.gat1400.listener;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 作者：Administrator
 * 创建时间：2025/4/27
 * 邮箱：yongwangyi999@163.com
 */
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface test {
    int number() default 0;
    String name() default "";
}
