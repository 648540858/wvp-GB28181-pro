package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;

/**
 * 记录国标点播的状态，包括实时预览，下载，录像回放
 */
public interface IInviteStreamService {

    /**
     * 更新点播的状态信息
     */
    void updateInviteInfo(InviteInfo inviteInfo);

    /**
     * 获取点播的状态信息
     */
    InviteInfo getInviteInfo(InviteSessionType type,
                             String deviceId,
                             String channelId,
                             String stream);

    /**
     * 移除点播的状态信息
     */
    void removeInviteInfo(InviteSessionType type,
                             String deviceId,
                             String channelId,
                             String stream);
    /**
     * 移除点播的状态信息
     */
    void removeInviteInfo(InviteInfo inviteInfo);
    /**
     * 移除点播的状态信息
     */
    void removeInviteInfoByDeviceAndChannel(InviteSessionType inviteSessionType, String deviceId, String channelId);

    /**
     * 获取点播的状态信息
     */
    InviteInfo getInviteInfoByDeviceAndChannel(InviteSessionType type,
                             String deviceId,
                             String channelId);

    /**
     * 获取点播的状态信息
     */
    InviteInfo getInviteInfoByStream(InviteSessionType type, String stream);


    /**
     * 添加一个invite回调
     */
    void once(InviteSessionType type, String deviceId, String channelId, String stream,  ErrorCallback<Object> callback);

    /**
     * 调用一个invite回调
     */
    void call(InviteSessionType type, String deviceId, String channelId, String stream,  int code, String msg, Object data);

    /**
     * 清空一个设备的所有invite信息
     */
    void clearInviteInfo(String deviceId);
}
