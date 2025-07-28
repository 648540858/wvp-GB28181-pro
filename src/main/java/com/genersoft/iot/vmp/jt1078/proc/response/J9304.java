package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * 云台控制指令-云台雨刷控制
 *
 */
@Setter
@Getter
@MsgId(id = "9304")
public class J9304 extends Rs {
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

    @Override
    public String toString() {
        return "J9304{" +
                "channel=" + channel +
                ", on=" + on +
                '}';
    }
}
