package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "位置信息")
public class JTPositionInfo {

    /**
     * 报警标志
     */
    @Schema(description = "报警标志")
    private JTAlarmSign alarmSign;

    /**
     * 状态
     */
    @Schema(description = "状态")
    private JTStatus status;

    /**
     * 经度
     */
    @Schema(description = "经度")
    private Double longitude;

    /**
     * 纬度
     */
    @Schema(description = "纬度")
    private Double latitude;

    /**
     * 高程
     */
    @Schema(description = "高程")
    private Integer altitude;

    /**
     * 速度
     */
    @Schema(description = "速度")
    private Integer speed;

    /**
     * 方向
     */
    @Schema(description = "方向")
    private Integer direction;

    /**
     * 时间
     */
    @Schema(description = "时间")
    private String time;

    public JTAlarmSign getAlarmSign() {
        return alarmSign;
    }

    public void setAlarmSign(JTAlarmSign alarmSign) {
        this.alarmSign = alarmSign;
    }

    public JTStatus getStatus() {
        return status;
    }

    public void setStatus(JTStatus status) {
        this.status = status;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Integer getAltitude() {
        return altitude;
    }

    public void setAltitude(Integer altitude) {
        this.altitude = altitude;
    }

    public Integer getSpeed() {
        return speed;
    }

    public void setSpeed(Integer speed) {
        this.speed = speed;
    }

    public Integer getDirection() {
        return direction;
    }

    public void setDirection(Integer direction) {
        this.direction = direction;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "位置汇报信息： " +
                " \n 报警标志：" + alarmSign.toString() +
                " \n 状态：" + status.toString() +
                " \n 经度：" + longitude +
                " \n 纬度：" + latitude +
                " \n 高程： " + altitude +
                " \n 速度： " + speed +
                " \n 方向： " + direction +
                " \n 时间： " + time +
                " \n";
    }
}
