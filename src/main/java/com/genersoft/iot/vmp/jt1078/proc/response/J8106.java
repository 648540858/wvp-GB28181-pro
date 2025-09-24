package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import lombok.Getter;

import java.util.Arrays;

/**
 * 查询指定终端参数
 */
@Getter
@MsgId(id = "8106")
public class J8106 extends Rs {

    private long[] params;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(params.length);
        for (long param : params) {
            buffer.writeInt((int) param);
        }
        return buffer;
    }

    public void setParams(long[] params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return "J8106{" +
                "params=" + Arrays.toString(params) +
                '}';
    }
}
