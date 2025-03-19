package com.genersoft.iot.vmp.storager.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.ServerInfo;
import com.genersoft.iot.vmp.common.SystemAllInfo;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.DeviceMapper;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.service.bean.MessageForPushChannel;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.dto.PlatformRegisterInfo;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.utils.JsonUtil;
import com.genersoft.iot.vmp.utils.SystemInfoUtils;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

@SuppressWarnings("rawtypes")
@Slf4j
@Component
public class RedisCatchStorageImpl implements IRedisCatchStorage {


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
    public List<SendRtpInfo> queryAllSendRTPServer() {
        return Collections.emptyList();
    }

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
    public void resetAllCSEQ() {
        String key = VideoManagerConstants.SIP_CSEQ_PREFIX  + userSetting.getServerId();
        redisTemplate.opsForValue().set(key, 1);
    }

    @Override
    public void updatePlatformCatchInfo(PlatformCatch parentPlatformCatch) {
        String key = VideoManagerConstants.PLATFORM_CATCH_PREFIX  + userSetting.getServerId() + "_" +  parentPlatformCatch.getId();
        redisTemplate.opsForValue().set(key, parentPlatformCatch);
    }

    @Override
    public PlatformCatch queryPlatformCatchInfo(String platformGbId) {
        return (PlatformCatch)redisTemplate.opsForValue().get(VideoManagerConstants.PLATFORM_CATCH_PREFIX + userSetting.getServerId() + "_" + platformGbId);
    }

