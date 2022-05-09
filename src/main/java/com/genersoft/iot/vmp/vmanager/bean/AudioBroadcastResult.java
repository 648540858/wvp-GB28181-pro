package com.genersoft.iot.vmp.vmanager.bean;

import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItemLite;

/**
 * @author lin
 */
public class AudioBroadcastResult {
    /**
     * 推流的媒体节点信息
     */
    private MediaServerItemLite mediaServerItem;

    /**
     * 编码格式
     */
    private String codec;

    /**
     * 向zlm推流的应用名
     */
    private String app;

    /**
     * 向zlm推流的流ID
     */
    private String stream;


    public MediaServerItemLite getMediaServerItem() {
        return mediaServerItem;
    }

    public void setMediaServerItem(MediaServerItemLite mediaServerItem) {
        this.mediaServerItem = mediaServerItem;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }
}
