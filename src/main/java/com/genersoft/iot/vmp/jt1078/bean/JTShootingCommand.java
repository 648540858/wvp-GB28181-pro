package com.genersoft.iot.vmp.jt1078.bean;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "拍摄命令参数")
public class JTShootingCommand {

    @Schema(description = "通道 ID")
    private int chanelId;

    @Schema(description = "0:停止拍摄；0xFFFF:录像；其他:拍照张数")
    private int command;

    @Schema(description = "拍照间隔/录像时间, 单位为秒(s) ,0 表示按最小间隔拍照或一直录像")
    private int time;

    @Schema(description = "1:保存； 0:实时上传")
    private int save;

    @Schema(description = "分辨率: " +
            "0x00:最低分辨率" +
            "0x01:320 x240；" +
            "0x02:640 x480；" +
            "0x03:800 x600；" +
            "0x04:1024 x768；" +
            "0x05:176 x144；" +
            "0x06:352 x288；" +
            "0x07:704 x288；" +
            "0x08:704 x576；" +
            "0xff:最高分辨率")
    private int resolvingPower;

    @Schema(description = "图像/视频质量: 取值范围为 1 ~ 10 ,1 代表质量损失最小 ,10 表示压缩 比最大")
    private int quality;

    @Schema(description = "亮度, 0 ~ 255")
    private int brightness;

    @Schema(description = "对比度,0 ~ 127")
    private int contrastRatio;

    @Schema(description = "饱和度,0 ~ 127")
    private int saturation;

    @Schema(description = "色度,0 ~ 255")
    private int chroma;

    public ByteBuf decode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(chanelId);
        byteBuf.writeShort((short)(command & 0xffff));
        byteBuf.writeShort((short)(time & 0xffff));
        byteBuf.writeByte(save);
        byteBuf.writeByte(resolvingPower);
        byteBuf.writeByte(quality);
        byteBuf.writeByte(brightness);
        byteBuf.writeByte(contrastRatio);
        byteBuf.writeByte(saturation);
        byteBuf.writeByte(chroma);
        return byteBuf;
    }

    @Override
    public String toString() {
        return "JTShootingCommand{" +
                "chanelId=" + chanelId +
                ", command=" + command +
                ", time=" + time +
                ", save=" + save +
                ", resolvingPower=" + resolvingPower +
                ", quality=" + quality +
                ", brightness=" + brightness +
                ", contrastRatio=" + contrastRatio +
                ", saturation=" + saturation +
                ", chroma=" + chroma +
                '}';
    }
}
