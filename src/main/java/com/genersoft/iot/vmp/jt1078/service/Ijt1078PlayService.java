package com.genersoft.iot.vmp.jt1078.service;

import com.genersoft.iot.vmp.common.GeneralCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.jt1078.bean.*;
import com.genersoft.iot.vmp.jt1078.proc.request.J1205;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface Ijt1078PlayService {
    JTMediaStreamType checkStreamFromJt(String stream);

    void play(String phoneNumber, Integer channelId, int type, GeneralCallback<StreamInfo> callback);

    void playback(String phoneNumber, Integer channelId, String startTime, String endTime, Integer type,
                  Integer rate, Integer playbackType, Integer playbackSpeed, GeneralCallback<StreamInfo> callback);

    void stopPlay(String phoneNumber, Integer channelId);

    void pausePlay(String phoneNumber, Integer channelId);

    void continueLivePlay(String phoneNumber, Integer channelId);

    List<J1205.JRecordItem> getRecordList(String phoneNumber, Integer channelId, String startTime, String endTime);

    void stopPlayback(String phoneNumber, Integer channelId);

    void startTalk(String phoneNumber, Integer channelId, String app, String stream, String mediaServerId, Boolean onlySend, GeneralCallback<StreamInfo> callback);

    void stopTalk(String phoneNumber, Integer channelId);

    void playbackControl(String phoneNumber, Integer channelId, Integer command, Integer playbackSpeed, String time);

}
