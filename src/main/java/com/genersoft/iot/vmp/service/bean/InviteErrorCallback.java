package com.genersoft.iot.vmp.service.bean;

public interface InviteErrorCallback<T> {

    void run(int code, String msg, T data);
}
