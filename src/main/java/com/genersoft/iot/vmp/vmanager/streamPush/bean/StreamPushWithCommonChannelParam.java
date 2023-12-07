package com.genersoft.iot.vmp.vmanager.streamPush.bean;

import com.genersoft.iot.vmp.common.CommonGbChannel;

/**
 * 推流信息加入资源库参数
 */
public class StreamPushWithCommonChannelParam extends CommonGbChannel {

    private Integer pushId;

    private String app;

    private String stream;

    public Integer getPushId() {
        return pushId;
    }

    public void setPushId(Integer pushId) {
        this.pushId = pushId;
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
