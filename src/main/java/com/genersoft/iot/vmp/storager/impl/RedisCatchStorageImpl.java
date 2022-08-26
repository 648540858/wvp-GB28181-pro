package com.genersoft.iot.vmp.storager.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.SystemInfoDto;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.media.zlm.dto.MediaItem;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.OnPublishHookParam;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.service.bean.MessageForPushChannel;
import com.genersoft.iot.vmp.service.bean.ThirdPartyGB;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@SuppressWarnings("rawtypes")
@Component
public class RedisCatchStorageImpl implements IRedisCatchStorage {

    private final Logger logger = LoggerFactory.getLogger(RedisCatchStorageImpl.class);

    @Autowired
    private DeviceChannelMapper deviceChannelMapper;

    @Autowired
    private UserSetting userSetting;

    @Override
    public Long getCSEQ() {
        String key = VideoManagerConstants.SIP_CSEQ_PREFIX  + userSetting.getServerId();

        long result =  RedisUtil.incr(key, 1L);
        if (result > Integer.MAX_VALUE) {
            RedisUtil.set(key, 1);
            result = 1;
        }
        return result;
    }

    @Override
    public Long getSN(String method) {
        String key = VideoManagerConstants.SIP_SN_PREFIX  + userSetting.getServerId() + "_" +  method;

        long result =  RedisUtil.incr(key, 1L);
        if (result > Integer.MAX_VALUE) {
            RedisUtil.set(key, 1);
            result = 1;
        }
        return result;
    }

    @Override
    public void resetAllCSEQ() {
        String scanKey = VideoManagerConstants.SIP_CSEQ_PREFIX  + userSetting.getServerId() + "_*";
        List<Object> keys = RedisUtil.scan(scanKey);
        for (Object o : keys) {
            String key = (String) o;
            RedisUtil.set(key, 1);
        }
    }

    @Override
    public void resetAllSN() {
        String scanKey = VideoManagerConstants.SIP_SN_PREFIX  + userSetting.getServerId() + "_*";
        List<Object> keys = RedisUtil.scan(scanKey);
        for (Object o : keys) {
            String key = (String) o;
            RedisUtil.set(key, 1);
        }
    }

    /**
     * 开始播放时将流存入redis
     *
     * @return
     */
    @Override
    public boolean startPlay(StreamInfo stream) {
        return RedisUtil.set(String.format("%S_%S_%s_%s_%s", VideoManagerConstants.PLAYER_PREFIX, userSetting.getServerId(),
                        stream.getStream(), stream.getDeviceID(), stream.getChannelId()),
                stream);
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
        return RedisUtil.del(String.format("%S_%s_%s_%s_%s", VideoManagerConstants.PLAYER_PREFIX,
                userSetting.getServerId(),
                streamInfo.getStream(),
                streamInfo.getDeviceID(),
                streamInfo.getChannelId()));
    }

    /**
     * 查询播放列表
     * @return
     */
    @Override
    public StreamInfo queryPlay(StreamInfo streamInfo) {
        return (StreamInfo)RedisUtil.get(String.format("%S_%s_%s_%s_%s",
                VideoManagerConstants.PLAYER_PREFIX,
                userSetting.getServerId(),
                streamInfo.getStream(),
                streamInfo.getDeviceID(),
                streamInfo.getChannelId()));
    }
    @Override
    public StreamInfo queryPlayByStreamId(String streamId) {
        List<Object> playLeys = RedisUtil.scan(String.format("%S_%s_%s_*", VideoManagerConstants.PLAYER_PREFIX, userSetting.getServerId(), streamId));
        if (playLeys == null || playLeys.size() == 0) {
            return null;
        }
        return (StreamInfo)RedisUtil.get(playLeys.get(0).toString());
    }

    @Override
    public StreamInfo queryPlayByDevice(String deviceId, String channelId) {
        List<Object> playLeys = RedisUtil.scan(String.format("%S_%s_*_%s_%s", VideoManagerConstants.PLAYER_PREFIX,
                userSetting.getServerId(),
                deviceId,
                channelId));
        if (playLeys == null || playLeys.size() == 0) {
            return null;
        }
        return (StreamInfo)RedisUtil.get(playLeys.get(0).toString());
    }

