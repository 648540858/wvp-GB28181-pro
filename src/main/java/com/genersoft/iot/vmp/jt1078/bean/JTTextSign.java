package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文本信息标志
 */
@Data
@Schema(description = "文本信息标志")
public class JTTextSign {

    @Schema(description = "1紧急,2服务,3通知")
    private int type;

    @Schema(description = "1终端显示器显示")
    private boolean terminalDisplay;

    @Schema(description = "1广告屏显示")
    private boolean adScreen;

    @Schema(description = "1终端 TTS 播读")
    private boolean tts;

    @Schema(description = "false: 中心导航信息 true CAN故障码信息")
    private boolean source;

    public byte encode(){
        byte byteSign = 0;
        byteSign |= (byte) type;
        if (terminalDisplay) {
            byteSign |= (0x1 << 2);
        }
        if (tts) {
            byteSign |= (0x1 << 3);
        }
        if (adScreen) {
            byteSign |= (0x1 << 4);
        }
        if (source) {
            byteSign |= (0x1 << 5);
        }
        return byteSign;
    }
}
