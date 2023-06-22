package com.genersoft.iot.vmp.common;

import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 记录每次发送invite消息的状态
 */
public class InviteInfo {

    private String deviceId;

    private String channelId;

    private String stream;

    private SSRCInfo ssrcInfo;

    private String receiveIp;

    private Integer receivePort;

    private String streamMode;

    private InviteSessionType type;

    private InviteSessionStatus status;

    private StreamInfo streamInfo;


    public static InviteInfo getInviteInfo(String deviceId, String channelId, String stream, SSRCInfo ssrcInfo,
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

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public InviteSessionType getType() {
        return type;
    }

    public void setType(InviteSessionType type) {
        this.type = type;
    }

    public InviteSessionStatus getStatus() {
        return status;
    }

    public void setStatus(InviteSessionStatus status) {
        this.status = status;
    }

    public StreamInfo getStreamInfo() {
        return streamInfo;
    }

    public void setStreamInfo(StreamInfo streamInfo) {
        this.streamInfo = streamInfo;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public SSRCInfo getSsrcInfo() {
        return ssrcInfo;
    }

    public void setSsrcInfo(SSRCInfo ssrcInfo) {
        this.ssrcInfo = ssrcInfo;
    }

    public String getReceiveIp() {
        return receiveIp;
    }

    public void setReceiveIp(String receiveIp) {
        this.receiveIp = receiveIp;
    }

    public Integer getReceivePort() {
        return receivePort;
    }

    public void setReceivePort(Integer receivePort) {
        this.receivePort = receivePort;
    }

    public String getStreamMode() {
        return streamMode;
    }

    public void setStreamMode(String streamMode) {
        this.streamMode = streamMode;
    }


    /*=========================设备主子码流逻辑START====================*/
    @Schema(description = "是否为子码流(true-是，false-主码流)")
    private boolean subStream;

    public boolean isSubStream() {
        return subStream;
    }

    public void setSubStream(boolean subStream) {
        this.subStream = subStream;
    }




}
