package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.gb28181.bean.SendRtpInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;

import java.util.List;

public interface ISendRtpServerService {

    SendRtpInfo createSendRtpInfo(MediaServer mediaServer, String ip, int port, String ssrc, String requesterId,
                                  String deviceId, Integer channelId, boolean isTcp, boolean rtcp);

    SendRtpInfo createSendRtpInfo(MediaServer mediaServer, String ip, int port, String ssrc, String platformId,
                                  String app, String stream, Integer channelId, boolean tcp, boolean rtcp);

    void update(SendRtpInfo sendRtpItem);

    SendRtpInfo queryByChannelId(Integer channelId);

    SendRtpInfo queryByCallId(String callId);

    SendRtpInfo queryByStream(String stream);

    void delete(SendRtpInfo sendRtpInfo);

    void deleteByCallId(String callId);

    void deleteByStream(String Stream);

    void deleteByChannel(Integer channelId);

    List<SendRtpInfo> queryAll();

    boolean isChannelSendingRTP(Integer channelId);
}
