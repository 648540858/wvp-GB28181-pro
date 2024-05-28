package com.genersoft.iot.vmp.storager.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.SystemAllInfo;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.AlarmChannelMessage;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatformCatch;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.media.MediaArrivalEvent;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.service.bean.MessageForPushChannel;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.storager.dao.DeviceMapper;
import com.genersoft.iot.vmp.storager.dao.dto.PlatformRegisterInfo;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.utils.JsonUtil;
import com.genersoft.iot.vmp.utils.SystemInfoUtils;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

@SuppressWarnings("rawtypes")
@Component
public class RedisCatchStorageImpl implements IRedisCatchStorage {

    private final Logger logger = LoggerFactory.getLogger(RedisCatchStorageImpl.class);

    @Autowired
    private DeviceChannelMapper deviceChannelMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Long getCSEQ() {
        String key = VideoManagerConstants.SIP_CSEQ_PREFIX  + userSetting.getServerId();

        Long result =  redisTemplate.opsForValue().increment(key, 1L);
        if (result != null && result > Integer.MAX_VALUE) {
            redisTemplate.opsForValue().set(key, 1);
            result = 1L;
        }
        return result;
    }

    @Override
    public Long getSN(String method) {
        String key = VideoManagerConstants.SIP_SN_PREFIX  + userSetting.getServerId() + "_" +  method;

        Long result =  redisTemplate.opsForValue().increment(key, 1L);
        if (result != null && result > Integer.MAX_VALUE) {
            redisTemplate.opsForValue().set(key, 1);
            result = 1L;
        }
        return result;
    }

    @Override
    public void resetAllCSEQ() {
        String key = VideoManagerConstants.SIP_CSEQ_PREFIX  + userSetting.getServerId();
        redisTemplate.opsForValue().set(key, 1);
    }

    @Override
    public void resetAllSN() {
        String scanKey = VideoManagerConstants.SIP_SN_PREFIX  + userSetting.getServerId() + "_*";
        List<Object> keys = RedisUtil.scan(redisTemplate, scanKey);
        for (Object o : keys) {
            String key = (String) o;
            redisTemplate.opsForValue().set(key, 1);
        }
    }

    @Override
    public void updatePlatformCatchInfo(ParentPlatformCatch parentPlatformCatch) {
        String key = VideoManagerConstants.PLATFORM_CATCH_PREFIX  + userSetting.getServerId() + "_" +  parentPlatformCatch.getId();
        redisTemplate.opsForValue().set(key, parentPlatformCatch);
    }

    @Override
    public ParentPlatformCatch queryPlatformCatchInfo(String platformGbId) {
        return (ParentPlatformCatch)redisTemplate.opsForValue().get(VideoManagerConstants.PLATFORM_CATCH_PREFIX + userSetting.getServerId() + "_" + platformGbId);
    }

    @Override
    public void delPlatformCatchInfo(String platformGbId) {
        redisTemplate.delete(VideoManagerConstants.PLATFORM_CATCH_PREFIX + userSetting.getServerId() + "_" + platformGbId);
    }

    @Override
    public void delPlatformKeepalive(String platformGbId) {
        redisTemplate.delete(VideoManagerConstants.PLATFORM_KEEPALIVE_PREFIX + userSetting.getServerId() + "_" + platformGbId);
    }

    @Override
    public void delPlatformRegister(String platformGbId) {
        redisTemplate.delete(VideoManagerConstants.PLATFORM_REGISTER_PREFIX + userSetting.getServerId() + "_" + platformGbId);
    }


    @Override
    public void updatePlatformRegisterInfo(String callId, PlatformRegisterInfo platformRegisterInfo) {
        String key = VideoManagerConstants.PLATFORM_REGISTER_INFO_PREFIX + userSetting.getServerId() + "_" + callId;
        Duration duration = Duration.ofSeconds(30L);
        redisTemplate.opsForValue().set(key, platformRegisterInfo, duration);
    }


    @Override
    public PlatformRegisterInfo queryPlatformRegisterInfo(String callId) {
        return (PlatformRegisterInfo)redisTemplate.opsForValue().get(VideoManagerConstants.PLATFORM_REGISTER_INFO_PREFIX + userSetting.getServerId() + "_" + callId);
    }

