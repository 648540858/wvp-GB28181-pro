package com.genersoft.iot.vmp.streamProxy.service;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxy;

public interface IStreamProxyPlayService {

    void start(int id, ErrorCallback<StreamInfo> callback);

    StreamInfo startProxy(StreamProxy streamProxy);

    void stop(int id);

    void stopProxy(StreamProxy streamProxy);
}
