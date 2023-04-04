package com.genersoft.iot.vmp.gb28181.bean;

public class ParentPlatformCatch {

    private String id;

    /**
     * 心跳未回复次数
     */
    private int keepAliveReply;

    // 注册未回复次数
    private int registerAliveReply;

    private String callId;

    private ParentPlatform parentPlatform;

    private SipTransactionInfo sipTransactionInfo;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getKeepAliveReply() {
        return keepAliveReply;
    }

    public void setKeepAliveReply(int keepAliveReply) {
        this.keepAliveReply = keepAliveReply;
    }

    public int getRegisterAliveReply() {
        return registerAliveReply;
    }

    public void setRegisterAliveReply(int registerAliveReply) {
        this.registerAliveReply = registerAliveReply;
    }

    public ParentPlatform getParentPlatform() {
        return parentPlatform;
    }

    public void setParentPlatform(ParentPlatform parentPlatform) {
        this.parentPlatform = parentPlatform;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public SipTransactionInfo getSipTransactionInfo() {
        return sipTransactionInfo;
    }

    public void setSipTransactionInfo(SipTransactionInfo sipTransactionInfo) {
        this.sipTransactionInfo = sipTransactionInfo;
    }
}
