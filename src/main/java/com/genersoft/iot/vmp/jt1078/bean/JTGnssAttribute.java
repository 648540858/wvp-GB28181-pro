package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * JT GNSS 模块属性
 */
@Schema(description = "JTGNSS 模块属性")
public class JTGnssAttribute {

    private boolean gps;

    private boolean beidou;

    private boolean glonass ;

    private boolean gaLiLeo;

    public static JTGnssAttribute getInstance(short content) {
        boolean gps = (content & 1) == 1;
        boolean beidou = (content >>> 1 & 1) == 1;
        boolean glonass = (content >>> 2 & 1) == 1;
        boolean gaLiLeo = (content >>> 3 & 1) == 1;
        return new JTGnssAttribute(gps, beidou, glonass, gaLiLeo);
    }

    public JTGnssAttribute(boolean gps, boolean beidou, boolean glonass, boolean gaLiLeo) {
        this.gps = gps;
        this.beidou = beidou;
        this.glonass = glonass;
        this.gaLiLeo = gaLiLeo;
    }

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
    public String toString() {
        return "JGnssAttribute{" +
                "gps=" + gps +
                ", beidou=" + beidou +
                ", glonass=" + glonass +
                ", gaLiLeo=" + gaLiLeo +
                '}';
    }
}
