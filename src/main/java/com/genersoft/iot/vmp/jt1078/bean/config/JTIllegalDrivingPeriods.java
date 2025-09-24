package com.genersoft.iot.vmp.jt1078.bean.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * 违规行驶时段范围 ,精确到分
 */
@Setter
@Getter
public class JTIllegalDrivingPeriods implements JTDeviceSubConfig{
    /**
     * 违规行驶时段-开始时间 HH:mm
     */
    private String startTime;

    /**
     * 违规行驶时段-结束时间 HH:mm
     */
    private String endTime;

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byte[] bytes = new byte[4];
        String[] startTimeArray = startTime.split(":");
        String[] endTimeArray = endTime.split(":");
        bytes[0] = (byte)Integer.parseInt(startTimeArray[0]);
        bytes[1] = (byte)Integer.parseInt(startTimeArray[1]);
        bytes[2] = (byte)Integer.parseInt(endTimeArray[0]);
        bytes[3] = (byte)Integer.parseInt(endTimeArray[1]);
        byteBuf.writeBytes(bytes);
        return byteBuf;
    }
}
