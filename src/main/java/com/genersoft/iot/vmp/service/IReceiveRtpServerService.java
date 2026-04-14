package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.hook.HookData;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.RTPServerParam;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;

public interface IReceiveRtpServerService {

    SSRCInfo openGbRTPServer(MediaServer mediaServer, String streamId, String presetSSRC, int tcpMode,
                             boolean playback, boolean ssrcCheck, boolean onlyAuto, boolean disableAuto,
                             ErrorCallback<OpenRTPServerResult> callback);

    SSRCInfo openGbRTPServerForPlay(MediaServer mediaServer, Device device, DeviceChannel channel,
                                    String presetSSRC, ErrorCallback<OpenRTPServerResult> callback);

    SSRCInfo openGbRTPServerForPlayback(MediaServer mediaServer, Device device, DeviceChannel channel,
                                        String startTime, String endTime, ErrorCallback<OpenRTPServerResult> callback);

    SSRCInfo openGbRTPServerForDownload(MediaServer mediaServer, Device device, DeviceChannel channel,
                                        ErrorCallback<OpenRTPServerResult> callback);

    SSRCInfo openGbRTPServerForBroadcast(MediaServer mediaServer, Platform platform, CommonGBChannel channel,
                                         ErrorCallback<OpenRTPServerResult> callback);

    int openCommonRTPServer(RTPServerParam rtpServerParam, ErrorCallback<HookData> callback);

    void closeRTPServer(MediaServer mediaServer, String app, String stream);

    void closeRTPServerByMediaServerId(String mediaServerId, String app, String stream);
}
