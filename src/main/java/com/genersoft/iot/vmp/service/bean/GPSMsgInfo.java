package com.genersoft.iot.vmp.service.bean;

import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import com.genersoft.iot.vmp.utils.DateUtil;

public class GPSMsgInfo {

    /**
     *
     */
    private String id;

    /**
     * 经度 (必选)
     */
    private double lng;

    /**
     * 纬度 (必选)
     */
    private double lat;

    /**
     * 速度,单位:km/h (可选)
     */
    private double speed;

    /**
     * 产生通知时间, 时间格式： 2020-01-14T14:32:12
     */
    private String time;

    /**
     * 方向,取值为当前摄像头方向与正北方的顺时针夹角,取值范围0°~360°,单位:(°)(可选)
     */
    private String direction;

    /**
     * 海拔高度,单位:m(可选)
     */
    private String altitude;

    private boolean stored;

    public static GPSMsgInfo getInstance(MobilePosition mobilePosition) {
        GPSMsgInfo gpsMsgInfo = new GPSMsgInfo();
        gpsMsgInfo.setId(mobilePosition.getChannelId());
        gpsMsgInfo.setAltitude(mobilePosition.getAltitude() + "");
        gpsMsgInfo.setLng(mobilePosition.getLongitude());
        gpsMsgInfo.setLat(mobilePosition.getLatitude());
        gpsMsgInfo.setSpeed(mobilePosition.getSpeed());
        gpsMsgInfo.setDirection(mobilePosition.getDirection() + "");
        gpsMsgInfo.setTime(DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(mobilePosition.getTime()));
        return gpsMsgInfo;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getAltitude() {
        return altitude;
    }

    public void setAltitude(String altitude) {
        this.altitude = altitude;
    }

    public boolean isStored() {
        return stored;
    }

    public void setStored(boolean stored) {
        this.stored = stored;
    }
}
