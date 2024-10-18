package com.genersoft.iot.vmp.streamProxy.service;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxy;

public interface IStreamProxyPlayService {

    StreamInfo start(int id);

    StreamInfo startProxy(StreamProxy streamProxy);

    void stop(int id);

    void stopProxy(StreamProxy streamProxy);
}
