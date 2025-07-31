package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;

/**
 * 资源能力接入-录像回放
 */
public interface ISourcePlaybackService {

    void playback(CommonGBChannel channel, Long startTime, Long stopTime, ErrorCallback<StreamInfo> callback);

    void stopPlayback(CommonGBChannel channel, String stream);

    void playbackPause(CommonGBChannel channel, String stream);
}
