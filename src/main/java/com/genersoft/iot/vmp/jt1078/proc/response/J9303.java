package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * 云台控制指令-光圈控制
 *
 */
@Setter
@Getter
@MsgId(id = "9303")
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

    @Override
    public String toString() {
        return "J9303{" +
                "channel=" + channel +
                ", iris=" + iris +
                '}';
    }
}
