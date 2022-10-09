package com.genersoft.iot.vmp.conf.exception;

/**
 * @author lin
 */
public class SsrcTransactionNotFoundException extends Exception{
    private String deviceId;
    private String channelId;
    private String callId;
    private String stream;



    public SsrcTransactionNotFoundException(String deviceId, String channelId, String callId, String stream) {
        this.deviceId = deviceId;
        this.channelId = channelId;
        this.callId = callId;
        this.stream = stream;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getCallId() {
        return callId;
    }

    public String getStream() {
        return stream;
    }

    @Override
    public String getMessage() {
        StringBuffer msg = new StringBuffer();
        msg.append(String.format("缓存事务信息未找到，device：%s channel: %s ",  deviceId, channelId));
        if (callId != null) {
            msg.append(",callId: " + callId);
        }
        if (stream != null) {
            msg.append(",stream: " + stream);
        }
        return msg.toString();
    }
}
