package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.jt1078.bean.config.JTDeviceSubConfig;
import com.genersoft.iot.vmp.jt1078.util.BCDUtil;
import com.genersoft.iot.vmp.utils.DateUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * 终端上传乘客流量
 */
@Setter
@Getter
public class JTPassengerNum implements JTDeviceSubConfig {

    /**
     * 起始时间, YY-MM-DD-HH-MM-SS( GMT + 8 时间,本标准中之后涉及的时间均采用此时区)
     */
    private String startTime;

    /**
     * 结束时间, YY-MM-DD-HH-MM-SS( GMT + 8 时间,本标准中之后涉及的时间均采用此时区)
     */
    private String endTime;

    /**
     * 上车人数
     */
    private int getIn;

    /**
     * 下车人数
     */
    private int getOut;

    @Override
    public ByteBuf encode() {
        return null;
    }

    public static JTPassengerNum decode(ByteBuf buf) {
        JTPassengerNum jtPassengerNum = new JTPassengerNum();
        byte[] bytes = new byte[6];
        buf.readBytes(bytes);
        jtPassengerNum.setStartTime(DateUtil.jt1078Toyyyy_MM_dd_HH_mm_ss(BCDUtil.transform(bytes)));
        buf.readBytes(bytes);
        jtPassengerNum.setEndTime(DateUtil.jt1078Toyyyy_MM_dd_HH_mm_ss(BCDUtil.transform(bytes)));
        jtPassengerNum.setGetIn(buf.readUnsignedShort());
        jtPassengerNum.setGetOut(buf.readUnsignedShort());
        return jtPassengerNum;
    }

    @Override
    public String toString() {
        return "终端上传乘客流量：" +
                " 时间： " + startTime + " 到 "  + endTime +
                ", 上车：" + getIn +
                ", 下车：" + getOut
                ;
    }
}
