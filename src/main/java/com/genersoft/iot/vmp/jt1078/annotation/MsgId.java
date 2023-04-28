package com.genersoft.iot.vmp.jt1078.annotation;

import java.lang.annotation.*;

/**
 * @author QingtaiJiang
 * @date 2023/4/27 18:31
 * @email qingtaij@163.com
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MsgId {
    String id();
}
