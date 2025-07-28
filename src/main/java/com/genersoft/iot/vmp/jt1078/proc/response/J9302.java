package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * 云台控制指令-焦距控制
 *
 */
@Setter
@Getter
@MsgId(id = "9302")
public class J9302 extends Rs {
    // 逻辑通道号
    private int channel;

    // 方向： 0：焦距调大； 1：焦距调小
    private int focalDirection;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(channel);
        buffer.writeByte(focalDirection);
        return buffer;
    }

    @Override
    public String toString() {
        return "J9302{" +
                "channel=" + channel +
                ", zoomDirection=" + focalDirection +
                '}';
    }
}
