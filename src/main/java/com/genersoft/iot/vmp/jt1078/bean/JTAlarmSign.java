package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.jt1078.bean.config.JTDeviceSubConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 报警标志
 */
@Data
@Schema(description = "报警标志")
public class JTAlarmSign implements JTDeviceSubConfig {

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
    private boolean alarmWrong;
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

    public JTAlarmSign() {
    }

    public JTAlarmSign(long alarmSignInt) {
        if (alarmSignInt == 0) {
            return;
        }
        // 解析alarm参数
        this.urgent = (alarmSignInt & 1) == 1;
        this.alarmSpeeding = (alarmSignInt >>> 1 & 1) == 1;
        this.alarmTired = (alarmSignInt >>> 2 & 1) == 1;
        this.alarmDangerous = (alarmSignInt >>> 3 & 1) == 1;
        this.alarmGnssFault = (alarmSignInt >>> 4 & 1) == 1;
        this.alarmGnssBreak = (alarmSignInt >>> 5 & 1) == 1;
        this.alarmGnssShortCircuited = (alarmSignInt >>> 6 & 1) == 1;
        this.alarmUnderVoltage = (alarmSignInt >>> 7 & 1) == 1;
        this.alarmPowerOff = (alarmSignInt >>> 8 & 1) == 1;
        this.alarmLCD = (alarmSignInt >>> 9 & 1) == 1;
        this.alarmTtsFault = (alarmSignInt >>> 10 & 1) == 1;
        this.alarmCameraFault = (alarmSignInt >>> 11 & 1) == 1;
        this.alarmIcFault = (alarmSignInt >>> 12 & 1) == 1;
        this.warningSpeeding = (alarmSignInt >>> 13 & 1) == 1;
        this.warningTired = (alarmSignInt >>> 14 & 1) == 1;
        this.alarmWrong = (alarmSignInt >>> 15 & 1) == 1;
        this.warningTirePressure = (alarmSignInt >>> 16 & 1) == 1;
        this.alarmBlindZone = (alarmSignInt >>> 17 & 1) == 1;
        this.alarmDrivingTimeout = (alarmSignInt >>> 18 & 1) == 1;
        this.alarmParkingTimeout = (alarmSignInt >>> 19 & 1) == 1;
        this.alarmRegion = (alarmSignInt >>> 20 & 1) == 1;
        this.alarmRoute = (alarmSignInt >>> 21 & 1) == 1;
        this.alarmTravelTime = (alarmSignInt >>> 22 & 1) == 1;
        this.alarmRouteDeviation = (alarmSignInt >>> 23 & 1) == 1;
        this.alarmVSS = (alarmSignInt >>> 24 & 1) == 1;
        this.alarmOil = (alarmSignInt >>> 25 & 1) == 1;
        this.alarmStolen = (alarmSignInt >>> 26 & 1) == 1;
        this.alarmIllegalIgnition = (alarmSignInt >>> 27 & 1) == 1;
        this.alarmIllegalDisplacement = (alarmSignInt >>> 28 & 1) == 1;
        this.alarmRollover = (alarmSignInt >>> 29 & 1) == 1;
        this.warningRollover = (alarmSignInt >>> 30 & 1) == 1;
    }

    public static JTAlarmSign decode(ByteBuf byteBuf) {
        long alarmSignByte = byteBuf.readUnsignedInt();
        return new JTAlarmSign(alarmSignByte);
    }

