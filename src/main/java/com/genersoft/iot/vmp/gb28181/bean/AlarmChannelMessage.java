package com.genersoft.iot.vmp.gb28181.bean;


import lombok.Data;

/**
 * 通过redis分发报警消息
 */
@Data
public class AlarmChannelMessage {
    /**
     * 通道国标编号
     */
    private String gbId;

    /**
     * 报警编号
     */
    private Integer alarmSn;

    /**
     * 告警类型
     */
    private Integer alarmType;

    /**
     * 报警描述
     */
    private String alarmDescription;
}
