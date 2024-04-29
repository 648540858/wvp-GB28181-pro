package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 查询终端属性
 */
@MsgId(id = "8107")
public class J8107 extends Rs {

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        return buffer;
    }

}
