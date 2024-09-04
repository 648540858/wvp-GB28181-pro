package com.genersoft.iot.vmp.service.bean;

import com.genersoft.iot.vmp.media.bean.MediaServer;
import lombok.Data;

@Data
public class RTPServerParam {

    MediaServer mediaServerItem;
    String streamId;
    String presetSsrc;
    boolean ssrcCheck;
    boolean isPlayback;
    Integer port;
    Boolean onlyAuto;
    Boolean disableAudio;
    Boolean reUsePort;
    Integer tcpMode;
}
