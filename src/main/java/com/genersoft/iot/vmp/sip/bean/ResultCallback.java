package com.genersoft.iot.vmp.sip.bean;

public interface ResultCallback<T> {

    void run(int code, String msg, T data);
}
