package com.genersoft.iot.vmp.jt1078.bean.common;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigAttribute {

    long id();

    String description();
}
