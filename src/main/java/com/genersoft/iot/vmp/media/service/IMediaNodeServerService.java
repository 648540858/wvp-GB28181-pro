package com.genersoft.iot.vmp.media.service;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;

import java.util.List;

public interface IMediaNodeServerService {
    int createRTPServer(MediaServerItem mediaServerItem, String streamId, long ssrc, Integer port, Boolean onlyAuto, Boolean reUsePort, Integer tcpMode);

    void closeRtpServer(MediaServerItem mediaServerItem, String streamId);

    void closeRtpServer(MediaServerItem mediaServerItem, String streamId, CommonCallback<Boolean> callback);

    void closeStreams(MediaServerItem mediaServerItem, String app, String stream);

    Boolean updateRtpServerSSRC(MediaServerItem mediaServerItem, String stream, String ssrc);

    boolean checkNodeId(MediaServerItem mediaServerItem);

    void online(MediaServerItem mediaServerItem);

    MediaServerItem checkMediaServer(String ip, int port, String secret);

    boolean stopSendRtp(MediaServerItem mediaInfo, String app, String stream, String ssrc);

    boolean deleteRecordDirectory(MediaServerItem mediaServerItem, String app, String stream, String date, String fileName);

    List<StreamInfo> getMediaList(MediaServerItem mediaServerItem, String app, String stream);
}
