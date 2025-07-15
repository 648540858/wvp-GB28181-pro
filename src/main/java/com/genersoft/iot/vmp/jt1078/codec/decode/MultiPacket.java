package com.genersoft.iot.vmp.jt1078.codec.decode;

import com.genersoft.iot.vmp.jt1078.proc.Header;
import io.netty.buffer.ByteBuf;
import lombok.Data;

/**
 * 分包消息
 */
@Data
public class MultiPacket {

    /**
     * 消息头
     */
    private Header header;

    /**
     * 包序号
     */
    private Integer number;

    /**
     * 分包数量
     */
    private Integer count;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 消息内容
     */
    private ByteBuf byteBuf;

    /**
     * 消息内容
     */
    private String bufStr;

    public static MultiPacket getInstance(Header header, Integer number, Integer count, ByteBuf byteBuf) {
        if (byteBuf == null) {
            System.err.println("MultiPacket error byteBuf is null");
        }
        MultiPacket multiPacket = new MultiPacket();
        multiPacket.setHeader(header);
        multiPacket.setNumber(number);
        multiPacket.setCount(count);
        multiPacket.setCreateTime(System.currentTimeMillis());
        multiPacket.setByteBuf(byteBuf);
        return multiPacket;
    }

    @Override
    public String toString() {
        return "MultiPacket{" +
                "消息头=" + header +
                ", 包序号=" + number +
                ", 分包数量=" + count +
                '}';
    }
}
