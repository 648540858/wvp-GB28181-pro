package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 报警标志
 */
@Schema(description = "报警标志")
public class JTAlarmSign {

    @Schema(description = "紧急报警，触动报警开关后触发")
    private boolean urgent;

    @Schema(description = "超速报警")
    private boolean alarmSpeeding;

    @Schema(description = "疲劳驾驶报警")
    private boolean alarmTired;
    @Schema(description = "危险驾驶行为报警")
    private boolean alarmDangerous;
    @Schema(description = "GNSS 模块发生故障报警")
    private boolean alarmGnssFault;
    @Schema(description = "GNSS 天线未接或被剪断报警")
    private boolean alarmGnssBreak;
    @Schema(description = "GNSS 天线短路报警")
    private boolean alarmGnssShortCircuited;
    @Schema(description = "终端主电源欠压报警")
    private boolean alarmUnderVoltage;
    @Schema(description = "终端主电源掉电报警")
    private boolean alarmPowerOff;
    @Schema(description = "终端 LCD或显示器故障报警")
    private boolean alarmLCD;
    @Schema(description = "TTS 模块故障报警")
    private boolean alarmTtsFault;
    @Schema(description = "摄像头故障报警")
    private boolean alarmCameraFault;
    @Schema(description = "道路运输证 IC卡模块故障报警")
    private boolean alarmIcFault;
    @Schema(description = "超速预警")
    private boolean warningSpeeding;
    @Schema(description = "疲劳驾驶预警")
    private boolean warningTired;
    @Schema(description = "违规行驶报警")
    private boolean alarmwrong;
    @Schema(description = "胎压预警")
    private boolean warningTirePressure;
    @Schema(description = "右转盲区异常报警")
    private boolean alarmBlindZone;
    @Schema(description = "当天累计驾驶超时报警")
    private boolean alarmDrivingTimeout;
    @Schema(description = "超时停车报警")
    private boolean alarmParkingTimeout;
    @Schema(description = "进出区域报警")
    private boolean alarmRegion;
    @Schema(description = "进出路线报警")
    private boolean alarmRoute;
    @Schema(description = "路段行驶时间不足/过长报警")
    private boolean alarmTravelTime;
    @Schema(description = "路线偏离报警")
    private boolean alarmRouteDeviation;
    @Schema(description = "车辆 VSS 故障")
    private boolean alarmVSS;
    @Schema(description = "车辆油量异常报警")
    private boolean alarmOil;
    @Schema(description = "车辆被盗报警(通过车辆防盗器)")
    private boolean alarmStolen;
    @Schema(description = "车辆非法点火报警")
    private boolean alarmIllegalIgnition;
    @Schema(description = "车辆非法位移报警")
    private boolean alarmIllegalDisplacement;
    @Schema(description = "碰撞侧翻报警")
    private boolean alarmRollover;
    @Schema(description = "侧翻预警")
    private boolean warningRollover;

    public boolean isUrgent() {
        return urgent;
    }

    public void setUrgent(boolean urgent) {
        this.urgent = urgent;
    }

    public boolean isAlarmSpeeding() {
        return alarmSpeeding;
    }

    public void setAlarmSpeeding(boolean alarmSpeeding) {
        this.alarmSpeeding = alarmSpeeding;
    }

    public boolean isAlarmTired() {
        return alarmTired;
    }

    public void setAlarmTired(boolean alarmTired) {
        this.alarmTired = alarmTired;
    }

    public boolean isAlarmDangerous() {
        return alarmDangerous;
    }

    public void setAlarmDangerous(boolean alarmDangerous) {
        this.alarmDangerous = alarmDangerous;
    }

    public boolean isAlarmGnssFault() {
        return alarmGnssFault;
    }

    public void setAlarmGnssFault(boolean alarmGnssFault) {
        this.alarmGnssFault = alarmGnssFault;
    }

    public boolean isAlarmGnssBreak() {
        return alarmGnssBreak;
    }

    public void setAlarmGnssBreak(boolean alarmGnssBreak) {
        this.alarmGnssBreak = alarmGnssBreak;
    }

