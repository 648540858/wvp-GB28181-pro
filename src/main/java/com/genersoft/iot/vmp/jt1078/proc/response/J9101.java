package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

/**
 * 实时音视频传输请求
 *
 * @author QingtaiJiang
 * @date 2023/4/27 18:25
 * @email qingtaij@163.com
 */
@MsgId(id = "9101")
public class J9101 extends Rs {
    String ip;

    // TCP端口
    Integer tcpPort;

    // UDP端口
    Integer udpPort;

    // 逻辑通道号
    Integer channel;

    // 数据类型
    /**
     * 0：音视频，1：视频，2：双向对讲，3：监听，4：中心广播，5：透传
     */
    Integer type;

    // 码流类型
    /**
     * 0：主码流，1：子码流
     */
    Integer rate;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(ip.getBytes().length);
        buffer.writeCharSequence(ip, CharsetUtil.UTF_8);
        buffer.writeShort(tcpPort);
        buffer.writeShort(udpPort);
        buffer.writeByte(channel);
        buffer.writeByte(type);
        buffer.writeByte(rate);
        return buffer;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getTcpPort() {
        return tcpPort;
    }

    public void setTcpPort(Integer tcpPort) {
        this.tcpPort = tcpPort;
    }

    public Integer getUdpPort() {
        return udpPort;
    }

    public void setUdpPort(Integer udpPort) {
        this.udpPort = udpPort;
    }

    public Integer getChannel() {
        return channel;
    }

    public void setChannel(Integer channel) {
        this.channel = channel;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getRate() {
        return rate;
    }

    public void setRate(Integer rate) {
        this.rate = rate;
    }

    @Override
    public String toString() {
        return "J9101{" +
                "ip='" + ip + '\'' +
                ", tcpPort=" + tcpPort +
                ", udpPort=" + udpPort +
                ", channel=" + channel +
                ", type=" + type +
                ", rate=" + rate +
                '}';
    }
}
