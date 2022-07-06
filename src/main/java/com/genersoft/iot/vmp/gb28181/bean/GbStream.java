package com.genersoft.iot.vmp.gb28181.bean;

/**
 * 直播流关联国标上级平台
 */
public class GbStream extends PlatformGbStream{

    private Integer gbStreamId;
    private String app;
    private String stream;
    private String gbId;
    private String name;
    private String mediaServerId;
    private double longitude;
    private double latitude;
    private String streamType;
    private boolean status;

    public String createTime;

    @Override
    public Integer getGbStreamId() {
        return gbStreamId;
    }

    @Override
    public void setGbStreamId(Integer gbStreamId) {
        this.gbStreamId = gbStreamId;
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

    public String getGbId() {
        return gbId;
    }

    public void setGbId(String gbId) {
        this.gbId = gbId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public String getStreamType() {
        return streamType;
    }

    public void setStreamType(String streamType) {
        this.streamType = streamType;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getMediaServerId() {
        return mediaServerId;
    }

    public void setMediaServerId(String mediaServerId) {
        this.mediaServerId = mediaServerId;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}
