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

    /**
     * 最早可回收时间（绝对毫秒时间戳）。
     *
     * <p>为 null 表示该条目不受定时清理约束（例如正在进行的点播/回放/下载）。
     * 录像下载在流离开后仍需要保留一段时间，等待 on_record_mp4 写入下载地址，
     * 此时把本字段设置为「当前时间 + 保留窗口」，由 {@code InviteStreamServiceImpl.execute()}
     * 负责到期回收，避免 WVP 重启导致延时任务丢失而永久泄漏。</p>
     */
    private Long cleanTime;


    public static InviteInfo getInviteInfo(String deviceId, Integer channelId, String stream, SSRCInfo ssrcInfo, String mediaServerId,
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
        inviteInfo.setMediaServerId(mediaServerId);
        return inviteInfo;
    }

}
