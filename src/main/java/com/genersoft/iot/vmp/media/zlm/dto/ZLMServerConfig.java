package com.genersoft.iot.vmp.media.zlm.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import com.genersoft.iot.vmp.media.zlm.dto.hook.HookParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ZLMServerConfig extends HookParam {

    @JSONField(name = "api.apiDebug")
    private String apiDebug;

    @JSONField(name = "api.secret")
    private String apiSecret;

    @JSONField(name = "api.snapRoot")
    private String apiSnapRoot;

    @JSONField(name = "api.defaultSnap")
    private String apiDefaultSnap;

    @JSONField(name = "ffmpeg.bin")
    private String ffmpegBin;

    @JSONField(name = "ffmpeg.cmd")
    private String ffmpegCmd;

    @JSONField(name = "ffmpeg.snap")
    private String ffmpegSnap;

    @JSONField(name = "ffmpeg.log")
    private String ffmpegLog;

    @JSONField(name = "ffmpeg.restart_sec")
    private String ffmpegRestartSec;

    @JSONField(name = "protocol.modify_stamp")
    private String protocolModifyStamp;

    @JSONField(name = "protocol.enable_audio")
    private String protocolEnableAudio;

    @JSONField(name = "protocol.add_mute_audio")
    private String protocolAddMuteAudio;

    @JSONField(name = "protocol.continue_push_ms")
    private String protocolContinuePushMs;

    @JSONField(name = "protocol.enable_hls")
    private String protocolEnableHls;

    @JSONField(name = "protocol.enable_mp4")
    private String protocolEnableMp4;

    @JSONField(name = "protocol.enable_rtsp")
    private String protocolEnableRtsp;

    @JSONField(name = "protocol.enable_rtmp")
    private String protocolEnableRtmp;

    @JSONField(name = "protocol.enable_ts")
    private String protocolEnableTs;

    @JSONField(name = "protocol.enable_fmp4")
    private String protocolEnableFmp4;

    @JSONField(name = "protocol.mp4_as_player")
    private String protocolMp4AsPlayer;

    @JSONField(name = "protocol.mp4_max_second")
    private String protocolMp4MaxSecond;

    @JSONField(name = "protocol.mp4_save_path")
    private String protocolMp4SavePath;

    @JSONField(name = "protocol.hls_save_path")
    private String protocolHlsSavePath;

    @JSONField(name = "protocol.hls_demand")
    private String protocolHlsDemand;

    @JSONField(name = "protocol.rtsp_demand")
    private String protocolRtspDemand;

    @JSONField(name = "protocol.rtmp_demand")
    private String protocolRtmpDemand;

    @JSONField(name = "protocol.ts_demand")
    private String protocolTsDemand;

    @JSONField(name = "protocol.fmp4_demand")
    private String protocolFmp4Demand;

    @JSONField(name = "general.enableVhost")
    private String generalEnableVhost;

    @JSONField(name = "general.flowThreshold")
    private String generalFlowThreshold;

    @JSONField(name = "general.maxStreamWaitMS")
    private String generalMaxStreamWaitMS;

    @JSONField(name = "general.streamNoneReaderDelayMS")
    private int generalStreamNoneReaderDelayMS;

    @JSONField(name = "general.resetWhenRePlay")
    private String generalResetWhenRePlay;

    @JSONField(name = "general.mergeWriteMS")
    private String generalMergeWriteMS;

    @JSONField(name = "general.mediaServerId")
    private String generalMediaServerId;

    @JSONField(name = "general.wait_track_ready_ms")
    private String generalWaitTrackReadyMs;

    @JSONField(name = "general.wait_add_track_ms")
    private String generalWaitAddTrackMs;

    @JSONField(name = "general.unready_frame_cache")
    private String generalUnreadyFrameCache;


    @JSONField(name = "ip")
    private String ip;

    private String sdpIp;

    private String streamIp;

    private String hookIp;

    private String updateTime;

    private String createTime;

    @JSONField(name = "hls.fileBufSize")
    private String hlsFileBufSize;

    @JSONField(name = "hls.filePath")
    private String hlsFilePath;

    @JSONField(name = "hls.segDur")
    private String hlsSegDur;

    @JSONField(name = "hls.segNum")
    private String hlsSegNum;

    @JSONField(name = "hls.segRetain")
    private String hlsSegRetain;

    @JSONField(name = "hls.broadcastRecordTs")
    private String hlsBroadcastRecordTs;

    @JSONField(name = "hls.deleteDelaySec")
    private String hlsDeleteDelaySec;

    @JSONField(name = "hls.segKeep")
    private String hlsSegKeep;

    @JSONField(name = "hook.access_file_except_hls")
    private String hookAccessFileExceptHLS;

    @JSONField(name = "hook.admin_params")
    private String hookAdminParams;

    @JSONField(name = "hook.alive_interval")
    private Float hookAliveInterval;

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

    @JSONField(name = "hook.on_server_started")
    private String hookOnServerStarted;

    @JSONField(name = "hook.on_server_keepalive")
    private String hookOnServerKeepalive;

    @JSONField(name = "hook.on_send_rtp_stopped")
    private String hookOnSendRtpStopped;

    @JSONField(name = "hook.on_rtp_server_timeout")
    private String hookOnRtpServerTimeout;

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
    private int httpPort;

    @JSONField(name = "http.rootPath")
    private String httpRootPath;

    @JSONField(name = "http.sendBufSize")
    private String httpSendBufSize;

    @JSONField(name = "http.sslport")
    private int httpSSLport;

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
    private int rtmpPort;

    @JSONField(name = "rtmp.sslport")
    private int rtmpSslPort;

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
    private int rtpProxyPort;

    @JSONField(name = "rtp_proxy.port_range")
    private String portRange;

    @JSONField(name = "rtp_proxy.timeoutSec")
    private String rtpProxyTimeoutSec;

    @JSONField(name = "rtsp.authBasic")
    private String rtspAuthBasic;

    @JSONField(name = "rtsp.handshakeSecond")
    private String rtspHandshakeSecond;

    @JSONField(name = "rtsp.keepAliveSecond")
    private String rtspKeepAliveSecond;

    @JSONField(name = "rtsp.port")
    private int rtspPort;

    @JSONField(name = "rtsp.sslport")
    private int rtspSSlport;

    @JSONField(name = "shell.maxReqSize")
    private String shellMaxReqSize;

    @JSONField(name = "shell.shell")
    private String shellPhell;

    @JSONField(name = "transcode.suffix")
    private String transcodeSuffix;

}
