package com.genersoft.iot.vmp.jt1078.bean.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * OSD字幕叠加设置
 */
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

    public boolean isTime() {
        return time;
    }

    public void setTime(boolean time) {
        this.time = time;
    }

    public boolean isLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(boolean licensePlate) {
        this.licensePlate = licensePlate;
    }

    public boolean isChannelId() {
        return channelId;
    }

    public void setChannelId(boolean channelId) {
        this.channelId = channelId;
    }

    public boolean isPosition() {
        return position;
    }

    public void setPosition(boolean position) {
        this.position = position;
    }

    public boolean isSpeed() {
        return speed;
    }

    public void setSpeed(boolean speed) {
        this.speed = speed;
    }

    public boolean isSpeedForGPS() {
        return speedForGPS;
    }

    public void setSpeedForGPS(boolean speedForGPS) {
        this.speedForGPS = speedForGPS;
    }

    public boolean isDrivingTime() {
        return drivingTime;
    }

    public void setDrivingTime(boolean drivingTime) {
        this.drivingTime = drivingTime;
    }

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
