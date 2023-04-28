package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

/**
 * 平台下发远程录像回放控制
 *
 * @author QingtaiJiang
 * @date 2023/4/28 10:37
 * @email qingtaij@163.com
 */
@MsgId(id = "9202")
public class J9202 extends Rs {
    // 逻辑通道号
    private int channel;

    // 回放控制：0.开始回放 1.暂停回放 2.结束回放 3.快进回放 4.关键帧快退回放 5.拖动回放 6.关键帧播放
    private int playbackType;

    // 快进或快退倍数：0.无效 1.1倍 2.2倍 3.4倍 4.8倍 5.16倍 (回放控制为3和4时,此字段内容有效,否则置0)
    private int playbackSpeed;

    // 拖动回放位置(YYMMDDHHMMSS,回放控制为5时,此字段有效)
    private String playbackTime;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(channel);
        buffer.writeByte(playbackType);
        buffer.writeByte(playbackSpeed);
        buffer.writeBytes(ByteBufUtil.decodeHexDump(playbackTime));
        return buffer;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
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

    public String getPlaybackTime() {
        return playbackTime;
    }

    public void setPlaybackTime(String playbackTime) {
        this.playbackTime = playbackTime;
    }

    @Override
    public String toString() {
        return "J9202{" +
                "channel=" + channel +
                ", playbackType=" + playbackType +
                ", playbackSpeed=" + playbackSpeed +
                ", playbackTime='" + playbackTime + '\'' +
                '}';
    }
}
