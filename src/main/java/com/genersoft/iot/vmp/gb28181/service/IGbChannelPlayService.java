package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.InviteMessageInfo;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;

public interface IGbChannelPlayService {

    void start(CommonGBChannel channel, InviteMessageInfo inviteInfo, Platform platform, ErrorCallback<StreamInfo> callback);

    void stopPlay(InviteSessionType type, CommonGBChannel channel, String stream);

    void play(CommonGBChannel channel, Platform platform, Boolean record, ErrorCallback<StreamInfo> callback);

    void playGbDeviceChannel(CommonGBChannel channel, Boolean record, ErrorCallback<StreamInfo> callback);

    void stopPlayDeviceChannel(InviteSessionType type, CommonGBChannel channel, String stream);

    void playProxy(CommonGBChannel channel, Boolean record, ErrorCallback<StreamInfo> callback);

    void stopPlayProxy(CommonGBChannel channel);

    void playPush(CommonGBChannel channel, String platformDeviceId, String platformName, ErrorCallback<StreamInfo> callback);

    void  stopPlayPush(CommonGBChannel channel);

    void pauseRtp(String streamId);

    void resumeRtp(String streamId);
}
