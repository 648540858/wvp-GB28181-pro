package com.genersoft.iot.vmp.media.abl;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.media.service.IMediaNodeServerService;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("abl")
public class ABLMediaNodeServerService implements IMediaNodeServerService {

    @Override
    public int createRTPServer(MediaServerItem mediaServerItem, String streamId, long ssrc, Integer port, Boolean onlyAuto, Boolean reUsePort, Integer tcpMode) {
        return 0;
    }

    @Override
    public void closeRtpServer(MediaServerItem mediaServerItem, String streamId) {

    }

    @Override
    public void closeRtpServer(MediaServerItem mediaServerItem, String streamId, CommonCallback<Boolean> callback) {

    }

    @Override
    public void closeStreams(MediaServerItem mediaServerItem, String rtp, String streamId) {

    }

    @Override
    public Boolean updateRtpServerSSRC(MediaServerItem mediaServerItem, String streamId, String ssrc) {
        return null;
    }

    @Override
    public boolean checkNodeId(MediaServerItem mediaServerItem) {
        return false;
    }

    @Override
    public void online(MediaServerItem mediaServerItem) {

    }

    @Override
    public MediaServerItem checkMediaServer(String ip, int port, String secret) {
        return null;
    }

    @Override
    public boolean stopSendRtp(MediaServerItem mediaInfo, String app, String stream, String ssrc) {
        return false;
    }

    @Override
    public boolean deleteRecordDirectory(MediaServerItem mediaServerItem, String app, String stream, String date, String fileName) {
        return false;
    }

    @Override
    public List<StreamInfo> getMediaList(MediaServerItem mediaServerItem, String app, String stream, String callId) {
        return null;
    }
}
