package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * JT 通信模块属性
 */
@Setter
@Getter
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
