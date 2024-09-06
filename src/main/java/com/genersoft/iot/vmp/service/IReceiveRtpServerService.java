package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.hook.HookData;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.RTPServerParam;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;

public interface IReceiveRtpServerService {
    SSRCInfo openRTPServer(RTPServerParam rtpServerParam, ErrorCallback<HookData> callback);

    void closeRTPServer(MediaServer mediaServer, String stream);
}
