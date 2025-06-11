package com.genersoft.iot.vmp.gat1400.framework.domain.core;

import org.springframework.http.HttpStatus;

import java.io.Serializable;

import lombok.Getter;

@Getter
public class BaseResponse implements Serializable {

    private static final long serialVersionUID = 1L;
    private boolean success;
    private int code;
    private String msg;
    private long timestamp;

    public BaseResponse() {
        success(this);
    }

    public static void success(BaseResponse response) {
        response.setCode(HttpStatus.OK.value())
                .setMessage("操作成功")
                .setSuccess(true)
                .setTimestamp(System.currentTimeMillis());
    }

    public static BaseResponse success() {
        return success(HttpStatus.OK.value());
    }

    public static BaseResponse success(int code) {
        return success(code, "操作成功");
    }

    public static BaseResponse success(int code, String message) {
        return new BaseResponse()
                .setCode(code)
                .setSuccess(true)
                .setMessage(message)
                .setTimestamp(System.currentTimeMillis());
    }

    public static BaseResponse error() {
        return error(500);
    }

    public static BaseResponse error(int code) {
        return error(code, "操作失败");
    }

    public static BaseResponse error(int code, String message) {
        return new BaseResponse()
                .setCode(code)
                .setSuccess(false)
                .setMessage(message)
                .setTimestamp(System.currentTimeMillis());
    }

    public static BaseResponse withBoolean(boolean success) {
        return withBoolean(success, "操作失败");
    }

    public static BaseResponse withBoolean(boolean success, String message) {
        return success ? success() : error(500, message);
    }

    public BaseResponse setCode(int code) {
        this.code = code;
        return this;
    }

    public BaseResponse setSuccess(boolean success) {
        this.success = success;
        return this;
    }

    public BaseResponse setMessage(String msg) {
        this.msg = msg;
        return this;
    }

    public BaseResponse setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public boolean getSuccess() {
        return success;
    }
}
