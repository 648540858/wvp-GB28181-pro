package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.common.InviteSessionType;
import lombok.Data;

import java.util.Objects;

/** 仅保存录像对话协议终止的身份凭据，不包含设备密码、信令路由或活动事务。 */
@Data
public class RecordDialogCloseReceipt {
    private String callId;
    private String transactionCallId;
    private String deviceId;
    private Integer channelId;
    private InviteSessionType type;
    private String ssrc;
    private String app;
    private String stream;
    private String mediaServerId;
    private String fromTag;
    private String toTag;
    private String viaBranch;
    private long confirmedAt;
    private Integer responseCode;

    /** 从原对话快照复制允许持久化的身份字段，避免把终止路由继续暴露给查询方。 */
    public static RecordDialogCloseReceipt fromSnapshot(SsrcTransaction transaction, long confirmedAt,
                                                        Integer responseCode) {
        SipTransactionInfo sip = transaction.getSipTransactionInfo();
        RecordDialogCloseReceipt receipt = new RecordDialogCloseReceipt();
        receipt.setCallId(sip == null || sip.getCallId() == null ? transaction.getCallId() : sip.getCallId());
        receipt.setTransactionCallId(transaction.getCallId());
        receipt.setDeviceId(transaction.getDeviceId());
        receipt.setChannelId(transaction.getChannelId());
        receipt.setType(transaction.getType());
        receipt.setSsrc(transaction.getSsrc());
        receipt.setApp(transaction.getApp());
        receipt.setStream(transaction.getStream());
        receipt.setMediaServerId(transaction.getMediaServerId());
        receipt.setFromTag(sip == null ? null : sip.getFromTag());
        receipt.setToTag(sip == null ? null : sip.getToTag());
        receipt.setViaBranch(sip == null ? null : sip.getViaBranch());
        receipt.setConfirmedAt(confirmedAt);
        receipt.setResponseCode(responseCode);
        return receipt;
    }

    /** 缺少原Dialog身份的快照不能凭空索引或空标签推定设备已确认终止。 */
    public static boolean hasCompleteIdentity(SsrcTransaction transaction) {
        if (transaction == null || transaction.getPlatformId() != null
                || (transaction.getType() != InviteSessionType.DOWNLOAD
                && transaction.getType() != InviteSessionType.PLAYBACK)) {
            return false;
        }
        SipTransactionInfo sip = transaction.getSipTransactionInfo();
        return sip != null && hasText(sip.getCallId()) && hasText(sip.getFromTag())
                && hasText(sip.getToTag()) && hasText(transaction.getDeviceId())
                && transaction.getChannelId() != null && hasText(transaction.getSsrc())
                && hasText(transaction.getApp()) && hasText(transaction.getStream())
                && hasText(transaction.getMediaServerId());
    }

    /** 所有身份字段与协议结果都匹配才接受，防止旧Call-ID凭据串到新轮次。 */
    public boolean matches(SsrcTransaction transaction) {
        if (!hasCompleteIdentity(transaction) || confirmedAt <= 0 || responseCode == null
                || !((responseCode >= 200 && responseCode < 300) || responseCode == 481)) {
            return false;
        }
        SipTransactionInfo sip = transaction.getSipTransactionInfo();
        return Objects.equals(callId, sip.getCallId())
                && Objects.equals(transactionCallId, transaction.getCallId())
                && Objects.equals(deviceId, transaction.getDeviceId())
                && Objects.equals(channelId, transaction.getChannelId())
                && type == transaction.getType() && Objects.equals(ssrc, transaction.getSsrc())
                && Objects.equals(app, transaction.getApp()) && Objects.equals(stream, transaction.getStream())
                && Objects.equals(mediaServerId, transaction.getMediaServerId())
                && Objects.equals(fromTag, sip.getFromTag()) && Objects.equals(toTag, sip.getToTag())
                && Objects.equals(viaBranch, sip.getViaBranch());
    }

    /** 空白标识不具备可供确认的Dialog归属。 */
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
