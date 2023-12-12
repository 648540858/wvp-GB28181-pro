package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;

/**
 * 资源播放回调
 */
public interface IResourcePlayCallback {

    /**
     * 资源播放回调
     */
    void call(CommonGbChannel commonGbChannel, MediaServerItem mediaServerItem, int code, String message, StreamInfo streamInfo);
}
