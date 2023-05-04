package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 音视频实时传输控制
 *
 * @author QingtaiJiang
 * @date 2023/4/27 18:49
 * @email qingtaij@163.com
 */
@MsgId(id = "9102")
public class J9102 extends Rs {

    // 通道号
    Integer channel;

    // 控制指令
    /**
     * 0：关闭音视频传输指令；
     * 1：切换码流(增加暂停和继续)；
     * 2：暂停该通道所有流的发送；
     * 3：恢复暂停前流的发送，与暂停前的流类型一致；
     * 4：关闭双向对讲
     */
    Integer command;

    // 数据类型
    /**
     * 0：关闭该通道有关的音视频数据；
     * 1：只关闭该通道有关的音频，保留该通道
     * 有关的视频；
     * 2：只关闭该通道有关的视频，保留该通道
     * 有关的音频
     */
    Integer closeType;

    // 数据类型
    /**
     * 0：主码流；
     * 1：子码流
     */
    Integer streamType;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(channel);
        buffer.writeByte(command);
        buffer.writeByte(closeType);
        buffer.writeByte(streamType);
        return buffer;
    }


    public Integer getChannel() {
        return channel;
    }

    public void setChannel(Integer channel) {
        this.channel = channel;
    }

    public Integer getCommand() {
        return command;
    }

    public void setCommand(Integer command) {
        this.command = command;
    }

    public Integer getCloseType() {
        return closeType;
    }

    public void setCloseType(Integer closeType) {
        this.closeType = closeType;
    }

    public Integer getStreamType() {
        return streamType;
    }

    public void setStreamType(Integer streamType) {
        this.streamType = streamType;
    }

    @Override
    public String toString() {
        return "J9102{" +
                "channel=" + channel +
                ", command=" + command +
                ", closeType=" + closeType +
                ", streamType=" + streamType +
                '}';
    }
}
