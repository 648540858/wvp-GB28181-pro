package com.genersoft.iot.vmp.jt1078.bean;

import io.netty.buffer.ByteBuf;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "多媒体检索项数据")
public class JTMediaDataInfo {

    @Schema(description = "多媒体数据 ID")
    private long id;

    @Schema(description = "多媒体类型, 0：图像；1：音频；2：视频")
    private int type;

    @Schema(description = "事件项编码: 0：平台下发指令；1：定时动作；2：抢劫报警触发；3：碰 撞侧翻报警触发；4：门开拍照；5：门关拍照；6：车门由开 变关 ,车速从小于20km到超过20km；7：定距拍照")
    private int eventCode;

    @Schema(description = "通道 ID")
    private int channelId;

    @Schema(description = "表示拍摄或录制的起始时刻的汇报消息")
    private JTPositionBaseInfo positionBaseInfo;

    public static JTMediaDataInfo decode(ByteBuf buf) {
        JTMediaDataInfo jtMediaEventInfo = new JTMediaDataInfo();
        jtMediaEventInfo.setId(buf.readUnsignedInt());
        jtMediaEventInfo.setType(buf.readUnsignedByte());
        jtMediaEventInfo.setChannelId(buf.readUnsignedByte());
        jtMediaEventInfo.setEventCode(buf.readUnsignedByte());
        jtMediaEventInfo.setPositionBaseInfo(JTPositionBaseInfo.decode(buf));
        return jtMediaEventInfo;
    }

    @Override
    public String toString() {
        return "JTMediaDataInfo{" +
                "id=" + id +
                ", type=" + type +
                ", eventCode=" + eventCode +
                ", channelId=" + channelId +
                ", positionBaseInfo=" + positionBaseInfo +
                '}';
    }
}
