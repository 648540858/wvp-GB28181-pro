package com.genersoft.iot.vmp.jt1078.util;

/**
 * 32位整型的二进制读写
 */
public class Bin {

    private static final int[] bits = new int[32];

    static {
        bits[0] = 1;
        for (int i = 1; i < bits.length; i++) {
            bits[i] = bits[i - 1] << 1;
        }
    }

    /**
     * 读取n的第i位
     *
     * @param n int32
     * @param i 取值范围0-31
     */
    public static boolean get(int n, int i) {
        return (n & bits[i]) == bits[i];
    }

    /**
     * 不足位数从左边加0
     */
    public static String strHexPaddingLeft(String data, int length) {
        int dataLength = data.length();
        if (dataLength < length) {
            StringBuilder dataBuilder = new StringBuilder(data);
            for (int i = dataLength; i < length; i++) {
                dataBuilder.insert(0, "0");
            }
            data = dataBuilder.toString();
        }
        return data;
    }
}
