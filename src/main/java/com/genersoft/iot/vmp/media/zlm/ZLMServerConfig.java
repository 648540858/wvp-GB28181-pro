package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson2.annotation.JSONField;
import com.genersoft.iot.vmp.media.zlm.dto.hook.HookParam;

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


    public String getHookIp() {
        return hookIp;
    }

    public void setHookIp(String hookIp) {
        this.hookIp = hookIp;
    }

    public String getApiDebug() {
        return apiDebug;
    }

    public void setApiDebug(String apiDebug) {
        this.apiDebug = apiDebug;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public String getFfmpegBin() {
        return ffmpegBin;
    }

    public void setFfmpegBin(String ffmpegBin) {
        this.ffmpegBin = ffmpegBin;
    }

    public String getFfmpegCmd() {
        return ffmpegCmd;
    }

    public void setFfmpegCmd(String ffmpegCmd) {
        this.ffmpegCmd = ffmpegCmd;
    }

    public String getFfmpegLog() {
        return ffmpegLog;
    }

    public void setFfmpegLog(String ffmpegLog) {
        this.ffmpegLog = ffmpegLog;
    }

    public String getGeneralEnableVhost() {
        return generalEnableVhost;
    }

    public void setGeneralEnableVhost(String generalEnableVhost) {
        this.generalEnableVhost = generalEnableVhost;
    }

    public String getGeneralMediaServerId() {
        return generalMediaServerId;
    }

    public void setGeneralMediaServerId(String generalMediaServerId) {
        this.generalMediaServerId = generalMediaServerId;
    }

    public String getGeneralFlowThreshold() {
        return generalFlowThreshold;
    }

    public void setGeneralFlowThreshold(String generalFlowThreshold) {
        this.generalFlowThreshold = generalFlowThreshold;
    }

    public String getGeneralMaxStreamWaitMS() {
        return generalMaxStreamWaitMS;
    }

    public void setGeneralMaxStreamWaitMS(String generalMaxStreamWaitMS) {
        this.generalMaxStreamWaitMS = generalMaxStreamWaitMS;
    }

    public int getGeneralStreamNoneReaderDelayMS() {
        return generalStreamNoneReaderDelayMS;
    }

    public void setGeneralStreamNoneReaderDelayMS(int generalStreamNoneReaderDelayMS) {
        this.generalStreamNoneReaderDelayMS = generalStreamNoneReaderDelayMS;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getSdpIp() {
        return sdpIp;
    }

    public void setSdpIp(String sdpIp) {
        this.sdpIp = sdpIp;
    }

    public String getStreamIp() {
        return streamIp;
    }

    public void setStreamIp(String streamIp) {
        this.streamIp = streamIp;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getHlsFileBufSize() {
        return hlsFileBufSize;
    }

    public void setHlsFileBufSize(String hlsFileBufSize) {
        this.hlsFileBufSize = hlsFileBufSize;
    }

    public String getHlsFilePath() {
        return hlsFilePath;
    }

    public void setHlsFilePath(String hlsFilePath) {
        this.hlsFilePath = hlsFilePath;
    }

    public String getHlsSegDur() {
        return hlsSegDur;
    }

    public void setHlsSegDur(String hlsSegDur) {
        this.hlsSegDur = hlsSegDur;
    }

    public String getHlsSegNum() {
        return hlsSegNum;
    }

    public void setHlsSegNum(String hlsSegNum) {
        this.hlsSegNum = hlsSegNum;
    }

    public String getHookAccessFileExceptHLS() {
        return hookAccessFileExceptHLS;
    }

    public void setHookAccessFileExceptHLS(String hookAccessFileExceptHLS) {
        this.hookAccessFileExceptHLS = hookAccessFileExceptHLS;
    }

    public String getHookAdminParams() {
        return hookAdminParams;
    }

    public void setHookAdminParams(String hookAdminParams) {
        this.hookAdminParams = hookAdminParams;
    }

    public String getHookEnable() {
        return hookEnable;
    }

    public void setHookEnable(String hookEnable) {
        this.hookEnable = hookEnable;
    }

    public String getHookOnFlowReport() {
        return hookOnFlowReport;
    }

    public void setHookOnFlowReport(String hookOnFlowReport) {
        this.hookOnFlowReport = hookOnFlowReport;
    }

    public String getHookOnHttpAccess() {
        return hookOnHttpAccess;
    }

    public void setHookOnHttpAccess(String hookOnHttpAccess) {
        this.hookOnHttpAccess = hookOnHttpAccess;
    }

    public String getHookOnPlay() {
        return hookOnPlay;
    }

    public void setHookOnPlay(String hookOnPlay) {
        this.hookOnPlay = hookOnPlay;
    }

    public String getHookOnPublish() {
        return hookOnPublish;
    }

    public void setHookOnPublish(String hookOnPublish) {
        this.hookOnPublish = hookOnPublish;
    }

    public String getHookOnRecordMp4() {
        return hookOnRecordMp4;
    }

    public void setHookOnRecordMp4(String hookOnRecordMp4) {
        this.hookOnRecordMp4 = hookOnRecordMp4;
    }

    public String getHookOnRtspAuth() {
        return hookOnRtspAuth;
    }

    public void setHookOnRtspAuth(String hookOnRtspAuth) {
        this.hookOnRtspAuth = hookOnRtspAuth;
    }

    public String getHookOnRtspRealm() {
        return hookOnRtspRealm;
    }

    public void setHookOnRtspRealm(String hookOnRtspRealm) {
        this.hookOnRtspRealm = hookOnRtspRealm;
    }

    public String getHookOnShellLogin() {
        return hookOnShellLogin;
    }

    public void setHookOnShellLogin(String hookOnShellLogin) {
        this.hookOnShellLogin = hookOnShellLogin;
    }

    public String getHookOnStreamChanged() {
        return hookOnStreamChanged;
    }

    public void setHookOnStreamChanged(String hookOnStreamChanged) {
        this.hookOnStreamChanged = hookOnStreamChanged;
    }

    public String getHookOnStreamNoneReader() {
        return hookOnStreamNoneReader;
    }

    public void setHookOnStreamNoneReader(String hookOnStreamNoneReader) {
        this.hookOnStreamNoneReader = hookOnStreamNoneReader;
    }

    public String getHookOnStreamNotFound() {
        return hookOnStreamNotFound;
    }

    public void setHookOnStreamNotFound(String hookOnStreamNotFound) {
        this.hookOnStreamNotFound = hookOnStreamNotFound;
    }

    public String getHookTimeoutSec() {
        return hookTimeoutSec;
    }

    public void setHookTimeoutSec(String hookTimeoutSec) {
        this.hookTimeoutSec = hookTimeoutSec;
    }

    public String getHttpCharSet() {
        return httpCharSet;
    }

    public void setHttpCharSet(String httpCharSet) {
        this.httpCharSet = httpCharSet;
    }

    public String getHttpKeepAliveSecond() {
        return httpKeepAliveSecond;
    }

    public void setHttpKeepAliveSecond(String httpKeepAliveSecond) {
        this.httpKeepAliveSecond = httpKeepAliveSecond;
    }

    public String getHttpMaxReqCount() {
        return httpMaxReqCount;
    }

    public void setHttpMaxReqCount(String httpMaxReqCount) {
        this.httpMaxReqCount = httpMaxReqCount;
    }

    public String getHttpMaxReqSize() {
        return httpMaxReqSize;
    }

    public void setHttpMaxReqSize(String httpMaxReqSize) {
        this.httpMaxReqSize = httpMaxReqSize;
    }

    public String getHttpNotFound() {
        return httpNotFound;
    }

    public void setHttpNotFound(String httpNotFound) {
        this.httpNotFound = httpNotFound;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public String getHttpRootPath() {
        return httpRootPath;
    }

    public void setHttpRootPath(String httpRootPath) {
        this.httpRootPath = httpRootPath;
    }

    public String getHttpSendBufSize() {
        return httpSendBufSize;
    }

    public void setHttpSendBufSize(String httpSendBufSize) {
        this.httpSendBufSize = httpSendBufSize;
    }

    public int getHttpSSLport() {
        return httpSSLport;
    }

    public void setHttpSSLport(int httpSSLport) {
        this.httpSSLport = httpSSLport;
    }

    public String getMulticastAddrMax() {
        return multicastAddrMax;
    }

    public void setMulticastAddrMax(String multicastAddrMax) {
        this.multicastAddrMax = multicastAddrMax;
    }

    public String getMulticastAddrMin() {
        return multicastAddrMin;
    }

    public void setMulticastAddrMin(String multicastAddrMin) {
        this.multicastAddrMin = multicastAddrMin;
    }

    public String getMulticastUdpTTL() {
        return multicastUdpTTL;
    }

    public void setMulticastUdpTTL(String multicastUdpTTL) {
        this.multicastUdpTTL = multicastUdpTTL;
    }

    public String getRecordAppName() {
        return recordAppName;
    }

    public void setRecordAppName(String recordAppName) {
        this.recordAppName = recordAppName;
    }

    public String getRecordFilePath() {
        return recordFilePath;
    }

    public void setRecordFilePath(String recordFilePath) {
        this.recordFilePath = recordFilePath;
    }

    public String getRecordFileSecond() {
        return recordFileSecond;
    }

    public void setRecordFileSecond(String recordFileSecond) {
        this.recordFileSecond = recordFileSecond;
    }

    public String getRecordFileSampleMS() {
        return recordFileSampleMS;
    }

    public void setRecordFileSampleMS(String recordFileSampleMS) {
        this.recordFileSampleMS = recordFileSampleMS;
    }

    public String getRtmpHandshakeSecond() {
        return rtmpHandshakeSecond;
    }

    public void setRtmpHandshakeSecond(String rtmpHandshakeSecond) {
        this.rtmpHandshakeSecond = rtmpHandshakeSecond;
    }

    public String getRtmpKeepAliveSecond() {
        return rtmpKeepAliveSecond;
    }

    public void setRtmpKeepAliveSecond(String rtmpKeepAliveSecond) {
        this.rtmpKeepAliveSecond = rtmpKeepAliveSecond;
    }

    public String getRtmpModifyStamp() {
        return rtmpModifyStamp;
    }

    public void setRtmpModifyStamp(String rtmpModifyStamp) {
        this.rtmpModifyStamp = rtmpModifyStamp;
    }

    public int getRtmpPort() {
        return rtmpPort;
    }

    public void setRtmpPort(int rtmpPort) {
        this.rtmpPort = rtmpPort;
    }

    public int getRtmpSslPort() {
        return rtmpSslPort;
    }

    public void setRtmpSslPort(int rtmpSslPort) {
        this.rtmpSslPort = rtmpSslPort;
    }

    public String getRtpAudioMtuSize() {
        return rtpAudioMtuSize;
    }

    public void setRtpAudioMtuSize(String rtpAudioMtuSize) {
        this.rtpAudioMtuSize = rtpAudioMtuSize;
    }

    public String getRtpClearCount() {
        return rtpClearCount;
    }

    public void setRtpClearCount(String rtpClearCount) {
        this.rtpClearCount = rtpClearCount;
    }

    public String getRtpCycleMS() {
        return rtpCycleMS;
    }

    public void setRtpCycleMS(String rtpCycleMS) {
        this.rtpCycleMS = rtpCycleMS;
    }

    public String getRtpMaxRtpCount() {
        return rtpMaxRtpCount;
    }

    public void setRtpMaxRtpCount(String rtpMaxRtpCount) {
        this.rtpMaxRtpCount = rtpMaxRtpCount;
    }

    public String getRtpVideoMtuSize() {
        return rtpVideoMtuSize;
    }

    public void setRtpVideoMtuSize(String rtpVideoMtuSize) {
        this.rtpVideoMtuSize = rtpVideoMtuSize;
    }

    public String getRtpProxyCheckSource() {
        return rtpProxyCheckSource;
    }

    public void setRtpProxyCheckSource(String rtpProxyCheckSource) {
        this.rtpProxyCheckSource = rtpProxyCheckSource;
    }

    public String getRtpProxyDumpDir() {
        return rtpProxyDumpDir;
    }

    public void setRtpProxyDumpDir(String rtpProxyDumpDir) {
        this.rtpProxyDumpDir = rtpProxyDumpDir;
    }

    public int getRtpProxyPort() {
        return rtpProxyPort;
    }

    public void setRtpProxyPort(int rtpProxyPort) {
        this.rtpProxyPort = rtpProxyPort;
    }

    public String getRtpProxyTimeoutSec() {
        return rtpProxyTimeoutSec;
    }

    public void setRtpProxyTimeoutSec(String rtpProxyTimeoutSec) {
        this.rtpProxyTimeoutSec = rtpProxyTimeoutSec;
    }

    public String getRtspAuthBasic() {
        return rtspAuthBasic;
    }

    public void setRtspAuthBasic(String rtspAuthBasic) {
        this.rtspAuthBasic = rtspAuthBasic;
    }

    public String getRtspHandshakeSecond() {
        return rtspHandshakeSecond;
    }

    public void setRtspHandshakeSecond(String rtspHandshakeSecond) {
        this.rtspHandshakeSecond = rtspHandshakeSecond;
    }

    public String getRtspKeepAliveSecond() {
        return rtspKeepAliveSecond;
    }

    public void setRtspKeepAliveSecond(String rtspKeepAliveSecond) {
        this.rtspKeepAliveSecond = rtspKeepAliveSecond;
    }

    public int getRtspPort() {
        return rtspPort;
    }

    public void setRtspPort(int rtspPort) {
        this.rtspPort = rtspPort;
    }

    public int getRtspSSlport() {
        return rtspSSlport;
    }

    public void setRtspSSlport(int rtspSSlport) {
        this.rtspSSlport = rtspSSlport;
    }

    public String getShellMaxReqSize() {
        return shellMaxReqSize;
    }

    public void setShellMaxReqSize(String shellMaxReqSize) {
        this.shellMaxReqSize = shellMaxReqSize;
    }

    public String getShellPhell() {
        return shellPhell;
    }

    public void setShellPhell(String shellPhell) {
        this.shellPhell = shellPhell;
    }

    public Float getHookAliveInterval() {
        return hookAliveInterval;
    }

    public void setHookAliveInterval(Float hookAliveInterval) {
        this.hookAliveInterval = hookAliveInterval;
    }

    public String getPortRange() {
        return portRange;
    }

    public void setPortRange(String portRange) {
        this.portRange = portRange;
    }

    public String getApiSnapRoot() {
        return apiSnapRoot;
    }

    public void setApiSnapRoot(String apiSnapRoot) {
        this.apiSnapRoot = apiSnapRoot;
    }

    public String getApiDefaultSnap() {
        return apiDefaultSnap;
    }

    public void setApiDefaultSnap(String apiDefaultSnap) {
        this.apiDefaultSnap = apiDefaultSnap;
    }

    public String getFfmpegSnap() {
        return ffmpegSnap;
    }

    public void setFfmpegSnap(String ffmpegSnap) {
        this.ffmpegSnap = ffmpegSnap;
    }

    public String getFfmpegRestartSec() {
        return ffmpegRestartSec;
    }

    public void setFfmpegRestartSec(String ffmpegRestartSec) {
        this.ffmpegRestartSec = ffmpegRestartSec;
    }

    public String getProtocolModifyStamp() {
        return protocolModifyStamp;
    }

    public void setProtocolModifyStamp(String protocolModifyStamp) {
        this.protocolModifyStamp = protocolModifyStamp;
    }

    public String getProtocolEnableAudio() {
        return protocolEnableAudio;
    }

    public void setProtocolEnableAudio(String protocolEnableAudio) {
        this.protocolEnableAudio = protocolEnableAudio;
    }

    public String getProtocolAddMuteAudio() {
        return protocolAddMuteAudio;
    }

    public void setProtocolAddMuteAudio(String protocolAddMuteAudio) {
        this.protocolAddMuteAudio = protocolAddMuteAudio;
    }

    public String getProtocolContinuePushMs() {
        return protocolContinuePushMs;
    }

    public void setProtocolContinuePushMs(String protocolContinuePushMs) {
        this.protocolContinuePushMs = protocolContinuePushMs;
    }

    public String getProtocolEnableHls() {
        return protocolEnableHls;
    }

    public void setProtocolEnableHls(String protocolEnableHls) {
        this.protocolEnableHls = protocolEnableHls;
    }

    public String getProtocolEnableMp4() {
        return protocolEnableMp4;
    }

    public void setProtocolEnableMp4(String protocolEnableMp4) {
        this.protocolEnableMp4 = protocolEnableMp4;
    }

    public String getProtocolEnableRtsp() {
        return protocolEnableRtsp;
    }

    public void setProtocolEnableRtsp(String protocolEnableRtsp) {
        this.protocolEnableRtsp = protocolEnableRtsp;
    }

    public String getProtocolEnableRtmp() {
        return protocolEnableRtmp;
    }

    public void setProtocolEnableRtmp(String protocolEnableRtmp) {
        this.protocolEnableRtmp = protocolEnableRtmp;
    }

    public String getProtocolEnableTs() {
        return protocolEnableTs;
    }

    public void setProtocolEnableTs(String protocolEnableTs) {
        this.protocolEnableTs = protocolEnableTs;
    }

    public String getProtocolEnableFmp4() {
        return protocolEnableFmp4;
    }

    public void setProtocolEnableFmp4(String protocolEnableFmp4) {
        this.protocolEnableFmp4 = protocolEnableFmp4;
    }

    public String getProtocolMp4AsPlayer() {
        return protocolMp4AsPlayer;
    }

    public void setProtocolMp4AsPlayer(String protocolMp4AsPlayer) {
        this.protocolMp4AsPlayer = protocolMp4AsPlayer;
    }

    public String getProtocolMp4MaxSecond() {
        return protocolMp4MaxSecond;
    }

    public void setProtocolMp4MaxSecond(String protocolMp4MaxSecond) {
        this.protocolMp4MaxSecond = protocolMp4MaxSecond;
    }

    public String getProtocolMp4SavePath() {
        return protocolMp4SavePath;
    }

    public void setProtocolMp4SavePath(String protocolMp4SavePath) {
        this.protocolMp4SavePath = protocolMp4SavePath;
    }

    public String getProtocolHlsSavePath() {
        return protocolHlsSavePath;
    }

    public void setProtocolHlsSavePath(String protocolHlsSavePath) {
        this.protocolHlsSavePath = protocolHlsSavePath;
    }

    public String getProtocolHlsDemand() {
        return protocolHlsDemand;
    }

    public void setProtocolHlsDemand(String protocolHlsDemand) {
        this.protocolHlsDemand = protocolHlsDemand;
    }

    public String getProtocolRtspDemand() {
        return protocolRtspDemand;
    }

    public void setProtocolRtspDemand(String protocolRtspDemand) {
        this.protocolRtspDemand = protocolRtspDemand;
    }

    public String getProtocolRtmpDemand() {
        return protocolRtmpDemand;
    }

    public void setProtocolRtmpDemand(String protocolRtmpDemand) {
        this.protocolRtmpDemand = protocolRtmpDemand;
    }

    public String getProtocolTsDemand() {
        return protocolTsDemand;
    }

    public void setProtocolTsDemand(String protocolTsDemand) {
        this.protocolTsDemand = protocolTsDemand;
    }

    public String getProtocolFmp4Demand() {
        return protocolFmp4Demand;
    }

    public void setProtocolFmp4Demand(String protocolFmp4Demand) {
        this.protocolFmp4Demand = protocolFmp4Demand;
    }

    public String getGeneralResetWhenRePlay() {
        return generalResetWhenRePlay;
    }

    public void setGeneralResetWhenRePlay(String generalResetWhenRePlay) {
        this.generalResetWhenRePlay = generalResetWhenRePlay;
    }

    public String getGeneralMergeWriteMS() {
        return generalMergeWriteMS;
    }

    public void setGeneralMergeWriteMS(String generalMergeWriteMS) {
        this.generalMergeWriteMS = generalMergeWriteMS;
    }

    public String getGeneralWaitTrackReadyMs() {
        return generalWaitTrackReadyMs;
    }

    public void setGeneralWaitTrackReadyMs(String generalWaitTrackReadyMs) {
        this.generalWaitTrackReadyMs = generalWaitTrackReadyMs;
    }

    public String getGeneralWaitAddTrackMs() {
        return generalWaitAddTrackMs;
    }

    public void setGeneralWaitAddTrackMs(String generalWaitAddTrackMs) {
        this.generalWaitAddTrackMs = generalWaitAddTrackMs;
    }

    public String getGeneralUnreadyFrameCache() {
        return generalUnreadyFrameCache;
    }

    public void setGeneralUnreadyFrameCache(String generalUnreadyFrameCache) {
        this.generalUnreadyFrameCache = generalUnreadyFrameCache;
    }

    public String getHlsSegRetain() {
        return hlsSegRetain;
    }

    public void setHlsSegRetain(String hlsSegRetain) {
        this.hlsSegRetain = hlsSegRetain;
    }

    public String getHlsBroadcastRecordTs() {
        return hlsBroadcastRecordTs;
    }

    public void setHlsBroadcastRecordTs(String hlsBroadcastRecordTs) {
        this.hlsBroadcastRecordTs = hlsBroadcastRecordTs;
    }

    public String getHlsDeleteDelaySec() {
        return hlsDeleteDelaySec;
    }

    public void setHlsDeleteDelaySec(String hlsDeleteDelaySec) {
        this.hlsDeleteDelaySec = hlsDeleteDelaySec;
    }

    public String getHlsSegKeep() {
        return hlsSegKeep;
    }

    public void setHlsSegKeep(String hlsSegKeep) {
        this.hlsSegKeep = hlsSegKeep;
    }

    public String getHookOnServerStarted() {
        return hookOnServerStarted;
    }

    public void setHookOnServerStarted(String hookOnServerStarted) {
        this.hookOnServerStarted = hookOnServerStarted;
    }

    public String getHookOnServerKeepalive() {
        return hookOnServerKeepalive;
    }

    public void setHookOnServerKeepalive(String hookOnServerKeepalive) {
        this.hookOnServerKeepalive = hookOnServerKeepalive;
    }

    public String getHookOnSendRtpStopped() {
        return hookOnSendRtpStopped;
    }

    public void setHookOnSendRtpStopped(String hookOnSendRtpStopped) {
        this.hookOnSendRtpStopped = hookOnSendRtpStopped;
    }

    public String getHookOnRtpServerTimeout() {
        return hookOnRtpServerTimeout;
    }

    public void setHookOnRtpServerTimeout(String hookOnRtpServerTimeout) {
        this.hookOnRtpServerTimeout = hookOnRtpServerTimeout;
    }
}
