package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTTextSign;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.Charset;

/**
 * 电话回拨
 */
@Setter
@Getter
@MsgId(id = "8400")
public class J8400 extends Rs {

    /**
     * 标志， 0'普通通话,1'监听
     */
    private int sign;

    /**
     * 电话号码
     */
    private String phoneNumber;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(sign);
        buffer.writeCharSequence(phoneNumber, Charset.forName("GBK"));
        return buffer;
    }

}
