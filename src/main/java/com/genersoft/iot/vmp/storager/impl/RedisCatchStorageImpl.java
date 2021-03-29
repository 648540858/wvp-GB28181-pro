package com.genersoft.iot.vmp.storager.impl;

import com.genersoft.iot.vmp.common.RealVideo;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.MediaServerConfig;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatformCatch;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SuppressWarnings("rawtypes")
@Component
public class RedisCatchStorageImpl implements IRedisCatchStorage {

    @Autowired
    private RedisUtil redis;

    @Autowired
    private DeviceChannelMapper deviceChannelMapper;

    /**
     * 更新流媒体信息
     *
     * @param mediaServerConfig
     * @return
     */
    @Override
    public boolean updateMediaInfo(MediaServerConfig mediaServerConfig) {
        return redis.set(VideoManagerConstants.MEDIA_SERVER_PREFIX, mediaServerConfig);
    }

    /**
     * 获取流媒体信息
     *
     * @return
     */
    @Override
    public MediaServerConfig getMediaInfo() {
        return (MediaServerConfig) redis.get(VideoManagerConstants.MEDIA_SERVER_PREFIX);
    }


    /**
     * 开始播放时将流存入redis
     *
     * @return
     */
    @Override
    public boolean startPlay(StreamInfo stream) {
        String key = getKey(VideoManagerConstants.PLAYER_PREFIX,
                stream.getStreamId(),
                stream.getChannelId(),
                stream.getDeviceID()
        );
        return redis.set(key, stream);
    }

    /**
     * 停止播放时从redis删除
     *
     * @return
     */
    @Override
    public boolean stopPlay(StreamInfo streamInfo) {
        if (streamInfo == null) {
            return false;
        }
        String key = getKey(VideoManagerConstants.PLAYER_PREFIX,
                streamInfo.getStreamId(),
                streamInfo.getChannelId(),
                streamInfo.getDeviceID()
        );
        return redis.del(key);
    }

    @Override
    public StreamInfo queryPlayByStreamId(String channelId, String steamId) {
        String key = getKey(VideoManagerConstants.PLAYER_PREFIX,
                steamId,
                channelId,
                null
        );
        return scanOne(key);
    }

    @Override
    public StreamInfo queryPlayByChannel(String channelId) {
        String key = getKey(VideoManagerConstants.PLAYER_PREFIX,
                null,
                channelId,
                null
        );
        return scanOne(key);
    }

    /**
     * zlm流媒体服务器播流成功后回调，播放状态缓存到redis
     *
     * @param stream
     * @return
     */
    @Override
    public boolean startPlayback(StreamInfo stream) {
        String key = getKey(VideoManagerConstants.PLAY_BLACK_PREFIX,
                stream.getStreamId(),
                stream.getChannelId(),
                stream.getDeviceID()
        );
        return redis.set(key, stream);
    }

    /**
     * zlm流媒体服务器成功停止拉流后回调，从redis缓存移除播放状态
     *
     * @param streamInfo
     * @return
     */
    @Override
    public boolean stopPlayback(StreamInfo streamInfo) {
        if (streamInfo == null) {
            return false;
        }
        DeviceChannel deviceChannel = deviceChannelMapper.queryChannel(streamInfo.getDeviceID(), streamInfo.getChannelId());
        if (deviceChannel != null) {
            deviceChannel.setStreamId(null);
            deviceChannel.setDeviceId(streamInfo.getDeviceID());
            deviceChannelMapper.update(deviceChannel);
        }
        String key = getKey(VideoManagerConstants.PLAY_BLACK_PREFIX,
                streamInfo.getStreamId(),
                streamInfo.getChannelId(),
                streamInfo.getDeviceID()
        );
        return redis.del(key);
    }

    @Override
    public StreamInfo queryPlaybackByDevice(String deviceId, String channelId) {
        String keyByChannel = getKey(VideoManagerConstants.PLAY_BLACK_PREFIX,
                null,
                channelId,
                deviceId
        );
        List<Object> playLeys = redis.scan(keyByChannel);
        if (playLeys == null || playLeys.size() == 0) {
            String keyByDevice = getKey(VideoManagerConstants.PLAY_BLACK_PREFIX,
                    null,
                    null,
                    deviceId
            );
            playLeys = redis.scan(keyByDevice);
        }
        if (playLeys == null || playLeys.size() == 0) {
            return null;
        }
        return (StreamInfo) redis.get(playLeys.get(0).toString());
    }

    @Override
    public StreamInfo queryPlaybackByStreamId(String channelId, String steamId) {
        String key = getKey(VideoManagerConstants.PLAY_BLACK_PREFIX,
                steamId,
                channelId,
                null
        );
        return scanOne(key);
    }

    @Override
    public StreamInfo queryPlaybackByChannel(String channelId) {

        String key = getKey(VideoManagerConstants.PLAY_BLACK_PREFIX,
                null,
                channelId,
                null
        );
        return scanOne(key);
    }

    @Override
    public List<StreamInfo> queryPlayBackByDeviceId(String deviceId) {
        String key = getKey(VideoManagerConstants.PLAY_BLACK_PREFIX,
                null,
                null,
                deviceId
        );
        List<Object> players = redis.scan(key);
        if (players.size() == 0) {
            return new ArrayList<>();
        }
        List<StreamInfo> streamInfos = new ArrayList<>(players.size());
        for (int i = 0; i < players.size(); i++) {
            String redisKey = (String) players.get(i);
            StreamInfo streamInfo = (StreamInfo) redis.get(redisKey);
            streamInfos.add(streamInfo);
        }
        return streamInfos;
    }

