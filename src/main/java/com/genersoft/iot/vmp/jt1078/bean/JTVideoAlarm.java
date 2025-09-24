package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Schema(description = "视频报警上报")
public class JTVideoAlarm {

    @Schema(description = "视频信号丢失报警的通道")
    private List<Integer> videoLossChannels;

    @Schema(description = "视频信号遮挡报警的通道")
    private List<Integer> videoOcclusionChannels;

    @Schema(description = "存储器故障报警状态，第 1-12 个主存储器，12-15 分别表示第 1-4 个灾备存储装置")
    private List<Integer> storageFaultAlarm;

    @Schema(description = "异常驾驶行为-疲劳")
    private boolean drivingForFatigue;

    @Schema(description = "异常驾驶行为-打电话")
    private boolean drivingForCall;

    @Schema(description = "异常驾驶行为-抽烟")
    private boolean drivingSmoking;

    @Schema(description = "其他视频设备故障")
    private boolean otherDeviceFailure;

    @Schema(description = "客车超员报警")
    private boolean overcrowding;

    @Schema(description = "特殊报警录像达到存储阈值报警")
    private boolean specialRecordFull;

    public JTVideoAlarm() {
    }

    public static JTVideoAlarm getInstance(int alarm, int loss, int occlusion, short storageFault, short driving) {
        JTVideoAlarm jtVideoAlarm = new JTVideoAlarm();
        if (alarm == 0) {
            return jtVideoAlarm;
        }
        boolean lossAlarm = (alarm & 1) == 1;
        boolean occlusionAlarm = (alarm >>> 1 & 1) == 1;
        boolean storageFaultAlarm = (alarm >>> 2 & 1) == 1;
        jtVideoAlarm.setOtherDeviceFailure((alarm >>> 3 & 1) == 1);
        jtVideoAlarm.setOvercrowding((alarm >>> 4 & 1) == 1);
        boolean drivingAlarm = (alarm >>> 5 & 1) == 1;
        jtVideoAlarm.setSpecialRecordFull((alarm >>> 6 & 1) == 1);
        if (lossAlarm && loss > 0) {
            List<Integer> videoLossChannels = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                if ((loss >>> i & 1) == 1 ) {
                    videoLossChannels.add(i);
                }
            }
            jtVideoAlarm.setVideoLossChannels(videoLossChannels);
        }
        if (occlusionAlarm && occlusion > 0) {
            List<Integer> videoOcclusionChannels = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                if ((occlusion >>> i & 1) == 1) {
                    videoOcclusionChannels.add(i);
                }
            }
            jtVideoAlarm.setVideoOcclusionChannels(videoOcclusionChannels);
        }
        if (storageFaultAlarm && storageFault > 0) {
            List<Integer> storageFaultAlarmContent = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                if ((storageFault >>> i & 1) == 1) {
                    storageFaultAlarmContent.add(i);
                }
            }
            jtVideoAlarm.setStorageFaultAlarm(storageFaultAlarmContent);
        }
        if (drivingAlarm && driving > 0) {
            jtVideoAlarm.setDrivingForFatigue((driving & 1) == 1 );
            jtVideoAlarm.setDrivingForCall((driving >>> 1 & 1) == 1 );
            jtVideoAlarm.setDrivingSmoking((driving >>> 2 & 1) == 1 );
        }
        return jtVideoAlarm;
    }

}
