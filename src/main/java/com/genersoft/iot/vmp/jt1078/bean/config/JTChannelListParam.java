package com.genersoft.iot.vmp.jt1078.bean.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 音视频通道列表设置
 */
@Setter
@Getter
public class JTChannelListParam implements JTDeviceSubConfig{

    /**
     * 音视频通道总数
     */
    private int videoAndAudioCount;

    /**
     * 音频通道总数
     */
    private int audioCount;

    /**
     * 视频通道总数
     */
    private int videoCount;

    private List<JTChanel> chanelList;


    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(videoAndAudioCount);
        byteBuf.writeByte(audioCount);
        byteBuf.writeByte(videoCount);
        for (JTChanel jtChanel : chanelList) {
            byteBuf.writeBytes(jtChanel.encode());
        }
        return byteBuf;
    }

    public static JTChannelListParam decode(ByteBuf byteBuf) {
        JTChannelListParam channelListParam = new JTChannelListParam();
        channelListParam.setVideoAndAudioCount(byteBuf.readUnsignedByte());
        channelListParam.setAudioCount(byteBuf.readUnsignedByte());
        channelListParam.setVideoCount(byteBuf.readUnsignedByte());
        int total = channelListParam.getVideoAndAudioCount() + channelListParam.getVideoCount() + channelListParam.getAudioCount();
        List<JTChanel> chanelList = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            chanelList.add(JTChanel.decode(byteBuf));
        }
        channelListParam.setChanelList(chanelList);
        return channelListParam;
    }
}
