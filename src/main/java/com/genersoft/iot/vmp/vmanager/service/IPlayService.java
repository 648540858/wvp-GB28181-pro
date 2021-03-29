package com.genersoft.iot.vmp.vmanager.service;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.media.zlm.ZLMHttpHookSubscribe;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * 点播处理
 */
public interface IPlayService {
    RequestMessage createCallbackPlayMsg();

    void onPublishHandlerForPlayBack(JSONObject resonse, String deviceId, String channelId, RequestMessage msg);

    void onPublishHandlerForPlay(JSONObject resonse, String deviceId, String channelId, RequestMessage msg);

    DeferredResult<ResponseEntity<String>> play(String deviceId, String channelId, ZLMHttpHookSubscribe.Event event, SipSubscribe.Event errorEvent);
}
