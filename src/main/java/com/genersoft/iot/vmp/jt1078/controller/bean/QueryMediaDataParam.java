package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.JTQueryMediaDataCommand;
import com.genersoft.iot.vmp.jt1078.bean.JTShootingCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "存储多媒体数据参数")
public class QueryMediaDataParam {

    @Schema(description = "终端手机号")
    private String phoneNumber;

    @Schema(description = "多媒体 ID, 单条存储多媒体数据检索上传时有效")
    private Long mediaId;

    @Schema(description = "删除标志, 单条存储多媒体数据检索上传时有效")
    private int delete;

    @Schema(description = "存储多媒体数据参数")
    private JTQueryMediaDataCommand queryMediaDataCommand;

    @Override
    public String toString() {
        return "QueryMediaDataParam{" +
                "设备手机号='" + phoneNumber + '\'' +
                ", mediaId=" + mediaId +
                ", queryMediaDataCommand=" + queryMediaDataCommand +
                '}';
    }
}
