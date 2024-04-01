package com.genersoft.iot.vmp.media.abl.bean.hook;

public class ABLHookParam {
    private String mediaServerId;

    /**
     * 应用名
     */
    private String app;

    /**
     * 流id
     */
    private String stream;

    /**
     * 媒体流来源编号，可以根据这个key进行关闭流媒体 可以调用delMediaStream或close_streams 函数进行关闭
     */
    private String key;

    /**
     * 媒体流来源网络编号，可参考附表
     */
    private String networkType;

    public String getMediaServerId() {
        return mediaServerId;
    }

    public void setMediaServerId(String mediaServerId) {
        this.mediaServerId = mediaServerId;
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

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getNetworkType() {
        return networkType;
    }

    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }
}
