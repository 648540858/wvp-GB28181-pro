package com.genersoft.iot.vmp.gb28181.bean;

/**
 * 语音广播状态
 * @author lin
 */
public enum AudioBroadcastCatchStatus {

    // 发送语音广播消息等待对方回复语音广播
    Ready,
    // 收到回复等待invite消息
    WaiteInvite,
    // 收到invite消息
    Ok,
}
