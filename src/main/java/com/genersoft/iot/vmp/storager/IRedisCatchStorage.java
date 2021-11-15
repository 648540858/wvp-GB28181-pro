package com.genersoft.iot.vmp.storager;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatformCatch;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;

import java.util.List;
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

    StreamInfo queryPlayByDevice(String deviceId, String channelId);

    Map<String, StreamInfo> queryPlayByDeviceId(String deviceId);

    boolean startPlayback(StreamInfo stream);

    boolean stopPlayback(StreamInfo streamInfo);

    StreamInfo queryPlaybackByDevice(String deviceId, String code);

    void updatePlatformCatchInfo(ParentPlatformCatch parentPlatformCatch);

    ParentPlatformCatch queryPlatformCatchInfo(String platformGbId);

    void delPlatformCatchInfo(String platformGbId);

    void updatePlatformKeepalive(ParentPlatform parentPlatform);

    void delPlatformKeepalive(String platformGbId);

    void updatePlatformRegister(ParentPlatform parentPlatform);

    void delPlatformRegister(String platformGbId);

    void updatePlatformRegisterInfo(String callId, String platformGbId);

    String queryPlatformRegisterInfo(String callId);

    void delPlatformRegisterInfo(String callId);

    void cleanPlatformRegisterInfos();

    void updateSendRTPSever(SendRtpItem sendRtpItem);

    /**
     * 查询RTP推送信息缓存
     * @param platformGbId
     * @param channelId
     * @return sendRtpItem
     */
    SendRtpItem querySendRTPServer(String platformGbId, String channelId);

    List<SendRtpItem> querySendRTPServer(String platformGbId);

    /**
     * 删除RTP推送信息缓存
     * @param platformGbId
     * @param channelId
     */
    void deleteSendRTPServer(String platformGbId, String channelId);

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
     * 获取mediaServer节点
     * @param mediaServerId
     * @return
     */
//    MediaServerItem getMediaInfo(String mediaServerId);

    /**
     * 设置所有设备离线
     */
    void outlineForAll();

    /**
     * 获取所有在线的
     */
    List<String> getOnlineForAll();

    /**
     * 在redis添加wvp的信息
     */
    void updateWVPInfo(JSONObject jsonObject);
}
