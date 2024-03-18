package com.genersoft.iot.vmp.media;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;

public interface IMediaNodeServerService {
    int createRTPServer(MediaServerItem mediaServerItem, String streamId, long ssrc, Integer port, Boolean onlyAuto, Boolean reUsePort, Integer tcpMode);

    void closeRtpServer(MediaServerItem mediaServerItem, String streamId);

    void closeRtpServer(MediaServerItem mediaServerItem, String streamId, CommonCallback<Boolean> callback);

    void closeStreams(MediaServerItem mediaServerItem, String rtp, String streamId);

    Boolean updateRtpServerSSRC(MediaServerItem mediaServerItem, String streamId, String ssrc);

    boolean checkNodeId(MediaServerItem mediaServerItem);

    void online(MediaServerItem mediaServerItem);
}
