package com.genersoft.iot.vmp.service.redisMsg;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;

public interface IRedisRpcService {

    SendRtpItem getSendRtpItem(String sendRtpItemKey);

    WVPResult startSendRtp(String sendRtpItemKey, SendRtpItem sendRtpItem);

    WVPResult stopSendRtp(String sendRtpItemKey);

    long waitePushStreamOnline(SendRtpItem sendRtpItem, CommonCallback<String> callback);

    void stopWaitePushStreamOnline(SendRtpItem sendRtpItem);

    void rtpSendStopped(String sendRtpItemKey);

    void removeCallback(long key);

    long onStreamOnlineEvent(String app, String stream, CommonCallback<StreamInfo> callback);
    void unPushStreamOnlineEvent(String app, String stream);
}
