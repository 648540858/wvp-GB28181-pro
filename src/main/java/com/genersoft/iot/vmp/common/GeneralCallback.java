package com.genersoft.iot.vmp.common;

public interface GeneralCallback<T>{
    void run(int code, String msg, T data);
}
