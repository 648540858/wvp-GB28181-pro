package com.genersoft.iot.vmp.media.service;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServer;

import java.util.List;

public interface IMediaNodeServerService {
    int createRTPServer(MediaServer mediaServerItem, String streamId, long ssrc, Integer port, Boolean onlyAuto, Boolean reUsePort, Integer tcpMode);

    void closeRtpServer(MediaServer mediaServerItem, String streamId);

    void closeRtpServer(MediaServer mediaServerItem, String streamId, CommonCallback<Boolean> callback);

    void closeStreams(MediaServer mediaServerItem, String app, String stream);

    Boolean updateRtpServerSSRC(MediaServer mediaServerItem, String stream, String ssrc);

    boolean checkNodeId(MediaServer mediaServerItem);

    void online(MediaServer mediaServerItem);

    MediaServer checkMediaServer(String ip, int port, String secret);

    boolean stopSendRtp(MediaServer mediaInfo, String app, String stream, String ssrc);

    boolean deleteRecordDirectory(MediaServer mediaServerItem, String app, String stream, String date, String fileName);

    List<StreamInfo> getMediaList(MediaServer mediaServerItem, String app, String stream, String callId);

    Boolean connectRtpServer(MediaServer mediaServerItem, String address, int port, String stream);

    void getSnap(MediaServer mediaServerItem, String streamUrl, int timeoutSec, int expireSec, String path, String fileName);

    MediaInfo getMediaInfo(MediaServer mediaServerItem, String app, String stream);

    Boolean pauseRtpCheck(MediaServer mediaServerItem, String streamKey);

    Boolean resumeRtpCheck(MediaServer mediaServerItem, String streamKey);
}
