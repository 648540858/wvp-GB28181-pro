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


    public static InviteInfo getInviteInfo(String deviceId, Integer channelId, String stream, SSRCInfo ssrcInfo,
                                           String receiveIp, Integer receivePort, String streamMode,
                                           InviteSessionType type, InviteSessionStatus status) {
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
        return inviteInfo;
    }

}
