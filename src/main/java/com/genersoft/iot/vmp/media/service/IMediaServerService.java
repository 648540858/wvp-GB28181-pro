package com.genersoft.iot.vmp.media.service;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.bean.MediaServerLoad;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;

import java.util.List;

/**
 * 媒体服务节点
 */
public interface IMediaServerService {

    List<MediaServerItem> getAllOnlineList();

    List<MediaServerItem> getAll();

    List<MediaServerItem> getAllFromDatabase();

    List<MediaServerItem> getAllOnline();

    MediaServerItem getOne(String generalMediaServerId);

    void syncCatchFromDatabase();

    MediaServerItem getMediaServerForMinimumLoad(Boolean hasAssist);

    void updateVmServer(List<MediaServerItem>  mediaServerItemList);

    SSRCInfo openRTPServer(MediaServerItem mediaServerItem, String streamId, String presetSsrc, boolean ssrcCheck,
                           boolean isPlayback, Integer port, Boolean onlyAuto, Boolean reUsePort, Integer tcpMode);

    SSRCInfo openRTPServer(MediaServerItem mediaServerItem, String streamId, String ssrc, boolean ssrcCheck, boolean isPlayback, Integer port, Boolean onlyAuto);

    void closeRTPServer(MediaServerItem mediaServerItem, String streamId);

    void closeRTPServer(MediaServerItem mediaServerItem, String streamId, CommonCallback<Boolean> callback);
    Boolean updateRtpServerSSRC(MediaServerItem mediaServerItem, String streamId, String ssrc);

    void closeRTPServer(String mediaServerId, String streamId);

    void clearRTPServer(MediaServerItem mediaServerItem);

    void update(MediaServerItem mediaSerItem);

    void addCount(String mediaServerId);

    void removeCount(String mediaServerId);

    void releaseSsrc(String mediaServerItemId, String ssrc);

    void clearMediaServerForOnline();

    void add(MediaServerItem mediaSerItem);

    void resetOnlineServerItem(MediaServerItem serverItem);

    MediaServerItem checkMediaServer(String ip, int port, String secret, String type);

    boolean checkMediaRecordServer(String ip, int port);

    void delete(String id);

    MediaServerItem getDefaultMediaServer();

    MediaServerLoad getLoad(MediaServerItem mediaServerItem);

    List<MediaServerItem> getAllWithAssistPort();

    MediaServerItem getOneFromDatabase(String id);

    boolean stopSendRtp(MediaServerItem mediaInfo, String app, String stream, String ssrc);

    boolean deleteRecordDirectory(MediaServerItem mediaServerItem, String app, String stream, String date, String fileName);

    List<StreamInfo> getMediaList(MediaServerItem mediaInfo, String app, String stream, String callId);
}
