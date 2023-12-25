package com.genersoft.iot.vmp.media.zlm.dto;

import java.util.List;

public class StreamMediaInfo {

    private Boolean online;
    private Integer readerCount;
    private Integer totalReaderCount;
    private Integer bytesSpeed;
    private Integer aliveSecond;
    private List<StreamMediaTrack> tracks;

    public Boolean getOnline() {
        return online;
    }

    public void setOnline(Boolean online) {
        this.online = online;
    }

    public Integer getReaderCount() {
        return readerCount;
    }

    public void setReaderCount(Integer readerCount) {
        this.readerCount = readerCount;
    }

    public Integer getTotalReaderCount() {
        return totalReaderCount;
    }

    public void setTotalReaderCount(Integer totalReaderCount) {
        this.totalReaderCount = totalReaderCount;
    }

    public Integer getBytesSpeed() {
        return bytesSpeed;
    }

    public void setBytesSpeed(Integer bytesSpeed) {
        this.bytesSpeed = bytesSpeed;
    }

    public Integer getAliveSecond() {
        return aliveSecond;
    }

    public void setAliveSecond(Integer aliveSecond) {
        this.aliveSecond = aliveSecond;
    }

    public List<StreamMediaTrack> getTracks() {
        return tracks;
    }

    public void setTracks(List<StreamMediaTrack> tracks) {
        this.tracks = tracks;
    }
}
