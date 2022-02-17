package com.genersoft.iot.vmp.vmanager.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WVPResult<T> {

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

}
