package com.genersoft.iot.vmp.common;

/**
 * 标识invite消息发出后的各个状态，
 * 收到ok钱停止invite发送cancel，
 * 收到200ok后发送BYE停止invite
 */
public enum InviteSessionStatus {
    ready,
    ok,
}
