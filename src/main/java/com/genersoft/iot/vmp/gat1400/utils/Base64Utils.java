package com.genersoft.iot.vmp.gat1400.utils;

import com.genersoft.iot.vmp.gat1400.framework.exception.Base64ErrorException;

import java.util.Base64;


public class Base64Utils {

    public static byte[] decodeStringToBytes(String data) {
        try {
            Base64.Decoder decoder = Base64.getDecoder();
            return decoder.decode(data);
        } catch (Exception e) {
            throw new Base64ErrorException();
        }
    }
}
