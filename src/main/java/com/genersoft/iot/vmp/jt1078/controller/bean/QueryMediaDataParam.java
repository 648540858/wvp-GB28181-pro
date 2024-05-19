package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.JTQueryMediaDataCommand;
import com.genersoft.iot.vmp.jt1078.bean.JTShootingCommand;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "存储多媒体数据参数")
public class QueryMediaDataParam {

    @Schema(description = "设备")
    private String deviceId;

    @Schema(description = "多媒体 ID, 单条存储多媒体数据检索上传时有效")
    private Long mediaId;

    @Schema(description = "删除标志, 单条存储多媒体数据检索上传时有效")
    private int delete;

    @Schema(description = "存储多媒体数据参数")
    private JTQueryMediaDataCommand queryMediaDataCommand;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public JTQueryMediaDataCommand getQueryMediaDataCommand() {
        return queryMediaDataCommand;
    }

    public void setQueryMediaDataCommand(JTQueryMediaDataCommand queryMediaDataCommand) {
        this.queryMediaDataCommand = queryMediaDataCommand;
    }

    public int getDelete() {
        return delete;
    }

    public void setDelete(int delete) {
        this.delete = delete;
    }

    public Long getMediaId() {
        return mediaId;
    }

    public void setMediaId(Long mediaId) {
        this.mediaId = mediaId;
    }

    @Override
    public String toString() {
        return "QueryMediaDataParam{" +
                "deviceId='" + deviceId + '\'' +
                ", mediaId=" + mediaId +
                ", queryMediaDataCommand=" + queryMediaDataCommand +
                '}';
    }
}
