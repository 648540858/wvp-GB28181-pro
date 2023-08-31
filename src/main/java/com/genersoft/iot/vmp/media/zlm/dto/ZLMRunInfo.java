package com.genersoft.iot.vmp.media.zlm.dto;

/**
 * 记录zlm运行中一些参数
 */
public class ZLMRunInfo {

    /**
     * zlm当前流数量
     */
    private int mediaCount;

    /**
     * 在线状态
     */
    private boolean online;

    public int getMediaCount() {
        return mediaCount;
    }

    public void setMediaCount(int mediaCount) {
        this.mediaCount = mediaCount;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}
