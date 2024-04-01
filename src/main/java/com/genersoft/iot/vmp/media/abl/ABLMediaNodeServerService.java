package com.genersoft.iot.vmp.media.abl;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.service.IMediaNodeServerService;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service("abl")
public class ABLMediaNodeServerService implements IMediaNodeServerService {

    @Override
    public int createRTPServer(MediaServer mediaServerItem, String streamId, long ssrc, Integer port, Boolean onlyAuto, Boolean reUsePort, Integer tcpMode) {
        return 0;
    }

    @Override
    public void closeRtpServer(MediaServer mediaServerItem, String streamId) {

    }

    @Override
    public void closeRtpServer(MediaServer mediaServerItem, String streamId, CommonCallback<Boolean> callback) {

    }

    @Override
    public void closeStreams(MediaServer mediaServerItem, String rtp, String streamId) {

    }

    @Override
    public Boolean updateRtpServerSSRC(MediaServer mediaServerItem, String streamId, String ssrc) {
        return null;
    }

    @Override
    public boolean checkNodeId(MediaServer mediaServerItem) {
        return false;
    }

    @Override
    public void online(MediaServer mediaServerItem) {

    }

    @Override
    public MediaServer checkMediaServer(String ip, int port, String secret) {
        return null;
    }

    @Override
    public boolean stopSendRtp(MediaServer mediaInfo, String app, String stream, String ssrc) {
        return false;
    }

    @Override
    public boolean deleteRecordDirectory(MediaServer mediaServerItem, String app, String stream, String date, String fileName) {
        return false;
    }

    @Override
    public List<StreamInfo> getMediaList(MediaServer mediaServerItem, String app, String stream, String callId) {
        return null;
    }

    @Override
    public Boolean connectRtpServer(MediaServer mediaServerItem, String address, int port, String stream) {
        return null;
    }

    @Override
    public void getSnap(MediaServer mediaServerItem, String streamUrl, int timeoutSec, int expireSec, String path, String fileName) {

    }

    @Override
    public MediaInfo getMediaInfo(MediaServer mediaServerItem, String app, String stream) {
        return null;
    }

    @Override
    public Boolean pauseRtpCheck(MediaServer mediaServer, String streamKey) {
        return null;
    }

    @Override
    public Boolean resumeRtpCheck(MediaServer mediaServer, String streamKey) {
        return null;
    }

    @Override
    public String getFfmpegCmd(MediaServer mediaServer, String cmdKey) {
        return null;
    }

    @Override
    public WVPResult<String> addFFmpegSource(MediaServer mediaServer, String srcUrl, String dstUrl, int timeoutMs, boolean enableAudio, boolean enableMp4, String ffmpegCmdKey) {
        return null;
    }

    @Override
    public WVPResult<String> addStreamProxy(MediaServer mediaServer, String app, String stream, String url, boolean enableAudio, boolean enableMp4, String rtpType) {
        return null;
    }

    @Override
    public Boolean delFFmpegSource(MediaServer mediaServer, String streamKey) {
        return null;
    }

    @Override
    public Boolean delStreamProxy(MediaServer mediaServer, String streamKey) {
        return null;
    }

    @Override
    public Map<String, String> getFFmpegCMDs(MediaServer mediaServer) {
        return null;
    }
}
