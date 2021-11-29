package com.genersoft.iot.vmp.storager.impl;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;

@SuppressWarnings("rawtypes")
@Component
public class RedisCatchStorageImpl implements IRedisCatchStorage {

    @Autowired
	private RedisUtil redis;

    @Autowired
    private DeviceChannelMapper deviceChannelMapper;

    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

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
    public StreamInfo queryPlayByStreamId(String streamId) {
        List<Object> playLeys = redis.scan(String.format("%S_%s_*", VideoManagerConstants.PLAYER_PREFIX, streamId));
        if (playLeys == null || playLeys.size() == 0) return null;
        return (StreamInfo)redis.get(playLeys.get(0).toString());
    }

    @Override
    public StreamInfo queryPlaybackByStreamId(String streamId) {
        List<Object> playLeys = redis.scan(String.format("%S_%s_*", VideoManagerConstants.PLAY_BLACK_PREFIX, streamId));
        if (playLeys == null || playLeys.size() == 0) return null;
        return (StreamInfo)redis.get(playLeys.get(0).toString());
    }

    @Override
    public StreamInfo queryPlayByDevice(String deviceId, String channelId) {
//		List<Object> playLeys = redis.keys(String.format("%S_*_%s_%s", VideoManagerConstants.PLAYER_PREFIX,
        List<Object> playLeys = redis.scan(String.format("%S_*_%s_%s", VideoManagerConstants.PLAYER_PREFIX,
                deviceId,
                channelId));
        if (playLeys == null || playLeys.size() == 0) return null;
        return (StreamInfo)redis.get(playLeys.get(0).toString());
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
        return redis.set(String.format("%S_%s_%s_%s", VideoManagerConstants.PLAY_BLACK_PREFIX, stream.getStreamId(),
                        stream.getDeviceID(), stream.getChannelId()), stream);
    }

    @Override
    public boolean startDownload(StreamInfo streamInfo) {
        return redis.set(String.format("%S_%s_%s_%s", VideoManagerConstants.DOWNLOAD_PREFIX, streamInfo.getStreamId(),
                        streamInfo.getDeviceID(), streamInfo.getChannelId()), streamInfo);
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
        // String format = String.format("%S_*_%s_%s", VideoManagerConstants.PLAY_BLACK_PREFIX,
        //         deviceId,
        //         code);
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
        return (ParentPlatformCatch)redis.get(VideoManagerConstants.PLATFORM_CATCH_PREFIX + platformGbId);
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
        return (String)redis.get(VideoManagerConstants.PLATFORM_REGISTER_INFO_PREFIX + callId);
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
        return (SendRtpItem)redis.get(key);
    }

    @Override
    public List<SendRtpItem> querySendRTPServer(String platformGbId) {
        String key = VideoManagerConstants.PLATFORM_SEND_RTP_INFO_PREFIX + platformGbId + "_*";
        List<Object> queryResult = redis.scan(key);
        List<SendRtpItem> result= new ArrayList<>();

        for (int i = 0; i < queryResult.size(); i++) {
            String keyItem = (String) queryResult.get(i);
            result.add((SendRtpItem)redis.get(keyItem));
        }

        return result;
    }

    /**
     * 删除RTP推送信息缓存
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

    @Override
    public void clearCatchByDeviceId(String deviceId) {
        List<Object> playLeys = redis.scan(String.format("%S_*_%s_*", VideoManagerConstants.PLAYER_PREFIX,
                deviceId));
        if (playLeys.size() > 0) {
            for (Object key : playLeys) {
                redis.del(key.toString());
            }
        }

        List<Object> playBackers = redis.scan(String.format("%S_*_%s_*", VideoManagerConstants.PLAY_BLACK_PREFIX,
                deviceId));
        if (playBackers.size() > 0) {
            for (Object key : playBackers) {
                redis.del(key.toString());
            }
        }
    }

    @Override
    public void outlineForAll() {
        List<Object> onlineDevices = redis.scan(VideoManagerConstants.KEEPLIVEKEY_PREFIX + "*" );
        for (int i = 0; i < onlineDevices.size(); i++) {
            String key = (String) onlineDevices.get(i);
            redis.del(key);
        }
    }

    @Override
    public List<String> getOnlineForAll() {
        List<String> result = new ArrayList<>();
        List<Object> onlineDevices = redis.scan(VideoManagerConstants.KEEPLIVEKEY_PREFIX + "*" );
        for (int i = 0; i < onlineDevices.size(); i++) {
            String key = (String) onlineDevices.get(i);
            result.add((String) redis.get(key));
        }
        return result;
    }

    @Override
    public void updateWVPInfo(String id, JSONObject jsonObject, int time) {
        String key = VideoManagerConstants.WVP_SERVER_PREFIX + id;
        redis.set(key, jsonObject, time);
    }

    @Override
    public void sendStreamChangeMsg(JSONObject jsonObject) {
        String key = VideoManagerConstants.WVP_MSG_STREAM_PUSH_CHANGE_PREFIX;
        redis.convertAndSend(key, jsonObject);
    }

    @Override
    public void addPushStream(MediaServerItem mediaServerItem, String app, String streamId, StreamInfo streamInfo) {
        String key = VideoManagerConstants.WVP_SERVER_STREAM_PUSH_PREFIX + app + "_" + streamId + "_" + mediaServerItem.getId();
        redis.set(key, streamInfo);
    }

    @Override
    public void removePushStream(MediaServerItem mediaServerItem, String app, String streamId) {
        String key = VideoManagerConstants.WVP_SERVER_STREAM_PUSH_PREFIX + app + "_" + streamId + "_" + mediaServerItem.getId();
        redis.del(key);
    }

    @Override
    public StreamInfo queryDownloadByStreamId(String streamId) {
        List<Object> playLeys = redis.scan(String.format("%S_%s_*", VideoManagerConstants.DOWNLOAD_PREFIX, streamId));
        if (playLeys == null || playLeys.size() == 0) return null;
        return (StreamInfo)redis.get(playLeys.get(0).toString());
    }
}
