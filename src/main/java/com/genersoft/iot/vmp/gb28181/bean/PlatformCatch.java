package com.genersoft.iot.vmp.gb28181.bean;

import lombok.Data;

@Data
public class PlatformCatch {

    private String id;

    /**
     * 心跳未回复次数
     */
    private int keepAliveReply;

    // 注册未回复次数
    private int registerAliveReply;

    private String callId;

    private Platform parentPlatform;

    private SipTransactionInfo sipTransactionInfo;

}
