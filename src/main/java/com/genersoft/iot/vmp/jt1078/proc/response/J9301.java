package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * 云台控制指令-云台旋转
 *
 */
@Setter
@Getter
@MsgId(id = "9301")
public class J9301 extends Rs {
    // 逻辑通道号
    private int channel;

    // 方向： 0：停止； 1：上； 2：下； 3：左； 4：右
    private int direction;

    // 速度：0 ～ 255
    private int speed;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(channel);
        buffer.writeByte(direction);
        buffer.writeByte(speed);
        return buffer;
    }

    @Override
    public String toString() {
        return "J9301{" +
                "channel=" + channel +
                ", direction=" + direction +
                ", speed=" + speed +
                '}';
    }
}
