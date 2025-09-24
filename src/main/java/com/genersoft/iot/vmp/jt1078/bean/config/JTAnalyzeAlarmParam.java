package com.genersoft.iot.vmp.jt1078.bean.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * 视频分析报警参数
 */
@Setter
@Getter
public class JTAnalyzeAlarmParam implements JTDeviceSubConfig{

    /**
     * 车辆核载人数
     */
    private int numberForPeople;


    /**
     * 疲劳程度阈值
     */
    private int fatigueThreshold;


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
