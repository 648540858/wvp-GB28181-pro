package com.genersoft.iot.vmp.conf;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class MediaServerConfig {

    @JSONField(name = "api.apiDebug")
    private String apiDebug;

    @JSONField(name = "api.secret")
    private String apiSecret;

    @JSONField(name = "ffmpeg.bin")
    private String ffmpegBin;

    @JSONField(name = "ffmpeg.cmd")
    private String ffmpegCmd;

    @JSONField(name = "ffmpeg.log")
    private String ffmpegLog;

    @JSONField(name = "general.enableVhost")
    private String generalEnableVhost;

    @JSONField(name = "general.flowThreshold")
    private String generalFlowThreshold;

    @JSONField(name = "general.maxStreamWaitMS")
    private String generalMaxStreamWaitMS;

    @JSONField(name = "general.streamNoneReaderDelayMS")
    private String generalStreamNoneReaderDelayMS;

    @JSONField(name = "general.localIP")
    private String localIP;

    @JSONField(name = "hls.fileBufSize")
    private String hlsFileBufSize;

    @JSONField(name = "hls.filePath")
    private String hlsFilePath;

    @JSONField(name = "hls.segDur")
    private String hlsSegDur;

    @JSONField(name = "hls.segNum")
    private String hlsSegNum;

    @JSONField(name = "hook.access_file_except_hls")
    private String hookAccessFileExceptHLS;

    @JSONField(name = "hook.admin_params")
    private String hookAdminParams;

    @JSONField(name = "hook.enable")
    private String hookEnable;

    @JSONField(name = "hook.on_flow_report")
    private String hookOnFlowReport;

    @JSONField(name = "hook.on_http_access")
    private String hookOnHttpAccess;

    @JSONField(name = "hook.on_play")
    private String hookOnPlay;

    @JSONField(name = "hook.on_publish")
    private String hookOnPublish;

    @JSONField(name = "hook.on_record_mp4")
    private String hookOnRecordMp4;

    @JSONField(name = "hook.on_rtsp_auth")
    private String hookOnRtspAuth;

    @JSONField(name = "hook.on_rtsp_realm")
    private String hookOnRtspRealm;

    @JSONField(name = "hook.on_shell_login")
    private String hookOnShellLogin;

    @JSONField(name = "hook.on_stream_changed")
    private String hookOnStreamChanged;

    @JSONField(name = "hook.on_stream_none_reader")
    private String hookOnStreamNoneReader;

    @JSONField(name = "hook.on_stream_not_found")
    private String hookOnStreamNotFound;

    @JSONField(name = "hook.timeoutSec")
    private String hookTimeoutSec;

    @JSONField(name = "http.charSet")
    private String httpCharSet;

    @JSONField(name = "http.keepAliveSecond")
    private String httpKeepAliveSecond;

    @JSONField(name = "http.maxReqCount")
    private String httpMaxReqCount;

    @JSONField(name = "http.maxReqSize")
    private String httpMaxReqSize;

    @JSONField(name = "http.notFound")
    private String httpNotFound;

    @JSONField(name = "http.port")
    private String httpPort;

    @JSONField(name = "http.rootPath")
    private String httpRootPath;

    @JSONField(name = "http.sendBufSize")
    private String httpSendBufSize;

    @JSONField(name = "http.sslport")
    private String httpSSLport;

    @JSONField(name = "multicast.addrMax")
    private String multicastAddrMax;

    @JSONField(name = "multicast.addrMin")
    private String multicastAddrMin;

    @JSONField(name = "multicast.udpTTL")
    private String multicastUdpTTL;

    @JSONField(name = "record.appName")
    private String recordAppName;

    @JSONField(name = "record.filePath")
    private String recordFilePath;

    @JSONField(name = "record.fileSecond")
    private String recordFileSecond;

    @JSONField(name = "record.sampleMS")
    private String recordFileSampleMS;

    @JSONField(name = "rtmp.handshakeSecond")
    private String rtmpHandshakeSecond;

    @JSONField(name = "rtmp.keepAliveSecond")
    private String rtmpKeepAliveSecond;

    @JSONField(name = "rtmp.modifyStamp")
    private String rtmpModifyStamp;

    @JSONField(name = "rtmp.port")
    private String rtmpPort;

    @JSONField(name = "rtp.audioMtuSize")
    private String rtpAudioMtuSize;

    @JSONField(name = "rtp.clearCount")
    private String rtpClearCount;

    @JSONField(name = "rtp.cycleMS")
    private String rtpCycleMS;

    @JSONField(name = "rtp.maxRtpCount")
    private String rtpMaxRtpCount;

    @JSONField(name = "rtp.videoMtuSize")
    private String rtpVideoMtuSize;

    @JSONField(name = "rtp_proxy.checkSource")
    private String rtpProxyCheckSource;

    @JSONField(name = "rtp_proxy.dumpDir")
    private String rtpProxyDumpDir;

    @JSONField(name = "rtp_proxy.port")
    private String rtpProxyPort;

    @JSONField(name = "rtp_proxy.timeoutSec")
    private String rtpProxyTimeoutSec;

    @JSONField(name = "rtsp.authBasic")
    private String rtspAuthBasic;

    @JSONField(name = "rtsp.handshakeSecond")
    private String rtspHandshakeSecond;

    @JSONField(name = "rtsp.keepAliveSecond")
    private String rtspKeepAliveSecond;

    @JSONField(name = "rtsp.port")
    private String rtspPort;

    @JSONField(name = "rtsp.sslport")
    private String rtspSSlport;

    @JSONField(name = "shell.maxReqSize")
    private String shellMaxReqSize;

    @JSONField(name = "shell.shell")
    private String shellPhell;
}