    @Override
    public void delPlatformRegisterInfo(String callId) {
         redisTemplate.delete(VideoManagerConstants.PLATFORM_REGISTER_INFO_PREFIX + userSetting.getServerId() + "_" + callId);
    }

    @Override
    public void updateSendRTPSever(SendRtpItem sendRtpItem) {
        redisTemplate.opsForValue().set(sendRtpItem.getRedisKey(), sendRtpItem);
    }

    @Override
    public List<SendRtpItem> querySendRTPServer(String platformGbId, String channelId, String streamId) {
        String scanKey = VideoManagerConstants.PLATFORM_SEND_RTP_INFO_PREFIX
                + userSetting.getServerId() + "_*_"
                + platformGbId + "_"
                + channelId + "_"
                + streamId + "_"
                + "*";
        List<SendRtpItem> result = new ArrayList<>();
        List<Object> scan = RedisUtil.scan(redisTemplate, scanKey);
        if (!scan.isEmpty()) {
            for (Object o : scan) {
                String key = (String) o;
                result.add(JsonUtil.redisJsonToObject(redisTemplate, key, SendRtpItem.class));
            }
        }
        return result;
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
        String key = VideoManagerConstants.PLATFORM_SEND_RTP_INFO_PREFIX
                + "*_*_"
                + platformGbId + "_"
                + channelId + "_"
                + streamId + "_"
                + callId;
        List<Object> scan = RedisUtil.scan(redisTemplate, key);
        if (scan.size() > 0) {
            return (SendRtpItem)redisTemplate.opsForValue().get(scan.get(0));
        }else {
            return null;
        }
    }

    @Override
    public List<SendRtpItem> querySendRTPServerByChannelId(String channelId) {
        if (channelId == null) {
            return null;
        }
        String platformGbId = "*";
        String callId = "*";
        String streamId = "*";
        String key = VideoManagerConstants.PLATFORM_SEND_RTP_INFO_PREFIX
                + userSetting.getServerId() + "_*_"
                + platformGbId + "_"
                + channelId + "_"
                + streamId + "_"
                + callId;
        List<Object> scan = RedisUtil.scan(redisTemplate, key);
        List<SendRtpItem> result = new ArrayList<>();
        for (Object o : scan) {
            result.add((SendRtpItem) redisTemplate.opsForValue().get(o));
        }
        return result;
    }

    @Override
    public List<SendRtpItem> querySendRTPServerByStream(String stream) {
        if (stream == null) {
            return null;
        }
        String platformGbId = "*";
        String callId = "*";
        String channelId = "*";
        String key = VideoManagerConstants.PLATFORM_SEND_RTP_INFO_PREFIX
                + userSetting.getServerId() + "_*_"
                + platformGbId + "_"
                + channelId + "_"
                + stream + "_"
                + callId;
        List<Object> scan = RedisUtil.scan(redisTemplate, key);
        List<SendRtpItem> result = new ArrayList<>();
        for (Object o : scan) {
            result.add((SendRtpItem) redisTemplate.opsForValue().get(o));
        }
        return result;
    }

    @Override
    public List<SendRtpItem> querySendRTPServer(String platformGbId) {
        if (platformGbId == null) {
            platformGbId = "*";
        }
        String key = VideoManagerConstants.PLATFORM_SEND_RTP_INFO_PREFIX
                + userSetting.getServerId() + "_*_"
                + platformGbId + "_*" + "_*" + "_*";
        List<Object> queryResult = RedisUtil.scan(redisTemplate, key);
        List<SendRtpItem> result= new ArrayList<>();

        for (Object o : queryResult) {
            String keyItem = (String) o;
            result.add((SendRtpItem) redisTemplate.opsForValue().get(keyItem));
        }

        return result;
    }

    /**
     * 删除RTP推送信息缓存
     */
    @Override
    public void deleteSendRTPServer(String platformGbId, String channelId, String callId, String streamId) {
        if (streamId == null) {
            streamId = "*";
        }
        if (callId == null) {
            callId = "*";
        }
        String key = VideoManagerConstants.PLATFORM_SEND_RTP_INFO_PREFIX
                + userSetting.getServerId() + "_*_"
                + platformGbId + "_"
                + channelId + "_"
                + streamId + "_"
                + callId;
        List<Object> scan = RedisUtil.scan(redisTemplate, key);
        if (scan.size() > 0) {
            for (Object keyStr : scan) {
                logger.info("[删除 redis的SendRTP]： {}", keyStr.toString());
                redisTemplate.delete(keyStr);
            }
        }
    }

