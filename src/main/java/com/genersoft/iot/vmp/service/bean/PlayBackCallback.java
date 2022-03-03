package com.genersoft.iot.vmp.service.bean;

import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;

public interface PlayBackCallback {

    void call(RequestMessage msg);

}
