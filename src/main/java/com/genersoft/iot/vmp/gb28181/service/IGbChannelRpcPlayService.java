package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;

public interface IGbChannelRpcPlayService {
    void play(String serverId, Integer channelId, ErrorCallback<StreamInfo> callback);
}
