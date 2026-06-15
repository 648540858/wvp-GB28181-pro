package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.vmanager.bean.AudioTalkResult;

/**
 * 资源能力接入-语音对讲
 */
public interface ISourceBroadcastService {

    AudioTalkResult startTalk(CommonGBChannel channel);

    void stopTalk(CommonGBChannel channel);

    AudioTalkResult startBroadcast(CommonGBChannel channel);

    void stopBroadcast(CommonGBChannel channel);
}
