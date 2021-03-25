package com.genersoft.iot.vmp.vmanager.service;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;

/**
 * 点播处理
 */
public interface IPlayService {
    RequestMessage createCallbackPlayMsg();

    void onPublishHandlerForPlayBack(JSONObject resonse, String deviceId, String channelId, RequestMessage msg);


    void onPublishHandlerForPlay(JSONObject resonse, String deviceId, String channelId, RequestMessage msg);
}
