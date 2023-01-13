package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.media.zlm.ZLMServerConfig;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.ServerKeepaliveData;
import com.genersoft.iot.vmp.service.bean.MediaServerLoad;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;

import java.util.List;

/**
 * 媒体服务节点
 */
public interface IMediaServerService {

    List<MediaServerItem> getAll();

    List<MediaServerItem> getAllFromDatabase();

    List<MediaServerItem> getAllOnline();

    MediaServerItem getOne(String generalMediaServerId);

    void syncCatchFromDatabase();

    /**
     * 新的节点加入
     * @param zlmServerConfig
     * @return
     */
    void zlmServerOnline(ZLMServerConfig zlmServerConfig);

    /**
     * 节点离线
     * @param mediaServerId
     * @return
     */
    void zlmServerOffline(String mediaServerId);

    MediaServerItem getMediaServerForMinimumLoad(Boolean hasAssist);

    void setZLMConfig(MediaServerItem mediaServerItem, boolean restart);

    void updateVmServer(List<MediaServerItem>  mediaServerItemList);

    SSRCInfo openRTPServer(MediaServerItem mediaServerItem, String streamId, boolean ssrcCheck, boolean isPlayback);

    SSRCInfo openRTPServer(MediaServerItem mediaServerItem, String streamId, String ssrc, boolean ssrcCheck, boolean isPlayback);

    SSRCInfo openRTPServer(MediaServerItem mediaServerItem, String streamId, String ssrc, boolean ssrcCheck, boolean isPlayback, Integer port);

    void closeRTPServer(MediaServerItem mediaServerItem, String streamId);

    void closeRTPServer(String mediaServerId, String streamId);

    void clearRTPServer(MediaServerItem mediaServerItem);

    void update(MediaServerItem mediaSerItem);

    void addCount(String mediaServerId);

    void removeCount(String mediaServerId);

    void releaseSsrc(String mediaServerItemId, String ssrc);

    void clearMediaServerForOnline();

    void add(MediaServerItem mediaSerItem);

    int addToDatabase(MediaServerItem mediaSerItem);

    int updateToDatabase(MediaServerItem mediaSerItem);

    void resetOnlineServerItem(MediaServerItem serverItem);

    MediaServerItem checkMediaServer(String ip, int port, String secret);

    boolean checkMediaRecordServer(String ip, int port);

    void delete(String id);

    void deleteDb(String id);

    MediaServerItem getDefaultMediaServer();

    void updateMediaServerKeepalive(String mediaServerId, ServerKeepaliveData data);

    boolean checkRtpServer(MediaServerItem mediaServerItem, String rtp, String stream);

    /**
     * 获取负载信息
     * @return
     */
    MediaServerLoad getLoad(MediaServerItem mediaServerItem);
}
