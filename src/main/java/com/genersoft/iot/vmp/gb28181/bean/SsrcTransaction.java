package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.common.InviteSessionType;
import lombok.Data;

@Data
public class SsrcTransaction {

    private String deviceId;
    private String platformId;
    private Integer channelId;
    private String callId;
    private String stream;
    private String mediaServerId;
    private String ssrc;

    private SipTransactionInfo sipTransactionInfo;

    private InviteSessionType type;

    public SsrcTransaction(String deviceId, String platformId, Integer channelId, String callId,
                           String stream, String mediaServerId, String ssrc,
                           SipTransactionInfo sipTransactionInfo, InviteSessionType type) {
        this.deviceId = deviceId;
        this.platformId = platformId;
        this.channelId = channelId;
        this.callId = callId;
        this.stream = stream;
        this.mediaServerId = mediaServerId;
        this.ssrc = ssrc;
        this.sipTransactionInfo = sipTransactionInfo;
        this.type = type;
    }

    public SsrcTransaction() {
    }
}
