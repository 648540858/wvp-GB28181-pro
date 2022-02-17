package com.genersoft.iot.vmp.vmanager.bean;

import com.genersoft.iot.vmp.gb28181.bean.GbStream;

import java.util.List;

public class BatchGBStreamParam {
    private List<GbStream> gbStreams;

    public List<GbStream> getGbStreams() {
        return gbStreams;
    }

    public void setGbStreams(List<GbStream> gbStreams) {
        this.gbStreams = gbStreams;
    }
}
