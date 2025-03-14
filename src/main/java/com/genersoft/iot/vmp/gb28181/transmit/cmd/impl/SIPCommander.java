package com.genersoft.iot.vmp.gb28181.transmit.cmd.impl;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.MessageSubscribe;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.event.sip.MessageEvent;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.SIPSender;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.SIPRequestHeaderProvider;
import com.genersoft.iot.vmp.gb28181.utils.NumericUtil;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.hook.Hook;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.hook.HookType;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Request;
import java.text.ParseException;

/**
 * @description:设备能力接口，用于定义设备的控制、查询能力
 * @author: swwheihei
 * @date: 2020年5月3日 下午9:22:48
 */
@Component
@DependsOn("sipLayer")
@Slf4j
public class SIPCommander implements ISIPCommander {

    @Autowired
    private SipConfig sipConfig;

    @Autowired
    private SipLayer sipLayer;

    @Autowired
    private SIPSender sipSender;

    @Autowired
    private SIPRequestHeaderProvider headerProvider;

    @Autowired
    private SipInviteSessionManager sessionManager;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private HookSubscribe subscribe;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private MessageSubscribe messageSubscribe;

    /**
     * 云台指令码计算
     *
     * @param cmdCode      指令码
     * @param parameter1   数据1
     * @param parameter2   数据2
     * @param combineCode2 组合码2
     */
    public static String frontEndCmdString(int cmdCode, int parameter1, int parameter2, int combineCode2) {
        StringBuilder builder = new StringBuilder("A50F01");
        String strTmp;
        strTmp = String.format("%02X", cmdCode);
        builder.append(strTmp, 0, 2);
        strTmp = String.format("%02X", parameter1);
        builder.append(strTmp, 0, 2);
        strTmp = String.format("%02X", parameter2);
        builder.append(strTmp, 0, 2);
        strTmp = String.format("%02X", combineCode2 << 4);
        builder.append(strTmp, 0, 2);
        //计算校验码
        int checkCode = (0XA5 + 0X0F + 0X01 + cmdCode + parameter1 + parameter2 + (combineCode2 << 4)) % 0X100;
        strTmp = String.format("%02X", checkCode);
        builder.append(strTmp, 0, 2);
        return builder.toString();
    }

    /**
     * 云台控制，支持方向与缩放控制
     *
     * @param device    控制设备
     * @param channelId 预览通道
     * @param leftRight 镜头左移右移 0:停止 1:左移 2:右移
     * @param upDown    镜头上移下移 0:停止 1:上移 2:下移
     * @param inOut     镜头放大缩小 0:停止 1:缩小 2:放大
     * @param moveSpeed 镜头移动速度
     * @param zoomSpeed 镜头缩放速度
     */
    @Override
    public void ptzCmd(Device device, String channelId, int leftRight, int upDown, int inOut, int moveSpeed,
                       int zoomSpeed) throws InvalidArgumentException, SipException, ParseException {
        String cmdStr = SipUtils.cmdString(leftRight, upDown, inOut, moveSpeed, zoomSpeed);
        StringBuilder ptzXml = new StringBuilder(200);
        String charset = device.getCharset();
        ptzXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        ptzXml.append("<Control>\r\n");
        ptzXml.append("<CmdType>DeviceControl</CmdType>\r\n");
        ptzXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        ptzXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        ptzXml.append("<PTZCmd>" + cmdStr + "</PTZCmd>\r\n");
        ptzXml.append("<Info>\r\n");
        ptzXml.append("<ControlPriority>5</ControlPriority>\r\n");
        ptzXml.append("</Info>\r\n");
        ptzXml.append("</Control>\r\n");

        Request request = headerProvider.createMessageRequest(device, ptzXml.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null, sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()),request);
    }

    /**
     * 前端控制，包括PTZ指令、FI指令、预置位指令、巡航指令、扫描指令和辅助开关指令
     *
     * @param device       控制设备
     * @param channelId    预览通道
     * @param cmdCode      指令码
     * @param parameter1   数据1
     * @param parameter2   数据2
     * @param combineCode2 组合码2
     */
    @Override
    public void frontEndCmd(Device device, String channelId, int cmdCode, int parameter1, int parameter2, int combineCode2) throws SipException, InvalidArgumentException, ParseException {

        String cmdStr = frontEndCmdString(cmdCode, parameter1, parameter2, combineCode2);
        StringBuffer ptzXml = new StringBuffer(200);
        String charset = device.getCharset();
        ptzXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        ptzXml.append("<Control>\r\n");
        ptzXml.append("<CmdType>DeviceControl</CmdType>\r\n");
        ptzXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        ptzXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        ptzXml.append("<PTZCmd>" + cmdStr + "</PTZCmd>\r\n");
        ptzXml.append("<Info>\r\n");
        ptzXml.append("<ControlPriority>5</ControlPriority>\r\n");
        ptzXml.append("</Info>\r\n");
        ptzXml.append("</Control>\r\n");

        SIPRequest request = (SIPRequest) headerProvider.createMessageRequest(device, ptzXml.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()),request);

    }

    /**
     * 前端控制指令（用于转发上级指令）
     *
     * @param device    控制设备
     * @param channelId 预览通道
     * @param cmdString 前端控制指令串
     */
    @Override
    public void fronEndCmd(Device device, String channelId, String cmdString, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer ptzXml = new StringBuffer(200);
        String charset = device.getCharset();
        ptzXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        ptzXml.append("<Control>\r\n");
        ptzXml.append("<CmdType>DeviceControl</CmdType>\r\n");
        ptzXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        ptzXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        ptzXml.append("<PTZCmd>" + cmdString + "</PTZCmd>\r\n");
        ptzXml.append("<Info>\r\n");
        ptzXml.append("<ControlPriority>5</ControlPriority>\r\n");
        ptzXml.append("</Info>\r\n");
        ptzXml.append("</Control>\r\n");


        Request request = headerProvider.createMessageRequest(device, ptzXml.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()),request, errorEvent, okEvent);

    }

    /**
     * 请求预览视频流
     *
     * @param device     视频设备
     * @param channel  预览通道
     * @param errorEvent sip错误订阅
     */
    @Override
    public void playStreamCmd(MediaServer mediaServerItem, SSRCInfo ssrcInfo, Device device, DeviceChannel channel,
                              SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent, Long timeout) throws InvalidArgumentException, SipException, ParseException {
        String stream = ssrcInfo.getStream();

        if (device == null) {
            return;
        }
        String sdpIp;
        if (!ObjectUtils.isEmpty(device.getSdpIp())) {
            sdpIp = device.getSdpIp();
        }else {
            sdpIp = mediaServerItem.getSdpIp();
        }
        StringBuffer content = new StringBuffer(200);
        content.append("v=0\r\n");
        content.append("o=" + device.getDeviceId() + " 0 0 IN IP4 " + sdpIp + "\r\n");
        content.append("s=Play\r\n");
        content.append("c=IN IP4 " + sdpIp + "\r\n");
        content.append("t=0 0\r\n");

        if (userSetting.getSeniorSdp()) {
            if ("TCP-PASSIVE".equalsIgnoreCase(device.getStreamMode())) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
            } else if ("TCP-ACTIVE".equalsIgnoreCase(device.getStreamMode())) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
            } else if ("UDP".equalsIgnoreCase(device.getStreamMode())) {
                content.append("m=video " + ssrcInfo.getPort() + " RTP/AVP 96 126 125 99 34 98 97\r\n");
            }
            content.append("a=recvonly\r\n");
            content.append("a=rtpmap:96 PS/90000\r\n");
            content.append("a=fmtp:126 profile-level-id=42e01e\r\n");
            content.append("a=rtpmap:126 H264/90000\r\n");
            content.append("a=rtpmap:125 H264S/90000\r\n");
            content.append("a=fmtp:125 profile-level-id=42e01e\r\n");
            content.append("a=rtpmap:99 H265/90000\r\n");
            content.append("a=rtpmap:98 H264/90000\r\n");
            content.append("a=rtpmap:97 MPEG4/90000\r\n");
            if ("TCP-PASSIVE".equalsIgnoreCase(device.getStreamMode())) { // tcp被动模式
                content.append("a=setup:passive\r\n");
                content.append("a=connection:new\r\n");
            } else if ("TCP-ACTIVE".equalsIgnoreCase(device.getStreamMode())) { // tcp主动模式
                content.append("a=setup:active\r\n");
                content.append("a=connection:new\r\n");
            }
        } else {
            if ("TCP-PASSIVE".equalsIgnoreCase(device.getStreamMode())) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 97 98 99\r\n");
            } else if ("TCP-ACTIVE".equalsIgnoreCase(device.getStreamMode())) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 97 98 99\r\n");
            } else if ("UDP".equalsIgnoreCase(device.getStreamMode())) {
                content.append("m=video " + ssrcInfo.getPort() + " RTP/AVP 96 97 98 99\r\n");
            }
            content.append("a=recvonly\r\n");
            content.append("a=rtpmap:96 PS/90000\r\n");
            content.append("a=rtpmap:98 H264/90000\r\n");
            content.append("a=rtpmap:97 MPEG4/90000\r\n");
            content.append("a=rtpmap:99 H265/90000\r\n");
            if ("TCP-PASSIVE".equalsIgnoreCase(device.getStreamMode())) { // tcp被动模式
                content.append("a=setup:passive\r\n");
                content.append("a=connection:new\r\n");
            } else if ("TCP-ACTIVE".equalsIgnoreCase(device.getStreamMode())) { // tcp主动模式
                content.append("a=setup:active\r\n");
                content.append("a=connection:new\r\n");
            }
        }

        if (!ObjectUtils.isEmpty(channel.getStreamIdentification())) {
            content.append("a=" + channel.getStreamIdentification() + "\r\n");
        }

        content.append("y=" + ssrcInfo.getSsrc() + "\r\n");//ssrc
        // f字段:f= v/编码格式/分辨率/帧率/码率类型/码率大小a/编码格式/码率大小/采样率
