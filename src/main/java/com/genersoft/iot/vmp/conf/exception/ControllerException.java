package com.genersoft.iot.vmp.conf.exception;

import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;

/**
 * 自定义异常，controller出现错误时直接抛出异常由全局异常捕获并返回结果
 */
public class ControllerException extends RuntimeException{

    private int code;
    private String msg;

    public ControllerException(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
    public ControllerException(ErrorCode errorCode) {
        this.code = errorCode.getCode();
        this.msg = errorCode.getMsg();
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
