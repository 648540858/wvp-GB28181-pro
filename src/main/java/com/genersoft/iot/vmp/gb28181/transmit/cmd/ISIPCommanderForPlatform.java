package com.genersoft.iot.vmp.gb28181.transmit.cmd;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;

import javax.sip.header.WWWAuthenticateHeader;
import java.util.List;

public interface ISIPCommanderForPlatform {

    /**
     * 向上级平台注册
     * @param parentPlatform
     * @return
     */
    boolean register(ParentPlatform parentPlatform, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent);
    boolean register(ParentPlatform parentPlatform, String callId, WWWAuthenticateHeader www, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent, boolean registerAgain);

    /**
     * 向上级平台注销
     * @param parentPlatform
     * @return
     */
    boolean unregister(ParentPlatform parentPlatform, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent);


    /**
     * 向上级平发送心跳信息
     * @param parentPlatform
     * @return callId(作为接受回复的判定)
     */
    String keepalive(ParentPlatform parentPlatform);


    /**
     * 向上级回复通道信息
     * @param channel 通道信息
     * @param parentPlatform 平台信息
     * @param sn
     * @param fromTag
     * @param size
     * @return
     */
    boolean catalogQuery(DeviceChannel channel, ParentPlatform parentPlatform, String sn, String fromTag, int size);
    boolean catalogQuery(List<DeviceChannel> channels, ParentPlatform parentPlatform, String sn, String fromTag);

    /**
     * 向上级回复DeviceInfo查询信息
     * @param parentPlatform 平台信息
     * @param sn
     * @param fromTag
     * @return
     */
    boolean deviceInfoResponse(ParentPlatform parentPlatform, String sn, String fromTag);

    /**
     * 向上级回复DeviceStatus查询信息
     * @param parentPlatform 平台信息
     * @param sn
     * @param fromTag
     * @return
     */
    boolean deviceStatusResponse(ParentPlatform parentPlatform, String sn, String fromTag);

    /**
     * 向上级回复移动位置订阅消息
     * @param parentPlatform 平台信息
     * @param gpsMsgInfo GPS信息
     * @param subscribeInfo 订阅相关的信息
     * @return
     */
    boolean sendNotifyMobilePosition(ParentPlatform parentPlatform, GPSMsgInfo gpsMsgInfo, SubscribeInfo subscribeInfo);

    /**
     * 向上级回复报警消息
     * @param parentPlatform 平台信息
     * @param deviceAlarm 报警信息信息
     * @return
     */
    boolean sendAlarmMessage(ParentPlatform parentPlatform, DeviceAlarm deviceAlarm);

    /**
     * 回复catalog事件-增加/更新
     * @param parentPlatform
     * @param deviceChannels
     */
    boolean sendNotifyForCatalogAddOrUpdate(String type, ParentPlatform parentPlatform, List<DeviceChannel> deviceChannels, SubscribeInfo subscribeInfo, Integer index);

    /**
     * 回复catalog事件-删除
     * @param parentPlatform
     * @param deviceChannels
     */
    boolean sendNotifyForCatalogOther(String type, ParentPlatform parentPlatform, List<DeviceChannel> deviceChannels, SubscribeInfo subscribeInfo, Integer index);

    /**
     * 回复recordInfo
     * @param deviceChannel 通道信息
     * @param parentPlatform 平台信息
     * @param fromTag fromTag
     * @param recordInfo 录像信息
     */
    boolean recordInfo(DeviceChannel deviceChannel, ParentPlatform parentPlatform, String fromTag, RecordInfo recordInfo);

    /**
     * 录像播放推送完成时发送MediaStatus消息
     * @param platform
     * @param sendRtpItem
     * @return
     */
    boolean sendMediaStatusNotify(ParentPlatform platform, SendRtpItem sendRtpItem);

    /**
     * 向发起点播的上级回复bye
     * @param platform 平台信息
     * @param callId  callId
     */
    void streamByeCmd(ParentPlatform platform, String callId);
}
