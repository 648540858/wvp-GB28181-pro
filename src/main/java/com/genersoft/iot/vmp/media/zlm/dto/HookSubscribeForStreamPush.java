package com.genersoft.iot.vmp.media.zlm.dto;

import com.alibaba.fastjson.JSONObject;

import java.time.Instant;

/**
 * hook订阅-开始推流
 * @author lin
 */
public class HookSubscribeForStreamPush implements IHookSubscribe{

    private HookType hookType = HookType.on_publish;

    private JSONObject content;

    private Instant expires;

    @Override
    public HookType getHookType() {
        return hookType;
    }

    @Override
    public JSONObject getContent() {
        return content;
    }

    public void setContent(JSONObject content) {
        this.content = content;
    }

    @Override
    public Instant getExpires() {
        return expires;
    }

    @Override
    public void setExpires(Instant expires) {
        this.expires = expires;
    }
}
