package com.genersoft.iot.vmp.conf.trace;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 国标级联消息通道链路追踪事件。
 * <p>
 * 通过 WebSocket 推送给监控页面，用于渲染完整时序图。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceEvent {

    /**
     * 事件唯一ID，用于页面去重
     */
    private String eventId;

    /**
     * 链路ID，同一条报警的所有事件共享该ID
     */
    private String traceId;

    /**
     * 事件类型，目前只有 ALARM
     */
    private String type;

    /**
     * 阶段：REDIS_IN / FORWARD / SIP_SEND / SIP_RECV / REDIS_OUT
     */
    private String stage;

    /**
     * 节点 serverId
     */
    private String serverId;

    /**
     * 节点展示名：IP:serverPort
     */
    private String nodeName;

    /**
     * SIP 地址：IP:sipPort
     */
    private String node;

    /**
     * 目标类型：platform / device
     */
    private String targetType;

    /**
     * 目标标识：平台 serverGBId/deviceGBId 或设备 deviceId
     */
    private String target;

    /**
     * 报警里的 DeviceID
     */
    private String gbId;

    /**
     * 报警方式
     */
    private Integer alarmSn;

    /**
     * 报警类型
     */
    private Integer alarmType;

    /**
     * 报警描述
     */
    private String description;

    /**
     * SIP CallId
     */
    private String sipCallId;

    /**
     * 事件时间戳
     */
    private Long timestamp;

    /**
     * 方向：IN / OUT
     */
    private String direction;

    /**
     * 错误信息（发送失败时使用）
     */
    private String error;

    /**
     * 扩展信息
     */
    private Map<String, Object> extra;
}