    @Override
    public Map<String, StreamInfo> queryPlayByDeviceId(String deviceId) {
        Map<String, StreamInfo> streamInfos = new HashMap<>();
//		List<Object> playLeys = RedisUtil.keys(String.format("%S_*_%S_*", VideoManagerConstants.PLAYER_PREFIX, deviceId));
        List<Object> players = RedisUtil.scan(String.format("%S_%s_*_%S_*", VideoManagerConstants.PLAYER_PREFIX, userSetting.getServerId(),deviceId));
        if (players.size() == 0) {
            return streamInfos;
        }
        for (Object player : players) {
            String key = (String) player;
            StreamInfo streamInfo = (StreamInfo) RedisUtil.get(key);
            streamInfos.put(streamInfo.getDeviceID() + "_" + streamInfo.getChannelId(), streamInfo);
        }
        return streamInfos;
    }


    @Override
    public boolean startPlayback(StreamInfo stream, String callId) {
        return RedisUtil.set(String.format("%S_%s_%s_%s_%s_%s", VideoManagerConstants.PLAY_BLACK_PREFIX,
                userSetting.getServerId(), stream.getDeviceID(), stream.getChannelId(), stream.getStream(), callId), stream);
    }

    @Override
    public boolean startDownload(StreamInfo stream, String callId) {
        boolean result;
        if (stream.getProgress() == 1) {
            result = RedisUtil.set(String.format("%S_%s_%s_%s_%s_%s", VideoManagerConstants.DOWNLOAD_PREFIX,
                    userSetting.getServerId(), stream.getDeviceID(), stream.getChannelId(), stream.getStream(), callId), stream);
        }else {
            result = RedisUtil.set(String.format("%S_%s_%s_%s_%s_%s", VideoManagerConstants.DOWNLOAD_PREFIX,
                    userSetting.getServerId(), stream.getDeviceID(), stream.getChannelId(), stream.getStream(), callId), stream, 60*60);
        }
        return result;
    }
    @Override
    public boolean stopDownload(String deviceId, String channelId, String stream, String callId) {
        DeviceChannel deviceChannel = deviceChannelMapper.queryChannel(deviceId, channelId);
        if (deviceChannel != null) {
            deviceChannel.setStreamId(null);
            deviceChannel.setDeviceId(deviceId);
            deviceChannelMapper.update(deviceChannel);
        }
        if (deviceId == null) {
            deviceId = "*";
        }
        if (channelId == null) {
            channelId = "*";
        }
        if (stream == null) {
            stream = "*";
        }
        if (callId == null) {
            callId = "*";
        }
        String key = String.format("%S_%s_%s_%s_%s_%s", VideoManagerConstants.DOWNLOAD_PREFIX,
                userSetting.getServerId(),
                deviceId,
                channelId,
                stream,
                callId
        );
        List<Object> scan = RedisUtil.scan(key);
        if (scan.size() > 0) {
            for (Object keyObj : scan) {
                RedisUtil.del((String) keyObj);
            }
        }
        return true;
    }

    @Override
    public boolean stopPlayback(String deviceId, String channelId, String stream, String callId) {
        DeviceChannel deviceChannel = deviceChannelMapper.queryChannel(deviceId, channelId);
        if (deviceChannel != null) {
            deviceChannel.setStreamId(null);
            deviceChannel.setDeviceId(deviceId);
            deviceChannelMapper.update(deviceChannel);
        }
        if (deviceId == null) {
            deviceId = "*";
        }
        if (channelId == null) {
            channelId = "*";
        }
        if (stream == null) {
            stream = "*";
        }
        if (callId == null) {
            callId = "*";
        }
        String key = String.format("%S_%s_%s_%s_%s_%s", VideoManagerConstants.PLAY_BLACK_PREFIX,
                userSetting.getServerId(),
                deviceId,
                channelId,
                stream,
                callId
        );
        List<Object> scan = RedisUtil.scan(key);
        if (scan.size() > 0) {
            for (Object keyObj : scan) {
                RedisUtil.del((String) keyObj);
            }
        }
        return true;
    }