    /**
     * 删除RTP推送信息缓存
     */
    @Override
    public void deleteSendRTPServer(SendRtpItem sendRtpItem) {
        deleteSendRTPServer(sendRtpItem.getPlatformId(), sendRtpItem.getChannelId(),sendRtpItem.getCallId(), sendRtpItem.getStream());
    }

    @Override
    public List<SendRtpItem> queryAllSendRTPServer() {
        String key = VideoManagerConstants.PLATFORM_SEND_RTP_INFO_PREFIX
                + userSetting.getServerId() + "_*";
        List<Object> queryResult = RedisUtil.scan(redisTemplate, key);
        List<SendRtpItem> result= new ArrayList<>();

        for (Object o : queryResult) {
            String keyItem = (String) o;
            result.add((SendRtpItem) redisTemplate.opsForValue().get(keyItem));
        }

        return result;
    }

    /**
     * 查询某个通道是否存在上级点播（RTP推送）
     */
    @Override
    public boolean isChannelSendingRTP(String channelId) {
        String key = VideoManagerConstants.PLATFORM_SEND_RTP_INFO_PREFIX
                + userSetting.getServerId() + "_*_*_"
                + channelId + "*_" + "*_";
        List<Object> RtpStreams = RedisUtil.scan(redisTemplate, key);
        return RtpStreams.size() > 0;
    }

    @Override
    public void updateWVPInfo(JSONObject jsonObject, int time) {
        String key = VideoManagerConstants.WVP_SERVER_PREFIX + userSetting.getServerId();
        Duration duration = Duration.ofSeconds(time);
        redisTemplate.opsForValue().set(key, jsonObject, duration);
    }

    @Override
    public void sendStreamChangeMsg(String type, JSONObject jsonObject) {
        String key = VideoManagerConstants.WVP_MSG_STREAM_CHANGE_PREFIX + type;
        logger.info("[redis 流变化事件] 发送 {}: {}", key, jsonObject.toString());
        redisTemplate.convertAndSend(key, jsonObject);
    }

    @Override
    public void addStream(MediaServer mediaServerItem, String type, String app, String streamId, MediaInfo mediaInfo) {
        // 查找是否使用了callID
        StreamAuthorityInfo streamAuthorityInfo = getStreamAuthorityInfo(app, streamId);
        String key = VideoManagerConstants.WVP_SERVER_STREAM_PREFIX  + userSetting.getServerId() + "_" + type + "_" + app + "_" + streamId + "_" + mediaServerItem.getId();
        if (streamAuthorityInfo != null) {
            mediaInfo.setCallId(streamAuthorityInfo.getCallId());
        }
        redisTemplate.opsForValue().set(key, mediaInfo);
    }

    @Override
    public void removeStream(String mediaServerId, String type, String app, String streamId) {
        String key = VideoManagerConstants.WVP_SERVER_STREAM_PREFIX + userSetting.getServerId() + "_" + type + "_"  + app + "_" + streamId + "_" + mediaServerId;
        redisTemplate.delete(key);
    }

    @Override
    public void removeStream(String mediaServerId, String type) {
        String key = VideoManagerConstants.WVP_SERVER_STREAM_PREFIX + userSetting.getServerId() + "_" + type + "_*_*_" + mediaServerId;
        List<Object> streams = RedisUtil.scan(redisTemplate, key);
        for (Object stream : streams) {
            redisTemplate.delete(stream);
        }
    }

    @Override
    public List<MediaInfo> getStreams(String mediaServerId, String type) {
        List<MediaInfo> result = new ArrayList<>();
        String key = VideoManagerConstants.WVP_SERVER_STREAM_PREFIX + userSetting.getServerId() + "_" + type + "_*_*_" + mediaServerId;
        List<Object> streams = RedisUtil.scan(redisTemplate, key);
        for (Object stream : streams) {
            MediaInfo mediaInfo = (MediaInfo)redisTemplate.opsForValue().get(stream);
            result.add(mediaInfo);
        }
        return result;
    }