    private StreamInfo scanOne(String key) {
        List<Object> playLeys = redis.scan(key);
        if (playLeys == null || playLeys.size() == 0) {
            return null;
        }
        return (StreamInfo) redis.get(playLeys.get(0).toString());
    }

    @Override
    public void updatePlatformCatchInfo(ParentPlatformCatch parentPlatformCatch) {
        String key = VideoManagerConstants.PLATFORM_CATCH_PREFIX + parentPlatformCatch.getId();
        redis.set(key, parentPlatformCatch);
    }

    @Override
    public void updatePlatformKeepalive(ParentPlatform parentPlatform) {
        String key = VideoManagerConstants.PLATFORM_KEEPLIVEKEY_PREFIX + parentPlatform.getServerGBId();
        redis.set(key, "", Integer.parseInt(parentPlatform.getKeepTimeout()));
    }

    @Override
    public void updatePlatformRegister(ParentPlatform parentPlatform) {
        String key = VideoManagerConstants.PLATFORM_REGISTER_PREFIX + parentPlatform.getServerGBId();
        redis.set(key, "", Integer.parseInt(parentPlatform.getExpires()));
    }

    @Override
    public ParentPlatformCatch queryPlatformCatchInfo(String platformGbId) {
        return (ParentPlatformCatch) redis.get(VideoManagerConstants.PLATFORM_CATCH_PREFIX + platformGbId);
    }

    @Override
    public void delPlatformCatchInfo(String platformGbId) {
        redis.del(VideoManagerConstants.PLATFORM_CATCH_PREFIX + platformGbId);
    }

    @Override
    public void delPlatformKeepalive(String platformGbId) {
        redis.del(VideoManagerConstants.PLATFORM_KEEPLIVEKEY_PREFIX + platformGbId);
    }

    @Override
    public void delPlatformRegister(String platformGbId) {
        redis.del(VideoManagerConstants.PLATFORM_REGISTER_PREFIX + platformGbId);
    }


    @Override
    public void updatePlatformRegisterInfo(String callId, String platformGbId) {
        String key = VideoManagerConstants.PLATFORM_REGISTER_INFO_PREFIX + callId;
        redis.set(key, platformGbId);
    }


    @Override
    public String queryPlatformRegisterInfo(String callId) {
        return (String) redis.get(VideoManagerConstants.PLATFORM_REGISTER_INFO_PREFIX + callId);
    }

    @Override
    public void delPlatformRegisterInfo(String callId) {
        redis.del(VideoManagerConstants.PLATFORM_REGISTER_INFO_PREFIX + callId);
    }

    @Override
    public void cleanPlatformRegisterInfos() {
        List regInfos = redis.scan(VideoManagerConstants.PLATFORM_REGISTER_INFO_PREFIX + "*");
        for (Object key : regInfos) {
            redis.del(key.toString());
        }
    }

    @Override
    public void updateSendRTPSever(SendRtpItem sendRtpItem) {
        String key = VideoManagerConstants.PLATFORM_SEND_RTP_INFO_PREFIX + sendRtpItem.getPlatformId() + "_" + sendRtpItem.getChannelId();
        redis.set(key, sendRtpItem);
    }

    @Override
    public SendRtpItem querySendRTPServer(String platformGbId, String channelId) {
        String key = VideoManagerConstants.PLATFORM_SEND_RTP_INFO_PREFIX + platformGbId + "_" + channelId;
        return (SendRtpItem) redis.get(key);
    }

    /**
     * 删除RTP推送信息缓存
     *
     * @param platformGbId
     * @param channelId
     */
    @Override
    public void deleteSendRTPServer(String platformGbId, String channelId) {
        String key = VideoManagerConstants.PLATFORM_SEND_RTP_INFO_PREFIX + platformGbId + "_" + channelId;
        redis.del(key);
    }

    /**
     * 查询某个通道是否存在上级点播（RTP推送）
     *
     * @param channelId
     */
    @Override
    public boolean isChannelSendingRTP(String channelId) {
        String key = VideoManagerConstants.PLATFORM_SEND_RTP_INFO_PREFIX + "*_" + channelId;
        List<Object> RtpStreams = redis.scan(key);
        if (RtpStreams.size() > 0) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * 更新媒体流列表
     *
     * @param mediaList
     */
    @Override
    public void updateMediaList(List<RealVideo> mediaList) {
        String key = VideoManagerConstants.MEDIA_STREAM_PREFIX;
        redis.del(key);
        for (int i = 0; i < mediaList.size(); i++) {
            RealVideo realVideo = mediaList.get(i);
            redis.zAdd(key, realVideo, realVideo.getCreateStamp());
        }
    }


    /**
     * 获取当前媒体流列表
     *
     * @return List<RealVideo>
     */
    @Override
    public List<Object> getMediaList(int start, int end) {
        String key = VideoManagerConstants.MEDIA_STREAM_PREFIX;
        Set<Object> realVideos = redis.ZRange(key, start, end);
        return new ArrayList(realVideos);
    }

    public static String getKey(String prefix, String streamId, String channelId, String deviceId) {
        if (StringUtils.isBlank(streamId)) {
            streamId = "*";
        }
        if (StringUtils.isBlank(channelId)) {
            channelId = "*";
        }
        if (StringUtils.isBlank(deviceId)) {
            deviceId = "*";
        }
        return String.format("%S%s_%s_%s",
                prefix,
                streamId,
                channelId,
                deviceId);
    }
}
