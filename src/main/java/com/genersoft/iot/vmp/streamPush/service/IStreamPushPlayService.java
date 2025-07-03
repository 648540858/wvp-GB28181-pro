package com.genersoft.iot.vmp.streamPush.service;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;

public interface IStreamPushPlayService {
    void start(Integer id, ErrorCallback<StreamInfo> callback, String platformDeviceId, String platformName );

    void stop(String app, String stream);

    void stop(Integer id);
}
