package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 查询终端音视频属性
 */
@MsgId(id = "9003")
public class J9003 extends Rs {

    @Override
    public ByteBuf encode() {
        return Unpooled.buffer();
    }

}
