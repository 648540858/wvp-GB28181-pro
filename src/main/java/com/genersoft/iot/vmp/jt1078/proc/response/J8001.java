package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

/**
 * @author QingtaiJiang
 * @date 2023/4/27 18:48
 * @email qingtaij@163.com
 */
@MsgId(id = "8001")
public class J8001 extends Rs {
    public static final Integer SUCCESS = 0;

    Integer respNo;
    String respId;
    Integer result;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeShort(respNo);
        buffer.writeBytes(ByteBufUtil.decodeHexDump(respId));
        buffer.writeByte(result);

        return buffer;
    }


    public void setRespNo(Integer respNo) {
        this.respNo = respNo;
    }

    public void setRespId(String respId) {
        this.respId = respId;
    }

    public void setResult(Integer result) {
        this.result = result;
    }
}
