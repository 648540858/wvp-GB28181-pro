package com.genersoft.iot.vmp.media.bean;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 视频信息
 */
@Schema(description = "视频信息")
public class MediaInfo {
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

    public static MediaInfo getInstance(JSONObject jsonObject) {
        MediaInfo mediaInfo = new MediaInfo();
        Integer totalReaderCount = jsonObject.getInteger("totalReaderCount");
        if (totalReaderCount != null) {
            mediaInfo.setReaderCount(totalReaderCount);
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

    public static MediaInfo getInstance(OnStreamChangedHookParam param) {
        List<OnStreamChangedHookParam.MediaTrack> tracks = param.getTracks();
        MediaInfo mediaInfo = new MediaInfo();
        mediaInfo.setReaderCount(param.getTotalReaderCount());
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
}
