package com.genersoft.iot.vmp.media.abl.bean.hook;

public class OnServerStaredABLHookParam {
    private String localipAddress;
    private String mediaServerId;
    private String datetime;


    public String getLocalipAddress() {
        return localipAddress;
    }

    public void setLocalipAddress(String localipAddress) {
        this.localipAddress = localipAddress;
    }

    public String getMediaServerId() {
        return mediaServerId;
    }

    public void setMediaServerId(String mediaServerId) {
        this.mediaServerId = mediaServerId;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }
}
