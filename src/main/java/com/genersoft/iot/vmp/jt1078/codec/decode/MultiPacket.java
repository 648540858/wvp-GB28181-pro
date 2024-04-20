package com.genersoft.iot.vmp.jt1078.codec.decode;

import com.genersoft.iot.vmp.jt1078.proc.Header;
import io.netty.buffer.ByteBuf;

/**
 * 分包消息
 */
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

    public static MultiPacket getInstance(Header header, Integer number, Integer count, ByteBuf byteBuf) {
        MultiPacket multiPacket = new MultiPacket();
        multiPacket.setHeader(header);
        multiPacket.setNumber(number);
        multiPacket.setCount(count);
        multiPacket.setCreateTime(System.currentTimeMillis());
        multiPacket.setByteBuf(byteBuf);
        return multiPacket;
    }

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public ByteBuf getByteBuf() {
        return byteBuf;
    }

    public void setByteBuf(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }
}
