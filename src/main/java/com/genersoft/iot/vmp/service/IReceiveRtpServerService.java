package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.bean.ResultForOnPublish;
import com.genersoft.iot.vmp.media.event.hook.HookData;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.RTPServerParam;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;

public interface IReceiveRtpServerService {

    SSRCInfo openGbRTPServer(MediaServer mediaServer, String streamId, String presetSSRC, int tcpMode,
                             boolean playback, boolean ssrcCheck, boolean onlyAuto, boolean disableAuto,
                             ErrorCallback<OpenRTPServerResult> callback);

    SSRCInfo openGbRTPServerForPlay(MediaServer mediaServer, Device device, DeviceChannel channel,
                                    String presetSSRC, boolean record, ErrorCallback<OpenRTPServerResult> callback);

    SSRCInfo openGbRTPServerForPlayback(MediaServer mediaServer, Device device, DeviceChannel channel,
                                        String startTime, String endTime, ErrorCallback<OpenRTPServerResult> callback);

    /** 为指定播放器会话创建RTP接收器，确保媒体重命名与逻辑会话一致。 */
    SSRCInfo openGbRTPServerForPlayback(MediaServer mediaServer, Device device, DeviceChannel channel,
                                      String startTime, String endTime, String sessionId,
                                      ErrorCallback<OpenRTPServerResult> callback);

    SSRCInfo openGbRTPServerForDownload(MediaServer mediaServer, Device device, DeviceChannel channel,
                                        String startTime, String endTime, ErrorCallback<OpenRTPServerResult> callback);

    String getPlaybackStream(Device device, DeviceChannel channel, String startTime, String endTime);

    /** 生成独立回放流名，空会话标识保留旧四段格式。 */
    String getPlaybackStream(Device device, DeviceChannel channel, String startTime, String endTime, String sessionId);

    SSRCInfo openGbRTPServerForBroadcast(MediaServer mediaServer, Platform platform, CommonGBChannel channel,
                                         ErrorCallback<OpenRTPServerResult> callback);

    int openCommonRTPServer(RTPServerParam rtpServerParam, ErrorCallback<HookData> callback);

    void closeRTPServer(MediaServer mediaServer, String app, String stream);

    void closeRTPServerByMediaServerId(String mediaServerId, String app, String stream);

    void addAuthenticateInfoForGb28181Talk(MediaServer mediaServer, String streamId);

    void addAuthenticateInfo(String streamId, String streamReplace, Boolean enableAudio, Boolean enableMp4, Integer mp4MaxSecond);

    ResultForOnPublish getAuthenticateInfo(String streamId);

    void refreshAuthenticateInfo(String oldStreamId, String newStreamId);
}
