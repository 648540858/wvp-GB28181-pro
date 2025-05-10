package com.genersoft.iot.vmp.media.bean;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;
import com.genersoft.iot.vmp.utils.MediaServerUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;

/**
 * 视频信息
 */
@Data
@Schema(description = "视频信息")
public class MediaInfo {
    @Schema(description = "应用名")
    private String app;
    @Schema(description = "流ID")
    private String stream;
    @Schema(description = "流媒体节点")
    private MediaServer mediaServer;
    @Schema(description = "协议")
    private String schema;

    @Schema(description = "观看人数")
    private Integer readerCount;
    @Schema(description = "视频编码类型")
    private String videoCodec;
    @Schema(description = "视频宽度")
    private Integer width;
    @Schema(description = "视频高度")
    private Integer height;
    @Schema(description = "FPS")
    private Integer fps;
    @Schema(description = "丢包率")
    private Integer loss;
    @Schema(description = "音频编码类型")
    private String audioCodec;
    @Schema(description = "音频通道数")
    private Integer audioChannels;
    @Schema(description = "音频采样率")
    private Integer audioSampleRate;
    @Schema(description = "时长")
    private Long duration;
    @Schema(description = "在线")
    private Boolean online;
    @Schema(description = "unknown = 0,rtmp_push=1,rtsp_push=2,rtp_push=3,pull=4,ffmpeg_pull=5,mp4_vod=6,device_chn=7,rtc_push=8")
    private Integer originType;
    @Schema(description = "originType的文本描述")
    private String originTypeStr;
    @Schema(description = "产生流的源流地址")
    private String originUrl;
    @Schema(description = "存活时间，单位秒")
    private Long aliveSecond;
    @Schema(description = "数据产生速度，单位byte/s")
    private Long bytesSpeed;
    @Schema(description = "鉴权参数")
    private String callId;
    @Schema(description = "额外参数")
    private Map<String, String> paramMap;
    @Schema(description = "服务ID")
    private String serverId;


