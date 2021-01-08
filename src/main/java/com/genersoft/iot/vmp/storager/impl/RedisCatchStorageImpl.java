package com.genersoft.iot.vmp.storager.impl;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.MediaServerConfig;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Component
public class RedisCatchStorageImpl implements IRedisCatchStorage {

    @Autowired
	private RedisUtil redis;

    @Autowired
    private DeviceChannelMapper deviceChannelMapper;


    /**
     * 开始播放时将流存入redis
     *
     * @return
     */
    @Override
    public boolean startPlay(StreamInfo stream) {
        return redis.set(String.format("%S_%s_%s_%s", VideoManagerConstants.PLAYER_PREFIX, stream.getStreamId(),stream.getDeviceID(), stream.getChannelId()),
                stream);
    }

    /**
     * 停止播放时从redis删除
     *
     * @return
     */
    @Override
    public boolean stopPlay(StreamInfo streamInfo) {
        if (streamInfo == null) return false;
        return redis.del(String.format("%S_%s_%s_%s", VideoManagerConstants.PLAYER_PREFIX,
                streamInfo.getStreamId(),
                streamInfo.getDeviceID(),
                streamInfo.getChannelId()));
    }

    /**
     * 查询播放列表
     * @return
     */
    @Override
    public StreamInfo queryPlay(StreamInfo streamInfo) {
        return (StreamInfo)redis.get(String.format("%S_%s_%s_%s",
                VideoManagerConstants.PLAYER_PREFIX,
                streamInfo.getStreamId(),
                streamInfo.getDeviceID(),
                streamInfo.getChannelId()));
    }
    @Override
    public StreamInfo queryPlayByStreamId(String steamId) {
        List<Object> playLeys = redis.scan(String.format("%S_%s_*", VideoManagerConstants.PLAYER_PREFIX, steamId));
        if (playLeys == null || playLeys.size() == 0) return null;
        return (StreamInfo)redis.get(playLeys.get(0).toString());
    }

    @Override
    public StreamInfo queryPlaybackByStreamId(String steamId) {
        List<Object> playLeys = redis.scan(String.format("%S_%s_*", VideoManagerConstants.PLAY_BLACK_PREFIX, steamId));
        if (playLeys == null || playLeys.size() == 0) return null;
        return (StreamInfo)redis.get(playLeys.get(0).toString());
    }

    @Override
    public StreamInfo queryPlayByDevice(String deviceId, String code) {
//		List<Object> playLeys = redis.keys(String.format("%S_*_%s_%s", VideoManagerConstants.PLAYER_PREFIX,
        List<Object> playLeys = redis.scan(String.format("%S_*_%s_%s", VideoManagerConstants.PLAYER_PREFIX,
                deviceId,
                code));
        if (playLeys == null || playLeys.size() == 0) return null;
        return (StreamInfo)redis.get(playLeys.get(0).toString());
    }

    /**
     * 更新流媒体信息
     * @param mediaServerConfig
     * @return
     */
    @Override
    public boolean updateMediaInfo(MediaServerConfig mediaServerConfig) {
        return redis.set(VideoManagerConstants.MEDIA_SERVER_PREFIX,mediaServerConfig);
    }

    /**
     * 获取流媒体信息
     * @return
     */
    @Override
    public MediaServerConfig getMediaInfo() {
        return (MediaServerConfig)redis.get(VideoManagerConstants.MEDIA_SERVER_PREFIX);
    }

    @Override
    public Map<String, StreamInfo> queryPlayByDeviceId(String deviceId) {
        Map<String, StreamInfo> streamInfos = new HashMap<>();
//		List<Object> playLeys = redis.keys(String.format("%S_*_%S_*", VideoManagerConstants.PLAYER_PREFIX, deviceId));
        List<Object> players = redis.scan(String.format("%S_*_%S_*", VideoManagerConstants.PLAYER_PREFIX, deviceId));
        if (players.size() == 0) return streamInfos;
        for (int i = 0; i < players.size(); i++) {
            String key = (String) players.get(i);
            StreamInfo streamInfo = (StreamInfo)redis.get(key);
            streamInfos.put(streamInfo.getDeviceID() + "_" + streamInfo.getChannelId(), streamInfo);
        }
        return streamInfos;
    }


    @Override
    public boolean startPlayback(StreamInfo stream) {
        return redis.set(String.format("%S_%s_%s_%s", VideoManagerConstants.PLAY_BLACK_PREFIX, stream.getStreamId(),stream.getDeviceID(), stream.getChannelId()),
                stream);
    }


    @Override
    public boolean stopPlayback(StreamInfo streamInfo) {
        if (streamInfo == null) return false;
        DeviceChannel deviceChannel = deviceChannelMapper.queryChannel(streamInfo.getDeviceID(), streamInfo.getChannelId());
        if (deviceChannel != null) {
            deviceChannel.setStreamId(null);
            deviceChannel.setDeviceId(streamInfo.getDeviceID());
            deviceChannelMapper.update(deviceChannel);
        }
        return redis.del(String.format("%S_%s_%s_%s", VideoManagerConstants.PLAY_BLACK_PREFIX,
                streamInfo.getStreamId(),
                streamInfo.getDeviceID(),
                streamInfo.getChannelId()));
    }

    @Override
    public StreamInfo queryPlaybackByDevice(String deviceId, String code) {
        String format = String.format("%S_*_%s_%s", VideoManagerConstants.PLAY_BLACK_PREFIX,
                deviceId,
                code);
        List<Object> playLeys = redis.scan(String.format("%S_*_%s_%s", VideoManagerConstants.PLAY_BLACK_PREFIX,
                deviceId,
                code));
        if (playLeys == null || playLeys.size() == 0) {
            playLeys = redis.scan(String.format("%S_*_*_%s", VideoManagerConstants.PLAY_BLACK_PREFIX,
                    deviceId));
        }
        if (playLeys == null || playLeys.size() == 0) return null;
        return (StreamInfo)redis.get(playLeys.get(0).toString());
    }
}
