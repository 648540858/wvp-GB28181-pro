package com.genersoft.iot.vmp.service.bean;

import com.genersoft.iot.vmp.media.bean.MediaServer;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RTPServerParam {

    /**
     * 使用的流媒体
     */
    private MediaServer mediaServer;
    private String app;
    private String streamId;
    /**
     * 开启rtpServer时使用的ssrc，开启rtpServer时会根据这个ssrc进行校验，如果不填则不校验
     */
    private Long ssrc;
    private Integer port;
    private boolean onlyAuto;
    private boolean disableAudio;
    private boolean reUsePort;

    /**
     * tcp模式，0时为不启用tcp监听，1时为启用tcp监听，2时为tcp主动连接模式
     */
    private Integer tcpMode;

    public RTPServerParam(MediaServer mediaServer, String app, String streamId, Long ssrc, Integer port,
                          boolean onlyAuto, boolean disableAudio, boolean reUsePort, Integer tcpMode) {
        this.mediaServer = mediaServer;
        this.app = app;
        this.streamId = streamId;
        this.ssrc = ssrc;
        this.port = port;
        this.onlyAuto = onlyAuto;
        this.disableAudio = disableAudio;
        this.reUsePort = reUsePort;
        this.tcpMode = tcpMode;
    }
}
