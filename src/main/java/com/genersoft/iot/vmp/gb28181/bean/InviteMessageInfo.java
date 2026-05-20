package com.genersoft.iot.vmp.gb28181.bean;

import lombok.Data;

// 从INVITE消息中解析需要的信息
@Data
public class InviteMessageInfo {
    private String requesterId;
    private String targetChannelId;
    private String sourceChannelId;
    private String sessionName;
    private String ssrc;
    private boolean tcp;
    private boolean tcpActive;
    private String callId;
    private Long startTime;
    private Long stopTime;
    private String downloadSpeed;
    private String ip;
    private int port;

}
