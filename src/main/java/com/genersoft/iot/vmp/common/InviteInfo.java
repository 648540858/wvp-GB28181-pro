package com.genersoft.iot.vmp.common;

import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import lombok.Data;

/**
 * 记录每次发送invite消息的状态
 */
@Data
public class InviteInfo {

    private String deviceId;

    private Integer channelId;

    private String stream;

    private SSRCInfo ssrcInfo;

    private String receiveIp;

    private Integer receivePort;

    private String streamMode;

    private InviteSessionType type;

    private InviteSessionStatus status;

    private StreamInfo streamInfo;

    private String mediaServerId;

    private Long expirationTime;

    private Long createTime;

    private Boolean record;

    private String startTime;

    private String endTime;


    public static InviteInfo getInviteInfo(String deviceId, Integer channelId, String stream, SSRCInfo ssrcInfo, String mediaServerId,
                                           String receiveIp, Integer receivePort, String streamMode,
                                           InviteSessionType type, InviteSessionStatus status, Boolean record) {
        InviteInfo inviteInfo = new InviteInfo();
        inviteInfo.setDeviceId(deviceId);
        inviteInfo.setChannelId(channelId);
        inviteInfo.setStream(stream);
        inviteInfo.setSsrcInfo(ssrcInfo);
        inviteInfo.setReceiveIp(receiveIp);
        inviteInfo.setReceivePort(receivePort);
        inviteInfo.setStreamMode(streamMode);
        inviteInfo.setType(type);
        inviteInfo.setStatus(status);
        inviteInfo.setMediaServerId(mediaServerId);
        inviteInfo.setRecord(record);
        return inviteInfo;
    }

}
