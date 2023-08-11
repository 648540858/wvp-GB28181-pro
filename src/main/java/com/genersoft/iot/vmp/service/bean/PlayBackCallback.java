package com.genersoft.iot.vmp.service.bean;

public interface PlayBackCallback<T> {

    void call(PlayBackResult<T> msg);

}
