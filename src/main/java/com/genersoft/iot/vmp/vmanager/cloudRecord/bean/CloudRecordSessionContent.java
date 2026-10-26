package com.genersoft.iot.vmp.vmanager.cloudRecord.bean;

import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 独立观看会话的安全投影，不包含录像磁盘路径或媒体节点密钥。 */
@Data
@Schema(description = "已有云录像的独立观看会话")
public class CloudRecordSessionContent {
    @Schema(description = "独立会话ID，控制请求须回到创建它的WVP实例")
    private String sessionId;
    @Schema(description = "云录像记录ID")
    private int cloudRecordId;
    @Schema(description = "会话状态：playing或paused；结束或过期后查询返回404")
    private String state;
    @Schema(description = "最近一次明确设置的位置，单位毫秒；不是播放器实时进度")
    private long positionMs;
    @Schema(description = "文件总时长，单位毫秒")
    private long durationMs;
    @Schema(description = "会话过期的Unix毫秒时间；查询不续期")
    private long expiresAt;
    @Schema(description = "持有会话的WVP实例ID")
    private String serverId;
    @Schema(description = "当前媒体地址；暂停时为空，定位和恢复后须替换播放器地址")
    private StreamContent playback;
}
