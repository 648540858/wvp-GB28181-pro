package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 云台控制指令-焦距控制
 *
 */
@MsgId(id = "9302")
public class J9302 extends Rs {
    // 逻辑通道号
    private int channel;

    // 方向： 0：焦距调大； 1：焦距调小
    private int zoomDirection;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(channel);
        buffer.writeByte(zoomDirection);
        return buffer;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getZoomDirection() {
        return zoomDirection;
    }

    public void setZoomDirection(int zoomDirection) {
        this.zoomDirection = zoomDirection;
    }

    @Override
    public String toString() {
        return "J9302{" +
                "channel=" + channel +
                ", zoomDirection=" + zoomDirection +
                '}';
    }
}
