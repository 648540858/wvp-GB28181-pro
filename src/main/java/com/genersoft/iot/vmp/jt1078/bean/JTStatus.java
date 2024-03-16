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
    private boolean driving;

    public JTStatus() {
    }

    public JTStatus(int statusInt) {
        if (statusInt == 0) {
            return;
        }
        this.acc = (statusInt & 1) == 1;
        this.positioning = (statusInt >>> 1 & 1) == 1;
        this.southLatitude = (statusInt >>> 2 & 1) == 1;
        this.wesLongitude = (statusInt >>> 3 & 1) == 1;
        this.outage = (statusInt >>> 4 & 1) == 1;
        this.positionEncryption = (statusInt >>> 5 & 1) == 1;
        this.warningFrontCrash = (statusInt >>> 6 & 1) == 1;
        this.warningShifting = (statusInt >>> 7 & 1) == 1;
        this.load = (statusInt >>> 8 & 3);
        this.oilWayBreak = (statusInt >>> 10 & 1) == 1;
        this.circuitBreak = (statusInt >>> 11 & 1) == 1;
        this.doorLocking = (statusInt >>> 12 & 1) == 1;
        this.door1Open = (statusInt >>> 13 & 1) == 1;
        this.door2Open = (statusInt >>> 14 & 1) == 1;
        this.door3Open = (statusInt >>> 15 & 1) == 1;
        this.door4Open = (statusInt >>> 16 & 1) == 1;
        this.door5Open = (statusInt >>> 17 & 1) == 1;
        this.gps = (statusInt >>> 18 & 1) == 1;
        this.beidou = (statusInt >>> 19 & 1) == 1;
        this.glonass = (statusInt >>> 20 & 1) == 1;
        this.gaLiLeo = (statusInt >>> 21 & 1) == 1;
        this.driving = (statusInt >>> 22 & 1) == 1;
    }


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
        return driving;
    }

    public void setDriving(boolean driving) {
        this.driving = driving;
    }

    @Override
    public String toString() {
        return "状态位：" +
                "\n      acc状态：" + (acc?"开":"关") +
                "\n      定位状态：" + (positioning?"定位":"未定位") +
                "\n      南北纬：" + (southLatitude?"南纬":"北纬") +
                "\n      东西经：" + (wesLongitude?"西经":"东经") +
                "\n      运营状态：" + (outage?"停运":"运营") +
                "\n      经纬度保密：" + (positionEncryption?"加密":"未加密") +
                "\n      前撞预警：" + (warningFrontCrash?"紧急刹车系统采集的前撞预警":"无") +
                "\n      车道偏移预警：" + (warningShifting?"车道偏移预警":"无") +
                "\n      空/半/满载状态：" + (load == 0?"空车":(load == 1?"半载":(load == 3?"满载":"未定义状态"))) +
                "\n      车辆油路状态：" + (oilWayBreak?"车辆油路断开":"车辆油路正常") +
                "\n      车辆电路状态：" + (circuitBreak?"车辆电路断开":"车辆电路正常") +
                "\n      门锁状态：" + (doorLocking?"车门加锁":"车门解锁") +
                "\n      门1(前门)状态：" + (door1Open?"开":"关") +
                "\n      门2(中门)状态：" + (door2Open?"开":"关") +
                "\n      门3(后门)状态：" + (door3Open?"开":"关") +
                "\n      门4(驾驶席门)状态：" + (door4Open?"开":"关") +
                "\n      门5(自定义)状态：" + (door5Open?"开":"关") +
                "\n      GPS卫星定位状态： " + (gps?"使用":"未使用") +
                "\n      北斗卫星定位状态： " + (beidou?"使用":"未使用") +
                "\n      GLONASS卫星定位状态： " + (glonass?"使用":"未使用") +
                "\n      GaLiLeo卫星定位状态： " + (gaLiLeo?"使用":"未使用") +
                "\n      GaLiLeo卫星定位状态： " + (gaLiLeo?"使用":"未使用") +
                "\n      车辆行驶状态： " + (driving?"车辆行驶":"车辆停止") +
                "\n       ";
    }
}
