package com.genersoft.iot.vmp.media.abl.bean;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConfigKeyId {
    String value();
}
