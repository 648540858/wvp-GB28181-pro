package com.genersoft.iot.vmp.gb28181.bean;


/**
 * 通过redis分发报警消息
 */
public class AlarmChannelMessage {
    /**
     * 国标编号
     */
    private String gbId;
    /**
     * 报警编号
     */
    private int alarmSn;
    /**
     * 告警类型
     */
    private int alarmType;

    /**
     * 报警描述
     */
    private String alarmDescription;

    public String getGbId() {
        return gbId;
    }

    public void setGbId(String gbId) {
        this.gbId = gbId;
    }

    public int getAlarmSn() {
        return alarmSn;
    }

    public void setAlarmSn(int alarmSn) {
        this.alarmSn = alarmSn;
    }

    public int getAlarmType() {
        return alarmType;
    }

    public void setAlarmType(int alarmType) {
        this.alarmType = alarmType;
    }

    public String getAlarmDescription() {
        return alarmDescription;
    }

    public void setAlarmDescription(String alarmDescription) {
        this.alarmDescription = alarmDescription;
    }
}
