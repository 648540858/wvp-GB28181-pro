package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.InviteInfo;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;

public interface IGbChannelPlayService {

    void start(CommonGBChannel channel, InviteInfo inviteInfo, Platform platform, ErrorCallback<StreamInfo> callback);

    void playGbDeviceChannel(CommonGBChannel channel, ErrorCallback<StreamInfo> callback);

    void playProxy(CommonGBChannel channel, ErrorCallback<StreamInfo> callback);

    void playPush(CommonGBChannel channel, String platformDeviceId, String platformName, ErrorCallback<StreamInfo> callback);
}
