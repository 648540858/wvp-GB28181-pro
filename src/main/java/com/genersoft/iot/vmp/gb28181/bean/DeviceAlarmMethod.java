package com.genersoft.iot.vmp.gb28181.bean;

/**
 * 报警方式
 * @author lin
 * 1为电话报警, 2为设备报警, 3为短信报警, 4为 GPS报警, 5为视频报警, 6为设备故障报警,
 * 7其他报警;可以为直接组合如12为电话报警或 设备报警-
 */
public enum DeviceAlarmMethod {
    // 1为电话报警
    Telephone(1),

    // 2为设备报警
    Device(2),

    // 3为短信报警
    SMS(3),

    // 4为 GPS报警
    GPS(4),

    // 5为视频报警
    Video(5),

    // 6为设备故障报警
    DeviceFailure(6),

    // 7其他报警
    Other(7);

    private final int val;

    DeviceAlarmMethod(int val) {
        this.val=val;
    }

    public int getVal() {
        return val;
    }
}
