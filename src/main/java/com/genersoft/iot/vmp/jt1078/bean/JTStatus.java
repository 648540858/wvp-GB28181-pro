package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "状态信息")
public class JTStatus {

    @Schema(description = "false:ACC关；true: ACC开")
    private boolean acc;

    @Schema(description = "false:未定位；true: 定位")
    private boolean positioning;

    @Schema(description = "false:北纬；true: 南纬")
    private boolean southLatitude;

    @Schema(description = "false:东经；true: 西经")
    private boolean wesLongitude;

    @Schema(description = "false:运营状态；true: 停运状态")
    private boolean outage;

    @Schema(description = "false:经纬度未经保密插件加密；true: 经纬度已经保密插件加密")
    private boolean positionEncryption;


    @Schema(description = "true: 紧急刹车系统采集的前撞预警")
    private boolean warningFrontCrash;

    @Schema(description = "true: 车道偏移预警")
    private boolean warningShifting;

    @Schema(description = "00:空车；01:半载；10:保留；11:满载。可表示客车的空载状态 ,重车及货车的空载、满载状态 ,该状态可由人工输入或传感器获取")
    private int load;

    @Schema(description = "false:车辆油路正常；true: 车辆油路断开")
    private boolean oilWayBreak;

    @Schema(description = "false:车辆电路正常；true: 车辆电路断开")
    private boolean circuitBreak;

    @Schema(description = "false:车门解锁；true: 车门加锁")
    private boolean doorLocking;

    @Schema(description = "false:门1 关；true: 门1 开(前门)")
    private boolean door1Open;

    @Schema(description = "false:门2 关；true: 门2 开(中门)")
    private boolean door2Open;

    @Schema(description = "false:门3 关；true: 门3 开(后门)")
    private boolean door3Open;

    @Schema(description = "false:门4 关；true: 门4 开(驾驶席门)")
    private boolean door4Open;

    @Schema(description = "false:门5 关；true: 门5 开(自定义)")
    private boolean door5Open;

    @Schema(description = "false:未使用 GPS 卫星进行定位；true:使用 GPS 卫星进行定位")
    private boolean gps;

    @Schema(description = "false:未使用北斗卫星进行定位；true:使用北斗卫星进行定位")
    private boolean beidou;

    @Schema(description = "false:未使用GLONASS 卫星进行定位；true:使用GLONASS 卫星进行定位")
    private boolean glonass;

    @Schema(description = "false:未使用GaLiLeo 卫星进行定位；true:使用GaLiLeo 卫星进行定位")
    private boolean gaLiLeo;

    @Schema(description = "false:车辆处于停止状态；true:车辆处于行驶状态")
    private boolean Driving;

    public boolean isAcc() {
        return acc;
    }

    public void setAcc(boolean acc) {
        this.acc = acc;
    }

    public boolean isPositioning() {
        return positioning;
    }

    public void setPositioning(boolean positioning) {
        this.positioning = positioning;
    }

    public boolean isSouthLatitude() {
        return southLatitude;
    }

    public void setSouthLatitude(boolean southLatitude) {
        this.southLatitude = southLatitude;
    }

    public boolean isWesLongitude() {
        return wesLongitude;
    }

    public void setWesLongitude(boolean wesLongitude) {
        this.wesLongitude = wesLongitude;
    }

    public boolean isOutage() {
        return outage;
    }

    public void setOutage(boolean outage) {
        this.outage = outage;
    }

    public boolean isPositionEncryption() {
        return positionEncryption;
    }

    public void setPositionEncryption(boolean positionEncryption) {
        this.positionEncryption = positionEncryption;
    }

    public boolean isWarningFrontCrash() {
        return warningFrontCrash;
    }

    public void setWarningFrontCrash(boolean warningFrontCrash) {
        this.warningFrontCrash = warningFrontCrash;
    }

    public boolean isWarningShifting() {
        return warningShifting;
    }

    public void setWarningShifting(boolean warningShifting) {
        this.warningShifting = warningShifting;
    }

    public int getLoad() {
        return load;
    }

    public void setLoad(int load) {
        this.load = load;
    }

    public boolean isOilWayBreak() {
        return oilWayBreak;
    }

    public void setOilWayBreak(boolean oilWayBreak) {
        this.oilWayBreak = oilWayBreak;
    }

    public boolean isCircuitBreak() {
        return circuitBreak;
    }

    public void setCircuitBreak(boolean circuitBreak) {
        this.circuitBreak = circuitBreak;
    }

    public boolean isDoorLocking() {
        return doorLocking;
    }

    public void setDoorLocking(boolean doorLocking) {
        this.doorLocking = doorLocking;
    }

    public boolean isDoor1Open() {
        return door1Open;
    }

    public void setDoor1Open(boolean door1Open) {
        this.door1Open = door1Open;
    }

    public boolean isDoor2Open() {
        return door2Open;
    }

    public void setDoor2Open(boolean door2Open) {
        this.door2Open = door2Open;
    }

    public boolean isDoor3Open() {
        return door3Open;
    }

    public void setDoor3Open(boolean door3Open) {
        this.door3Open = door3Open;
    }

    public boolean isDoor4Open() {
        return door4Open;
    }

    public void setDoor4Open(boolean door4Open) {
        this.door4Open = door4Open;
    }

    public boolean isDoor5Open() {
        return door5Open;
    }

    public void setDoor5Open(boolean door5Open) {
        this.door5Open = door5Open;
    }

    public boolean isGps() {
        return gps;
    }

    public void setGps(boolean gps) {
        this.gps = gps;
    }

    public boolean isBeidou() {
        return beidou;
    }

    public void setBeidou(boolean beidou) {
        this.beidou = beidou;
    }

    public boolean isGlonass() {
        return glonass;
    }

    public void setGlonass(boolean glonass) {
        this.glonass = glonass;
    }

    public boolean isGaLiLeo() {
        return gaLiLeo;
    }

    public void setGaLiLeo(boolean gaLiLeo) {
        this.gaLiLeo = gaLiLeo;
    }

    public boolean isDriving() {
        return Driving;
    }

    public void setDriving(boolean driving) {
        Driving = driving;
    }
}
