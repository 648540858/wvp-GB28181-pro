package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.Setter;

/**
 * @author QingtaiJiang
 * @date 2023/4/27 18:48
 * @email qingtaij@163.com
 */
@Setter
@MsgId(id = "8001")
public class J8001 extends Rs {

    public static final Integer SUCCESS = 0;

    public static final Integer FAIL = 1;

    public static final Integer ERROR = 2;
    public static final Integer NOT_SUPPORTED = 3;
    public static final Integer ALARM_ACK = 3;

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


}
