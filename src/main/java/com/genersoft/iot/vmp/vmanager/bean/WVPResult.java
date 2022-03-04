package com.genersoft.iot.vmp.vmanager.bean;


public class WVPResult<T> {

    public WVPResult() {
    }

    public WVPResult(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    private int code;
    private String msg;
    private T data;

    private static final Integer SUCCESS = 200;
    private static final Integer FAILED = 400;

    public static <T> WVPResult<T> Data(T t, String msg) {
        return new WVPResult<>(SUCCESS, msg, t);
    }

    public static <T> WVPResult<T> Data(T t) {
        return Data(t, "成功");
    }

    public static <T> WVPResult<T> fail(int code, String msg) {
        return new WVPResult<>(code, msg, null);
    }

    public static <T> WVPResult<T> fail(String msg) {
        return fail(FAILED, msg);
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

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