    @Override
    public void updateDevice(Device device) {
        String key = VideoManagerConstants.DEVICE_PREFIX + userSetting.getServerId() + "_" + device.getDeviceId();
        redisTemplate.opsForValue().set(key, device);
    }

    @Override
    public void removeDevice(String deviceId) {
        String key = VideoManagerConstants.DEVICE_PREFIX + userSetting.getServerId() + "_" + deviceId;
        redisTemplate.delete(key);
    }

    @Override
    public void removeAllDevice() {
        String scanKey = VideoManagerConstants.DEVICE_PREFIX + userSetting.getServerId() + "_*";
        List<Object> keys = RedisUtil.scan(redisTemplate, scanKey);
        for (Object key : keys) {
            redisTemplate.delete(key);
        }
    }

    @Override
    public List<Device> getAllDevices() {
        String scanKey = VideoManagerConstants.DEVICE_PREFIX + userSetting.getServerId() + "_*";
        List<Device> result = new ArrayList<>();
        List<Object> keys = RedisUtil.scan(redisTemplate, scanKey);
        for (Object o : keys) {
            String key = (String) o;
            Device device = JsonUtil.redisJsonToObject(redisTemplate, key, Device.class);
            if (Objects.nonNull(device)) {
                // 只取没有存过得
                result.add(JsonUtil.redisJsonToObject(redisTemplate, key, Device.class));
            }
        }

        return result;
    }

    @Override
    public Device getDevice(String deviceId) {
        String key = VideoManagerConstants.DEVICE_PREFIX + userSetting.getServerId() + "_" + deviceId;
        Device device = JsonUtil.redisJsonToObject(redisTemplate, key, Device.class);
        if (device == null){
            device = deviceMapper.getDeviceByDeviceId(deviceId);
            if (device != null) {
                updateDevice(device);
            }
        }
        return device;
    }

    @Override
    public void updateGpsMsgInfo(GPSMsgInfo gpsMsgInfo) {
        String key = VideoManagerConstants.WVP_STREAM_GPS_MSG_PREFIX + userSetting.getServerId() + "_" + gpsMsgInfo.getId();
        Duration duration = Duration.ofSeconds(60L);
        redisTemplate.opsForValue().set(key, gpsMsgInfo, duration);
        // 默认GPS消息保存1分钟
    }

    @Override
    public GPSMsgInfo getGpsMsgInfo(String gbId) {
        String key = VideoManagerConstants.WVP_STREAM_GPS_MSG_PREFIX + userSetting.getServerId() + "_" + gbId;
        return JsonUtil.redisJsonToObject(redisTemplate, key, GPSMsgInfo.class);
    }

    @Override
    public List<GPSMsgInfo> getAllGpsMsgInfo() {
        String scanKey = VideoManagerConstants.WVP_STREAM_GPS_MSG_PREFIX + userSetting.getServerId() + "_*";
        List<GPSMsgInfo> result = new ArrayList<>();
        List<Object> keys = RedisUtil.scan(redisTemplate, scanKey);
        for (Object o : keys) {
            String key = (String) o;
            GPSMsgInfo gpsMsgInfo = JsonUtil.redisJsonToObject(redisTemplate, key, GPSMsgInfo.class);
            if (Objects.nonNull(gpsMsgInfo) && !gpsMsgInfo.isStored()) { // 只取没有存过得
                result.add(JsonUtil.redisJsonToObject(redisTemplate, key, GPSMsgInfo.class));
            }
        }

        return result;
    }

    @Override
    public void updateStreamAuthorityInfo(String app, String stream, StreamAuthorityInfo streamAuthorityInfo) {
        String key = VideoManagerConstants.MEDIA_STREAM_AUTHORITY + userSetting.getServerId() + "_" + app+ "_" + stream;
        redisTemplate.opsForValue().set(key, streamAuthorityInfo);
    }

    @Override
    public void removeStreamAuthorityInfo(String app, String stream) {
        String key = VideoManagerConstants.MEDIA_STREAM_AUTHORITY + userSetting.getServerId() + "_" + app+ "_" + stream ;
        redisTemplate.delete(key);
    }