    @Override
    public void delPlatformCatchInfo(String platformGbId) {
        redisTemplate.delete(VideoManagerConstants.PLATFORM_CATCH_PREFIX + userSetting.getServerId() + "_" + platformGbId);
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
    public void updateWVPInfo(ServerInfo serverInfo, int time) {
        String key = VideoManagerConstants.WVP_SERVER_PREFIX + userSetting.getServerId();
        Duration duration = Duration.ofSeconds(time);
        redisTemplate.opsForValue().set(key, serverInfo, duration);
        // 设置平台的分数值
        String setKey = VideoManagerConstants.WVP_SERVER_LIST;
        // 首次设置就设置为0, 后续值越小说明越是最近启动的
        redisTemplate.opsForZSet().add(setKey, userSetting.getServerId(), System.currentTimeMillis());
    }

    @Override
    public void sendStreamChangeMsg(String type, JSONObject jsonObject) {
        String key = VideoManagerConstants.WVP_MSG_STREAM_CHANGE_PREFIX + type;
        log.info("[redis 流变化事件] 发送 {}: {}", key, jsonObject.toString());
        redisTemplate.convertAndSend(key, jsonObject);
    }

    @Override
    public void addStream(MediaServer mediaServerItem, String type, String app, String streamId, MediaInfo mediaInfo) {
        // 查找是否使用了callID
        StreamAuthorityInfo streamAuthorityInfo = getStreamAuthorityInfo(app, streamId);
        String key = VideoManagerConstants.WVP_SERVER_STREAM_PREFIX  + userSetting.getServerId() + "_" + type.toUpperCase() + "_" + app + "_" + streamId + "_" + mediaServerItem.getId();
        if (streamAuthorityInfo != null) {
            mediaInfo.setCallId(streamAuthorityInfo.getCallId());
        }
        redisTemplate.opsForValue().set(key, mediaInfo);
    }

    @Override
    public void removeStream(String mediaServerId, String type, String app, String streamId) {
        String key = VideoManagerConstants.WVP_SERVER_STREAM_PREFIX + userSetting.getServerId() + "_" + type.toUpperCase() + "_"  + app + "_" + streamId + "_" + mediaServerId;
        redisTemplate.delete(key);
    }

    @Override
    public void removeStream(String mediaServerId, String type) {
        String key = VideoManagerConstants.WVP_SERVER_STREAM_PREFIX + userSetting.getServerId() + "_" + type.toUpperCase() + "_*_*_" + mediaServerId;
        List<Object> streams = RedisUtil.scan(redisTemplate, key);
        for (Object stream : streams) {
            redisTemplate.delete(stream);
        }
    }

    @Override
    public List<MediaInfo> getStreams(String mediaServerId, String type) {
        List<MediaInfo> result = new ArrayList<>();
        String key = VideoManagerConstants.WVP_SERVER_STREAM_PREFIX + userSetting.getServerId() + "_" + type.toUpperCase() + "_*_*_" + mediaServerId;
        List<Object> streams = RedisUtil.scan(redisTemplate, key);
        for (Object stream : streams) {
            MediaInfo mediaInfo = (MediaInfo)redisTemplate.opsForValue().get(stream);
            result.add(mediaInfo);
        }
        return result;
    }

    @Override
    public void updateDevice(Device device) {
        String key = VideoManagerConstants.DEVICE_PREFIX;
        redisTemplate.opsForHash().put(key, device.getDeviceId(), device);
    }

    @Override
    public void removeDevice(String deviceId) {
        String key = VideoManagerConstants.DEVICE_PREFIX;
        redisTemplate.opsForHash().delete(key, deviceId);
    }

    @Override
    public void removeAllDevice() {
        String key = VideoManagerConstants.DEVICE_PREFIX;
        redisTemplate.delete(key);
    }

    @Override
    public List<Device> getAllDevices() {
        String key = VideoManagerConstants.DEVICE_PREFIX;
        List<Device> result = new ArrayList<>();
        List<Object> values = redisTemplate.opsForHash().values(key);
        for (Object value : values) {
            if (Objects.nonNull(value)) {
                result.add((Device)value);
            }
        }
        return result;
    }

    @Override
    public Device getDevice(String deviceId) {
        String key = VideoManagerConstants.DEVICE_PREFIX;
        Device device;
        Object object = redisTemplate.opsForHash().get(key, deviceId);
        if (object == null){
            device = deviceMapper.getDeviceByDeviceId(deviceId);
            if (device != null) {
                updateDevice(device);
            }
        }else {
            device = (Device)object;
        }
        return device;
    }

    @Override
    public void updateGpsMsgInfo(GPSMsgInfo gpsMsgInfo) {
        String key = VideoManagerConstants.WVP_STREAM_GPS_MSG_PREFIX + userSetting.getServerId();
        Duration duration = Duration.ofSeconds(60L);
        redisTemplate.opsForHash().put(key, gpsMsgInfo.getId(),gpsMsgInfo);
        redisTemplate.expire(key, duration);
        // 默认GPS消息保存1分钟
    }

    @Override
    public GPSMsgInfo getGpsMsgInfo(String channelId) {
        String key = VideoManagerConstants.WVP_STREAM_GPS_MSG_PREFIX + userSetting.getServerId();
        return (GPSMsgInfo) redisTemplate.opsForHash().get(key, channelId);
    }

    @Override
    public List<GPSMsgInfo> getAllGpsMsgInfo() {
        String key = VideoManagerConstants.WVP_STREAM_GPS_MSG_PREFIX + userSetting.getServerId();
        List<GPSMsgInfo> result = new ArrayList<>();
        List<Object> values = redisTemplate.opsForHash().values(key);
        for (Object value : values) {
            result.add((GPSMsgInfo)value);
        }
        return result;
    }

    @Override
    public void updateStreamAuthorityInfo(String app, String stream, StreamAuthorityInfo streamAuthorityInfo) {
        String key = VideoManagerConstants.MEDIA_STREAM_AUTHORITY + userSetting.getServerId();
        String objectKey = app+ "_" + stream;
        redisTemplate.opsForHash().put(key, objectKey, streamAuthorityInfo);
    }

    @Override
    public void removeStreamAuthorityInfo(String app, String stream) {
        String key = VideoManagerConstants.MEDIA_STREAM_AUTHORITY + userSetting.getServerId();
        String objectKey = app+ "_" + stream;
        redisTemplate.opsForHash().delete(key, objectKey);
    }

    @Override
    public StreamAuthorityInfo getStreamAuthorityInfo(String app, String stream) {
        String key = VideoManagerConstants.MEDIA_STREAM_AUTHORITY + userSetting.getServerId();
        String objectKey = app+ "_" + stream;
        return (StreamAuthorityInfo)redisTemplate.opsForHash().get(key, objectKey);

    }

    @Override
    public List<StreamAuthorityInfo> getAllStreamAuthorityInfo() {
        String key = VideoManagerConstants.MEDIA_STREAM_AUTHORITY + userSetting.getServerId();
        List<StreamAuthorityInfo> result = new ArrayList<>();
        List<Object> values = redisTemplate.opsForHash().values(key);
        for (Object value : values) {
            result.add((StreamAuthorityInfo)value);
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
    public MediaInfo getProxyStream(String app, String streamId) {
        String scanKey = VideoManagerConstants.WVP_SERVER_STREAM_PREFIX  + userSetting.getServerId() + "_PULL_" + app + "_" + streamId + "_*";

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
        log.debug("[redis发送通知] 发送 移动位置 {}: {}", key, jsonObject.toString());
        redisTemplate.convertAndSend(key, jsonObject);
    }

    @Override
    public void sendStreamPushRequestedMsg(MessageForPushChannel msg) {
        String key = VideoManagerConstants.VM_MSG_STREAM_PUSH_REQUESTED;
        log.info("[redis发送通知] 发送 推流被请求 {}: {}/{}", key, msg.getApp(), msg.getStream());
        redisTemplate.convertAndSend(key, JSON.toJSON(msg));
    }

    @Override
    public void sendAlarmMsg(AlarmChannelMessage msg) {
        // 此消息用于对接第三方服务下级来的消息内容
        String key = VideoManagerConstants.VM_MSG_SUBSCRIBE_ALARM;
        log.info("[redis发送通知] 发送 报警{}: {}", key, JSON.toJSON(msg));
        redisTemplate.convertAndSend(key, JSON.toJSON(msg));
    }

    @Override
    public boolean deviceIsOnline(String deviceId) {
        return getDevice(deviceId).isOnLine();
    }


    @Override
    public void sendStreamPushRequestedMsgForStatus() {
        String key = VideoManagerConstants.VM_MSG_GET_ALL_ONLINE_REQUESTED;
        log.info("[redis通知] 发送 获取所有推流设备的状态");
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
        String key = VideoManagerConstants.SEND_RTP_INFO_CALLID;
        return redisTemplate.opsForHash().size(key).intValue();
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
        log.info("[redis通知] 推送设备/通道状态-> {} ", msg);
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
        log.info("[redis通知] 推送通道-> {}", msg);
        // 使用 RedisTemplate<Object, Object> 发送字符串消息会导致发送的消息多带了双引号
        stringRedisTemplate.convertAndSend(key, msg.toString());
    }

    @Override
    public void sendPlatformStartPlayMsg(SendRtpInfo sendRtpItem, DeviceChannel channel, Platform platform) {
        if (sendRtpItem.getPlayType() == InviteStreamType.PUSH && platform  != null) {
            MessageForPushChannel messageForPushChannel = MessageForPushChannel.getInstance(0, sendRtpItem.getApp(), sendRtpItem.getStream(),
                    channel.getDeviceId(), platform.getServerGBId(), platform.getName(), userSetting.getServerId(),
                    sendRtpItem.getMediaServerId());
            messageForPushChannel.setPlatFormIndex(platform.getId());
            String key = VideoManagerConstants.VM_MSG_STREAM_START_PLAY_NOTIFY;
            log.info("[redis发送通知] 发送 推流被上级平台观看 {}: {}/{}->{}", key, sendRtpItem.getApp(), sendRtpItem.getStream(), platform.getServerGBId());
            redisTemplate.convertAndSend(key, JSON.toJSON(messageForPushChannel));
        }
    }

    @Override
    public void sendPlatformStopPlayMsg(SendRtpInfo sendRtpItem, Platform platform, CommonGBChannel channel) {

        MessageForPushChannel msg = MessageForPushChannel.getInstance(0,
                sendRtpItem.getApp(), sendRtpItem.getStream(), channel.getGbDeviceId(),
                sendRtpItem.getTargetId(), platform.getName(), userSetting.getServerId(), sendRtpItem.getMediaServerId());
        msg.setPlatFormIndex(platform.getId());

        String key = VideoManagerConstants.VM_MSG_STREAM_STOP_PLAY_NOTIFY;
        log.info("[redis发送通知] 发送 上级平台停止观看 {}: {}/{}->{}", key, sendRtpItem.getApp(), sendRtpItem.getStream(), platform.getServerGBId());
        redisTemplate.convertAndSend(key, JSON.toJSON(msg));
    }

    @Override
    public void addPushListItem(String app, String stream, MediaInfo mediaInfo) {
        String key = VideoManagerConstants.PUSH_STREAM_LIST + app + "_" + stream;
        redisTemplate.opsForValue().set(key, mediaInfo);
    }

    @Override
    public MediaInfo getPushListItem(String app, String stream) {
        String key = VideoManagerConstants.PUSH_STREAM_LIST + app + "_" + stream;
        return (MediaInfo)redisTemplate.opsForValue().get(key);
    }

    @Override
    public void removePushListItem(String app, String stream, String mediaServerId) {
        String key = VideoManagerConstants.PUSH_STREAM_LIST + app + "_" + stream;
        MediaInfo param = (MediaInfo)redisTemplate.opsForValue().get(key);
        if (param != null) {
            redisTemplate.delete(key);
        }
    }

    @Override
    public void sendPushStreamClose(MessageForPushChannel msg) {
        String key = VideoManagerConstants.VM_MSG_STREAM_PUSH_CLOSE_REQUESTED;
        log.info("[redis发送通知] 发送 停止向上级推流 {}: {}/{}->{}", key, msg.getApp(), msg.getStream(), msg.getPlatFormId());
        redisTemplate.convertAndSend(key, JSON.toJSON(msg));
    }

    @Override
    public void addWaiteSendRtpItem(SendRtpInfo sendRtpItem, int platformPlayTimeout) {
        String key = VideoManagerConstants.WAITE_SEND_PUSH_STREAM + sendRtpItem.getApp() + "_" + sendRtpItem.getStream();
        redisTemplate.opsForValue().set(key, sendRtpItem);
    }

    @Override
    public SendRtpInfo getWaiteSendRtpItem(String app, String stream) {
        String key = VideoManagerConstants.WAITE_SEND_PUSH_STREAM + app + "_" + stream;
        return JsonUtil.redisJsonToObject(redisTemplate, key, SendRtpInfo.class);
    }

    @Override
    public void sendStartSendRtp(SendRtpInfo sendRtpItem) {
        String key = VideoManagerConstants.START_SEND_PUSH_STREAM + sendRtpItem.getApp() + "_" + sendRtpItem.getStream();
        log.info("[redis发送通知] 通知其他WVP推流 {}: {}/{}->{}", key, sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getTargetId());
        redisTemplate.convertAndSend(key, JSON.toJSON(sendRtpItem));
    }

    @Override
    public void sendPushStreamOnline(SendRtpInfo sendRtpItem) {
        String key = VideoManagerConstants.VM_MSG_STREAM_PUSH_CLOSE_REQUESTED;
        log.info("[redis发送通知] 流上线 {}: {}/{}->{}", key, sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getTargetId());
        redisTemplate.convertAndSend(key, JSON.toJSON(sendRtpItem));
    }

    @Override
    public ServerInfo queryServerInfo(String serverId) {
        String key = VideoManagerConstants.WVP_SERVER_PREFIX + serverId;
        return (ServerInfo)redisTemplate.opsForValue().get(key);
    }

    @Override
    public String chooseOneServer(String serverId) {
        String key = VideoManagerConstants.WVP_SERVER_LIST;
        redisTemplate.opsForZSet().remove(key, serverId);
        Set<Object> range = redisTemplate.opsForZSet().range(key, 0, 0);
        if (range == null || range.isEmpty()) {
            return null;
        }
        return (String) range.iterator().next();
    }
}
