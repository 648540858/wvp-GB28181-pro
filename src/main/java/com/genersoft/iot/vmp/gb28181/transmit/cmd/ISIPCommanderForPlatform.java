package com.genersoft.iot.vmp.gb28181.transmit.cmd;

import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.header.WWWAuthenticateHeader;
import java.text.ParseException;
import java.util.List;

public interface ISIPCommanderForPlatform {

    /**
     * 向上级平台注册
     *
     * @param parentPlatform
     * @return
     */
    void register(Platform parentPlatform, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent) throws InvalidArgumentException, ParseException, SipException;

    void register(Platform parentPlatform, SipTransactionInfo sipTransactionInfo, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent) throws InvalidArgumentException, ParseException, SipException;


    void register(Platform parentPlatform, SipTransactionInfo sipTransactionInfo, WWWAuthenticateHeader www, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent, boolean isRegister) throws SipException, InvalidArgumentException, ParseException;

    /**
     * 向上级平台注销
     *
     * @param parentPlatform
     * @return
     */
    void unregister(Platform parentPlatform, SipTransactionInfo sipTransactionInfo, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent) throws InvalidArgumentException, ParseException, SipException;


    /**
     * 向上级平发送心跳信息
     *
     * @param parentPlatform
     * @return callId(作为接受回复的判定)
     */
    String keepalive(Platform parentPlatform, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent)
            throws SipException, InvalidArgumentException, ParseException;


    /**
     * 向上级回复通道信息
     *
     * @param channel        通道信息
     * @param parentPlatform 平台信息
     * @param sn
     * @param fromTag
     * @param size
     * @return
     */
    void catalogQuery(CommonGBChannel channel, Platform parentPlatform, String sn, String fromTag, int size)
            throws SipException, InvalidArgumentException, ParseException;

    void catalogQuery(List<CommonGBChannel> channels, Platform parentPlatform, String sn, String fromTag)
            throws InvalidArgumentException, ParseException, SipException;

    /**
     * 向上级回复DeviceInfo查询信息
     *
     * @param parentPlatform 平台信息
     * @param sn SN
     * @param fromTag FROM头的tag信息
     * @return
     */
    void deviceInfoResponse(Platform parentPlatform, Device device, String sn, String fromTag) throws SipException, InvalidArgumentException, ParseException;

    /**
     * 向上级回复DeviceStatus查询信息
     *
     * @param parentPlatform 平台信息
     * @param sn
     * @param fromTag
     * @return
     */
    void deviceStatusResponse(Platform parentPlatform, String channelId, String sn, String fromTag, boolean status) throws SipException, InvalidArgumentException, ParseException;

    /**
     * 向上级回复移动位置订阅消息
     *
     * @param parentPlatform 平台信息
     * @param gpsMsgInfo     GPS信息
     * @param subscribeInfo  订阅相关的信息
     * @return
     */
    void sendNotifyMobilePosition(Platform parentPlatform, GPSMsgInfo gpsMsgInfo, CommonGBChannel channel, SubscribeInfo subscribeInfo)
            throws InvalidArgumentException, ParseException, NoSuchFieldException, SipException, IllegalAccessException;

    /**
     * 向上级回复报警消息
     *
     * @param parentPlatform 平台信息
     * @param deviceAlarm    报警信息信息
     * @return
     */
    void sendAlarmMessage(Platform parentPlatform, DeviceAlarm deviceAlarm) throws SipException, InvalidArgumentException, ParseException;

    /**
     * 回复catalog事件-增加/更新
     *
     * @param parentPlatform
     * @param deviceChannels
     */
    void sendNotifyForCatalogAddOrUpdate(String type, Platform parentPlatform, List<CommonGBChannel> deviceChannels, SubscribeInfo subscribeInfo, Integer index) throws InvalidArgumentException, ParseException, NoSuchFieldException, SipException, IllegalAccessException;

    /**
     * 回复catalog事件-删除
     *
     * @param parentPlatform
     * @param deviceChannels
     */
    void sendNotifyForCatalogOther(String type, Platform parentPlatform, List<CommonGBChannel> deviceChannels,
                                   SubscribeInfo subscribeInfo, Integer index) throws InvalidArgumentException,
            ParseException, NoSuchFieldException, SipException, IllegalAccessException;

    /**
     * 回复recordInfo
     *
     * @param deviceChannel  通道信息
     * @param parentPlatform 平台信息
     * @param fromTag        fromTag
     * @param recordInfo     录像信息
     */
    void recordInfo(CommonGBChannel deviceChannel, Platform parentPlatform, String fromTag, RecordInfo recordInfo)
            throws SipException, InvalidArgumentException, ParseException;

    /**
     * 录像播放推送完成时发送MediaStatus消息
     *
     * @param platform
     * @param sendRtpItem
     * @return
     */
    void sendMediaStatusNotify(Platform platform, SendRtpInfo sendRtpItem) throws SipException, InvalidArgumentException, ParseException;

    /**
     * 向发起点播的上级回复bye
     *
     * @param platform 平台信息
     * @param callId   callId
     */
    void streamByeCmd(Platform platform, String callId) throws SipException, InvalidArgumentException, ParseException;

    void streamByeCmd(Platform platform, SendRtpInfo sendRtpItem, CommonGBChannel channel) throws SipException, InvalidArgumentException, ParseException;

    void streamByeCmd(Platform platform, CommonGBChannel channel, String stream, String callId, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException, SsrcTransactionNotFoundException;

    void broadcastInviteCmd(Platform platform, CommonGBChannel channel, MediaServer mediaServerItem,
                            SSRCInfo ssrcInfo, HookSubscribe.Event event, SipSubscribe.Event okEvent,
                            SipSubscribe.Event errorEvent) throws ParseException, SipException, InvalidArgumentException;

    void broadcastResultCmd(Platform platform, CommonGBChannel deviceChannel, String sn, boolean result, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException;
}