    @Override
    public StreamInfo queryPlayback(String deviceId, String channelId, String stream, String callId) {
        if (stream == null && callId == null) {
            return null;
        }
        if (deviceId == null) {
            deviceId = "*";
        }
        if (channelId == null) {
            channelId = "*";
        }
        if (stream == null) {
            stream = "*";
        }
        if (callId == null) {
            callId = "*";
        }
        String key = String.format("%S_%s_%s_%s_%s_%s", VideoManagerConstants.PLAY_BLACK_PREFIX,
                userSetting.getServerId(),
                deviceId,
                channelId,
                stream,
                callId
        );
        List<Object> streamInfoScan = RedisUtil.scan(key);
        if (streamInfoScan.size() > 0) {
            return (StreamInfo) RedisUtil.get((String) streamInfoScan.get(0));
        }else {
            return null;
        }
    }

    @Override
    public void updatePlatformCatchInfo(ParentPlatformCatch parentPlatformCatch) {
        String key = VideoManagerConstants.PLATFORM_CATCH_PREFIX  + userSetting.getServerId() + "_" +  parentPlatformCatch.getId();
        RedisUtil.set(key, parentPlatformCatch);
    }

    @Override
    public void updatePlatformKeepalive(ParentPlatform parentPlatform) {
        String key = VideoManagerConstants.PLATFORM_KEEPALIVE_PREFIX  + userSetting.getServerId() + "_" + parentPlatform.getServerGBId();
        RedisUtil.set(key, "", Integer.parseInt(parentPlatform.getKeepTimeout()));
    }

    @Override
    public void updatePlatformRegister(ParentPlatform parentPlatform) {
        String key = VideoManagerConstants.PLATFORM_REGISTER_PREFIX + userSetting.getServerId() + "_" + parentPlatform.getServerGBId();
        RedisUtil.set(key, "", Integer.parseInt(parentPlatform.getExpires()));
    }

    @Override
    public ParentPlatformCatch queryPlatformCatchInfo(String platformGbId) {
        return (ParentPlatformCatch)RedisUtil.get(VideoManagerConstants.PLATFORM_CATCH_PREFIX + userSetting.getServerId() + "_" + platformGbId);
    }

    @Override
    public void delPlatformCatchInfo(String platformGbId) {
        RedisUtil.del(VideoManagerConstants.PLATFORM_CATCH_PREFIX + userSetting.getServerId() + "_" + platformGbId);
    }

    @Override
    public void delPlatformKeepalive(String platformGbId) {
        RedisUtil.del(VideoManagerConstants.PLATFORM_KEEPALIVE_PREFIX + userSetting.getServerId() + "_" + platformGbId);
    }

    @Override
    public void delPlatformRegister(String platformGbId) {
        RedisUtil.del(VideoManagerConstants.PLATFORM_REGISTER_PREFIX + userSetting.getServerId() + "_" + platformGbId);
    }


    @Override
    public void updatePlatformRegisterInfo(String callId, String platformGbId) {
        String key = VideoManagerConstants.PLATFORM_REGISTER_INFO_PREFIX + userSetting.getServerId() + "_" + callId;
        RedisUtil.set(key, platformGbId, 30);
    }


    @Override
    public String queryPlatformRegisterInfo(String callId) {
        return (String)RedisUtil.get(VideoManagerConstants.PLATFORM_REGISTER_INFO_PREFIX + userSetting.getServerId() + "_" + callId);
    }

    @Override
    public void delPlatformRegisterInfo(String callId) {
        RedisUtil.del(VideoManagerConstants.PLATFORM_REGISTER_INFO_PREFIX + userSetting.getServerId() + "_" + callId);
    }

