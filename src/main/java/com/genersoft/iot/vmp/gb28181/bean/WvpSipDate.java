package com.genersoft.iot.vmp.gb28181.bean;

import gov.nist.core.InternalErrorHandler;
import gov.nist.javax.sip.header.SIPDate;

import java.util.*;

/**
 * 重写jain sip的SIPDate解决与国标时间格式不一致的问题
 */
public class WvpSipDate extends SIPDate {

    private Calendar javaCal;

    public WvpSipDate(long timeMillis) {
        this.javaCal = new GregorianCalendar(TimeZone.getDefault(), Locale.getDefault());
        Date date = new Date(timeMillis);
        this.javaCal.setTime(date);
        this.wkday = this.javaCal.get(7);
        switch(this.wkday) {
            case 1:
                this.sipWkDay = "Sun";
                break;
            case 2:
                this.sipWkDay = "Mon";
                break;
            case 3:
                this.sipWkDay = "Tue";
                break;
            case 4:
                this.sipWkDay = "Wed";
                break;
            case 5:
                this.sipWkDay = "Thu";
                break;
            case 6:
                this.sipWkDay = "Fri";
                break;
            case 7:
                this.sipWkDay = "Sat";
                break;
            default:
                InternalErrorHandler.handleException("No date map for wkday " + this.wkday);
        }

        this.day = this.javaCal.get(5);
        this.month = this.javaCal.get(2);
        switch(this.month) {
            case 0:
                this.sipMonth = "Jan";
                break;
            case 1:
                this.sipMonth = "Feb";
                break;
            case 2:
                this.sipMonth = "Mar";
                break;
            case 3:
                this.sipMonth = "Apr";
                break;
            case 4:
                this.sipMonth = "May";
                break;
            case 5:
                this.sipMonth = "Jun";
                break;
            case 6:
                this.sipMonth = "Jul";
                break;
            case 7:
                this.sipMonth = "Aug";
                break;
            case 8:
                this.sipMonth = "Sep";
                break;
            case 9:
                this.sipMonth = "Oct";
                break;
            case 10:
                this.sipMonth = "Nov";
                break;
            case 11:
                this.sipMonth = "Dec";
                break;
            default:
                InternalErrorHandler.handleException("No date map for month " + this.month);
        }

        this.year = this.javaCal.get(1);
        this.hour = this.javaCal.get(11);
        this.minute = this.javaCal.get(12);
        this.second = this.javaCal.get(13);
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

        int var8 = this.javaCal.get(14);
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
