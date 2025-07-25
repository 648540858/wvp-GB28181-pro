package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.jt1078.util.BCDUtil;
import com.genersoft.iot.vmp.utils.DateUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "存储多媒体数据")
public class JTQueryMediaDataCommand {

    @Schema(description = "多媒体类型: 0：图像；1：音频；2：视频")
    private int type;

    @Schema(description = "通道 ID, 0 表示检索该媒体类型的所有通道")
    private int chanelId;

    @Schema(description = "事件项编码: 0：平台下发指令；1：定时动作；2：抢劫报警触发；3：碰 撞侧翻报警触发；其他保留")
    private int event;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;

    @Schema(description = "删除标志, 0:保留；1:删除, 存储多媒体数据上传命令中使用")
    private Integer delete;


    public ByteBuf decode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(type);
        byteBuf.writeByte(chanelId);
        byteBuf.writeByte(event);
        if (startTime == null) {
            byteBuf.writeBytes(BCDUtil.strToBcd("000000000000"));
        }else {
            byteBuf.writeBytes(BCDUtil.strToBcd(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(startTime)));
        }
        if (endTime == null) {
            byteBuf.writeBytes(BCDUtil.strToBcd("000000000000"));
        }else {
            byteBuf.writeBytes(BCDUtil.strToBcd(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(endTime)));
        }
        if (delete != null) {
            byteBuf.writeByte(delete);
        }
        return byteBuf;
    }

    @Override
    public String toString() {
        return "JTQueryMediaDataCommand{" +
                "type=" + type +
                ", chanelId=" + chanelId +
                ", event=" + event +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", delete='" + delete + '\'' +
                '}';
    }
}
