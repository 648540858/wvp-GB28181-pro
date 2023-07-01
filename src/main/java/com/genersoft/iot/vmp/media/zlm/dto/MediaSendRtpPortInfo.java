package com.genersoft.iot.vmp.media.zlm.dto;

public class MediaSendRtpPortInfo {

    private int start;
    private int end;
    private String mediaServerId;

    private int current;


    public MediaSendRtpPortInfo(int start, int end, String mediaServerId) {
        this.start = start;
        this.current = start;
        this.end = end;
        this.mediaServerId = mediaServerId;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public String getMediaServerId() {
        return mediaServerId;
    }

    public void setMediaServerId(String mediaServerId) {
        this.mediaServerId = mediaServerId;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }
}
