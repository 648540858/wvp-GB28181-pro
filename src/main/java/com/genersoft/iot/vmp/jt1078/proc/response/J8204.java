package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 链路检测
 */
@MsgId(id = "8204")
public class J8204 extends Rs {

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        return buffer;
    }

}
