package com.genersoft.iot.vmp.service.bean;

import java.util.List;

/**
 * 收到redis通知修改推流通道状态
 * @author lin
 */
public class PushStreamStatusChangeFromRedisDto {

    private boolean setAllOffline;

    private List<StreamPushItemFromRedis> onlineStreams;

    private List<StreamPushItemFromRedis> offlineStreams;


    public boolean isSetAllOffline() {
        return setAllOffline;
    }

    public void setSetAllOffline(boolean setAllOffline) {
        this.setAllOffline = setAllOffline;
    }

    public List<StreamPushItemFromRedis> getOnlineStreams() {
        return onlineStreams;
    }

    public void setOnlineStreams(List<StreamPushItemFromRedis> onlineStreams) {
        this.onlineStreams = onlineStreams;
    }

    public List<StreamPushItemFromRedis> getOfflineStreams() {
        return offlineStreams;
    }

    public void setOfflineStreams(List<StreamPushItemFromRedis> offlineStreams) {
        this.offlineStreams = offlineStreams;
    }
}
