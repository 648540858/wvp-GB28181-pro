package com.genersoft.iot.vmp.storager;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.MediaServerConfig;

import java.util.List;

public interface IRedisCatchStorage {

    /**
     * 更新流媒体信息
     *
     * @param mediaServerConfig
     * @return
     */
    boolean updateMediaInfo(MediaServerConfig mediaServerConfig);

    /**
     * 获取流媒体信息
     *
     * @return
     */
    MediaServerConfig getMediaInfo();

    /**
     * 开始播放时将流存入
     *
     * @param stream 流信息
     * @return
     */
    boolean startPlay(StreamInfo stream);


    /**
     * 停止播放时删除
     *
     * @return
     */
    boolean stopPlay(StreamInfo streamInfo);

    StreamInfo queryPlayByStreamId(String channelId, String steamId);

    StreamInfo queryPlayByChannel(String channelId);

    boolean startPlayback(StreamInfo stream);

    boolean stopPlayback(StreamInfo streamInfo);

    StreamInfo queryPlaybackByStreamId(String channelId, String steamId);

    StreamInfo queryPlaybackByChannel(String channelId);

    List<StreamInfo> queryPlayBackByDeviceId(String deviceId);
}
