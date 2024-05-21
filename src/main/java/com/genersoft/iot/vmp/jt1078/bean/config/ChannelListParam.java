package com.genersoft.iot.vmp.jt1078.bean.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.List;

/**
 * 音视频通道列表设置
 */
public class ChannelListParam implements JTDeviceSubConfig{

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


    public int getVideoAndAudioCount() {
        return videoAndAudioCount;
    }

    public void setVideoAndAudioCount(int videoAndAudioCount) {
        this.videoAndAudioCount = videoAndAudioCount;
    }

    public int getAudioCount() {
        return audioCount;
    }

    public void setAudioCount(int audioCount) {
        this.audioCount = audioCount;
    }

    public int getVideoCount() {
        return videoCount;
    }

    public void setVideoCount(int videoCount) {
        this.videoCount = videoCount;
    }

    public List<JTChanel> getChanelList() {
        return chanelList;
    }

    public void setChanelList(List<JTChanel> chanelList) {
        this.chanelList = chanelList;
    }

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

    public static ChannelListParam decode(ByteBuf byteBuf) {
        ChannelListParam channelListParam = new ChannelListParam();
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