//			content.append("f=v/2/5/25/1/4000a/1/8/1" + "\r\n"); // 未发现支持此特性的设备

        Request request = headerProvider.createInviteRequest(device, channel.getDeviceId(), content.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null, ssrcInfo.getSsrc(),sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, (e -> {
            sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());
            mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
            errorEvent.response(e);
        }), e -> {
            ResponseEvent responseEvent = (ResponseEvent) e.event;
            SIPResponse response = (SIPResponse) responseEvent.getResponse();
            String callId = response.getCallIdHeader().getCallId();
            SsrcTransaction ssrcTransaction = SsrcTransaction.buildForDevice(device.getDeviceId(), channel.getId(),
                    callId,ssrcInfo.getApp(), ssrcInfo.getStream(), ssrcInfo.getSsrc(), mediaServerItem.getId(), response,
                    InviteSessionType.PLAY);
            sessionManager.put(ssrcTransaction);
            okEvent.response(e);
        }, timeout);
    }

    /**
     * 请求回放视频流
     *
     * @param device    视频设备
     * @param channel 预览通道
     * @param startTime 开始时间,格式要求：yyyy-MM-dd HH:mm:ss
     * @param endTime   结束时间,格式要求：yyyy-MM-dd HH:mm:ss
     */
    @Override
    public void playbackStreamCmd(MediaServer mediaServerItem, SSRCInfo ssrcInfo, Device device, DeviceChannel channel,
                                  String startTime, String endTime,
                                  SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent, Long timeout) throws InvalidArgumentException, SipException, ParseException {


        log.info("{} 分配的ZLM为: {} [{}:{}]", ssrcInfo.getStream(), mediaServerItem.getId(), mediaServerItem.getSdpIp(), ssrcInfo.getPort());
        String sdpIp;
        if (!ObjectUtils.isEmpty(device.getSdpIp())) {
            sdpIp = device.getSdpIp();
        }else {
            sdpIp = mediaServerItem.getSdpIp();
        }
        StringBuffer content = new StringBuffer(200);
        content.append("v=0\r\n");
        content.append("o=" + device.getDeviceId() + " 0 0 IN IP4 " + sdpIp + "\r\n");
        content.append("s=Playback\r\n");
        content.append("u=" + channel.getDeviceId() + ":0\r\n");
        content.append("c=IN IP4 " + sdpIp + "\r\n");
        content.append("t=" + DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime) + " "
                + DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime) + "\r\n");

        String streamMode = device.getStreamMode();

        if (userSetting.getSeniorSdp()) {
            if ("TCP-PASSIVE".equalsIgnoreCase(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
            } else if ("TCP-ACTIVE".equalsIgnoreCase(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
            } else if ("UDP".equalsIgnoreCase(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " RTP/AVP 96 126 125 99 34 98 97\r\n");
            }
            content.append("a=recvonly\r\n");
            content.append("a=rtpmap:96 PS/90000\r\n");
            content.append("a=fmtp:126 profile-level-id=42e01e\r\n");
            content.append("a=rtpmap:126 H264/90000\r\n");
            content.append("a=rtpmap:125 H264S/90000\r\n");
            content.append("a=fmtp:125 profile-level-id=42e01e\r\n");
            content.append("a=rtpmap:99 H265/90000\r\n");
            content.append("a=rtpmap:98 H264/90000\r\n");
            content.append("a=rtpmap:97 MPEG4/90000\r\n");
            if ("TCP-PASSIVE".equalsIgnoreCase(streamMode)) { // tcp被动模式
                content.append("a=setup:passive\r\n");
                content.append("a=connection:new\r\n");
            } else if ("TCP-ACTIVE".equalsIgnoreCase(streamMode)) { // tcp主动模式
                content.append("a=setup:active\r\n");
                content.append("a=connection:new\r\n");
            }
        } else {
            if ("TCP-PASSIVE".equalsIgnoreCase(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 97 98 99\r\n");
            } else if ("TCP-ACTIVE".equalsIgnoreCase(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 97 98 99\r\n");
            } else if ("UDP".equalsIgnoreCase(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " RTP/AVP 96 97 98 99\r\n");
            }
            content.append("a=recvonly\r\n");
            content.append("a=rtpmap:96 PS/90000\r\n");
            content.append("a=rtpmap:97 MPEG4/90000\r\n");
            content.append("a=rtpmap:98 H264/90000\r\n");
            content.append("a=rtpmap:99 H265/90000\r\n");
            if ("TCP-PASSIVE".equalsIgnoreCase(streamMode)) {
                // tcp被动模式
                content.append("a=setup:passive\r\n");
                content.append("a=connection:new\r\n");
            } else if ("TCP-ACTIVE".equalsIgnoreCase(streamMode)) {
                // tcp主动模式
                content.append("a=setup:active\r\n");
                content.append("a=connection:new\r\n");
            }
        }

        //ssrc
        content.append("y=" + ssrcInfo.getSsrc() + "\r\n");

        Request request = headerProvider.createPlaybackInviteRequest(device, channel.getDeviceId(), content.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()), ssrcInfo.getSsrc());

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent, event -> {
            ResponseEvent responseEvent = (ResponseEvent) event.event;
            SIPResponse response = (SIPResponse) responseEvent.getResponse();
            SsrcTransaction ssrcTransaction = SsrcTransaction.buildForDevice(device.getDeviceId(),
                    channel.getId(), sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),
                            device.getTransport()).getCallId(), ssrcInfo.getApp(), ssrcInfo.getStream(), ssrcInfo.getSsrc(),
                    mediaServerItem.getId(), response, InviteSessionType.PLAYBACK);
            sessionManager.put(ssrcTransaction);
            okEvent.response(event);
        }, timeout);
    }

    /**
     * 请求历史媒体下载
     */
    @Override
    public void downloadStreamCmd(MediaServer mediaServerItem, SSRCInfo ssrcInfo, Device device, DeviceChannel channel,
                                  String startTime, String endTime, int downloadSpeed,
                                  SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent, Long timeout) throws InvalidArgumentException, SipException, ParseException {

        log.info("[发送-请求历史媒体下载-命令] 流ID： {}，节点为: {} [{}:{}]", ssrcInfo.getStream(), mediaServerItem.getId(), mediaServerItem.getSdpIp(), ssrcInfo.getPort());
        String sdpIp;
        if (!ObjectUtils.isEmpty(device.getSdpIp())) {
            sdpIp = device.getSdpIp();
        }else {
            sdpIp = mediaServerItem.getSdpIp();
        }
        StringBuffer content = new StringBuffer(200);
        content.append("v=0\r\n");
        content.append("o=" + device.getDeviceId() + " 0 0 IN IP4 " + sdpIp + "\r\n");
        content.append("s=Download\r\n");
        content.append("u=" + channel.getDeviceId() + ":0\r\n");
        content.append("c=IN IP4 " + sdpIp + "\r\n");
        content.append("t=" + DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime) + " "
                + DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime) + "\r\n");

        String streamMode = device.getStreamMode().toUpperCase();

        if (userSetting.getSeniorSdp()) {
            if ("TCP-PASSIVE".equals(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
            } else if ("TCP-ACTIVE".equals(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
            } else if ("UDP".equals(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " RTP/AVP 96 126 125 99 34 98 97\r\n");
            }
            content.append("a=recvonly\r\n");
            content.append("a=rtpmap:96 PS/90000\r\n");
            content.append("a=fmtp:126 profile-level-id=42e01e\r\n");
            content.append("a=rtpmap:126 H264/90000\r\n");
            content.append("a=rtpmap:125 H264S/90000\r\n");
            content.append("a=fmtp:125 profile-level-id=42e01e\r\n");
            content.append("a=rtpmap:99 MP4V-ES/90000\r\n");
            content.append("a=fmtp:99 profile-level-id=3\r\n");
            content.append("a=rtpmap:98 H264/90000\r\n");
            content.append("a=rtpmap:97 MPEG4/90000\r\n");
            if ("TCP-PASSIVE".equals(streamMode)) { // tcp被动模式
                content.append("a=setup:passive\r\n");
                content.append("a=connection:new\r\n");
            } else if ("TCP-ACTIVE".equals(streamMode)) { // tcp主动模式
                content.append("a=setup:active\r\n");
                content.append("a=connection:new\r\n");
            }
        } else {
            if ("TCP-PASSIVE".equals(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 97 98 99\r\n");
            } else if ("TCP-ACTIVE".equals(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 97 98 99\r\n");
            } else if ("UDP".equals(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " RTP/AVP 96 97 98 99\r\n");
            }
            content.append("a=recvonly\r\n");
            content.append("a=rtpmap:96 PS/90000\r\n");
            content.append("a=rtpmap:97 MPEG4/90000\r\n");
            content.append("a=rtpmap:98 H264/90000\r\n");
            content.append("a=rtpmap:99 H265/90000\r\n");
            if ("TCP-PASSIVE".equals(streamMode)) { // tcp被动模式
                content.append("a=setup:passive\r\n");
                content.append("a=connection:new\r\n");
            } else if ("TCP-ACTIVE".equals(streamMode)) { // tcp主动模式
                content.append("a=setup:active\r\n");
                content.append("a=connection:new\r\n");
            }
        }
        content.append("a=downloadspeed:" + downloadSpeed + "\r\n");

        content.append("y=" + ssrcInfo.getSsrc() + "\r\n");//ssrc
        log.debug("此时请求下载信令的ssrc===>{}",ssrcInfo.getSsrc());
        // 添加订阅
        CallIdHeader newCallIdHeader = sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()), device.getTransport());
        Request request = headerProvider.createPlaybackInviteRequest(device, channel.getDeviceId(), content.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,newCallIdHeader, ssrcInfo.getSsrc());

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent, event -> {
            ResponseEvent responseEvent = (ResponseEvent) event.event;
            SIPResponse response = (SIPResponse) responseEvent.getResponse();
            String contentString =new String(response.getRawContent());
            String ssrc = SipUtils.getSsrcFromSdp(contentString);
            SsrcTransaction ssrcTransaction = SsrcTransaction.buildForDevice(device.getDeviceId(), channel.getId(),
                    response.getCallIdHeader().getCallId(), ssrcInfo.getApp(), ssrcInfo.getStream(), ssrc,
                    mediaServerItem.getId(), response, InviteSessionType.DOWNLOAD);
            sessionManager.put(ssrcTransaction);
            okEvent.response(event);
        }, timeout);
    }

    @Override
    public void talkStreamCmd(MediaServer mediaServerItem, SendRtpInfo sendRtpItem, Device device, DeviceChannel channel,
                              String callId, HookSubscribe.Event event, HookSubscribe.Event eventForPush, SipSubscribe.Event okEvent,
                              SipSubscribe.Event errorEvent, Long timeout) throws InvalidArgumentException, SipException, ParseException {

        String stream = sendRtpItem.getStream();

        if (device == null) {
            return;
        }
        if (!mediaServerItem.isRtpEnable()) {
            // 单端口暂不支持语音喊话
            log.info("[语音喊话] 单端口暂不支持此操作");
            return;
        }

        log.info("[语音喊话] {} 分配的ZLM为: {} [{}:{}]", stream, mediaServerItem.getId(), mediaServerItem.getIp(), sendRtpItem.getPort());
        Hook hook = Hook.getInstance(HookType.on_media_arrival, "rtp", stream, mediaServerItem.getId());
        subscribe.addSubscribe(hook, (hookData) -> {
            if (event != null) {
                event.response(hookData);
                subscribe.removeSubscribe(hook);
            }
        });

        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()), device.getTransport());
        callIdHeader.setCallId(callId);
        Hook publishHook = Hook.getInstance(HookType.on_publish, "rtp", stream, mediaServerItem.getId());
        subscribe.addSubscribe(publishHook, (hookData) -> {
            if (eventForPush != null) {
                eventForPush.response(hookData);
            }
        });
        //
        StringBuffer content = new StringBuffer(200);
        content.append("v=0\r\n");
        content.append("o=" + device.getDeviceId() + " 0 0 IN IP4 " + mediaServerItem.getSdpIp() + "\r\n");
        content.append("s=Talk\r\n");
        content.append("c=IN IP4 " + mediaServerItem.getSdpIp() + "\r\n");
        content.append("t=0 0\r\n");

        content.append("m=audio " + sendRtpItem.getPort() + " TCP/RTP/AVP 8\r\n");
        content.append("a=setup:passive\r\n");
        content.append("a=connection:new\r\n");
        content.append("a=sendrecv\r\n");
        content.append("a=rtpmap:8 PCMA/8000\r\n");

        content.append("y=" + sendRtpItem.getSsrc() + "\r\n");//ssrc
        // f字段:f= v/编码格式/分辨率/帧率/码率类型/码率大小a/编码格式/码率大小/采样率
        content.append("f=v/////a/1/8/1" + "\r\n");

        Request request = headerProvider.createInviteRequest(device, channel.getDeviceId(), content.toString(),
                SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null, sendRtpItem.getSsrc(), callIdHeader);
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, (e -> {
            sessionManager.removeByStream(sendRtpItem.getApp(), sendRtpItem.getStream());
            mediaServerService.releaseSsrc(mediaServerItem.getId(), sendRtpItem.getSsrc());
            errorEvent.response(e);
        }), e -> {
            // 这里为例避免一个通道的点播只有一个callID这个参数使用一个固定值
            ResponseEvent responseEvent = (ResponseEvent) e.event;
            SIPResponse response = (SIPResponse) responseEvent.getResponse();
            SsrcTransaction ssrcTransaction = SsrcTransaction.buildForDevice(device.getDeviceId(), channel.getId(), "talk",sendRtpItem.getApp(), stream, sendRtpItem.getSsrc(), mediaServerItem.getId(), response, InviteSessionType.TALK);
            sessionManager.put(ssrcTransaction);
            okEvent.response(e);
        }, timeout);
    }

    /**
     * 视频流停止
     */
    @Override
    public void streamByeCmd(Device device, String channelId, String app, String stream, String callId, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException, SsrcTransactionNotFoundException {
        if (device == null) {
            log.warn("[发送BYE] device为null");
            return;
        }
        SsrcTransaction ssrcTransaction = null;
        if (callId != null) {
            ssrcTransaction = sessionManager.getSsrcTransactionByCallId(callId);
        }else if (stream != null) {
            ssrcTransaction = sessionManager.getSsrcTransactionByStream(app, stream);
        }

        if (ssrcTransaction == null) {
            log.info("[发送BYE] 未找到事务信息,设备： device: {}, channel: {}", device.getDeviceId(), channelId);
            throw new SsrcTransactionNotFoundException(device.getDeviceId(), channelId, callId, stream);
        }

        log.info("[发送BYE] 设备： device: {}, channel: {}, callId: {}", device.getDeviceId(), channelId, ssrcTransaction.getCallId());
        sessionManager.removeByCallId(ssrcTransaction.getCallId());
        Request byteRequest = headerProvider.createByteRequest(device, channelId, ssrcTransaction.getSipTransactionInfo());
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), byteRequest, null, okEvent);
    }

    @Override
    public void streamByeCmd(Device device, String channelId, SipTransactionInfo sipTransactionInfo, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException, SsrcTransactionNotFoundException {
        Request byteRequest = headerProvider.createByteRequest(device, channelId, sipTransactionInfo);
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), byteRequest, null, okEvent);
    }

    @Override
    public void streamByeCmdForDeviceInvite(Device device, String channelId, SipTransactionInfo sipTransactionInfo, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException, SsrcTransactionNotFoundException {
        Request byteRequest = headerProvider.createByteRequestForDeviceInvite(device, channelId, sipTransactionInfo);
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), byteRequest, null, okEvent);
    }

    /**
     * 语音广播
     *
     * @param device 视频设备
     */
	@Override
	public void audioBroadcastCmd(Device device, String channelId, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException {
        StringBuffer broadcastXml = new StringBuffer(200);
        String charset = device.getCharset();
        broadcastXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        broadcastXml.append("<Notify>\r\n");
        broadcastXml.append("<CmdType>Broadcast</CmdType>\r\n");
        broadcastXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
        broadcastXml.append("<SourceID>" + sipConfig.getId() + "</SourceID>\r\n");
        broadcastXml.append("<TargetID>" + channelId + "</TargetID>\r\n");
        broadcastXml.append("</Notify>\r\n");

        Request request = headerProvider.createMessageRequest(device, broadcastXml.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent, okEvent);

    }


    /**
     * 音视频录像控制
     *
     * @param device       视频设备
     * @param channelId    预览通道
     * @param recordCmdStr 录像命令：Record / StopRecord
     */
    @Override
    public void recordCmd(Device device, String channelId, String recordCmdStr, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException {
        final String cmdType = "DeviceControl";
        final int sn = (int) ((Math.random() * 9 + 1) * 100000);

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Control>\r\n");
        cmdXml.append("<CmdType>" + cmdType + "</CmdType>\r\n");
        cmdXml.append("<SN>" + sn + "</SN>\r\n");
        if (ObjectUtils.isEmpty(channelId)) {
            cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        } else {
            cmdXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        }
        cmdXml.append("<RecordCmd>" + recordCmdStr + "</RecordCmd>\r\n");
        cmdXml.append("</Control>\r\n");

        MessageEvent<String> messageEvent = MessageEvent.getInstance(cmdType, sn + "", channelId, 1000L, callback);
        messageSubscribe.addSubscribe(messageEvent);

        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, eventResult -> {
            messageSubscribe.removeSubscribe(messageEvent.getKey());
            callback.run(ErrorCode.ERROR100.getCode(), "失败，" + eventResult.msg, null);
        },null);
    }

    /**
     * 远程启动控制命令
     *
     * @param device 视频设备
     */
    @Override
    public void teleBootCmd(Device device) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Control>\r\n");
        cmdXml.append("<CmdType>DeviceControl</CmdType>\r\n");
        cmdXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        cmdXml.append("<TeleBoot>Boot</TeleBoot>\r\n");
        cmdXml.append("</Control>\r\n");



        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request);
    }

    /**
     * 报警布防/撤防命令
     *
     * @param device      视频设备
     * @param guardCmdStr "SetGuard"/"ResetGuard"
     */
    @Override
    public void guardCmd(Device device, String guardCmdStr, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException {

        String cmdType = "DeviceControl";
        int sn = (int) ((Math.random() * 9 + 1) * 100000);

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Control>\r\n");
        cmdXml.append("<CmdType>" + cmdType + "</CmdType>\r\n");
        cmdXml.append("<SN>" + sn + "</SN>\r\n");
        cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        cmdXml.append("<GuardCmd>" + guardCmdStr + "</GuardCmd>\r\n");
        cmdXml.append("</Control>\r\n");

        MessageEvent<String> messageEvent = MessageEvent.getInstance(cmdType, sn + "", device.getDeviceId(), 1000L, callback);
        messageSubscribe.addSubscribe(messageEvent);

        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, eventResult -> {
            messageSubscribe.removeSubscribe(messageEvent.getKey());
            callback.run(ErrorCode.ERROR100.getCode(), "失败，" + eventResult.msg, null);
        });
    }

    /**
     * 报警复位命令
     *
     * @param device 视频设备
     */
    @Override
    public void alarmResetCmd(Device device, String alarmMethod, String alarmType, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException {

        String cmdType = "DeviceControl";
        int sn = (int) ((Math.random() * 9 + 1) * 100000);

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Control>\r\n");
        cmdXml.append("<CmdType>" + cmdType + "</CmdType>\r\n");
        cmdXml.append("<SN>" + sn + "</SN>\r\n");
        cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        cmdXml.append("<AlarmCmd>ResetAlarm</AlarmCmd>\r\n");
        if (!ObjectUtils.isEmpty(alarmMethod) || !ObjectUtils.isEmpty(alarmType)) {
            cmdXml.append("<Info>\r\n");
        }
        if (!ObjectUtils.isEmpty(alarmMethod)) {
            cmdXml.append("<AlarmMethod>" + alarmMethod + "</AlarmMethod>\r\n");
        }
        if (!ObjectUtils.isEmpty(alarmType)) {
            cmdXml.append("<AlarmType>" + alarmType + "</AlarmType>\r\n");
        }
        if (!ObjectUtils.isEmpty(alarmMethod) || !ObjectUtils.isEmpty(alarmType)) {
            cmdXml.append("</Info>\r\n");
        }
        cmdXml.append("</Control>\r\n");

        MessageEvent<String> messageEvent = MessageEvent.getInstance(cmdType, sn + "", device.getDeviceId(), 1000L, callback);
        messageSubscribe.addSubscribe(messageEvent);

        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, eventResult -> {
            messageSubscribe.removeSubscribe(messageEvent.getKey());
            callback.run(ErrorCode.ERROR100.getCode(), "失败，" + eventResult.msg, null);
        });
    }

    /**
     * 强制关键帧命令,设备收到此命令应立刻发送一个IDR帧
     *
     * @param device    视频设备
     * @param channelId 预览通道
     */
    @Override
    public void iFrameCmd(Device device, String channelId) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Control>\r\n");
        cmdXml.append("<CmdType>DeviceControl</CmdType>\r\n");
        cmdXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        if (ObjectUtils.isEmpty(channelId)) {
            cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        } else {
            cmdXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        }
        cmdXml.append("<IFameCmd>Send</IFameCmd>\r\n");
        cmdXml.append("</Control>\r\n");



        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request);
    }

    /**
     * 看守位控制命令
     *
     * @param device      视频设备
     * @param channelId      通道id，非通道则是设备本身
     * @param enabled     看守位使能：1 = 开启，0 = 关闭
     * @param resetTime   自动归位时间间隔，开启看守位时使用，单位:秒(s)
     * @param presetIndex 调用预置位编号，开启看守位时使用，取值范围0~255
     */
    @Override
    public void homePositionCmd(Device device, String channelId, Boolean enabled, Integer resetTime, Integer presetIndex, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException {

        String cmdType = "DeviceControl";
        int sn = (int) ((Math.random() * 9 + 1) * 100000);

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Control>\r\n");
        cmdXml.append("<CmdType>" + cmdType + "</CmdType>\r\n");
        cmdXml.append("<SN>" + sn + "</SN>\r\n");
        if (ObjectUtils.isEmpty(channelId)) {
            channelId = device.getDeviceId();
        }
        cmdXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        cmdXml.append("<HomePosition>\r\n");
        if (enabled) {
            cmdXml.append("<Enabled>1</Enabled>\r\n");
            cmdXml.append("<ResetTime>" + resetTime + "</ResetTime>\r\n");
            cmdXml.append("<PresetIndex>" + presetIndex + "</PresetIndex>\r\n");
        } else {
            cmdXml.append("<Enabled>0</Enabled>\r\n");
        }
        cmdXml.append("</HomePosition>\r\n");
        cmdXml.append("</Control>\r\n");

        MessageEvent<String> messageEvent = MessageEvent.getInstance(cmdType, sn + "", channelId, 1000L, callback);
        messageSubscribe.addSubscribe(messageEvent);

        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, eventResult -> {
            messageSubscribe.removeSubscribe(messageEvent.getKey());
            callback.run(ErrorCode.ERROR100.getCode(), "失败，" + eventResult.msg, null);
        });
    }

    /**
     * 设备配置命令
     *
     * @param device 视频设备
     */
    @Override
    public void deviceConfigCmd(Device device) {
        // TODO Auto-generated method stub
    }

    /**
     * 设备配置命令：basicParam
     */
    @Override
    public void deviceBasicConfigCmd(Device device, BasicParam basicParam, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException {

        int sn = (int) ((Math.random() * 9 + 1) * 100000);
        String cmdType = "DeviceConfig";
        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Control>\r\n");
        cmdXml.append("<CmdType>" + cmdType + "</CmdType>\r\n");
        cmdXml.append("<SN>" + sn + "</SN>\r\n");
        String channelId = basicParam.getChannelId();
        if (ObjectUtils.isEmpty(channelId)) {
            channelId = device.getDeviceId();
        }
        cmdXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        cmdXml.append("<BasicParam>\r\n");
        if (!ObjectUtils.isEmpty(basicParam.getName())) {
            cmdXml.append("<Name>" + basicParam.getName() + "</Name>\r\n");
        }
        if (NumericUtil.isInteger(basicParam.getExpiration())) {
            if (Integer.parseInt(basicParam.getExpiration()) > 0) {
                cmdXml.append("<Expiration>" + basicParam.getExpiration() + "</Expiration>\r\n");
            }
        }
        if (basicParam.getHeartBeatInterval() != null && basicParam.getHeartBeatInterval() > 0) {
            cmdXml.append("<HeartBeatInterval>" + basicParam.getHeartBeatInterval() + "</HeartBeatInterval>\r\n");
        }
        if (basicParam.getHeartBeatCount() != null && basicParam.getHeartBeatCount() > 0) {
            cmdXml.append("<HeartBeatCount>" + basicParam.getHeartBeatCount() + "</HeartBeatCount>\r\n");
        }
        cmdXml.append("</BasicParam>\r\n");
        cmdXml.append("</Control>\r\n");


        MessageEvent<String> messageEvent = MessageEvent.getInstance(cmdType, sn + "", channelId, 1000L, callback);
        messageSubscribe.addSubscribe(messageEvent);

        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, eventResult -> {
            messageSubscribe.removeSubscribe(messageEvent.getKey());
            callback.run(ErrorCode.ERROR100.getCode(), "失败，" + eventResult.msg, null);
        });
    }

    /**
     * 查询设备状态
     *
     * @param device 视频设备
     */
    @Override
    public void deviceStatusQuery(Device device, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException {

        String cmdType = "DeviceStatus";
        int sn = (int) ((Math.random() * 9 + 1) * 100000);

        String charset = device.getCharset();
        StringBuffer catalogXml = new StringBuffer(200);
        catalogXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        catalogXml.append("<Query>\r\n");
        catalogXml.append("<CmdType>" + cmdType + "</CmdType>\r\n");
        catalogXml.append("<SN>" + sn + "</SN>\r\n");
        catalogXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        catalogXml.append("</Query>\r\n");

        MessageEvent<String> messageEvent = MessageEvent.getInstance(cmdType, sn + "", device.getDeviceId(), 1000L, callback);
        messageSubscribe.addSubscribe(messageEvent);

        Request request = headerProvider.createMessageRequest(device, catalogXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, eventResult -> {
            messageSubscribe.removeSubscribe(messageEvent.getKey());
            callback.run(ErrorCode.ERROR100.getCode(), "失败，" + eventResult.msg, null);
        });
    }

    /**
     * 查询设备信息
     *
     * @param device   视频设备
     * @param callback
     */
    @Override
    public void deviceInfoQuery(Device device, ErrorCallback<Object> callback) throws InvalidArgumentException, SipException, ParseException {

        String cmdType = "DeviceInfo";
        String sn = (int) ((Math.random() * 9 + 1) * 100000) + "";

        StringBuffer catalogXml = new StringBuffer(200);
        String charset = device.getCharset();
        catalogXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        catalogXml.append("<Query>\r\n");
        catalogXml.append("<CmdType>" + cmdType +"</CmdType>\r\n");
        catalogXml.append("<SN>" + sn + "</SN>\r\n");
        catalogXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        catalogXml.append("</Query>\r\n");

        MessageEvent<Object> messageEvent = MessageEvent.getInstance(cmdType, sn, device.getDeviceId(), 1000L, callback);
        messageSubscribe.addSubscribe(messageEvent);

        Request request = headerProvider.createMessageRequest(device, catalogXml.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, eventResult -> {
            messageSubscribe.removeSubscribe(messageEvent.getKey());
            if (callback != null) {
                callback.run(ErrorCode.ERROR100.getCode(), "失败，" + eventResult.msg, null);
            }
        });

    }

    /**
     * 查询目录列表
     *
     * @param device 视频设备
     */
    @Override
    public void catalogQuery(Device device, int sn, SipSubscribe.Event errorEvent) throws SipException, InvalidArgumentException, ParseException {

        StringBuffer catalogXml = new StringBuffer(200);
        String charset = device.getCharset();
        catalogXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        catalogXml.append("<Query>\r\n");
        catalogXml.append("  <CmdType>Catalog</CmdType>\r\n");
        catalogXml.append("  <SN>" + sn + "</SN>\r\n");
        catalogXml.append("  <DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        catalogXml.append("</Query>\r\n");

        Request request = headerProvider.createMessageRequest(device, catalogXml.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent);
    }

    /**
     * 查询录像信息
     *
     * @param device    视频设备
     * @param startTime 开始时间,格式要求：yyyy-MM-dd HH:mm:ss
     * @param endTime   结束时间,格式要求：yyyy-MM-dd HH:mm:ss
     */
    @Override
    public void recordInfoQuery(Device device, String channelId, String startTime, String endTime, int sn, Integer secrecy, String type, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException {
        if (secrecy == null) {
            secrecy = 0;
        }
        if (type == null) {
            type = "all";
        }

        StringBuffer recordInfoXml = new StringBuffer(200);
        String charset = device.getCharset();
        recordInfoXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        recordInfoXml.append("<Query>\r\n");
        recordInfoXml.append("<CmdType>RecordInfo</CmdType>\r\n");
        recordInfoXml.append("<SN>" + sn + "</SN>\r\n");
        recordInfoXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        if (startTime != null) {
            recordInfoXml.append("<StartTime>" + DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(startTime) + "</StartTime>\r\n");
        }
        if (endTime != null) {
            recordInfoXml.append("<EndTime>" + DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(endTime) + "</EndTime>\r\n");
        }
        if (secrecy != null) {
            recordInfoXml.append("<Secrecy> " + secrecy + " </Secrecy>\r\n");
        }
        if (type != null) {
            // 大华NVR要求必须增加一个值为all的文本元素节点Type
            recordInfoXml.append("<Type>" + type + "</Type>\r\n");
        }
        recordInfoXml.append("</Query>\r\n");



        Request request = headerProvider.createMessageRequest(device, recordInfoXml.toString(),
                SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent, okEvent);
    }

    /**
     * 查询报警信息
     *
     * @param device        视频设备
     * @param startPriority 报警起始级别（可选）
     * @param endPriority   报警终止级别（可选）
     * @param alarmMethod   报警方式条件（可选）
     * @param alarmType     报警类型
     * @param startTime     报警发生起始时间（可选）
     * @param endTime       报警发生终止时间（可选）
     * @return true = 命令发送成功
     */
    @Override
    public void alarmInfoQuery(Device device, String startPriority, String endPriority, String alarmMethod, String alarmType,
                               String startTime, String endTime, ErrorCallback<Object> callback) throws InvalidArgumentException, SipException, ParseException {

        String cmdType = "Alarm";
        String sn = (int) ((Math.random() * 9 + 1) * 100000) + "";

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Query>\r\n");
        cmdXml.append("<CmdType>" + cmdType + "</CmdType>\r\n");
        cmdXml.append("<SN>" + sn + "</SN>\r\n");
        cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        if (!ObjectUtils.isEmpty(startPriority)) {
            cmdXml.append("<StartAlarmPriority>" + startPriority + "</StartAlarmPriority>\r\n");
        }
        if (!ObjectUtils.isEmpty(endPriority)) {
            cmdXml.append("<EndAlarmPriority>" + endPriority + "</EndAlarmPriority>\r\n");
        }
        if (!ObjectUtils.isEmpty(alarmMethod)) {
            cmdXml.append("<AlarmMethod>" + alarmMethod + "</AlarmMethod>\r\n");
        }
        if (!ObjectUtils.isEmpty(alarmType)) {
            cmdXml.append("<AlarmType>" + alarmType + "</AlarmType>\r\n");
        }
        if (!ObjectUtils.isEmpty(startTime)) {
            cmdXml.append("<StartAlarmTime>" + startTime + "</StartAlarmTime>\r\n");
        }
        if (!ObjectUtils.isEmpty(endTime)) {
            cmdXml.append("<EndAlarmTime>" + endTime + "</EndAlarmTime>\r\n");
        }
        cmdXml.append("</Query>\r\n");

        MessageEvent<Object> messageEvent = MessageEvent.getInstance(cmdType, sn, device.getDeviceId(), 1000L, callback);
        messageSubscribe.addSubscribe(messageEvent);

        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, eventResult -> {
            messageSubscribe.removeSubscribe(messageEvent.getKey());
            callback.run(ErrorCode.ERROR100.getCode(), "失败，" + eventResult.msg, null);
        });
    }

    /**
     * 查询设备配置
     *
     * @param device     视频设备
     * @param channelId  通道编码（可选）
     * @param configType 配置类型：
     */
    @Override
    public void deviceConfigQuery(Device device, String channelId, String configType, ErrorCallback<Object> callback) throws InvalidArgumentException, SipException, ParseException {

        String cmdType = "ConfigDownload";
        int sn = (int) ((Math.random() * 9 + 1) * 100000);
        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Query>\r\n");
        cmdXml.append("<CmdType>" + cmdType + "</CmdType>\r\n");
        cmdXml.append("<SN>" + sn + "</SN>\r\n");
        if (ObjectUtils.isEmpty(channelId)) {
            cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        } else {
            cmdXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        }
        cmdXml.append("<ConfigType>" + configType + "</ConfigType>\r\n");
        cmdXml.append("</Query>\r\n");

        MessageEvent<Object> messageEvent = MessageEvent.getInstance(cmdType, sn + "", channelId, 1000L, callback);
        messageSubscribe.addSubscribe(messageEvent);

        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, eventResult -> {
            messageSubscribe.removeSubscribe(messageEvent.getKey());
            if (callback !=  null) {
                callback.run(ErrorCode.ERROR100.getCode(), "失败，" + eventResult.msg, null);
            }
        });
    }

    /**
     * 查询设备预置位置
     *
     * @param device 视频设备
     */
    @Override
    public void presetQuery(Device device, String channelId, ErrorCallback<Object> callback) throws InvalidArgumentException, SipException, ParseException {

        String cmdType = "PresetQuery";
        int sn = (int) ((Math.random() * 9 + 1) * 100000);

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Query>\r\n");
        cmdXml.append("<CmdType>" + cmdType + "</CmdType>\r\n");
        cmdXml.append("<SN>" + sn + "</SN>\r\n");
        if (ObjectUtils.isEmpty(channelId)) {
            cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        } else {
            cmdXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        }
        cmdXml.append("</Query>\r\n");

        MessageEvent<Object> messageEvent = MessageEvent.getInstance(cmdType, sn + "", channelId, 1000L, callback);
        messageSubscribe.addSubscribe(messageEvent);

        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, eventResult -> {
            messageSubscribe.removeSubscribe(messageEvent.getKey());
            callback.run(ErrorCode.ERROR100.getCode(), "失败，" + eventResult.msg, null);
        });
    }

    /**
     * 查询移动设备位置数据
     *
     * @param device 视频设备
     */
    @Override
    public void mobilePostitionQuery(Device device, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer mobilePostitionXml = new StringBuffer(200);
        String charset = device.getCharset();
        mobilePostitionXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        mobilePostitionXml.append("<Query>\r\n");
        mobilePostitionXml.append("<CmdType>MobilePosition</CmdType>\r\n");
        mobilePostitionXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        mobilePostitionXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        mobilePostitionXml.append("<Interval>60</Interval>\r\n");
        mobilePostitionXml.append("</Query>\r\n");



        Request request = headerProvider.createMessageRequest(device, mobilePostitionXml.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent);

    }

    /**
     * 订阅、取消订阅移动位置
     *
     * @param device 视频设备
     * @return true = 命令发送成功
     */
    @Override
    public SIPRequest mobilePositionSubscribe(Device device, SIPRequest requestOld, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer subscribePostitionXml = new StringBuffer(200);
        String charset = device.getCharset();
        subscribePostitionXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        subscribePostitionXml.append("<Query>\r\n");
        subscribePostitionXml.append("<CmdType>MobilePosition</CmdType>\r\n");
        subscribePostitionXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        subscribePostitionXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        if (device.getSubscribeCycleForMobilePosition() > 0) {
            subscribePostitionXml.append("<Interval>" + device.getMobilePositionSubmissionInterval() + "</Interval>\r\n");
        }else {
            subscribePostitionXml.append("<Interval>5</Interval>\r\n");
        }
        subscribePostitionXml.append("</Query>\r\n");

        CallIdHeader callIdHeader;

        if (requestOld != null) {
            callIdHeader = SipFactory.getInstance().createHeaderFactory().createCallIdHeader(requestOld.getCallIdHeader().getCallId());
        } else {
            callIdHeader = sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport());
        }
        SIPRequest request = (SIPRequest) headerProvider.createSubscribeRequest(device, subscribePostitionXml.toString(), requestOld, device.getSubscribeCycleForMobilePosition(), "presence",callIdHeader); //Position;id=" + tm.substring(tm.length() - 4));

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent, okEvent);
        return request;
    }

    /**
     * 订阅、取消订阅报警信息
     *
     * @param device        视频设备
     * @param expires       订阅过期时间（0 = 取消订阅）
     * @param startPriority 报警起始级别（可选）
     * @param endPriority   报警终止级别（可选）
     * @param alarmMethod   报警方式条件（可选）
     * @param startTime     报警发生起始时间（可选）
     * @param endTime       报警发生终止时间（可选）
     * @return true = 命令发送成功
     */
    @Override
    public void alarmSubscribe(Device device, int expires, String startPriority, String endPriority, String alarmMethod, String startTime, String endTime) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Query>\r\n");
        cmdXml.append("<CmdType>Alarm</CmdType>\r\n");
        cmdXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        if (!ObjectUtils.isEmpty(startPriority)) {
            cmdXml.append("<StartAlarmPriority>" + startPriority + "</StartAlarmPriority>\r\n");
        }
        if (!ObjectUtils.isEmpty(endPriority)) {
            cmdXml.append("<EndAlarmPriority>" + endPriority + "</EndAlarmPriority>\r\n");
        }
        if (!ObjectUtils.isEmpty(alarmMethod)) {
            cmdXml.append("<AlarmMethod>" + alarmMethod + "</AlarmMethod>\r\n");
        }
        if (!ObjectUtils.isEmpty(startTime)) {
            cmdXml.append("<StartAlarmTime>" + startTime + "</StartAlarmTime>\r\n");
        }
        if (!ObjectUtils.isEmpty(endTime)) {
            cmdXml.append("<EndAlarmTime>" + endTime + "</EndAlarmTime>\r\n");
        }
        cmdXml.append("</Query>\r\n");



        Request request = headerProvider.createSubscribeRequest(device, cmdXml.toString(), null, expires, "presence",sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request);

    }

    @Override
    public SIPRequest catalogSubscribe(Device device, SIPRequest requestOld, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Query>\r\n");
        cmdXml.append("<CmdType>Catalog</CmdType>\r\n");
        cmdXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        cmdXml.append("</Query>\r\n");

        CallIdHeader callIdHeader;

        if (requestOld != null) {
            callIdHeader = SipFactory.getInstance().createHeaderFactory().createCallIdHeader(requestOld.getCallIdHeader().getCallId());
        } else {
            callIdHeader = sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport());
        }

        // 有效时间默认为60秒以上
        SIPRequest request = (SIPRequest) headerProvider.createSubscribeRequest(device, cmdXml.toString(), requestOld, device.getSubscribeCycleForCatalog(), "Catalog",
                callIdHeader);
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent, okEvent);
        return request;
    }

    @Override
    public void dragZoomCmd(Device device, String channelId, String cmdString, ErrorCallback<String> callback) throws InvalidArgumentException, SipException, ParseException {

        String cmdType = "DeviceControl";
        int sn = (int) ((Math.random() * 9 + 1) * 100000);

        StringBuffer dragXml = new StringBuffer(200);
        String charset = device.getCharset();
        dragXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        dragXml.append("<Control>\r\n");
        dragXml.append("<CmdType>" + cmdType + "</CmdType>\r\n");
        dragXml.append("<SN>" + sn + "</SN>\r\n");
        if (ObjectUtils.isEmpty(channelId)) {
            dragXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        } else {
            dragXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        }
        dragXml.append(cmdString);
        dragXml.append("</Control>\r\n");

        MessageEvent<String> messageEvent = MessageEvent.getInstance(cmdType, sn + "", channelId, 1000L, callback);
        messageSubscribe.addSubscribe(messageEvent);

        Request request = headerProvider.createMessageRequest(device, dragXml.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()),request);
    }



    /**
     * 回放暂停
     */
    @Override
    public void playPauseCmd(Device device, DeviceChannel channel, StreamInfo streamInfo) throws InvalidArgumentException, ParseException, SipException {
        StringBuffer content = new StringBuffer(200);
        content.append("PAUSE RTSP/1.0\r\n");
        content.append("CSeq: " + getInfoCseq() + "\r\n");
        content.append("PauseTime: now\r\n");

        playbackControlCmd(device, channel, streamInfo, content.toString(), null, null);
    }


    /**
     * 回放恢复
     */
    @Override
    public void playResumeCmd(Device device, DeviceChannel channel, StreamInfo streamInfo) throws InvalidArgumentException, ParseException, SipException {
        StringBuffer content = new StringBuffer(200);
        content.append("PLAY RTSP/1.0\r\n");
        content.append("CSeq: " + getInfoCseq() + "\r\n");
        content.append("Range: npt=now-\r\n");

        playbackControlCmd(device, channel, streamInfo, content.toString(), null, null);
    }

    /**
     * 回放拖动播放
     */
    @Override
    public void playSeekCmd(Device device, DeviceChannel channel, StreamInfo streamInfo, long seekTime) throws InvalidArgumentException, ParseException, SipException {
        StringBuffer content = new StringBuffer(200);
        content.append("PLAY RTSP/1.0\r\n");
        content.append("CSeq: " + getInfoCseq() + "\r\n");
        content.append("Range: npt=" + Math.abs(seekTime) + "-\r\n");

        playbackControlCmd(device, channel, streamInfo, content.toString(), null, null);
    }

    /**
     * 回放倍速播放
     */
    @Override
    public void playSpeedCmd(Device device, DeviceChannel channel, StreamInfo streamInfo, Double speed) throws InvalidArgumentException, ParseException, SipException {
        StringBuffer content = new StringBuffer(200);
        content.append("PLAY RTSP/1.0\r\n");
        content.append("CSeq: " + getInfoCseq() + "\r\n");
        content.append("Scale: " + String.format("%.6f", speed) + "\r\n");

        playbackControlCmd(device, channel, streamInfo, content.toString(), null, null);
    }

    private int getInfoCseq() {
        return (int) ((Math.random() * 9 + 1) * Math.pow(10, 8));
    }

    @Override
    public void playbackControlCmd(Device device, DeviceChannel channel, StreamInfo streamInfo, String content, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws SipException, InvalidArgumentException, ParseException {

        playbackControlCmd(device, channel, streamInfo.getStream(), content, errorEvent, okEvent);
    }

    @Override
    public void playbackControlCmd(Device device, DeviceChannel channel, String stream, String content, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws SipException, InvalidArgumentException, ParseException {

        SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByStream("rtp", stream);
        if (ssrcTransaction == null) {
            log.info("[回放控制]未找到视频流信息，设备：{}, 流ID: {}", device.getDeviceId(), stream);
            return;
        }

        SIPRequest request = headerProvider.createInfoRequest(device, channel.getDeviceId(), content, ssrcTransaction.getSipTransactionInfo());
        if (request == null) {
            log.info("[回放控制]构建Request信息失败，设备：{}, 流ID: {}", device.getDeviceId(), stream);
            return;
        }

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent, okEvent);
    }

    @Override
    public void sendAlarmMessage(Device device, DeviceAlarm deviceAlarm) throws InvalidArgumentException, SipException, ParseException {
        if (device == null) {
            return;
        }
        log.info("[发送报警通知]设备： {}/{}->{},{}", device.getDeviceId(), deviceAlarm.getChannelId(),
                deviceAlarm.getLongitude(), deviceAlarm.getLatitude());

        String characterSet = device.getCharset();
        StringBuffer deviceStatusXml = new StringBuffer(600);
        deviceStatusXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n");
        deviceStatusXml.append("<Notify>\r\n");
        deviceStatusXml.append("<CmdType>Alarm</CmdType>\r\n");
        deviceStatusXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        deviceStatusXml.append("<DeviceID>" + deviceAlarm.getChannelId() + "</DeviceID>\r\n");
        deviceStatusXml.append("<AlarmPriority>" + deviceAlarm.getAlarmPriority() + "</AlarmPriority>\r\n");
        deviceStatusXml.append("<AlarmMethod>" + deviceAlarm.getAlarmMethod() + "</AlarmMethod>\r\n");
        deviceStatusXml.append("<AlarmTime>" + DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(deviceAlarm.getAlarmTime()) + "</AlarmTime>\r\n");
        deviceStatusXml.append("<AlarmDescription>" + deviceAlarm.getAlarmDescription() + "</AlarmDescription>\r\n");
        deviceStatusXml.append("<Longitude>" + deviceAlarm.getLongitude() + "</Longitude>\r\n");
        deviceStatusXml.append("<Latitude>" + deviceAlarm.getLatitude() + "</Latitude>\r\n");
        deviceStatusXml.append("<info>\r\n");
        deviceStatusXml.append("<AlarmType>" + deviceAlarm.getAlarmType() + "</AlarmType>\r\n");
        deviceStatusXml.append("</info>\r\n");
        deviceStatusXml.append("</Notify>\r\n");


        Request request = headerProvider.createMessageRequest(device, deviceStatusXml.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()),request);


    }
}
