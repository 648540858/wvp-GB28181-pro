package com.genersoft.iot.vmp.media.service;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServer;
import com.genersoft.iot.vmp.service.bean.MediaServerLoad;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;

import java.util.List;
import java.util.Map;

/**
 * 媒体服务节点
 */
public interface IMediaServerService {

    List<MediaServer> getAllOnlineList();

    List<MediaServer> getAll();

    List<MediaServer> getAllFromDatabase();

    List<MediaServer> getAllOnline();

    MediaServer getOne(String generalMediaServerId);

    void syncCatchFromDatabase();

    MediaServer getMediaServerForMinimumLoad(Boolean hasAssist);

    void updateVmServer(List<MediaServer>  mediaServerItemList);

    SSRCInfo openRTPServer(MediaServer mediaServerItem, String streamId, String presetSsrc, boolean ssrcCheck,
                           boolean isPlayback, Integer port, Boolean onlyAuto, Boolean reUsePort, Integer tcpMode);

    SSRCInfo openRTPServer(MediaServer mediaServerItem, String streamId, String ssrc, boolean ssrcCheck, boolean isPlayback, Integer port, Boolean onlyAuto);

    void closeRTPServer(MediaServer mediaServerItem, String streamId);

    void closeRTPServer(MediaServer mediaServerItem, String streamId, CommonCallback<Boolean> callback);
    Boolean updateRtpServerSSRC(MediaServer mediaServerItem, String streamId, String ssrc);

    void closeRTPServer(String mediaServerId, String streamId);

    void clearRTPServer(MediaServer mediaServerItem);

    void update(MediaServer mediaSerItem);

    void addCount(String mediaServerId);

    void removeCount(String mediaServerId);

    void releaseSsrc(String mediaServerItemId, String ssrc);

    void clearMediaServerForOnline();

    void add(MediaServer mediaSerItem);

    void resetOnlineServerItem(MediaServer serverItem);

    MediaServer checkMediaServer(String ip, int port, String secret, String type);

    boolean checkMediaRecordServer(String ip, int port);

    void delete(String id);

    MediaServer getDefaultMediaServer();

    MediaServerLoad getLoad(MediaServer mediaServerItem);

    List<MediaServer> getAllWithAssistPort();

    MediaServer getOneFromDatabase(String id);

    boolean stopSendRtp(MediaServer mediaInfo, String app, String stream, String ssrc);

    boolean deleteRecordDirectory(MediaServer mediaServerItem, String app, String stream, String date, String fileName);

    List<StreamInfo> getMediaList(MediaServer mediaInfo, String app, String stream, String callId);

    Boolean connectRtpServer(MediaServer mediaServerItem, String address, int port, String stream);

    void getSnap(MediaServer mediaServerItemInuse, String streamUrl, int timeoutSec, int expireSec, String path, String fileName);

    MediaInfo getMediaInfo(MediaServer mediaServerItem, String app, String stream);

    Boolean pauseRtpCheck(MediaServer mediaServerItem, String streamKey);

    boolean resumeRtpCheck(MediaServer mediaServerItem, String streamKey);

    String getFfmpegCmd(MediaServer mediaServer, String cmdKey);

    void closeStreams(MediaServer mediaServerItem, String app, String stream);

    WVPResult<String> addFFmpegSource(MediaServer mediaServerItem, String srcUrl, String dstUrl, int timeoutMs, boolean enableAudio, boolean enableMp4, String ffmpegCmdKey);

    WVPResult<String> addStreamProxy(MediaServer mediaServerItem, String app, String stream, String url, boolean enableAudio, boolean enableMp4, String rtpType);

    Boolean delFFmpegSource(MediaServer mediaServerItem, String streamKey);

    Boolean delStreamProxy(MediaServer mediaServerItem, String streamKey);

    Map<String, String> getFFmpegCMDs(MediaServer mediaServer);

}
