package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

import java.util.Arrays;

/**
 * 查询指定终端参数
 */
@MsgId(id = "8106")
public class J8106 extends Rs {

    private byte[] params;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(params.length);
        for (int param : params) {
            buffer.writeByte(param);
        }
        return buffer;
    }

    public byte[] getParams() {
        return params;
    }

    public void setParams(byte[] params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return "J8106{" +
                "params=" + Arrays.toString(params) +
                '}';
    }
}
