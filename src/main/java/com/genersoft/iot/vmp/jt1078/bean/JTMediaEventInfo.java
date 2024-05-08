package com.genersoft.iot.vmp.jt1078.bean;

import io.netty.buffer.ByteBuf;
import io.swagger.v3.oas.annotations.media.Schema;

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

    public static JTMediaEventInfo decode(ByteBuf buf) {
        JTMediaEventInfo jtMediaEventInfo = new JTMediaEventInfo();
        jtMediaEventInfo.setId(buf.readUnsignedInt());
        jtMediaEventInfo.setType(buf.readUnsignedByte());
        jtMediaEventInfo.setCode(buf.readUnsignedByte());
        jtMediaEventInfo.setEventCode(buf.readUnsignedByte());
        jtMediaEventInfo.setChannelId(buf.readUnsignedByte());
        return jtMediaEventInfo;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getEventCode() {
        return eventCode;
    }

    public void setEventCode(int eventCode) {
        this.eventCode = eventCode;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    @Override
    public String toString() {
        return "JTMediaEventInfo{" +
                "id=" + id +
                ", type=" + type +
                ", code=" + code +
                ", eventCode=" + eventCode +
                ", channelId=" + channelId +
                '}';
    }
}
