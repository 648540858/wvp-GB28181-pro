package com.genersoft.iot.vmp.service.bean;

import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;

public interface PlayBackCallback {

    void call(PlayBackResult<RequestMessage> msg);

}