    @Override
    public ByteBuf encode() {
        // 限定容量 避免影响后续占位
        ByteBuf byteBuf = Unpooled.buffer();
        int alarmSignValue = 0;
        if (urgent) {
            alarmSignValue = alarmSignValue | 1;
        }
        if (alarmSpeeding) {
            alarmSignValue = alarmSignValue | 1 << 1;
        }
        if (alarmTired) {
            alarmSignValue = alarmSignValue | 1 << 2;
        }
        if (alarmDangerous) {
            alarmSignValue = alarmSignValue | 1 << 3;
        }
        if (alarmGnssFault) {
            alarmSignValue = alarmSignValue | 1 << 4;
        }
        if (alarmGnssBreak) {
            alarmSignValue = alarmSignValue | 1 << 5;
        }
        if (alarmGnssShortCircuited) {
            alarmSignValue = alarmSignValue | 1 << 6;
        }
        if (alarmUnderVoltage) {
            alarmSignValue = alarmSignValue | 1 << 7;
        }
        if (alarmPowerOff) {
            alarmSignValue = alarmSignValue | 1 << 8;
        }
        if (alarmLCD) {
            alarmSignValue = alarmSignValue | 1 << 9;
        }
        if (alarmTtsFault) {
            alarmSignValue = alarmSignValue | 1 << 10;
        }
        if (alarmCameraFault) {
            alarmSignValue = alarmSignValue | 1 << 11;
        }
        if (alarmIcFault) {
            alarmSignValue = alarmSignValue | 1 << 12;
        }
        if (warningSpeeding) {
            alarmSignValue = alarmSignValue | 1 << 13;
        }
        if (warningTired) {
            alarmSignValue = alarmSignValue | 1 << 14;
        }
        if (alarmWrong) {
            alarmSignValue = alarmSignValue | 1 << 15;
        }
        if (warningTirePressure) {
            alarmSignValue = alarmSignValue | 1 << 16;
        }
        if (alarmBlindZone) {
            alarmSignValue = alarmSignValue | 1 << 17;
        }
        if (alarmDrivingTimeout) {
            alarmSignValue = alarmSignValue | 1 << 18;
        }
        if (alarmParkingTimeout) {
            alarmSignValue = alarmSignValue | 1 << 19;
        }
        if (alarmRegion) {
            alarmSignValue = alarmSignValue | 1 << 20;
        }
        if (alarmRoute) {
            alarmSignValue = alarmSignValue | 1 << 21;
        }
        if (alarmTravelTime) {
            alarmSignValue = alarmSignValue | 1 << 22;
        }
        if (alarmRouteDeviation) {
            alarmSignValue = alarmSignValue | 1 << 23;
        }
        if (alarmVSS) {
            alarmSignValue = alarmSignValue | 1 << 24;
        }
        if (alarmOil) {
            alarmSignValue = alarmSignValue | 1 << 25;
        }
        if (alarmStolen) {
            alarmSignValue = alarmSignValue | 1 << 26;
        }
        if (alarmIllegalIgnition) {
            alarmSignValue = alarmSignValue | 1 << 27;
        }
        if (alarmIllegalDisplacement) {
            alarmSignValue = alarmSignValue | 1 << 28;
        }
        if (alarmRollover) {
            alarmSignValue = alarmSignValue | 1 << 29;
        }
        if (warningRollover) {
            alarmSignValue = alarmSignValue | 1 << 30;
        }
        byteBuf.writeInt(alarmSignValue);
        return byteBuf;
    }

    @Override
    public String toString() {
        return "状态报警标志位：" +
                "\n      紧急报警：" + (urgent?"开":"关") +
                "\n      超速报警：" + (alarmSpeeding?"开":"关") +
                "\n      疲劳驾驶报警：" + (alarmTired?"开":"关") +
                "\n      危险驾驶行为报警：" + (alarmDangerous?"开":"关") +
                "\n      GNSS 模块发生故障报警：" + (alarmGnssFault?"开":"关") +
                "\n      GNSS 天线未接或被剪断报警：" + (alarmGnssBreak?"开":"关") +
                "\n      GNSS 天线短路报警：" + (alarmGnssShortCircuited?"开":"关") +
                "\n      终端主电源欠压报警：" + (alarmUnderVoltage?"开":"关") +
                "\n      终端主电源掉电报警：" + (alarmPowerOff?"开":"关") +
                "\n      终端LCD或显示器故障报警：" + (alarmLCD?"开":"关") +
                "\n      TTS 模块故障报警：" + (alarmTtsFault?"开":"关") +
                "\n      摄像头故障报警：" + (alarmCameraFault?"开":"关") +
                "\n      道路运输证IC卡模块故障报警：" + (alarmIcFault?"开":"关") +
                "\n      超速预警：" + (warningSpeeding?"开":"关") +
                "\n      疲劳驾驶预警：" + (warningTired?"开":"关") +
                "\n      违规行驶报警：" + (alarmWrong ?"开":"关") +
                "\n      胎压预警：" + (warningTirePressure?"开":"关") +
                "\n      右转盲区异常报警：" + (alarmBlindZone?"开":"关") +
                "\n      当天累计驾驶超时报警：" + (alarmDrivingTimeout?"开":"关") +
                "\n      超时停车报警：" + (alarmParkingTimeout?"开":"关") +
                "\n      进出区域报警：" + (alarmRegion?"开":"关") +
                "\n      进出路线报警：" + (alarmRoute?"开":"关") +
                "\n      路段行驶时间不足/过长报警：" + (alarmTravelTime?"开":"关") +
                "\n      路线偏离报警：" + (alarmRouteDeviation?"开":"关") +
                "\n      车辆 VSS 故障：" + (alarmVSS?"开":"关") +
                "\n      车辆油量异常报警：" + (alarmOil?"开":"关") +
                "\n      车辆被盗报警(通过车辆防盗器)：" + (alarmStolen?"开":"关") +
                "\n      车辆非法点火报警：" + (alarmIllegalIgnition?"开":"关") +
                "\n      车辆非法位移报警：" + (alarmIllegalDisplacement?"开":"关") +
                "\n      碰撞侧翻报警：" + (alarmRollover?"开":"关") +
                "\n      侧翻预警：" + (warningRollover?"开":"关") +
                "\n       ";
    }

}
