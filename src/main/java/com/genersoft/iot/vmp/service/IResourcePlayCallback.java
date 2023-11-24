package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.common.StreamInfo;

/**
 * 资源播放回调
 */
public interface IResourcePlayCallback {

    /**
     * 资源播放回调
     * @param commonGbChannel 通道
     * @param code
     * @param message
     * @param streamInfo
     */
    void call(CommonGbChannel commonGbChannel, int code, String message, StreamInfo streamInfo);
}
