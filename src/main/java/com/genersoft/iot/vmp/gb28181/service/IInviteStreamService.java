package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;

import java.util.List;

/**
 * 记录国标点播的状态，包括实时预览，下载，录像回放
 */
public interface IInviteStreamService {

    /**
     * 更新点播的状态信息
     */
    void updateInviteInfo(InviteInfo inviteInfo);

    void updateInviteInfo(InviteInfo inviteInfo, Long time);

    InviteInfo updateInviteInfoForStream(InviteInfo inviteInfo, String stream);

    /**
     * 获取点播的状态信息
     */
    InviteInfo getInviteInfo(InviteSessionType type, Integer channelId, String stream);

    /**
     * 移除点播的状态信息
     */
    void removeInviteInfo(InviteSessionType type, Integer channelId, String stream);
    /**
     * 移除点播的状态信息
     */
    void removeInviteInfo(InviteInfo inviteInfo);
    /**
     * 移除点播的状态信息
     */
    void removeInviteInfoByDeviceAndChannel(InviteSessionType inviteSessionType, Integer channelId);

    List<InviteInfo> getAllInviteInfo(InviteSessionType type, Integer channelId, String stream);

    /**
     * 获取点播的状态信息
     */
    InviteInfo getInviteInfoByDeviceAndChannel(InviteSessionType type, Integer channelId);

    /**
     * 获取点播的状态信息
     */
    InviteInfo getInviteInfoByStream(InviteSessionType type, String stream);


    /**
     * 添加一个invite回调
     */
    void once(InviteSessionType type, Integer channelId, String stream,  ErrorCallback<StreamInfo> callback);

    /**
     * 调用一个invite回调
     */
    void call(InviteSessionType type,  Integer channelId, String stream,  int code, String msg, StreamInfo data);

    /**
     * 清空一个设备的所有invite信息
     */
    void clearInviteInfo(String deviceId);

    /**
     * 统计同一个zlm下的国标收流个数
     */
    int getStreamInfoCount(String mediaServerId);


    /**
     * 获取MediaServer下的流信息
     */
    InviteInfo getInviteInfoBySSRC(String ssrc);

    /**
     * 更新ssrc
     */
    InviteInfo updateInviteInfoForSSRC(InviteInfo inviteInfo, String ssrcInResponse);
}