    @Override
    public void cleanPlatformRegisterInfos() {
        List regInfos = RedisUtil.scan(VideoManagerConstants.PLATFORM_REGISTER_INFO_PREFIX + userSetting.getServerId() + "_" + "*");
        for (Object key : regInfos) {
            RedisUtil.del(key.toString());
        }
    }

    @Override
    public void updateSendRTPSever(SendRtpItem sendRtpItem) {
        String key = VideoManagerConstants.PLATFORM_SEND_RTP_INFO_PREFIX + userSetting.getServerId() + "_"
                + sendRtpItem.getPlatformId() + "_" + sendRtpItem.getChannelId() + "_"
                + sendRtpItem.getStreamId() + "_" + sendRtpItem.getCallId();
        RedisUtil.set(key, sendRtpItem);
    }

    @Override
    public SendRtpItem querySendRTPServer(String platformGbId, String channelId, String streamId, String callId) {
        if (platformGbId == null) {
            platformGbId = "*";
        }
        if (channelId == null) {
            channelId = "*";
        }
        if (streamId == null) {
            streamId = "*";
        }
        if (callId == null) {
            callId = "*";
        }
        String key = VideoManagerConstants.PLATFORM_SEND_RTP_INFO_PREFIX + userSetting.getServerId() + "_" + platformGbId
                + "_" + channelId + "_" + streamId + "_" + callId;
        List<Object> scan = RedisUtil.scan(key);
        if (scan.size() > 0) {
            return (SendRtpItem)RedisUtil.get((String)scan.get(0));
        }else {
            return null;
        }
    }

    @Override
    public List<SendRtpItem> querySendRTPServerByChnnelId(String channelId) {
        if (channelId == null) {
            return null;
        }
        String platformGbId = "*";
        String callId = "*";
        String streamId = "*";
        String key = VideoManagerConstants.PLATFORM_SEND_RTP_INFO_PREFIX + userSetting.getServerId() + "_" + platformGbId
                + "_" + channelId + "_" + streamId + "_" + callId;
        List<Object> scan = RedisUtil.scan(key);
        List<SendRtpItem> result = new ArrayList<>();
        for (Object o : scan) {
            result.add((SendRtpItem) RedisUtil.get((String) o));
        }
        return result;
    }

    @Override
    public List<SendRtpItem> querySendRTPServer(String platformGbId) {
        if (platformGbId == null) {
            platformGbId = "*";
        }
        String key = VideoManagerConstants.PLATFORM_SEND_RTP_INFO_PREFIX + userSetting.getServerId() + "_" + platformGbId + "_*" + "_*" + "_*";
        List<Object> queryResult = RedisUtil.scan(key);
        List<SendRtpItem> result= new ArrayList<>();

        for (Object o : queryResult) {
            String keyItem = (String) o;
            result.add((SendRtpItem) RedisUtil.get(keyItem));
        }

        return result;
    }

    /**
     * 删除RTP推送信息缓存
     * @param platformGbId
     * @param channelId
     */
    @Override
    public void deleteSendRTPServer(String platformGbId, String channelId, String callId, String streamId) {
        if (streamId == null) {
            streamId = "*";
        }
        if (callId == null) {
            callId = "*";
        }
        String key = VideoManagerConstants.PLATFORM_SEND_RTP_INFO_PREFIX + userSetting.getServerId() + "_" + platformGbId
                + "_" + channelId + "_" + streamId + "_" + callId;
        List<Object> scan = RedisUtil.scan(key);
        if (scan.size() > 0) {
            for (Object keyStr : scan) {
                RedisUtil.del((String)keyStr);
            }
        }
    }



