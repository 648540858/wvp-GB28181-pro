package com.genersoft.iot.vmp.jt1078.bean.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

/**
 * GNSS 定位模式
 */
@Setter
@Getter
public class JTGnssPositioningMode implements JTDeviceSubConfig{

    /**
     * GPS 定位 true: 开启， false： 关闭
     */
    private boolean gps;
    /**
     * 北斗定位 true: 开启， false： 关闭
     */
    private boolean beidou;
    /**
     * GLONASS定位 true: 开启， false： 关闭
     */
    private boolean glonass;
    /**
     * GaLiLeo定位 true: 开启， false： 关闭
     */
    private boolean gaLiLeo;

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byte[] bytes = new byte[1];
        bytes[0] = 0;
        if (gps) {
            bytes[0] = (byte)(bytes[0] | 1);
        }
        if (beidou) {
            bytes[0] = (byte)(bytes[0] | 2);
        }
        if (glonass) {
            bytes[0] = (byte)(bytes[0] | 4);
        }
        if (gaLiLeo) {
            bytes[0] = (byte)(bytes[0] | 8);
        }
        byteBuf.writeBytes(bytes);
        return byteBuf;
    }
}
