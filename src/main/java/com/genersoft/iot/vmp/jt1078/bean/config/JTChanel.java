package com.genersoft.iot.vmp.jt1078.bean.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Stack;

/**
 * 音视频通道
 */
public class JTChanel implements JTDeviceSubConfig{

    /**
     * 物理通道号
     */
    private int physicalChannelId;

    /**
     * 逻辑通道号
     */
    private int logicChannelId;

    /**
     * 通道类型:
     * 0:音视频;
     * 1:音频
     * 2:视频
     */
    private int channelType;
    /**
     * 是否连接云台: 通道类型为 0 和 2 时,此字段有效
     * 0:未连接;1:连接
     */
    private int ptzEnable;

    public int getPhysicalChannelId() {
        return physicalChannelId;
    }

    public void setPhysicalChannelId(int physicalChannelId) {
        this.physicalChannelId = physicalChannelId;
    }

    public int getLogicChannelId() {
        return logicChannelId;
    }

    public void setLogicChannelId(int logicChannelId) {
        this.logicChannelId = logicChannelId;
    }

    public int getChannelType() {
        return channelType;
    }

    public void setChannelType(int channelType) {
        this.channelType = channelType;
    }

    public int getPtzEnable() {
        return ptzEnable;
    }

    public void setPtzEnable(int ptzEnable) {
        this.ptzEnable = ptzEnable;
    }

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(physicalChannelId);
        byteBuf.writeByte(logicChannelId);
        byteBuf.writeByte(channelType);
        byteBuf.writeByte(ptzEnable);
        return byteBuf;
    }

    public static JTChanel decode(ByteBuf byteBuf) {
        JTChanel jtChanel = new JTChanel();
        jtChanel.setPhysicalChannelId(byteBuf.readUnsignedByte());
        jtChanel.setLogicChannelId(byteBuf.readUnsignedByte());
        jtChanel.setChannelType(byteBuf.readUnsignedByte());
        jtChanel.setPtzEnable(byteBuf.readUnsignedByte());
        return jtChanel;
    }
}
