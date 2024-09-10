package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.gb28181.bean.OpenRTPServerResult;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.RTPServerParam;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;

public interface IReceiveRtpServerService {
    SSRCInfo openRTPServer(RTPServerParam rtpServerParam, ErrorCallback<OpenRTPServerResult> callback);

    void closeRTPServer(MediaServer mediaServer, SSRCInfo ssrcInfo);
}
