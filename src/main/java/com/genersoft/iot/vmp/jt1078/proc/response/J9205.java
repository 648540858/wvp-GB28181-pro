package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

/**
 * 查询资源列表
 *
 * @author QingtaiJiang
 * @date 2023/4/28 10:36
 * @email qingtaij@163.com
 */
@MsgId(id = "9205")
public class J9205 extends Rs {
    // 逻辑通道号
    private int channelId;

    // 开始时间YYMMDDHHMMSS,全0表示无起始时间
    private String startTime;

    // 结束时间YYMMDDHHMMSS,全0表示无终止时间
    private String endTime;

    // 报警标志
    private final int warnType = 0;

    // 音视频资源类型：0.音视频 1.音频 2.视频 3.视频或音视频
    private int mediaType;

    // 码流类型：0.所有码流 1.主码流 2.子码流
    private int streamType = 0;

    // 存储器类型：0.所有存储器 1.主存储器 2.灾备存储器
    private int storageType = 0;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();

        buffer.writeByte(channelId);
        buffer.writeBytes(ByteBufUtil.decodeHexDump(startTime));
        buffer.writeBytes(ByteBufUtil.decodeHexDump(endTime));
        buffer.writeLong(warnType);
        buffer.writeByte(mediaType);
        buffer.writeByte(streamType);
        buffer.writeByte(storageType);

        return buffer;
    }


    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public void setMediaType(int mediaType) {
        this.mediaType = mediaType;
    }

    public void setStreamType(int streamType) {
        this.streamType = streamType;
    }

    public void setStorageType(int storageType) {
        this.storageType = storageType;
    }

    public int getWarnType() {
        return warnType;
    }

    @Override
    public String toString() {
        return "J9205{" +
                "channelId=" + channelId +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", warnType=" + warnType +
                ", mediaType=" + mediaType +
                ", streamType=" + streamType +
                ", storageType=" + storageType +
                '}';
    }
}
