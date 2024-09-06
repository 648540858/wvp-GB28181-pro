package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.common.InviteSessionType;
import gov.nist.javax.sip.message.SIPResponse;
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

    public static SsrcTransaction buildForDevice(String deviceId, Integer channelId, String callId, String stream,
                                                 String ssrc, String mediaServerId, SIPResponse response, InviteSessionType type) {
        SsrcTransaction ssrcTransaction = new SsrcTransaction();
        ssrcTransaction.setDeviceId(deviceId);
        ssrcTransaction.setChannelId(channelId);
        ssrcTransaction.setCallId(callId);
        ssrcTransaction.setStream(stream);
        ssrcTransaction.setMediaServerId(mediaServerId);
        ssrcTransaction.setSsrc(ssrc);
        ssrcTransaction.setSipTransactionInfo(new SipTransactionInfo(response));
        ssrcTransaction.setType(type);
        return ssrcTransaction;
    }
    public static SsrcTransaction buildForPlatform(String platformId, Integer channelId, String callId, String stream,
                                                 String ssrc, String mediaServerId, SIPResponse response, InviteSessionType type) {
        SsrcTransaction ssrcTransaction = new SsrcTransaction();
        ssrcTransaction.setPlatformId(platformId);
        ssrcTransaction.setChannelId(channelId);
        ssrcTransaction.setCallId(callId);
        ssrcTransaction.setStream(stream);
        ssrcTransaction.setMediaServerId(mediaServerId);
        ssrcTransaction.setSsrc(ssrc);
        ssrcTransaction.setSipTransactionInfo(new SipTransactionInfo(response));
        ssrcTransaction.setType(type);
        return ssrcTransaction;
    }

    public SsrcTransaction() {
    }
}
