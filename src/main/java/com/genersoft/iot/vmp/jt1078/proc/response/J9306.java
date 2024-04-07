package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 云台控制指令-云台变倍控制
 *
 */
@MsgId(id = "9306")
public class J9306 extends Rs {
    // 逻辑通道号
    private int channel;

    // 0：调大； 1：调小
    private int zoom;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(channel);
        buffer.writeByte(zoom);
        return buffer;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getZoom() {
        return zoom;
    }

    public void setZoom(int zoom) {
        this.zoom = zoom;
    }

    @Override
    public String toString() {
        return "J9306{" +
                "channel=" + channel +
                ", zoom=" + zoom +
                '}';
    }
}
