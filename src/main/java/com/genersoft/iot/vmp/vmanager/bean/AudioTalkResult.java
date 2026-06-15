package com.genersoft.iot.vmp.vmanager.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "对讲信息")
public class AudioTalkResult {

    @Schema(description = "推流地址（浏览器 WebRTC推流到ZLM）")
    private StreamContent pushStream;

    @Schema(description = "播放地址（设备音频通过ZLM播放给浏览器），喊话时为null")
    private StreamContent playStream;
}