    @Override
    public StreamAuthorityInfo getStreamAuthorityInfo(String app, String stream) {
        String key = VideoManagerConstants.MEDIA_STREAM_AUTHORITY + userSetting.getServerId() + "_" + app+ "_" + stream ;
        return JsonUtil.redisJsonToObject(redisTemplate, key, StreamAuthorityInfo.class);

    }

    @Override
    public List<StreamAuthorityInfo> getAllStreamAuthorityInfo() {
        String scanKey = VideoManagerConstants.MEDIA_STREAM_AUTHORITY + userSetting.getServerId() + "_*_*" ;
        List<StreamAuthorityInfo> result = new ArrayList<>();
        List<Object> keys = RedisUtil.scan(redisTemplate, scanKey);
        for (Object o : keys) {
            String key = (String) o;
            result.add(JsonUtil.redisJsonToObject(redisTemplate, key, StreamAuthorityInfo.class));
        }
        return result;
    }


    @Override
    public MediaInfo getStreamInfo(String app, String streamId, String mediaServerId) {
        String scanKey = VideoManagerConstants.WVP_SERVER_STREAM_PREFIX  + userSetting.getServerId() + "_*_" + app + "_" + streamId + "_" + mediaServerId;

        MediaInfo result = null;
        List<Object> keys = RedisUtil.scan(redisTemplate, scanKey);
        if (keys.size() > 0) {
            String key = (String) keys.get(0);
            result = JsonUtil.redisJsonToObject(redisTemplate, key, MediaInfo.class);
        }

        return result;
    }

