package com.genersoft.iot.vmp.media.bean;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServer;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 视频信息
 */
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
    @Schema(description = "音频编码类型")
    private String audioCodec;
    @Schema(description = "音频通道数")
    private Integer audioChannels;
    @Schema(description = "音频采样率")
    private Integer audioSampleRate;
    @Schema(description = "音频采样率")
    private Long duration;
    @Schema(description = "在线")
    private Boolean online;
    @Schema(description = "unknown = 0,rtmp_push=1,rtsp_push=2,rtp_push=3,pull=4,ffmpeg_pull=5,mp4_vod=6,device_chn=7")
    private Integer originType;
    @Schema(description = "存活时间，单位秒")
    private Long aliveSecond;
    @Schema(description = "数据产生速度，单位byte/s")
    private Long bytesSpeed;

    public static MediaInfo getInstance(JSONObject jsonObject, MediaServer mediaServer) {
        MediaInfo mediaInfo = new MediaInfo();
        mediaInfo.setMediaServer(mediaServer);
        String app = jsonObject.getString("app");
        mediaInfo.setApp(app);
        String stream = jsonObject.getString("stream");
        mediaInfo.setStream(stream);
        String schema = jsonObject.getString("schema");
        mediaInfo.setSchema(schema);
        Integer totalReaderCount = jsonObject.getInteger("totalReaderCount");
        Boolean online = jsonObject.getBoolean("online");
        Integer originType = jsonObject.getInteger("originType");
        Long aliveSecond = jsonObject.getLong("aliveSecond");
        Long bytesSpeed = jsonObject.getLong("bytesSpeed");
        if (totalReaderCount != null) {
            mediaInfo.setReaderCount(totalReaderCount);
        }
        if (online != null) {
            mediaInfo.setOnline(online);
        }
        if (originType != null) {
            mediaInfo.setOriginType(originType);
        }
        if (aliveSecond != null) {
            mediaInfo.setAliveSecond(aliveSecond);
        }
        if (bytesSpeed != null) {
            mediaInfo.setBytesSpeed(bytesSpeed);
        }
        JSONArray jsonArray = jsonObject.getJSONArray("tracks");
        if (jsonArray.isEmpty()) {
            return null;
        }
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject trackJson = jsonArray.getJSONObject(i);
            Integer channels = trackJson.getInteger("channels");
            Integer codecId = trackJson.getInteger("codec_id");
            Integer codecType = trackJson.getInteger("codec_type");
            Integer sampleRate = trackJson.getInteger("sample_rate");
            Integer height = trackJson.getInteger("height");
            Integer width = trackJson.getInteger("height");
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
        return mediaInfo;
    }

    public static MediaInfo getInstance(OnStreamChangedHookParam param, MediaServer mediaServer) {
        List<OnStreamChangedHookParam.MediaTrack> tracks = param.getTracks();
        MediaInfo mediaInfo = new MediaInfo();
        mediaInfo.setApp(param.getApp());
        mediaInfo.setStream(param.getStream());
        mediaInfo.setSchema(param.getSchema());
        mediaInfo.setMediaServer(mediaServer);
        mediaInfo.setReaderCount(param.getTotalReaderCount());
        mediaInfo.setOnline(param.isRegist());
        mediaInfo.setOriginType(param.getOriginType());
        mediaInfo.setAliveSecond(param.getAliveSecond());
        mediaInfo.setBytesSpeed(param.getBytesSpeed());
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

    public Integer getReaderCount() {
        return readerCount;
    }

    public void setReaderCount(Integer readerCount) {
        this.readerCount = readerCount;
    }

    public String getVideoCodec() {
        return videoCodec;
    }

    public void setVideoCodec(String videoCodec) {
        this.videoCodec = videoCodec;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getAudioCodec() {
        return audioCodec;
    }

    public void setAudioCodec(String audioCodec) {
        this.audioCodec = audioCodec;
    }

    public Integer getAudioChannels() {
        return audioChannels;
    }

    public void setAudioChannels(Integer audioChannels) {
        this.audioChannels = audioChannels;
    }

    public Integer getAudioSampleRate() {
        return audioSampleRate;
    }

    public void setAudioSampleRate(Integer audioSampleRate) {
        this.audioSampleRate = audioSampleRate;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Boolean getOnline() {
        return online;
    }

    public void setOnline(Boolean online) {
        this.online = online;
    }

    public Integer getOriginType() {
        return originType;
    }

    public void setOriginType(Integer originType) {
        this.originType = originType;
    }

    public Long getAliveSecond() {
        return aliveSecond;
    }

    public void setAliveSecond(Long aliveSecond) {
        this.aliveSecond = aliveSecond;
    }

    public Long getBytesSpeed() {
        return bytesSpeed;
    }

    public void setBytesSpeed(Long bytesSpeed) {
        this.bytesSpeed = bytesSpeed;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public MediaServer getMediaServer() {
        return mediaServer;
    }

    public void setMediaServer(MediaServer mediaServer) {
        this.mediaServer = mediaServer;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }
}
