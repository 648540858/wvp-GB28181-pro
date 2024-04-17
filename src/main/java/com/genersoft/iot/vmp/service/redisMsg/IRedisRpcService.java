package com.genersoft.iot.vmp.service.redisMsg;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;

public interface IRedisRpcService {

    SendRtpItem getSendRtpItem(SendRtpItem sendRtpItem);

    WVPResult startSendRtp(SendRtpItem sendRtpItem);

    void waitePushStreamOnline(SendRtpItem sendRtpItem, CommonCallback<SendRtpItem> callback);

    WVPResult stopSendRtp(SendRtpItem sendRtpItem);

    void stopWaitePushStreamOnline(SendRtpItem sendRtpItem);

    void rtpSendStopped(SendRtpItem sendRtpItem);

}
