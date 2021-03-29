package com.genersoft.iot.vmp.vmanager.service;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.media.zlm.ZLMHttpHookSubscribe;
import com.genersoft.iot.vmp.vmanager.play.bean.PlayResult;

/**
 * 点播处理
 */
public interface IPlayService {
    RequestMessage createCallbackPlayMsg();

    void onPublishHandlerForPlayBack(JSONObject resonse, String deviceId, String channelId, RequestMessage msg);

    void onPublishHandlerForPlay(JSONObject resonse, String deviceId, String channelId, RequestMessage msg);

    PlayResult play(String deviceId, String channelId, ZLMHttpHookSubscribe.Event event, SipSubscribe.Event errorEvent);
}
