package com.genersoft.iot.vmp.media.zlm.dto;

import com.alibaba.fastjson.JSONObject;

import java.time.Instant;

/**
 * zlm hook事件的参数
 * @author lin
 */
public interface IHookSubscribe {

    /**
     * 获取hook类型
     * @return hook类型
     */
    HookType getHookType();

    /**
     * 获取hook的具体内容
     * @return hook的具体内容
     */
    JSONObject getContent();

    /**
     * 设置过期时间
     * @param instant 过期时间
     */
    void setExpires(Instant instant);

    /**
     * 获取过期时间
     * @return 过期时间
     */
    Instant getExpires();
}
