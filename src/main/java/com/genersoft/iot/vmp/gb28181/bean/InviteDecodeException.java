package com.genersoft.iot.vmp.gb28181.bean;

import lombok.Data;

@Data
public class InviteDecodeException extends RuntimeException{
    private int code;
    private String msg;

    public InviteDecodeException(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
