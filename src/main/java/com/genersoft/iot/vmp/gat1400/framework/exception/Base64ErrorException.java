package com.genersoft.iot.vmp.gat1400.framework.exception;

public class Base64ErrorException extends VIIDRuntimeException {

    public Base64ErrorException() {
        super("base64数据解析错误");
    }
}
