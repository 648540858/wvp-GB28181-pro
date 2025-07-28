package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * 临时位置跟踪控制
 */
@Setter
@Getter
@MsgId(id = "8202")
public class J8202 extends Rs {

    /**
     * 时间间隔,单位为秒,时间间隔为0 时停止跟踪,停止跟踪无需带后继字段
     */
    private int timeInterval;

    /**
     * 位置跟踪有效期, 单位为秒,终端在接收到位置跟踪控制消息后,在有效期截止时间之前依据消息中的时间间隔发送位置汇报
     */
    private long validityPeriod;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeShort((short)(timeInterval & 0xffff));
        if (timeInterval > 0) {
            buffer.writeInt((int) (validityPeriod & 0xffffffffL));
        }
        return buffer;
    }

}
