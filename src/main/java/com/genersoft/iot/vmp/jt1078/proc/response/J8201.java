package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 位置信息查询
 */
@MsgId(id = "8201")
public class J8201 extends Rs {

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        return buffer;
    }

}
