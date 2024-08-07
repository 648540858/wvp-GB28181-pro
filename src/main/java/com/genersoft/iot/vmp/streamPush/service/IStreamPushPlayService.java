package com.genersoft.iot.vmp.streamPush.service;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.StreamInfo;

public interface IStreamPushPlayService {
    void start(Integer id, CommonCallback<StreamInfo> callback);
}
