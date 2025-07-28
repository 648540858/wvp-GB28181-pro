package com.genersoft.iot.vmp.jt1078.bean.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * OSD字幕叠加设置
 */
@Setter
@Getter
public class JTOSDConfig {

    /**
     * 日期和时间
     */
    private boolean time;

    /**
     * 车牌号码
     */
    private boolean licensePlate;

    /**
     * 逻辑通道号
     */
    private boolean channelId;

    /**
     * 经纬度
     */
    private boolean position;

    /**
     * 行驶记录速度
     */
    private boolean speed;

    /**
     * 卫星定位速度
     */
    private boolean speedForGPS;

    /**
     * 连续驾驶时间
     */
    private boolean drivingTime;

    public ByteBuf encode(){
        ByteBuf byteBuf = Unpooled.buffer();
        byte content = 0;
        if (time) {
            content = (byte)(content | 1);
        }
        if (licensePlate) {
            content = (byte)(content | (1 << 1));
        }
        if (channelId) {
            content = (byte)(content | (1 << 2));
        }
        if (position) {
            content = (byte)(content | (1 << 3));
        }
        if (speed) {
            content = (byte)(content | (1 << 4));
        }
        if (speedForGPS) {
            content = (byte)(content | (1 << 5));
        }
        if (drivingTime) {
            content = (byte)(content | (1 << 6));
        }
        byteBuf.writeByte(content);
        byteBuf.writeByte(0);
        return byteBuf;

    }

    public static JTOSDConfig decode(ByteBuf buf) {
        JTOSDConfig config = new JTOSDConfig();
        int content = buf.readUnsignedShort();
        config.setTime((content & 1) == 1);
        config.setLicensePlate((content >>> 1 & 1) == 1);
        config.setChannelId((content >>> 2 & 1) == 1);
        config.setPosition((content >>> 3 & 1) == 1);
        config.setSpeed((content >>> 4 & 1) == 1);
        config.setSpeedForGPS((content >>> 5 & 1) == 1);
        config.setDrivingTime((content >>> 6 & 1) == 1);
        return config;
    }
}
