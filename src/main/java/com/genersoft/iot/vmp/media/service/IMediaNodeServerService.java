package com.genersoft.iot.vmp.media.service;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpInfo;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxy;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;

import java.util.List;
import java.util.Map;

public interface IMediaNodeServerService {
    int createRTPServer(MediaServer mediaServer, String streamId, long ssrc, Integer port, Boolean onlyAuto, Boolean disableAudio, Boolean reUsePort, Integer tcpMode);

    void closeRtpServer(MediaServer mediaServer, String streamId);

    void closeRtpServer(MediaServer mediaServer, String streamId, CommonCallback<Boolean> callback);

    void closeStreams(MediaServer mediaServer, String app, String stream);

    Boolean updateRtpServerSSRC(MediaServer mediaServer, String stream, String ssrc);

    boolean checkNodeId(MediaServer mediaServer);

    void online(MediaServer mediaServer);

    MediaServer checkMediaServer(String ip, int port, String secret);

    boolean stopSendRtp(MediaServer mediaInfo, String app, String stream, String ssrc);

    boolean initStopSendRtp(MediaServer mediaInfo, String app, String stream, String ssrc);

    boolean deleteRecordDirectory(MediaServer mediaServer, String app, String stream, String date, String fileName);

    List<StreamInfo> getMediaList(MediaServer mediaServer, String app, String stream, String callId);

    Boolean connectRtpServer(MediaServer mediaServer, String address, int port, String stream);

    void getSnap(MediaServer mediaServer, String streamUrl, int timeoutSec, int expireSec, String path, String fileName);

    MediaInfo getMediaInfo(MediaServer mediaServer, String app, String stream);

    Boolean pauseRtpCheck(MediaServer mediaServer, String streamKey);

    Boolean resumeRtpCheck(MediaServer mediaServer, String streamKey);

    String getFfmpegCmd(MediaServer mediaServer, String cmdKey);

    WVPResult<String> addFFmpegSource(MediaServer mediaServer, String srcUrl, String dstUrl, int timeoutMs, boolean enableAudio, boolean enableMp4, String ffmpegCmdKey);

    WVPResult<String> addStreamProxy(MediaServer mediaServer, String app, String stream, String url, boolean enableAudio, boolean enableMp4, String rtpType, Integer timeout);

    Boolean delFFmpegSource(MediaServer mediaServer, String streamKey);

    Boolean delStreamProxy(MediaServer mediaServer, String streamKey);

    Map<String, String> getFFmpegCMDs(MediaServer mediaServer);

    Integer startSendRtpPassive(MediaServer mediaServer, SendRtpInfo sendRtpItem, Integer timeout);

    void startSendRtpStream(MediaServer mediaServer, SendRtpInfo sendRtpItem);

    Long updateDownloadProcess(MediaServer mediaServer, String app, String stream);

    StreamInfo startProxy(MediaServer mediaServer, StreamProxy streamProxy);

    void stopProxy(MediaServer mediaServer, String streamKey);

    List<String> listRtpServer(MediaServer mediaServer);

    void loadMP4File(MediaServer mediaServer, String app, String stream, String datePath);

    void seekRecordStamp(MediaServer mediaServer, String app, String stream, Double stamp, String schema);

    void setRecordSpeed(MediaServer mediaServer, String app, String stream, Integer speed, String schema);
}
