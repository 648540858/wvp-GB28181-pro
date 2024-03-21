package com.genersoft.iot.vmp.service.bean;

import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;


public class RequestStopPushStreamMsg {


    private SendRtpItem sendRtpItem;


    private String platformName;


    private int platFormIndex;

    public SendRtpItem getSendRtpItem() {
        return sendRtpItem;
    }

    public void setSendRtpItem(SendRtpItem sendRtpItem) {
        this.sendRtpItem = sendRtpItem;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }


    public int getPlatFormIndex() {
        return platFormIndex;
    }

    public void setPlatFormIndex(int platFormIndex) {
        this.platFormIndex = platFormIndex;
    }

    public static RequestStopPushStreamMsg getInstance(SendRtpItem sendRtpItem, String platformName, int platFormIndex) {
        RequestStopPushStreamMsg streamMsg = new RequestStopPushStreamMsg();
        streamMsg.setSendRtpItem(sendRtpItem);
        streamMsg.setPlatformName(platformName);
        streamMsg.setPlatFormIndex(platFormIndex);
        return streamMsg;
    }
}
