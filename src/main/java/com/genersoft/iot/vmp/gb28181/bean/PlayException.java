package com.genersoft.iot.vmp.gb28181.bean;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayException extends RuntimeException{
    private int code;
    private String msg;

    public PlayException(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
