package com.genersoft.iot.vmp.vmanager.cloudRecord.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 文件会话创建和位置控制的输入，禁止客户端指定磁盘路径或媒体流。 */
@Data
public class CloudRecordSessionRequest {
    @Schema(description = "创建会话必填的云录像记录ID")
    private Integer cloudRecordId;
    @Schema(description = "相对文件起点的位置，单位毫秒；创建默认0，定位和暂停必填")
    private Long positionMs;
}
