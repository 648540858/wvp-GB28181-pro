package com.genersoft.iot.vmp.media.abl;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.service.IMediaNodeServerService;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServer;
import org.springframework.stereotype.Service;

import java.util.List;

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
}
