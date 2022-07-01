package com.genersoft.iot.vmp.vmanager.bean;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItemLite;

/**
 * @author lin
 */
public class AudioBroadcastResult {
    /**
     * 推流的各个方式流地址
     */
    private StreamInfo streamInfo;

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


    public StreamInfo getStreamInfo() {
        return streamInfo;
    }

    public void setStreamInfo(StreamInfo streamInfo) {
        this.streamInfo = streamInfo;
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
