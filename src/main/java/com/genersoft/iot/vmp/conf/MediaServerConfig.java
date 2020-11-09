package com.genersoft.iot.vmp.conf;

import com.alibaba.fastjson.annotation.JSONField;

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

    private String localIP;

    private String wanIp;

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

    public String getGeneralStreamNoneReaderDelayMS() {
        return generalStreamNoneReaderDelayMS;
    }

    public void setGeneralStreamNoneReaderDelayMS(String generalStreamNoneReaderDelayMS) {
        this.generalStreamNoneReaderDelayMS = generalStreamNoneReaderDelayMS;
    }

    public String getLocalIP() {
        return localIP;
    }

    public void setLocalIP(String localIP) {
        this.localIP = localIP;
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

    public String getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(String httpPort) {
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

    public String getHttpSSLport() {
        return httpSSLport;
    }

    public void setHttpSSLport(String httpSSLport) {
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

    public String getRtmpPort() {
        return rtmpPort;
    }

    public void setRtmpPort(String rtmpPort) {
        this.rtmpPort = rtmpPort;
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

    public String getRtpProxyPort() {
        return rtpProxyPort;
    }

    public void setRtpProxyPort(String rtpProxyPort) {
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

    public String getRtspPort() {
        return rtspPort;
    }

    public void setRtspPort(String rtspPort) {
        this.rtspPort = rtspPort;
    }

    public String getRtspSSlport() {
        return rtspSSlport;
    }

    public void setRtspSSlport(String rtspSSlport) {
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

    public String getWanIp() {
        return wanIp;
    }

    public void setWanIp(String wanIp) {
        this.wanIp = wanIp;
    }
}
