package com.genersoft.iot.vmp.gb28181.bean;

import gov.nist.javax.sdp.TimeDescriptionImpl;
import gov.nist.javax.sdp.fields.TimeField;

import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SessionDescription;
import java.time.Instant;
import java.util.Vector;

/**
 * 28181 的SDP解析器
 */
public class Gb28181Sdp  {
    private SessionDescription baseSdb;
    private String ssrc;

    private String mediaDescription;

    private Long startTime = null;
    private Long stopTime = null;

    private boolean tcp;
    private boolean tcpActive;

    private String sdpIp;

    private Integer sdpPort;

    private String username;
    private String addressStr;

    private Integer downloadSpeed;

    public static Gb28181Sdp getInstance(SessionDescription baseSdb, String ssrc, String mediaDescriptionStr) throws SdpException {
        Gb28181Sdp gb28181Sdp = new Gb28181Sdp();
        gb28181Sdp.setBaseSdb(baseSdb);
        gb28181Sdp.setSsrc(ssrc);
        gb28181Sdp.setMediaDescription(mediaDescriptionStr);

        if (baseSdb.getTimeDescriptions(false) != null && baseSdb.getTimeDescriptions(false).size() > 0) {
            TimeDescriptionImpl timeDescription = (TimeDescriptionImpl) (baseSdb.getTimeDescriptions(false).get(0));
            TimeField startTimeFiled = (TimeField) timeDescription.getTime();
            Long startTime = startTimeFiled.getStartTime();
            Long stopTime = startTimeFiled.getStopTime();
            gb28181Sdp.setStartTime(startTime);
            gb28181Sdp.setStopTime(stopTime);
        }
        //  获取支持的格式
        Vector mediaDescriptions = baseSdb.getMediaDescriptions(true);

        for (Object description : mediaDescriptions) {
            MediaDescription mediaDescription = (MediaDescription) description;
            if (mediaDescription.getAttribute("downloadspeed") != null) {
                gb28181Sdp.setDownloadSpeed(Integer.parseInt(mediaDescription.getAttribute("downloadspeed")));
            }
            Media media = mediaDescription.getMedia();
            Vector mediaFormats = media.getMediaFormats(false);
            // 查看是否支持PS 负载96
            if (mediaFormats.contains("96")) {
                gb28181Sdp.setSdpPort(media.getMediaPort());
                String protocol = media.getProtocol();

                // 区分TCP发流还是udp， 当前默认udp
                if ("TCP/RTP/AVP".equalsIgnoreCase(protocol)) {
                    String setup = mediaDescription.getAttribute("setup");
                    if (setup != null) {
                        gb28181Sdp.setTcp(true);
                        if ("active".equalsIgnoreCase(setup)) {
                            gb28181Sdp.setTcpActive(true);
                        } else if ("passive".equalsIgnoreCase(setup)) {
                            gb28181Sdp.setTcpActive(false);
                        }
                    }
                }
                break;
            }
        }

        gb28181Sdp.setUsername(baseSdb.getOrigin().getUsername());
        gb28181Sdp.setAddressStr(baseSdb.getConnection().getAddress());
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

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getStopTime() {
        return stopTime;
    }

    public void setStopTime(Long stopTime) {
        this.stopTime = stopTime;
    }

    public boolean isTcp() {
        return tcp;
    }

    public void setTcp(boolean tcp) {
        this.tcp = tcp;
    }

    public boolean isTcpActive() {
        return tcpActive;
    }

    public void setTcpActive(boolean tcpActive) {
        this.tcpActive = tcpActive;
    }

    public String getSdpIp() {
        return sdpIp;
    }

    public void setSdpIp(String sdpIp) {
        this.sdpIp = sdpIp;
    }

    public Integer getSdpPort() {
        return sdpPort;
    }

    public void setSdpPort(Integer sdpPort) {
        this.sdpPort = sdpPort;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAddressStr() {
        return addressStr;
    }

    public void setAddressStr(String addressStr) {
        this.addressStr = addressStr;
    }

    public Integer getDownloadSpeed() {
        return downloadSpeed;
    }

    public void setDownloadSpeed(Integer downloadSpeed) {
        this.downloadSpeed = downloadSpeed;
    }
}
