package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * 数据下行透传
 */
@Setter
@Getter
@MsgId(id = "8900")
public class J8900 extends Rs {

    /**
     * 透传消息类型, 0x00: GNSS 模块详细定位数据, 0X0B: 道路运输证 IC卡信息, 0X41: 串口1 透传, 0X42: 串口2 透传, 0XF0 ~ 0XFF: 用户自定义透传
     */
    private Integer type;

    /**
     * 透传消息内容
     */
    private byte[] content;

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(type);
        byteBuf.writeBytes(content);
        return byteBuf;
    }

}
