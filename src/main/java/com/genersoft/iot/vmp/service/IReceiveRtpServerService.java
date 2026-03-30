package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.OpenRTPServerResult;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.hook.HookData;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.RTPServerParam;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;

public interface IReceiveRtpServerService {

    SSRCInfo openGbRTPServer(MediaServer mediaServer, String streamId, String presetSSRC, int tcpMode,
                             boolean playback, boolean ssrcCheck, boolean onlyAuto, boolean disableAuto,
                             ErrorCallback<OpenRTPServerResult> callback);

    int openRTPServer(RTPServerParam rtpServerParam, ErrorCallback<HookData> callback);

    void closeRTPServer(MediaServer mediaServer, String app, String stream);

    void closeRTPServerByMediaServerId(String mediaServerId, String app, String stream);
}
