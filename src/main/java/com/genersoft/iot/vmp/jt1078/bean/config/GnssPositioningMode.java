package com.genersoft.iot.vmp.jt1078.bean.config;

/**
 * GNSS 定位模式
 */
public class GnssPositioningMode implements JTDeviceSubConfig{

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

    public boolean isGps() {
        return gps;
    }

    public void setGps(boolean gps) {
        this.gps = gps;
    }

    public boolean isBeidou() {
        return beidou;
    }

    public void setBeidou(boolean beidou) {
        this.beidou = beidou;
    }

    public boolean isGlonass() {
        return glonass;
    }

    public void setGlonass(boolean glonass) {
        this.glonass = glonass;
    }

    public boolean isGaLiLeo() {
        return gaLiLeo;
    }

    public void setGaLiLeo(boolean gaLiLeo) {
        this.gaLiLeo = gaLiLeo;
    }

    @Override
    public byte[] encode() {
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
        return bytes;
    }
}
