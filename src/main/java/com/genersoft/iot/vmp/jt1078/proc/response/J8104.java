package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Arrays;

/**
 * 查询终端参数
 */
@MsgId(id = "8104")
public class J8104 extends Rs {

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        return buffer;
    }

}
