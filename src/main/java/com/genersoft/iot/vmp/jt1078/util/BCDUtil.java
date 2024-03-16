package com.genersoft.iot.vmp.jt1078.util;

/**
 * BCD码转换
 */
public class BCDUtil {

    public static String transform(byte[] bytes) {
        if (bytes.length == 0) {
            return null;
        }
        // BCD
        StringBuilder stringBuffer = new StringBuilder(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            // 每次取出四位的值，一个byte是八位，第一取出高四位，第二次取出低四位，
            // 这里也可以先 & 0xf0再右移4位，0xf0二进制为11110000，与运算后，可以得到高4位是值，低四位清零的结果
            stringBuffer.append((byte) ((bytes[i]  >>> 4 & 0xf)));
            stringBuffer.append((byte) (bytes[i] & 0x0f));
        }
        return stringBuffer.toString();
    }
}