    public boolean isAlarmGnssShortCircuited() {
        return alarmGnssShortCircuited;
    }

    public void setAlarmGnssShortCircuited(boolean alarmGnssShortCircuited) {
        this.alarmGnssShortCircuited = alarmGnssShortCircuited;
    }

    public boolean isAlarmUnderVoltage() {
        return alarmUnderVoltage;
    }

    public void setAlarmUnderVoltage(boolean alarmUnderVoltage) {
        this.alarmUnderVoltage = alarmUnderVoltage;
    }

    public boolean isAlarmPowerOff() {
        return alarmPowerOff;
    }

    public void setAlarmPowerOff(boolean alarmPowerOff) {
        this.alarmPowerOff = alarmPowerOff;
    }

    public boolean isAlarmLCD() {
        return alarmLCD;
    }

    public void setAlarmLCD(boolean alarmLCD) {
        this.alarmLCD = alarmLCD;
    }

    public boolean isAlarmTtsFault() {
        return alarmTtsFault;
    }

    public void setAlarmTtsFault(boolean alarmTtsFault) {
        this.alarmTtsFault = alarmTtsFault;
    }

    public boolean isAlarmCameraFault() {
        return alarmCameraFault;
    }

    public void setAlarmCameraFault(boolean alarmCameraFault) {
        this.alarmCameraFault = alarmCameraFault;
    }

    public boolean isAlarmIcFault() {
        return alarmIcFault;
    }

    public void setAlarmIcFault(boolean alarmIcFault) {
        this.alarmIcFault = alarmIcFault;
    }

    public boolean isWarningSpeeding() {
        return warningSpeeding;
    }

    public void setWarningSpeeding(boolean warningSpeeding) {
        this.warningSpeeding = warningSpeeding;
    }

    public boolean isWarningTired() {
        return warningTired;
    }

    public void setWarningTired(boolean warningTired) {
        this.warningTired = warningTired;
    }

    public boolean isAlarmwrong() {
        return alarmwrong;
    }

    public void setAlarmwrong(boolean alarmwrong) {
        this.alarmwrong = alarmwrong;
    }

    public boolean isWarningTirePressure() {
        return warningTirePressure;
    }

    public void setWarningTirePressure(boolean warningTirePressure) {
        this.warningTirePressure = warningTirePressure;
    }

    public boolean isAlarmBlindZone() {
        return alarmBlindZone;
    }

    public void setAlarmBlindZone(boolean alarmBlindZone) {
        this.alarmBlindZone = alarmBlindZone;
    }

    public boolean isAlarmDrivingTimeout() {
        return alarmDrivingTimeout;
    }

    public void setAlarmDrivingTimeout(boolean alarmDrivingTimeout) {
        this.alarmDrivingTimeout = alarmDrivingTimeout;
    }

    public boolean isAlarmParkingTimeout() {
        return alarmParkingTimeout;
    }

    public void setAlarmParkingTimeout(boolean alarmParkingTimeout) {
        this.alarmParkingTimeout = alarmParkingTimeout;
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

    public boolean isAlarmRouteDeviation() {
        return alarmRouteDeviation;
    }

    public void setAlarmRouteDeviation(boolean alarmRouteDeviation) {
        this.alarmRouteDeviation = alarmRouteDeviation;
    }

    public boolean isAlarmVSS() {
        return alarmVSS;
    }

    public void setAlarmVSS(boolean alarmVSS) {
        this.alarmVSS = alarmVSS;
    }

    public boolean isAlarmOil() {
        return alarmOil;
    }

    public void setAlarmOil(boolean alarmOil) {
        this.alarmOil = alarmOil;
    }

    public boolean isAlarmStolen() {
        return alarmStolen;
    }

    public void setAlarmStolen(boolean alarmStolen) {
        this.alarmStolen = alarmStolen;
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

    public boolean isAlarmRollover() {
        return alarmRollover;
    }

    public void setAlarmRollover(boolean alarmRollover) {
        this.alarmRollover = alarmRollover;
    }

    public boolean isWarningRollover() {
        return warningRollover;
    }

    public void setWarningRollover(boolean warningRollover) {
        this.warningRollover = warningRollover;
    }
}
