package com.genersoft.iot.vmp.jt1078.bean.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 单独通道视频
 */
public class JTAloneChanel implements JTDeviceSubConfig{

    /**
     * 逻辑通道号
     */
    private int logicChannelId;

    /**
     * 实时流编码模式
     * 0:CBR( 固定码率) ;
     * 1:VBR( 可变码率) ;
     * 2:ABR( 平均码率) ;
     * 100 ~ 127:自定义
     */
    private int liveStreamCodeRateType;

    /**
     * 实时流分辨率
     * 0:QCIF;
     * 1:CIF;
     * 2:WCIF;
     * 3:D1;
     * 4:WD1;
     * 5:720P;
     * 6:1 080P;
     * 100 ~ 127:自定义
     */
    private int liveStreamResolving;

    /**
     * 实时流关键帧间隔, 范围(1 ~ 1 000) 帧
     */
    private int liveStreamIInterval;

    /**
     * 实时流目标帧率,范围(1 ~ 120) 帧 / s
     */
    private int liveStreamFrameRate;

    /**
     * 实时流目标码率,单位为千位每秒( kbps)
     */
    private long liveStreamCodeRate;


    /**
     * 存储流编码模式
     * 0:CBR( 固定码率) ;
     * 1:VBR( 可变码率) ;
     * 2:ABR( 平均码率) ;
     * 100 ~ 127:自定义
     */
    private int storageStreamCodeRateType;

    /**
     * 存储流分辨率
     * 0:QCIF;
     * 1:CIF;
     * 2:WCIF;
     * 3:D1;
     * 4:WD1;
     * 5:720P;
     * 6:1 080P;
     * 100 ~ 127:自定义
     */
    private int storageStreamResolving;

    /**
     * 存储流关键帧间隔, 范围(1 ~ 1 000) 帧
     */
    private int storageStreamIInterval;

    /**
     * 存储流目标帧率,范围(1 ~ 120) 帧 / s
     */
    private int storageStreamFrameRate;

    /**
     * 存储流目标码率,单位为千位每秒( kbps)
     */
    private long storageStreamCodeRate;

    /**
     * 字幕叠加设置
     */
    private OSDConfig osd;

    public int getLogicChannelId() {
        return logicChannelId;
    }

    public void setLogicChannelId(int logicChannelId) {
        this.logicChannelId = logicChannelId;
    }

    public int getLiveStreamCodeRateType() {
        return liveStreamCodeRateType;
    }

    public void setLiveStreamCodeRateType(int liveStreamCodeRateType) {
        this.liveStreamCodeRateType = liveStreamCodeRateType;
    }

    public int getLiveStreamResolving() {
        return liveStreamResolving;
    }

    public void setLiveStreamResolving(int liveStreamResolving) {
        this.liveStreamResolving = liveStreamResolving;
    }

    public int getLiveStreamIInterval() {
        return liveStreamIInterval;
    }

    public void setLiveStreamIInterval(int liveStreamIInterval) {
        this.liveStreamIInterval = liveStreamIInterval;
    }

    public int getLiveStreamFrameRate() {
        return liveStreamFrameRate;
    }

    public void setLiveStreamFrameRate(int liveStreamFrameRate) {
        this.liveStreamFrameRate = liveStreamFrameRate;
    }

    public long getLiveStreamCodeRate() {
        return liveStreamCodeRate;
    }

    public void setLiveStreamCodeRate(long liveStreamCodeRate) {
        this.liveStreamCodeRate = liveStreamCodeRate;
    }

    public int getStorageStreamCodeRateType() {
        return storageStreamCodeRateType;
    }

    public void setStorageStreamCodeRateType(int storageStreamCodeRateType) {
        this.storageStreamCodeRateType = storageStreamCodeRateType;
    }

    public int getStorageStreamResolving() {
        return storageStreamResolving;
    }

    public void setStorageStreamResolving(int storageStreamResolving) {
        this.storageStreamResolving = storageStreamResolving;
    }

    public int getStorageStreamIInterval() {
        return storageStreamIInterval;
    }

    public void setStorageStreamIInterval(int storageStreamIInterval) {
        this.storageStreamIInterval = storageStreamIInterval;
    }

    public int getStorageStreamFrameRate() {
        return storageStreamFrameRate;
    }

    public void setStorageStreamFrameRate(int storageStreamFrameRate) {
        this.storageStreamFrameRate = storageStreamFrameRate;
    }

    public long getStorageStreamCodeRate() {
        return storageStreamCodeRate;
    }

    public void setStorageStreamCodeRate(long storageStreamCodeRate) {
        this.storageStreamCodeRate = storageStreamCodeRate;
    }

    public OSDConfig getOsd() {
        return osd;
    }

    public void setOsd(OSDConfig osd) {
        this.osd = osd;
    }

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(logicChannelId);
        byteBuf.writeByte(liveStreamCodeRateType);
        byteBuf.writeByte(liveStreamResolving);
        byteBuf.writeShort((short)(liveStreamIInterval & 0xffff));
        byteBuf.writeByte(liveStreamFrameRate);
        byteBuf.writeInt((int) (liveStreamCodeRate & 0xffffffffL));

        byteBuf.writeByte(storageStreamCodeRateType);
        byteBuf.writeByte(storageStreamResolving);
        byteBuf.writeShort((short)(storageStreamIInterval & 0xffff));
        byteBuf.writeByte(storageStreamFrameRate);
        byteBuf.writeInt((int) (storageStreamCodeRate & 0xffffffffL));
        byteBuf.writeBytes(osd.encode());
        return byteBuf;
    }

    public static JTAloneChanel decode(ByteBuf buf) {
        JTAloneChanel jtAloneChanel = new JTAloneChanel();
        jtAloneChanel.setLogicChannelId(buf.readByte());
        jtAloneChanel.setLiveStreamCodeRateType(buf.readByte());
        jtAloneChanel.setLiveStreamResolving(buf.readByte());
        jtAloneChanel.setLiveStreamIInterval(buf.readUnsignedShort());
        jtAloneChanel.setLiveStreamFrameRate(buf.readByte());
        jtAloneChanel.setLiveStreamCodeRate(buf.readUnsignedInt());

        jtAloneChanel.setStorageStreamCodeRateType(buf.readByte());
        jtAloneChanel.setStorageStreamResolving(buf.readByte());
        jtAloneChanel.setStorageStreamIInterval(buf.readUnsignedShort());
        jtAloneChanel.setStorageStreamFrameRate(buf.readByte());
        jtAloneChanel.setStorageStreamCodeRate(buf.readUnsignedInt());
        jtAloneChanel.setOsd(OSDConfig.decode(buf));
        return null;
    }
}
