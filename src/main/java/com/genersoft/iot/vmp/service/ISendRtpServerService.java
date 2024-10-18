package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.gb28181.bean.SendRtpInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;

import java.util.List;

public interface ISendRtpServerService {

    SendRtpInfo createSendRtpInfo(MediaServer mediaServer, String ip, Integer port, String ssrc, String requesterId,
                                  String deviceId, Integer channelId, Boolean isTcp, Boolean rtcp);

    SendRtpInfo createSendRtpInfo(MediaServer mediaServer, String ip, Integer port, String ssrc, String platformId,
                                  String app, String stream, Integer channelId, Boolean tcp, Boolean rtcp);

    void update(SendRtpInfo sendRtpItem);

    SendRtpInfo queryByChannelId(Integer channelId, String targetId);

    SendRtpInfo queryByCallId(String callId);

    List<SendRtpInfo> queryByStream(String stream);

    SendRtpInfo queryByStream(String stream, String targetId);

    void delete(SendRtpInfo sendRtpInfo);

    void deleteByCallId(String callId);

    void deleteByStream(String Stream, String targetId);

    void deleteByChannel(Integer channelId, String targetId);

    List<SendRtpInfo> queryAll();

    boolean isChannelSendingRTP(Integer channelId);

    List<SendRtpInfo> queryForPlatform(String platformId);

    List<SendRtpInfo> queryByChannelId(int id);

    void deleteByStream(String stream);
}
