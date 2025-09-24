package com.genersoft.iot.vmp.jt1078.bean.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 视频报警标志位
 */
public class JTVideoAlarmBit implements JTDeviceSubConfig{

    /**
     * 视频信号丢失报警
     */
    private boolean lossSignal;
    /**
     * 视频信号遮挡报警
     */
    private boolean occlusionSignal;
    /**
     * 存储单元故障报警
     */
    private boolean storageFault;
    /**
     * 其他视频设备故障报警
     */
    private boolean otherDeviceFailure;
    /**
     * 客车超员报警
     */
    private boolean overcrowding;
    /**
     * 异常驾驶行为报警
     */
    private boolean abnormalDriving;
    /**
     * 特殊报警录像达到存储阈值报警
     */
    private boolean storageLimit;

    public boolean isLossSignal() {
        return lossSignal;
    }

    public void setLossSignal(boolean lossSignal) {
        this.lossSignal = lossSignal;
    }

    public boolean isOcclusionSignal() {
        return occlusionSignal;
    }

    public void setOcclusionSignal(boolean occlusionSignal) {
        this.occlusionSignal = occlusionSignal;
    }

    public boolean isStorageFault() {
        return storageFault;
    }

    public void setStorageFault(boolean storageFault) {
        this.storageFault = storageFault;
    }

    public boolean isOtherDeviceFailure() {
        return otherDeviceFailure;
    }

    public void setOtherDeviceFailure(boolean otherDeviceFailure) {
        this.otherDeviceFailure = otherDeviceFailure;
    }

    public boolean isOvercrowding() {
        return overcrowding;
    }

    public void setOvercrowding(boolean overcrowding) {
        this.overcrowding = overcrowding;
    }

    public boolean isAbnormalDriving() {
        return abnormalDriving;
    }

    public void setAbnormalDriving(boolean abnormalDriving) {
        this.abnormalDriving = abnormalDriving;
    }

    public boolean isStorageLimit() {
        return storageLimit;
    }

    public void setStorageLimit(boolean storageLimit) {
        this.storageLimit = storageLimit;
    }

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byte content = 0;
        if (lossSignal) {
            content = content |= 1;
        }
        if (occlusionSignal) {
            content = content |= (1 << 1);
        }
        if (storageFault) {
            content = content |= (1 << 2);
        }
        if (otherDeviceFailure) {
            content = content |= (1 << 3);
        }
        if (overcrowding) {
            content = content |= (1 << 4);
        }
        if (abnormalDriving) {
            content = content |= (1 << 5);
        }
        if (storageLimit) {
            content = content |= (1 << 6);
        }
        byteBuf.writeByte(content);
        byteBuf.writeByte(0);
        byteBuf.writeByte(0);
        byteBuf.writeByte(0);
        return byteBuf;
    }

    public static JTVideoAlarmBit decode(ByteBuf byteBuf) {
        JTVideoAlarmBit videoAlarmBit = new JTVideoAlarmBit();
        byte content = byteBuf.readByte();
        videoAlarmBit.setLossSignal((content & 1) == 1);
        videoAlarmBit.setOcclusionSignal((content >>> 1 & 1) == 1);
        videoAlarmBit.setStorageFault((content >>> 2 & 1) == 1);
        videoAlarmBit.setOtherDeviceFailure((content >>> 3 & 1) == 1);
        videoAlarmBit.setOvercrowding((content >>> 4 & 1) == 1);
        videoAlarmBit.setAbnormalDriving((content >>> 5 & 1) == 1);
        videoAlarmBit.setStorageLimit((content >>> 6 & 1) == 1);
        byteBuf.readByte();
        byteBuf.readByte();
        byteBuf.readByte();
        return videoAlarmBit;
    }
}
