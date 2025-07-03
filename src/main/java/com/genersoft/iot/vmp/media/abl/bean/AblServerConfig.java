package com.genersoft.iot.vmp.media.abl.bean;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class AblServerConfig {

    @ConfigKeyId("secret")
    private String secret;

    @ConfigKeyId("ServerIP")
    private String serverIp;

    @ConfigKeyId("mediaServerID")
    private String mediaServerId;

    @ConfigKeyId("hook_enable")
    private Integer hookEnable;

    @ConfigKeyId("enable_audio")
    private Integer enableAudio;

    @ConfigKeyId("httpServerPort")
    private Integer httpServerPort;

    @ConfigKeyId("rtspPort")
    private Integer rtspPort;

    @ConfigKeyId("rtmpPort")
    private Integer rtmpPort;

    @ConfigKeyId("httpFlvPort")
    private Integer httpFlvPort;

    @ConfigKeyId("hls_enable")
    private Integer hlsEnable;

    @ConfigKeyId("hlsPort")
    private Integer hlsPort;

    @ConfigKeyId("wsPort")
    private Integer wsPort;

    @ConfigKeyId("mp4Port")
    private Integer mp4Port;

    @ConfigKeyId("ps_tsRecvPort")
    private Integer psTsRecvPort;

    @ConfigKeyId("hlsCutType")
    private Integer hlsCutType;

    @ConfigKeyId("h265CutType")
    private Integer h265CutType;

    @ConfigKeyId("RecvThreadCount")
    private Integer RecvThreadCount;

    @ConfigKeyId("SendThreadCount")
    private Integer SendThreadCount;

    @ConfigKeyId("GB28181RtpTCPHeadType")
    private Integer GB28181RtpTCPHeadType;

    @ConfigKeyId("ReConnectingCount")
    private Integer ReConnectingCount;

    @ConfigKeyId("maxTimeNoOneWatch")
    private Integer maxTimeNoOneWatch;

    @ConfigKeyId("pushEnable_mp4")
    private Integer pushEnableMp4;

    @ConfigKeyId("fileSecond")
    private Integer fileSecond;

    @ConfigKeyId("fileKeepMaxTime")
    private Integer fileKeepMaxTime;

    @ConfigKeyId("httpDownloadSpeed")
    private Integer httpDownloadSpeed;

    @ConfigKeyId("RecordReplayThread")
    private Integer RecordReplayThread;

    @ConfigKeyId("convertMaxObject")
    private Integer convertMaxObject;

    @ConfigKeyId("version")
    private String version;

    @ConfigKeyId("recordPath")
    private String recordPath;

    @ConfigKeyId("picturePath")
    private String picturePath;

    @ConfigKeyId("noneReaderDuration")
    private Integer noneReaderDuration;

    @ConfigKeyId("on_server_started")
    private String onServerStarted;

    @ConfigKeyId("on_server_keepalive")
    private String onServerKeepalive;

    @ConfigKeyId("on_play")
    private String onPlay;

    @ConfigKeyId("on_publish")
    private String onPublish;

    @ConfigKeyId("on_stream_arrive")
    private String onStreamArrive;

    @ConfigKeyId("on_stream_not_arrive")
    private String onStreamNotArrive;

    @ConfigKeyId("on_stream_none_reader")
    private String onStreamNoneReader;

    @ConfigKeyId("on_stream_disconnect")
    private String onStreamDisconnect;

    @ConfigKeyId("on_stream_not_found")
    private String onStreamNotFound;

    @ConfigKeyId("on_record_mp4")
    private String onRecordMp4;

    @ConfigKeyId("on_delete_record_mp4")
    private String onDeleteRecordMp4;

    @ConfigKeyId("on_record_progress")
    private String onRecordProgress;

    @ConfigKeyId("on_record_ts")
    private String onRecordTs;

    @ConfigKeyId("enable_GetFileDuration")
    private Integer enableGetFileDuration;

    @ConfigKeyId("keepaliveDuration")
    private Integer keepaliveDuration;

    @ConfigKeyId("captureReplayType")
    private Integer captureReplayType;

    @ConfigKeyId("pictureMaxCount")
    private Integer pictureMaxCount;

    @ConfigKeyId("videoFileFormat")
    private Integer videoFileFormat;

    @ConfigKeyId("MaxDiconnectTimeoutSecond")
    private Integer maxDiconnectTimeoutSecond;

    @ConfigKeyId("G711ConvertAAC")
    private Integer g711ConvertAAC;

    @ConfigKeyId("filterVideo_enable")
    private Integer filterVideoEnable;

    @ConfigKeyId("filterVideo_text")
    private String filterVideoText;

    @ConfigKeyId("FilterFontSize")
    private Integer filterFontSize;

    @ConfigKeyId("FilterFontColor")
    private String filterFontColor;

    @ConfigKeyId("FilterFontLeft")
    private Integer filterFontLeft;

    @ConfigKeyId("FilterFontTop")
    private Integer filterFontTop;

    @ConfigKeyId("FilterFontAlpha")
    private Double filterFontAlpha;

    @ConfigKeyId("convertOutWidth")
    private Integer convertOutWidth;

    @ConfigKeyId("convertOutHeight")
    private Integer convertOutHeight;

    @ConfigKeyId("convertOutBitrate")
    private Integer convertOutBitrate;

    @ConfigKeyId("flvPlayAddMute")
    private Integer flvPlayAddMute;

    @ConfigKeyId("gb28181LibraryUse")
    private Integer gb28181LibraryUse;

    @ConfigKeyId("rtc.listening-ip")
    private String rtcListeningIp;

    @ConfigKeyId("rtc.listening-port")
    private Integer rtcListeningIpPort;

    @ConfigKeyId("rtc.external-ip")
    private String rtcExternalIp;

    @ConfigKeyId("rtc.realm")
    private String rtcRealm;

    @ConfigKeyId("rtc.user")
    private String rtcUser;

    @ConfigKeyId("rtc.min-port")
    private Integer rtcMinPort;

    @ConfigKeyId("rtc.max-port")
    private Integer rtcMaxPort;

    public static AblServerConfig getInstance(JSONArray jsonArray) {
        if (jsonArray == null || jsonArray.isEmpty()) {
            return null;
        }
        AblServerConfig ablServerConfig = new AblServerConfig();
        Field[] fields = AblServerConfig.class.getDeclaredFields();
        Map<String, Field> fieldMap = new HashMap<>();
        for (Field field : fields) {
            if (field.isAnnotationPresent(ConfigKeyId.class)) {
                ConfigKeyId configKeyId = field.getAnnotation(ConfigKeyId.class);
                fieldMap.put(configKeyId.value(), field);
            }
        }
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            if (jsonObject == null) {
                continue;
            }
            for (String key : fieldMap.keySet()) {
                if (jsonObject.containsKey(key)) {
                    Field field = fieldMap.get(key);
                    field.setAccessible(true);
                    try {
                        field.set(ablServerConfig, jsonObject.getObject(key, fieldMap.get(key).getType()));
                    } catch (IllegalAccessException e) {}
                }
            }
        }
        return ablServerConfig;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public String getMediaServerId() {
        return mediaServerId;
    }

    public void setMediaServerId(String mediaServerId) {
        this.mediaServerId = mediaServerId;
    }

    public Integer getHookEnable() {
        return hookEnable;
    }

    public void setHookEnable(Integer hookEnable) {
        this.hookEnable = hookEnable;
    }

    public Integer getEnableAudio() {
        return enableAudio;
    }

    public void setEnableAudio(Integer enableAudio) {
        this.enableAudio = enableAudio;
    }

    public Integer getHttpServerPort() {
        return httpServerPort;
    }

    public void setHttpServerPort(Integer httpServerPort) {
        this.httpServerPort = httpServerPort;
    }

    public Integer getRtspPort() {
        return rtspPort;
    }

    public void setRtspPort(Integer rtspPort) {
        this.rtspPort = rtspPort;
    }

    public Integer getRtmpPort() {
        return rtmpPort;
    }

    public void setRtmpPort(Integer rtmpPort) {
        this.rtmpPort = rtmpPort;
    }

    public Integer getHttpFlvPort() {
        return httpFlvPort;
    }

    public void setHttpFlvPort(Integer httpFlvPort) {
        this.httpFlvPort = httpFlvPort;
    }

    public Integer getHlsEnable() {
        return hlsEnable;
    }

    public void setHlsEnable(Integer hlsEnable) {
        this.hlsEnable = hlsEnable;
    }

    public Integer getHlsPort() {
        return hlsPort;
    }

    public void setHlsPort(Integer hlsPort) {
        this.hlsPort = hlsPort;
    }

    public Integer getWsPort() {
        return wsPort;
    }

    public void setWsPort(Integer wsPort) {
        this.wsPort = wsPort;
    }

    public Integer getMp4Port() {
        return mp4Port;
    }

    public void setMp4Port(Integer mp4Port) {
        this.mp4Port = mp4Port;
    }

    public Integer getPsTsRecvPort() {
        return psTsRecvPort;
    }

    public void setPsTsRecvPort(Integer psTsRecvPort) {
        this.psTsRecvPort = psTsRecvPort;
    }

    public Integer getHlsCutType() {
        return hlsCutType;
    }

    public void setHlsCutType(Integer hlsCutType) {
        this.hlsCutType = hlsCutType;
    }

    public Integer getH265CutType() {
        return h265CutType;
    }

    public void setH265CutType(Integer h265CutType) {
        this.h265CutType = h265CutType;
    }

    public Integer getRecvThreadCount() {
        return RecvThreadCount;
    }

    public void setRecvThreadCount(Integer recvThreadCount) {
        RecvThreadCount = recvThreadCount;
    }

    public Integer getSendThreadCount() {
        return SendThreadCount;
    }

    public void setSendThreadCount(Integer sendThreadCount) {
        SendThreadCount = sendThreadCount;
    }

    public Integer getGB28181RtpTCPHeadType() {
        return GB28181RtpTCPHeadType;
    }

    public void setGB28181RtpTCPHeadType(Integer GB28181RtpTCPHeadType) {
        this.GB28181RtpTCPHeadType = GB28181RtpTCPHeadType;
    }

    public Integer getReConnectingCount() {
        return ReConnectingCount;
    }

    public void setReConnectingCount(Integer reConnectingCount) {
        ReConnectingCount = reConnectingCount;
    }

    public Integer getMaxTimeNoOneWatch() {
        return maxTimeNoOneWatch;
    }

    public void setMaxTimeNoOneWatch(Integer maxTimeNoOneWatch) {
        this.maxTimeNoOneWatch = maxTimeNoOneWatch;
    }

    public Integer getPushEnableMp4() {
        return pushEnableMp4;
    }

    public void setPushEnableMp4(Integer pushEnableMp4) {
        this.pushEnableMp4 = pushEnableMp4;
    }

    public Integer getFileSecond() {
        return fileSecond;
    }

    public void setFileSecond(Integer fileSecond) {
        this.fileSecond = fileSecond;
    }

    public Integer getFileKeepMaxTime() {
        return fileKeepMaxTime;
    }

    public void setFileKeepMaxTime(Integer fileKeepMaxTime) {
        this.fileKeepMaxTime = fileKeepMaxTime;
    }

    public Integer getHttpDownloadSpeed() {
        return httpDownloadSpeed;
    }

    public void setHttpDownloadSpeed(Integer httpDownloadSpeed) {
        this.httpDownloadSpeed = httpDownloadSpeed;
    }

    public Integer getRecordReplayThread() {
        return RecordReplayThread;
    }

    public void setRecordReplayThread(Integer recordReplayThread) {
        RecordReplayThread = recordReplayThread;
    }

    public Integer getConvertMaxObject() {
        return convertMaxObject;
    }

    public void setConvertMaxObject(Integer convertMaxObject) {
        this.convertMaxObject = convertMaxObject;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getRecordPath() {
        return recordPath;
    }

    public void setRecordPath(String recordPath) {
        this.recordPath = recordPath;
    }

    public String getPicturePath() {
        return picturePath;
    }

    public void setPicturePath(String picturePath) {
        this.picturePath = picturePath;
    }

    public Integer getNoneReaderDuration() {
        return noneReaderDuration;
    }

    public void setNoneReaderDuration(Integer noneReaderDuration) {
        this.noneReaderDuration = noneReaderDuration;
    }

    public String getOnServerStarted() {
        return onServerStarted;
    }

    public void setOnServerStarted(String onServerStarted) {
        this.onServerStarted = onServerStarted;
    }

    public String getOnServerKeepalive() {
        return onServerKeepalive;
    }

    public void setOnServerKeepalive(String onServerKeepalive) {
        this.onServerKeepalive = onServerKeepalive;
    }

    public String getOnPlay() {
        return onPlay;
    }

    public void setOnPlay(String onPlay) {
        this.onPlay = onPlay;
    }

    public String getOnPublish() {
        return onPublish;
    }

    public void setOnPublish(String onPublish) {
        this.onPublish = onPublish;
    }

    public String getOnStreamArrive() {
        return onStreamArrive;
    }

    public void setOnStreamArrive(String onStreamArrive) {
        this.onStreamArrive = onStreamArrive;
    }

    public String getOnStreamNotArrive() {
        return onStreamNotArrive;
    }

    public void setOnStreamNotArrive(String onStreamNotArrive) {
        this.onStreamNotArrive = onStreamNotArrive;
    }

    public String getOnStreamNoneReader() {
        return onStreamNoneReader;
    }

    public void setOnStreamNoneReader(String onStreamNoneReader) {
        this.onStreamNoneReader = onStreamNoneReader;
    }

    public String getOnStreamDisconnect() {
        return onStreamDisconnect;
    }

    public void setOnStreamDisconnect(String onStreamDisconnect) {
        this.onStreamDisconnect = onStreamDisconnect;
    }

    public String getOnDeleteRecordMp4() {
        return onDeleteRecordMp4;
    }

    public void setOnDeleteRecordMp4(String onDeleteRecordMp4) {
        this.onDeleteRecordMp4 = onDeleteRecordMp4;
    }

    public String getOnRecordProgress() {
        return onRecordProgress;
    }

    public void setOnRecordProgress(String onRecordProgress) {
        this.onRecordProgress = onRecordProgress;
    }

    public String getOnRecordTs() {
        return onRecordTs;
    }

    public void setOnRecordTs(String onRecordTs) {
        this.onRecordTs = onRecordTs;
    }

    public Integer getEnableGetFileDuration() {
        return enableGetFileDuration;
    }

    public void setEnableGetFileDuration(Integer enableGetFileDuration) {
        this.enableGetFileDuration = enableGetFileDuration;
    }

    public Integer getKeepaliveDuration() {
        return keepaliveDuration;
    }

    public void setKeepaliveDuration(Integer keepaliveDuration) {
        this.keepaliveDuration = keepaliveDuration;
    }

    public Integer getCaptureReplayType() {
        return captureReplayType;
    }

    public void setCaptureReplayType(Integer captureReplayType) {
        this.captureReplayType = captureReplayType;
    }

    public Integer getVideoFileFormat() {
        return videoFileFormat;
    }

    public void setVideoFileFormat(Integer videoFileFormat) {
        this.videoFileFormat = videoFileFormat;
    }

    public Integer getMaxDiconnectTimeoutSecond() {
        return maxDiconnectTimeoutSecond;
    }

    public void setMaxDiconnectTimeoutSecond(Integer maxDiconnectTimeoutSecond) {
        this.maxDiconnectTimeoutSecond = maxDiconnectTimeoutSecond;
    }

    public Integer getG711ConvertAAC() {
        return g711ConvertAAC;
    }

    public void setG711ConvertAAC(Integer g711ConvertAAC) {
        this.g711ConvertAAC = g711ConvertAAC;
    }

    public Integer getFilterVideoEnable() {
        return filterVideoEnable;
    }

    public void setFilterVideoEnable(Integer filterVideoEnable) {
        this.filterVideoEnable = filterVideoEnable;
    }

    public String getFilterVideoText() {
        return filterVideoText;
    }

    public void setFilterVideoText(String filterVideoText) {
        this.filterVideoText = filterVideoText;
    }

    public Integer getFilterFontSize() {
        return filterFontSize;
    }

    public void setFilterFontSize(Integer filterFontSize) {
        this.filterFontSize = filterFontSize;
    }

    public String getFilterFontColor() {
        return filterFontColor;
    }

    public void setFilterFontColor(String filterFontColor) {
        this.filterFontColor = filterFontColor;
    }

    public Integer getFilterFontLeft() {
        return filterFontLeft;
    }

    public void setFilterFontLeft(Integer filterFontLeft) {
        this.filterFontLeft = filterFontLeft;
    }

    public Integer getFilterFontTop() {
        return filterFontTop;
    }

    public void setFilterFontTop(Integer filterFontTop) {
        this.filterFontTop = filterFontTop;
    }

    public Double getFilterFontAlpha() {
        return filterFontAlpha;
    }

    public void setFilterFontAlpha(Double filterFontAlpha) {
        this.filterFontAlpha = filterFontAlpha;
    }

    public Integer getConvertOutWidth() {
        return convertOutWidth;
    }

    public void setConvertOutWidth(Integer convertOutWidth) {
        this.convertOutWidth = convertOutWidth;
    }

    public Integer getConvertOutHeight() {
        return convertOutHeight;
    }

    public void setConvertOutHeight(Integer convertOutHeight) {
        this.convertOutHeight = convertOutHeight;
    }

    public Integer getConvertOutBitrate() {
        return convertOutBitrate;
    }

    public void setConvertOutBitrate(Integer convertOutBitrate) {
        this.convertOutBitrate = convertOutBitrate;
    }

    public Integer getFlvPlayAddMute() {
        return flvPlayAddMute;
    }

    public void setFlvPlayAddMute(Integer flvPlayAddMute) {
        this.flvPlayAddMute = flvPlayAddMute;
    }

    public Integer getGb28181LibraryUse() {
        return gb28181LibraryUse;
    }

    public void setGb28181LibraryUse(Integer gb28181LibraryUse) {
        this.gb28181LibraryUse = gb28181LibraryUse;
    }

    public String getRtcListeningIp() {
        return rtcListeningIp;
    }

    public void setRtcListeningIp(String rtcListeningIp) {
        this.rtcListeningIp = rtcListeningIp;
    }

    public Integer getRtcListeningIpPort() {
        return rtcListeningIpPort;
    }

    public void setRtcListeningIpPort(Integer rtcListeningIpPort) {
        this.rtcListeningIpPort = rtcListeningIpPort;
    }

    public String getRtcExternalIp() {
        return rtcExternalIp;
    }

    public void setRtcExternalIp(String rtcExternalIp) {
        this.rtcExternalIp = rtcExternalIp;
    }

    public String getRtcRealm() {
        return rtcRealm;
    }

    public void setRtcRealm(String rtcRealm) {
        this.rtcRealm = rtcRealm;
    }

    public String getRtcUser() {
        return rtcUser;
    }

    public void setRtcUser(String rtcUser) {
        this.rtcUser = rtcUser;
    }

    public Integer getRtcMinPort() {
        return rtcMinPort;
    }

    public void setRtcMinPort(Integer rtcMinPort) {
        this.rtcMinPort = rtcMinPort;
    }

    public Integer getRtcMaxPort() {
        return rtcMaxPort;
    }

    public void setRtcMaxPort(Integer rtcMaxPort) {
        this.rtcMaxPort = rtcMaxPort;
    }

    public String getOnRecordMp4() {
        return onRecordMp4;
    }

    public void setOnRecordMp4(String onRecordMp4) {
        this.onRecordMp4 = onRecordMp4;
    }

    public Integer getPictureMaxCount() {
        return pictureMaxCount;
    }

    public void setPictureMaxCount(Integer pictureMaxCount) {
        this.pictureMaxCount = pictureMaxCount;
    }

    public String getOnStreamNotFound() {
        return onStreamNotFound;
    }

    public void setOnStreamNotFound(String onStreamNotFound) {
        this.onStreamNotFound = onStreamNotFound;
    }
}