    public static MediaInfo getInstance(JSONObject jsonObject, MediaServer mediaServer, String serverId) {
        MediaInfo mediaInfo = new MediaInfo();
        mediaInfo.setMediaServer(mediaServer);
        mediaInfo.setServerId(serverId);
        String app = jsonObject.getString("app");
        mediaInfo.setApp(app);
        String stream = jsonObject.getString("stream");
        mediaInfo.setStream(stream);
        String schema = jsonObject.getString("schema");
        mediaInfo.setSchema(schema);
        Integer totalReaderCount = jsonObject.getInteger("totalReaderCount");
        Boolean online = jsonObject.getBoolean("online");
        Integer originType = jsonObject.getInteger("originType");
        String originUrl = jsonObject.getString("originUrl");
        String originTypeStr = jsonObject.getString("originTypeStr");
        Long aliveSecond = jsonObject.getLong("aliveSecond");
        String params = jsonObject.getString("params");
        Long bytesSpeed = jsonObject.getLong("bytesSpeed");
        if (totalReaderCount != null) {
            mediaInfo.setReaderCount(totalReaderCount);
        } else {
            mediaInfo.setReaderCount(0);
        }
        if (online != null) {
            mediaInfo.setOnline(online);
        }
        if (originType != null) {
            mediaInfo.setOriginType(originType);
        }
        if (originTypeStr != null) {
            mediaInfo.setOriginTypeStr(originTypeStr);
        }

        if (aliveSecond != null) {
            mediaInfo.setAliveSecond(aliveSecond);
        }
        if (bytesSpeed != null) {
            mediaInfo.setBytesSpeed(bytesSpeed);
        }
        if (params != null) {
            mediaInfo.setParamMap(MediaServerUtils.urlParamToMap(params));
            if(mediaInfo.getCallId() == null) {
                mediaInfo.setCallId(mediaInfo.getParamMap().get("callId"));
            }
        }
        JSONArray jsonArray = jsonObject.getJSONArray("tracks");
        if (!ObjectUtils.isEmpty(jsonArray)) {
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject trackJson = jsonArray.getJSONObject(i);
                Integer channels = trackJson.getInteger("channels");
                Integer codecId = trackJson.getInteger("codec_id");
                Integer codecType = trackJson.getInteger("codec_type");
                Integer sampleRate = trackJson.getInteger("sample_rate");
                Integer height = trackJson.getInteger("height");
                Integer width = trackJson.getInteger("width");
                Integer fps = trackJson.getInteger("fps");
                Integer loss = trackJson.getInteger("loss");
                Integer frames = trackJson.getInteger("frames");
                Long keyFrames = trackJson.getLongValue("key_frames");
                Integer gop_interval_ms = trackJson.getInteger("gop_interval_ms");
                Long gop_size = trackJson.getLongValue("gop_size");

                Long duration = trackJson.getLongValue("duration");
                if (channels != null) {
                    mediaInfo.setAudioChannels(channels);
                }
                if (sampleRate != null) {
                    mediaInfo.setAudioSampleRate(sampleRate);
                }
                if (height != null) {
                    mediaInfo.setHeight(height);
                }
                if (width != null) {
                    mediaInfo.setWidth(width);
                }
                if (fps != null) {
                    mediaInfo.setFps(fps);
                }
                if (loss != null) {
                    mediaInfo.setLoss(loss);
                }
                if (duration > 0L) {
                    mediaInfo.setDuration(duration);
                }
                if (codecId != null) {
                    switch (codecId) {
                        case 0:
                            mediaInfo.setVideoCodec("H264");
                            break;
                        case 1:
                            mediaInfo.setVideoCodec("H265");
                            break;
                        case 2:
                            mediaInfo.setAudioCodec("AAC");
                            break;
                        case 3:
                            mediaInfo.setAudioCodec("G711A");
                            break;
                        case 4:
                            mediaInfo.setAudioCodec("G711U");
                            break;
                    }
                }
            }
        }
        return mediaInfo;
    }

    public static MediaInfo getInstance(OnStreamChangedHookParam param, MediaServer mediaServer, String serverId) {

        MediaInfo mediaInfo = new MediaInfo();
        mediaInfo.setApp(param.getApp());
        mediaInfo.setStream(param.getStream());
        mediaInfo.setSchema(param.getSchema());
        mediaInfo.setMediaServer(mediaServer);
        mediaInfo.setReaderCount(param.getTotalReaderCount());
        mediaInfo.setOnline(param.isRegist());
        mediaInfo.setOriginType(param.getOriginType());
        mediaInfo.setOriginTypeStr(param.getOriginTypeStr());
        mediaInfo.setOriginUrl(param.getOriginUrl());
        mediaInfo.setOriginUrl(param.getOriginUrl());
        mediaInfo.setAliveSecond(param.getAliveSecond());
        mediaInfo.setBytesSpeed(param.getBytesSpeed());
        mediaInfo.setParamMap(param.getParamMap());
        if(mediaInfo.getCallId() == null) {
            mediaInfo.setCallId(param.getParamMap().get("callId"));
        }
        mediaInfo.setServerId(serverId);
        List<OnStreamChangedHookParam.MediaTrack> tracks = param.getTracks();
        if (tracks == null || tracks.isEmpty()) {
            return mediaInfo;
        }
        for (OnStreamChangedHookParam.MediaTrack mediaTrack : tracks) {
            switch (mediaTrack.getCodec_id()) {
                case 0:
                    mediaInfo.setVideoCodec("H264");
                    break;
                case 1:
                    mediaInfo.setVideoCodec("H265");
                    break;
                case 2:
                    mediaInfo.setAudioCodec("AAC");
                    break;
                case 3:
                    mediaInfo.setAudioCodec("G711A");
                    break;
                case 4:
                    mediaInfo.setAudioCodec("G711U");
                    break;
            }
            if (mediaTrack.getSample_rate() > 0) {
                mediaInfo.setAudioSampleRate(mediaTrack.getSample_rate());
            }
            if (mediaTrack.getChannels() > 0) {
                mediaInfo.setAudioChannels(mediaTrack.getChannels());
            }
            if (mediaTrack.getHeight() > 0) {
                mediaInfo.setHeight(mediaTrack.getHeight());
            }
            if (mediaTrack.getWidth() > 0) {
                mediaInfo.setWidth(mediaTrack.getWidth());
            }
        }
        return mediaInfo;
    }
}
