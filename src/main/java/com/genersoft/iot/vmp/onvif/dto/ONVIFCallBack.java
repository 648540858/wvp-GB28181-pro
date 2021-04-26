package com.genersoft.iot.vmp.onvif.dto;

public interface ONVIFCallBack<T> {
    void run(int errorCode, T t);
}
