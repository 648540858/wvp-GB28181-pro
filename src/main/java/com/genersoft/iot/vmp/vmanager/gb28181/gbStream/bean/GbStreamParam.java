package com.genersoft.iot.vmp.vmanager.gb28181.gbStream.bean;

import com.genersoft.iot.vmp.gb28181.bean.GbStream;

import java.util.List;

public class GbStreamParam {

    private String platformId;

    private List<GbStream> gbStreams;

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public List<GbStream> getGbStreams() {
        return gbStreams;
    }

    public void setGbStreams(List<GbStream> gbStreams) {
        this.gbStreams = gbStreams;
    }
}
