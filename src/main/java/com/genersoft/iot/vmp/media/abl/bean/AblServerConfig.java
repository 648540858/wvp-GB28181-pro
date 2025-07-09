package com.genersoft.iot.vmp.media.abl.bean;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Data
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

    @ConfigKeyId("1078Port")
    private Integer jtt1078RecvPort;

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
}
