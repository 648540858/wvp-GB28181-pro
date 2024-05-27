package com.genersoft.iot.vmp.jt1078.bean.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 特殊报警录像参数
 */
public class JTAlarmRecordingParam implements  JTDeviceSubConfig{

    /**
     * 特殊报警录像存储阈值, 分比,取值 特殊报警录像占用主存储器存储阈值百 1 ~ 99,默认值为 20
     */
    private int storageLimit;

    /**
     * 特殊报警录像持续时间,特殊报警录像的最长持续时间,单位为分钟(min) ,默认值为 5
     */
    private int duration;

    /**
     * 特殊报警标识起始时间, 特殊报警发生前进行标记的录像时间, 单位为分钟( min) ,默认值为 1
     */
    private int startTime;

    public int getStorageLimit() {
        return storageLimit;
    }

    public void setStorageLimit(int storageLimit) {
        this.storageLimit = storageLimit;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(storageLimit);
        byteBuf.writeByte(duration);
        byteBuf.writeByte(startTime);
        return byteBuf;
    }

    public static JTAlarmRecordingParam decode(ByteBuf byteBuf) {
        JTAlarmRecordingParam alarmRecordingParam = new JTAlarmRecordingParam();
        alarmRecordingParam.setStorageLimit(byteBuf.readUnsignedByte());
        alarmRecordingParam.setDuration(byteBuf.readUnsignedByte());
        alarmRecordingParam.setStartTime(byteBuf.readUnsignedByte());
        return alarmRecordingParam;
    }
}
