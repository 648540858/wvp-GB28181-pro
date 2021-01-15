package com.genersoft.iot.vmp.gb28181.bean;

import gov.nist.javax.sip.header.SIPDate;

/**
 * 重写jain sip的SIPDate解决与国标时间格式不一致的问题
 */
public class WvpSipDate extends SIPDate {

    public WvpSipDate(long timeMillis) {
        super(timeMillis);
    }

    @Override
    public StringBuilder encode(StringBuilder var1) {
        String var2;
        if (this.month < 9) {
            var2 = "0" + (this.month + 1);
        } else {
            var2 = "" + (this.month + 1);
        }

        String var3;
        if (this.day < 10) {
            var3 = "0" + this.day;
        } else {
            var3 = "" + this.day;
        }

        String var4;
        if (this.hour < 10) {
            var4 = "0" + this.hour;
        } else {
            var4 = "" + this.hour;
        }

        String var5;
        if (this.minute < 10) {
            var5 = "0" + this.minute;
        } else {
            var5 = "" + this.minute;
        }

        String var6;
        if (this.second < 10) {
            var6 = "0" + this.second;
        } else {
            var6 = "" + this.second;
        }

        int var8 = this.getJavaCal().get(14);
        String var7;
        if (var8 < 10) {
            var7 = "00" + var8;
        } else if (var8 < 100) {
            var7 = "0" + var8;
        } else {
            var7 = "" + var8;
        }

        return var1.append(this.year).append("-").append(var2).append("-").append(var3).append("T").append(var4).append(":").append(var5).append(":").append(var6).append(".").append(var7);
    }
}
