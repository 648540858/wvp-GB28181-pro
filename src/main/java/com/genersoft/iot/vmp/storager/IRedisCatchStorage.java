package com.genersoft.iot.vmp.storager;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.SystemAllInfo;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.media.zlm.dto.*;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.service.bean.MessageForPushChannel;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.service.bean.ThirdPartyGB;
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

    StreamInfo queryPlayByDevice(String deviceId, String channelId);

    Map<String, StreamInfo> queryPlayByDeviceId(String deviceId);

    boolean startPlayback(StreamInfo stream, String callId);

    boolean stopPlayback(String deviceId, String channelId, String stream, String callId);

    StreamInfo queryPlayback(String deviceId, String channelID, String stream, String callId);

    String queryPlaybackForKey(String deviceId, String channelId, String stream, String callId);

    void updatePlatformCatchInfo(ParentPlatformCatch parentPlatformCatch);

    ParentPlatformCatch queryPlatformCatchInfo(String platformGbId);

    void delPlatformCatchInfo(String platformGbId);

    void delPlatformKeepalive(String platformGbId);

    void delPlatformRegister(String platformGbId);

    void updatePlatformRegisterInfo(String callId, PlatformRegisterInfo platformRegisterInfo);

    PlatformRegisterInfo queryPlatformRegisterInfo(String callId);

    void delPlatformRegisterInfo(String callId);

    void cleanPlatformRegisterInfos();

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
     * 清空某个设备的所有缓存
     * @param deviceId 设备ID
     */
    void clearCatchByDeviceId(String deviceId);

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
    void addStream(MediaServerItem mediaServerItem, String type, String app, String streamId, MediaItem item);

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

    /**
     * 开始下载录像时存入
     * @param streamInfo
     */
    boolean startDownload(StreamInfo streamInfo, String callId);

    StreamInfo queryDownload(String deviceId, String channelId, String stream, String callId);

    boolean stopDownload(String deviceId, String channelId, String stream, String callId);

    /**
     * 查找第三方系统留下的国标预设值
     * @param queryKey
     * @return
     */
    ThirdPartyGB queryMemberNoGBId(String queryKey);

    List<MediaItem> getStreams(String mediaServerId, String pull);

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

    MediaItem getStreamInfo(String app, String streamId, String mediaServerId);

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

    /**
     * 发送redis消息 查询所有推流设备的状态
     */
    void sendStreamPushRequestedMsgForStatus();

    List<SendRtpItem> querySendRTPServerByChnnelId(String channelId);

    List<SendRtpItem> querySendRTPServerByStream(String stream);

    SystemAllInfo getSystemInfo();

    int getPushStreamCount(String id);

    int getProxyStreamCount(String id);

    int getGbReceiveCount(String id);

    int getGbSendCount(String id);

    void addDiskInfo(List<Map<String, Object>> diskInfo);

    List<SendRtpItem> queryAllSendRTPServer();

}
