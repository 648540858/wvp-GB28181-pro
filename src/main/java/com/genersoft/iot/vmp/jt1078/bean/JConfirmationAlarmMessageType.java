package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;

public class JConfirmationAlarmMessageType {
    @Schema(description = "确认紧急报警 ；")
    private boolean urgent;
    @Schema(description = "确认危险预警")
    private boolean alarmDangerous;
    @Schema(description = "确认进出区域报警")
    private boolean alarmRegion;
    @Schema(description = "确认进出路线报警")
    private boolean alarmRoute;
    @Schema(description = "确认路段行驶时间不足/过长报警")
    private boolean alarmTravelTime;
    @Schema(description = "确认车辆非法点火报警")
    private boolean alarmIllegalIgnition;
    @Schema(description = "确认车辆非法位移报警")
    private boolean alarmIllegalDisplacement;



    public boolean isUrgent() {
        return urgent;
    }

    public void setUrgent(boolean urgent) {
        this.urgent = urgent;
    }

    public boolean isAlarmDangerous() {
        return alarmDangerous;
    }

    public void setAlarmDangerous(boolean alarmDangerous) {
        this.alarmDangerous = alarmDangerous;
    }

    public boolean isAlarmRegion() {
        return alarmRegion;
    }

    public void setAlarmRegion(boolean alarmRegion) {
        this.alarmRegion = alarmRegion;
    }

    public boolean isAlarmRoute() {
        return alarmRoute;
    }

    public void setAlarmRoute(boolean alarmRoute) {
        this.alarmRoute = alarmRoute;
    }

    public boolean isAlarmTravelTime() {
        return alarmTravelTime;
    }

    public void setAlarmTravelTime(boolean alarmTravelTime) {
        this.alarmTravelTime = alarmTravelTime;
    }

    public boolean isAlarmIllegalIgnition() {
        return alarmIllegalIgnition;
    }

    public void setAlarmIllegalIgnition(boolean alarmIllegalIgnition) {
        this.alarmIllegalIgnition = alarmIllegalIgnition;
    }

    public boolean isAlarmIllegalDisplacement() {
        return alarmIllegalDisplacement;
    }

    public void setAlarmIllegalDisplacement(boolean alarmIllegalDisplacement) {
        this.alarmIllegalDisplacement = alarmIllegalDisplacement;
    }
}
