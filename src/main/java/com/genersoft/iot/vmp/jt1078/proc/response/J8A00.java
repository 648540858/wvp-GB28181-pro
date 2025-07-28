package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * 平台 RSA公钥
 */
@Setter
@Getter
@MsgId(id = "8A00")
public class J8A00 extends Rs {

    /**
     * 平台 RSA公钥{e ,n}中的 e
     */
    private Long e;

    /**
     * RSA公钥{e ,n}中的 n
     */
    private byte[] n;

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeInt((int) (e & 0xffffffffL));
        byteBuf.writeBytes(n);
        return byteBuf;
    }

}
