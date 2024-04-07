package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 云台控制指令-红外补光控制
 *
 */
@MsgId(id = "9305")
public class J9305 extends Rs {
    // 逻辑通道号
    private int channel;

    // 启停标识: 0：停止； 1：启动
    private int on;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(channel);
        buffer.writeByte(on);
        return buffer;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getOn() {
        return on;
    }

    public void setOn(int on) {
        this.on = on;
    }

    @Override
    public String toString() {
        return "J9305{" +
                "channel=" + channel +
                ", on=" + on +
                '}';
    }
}
