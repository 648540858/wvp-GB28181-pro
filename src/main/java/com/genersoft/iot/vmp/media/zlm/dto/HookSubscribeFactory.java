package com.genersoft.iot.vmp.media.zlm.dto;


import com.alibaba.fastjson.JSONObject;

/**
 * hook 订阅工厂
 * @author lin
 */
public class HookSubscribeFactory {

    public static HookSubscribeForStreamChange on_stream_changed(String app, String stream, boolean regist, String scheam, String mediaServerId) {
        HookSubscribeForStreamChange hookSubscribe = new HookSubscribeForStreamChange();
        JSONObject subscribeKey = new com.alibaba.fastjson.JSONObject();
        subscribeKey.put("app", app);
        subscribeKey.put("stream", stream);
        subscribeKey.put("regist", regist);
        if (scheam != null) {
            subscribeKey.put("schema", scheam);
        }
        subscribeKey.put("mediaServerId", mediaServerId);
        hookSubscribe.setContent(subscribeKey);

        return hookSubscribe;
    }

    public static HookSubscribeForServerStarted on_server_started() {
        HookSubscribeForServerStarted hookSubscribe = new HookSubscribeForServerStarted();
        hookSubscribe.setContent(new JSONObject());

        return hookSubscribe;
    }
}
