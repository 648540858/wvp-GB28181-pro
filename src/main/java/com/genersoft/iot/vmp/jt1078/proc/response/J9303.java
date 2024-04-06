package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 云台控制指令-光圈控制
 *
 */
@MsgId(id = "9302")
public class J9303 extends Rs {
    // 逻辑通道号
    private int channel;

    // 调整方式: 0：调大； 1：调小
    private int iris;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(channel);
        buffer.writeByte(iris);
        return buffer;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getIris() {
        return iris;
    }

    public void setIris(int iris) {
        this.iris = iris;
    }

    @Override
    public String toString() {
        return "J9303{" +
                "channel=" + channel +
                ", iris=" + iris +
                '}';
    }
}
