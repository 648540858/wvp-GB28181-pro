package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.jt1078.bean.config.JTDeviceSubConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 终端上传音视频属性
 */
public class JTMediaAttribute implements JTDeviceSubConfig {

    /**
     * 输入音频编码方式:
     * 1 G. 721
     * 2 G. 722
     * 3 G. 723
     * 4 G. 728
     * 5 G. 729
     * 6 G. 711A
     * 7 G. 711U
     * 8 G. 726
     * 9 G. 729A
     * 10 DVI4_3
     * 11 DVI4_4
     * 12 DVI4_8K
     * 13 DVI4_16K
     * 14 LPC
     * 15 S16BE_STEREO
     * 16 S16BE_MONO
     * 17 MPEGAUDIO
     * 18 LPCM
     * 19 AAC
     * 20 WMA9STD
     * 21 HEAAC
     * 22 PCM_VOICE
     * 23 PCM_AUDIO
     * 24 AACLC
     * 25 MP3
     * 26 ADPCMA
     * 27 MP4AUDIO
     * 28 AMR
     */
    private int audioEncoder;

    /**
     * 输入音频声道数
     */
    private int audioChannels;

    /**
     * 输入音频采样率:
     * 0:8 kHz;
     * 1:22. 05 kHz;
     * 2:44. 1 kHz;
     * 3:48 kHz
     */
    private int audioSamplingRate;

    /**
     * 输入音频采样位数:
     * 0:8 位;
     * 1:16 位;
     * 2:32 位
     */
    private int audioSamplingBits;

    /**
     * 音频帧长度: 范围 1 ~ 4 294 967 295
     */
    private int audioFrameLength;

    /**
     * 是否支持音频输出:
     * 0:不支持;1:支持
     */
    private int audioOutputEnable;

    /**
     * 视频编码方式:
     * 98 H. 264
     * 99 H. 265
     * 100 AVS
     * 101 SVAC
     */
    private int videoEncoder;

    /**
     * 终端支持的最大音频物理通道数量:
     */
    private int audioChannelMax;

    /**
     * 终端支持的最大视频物理通道数量:
     */
    private int videoChannelMax;

    public int getAudioEncoder() {
        return audioEncoder;
    }

    public void setAudioEncoder(int audioEncoder) {
        this.audioEncoder = audioEncoder;
    }

    public int getAudioChannels() {
        return audioChannels;
    }

    public void setAudioChannels(int audioChannels) {
        this.audioChannels = audioChannels;
    }

    public int getAudioSamplingRate() {
        return audioSamplingRate;
    }

    public void setAudioSamplingRate(int audioSamplingRate) {
        this.audioSamplingRate = audioSamplingRate;
    }

    public int getAudioFrameLength() {
        return audioFrameLength;
    }

    public void setAudioFrameLength(int audioFrameLength) {
        this.audioFrameLength = audioFrameLength;
    }

    public int getAudioOutputEnable() {
        return audioOutputEnable;
    }

    public void setAudioOutputEnable(int audioOutputEnable) {
        this.audioOutputEnable = audioOutputEnable;
    }

    public int getVideoEncoder() {
        return videoEncoder;
    }

    public void setVideoEncoder(int videoEncoder) {
        this.videoEncoder = videoEncoder;
    }

    public int getAudioChannelMax() {
        return audioChannelMax;
    }

    public void setAudioChannelMax(int audioChannelMax) {
        this.audioChannelMax = audioChannelMax;
    }

    public int getVideoChannelMax() {
        return videoChannelMax;
    }

    public void setVideoChannelMax(int videoChannelMax) {
        this.videoChannelMax = videoChannelMax;
    }

    public int getAudioSamplingBits() {
        return audioSamplingBits;
    }

    public void setAudioSamplingBits(int audioSamplingBits) {
        this.audioSamplingBits = audioSamplingBits;
    }

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(audioEncoder);
        byteBuf.writeByte(audioChannels);
        byteBuf.writeByte(audioSamplingRate);
        byteBuf.writeByte(audioSamplingBits);
        byteBuf.writeShort(audioFrameLength);
        byteBuf.writeByte(audioOutputEnable);
        byteBuf.writeByte(videoEncoder);
        byteBuf.writeByte(audioChannelMax);
        byteBuf.writeByte(videoChannelMax);
        return byteBuf;
    }

    public static JTMediaAttribute decode(ByteBuf byteBuf) {
        JTMediaAttribute jtMediaAttribute = new JTMediaAttribute();
        jtMediaAttribute.setAudioEncoder(byteBuf.readUnsignedByte());
        jtMediaAttribute.setAudioChannels(byteBuf.readUnsignedByte());
        jtMediaAttribute.setAudioSamplingRate(byteBuf.readUnsignedByte());
        jtMediaAttribute.setAudioSamplingBits(byteBuf.readUnsignedByte());
        jtMediaAttribute.setAudioFrameLength(byteBuf.readUnsignedShort());
        jtMediaAttribute.setAudioOutputEnable(byteBuf.readUnsignedByte());
        jtMediaAttribute.setVideoEncoder(byteBuf.readUnsignedByte());
        jtMediaAttribute.setAudioChannelMax(byteBuf.readUnsignedByte());
        jtMediaAttribute.setVideoChannelMax(byteBuf.readUnsignedByte());
        return jtMediaAttribute;
    }
}
