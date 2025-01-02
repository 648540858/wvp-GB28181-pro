package com.genersoft.iot.vmp.storager;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.ServerInfo;
import com.genersoft.iot.vmp.common.SystemAllInfo;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.service.bean.MessageForPushChannel;
import com.genersoft.iot.vmp.storager.dao.dto.PlatformRegisterInfo;

import java.util.List;
import java.util.Map;

public interface IRedisCatchStorage {

    /**
     * 计数器。为cseq进行计数
     *
     * @return
     */
    Long getCSEQ();

    void updatePlatformCatchInfo(PlatformCatch parentPlatformCatch);

    PlatformCatch queryPlatformCatchInfo(String platformGbId);

    void delPlatformCatchInfo(String platformGbId);

    void updatePlatformRegisterInfo(String callId, PlatformRegisterInfo platformRegisterInfo);

    PlatformRegisterInfo queryPlatformRegisterInfo(String callId);

    void delPlatformRegisterInfo(String callId);

    /**
     * 在redis添加wvp的信息
     */
    void updateWVPInfo(ServerInfo serverInfo, int time);

    /**
     * 发送推流生成与推流消失消息
     * @param jsonObject 消息内容
     */
    void sendStreamChangeMsg(String type, JSONObject jsonObject);

    /**
     * 发送报警消息
     * @param msg 消息内容
     */
    void sendAlarmMsg(AlarmChannelMessage msg);

    /**
     * 添加流信息到redis
     * @param mediaServerItem
     * @param app
     * @param streamId
     */
    void addStream(MediaServer mediaServerItem, String type, String app, String streamId, MediaInfo item);

    /**
     * 移除流信息从redis
     * @param mediaServerId
     * @param app
     * @param streamId
     */
    void removeStream(String mediaServerId, String type, String app, String streamId);


    /**
     * 移除流信息从redis
     * @param mediaServerId
     */
    void removeStream(String mediaServerId, String type);

    List<MediaInfo> getStreams(String mediaServerId, String pull);

    /**
     * 将device信息写入redis
     * @param device
     */
    void updateDevice(Device device);

    void removeDevice(String deviceId);

    /**
     * 获取Device
     */
    Device getDevice(String deviceId);

    void resetAllCSEQ();

    void updateGpsMsgInfo(GPSMsgInfo gpsMsgInfo);

    GPSMsgInfo getGpsMsgInfo(String gbId);

    List<GPSMsgInfo> getAllGpsMsgInfo();

    MediaInfo getStreamInfo(String app, String streamId, String mediaServerId);

    MediaInfo getProxyStream(String app, String streamId);

    void addCpuInfo(double cpuInfo);

    void addMemInfo(double memInfo);

    void addNetInfo(Map<String, Double> networkInterfaces);

    void sendMobilePositionMsg(JSONObject jsonObject);

    void sendStreamPushRequestedMsg(MessageForPushChannel messageForPushChannel);

    /**
     * 判断设备状态
     * @param deviceId 设备ID
     * @return
     */
    public boolean deviceIsOnline(String deviceId);

    /**
     * 存储推流的鉴权信息
     * @param app 应用名
     * @param stream 流
     * @param streamAuthorityInfo 鉴权信息
     */
    void updateStreamAuthorityInfo(String app, String stream, StreamAuthorityInfo streamAuthorityInfo);

    /**
     * 移除推流的鉴权信息
     * @param app 应用名
     * @param streamId 流
     */
    void removeStreamAuthorityInfo(String app, String streamId);

    /**
     * 获取推流的鉴权信息
     * @param app 应用名
     * @param stream 流
     * @return
     */
    StreamAuthorityInfo getStreamAuthorityInfo(String app, String stream);

    List<StreamAuthorityInfo> getAllStreamAuthorityInfo();

    /**
     * 发送redis消息 查询所有推流设备的状态
     */
    void sendStreamPushRequestedMsgForStatus();

    SystemAllInfo getSystemInfo();

    int getPushStreamCount(String id);

    int getProxyStreamCount(String id);

    int getGbSendCount(String id);

    void addDiskInfo(List<Map<String, Object>> diskInfo);

    List<SendRtpInfo> queryAllSendRTPServer();

    List<Device> getAllDevices();

    void removeAllDevice();

    void sendDeviceOrChannelStatus(String deviceId, String channelId, boolean online);

    void sendChannelAddOrDelete(String deviceId, String channelId, boolean add);

    void sendPlatformStartPlayMsg(SendRtpInfo sendRtpItem, DeviceChannel channel, Platform platform);

    void sendPlatformStopPlayMsg(SendRtpInfo sendRtpItem, Platform platform, CommonGBChannel channel);

    void addPushListItem(String app, String stream, MediaInfo param);

    MediaInfo getPushListItem(String app, String stream);

    void removePushListItem(String app, String stream, String mediaServerId);

    void sendPushStreamClose(MessageForPushChannel messageForPushChannel);

    void addWaiteSendRtpItem(SendRtpInfo sendRtpItem, int platformPlayTimeout);

    SendRtpInfo getWaiteSendRtpItem(String app, String stream);

    void sendStartSendRtp(SendRtpInfo sendRtpItem);

    void sendPushStreamOnline(SendRtpInfo sendRtpItem);

    ServerInfo queryServerInfo(String serverId);

    String chooseOneServer(String serverId);


}
