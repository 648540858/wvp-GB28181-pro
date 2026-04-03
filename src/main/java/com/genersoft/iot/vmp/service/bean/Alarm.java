package com.genersoft.iot.vmp.service.bean;


import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarmNotify;
import com.genersoft.iot.vmp.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "报警信息")
public class Alarm {

    @Schema(description = "数据库id")
    private Long id;

    @Schema(description = "关联通道的数据库id")
    private int channelId;

    @Schema(description = "关联通道国标编号")
    private String channelDeviceId;

    @Schema(description = "关联通道国标名称")
    private String channelName;

    @Schema(description = "报警描述")
    private String description;

    @Schema(description = "报警快照路径")
    private String snapPath;

    @Schema(description = "报警录像路径")
    private String recordPath;

    @Schema(description = "报警附带的经度")
    private Double longitude;

    @Schema(description = "报警附带的纬度")
    private Double latitude;

    @Schema(description = "报警类别")
    private AlarmType alarmType;

    @Schema(description = "报警时间")
    private Long alarmTime;

    public static Alarm buildFromDeviceAlarmNotify(DeviceAlarmNotify deviceAlarmNotify) {
        Alarm alarm = new Alarm();
        alarm.setDescription(deviceAlarmNotify.getAlarmDescription());
        alarm.setAlarmType(deviceAlarmNotify.getAlarmTypeEnum());
        alarm.setAlarmTime(DateUtil.yyyy_MM_dd_HH_mm_ssToTimestampMs(deviceAlarmNotify.getAlarmTime()));
        alarm.setLongitude(deviceAlarmNotify.getLongitude());
        alarm.setLatitude(deviceAlarmNotify.getLatitude());
        return alarm;
    }



}
