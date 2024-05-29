package com.genersoft.iot.vmp.jt1078.util;

public class SSRCUtil {

    public static String randomSSRC(){
        return String.format("%010d", Math.round(Math.random()*10000000000L));
    }
}
