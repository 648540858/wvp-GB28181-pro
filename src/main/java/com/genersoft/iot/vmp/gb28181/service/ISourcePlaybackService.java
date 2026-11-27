package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.CommonRecordInfo;
import com.genersoft.iot.vmp.gb28181.bean.PlayException;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;

import javax.sip.message.Response;

import java.util.List;

/**
 * 资源能力接入-录像回放
 */
public interface ISourcePlaybackService {

    void playback(CommonGBChannel channel, Long startTime, Long stopTime, ErrorCallback<StreamInfo> callback);

    /** 旧协议仅兼容空会话标识，不能静默把独立播放器请求合并为同一控制会话。 */
    default void playback(CommonGBChannel channel, Long startTime, Long stopTime,
                          String sessionId, ErrorCallback<StreamInfo> callback) {
        if (sessionId != null) {
            throw new PlayException(Response.NOT_IMPLEMENTED, "Not Implemented");
        }
        playback(channel, startTime, stopTime, callback);
    }

    void stopPlayback(CommonGBChannel channel, String stream);

    void playbackPause(CommonGBChannel channel, String stream);

    void playbackResume(CommonGBChannel channel, String stream);

    void playbackSeek(CommonGBChannel channel, String stream, long seekTime);

    void playbackSpeed(CommonGBChannel channel, String stream, Double speed);

    void queryRecord(CommonGBChannel channel, String startTime, String endTime, ErrorCallback<List<CommonRecordInfo>> callback);
}
