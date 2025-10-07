package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.CommonRecordInfo;
import com.genersoft.iot.vmp.gb28181.bean.InviteMessageInfo;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;

import java.util.List;

public interface IGbChannelPlayService {

    void startInvite(CommonGBChannel channel, InviteMessageInfo inviteInfo, Platform platform, ErrorCallback<StreamInfo> callback);

    void stopInvite(InviteSessionType type, CommonGBChannel channel, String stream);

    void playback(CommonGBChannel channel, Long startTime, Long stopTime, ErrorCallback<StreamInfo> callback);

    void download(CommonGBChannel channel, Long startTime, Long stopTime, Integer downloadSpeed,
                  ErrorCallback<StreamInfo> callback);

    void stopPlay(CommonGBChannel channel);

    void play(CommonGBChannel channel, Platform platform, Boolean record, ErrorCallback<StreamInfo> callback);

    void stopPlayback(CommonGBChannel channel, String stream);

    void stopDownload(CommonGBChannel channel, String stream);

    void playbackPause(CommonGBChannel channel, String streamId);

    void playbackResume(CommonGBChannel channel, String streamId);

    void playbackSeek(CommonGBChannel channel, String stream, long seekTime);

    void playbackSpeed(CommonGBChannel channel, String stream, Double speed);

    void queryRecord(CommonGBChannel channel, String startTime, String endTime, ErrorCallback<List<CommonRecordInfo>> callback);
}
