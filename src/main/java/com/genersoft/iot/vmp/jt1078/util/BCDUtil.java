package com.genersoft.iot.vmp.jt1078.util;

public class BCDUtil {

    public static String transform(byte[] bytes) {
        if (bytes.length == 0) {
            return null;
        }
        // BCD[6] 解析时间
        StringBuffer temp = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            temp.append((byte) ((bytes[i] & 0xf0) >>> 4));
            temp.append((byte) (bytes[i] & 0x0f));
        }
        return temp.toString();
    }
}
