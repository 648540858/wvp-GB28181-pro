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

    List<InviteInfo> getAllInviteInfo();

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
     * 原子注册invite回调并认领点播发起权
     * @return true 表示本次注册是第一个（发起者，负责发起点播），false 表示已有发起者在途，仅需等待结果
     */
    boolean onceAndFirst(InviteSessionType type, Integer channelId, String stream, ErrorCallback<StreamInfo> callback);

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

    /**
     * 判断流注销事件是否属于旧会话（流实例创建时间早于当前invite会话），
     * 避免停止后重新点播时，旧流的迟到注销事件误清理/误BYE新会话
     * @param inviteInfo 当前流对应的invite信息
     * @param streamCreateStamp 流实例创建时间戳(秒)，来自ZLM on_stream_changed，为null时无法判断
     * @return true 表示事件属于旧会话，应跳过清理
     */
    boolean isStaleStreamDeparture(InviteInfo inviteInfo, Long streamCreateStamp);
}
