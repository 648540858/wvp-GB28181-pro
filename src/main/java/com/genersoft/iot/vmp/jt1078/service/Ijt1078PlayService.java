package com.genersoft.iot.vmp.jt1078.service;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.jt1078.bean.*;
import com.genersoft.iot.vmp.jt1078.proc.request.J1205;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;

import java.util.List;

public interface Ijt1078PlayService {

    JTMediaStreamType checkStreamFromJt(String stream);

    void play(String phoneNumber, Integer channelId, int type, CommonCallback<WVPResult<StreamInfo>> callback);

    void playback(String phoneNumber, Integer channelId, String startTime, String endTime, Integer type,
                  Integer rate, Integer playbackType, Integer playbackSpeed, CommonCallback<WVPResult<StreamInfo>> callback);

    void stopPlay(String phoneNumber, Integer channelId);

    void pausePlay(String phoneNumber, Integer channelId);

    void continueLivePlay(String phoneNumber, Integer channelId);

    List<J1205.JRecordItem> getRecordList(String phoneNumber, Integer channelId, String startTime, String endTime);

    void stopPlayback(String phoneNumber, Integer channelId);

    StreamInfo startTalk(String phoneNumber, Integer channelId);

    void stopTalk(String phoneNumber, Integer channelId);

    void playbackControl(String phoneNumber, Integer channelId, Integer command, Integer playbackSpeed, String time);

}
