package com.genersoft.iot.vmp.storager;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.SystemAllInfo;
import com.genersoft.iot.vmp.gb28181.bean.AlarmChannelMessage;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatformCatch;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;
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

    void updatePlatformCatchInfo(ParentPlatformCatch parentPlatformCatch);

    ParentPlatformCatch queryPlatformCatchInfo(String platformGbId);

    void delPlatformCatchInfo(String platformGbId);

    void delPlatformKeepalive(String platformGbId);

    void delPlatformRegister(String platformGbId);

    void updatePlatformRegisterInfo(String callId, PlatformRegisterInfo platformRegisterInfo);

    PlatformRegisterInfo queryPlatformRegisterInfo(String callId);

    void delPlatformRegisterInfo(String callId);

    void updateSendRTPSever(SendRtpItem sendRtpItem);

    /**
     * 查询RTP推送信息缓存
     * @param platformGbId
     * @param channelId
     * @return sendRtpItem
     */
    SendRtpItem querySendRTPServer(String platformGbId, String channelId, String streamId, String callId);

    List<SendRtpItem> querySendRTPServer(String platformGbId);

    /**
     * 删除RTP推送信息缓存
     * @param platformGbId
     * @param channelId
     */
    void deleteSendRTPServer(String platformGbId, String channelId, String callId, String streamId);

    /**
     * 查询某个通道是否存在上级点播（RTP推送）
     * @param channelId
     */
    boolean isChannelSendingRTP(String channelId);

    /**
     * 在redis添加wvp的信息
     */
    void updateWVPInfo(JSONObject jsonObject, int time);

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
    void addStream(MediaServerItem mediaServerItem, String type, String app, String streamId, OnStreamChangedHookParam item);

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

    List<OnStreamChangedHookParam> getStreams(String mediaServerId, String pull);

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

    Long getSN(String method);

    void resetAllSN();

    OnStreamChangedHookParam getStreamInfo(String app, String streamId, String mediaServerId);

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

    List<SendRtpItem> querySendRTPServerByChnnelId(String channelId);

    List<SendRtpItem> querySendRTPServerByStream(String stream);

    SystemAllInfo getSystemInfo();

    int getPushStreamCount(String id);

    int getProxyStreamCount(String id);

    int getGbSendCount(String id);

    void addDiskInfo(List<Map<String, Object>> diskInfo);

    List<SendRtpItem> queryAllSendRTPServer();

    List<Device> getAllDevices();

    void removeAllDevice();

    void sendDeviceOrChannelStatus(String deviceId, String channelId, boolean online);

    void sendChannelAddOrDelete(String deviceId, String channelId, boolean add);

    void sendPlatformStartPlayMsg(MessageForPushChannel messageForPushChannel);

    void sendPlatformStopPlayMsg(MessageForPushChannel messageForPushChannel);
}
