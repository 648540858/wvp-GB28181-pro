package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * JT 通信模块属性
 */
@Schema(description = "JT通信模块属性")
public class JTCommunicationModuleAttribute {

    private boolean gprs ;
    private boolean cdma ;
    private boolean tdScdma ;
    private boolean wcdma ;
    private boolean cdma2000  ;
    private boolean tdLte  ;
    private boolean other  ;


    public static JTCommunicationModuleAttribute getInstance(short content) {
        boolean gprs = (content & 1) == 1;
        boolean cdma = (content >>> 1 & 1) == 1;
        boolean tdScdma = (content >>> 2 & 1) == 1;
        boolean wcdma = (content >>> 3 & 1) == 1;
        boolean cdma2000 = (content >>> 4 & 1) == 1;
        boolean tdLte = (content >>> 5 & 1) == 1;
        boolean other = (content >>> 7 & 1) == 1;
        return new JTCommunicationModuleAttribute(gprs, cdma, tdScdma, wcdma, cdma2000, tdLte, other);
    }

    public JTCommunicationModuleAttribute(boolean gprs, boolean cdma, boolean tdScdma, boolean wcdma, boolean cdma2000, boolean tdLte, boolean other) {
        this.gprs = gprs;
        this.cdma = cdma;
        this.tdScdma = tdScdma;
        this.wcdma = wcdma;
        this.cdma2000 = cdma2000;
        this.tdLte = tdLte;
        this.other = other;
    }

    public boolean isGprs() {
        return gprs;
    }

    public void setGprs(boolean gprs) {
        this.gprs = gprs;
    }

    public boolean isCdma() {
        return cdma;
    }

    public void setCdma(boolean cdma) {
        this.cdma = cdma;
    }

    public boolean isTdScdma() {
        return tdScdma;
    }

    public void setTdScdma(boolean tdScdma) {
        this.tdScdma = tdScdma;
    }

    public boolean isWcdma() {
        return wcdma;
    }

    public void setWcdma(boolean wcdma) {
        this.wcdma = wcdma;
    }

    public boolean isCdma2000() {
        return cdma2000;
    }

    public void setCdma2000(boolean cdma2000) {
        this.cdma2000 = cdma2000;
    }

    public boolean isTdLte() {
        return tdLte;
    }

    public void setTdLte(boolean tdLte) {
        this.tdLte = tdLte;
    }

    public boolean isOther() {
        return other;
    }

    public void setOther(boolean other) {
        this.other = other;
    }

    @Override
    public String toString() {
        return "JCommunicationModuleAttribute{" +
                "gprs=" + gprs +
                ", cdma=" + cdma +
                ", tdScdma=" + tdScdma +
                ", wcdma=" + wcdma +
                ", cdma2000=" + cdma2000 +
                ", tdLte=" + tdLte +
                ", other=" + other +
                '}';
    }
}
