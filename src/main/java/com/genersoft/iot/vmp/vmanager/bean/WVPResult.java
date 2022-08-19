package com.genersoft.iot.vmp.vmanager.bean;


import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "统一返回结果")
public class WVPResult<T> {

    public WVPResult() {
    }

    public WVPResult(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }


    @Schema(description = "错误码，0为成功")
    private int code;
    @Schema(description = "描述，错误时描述错误原因")
    private String msg;
    @Schema(description = "数据")
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
