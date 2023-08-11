package com.genersoft.iot.vmp.gb28181.bean;

/**
 * @description: 移动位置bean
 * @author: lawrencehj
 * @date: 2021年1月23日
 */

public class MobilePosition {
    /**
     * 设备Id
     */
    private String deviceId;

    /**
     * 通道Id
     */
    private String channelId;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 通知时间
     */
    private String time;

    /**
     * 经度
     */
    private double longitude;

    /**
     * 纬度
     */
    private double latitude;

    /**
     * 海拔高度
     */
    private double altitude;

    /**
     * 速度
     */
    private double speed;

    /**
     * 方向
     */
    private double direction;

    /**
     * 位置信息上报来源（Mobile Position、GPS Alarm）
     */
    private String reportSource;

    /**
     * 国内坐标系：经度坐标
     */
    private double longitudeGcj02;

    /**
     * 国内坐标系：纬度坐标
     */
    private double latitudeGcj02;

    /**
     * 国内坐标系：经度坐标
     */
    private double longitudeWgs84;

    /**
     * 国内坐标系：纬度坐标
     */
    private double latitudeWgs84;

    /**
     * 创建时间
     */
    private String createTime;


    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
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

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getDirection() {
        return direction;
    }

    public void setDirection(double direction) {
        this.direction = direction;
    }

    public String getReportSource() {
        return reportSource;
    }

    public void setReportSource(String reportSource) {
        this.reportSource = reportSource;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public double getLongitudeGcj02() {
        return longitudeGcj02;
    }

    public void setLongitudeGcj02(double longitudeGcj02) {
        this.longitudeGcj02 = longitudeGcj02;
    }

    public double getLatitudeGcj02() {
        return latitudeGcj02;
    }

    public void setLatitudeGcj02(double latitudeGcj02) {
        this.latitudeGcj02 = latitudeGcj02;
    }

    public double getLongitudeWgs84() {
        return longitudeWgs84;
    }

    public void setLongitudeWgs84(double longitudeWgs84) {
        this.longitudeWgs84 = longitudeWgs84;
    }

    public double getLatitudeWgs84() {
        return latitudeWgs84;
    }

    public void setLatitudeWgs84(double latitudeWgs84) {
        this.latitudeWgs84 = latitudeWgs84;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}
