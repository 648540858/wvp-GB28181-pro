package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

/**
 * 回放请求
 *
 * @author QingtaiJiang
 * @date 2023/4/28 10:37
 * @email qingtaij@163.com
 */
@MsgId(id = "9201")
public class J9201 extends Rs {
    // 服务器IP地址
    private String ip;

    // 实时视频服务器TCP端口号
    private int tcpPort;

    // 实时视频服务器UDP端口号
    private int udpPort;

    // 逻辑通道号
    private int channel;

    // 音视频资源类型：0.音视频 1.音频 2.视频 3.视频或音视频
    private int type;

    // 码流类型：0.所有码流 1.主码流 2.子码流(如果此通道只传输音频,此字段置0)
    private int rate;

    // 存储器类型：0.所有存储器 1.主存储器 2.灾备存储器"
    private int storageType;

    // 回放方式：0.正常回放 1.快进回放 2.关键帧快退回放 3.关键帧播放 4.单帧上传
    private int playbackType;

    // 快进或快退倍数：0.无效 1.1倍 2.2倍 3.4倍 4.8倍 5.16倍 (回放控制为1和2时,此字段内容有效,否则置0)
    private int playbackSpeed;

    // 开始时间YYMMDDHHMMSS,回放方式为4时,该字段表示单帧上传时间
    private String startTime;

    // 结束时间YYMMDDHHMMSS,回放方式为4时,该字段无效,为0表示一直回放
    private String endTime;

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
        buffer.writeByte(storageType);
        buffer.writeByte(playbackType);
        buffer.writeByte(playbackSpeed);
        buffer.writeBytes(ByteBufUtil.decodeHexDump(startTime));
        buffer.writeBytes(ByteBufUtil.decodeHexDump(endTime));
        return buffer;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public void setTcpPort(int tcpPort) {
        this.tcpPort = tcpPort;
    }

    public int getUdpPort() {
        return udpPort;
    }

    public void setUdpPort(int udpPort) {
        this.udpPort = udpPort;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    public int getStorageType() {
        return storageType;
    }

    public void setStorageType(int storageType) {
        this.storageType = storageType;
    }

    public int getPlaybackType() {
        return playbackType;
    }

    public void setPlaybackType(int playbackType) {
        this.playbackType = playbackType;
    }

    public int getPlaybackSpeed() {
        return playbackSpeed;
    }

    public void setPlaybackSpeed(int playbackSpeed) {
        this.playbackSpeed = playbackSpeed;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        return "J9201{" +
                "ip='" + ip + '\'' +
                ", tcpPort=" + tcpPort +
                ", udpPort=" + udpPort +
                ", channel=" + channel +
                ", type=" + type +
                ", rate=" + rate +
                ", storageType=" + storageType +
                ", playbackType=" + playbackType +
                ", playbackSpeed=" + playbackSpeed +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                '}';
    }
}
