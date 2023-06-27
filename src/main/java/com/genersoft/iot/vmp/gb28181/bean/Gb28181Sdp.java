package com.genersoft.iot.vmp.gb28181.bean;

import javax.sdp.SessionDescription;

/**
 * 28181 的SDP解析器
 */
public class Gb28181Sdp  {
    private SessionDescription baseSdb;
    private String ssrc;

    private String mediaDescription;

    public static Gb28181Sdp getInstance(SessionDescription baseSdb, String ssrc, String mediaDescription) {
        Gb28181Sdp gb28181Sdp = new Gb28181Sdp();
        gb28181Sdp.setBaseSdb(baseSdb);
        gb28181Sdp.setSsrc(ssrc);
        gb28181Sdp.setMediaDescription(mediaDescription);
        return gb28181Sdp;
    }


    public SessionDescription getBaseSdb() {
        return baseSdb;
    }

    public void setBaseSdb(SessionDescription baseSdb) {
        this.baseSdb = baseSdb;
    }

    public String getSsrc() {
        return ssrc;
    }

    public void setSsrc(String ssrc) {
        this.ssrc = ssrc;
    }

    public String getMediaDescription() {
        return mediaDescription;
    }

    public void setMediaDescription(String mediaDescription) {
        this.mediaDescription = mediaDescription;
    }
}
