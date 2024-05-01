package com.genersoft.iot.vmp.jt1078.bean;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文本信息标志
 */
@Schema(description = "文本信息标志")
public class JTTextSign {

    @Schema(description = "01服务,2紧急,3通知")
    private int type;

    @Schema(description = "1终端显示器显示")
    private boolean terminalDisplay;

    @Schema(description = "1终端 TTS 播读")
    private boolean tts;

    @Schema(description = "false: 中心导航信息 true CAN故障码信息")
    private boolean source;

    public byte encode(){
        byte bytes = 0;
        bytes |= (byte) type;
        if (terminalDisplay) {
            bytes |= (0x1 << 2);
        }
        if (tts) {
            bytes |= (0x1 << 3);
        }
        if (source) {
            bytes |= (0x1 << 5);
        }
        return bytes;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isTerminalDisplay() {
        return terminalDisplay;
    }

    public void setTerminalDisplay(boolean terminalDisplay) {
        this.terminalDisplay = terminalDisplay;
    }

    public boolean isTts() {
        return tts;
    }

    public void setTts(boolean tts) {
        this.tts = tts;
    }

    public boolean isSource() {
        return source;
    }

    public void setSource(boolean source) {
        this.source = source;
    }
}