    /**
     * 查询某个通道是否存在上级点播（RTP推送）
     * @param channelId
     */
    @Override
    public boolean isChannelSendingRTP(String channelId) {
        String key = VideoManagerConstants.PLATFORM_SEND_RTP_INFO_PREFIX + userSetting.getServerId() + "_" + "*_" + channelId + "*_" + "*_";
        List<Object> RtpStreams = RedisUtil.scan(key);
        if (RtpStreams.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void clearCatchByDeviceId(String deviceId) {
        List<Object> playLeys = RedisUtil.scan(String.format("%S_%s_*_%s_*", VideoManagerConstants.PLAYER_PREFIX,
                userSetting.getServerId(),
                deviceId));
        if (playLeys.size() > 0) {
            for (Object key : playLeys) {
                RedisUtil.del(key.toString());
            }
        }

        List<Object> playBackers = RedisUtil.scan(String.format("%S_%s_%s_*_*_*", VideoManagerConstants.PLAY_BLACK_PREFIX,
                userSetting.getServerId(),
                deviceId));
        if (playBackers.size() > 0) {
            for (Object key : playBackers) {
                RedisUtil.del(key.toString());
            }
        }

        List<Object> deviceCache = RedisUtil.scan(String.format("%S%s_%s", VideoManagerConstants.DEVICE_PREFIX,
                userSetting.getServerId(),
                deviceId));
        if (deviceCache.size() > 0) {
            for (Object key : deviceCache) {
                RedisUtil.del(key.toString());
            }
        }
    }

    @Override
    public void updateWVPInfo(JSONObject jsonObject, int time) {
        String key = VideoManagerConstants.WVP_SERVER_PREFIX + userSetting.getServerId();
        RedisUtil.set(key, jsonObject, time);
    }

    @Override
    public void sendStreamChangeMsg(String type, JSONObject jsonObject) {
        String key = VideoManagerConstants.WVP_MSG_STREAM_CHANGE_PREFIX + type;
        logger.info("[redis 流变化事件] {}: {}", key, jsonObject.toString());
        RedisUtil.convertAndSend(key, jsonObject);
    }

    @Override
    public void addStream(MediaServerItem mediaServerItem, String type, String app, String streamId, MediaItem mediaItem) {
        // 查找是否使用了callID
        StreamAuthorityInfo streamAuthorityInfo = getStreamAuthorityInfo(app, streamId);
        String key = VideoManagerConstants.WVP_SERVER_STREAM_PREFIX  + userSetting.getServerId() + "_" + type + "_" + app + "_" + streamId + "_" + mediaServerItem.getId();
        if (streamAuthorityInfo != null) {
            mediaItem.setCallId(streamAuthorityInfo.getCallId());
        }
        RedisUtil.set(key, mediaItem);
    }

    @Override
    public void removeStream(String mediaServerId, String type, String app, String streamId) {
        String key = VideoManagerConstants.WVP_SERVER_STREAM_PREFIX + userSetting.getServerId() + "_" + type + "_"  + app + "_" + streamId + "_" + mediaServerId;
        RedisUtil.del(key);
    }

    @Override
    public StreamInfo queryDownload(String deviceId, String channelId, String stream, String callId) {
        if (stream == null && callId == null) {
            return null;
        }
        if (deviceId == null) {
            deviceId = "*";
        }
        if (channelId == null) {
            channelId = "*";
        }
        if (stream == null) {
            stream = "*";
        }
        if (callId == null) {
            callId = "*";
        }
        String key = String.format("%S_%s_%s_%s_%s_%s", VideoManagerConstants.DOWNLOAD_PREFIX,
                userSetting.getServerId(),
                deviceId,
                channelId,
                stream,
                callId
        );
        List<Object> streamInfoScan = RedisUtil.scan(key);
        if (streamInfoScan.size() > 0) {
            return (StreamInfo) RedisUtil.get((String) streamInfoScan.get(0));
        }else {
            return null;
        }
    }

    @Override
    public ThirdPartyGB queryMemberNoGBId(String queryKey) {
        String key = VideoManagerConstants.WVP_STREAM_GB_ID_PREFIX + queryKey;
        JSONObject jsonObject = (JSONObject)RedisUtil.get(key);
        return  JSONObject.toJavaObject(jsonObject, ThirdPartyGB.class);
    }

    @Override
    public void removeStream(String mediaServerId, String type) {
        String key = VideoManagerConstants.WVP_SERVER_STREAM_PREFIX + userSetting.getServerId() + "_" + type + "_*_*_" + mediaServerId;
        List<Object> streams = RedisUtil.scan(key);
        for (Object stream : streams) {
            RedisUtil.del((String) stream);
        }
    }

    @Override
    public List<MediaItem> getStreams(String mediaServerId, String type) {
        List<MediaItem> result = new ArrayList<>();
        String key = VideoManagerConstants.WVP_SERVER_STREAM_PREFIX + userSetting.getServerId() + "_" + type + "_*_*_" + mediaServerId;
        List<Object> streams = RedisUtil.scan(key);
        for (Object stream : streams) {
            MediaItem mediaItem = (MediaItem)RedisUtil.get((String) stream);
            result.add(mediaItem);
        }
        return result;
    }

    @Override
    public void updateDevice(Device device) {
        String key = VideoManagerConstants.DEVICE_PREFIX + userSetting.getServerId() + "_" + device.getDeviceId();
        RedisUtil.set(key, device);
    }

    @Override
    public void removeDevice(String deviceId) {
        String key = VideoManagerConstants.DEVICE_PREFIX + userSetting.getServerId() + "_" + deviceId;
        RedisUtil.del(key);
    }

    @Override
    public Device getDevice(String deviceId) {
        String key = VideoManagerConstants.DEVICE_PREFIX + userSetting.getServerId() + "_" + deviceId;
        return (Device)RedisUtil.get(key);
    }

    @Override
    public void updateGpsMsgInfo(GPSMsgInfo gpsMsgInfo) {
        String key = VideoManagerConstants.WVP_STREAM_GPS_MSG_PREFIX + userSetting.getServerId() + "_" + gpsMsgInfo.getId();
        RedisUtil.set(key, gpsMsgInfo, 60); // 默认GPS消息保存1分钟
    }

    @Override
    public GPSMsgInfo getGpsMsgInfo(String gbId) {
        String key = VideoManagerConstants.WVP_STREAM_GPS_MSG_PREFIX + userSetting.getServerId() + "_" + gbId;
        return (GPSMsgInfo)RedisUtil.get(key);
    }

    @Override
    public List<GPSMsgInfo> getAllGpsMsgInfo() {
        String scanKey = VideoManagerConstants.WVP_STREAM_GPS_MSG_PREFIX + userSetting.getServerId() + "_*";
        List<GPSMsgInfo> result = new ArrayList<>();
        List<Object> keys = RedisUtil.scan(scanKey);
        for (Object o : keys) {
            String key = (String) o;
            GPSMsgInfo gpsMsgInfo = (GPSMsgInfo) RedisUtil.get(key);
            if (!gpsMsgInfo.isStored()) { // 只取没有存过得
                result.add((GPSMsgInfo) RedisUtil.get(key));
            }
        }

        return result;
    }

    @Override
    public void updateStreamAuthorityInfo(String app, String stream, StreamAuthorityInfo streamAuthorityInfo) {
        String key = VideoManagerConstants.MEDIA_STREAM_AUTHORITY + userSetting.getServerId() + "_" + app+ "_" + stream;
        RedisUtil.set(key, streamAuthorityInfo);
    }

    @Override
    public void removeStreamAuthorityInfo(String app, String stream) {
        String key = VideoManagerConstants.MEDIA_STREAM_AUTHORITY + userSetting.getServerId() + "_" + app+ "_" + stream ;
        RedisUtil.del(key);
    }

    @Override
    public StreamAuthorityInfo getStreamAuthorityInfo(String app, String stream) {
        String key = VideoManagerConstants.MEDIA_STREAM_AUTHORITY + userSetting.getServerId() + "_" + app+ "_" + stream ;
        return (StreamAuthorityInfo) RedisUtil.get(key);

    }


    @Override
    public MediaItem getStreamInfo(String app, String streamId, String mediaServerId) {
        String scanKey = VideoManagerConstants.WVP_SERVER_STREAM_PREFIX  + userSetting.getServerId() + "_*_" + app + "_" + streamId + "_" + mediaServerId;

        MediaItem result = null;
        List<Object> keys = RedisUtil.scan(scanKey);
        if (keys.size() > 0) {
            String key = (String) keys.get(0);
            result = (MediaItem)RedisUtil.get(key);
        }

        return result;
    }

    @Override
    public void addCpuInfo(double cpuInfo) {
        String key = VideoManagerConstants.SYSTEM_INFO_CPU_PREFIX + userSetting.getServerId();
        SystemInfoDto<Double> systemInfoDto = new SystemInfoDto<>();
        systemInfoDto.setTime(DateUtil.getNow());
        systemInfoDto.setData(cpuInfo);
        RedisUtil.lSet(key, systemInfoDto);
        // 每秒一个，最多只存30个
        if (RedisUtil.lGetListSize(key) > 30) {
            for (int i = 0; i < RedisUtil.lGetListSize(key) - 30; i++) {
                RedisUtil.lLeftPop(key);
            }
        }
    }

    @Override
    public void addMemInfo(double memInfo) {
        String key = VideoManagerConstants.SYSTEM_INFO_MEM_PREFIX + userSetting.getServerId();
        SystemInfoDto<Double> systemInfoDto = new SystemInfoDto<>();
        systemInfoDto.setTime(DateUtil.getNow());
        systemInfoDto.setData(memInfo);
        RedisUtil.lSet(key, systemInfoDto);
        // 每秒一个，最多只存30个
        if (RedisUtil.lGetListSize(key) > 30) {
            for (int i = 0; i < RedisUtil.lGetListSize(key) - 30; i++) {
                RedisUtil.lLeftPop(key);
            }
        }
    }

    @Override
    public void addNetInfo(Map<String, String> networkInterfaces) {
        String key = VideoManagerConstants.SYSTEM_INFO_NET_PREFIX + userSetting.getServerId();
        SystemInfoDto<Map<String, String>> systemInfoDto = new SystemInfoDto<>();
        systemInfoDto.setTime(DateUtil.getNow());
        systemInfoDto.setData(networkInterfaces);
        RedisUtil.lSet(key, systemInfoDto);
        // 每秒一个，最多只存30个
        if (RedisUtil.lGetListSize(key) > 30) {
            for (int i = 0; i < RedisUtil.lGetListSize(key) - 30; i++) {
                RedisUtil.lLeftPop(key);
            }
        }
    }

    @Override
    public void sendMobilePositionMsg(JSONObject jsonObject) {
        String key = VideoManagerConstants.VM_MSG_SUBSCRIBE_MOBILE_POSITION;
        logger.info("[redis发送通知]移动位置 {}: {}", key, jsonObject.toString());
        RedisUtil.convertAndSend(key, jsonObject);
    }

    @Override
    public void sendStreamPushRequestedMsg(MessageForPushChannel msg) {
        String key = VideoManagerConstants.VM_MSG_STREAM_PUSH_REQUESTED;
        logger.info("[redis发送通知]推流被请求 {}: {}/{}", key, msg.getApp(), msg.getStream());
        RedisUtil.convertAndSend(key, (JSONObject)JSON.toJSON(msg));
    }

    @Override
    public void sendAlarmMsg(AlarmChannelMessage msg) {
        String key = VideoManagerConstants.VM_MSG_SUBSCRIBE_ALARM;
        logger.info("[redis发送通知] 报警{}: {}", key, JSON.toJSON(msg));
        RedisUtil.convertAndSend(key, (JSONObject)JSON.toJSON(msg));
    }

    @Override
    public boolean deviceIsOnline(String deviceId) {
        return getDevice(deviceId).getOnline() == 1;
    }


    @Override
    public void sendStreamPushRequestedMsgForStatus() {
        String key = VideoManagerConstants.VM_MSG_GET_ALL_ONLINE_REQUESTED;
        logger.info("[redis通知]获取所有推流设备的状态");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(key, key);
        RedisUtil.convertAndSend(key, jsonObject);
    }
}
