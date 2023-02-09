package com.genersoft.iot.vmp.gb28181.utils;

import java.lang.annotation.*;

/**
 * @author gaofuwang
 * @version 1.0
 * @date 2022/6/28 14:58
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MessageElement {
    String value();

    String subVal() default "";
}