    @Override
    public void addCpuInfo(double cpuInfo) {
        String key = VideoManagerConstants.SYSTEM_INFO_CPU_PREFIX + userSetting.getServerId();
        Map<String, String> infoMap = new HashMap<>();
        infoMap.put("time", DateUtil.getNow());
        infoMap.put("data", String.valueOf(cpuInfo));
        redisTemplate.opsForList().rightPush(key, infoMap);
        // 每秒一个，最多只存30个
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size >= 30) {
            for (int i = 0; i < size - 30; i++) {
                redisTemplate.opsForList().leftPop(key);
            }
        }
    }

    @Override
    public void addMemInfo(double memInfo) {
        String key = VideoManagerConstants.SYSTEM_INFO_MEM_PREFIX + userSetting.getServerId();
        Map<String, String> infoMap = new HashMap<>();
        infoMap.put("time", DateUtil.getNow());
        infoMap.put("data", String.valueOf(memInfo));
        redisTemplate.opsForList().rightPush(key, infoMap);
        // 每秒一个，最多只存30个
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size >= 30) {
            for (int i = 0; i < size - 30; i++) {
                redisTemplate.opsForList().leftPop(key);
            }
        }
    }

    @Override
    public void addNetInfo(Map<String, Double> networkInterfaces) {
        String key = VideoManagerConstants.SYSTEM_INFO_NET_PREFIX + userSetting.getServerId();
        Map<String, Object> infoMap = new HashMap<>();
        infoMap.put("time", DateUtil.getNow());
        for (String netKey : networkInterfaces.keySet()) {
            infoMap.put(netKey, networkInterfaces.get(netKey));
        }
        redisTemplate.opsForList().rightPush(key, infoMap);
        // 每秒一个，最多只存30个
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size >= 30) {
            for (int i = 0; i < size - 30; i++) {
                redisTemplate.opsForList().leftPop(key);
            }
        }
    }

    @Override
    public void addDiskInfo(List<Map<String, Object>> diskInfo) {

        String key = VideoManagerConstants.SYSTEM_INFO_DISK_PREFIX + userSetting.getServerId();
        redisTemplate.opsForValue().set(key, diskInfo);
    }

    @Override
    public SystemAllInfo getSystemInfo() {
        String cpuKey = VideoManagerConstants.SYSTEM_INFO_CPU_PREFIX + userSetting.getServerId();
        String memKey = VideoManagerConstants.SYSTEM_INFO_MEM_PREFIX + userSetting.getServerId();
        String netKey = VideoManagerConstants.SYSTEM_INFO_NET_PREFIX + userSetting.getServerId();
        String diskKey = VideoManagerConstants.SYSTEM_INFO_DISK_PREFIX + userSetting.getServerId();
        SystemAllInfo systemAllInfo = new SystemAllInfo();
        systemAllInfo.setCpu(redisTemplate.opsForList().range(cpuKey, 0, -1));
        systemAllInfo.setMem(redisTemplate.opsForList().range(memKey, 0, -1));
        systemAllInfo.setNet(redisTemplate.opsForList().range(netKey, 0, -1));

        systemAllInfo.setDisk(redisTemplate.opsForValue().get(diskKey));
        systemAllInfo.setNetTotal(SystemInfoUtils.getNetworkTotal());
        return systemAllInfo;
    }

    @Override
    public void sendMobilePositionMsg(JSONObject jsonObject) {
        String key = VideoManagerConstants.VM_MSG_SUBSCRIBE_MOBILE_POSITION;
        logger.debug("[redis发送通知] 发送 移动位置 {}: {}", key, jsonObject.toString());
        redisTemplate.convertAndSend(key, jsonObject);
    }

    @Override
    public void sendStreamPushRequestedMsg(MessageForPushChannel msg) {
        String key = VideoManagerConstants.VM_MSG_STREAM_PUSH_REQUESTED;
        logger.info("[redis发送通知] 发送 推流被请求 {}: {}/{}", key, msg.getApp(), msg.getStream());
        redisTemplate.convertAndSend(key, JSON.toJSON(msg));
    }

    @Override
    public void sendAlarmMsg(AlarmChannelMessage msg) {
        // 此消息用于对接第三方服务下级来的消息内容
        String key = VideoManagerConstants.VM_MSG_SUBSCRIBE_ALARM;
        logger.info("[redis发送通知] 发送 报警{}: {}", key, JSON.toJSON(msg));
        redisTemplate.convertAndSend(key, JSON.toJSON(msg));
    }

    @Override
    public boolean deviceIsOnline(String deviceId) {
        return getDevice(deviceId).isOnLine();
    }


    @Override
    public void sendStreamPushRequestedMsgForStatus() {
        String key = VideoManagerConstants.VM_MSG_GET_ALL_ONLINE_REQUESTED;
        logger.info("[redis通知] 发送 获取所有推流设备的状态");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(key, key);
        redisTemplate.convertAndSend(key, jsonObject);
    }

    @Override
    public int getPushStreamCount(String id) {
        String key = VideoManagerConstants.WVP_SERVER_STREAM_PREFIX + userSetting.getServerId() + "_PUSH_*_*_" + id;
        return RedisUtil.scan(redisTemplate, key).size();
    }

    @Override
    public int getProxyStreamCount(String id) {
        String key = VideoManagerConstants.WVP_SERVER_STREAM_PREFIX + userSetting.getServerId() + "_PULL_*_*_" + id;
        return RedisUtil.scan(redisTemplate, key).size();
    }

    @Override
    public int getGbSendCount(String id) {
        String key = VideoManagerConstants.PLATFORM_SEND_RTP_INFO_PREFIX
                + userSetting.getServerId() + "_*_" + id + "_*";
        return RedisUtil.scan(redisTemplate, key).size();
    }

    @Override
    public void sendDeviceOrChannelStatus(String deviceId, String channelId, boolean online) {
        String key = VideoManagerConstants.VM_MSG_SUBSCRIBE_DEVICE_STATUS;
        StringBuilder msg = new StringBuilder();
        msg.append(deviceId);
        if (channelId != null) {
            msg.append(":").append(channelId);
        }
        msg.append(" ").append(online? "ON":"OFF");
        logger.info("[redis通知] 推送设备/通道状态-> {} ", msg);
        // 使用 RedisTemplate<Object, Object> 发送字符串消息会导致发送的消息多带了双引号
        stringRedisTemplate.convertAndSend(key, msg.toString());
    }

    @Override
    public void sendChannelAddOrDelete(String deviceId, String channelId, boolean add) {
        String key = VideoManagerConstants.VM_MSG_SUBSCRIBE_DEVICE_STATUS;


        StringBuilder msg = new StringBuilder();
        msg.append(deviceId);
        if (channelId != null) {
            msg.append(":").append(channelId);
        }
        msg.append(" ").append(add? "ADD":"DELETE");
        logger.info("[redis通知] 推送通道-> {}", msg);
        // 使用 RedisTemplate<Object, Object> 发送字符串消息会导致发送的消息多带了双引号
        stringRedisTemplate.convertAndSend(key, msg.toString());
    }

    @Override
    public void sendPlatformStartPlayMsg(MessageForPushChannel msg) {
        String key = VideoManagerConstants.VM_MSG_STREAM_START_PLAY_NOTIFY;
        logger.info("[redis发送通知] 发送 推流被上级平台观看 {}: {}/{}->{}", key, msg.getApp(), msg.getStream(), msg.getPlatFormId());
        redisTemplate.convertAndSend(key, JSON.toJSON(msg));
    }

    @Override
    public void sendPlatformStopPlayMsg(SendRtpItem sendRtpItem, ParentPlatform platform) {

        MessageForPushChannel msg = MessageForPushChannel.getInstance(0,
                sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getChannelId(),
                sendRtpItem.getPlatformId(), platform.getName(), userSetting.getServerId(), sendRtpItem.getMediaServerId());
        msg.setPlatFormIndex(platform.getId());

        String key = VideoManagerConstants.VM_MSG_STREAM_STOP_PLAY_NOTIFY;
        logger.info("[redis发送通知] 发送 上级平台停止观看 {}: {}/{}->{}", key, sendRtpItem.getApp(), sendRtpItem.getStream(), platform.getServerGBId());
        redisTemplate.convertAndSend(key, JSON.toJSON(msg));
    }

    @Override
    public void addPushListItem(String app, String stream, MediaArrivalEvent event) {
        String key = VideoManagerConstants.PUSH_STREAM_LIST + app + "_" + stream;
        StreamPushItem streamPushItem = StreamPushItem.getInstance(event, userSetting.getServerId());
        redisTemplate.opsForValue().set(key, streamPushItem);
    }

    @Override
    public StreamPushItem getPushListItem(String app, String stream) {
        String key = VideoManagerConstants.PUSH_STREAM_LIST + app + "_" + stream;
        return (StreamPushItem)redisTemplate.opsForValue().get(key);
    }

    @Override
    public void removePushListItem(String app, String stream, String mediaServerId) {
        String key = VideoManagerConstants.PUSH_STREAM_LIST + app + "_" + stream;
        StreamPushItem param = (StreamPushItem)redisTemplate.opsForValue().get(key);
        if (param != null && param.getMediaServerId().equalsIgnoreCase(mediaServerId)) {
            redisTemplate.delete(key);
        }

    }

    @Override
    public void sendPushStreamClose(MessageForPushChannel msg) {
        String key = VideoManagerConstants.VM_MSG_STREAM_PUSH_CLOSE_REQUESTED;
        logger.info("[redis发送通知] 发送 停止向上级推流 {}: {}/{}->{}", key, msg.getApp(), msg.getStream(), msg.getPlatFormId());
        redisTemplate.convertAndSend(key, JSON.toJSON(msg));
    }

    @Override
    public void addWaiteSendRtpItem(SendRtpItem sendRtpItem, int platformPlayTimeout) {
        String key = VideoManagerConstants.WAITE_SEND_PUSH_STREAM + sendRtpItem.getApp() + "_" + sendRtpItem.getStream();
        redisTemplate.opsForValue().set(key, sendRtpItem);
    }

    @Override
    public SendRtpItem getWaiteSendRtpItem(String app, String stream) {
        String key = VideoManagerConstants.WAITE_SEND_PUSH_STREAM + app + "_" + stream;
        return JsonUtil.redisJsonToObject(redisTemplate, key, SendRtpItem.class);
    }

    @Override
    public void sendStartSendRtp(SendRtpItem sendRtpItem) {
        String key = VideoManagerConstants.START_SEND_PUSH_STREAM + sendRtpItem.getApp() + "_" + sendRtpItem.getStream();
        logger.info("[redis发送通知] 通知其他WVP推流 {}: {}/{}->{}", key, sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getPlatformId());
        redisTemplate.convertAndSend(key, JSON.toJSON(sendRtpItem));
    }

    @Override
    public void sendPushStreamOnline(SendRtpItem sendRtpItem) {
        String key = VideoManagerConstants.VM_MSG_STREAM_PUSH_CLOSE_REQUESTED;
        logger.info("[redis发送通知] 流上线 {}: {}/{}->{}", key, sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getPlatformId());
        redisTemplate.convertAndSend(key, JSON.toJSON(sendRtpItem));
    }
}
