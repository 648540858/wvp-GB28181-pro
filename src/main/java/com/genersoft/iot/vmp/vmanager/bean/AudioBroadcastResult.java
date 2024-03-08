package com.genersoft.iot.vmp.vmanager.bean;

/**
 * @author lin
 */
public class AudioBroadcastResult {
    /**
     * 推流的各个方式流地址
     */
    private StreamContent streamInfo;

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


    public StreamContent getStreamInfo() {
        return streamInfo;
    }

    public void setStreamInfo(StreamContent streamInfo) {
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
