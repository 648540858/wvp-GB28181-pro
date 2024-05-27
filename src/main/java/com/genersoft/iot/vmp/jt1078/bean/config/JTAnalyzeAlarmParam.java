package com.genersoft.iot.vmp.jt1078.bean.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 视频分析报警参数
 */
public class JTAnalyzeAlarmParam implements JTDeviceSubConfig{

    /**
     * 车辆核载人数
     */
    private int numberForPeople;


    /**
     * 疲劳程度阈值
     */
    private int fatigueThreshold;


    public int getNumberForPeople() {
        return numberForPeople;
    }

    public void setNumberForPeople(int numberForPeople) {
        this.numberForPeople = numberForPeople;
    }

    public int getFatigueThreshold() {
        return fatigueThreshold;
    }

    public void setFatigueThreshold(int fatigueThreshold) {
        this.fatigueThreshold = fatigueThreshold;
    }

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(numberForPeople);
        byteBuf.writeByte(fatigueThreshold);
        return byteBuf;
    }

    public static JTAnalyzeAlarmParam decode(ByteBuf byteBuf) {
        JTAnalyzeAlarmParam analyzeAlarmParam = new JTAnalyzeAlarmParam();
        analyzeAlarmParam.setNumberForPeople(byteBuf.readUnsignedByte());
        analyzeAlarmParam.setFatigueThreshold(byteBuf.readUnsignedByte());
        return analyzeAlarmParam;
    }
}
