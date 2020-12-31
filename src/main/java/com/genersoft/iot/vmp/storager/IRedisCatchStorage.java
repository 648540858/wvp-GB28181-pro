package com.genersoft.iot.vmp.storager;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.MediaServerConfig;

import java.util.Map;

public interface IRedisCatchStorage {

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

    /**
     * 查询播放列表
     * @return
     */
    StreamInfo queryPlay(StreamInfo streamInfo);

    StreamInfo queryPlayByStreamId(String steamId);

    StreamInfo queryPlaybackByStreamId(String steamId);

    StreamInfo queryPlayByDevice(String deviceId, String code);

    /**
     * 更新流媒体信息
     * @param mediaServerConfig
     * @return
     */
    boolean updateMediaInfo(MediaServerConfig mediaServerConfig);

    /**
     * 获取流媒体信息
     * @return
     */
    MediaServerConfig getMediaInfo();

    Map<String, StreamInfo> queryPlayByDeviceId(String deviceId);

    boolean startPlayback(StreamInfo stream);

    boolean stopPlayback(StreamInfo streamInfo);

    StreamInfo queryPlaybackByDevice(String deviceId, String code);
}
