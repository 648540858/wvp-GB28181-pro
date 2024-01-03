package com.genersoft.iot.vmp.media.zlm.dto;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONField;

import java.time.Instant;

/**
 * hook订阅-录像完成
 * @author lin
 */
public class HookSubscribeForRecordMp4 implements IHookSubscribe{

    private HookType hookType = HookType.on_record_mp4;

    private JSONObject content;

    @JSONField(format="yyyy-MM-dd HH:mm:ss")
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
