package com.genersoft.iot.vmp.jt1078.bean;

import io.netty.buffer.ByteBuf;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "多媒体事件信息")
public class JTMediaEventInfo {

    @Schema(description = "多媒体数据 ID")
    private long id;

    @Schema(description = "多媒体类型, 0：图像；1：音频；2：视频")
    private int type;

    @Schema(description = "多媒体格式编码, 0：JPEG；1：TIF；2：MP3；3：WAV；4：WMV；其他保留")
    private int code;

    @Schema(description = "事件项编码: 0：平台下发指令；1：定时动作；2：抢劫报警触发；3：碰 撞侧翻报警触发；4：门开拍照；5：门关拍照；6：车门由开 变关 ,车速从小于20km到超过20km；7：定距拍照")
    private int eventCode;

    @Schema(description = "通道 ID")
    private int channelId;

    @Schema(description = "媒体数据")
    private byte[] mediaData;


    public static JTMediaEventInfo decode(ByteBuf buf) {
        JTMediaEventInfo jtMediaEventInfo = new JTMediaEventInfo();
        jtMediaEventInfo.setId(buf.readUnsignedInt());
        jtMediaEventInfo.setType(buf.readUnsignedByte());
        jtMediaEventInfo.setCode(buf.readUnsignedByte());
        jtMediaEventInfo.setEventCode(buf.readUnsignedByte());
        jtMediaEventInfo.setChannelId(buf.readUnsignedByte());

        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        jtMediaEventInfo.setMediaData(bytes);

        return jtMediaEventInfo;
    }

    @Override
    public String toString() {
        return "JTMediaEventInfo{" +
                "id=" + id +
                ", type=" + type +
                ", code=" + code +
                ", eventCode=" + eventCode +
                ", channelId=" + channelId +
                ", fileSize=" + mediaData.length +
                '}';
    }
}
