package com.genersoft.iot.vmp.gb28181.transmit.cmd.impl;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarm;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.SIPSender;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.SIPRequestHeaderProvider;
import com.genersoft.iot.vmp.gb28181.utils.NumericUtil;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeFactory;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForStreamChange;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.hook.HookParam;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.utils.DateUtil;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class SIPCommander implements ISIPCommander {

    private final Logger logger = LoggerFactory.getLogger(SIPCommander.class);

    @Autowired
    private SipConfig sipConfig;

    @Autowired
    private SipLayer sipLayer;

    @Autowired
    private SIPSender sipSender;
    
    @Autowired
    private SIPRequestHeaderProvider headerProvider;

    @Autowired
    private VideoStreamSessionManager streamSession;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private ZlmHttpHookSubscribe subscribe;



    @Autowired
    private IMediaServerService mediaServerService;


    /**
     * 云台方向放控制，使用配置文件中的默认镜头移动速度
     *
     * @param device    控制设备
     * @param channelId 预览通道
     * @param leftRight 镜头左移右移 0:停止 1:左移 2:右移
     * @param upDown    镜头上移下移 0:停止 1:上移 2:下移
     */
    @Override
    public void ptzdirectCmd(Device device, String channelId, int leftRight, int upDown) throws InvalidArgumentException, ParseException, SipException {
        ptzCmd(device, channelId, leftRight, upDown, 0, sipConfig.getPtzSpeed(), 0);
    }

    /**
     * 云台方向放控制
     *
     * @param device    控制设备
     * @param channelId 预览通道
     * @param leftRight 镜头左移右移 0:停止 1:左移 2:右移
     * @param upDown    镜头上移下移 0:停止 1:上移 2:下移
     * @param moveSpeed 镜头移动速度
     */
    @Override
    public void ptzdirectCmd(Device device, String channelId, int leftRight, int upDown, int moveSpeed) throws InvalidArgumentException, ParseException, SipException {
        ptzCmd(device, channelId, leftRight, upDown, 0, moveSpeed, 0);
    }

    /**
     * 云台缩放控制，使用配置文件中的默认镜头缩放速度
     *
     * @param device    控制设备
     * @param channelId 预览通道
     * @param inOut     镜头放大缩小 0:停止 1:缩小 2:放大
     */
    @Override
    public void ptzZoomCmd(Device device, String channelId, int inOut) throws InvalidArgumentException, ParseException, SipException {
        ptzCmd(device, channelId, 0, 0, inOut, 0, sipConfig.getPtzSpeed());
    }

    /**
     * 云台缩放控制
     *
     * @param device    控制设备
     * @param channelId 预览通道
     * @param inOut     镜头放大缩小 0:停止 1:缩小 2:放大
     * @param zoomSpeed 镜头缩放速度
     */
    @Override
    public void ptzZoomCmd(Device device, String channelId, int inOut, int zoomSpeed) throws InvalidArgumentException, ParseException, SipException {
        ptzCmd(device, channelId, 0, 0, inOut, 0, zoomSpeed);
    }

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
        //优化zoom变倍速率
        if ((combineCode2 > 0) && (combineCode2 <16))
        {
            combineCode2 = 16;
        }
        strTmp = String.format("%X", combineCode2);
        builder.append(strTmp, 0, 1).append("0");
        //计算校验码
        int checkCode = (0XA5 + 0X0F + 0X01 + cmdCode + parameter1 + parameter2 + (combineCode2 & 0XF0)) % 0X100;
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
     * @param channelId  预览通道
     * @param event      hook订阅
     * @param errorEvent sip错误订阅
     */
    @Override
    public void playStreamCmd(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo, Device device, String channelId,
                              ZlmHttpHookSubscribe.Event event, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException {
        String stream = ssrcInfo.getStream();

        if (device == null) {
            return;
        }

        logger.info("{} 分配的ZLM为: {} [{}:{}]", stream, mediaServerItem.getId(), mediaServerItem.getSdpIp(), ssrcInfo.getPort());
        HookSubscribeForStreamChange hookSubscribe = HookSubscribeFactory.on_stream_changed("rtp", stream, true, "rtsp", mediaServerItem.getId());
        subscribe.addSubscribe(hookSubscribe, (MediaServerItem mediaServerItemInUse, HookParam hookParam) -> {
            if (event != null) {
                event.response(mediaServerItemInUse, hookParam);
                subscribe.removeSubscribe(hookSubscribe);
            }
        });
        String sdpIp;
        if (!ObjectUtils.isEmpty(device.getSdpIp())) {
            sdpIp = device.getSdpIp();
        }else {
            sdpIp = mediaServerItem.getSdpIp();
        }
        StringBuffer content = new StringBuffer(200);
        content.append("v=0\r\n");
        content.append("o=" + channelId + " 0 0 IN IP4 " + sdpIp + "\r\n");
        content.append("s=Play\r\n");
        content.append("c=IN IP4 " + sdpIp + "\r\n");
        content.append("t=0 0\r\n");

        if (userSetting.isSeniorSdp()) {
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

        if( device.isSwitchPrimarySubStream() ){
            if("TP-LINK".equals(device.getManufacturer())){
                if (device.isSwitchPrimarySubStream()){
                    content.append("a=streamMode:sub\r\n");
                }else {
                    content.append("a=streamMode:main\r\n");
                }
            }else {
                if (device.isSwitchPrimarySubStream()){
                    content.append("a=streamprofile:1\r\n");
                }else {
                    content.append("a=streamprofile:0\r\n");
                }
            }
        }

        content.append("y=" + ssrcInfo.getSsrc() + "\r\n");//ssrc
        // f字段:f= v/编码格式/分辨率/帧率/码率类型/码率大小a/编码格式/码率大小/采样率
//			content.append("f=v/2/5/25/1/4000a/1/8/1" + "\r\n"); // 未发现支持此特性的设备



        Request request = headerProvider.createInviteRequest(device, channelId, content.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null, ssrcInfo.getSsrc(),sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, (e -> {
            streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());
            mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
            errorEvent.response(e);
        }), e -> {
            ResponseEvent responseEvent = (ResponseEvent) e.event;
            SIPResponse response = (SIPResponse) responseEvent.getResponse();
            streamSession.put(device.getDeviceId(), channelId, "play", stream, ssrcInfo.getSsrc(), mediaServerItem.getId(), response,
                    InviteSessionType.PLAY);
            okEvent.response(e);
        });
    }

    /**
     * 请求回放视频流
     *
     * @param device    视频设备
     * @param channelId 预览通道
     * @param startTime 开始时间,格式要求：yyyy-MM-dd HH:mm:ss
     * @param endTime   结束时间,格式要求：yyyy-MM-dd HH:mm:ss
     */
    @Override
    public void playbackStreamCmd(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo, Device device, String channelId,
                                  String startTime, String endTime, ZlmHttpHookSubscribe.Event hookEvent,
                                  SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException {


        logger.info("{} 分配的ZLM为: {} [{}:{}]", ssrcInfo.getStream(), mediaServerItem.getId(), mediaServerItem.getSdpIp(), ssrcInfo.getPort());
        String sdpIp;
        if (!ObjectUtils.isEmpty(device.getSdpIp())) {
            sdpIp = device.getSdpIp();
        }else {
            sdpIp = mediaServerItem.getSdpIp();
        }
        StringBuffer content = new StringBuffer(200);
        content.append("v=0\r\n");
        content.append("o=" + channelId + " 0 0 IN IP4 " + sdpIp + "\r\n");
        content.append("s=Playback\r\n");
        content.append("u=" + channelId + ":0\r\n");
        content.append("c=IN IP4 " + sdpIp + "\r\n");
        content.append("t=" + DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime) + " "
                + DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime) + "\r\n");

        String streamMode = device.getStreamMode();

        if (userSetting.isSeniorSdp()) {
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

        HookSubscribeForStreamChange hookSubscribe = HookSubscribeFactory.on_stream_changed("rtp", ssrcInfo.getStream(), true, "rtsp", mediaServerItem.getId());
        // 添加订阅
        subscribe.addSubscribe(hookSubscribe, (MediaServerItem mediaServerItemInUse, HookParam hookParam) -> {
            if (hookEvent != null) {
                hookEvent.response(mediaServerItemInUse, hookParam);
            }
            subscribe.removeSubscribe(hookSubscribe);
        });
        Request request = headerProvider.createPlaybackInviteRequest(device, channelId, content.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()), ssrcInfo.getSsrc());

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent, event -> {
            ResponseEvent responseEvent = (ResponseEvent) event.event;
            SIPResponse response = (SIPResponse) responseEvent.getResponse();
            streamSession.put(device.getDeviceId(), channelId,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()).getCallId(), ssrcInfo.getStream(), ssrcInfo.getSsrc(), mediaServerItem.getId(), response, InviteSessionType.PLAYBACK);
            okEvent.response(event);
        });
    }

    /**
     * 请求历史媒体下载
     *
     * @param device        视频设备
     * @param channelId     预览通道
     * @param startTime     开始时间,格式要求：yyyy-MM-dd HH:mm:ss
     * @param endTime       结束时间,格式要求：yyyy-MM-dd HH:mm:ss
     * @param downloadSpeed 下载倍速参数
     */
    @Override
    public void downloadStreamCmd(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo, Device device, String channelId,
                                  String startTime, String endTime, int downloadSpeed,
                                  ZlmHttpHookSubscribe.Event hookEvent,
                                  SipSubscribe.Event errorEvent,SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException {

        logger.info("{} 分配的ZLM为: {} [{}:{}]", ssrcInfo.getStream(), mediaServerItem.getId(), mediaServerItem.getSdpIp(), ssrcInfo.getPort());
        String sdpIp;
        if (!ObjectUtils.isEmpty(device.getSdpIp())) {
            sdpIp = device.getSdpIp();
        }else {
            sdpIp = mediaServerItem.getSdpIp();
        }
        StringBuffer content = new StringBuffer(200);
        content.append("v=0\r\n");
        content.append("o=" + channelId + " 0 0 IN IP4 " + sdpIp + "\r\n");
        content.append("s=Download\r\n");
        content.append("u=" + channelId + ":0\r\n");
        content.append("c=IN IP4 " + sdpIp + "\r\n");
        content.append("t=" + DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime) + " "
                + DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime) + "\r\n");

        String streamMode = device.getStreamMode().toUpperCase();

        if (userSetting.isSeniorSdp()) {
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
        logger.debug("此时请求下载信令的ssrc===>{}",ssrcInfo.getSsrc());
        HookSubscribeForStreamChange hookSubscribe = HookSubscribeFactory.on_stream_changed("rtp", ssrcInfo.getStream(), true, "rtsp", mediaServerItem.getId());
        // 添加订阅
        CallIdHeader newCallIdHeader = sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()), device.getTransport());
        String callId= newCallIdHeader.getCallId();
        subscribe.addSubscribe(hookSubscribe, (mediaServerItemInUse, hookParam) -> {
            logger.debug("sipc 添加订阅===callId {}",callId);
            hookEvent.response(mediaServerItemInUse, hookParam);
            subscribe.removeSubscribe(hookSubscribe);
            hookSubscribe.getContent().put("regist", false);
            hookSubscribe.getContent().put("schema", "rtsp");
            // 添加流注销的订阅，注销了后向设备发送bye
            subscribe.addSubscribe(hookSubscribe,
                    (mediaServerItemForEnd, hookParam1) -> {
                        logger.info("[录像]下载结束， 发送BYE");
                        try {
                            streamByeCmd(device, channelId, ssrcInfo.getStream(), callId);
                        } catch (InvalidArgumentException | ParseException | SipException |
                                 SsrcTransactionNotFoundException e) {
                            logger.error("[录像]下载结束， 发送BYE失败 {}", e.getMessage());
                        }
                    });
        });

        Request request = headerProvider.createPlaybackInviteRequest(device, channelId, content.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,newCallIdHeader, ssrcInfo.getSsrc());

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent, event -> {
            ResponseEvent responseEvent = (ResponseEvent) event.event;
            SIPResponse response = (SIPResponse) responseEvent.getResponse();
            String contentString =new String(response.getRawContent());
            String ssrc = SipUtils.getSsrcFromSdp(contentString);
            streamSession.put(device.getDeviceId(), channelId, response.getCallIdHeader().getCallId(), ssrcInfo.getStream(), ssrc, mediaServerItem.getId(), response, InviteSessionType.DOWNLOAD);
            okEvent.response(event);
        });
    }

    /**
     * 视频流停止, 不使用回调
     */
    @Override
    public void streamByeCmd(Device device, String channelId, String stream, String callId) throws InvalidArgumentException, ParseException, SipException, SsrcTransactionNotFoundException {
        streamByeCmd(device, channelId, stream, callId, null);
    }

    /**
     * 视频流停止
     */
    @Override
    public void streamByeCmd(Device device, String channelId, String stream, String callId, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException, SsrcTransactionNotFoundException {
        SsrcTransaction ssrcTransaction = streamSession.getSsrcTransaction(device.getDeviceId(), channelId, callId, stream);
        if (ssrcTransaction == null) {
            throw new SsrcTransactionNotFoundException(device.getDeviceId(), channelId, callId, stream);
        }

        mediaServerService.releaseSsrc(ssrcTransaction.getMediaServerId(), ssrcTransaction.getSsrc());
        mediaServerService.closeRTPServer(ssrcTransaction.getMediaServerId(), ssrcTransaction.getStream());
        streamSession.remove(ssrcTransaction.getDeviceId(), ssrcTransaction.getChannelId(), ssrcTransaction.getStream());

        Request byteRequest = headerProvider.createByteRequest(device, channelId, ssrcTransaction.getSipTransactionInfo());
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), byteRequest, null, okEvent);
    }

    /**
     * 语音广播
     *
     * @param device    视频设备
     * @param channelId 预览通道
     */
    @Override
    public void audioBroadcastCmd(Device device, String channelId) {
    }

    /**
     * 语音广播
     *
     * @param device 视频设备
     */
    @Override
    public void audioBroadcastCmd(Device device) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer broadcastXml = new StringBuffer(200);
        String charset = device.getCharset();
        broadcastXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        broadcastXml.append("<Notify>\r\n");
        broadcastXml.append("<CmdType>Broadcast</CmdType>\r\n");
        broadcastXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        broadcastXml.append("<SourceID>" + sipConfig.getId() + "</SourceID>\r\n");
        broadcastXml.append("<TargetID>" + device.getDeviceId() + "</TargetID>\r\n");
        broadcastXml.append("</Notify>\r\n");

        

        Request request = headerProvider.createMessageRequest(device, broadcastXml.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request);

    }

    @Override
    public void audioBroadcastCmd(Device device, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer broadcastXml = new StringBuffer(200);
        String charset = device.getCharset();
        broadcastXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        broadcastXml.append("<Notify>\r\n");
        broadcastXml.append("<CmdType>Broadcast</CmdType>\r\n");
        broadcastXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        broadcastXml.append("<SourceID>" + sipConfig.getId() + "</SourceID>\r\n");
        broadcastXml.append("<TargetID>" + device.getDeviceId() + "</TargetID>\r\n");
        broadcastXml.append("</Notify>\r\n");

        

        Request request = headerProvider.createMessageRequest(device, broadcastXml.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent);

    }


    /**
     * 音视频录像控制
     *
     * @param device       视频设备
     * @param channelId    预览通道
     * @param recordCmdStr 录像命令：Record / StopRecord
     */
    @Override
    public void recordCmd(Device device, String channelId, String recordCmdStr, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException {
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
        cmdXml.append("<RecordCmd>" + recordCmdStr + "</RecordCmd>\r\n");
        cmdXml.append("</Control>\r\n");

        

        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent,okEvent);
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
    public void guardCmd(Device device, String guardCmdStr, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Control>\r\n");
        cmdXml.append("<CmdType>DeviceControl</CmdType>\r\n");
        cmdXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        cmdXml.append("<GuardCmd>" + guardCmdStr + "</GuardCmd>\r\n");
        cmdXml.append("</Control>\r\n");

        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent,okEvent);
    }

    /**
     * 报警复位命令
     *
     * @param device 视频设备
     */
    @Override
    public void alarmCmd(Device device, String alarmMethod, String alarmType, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Control>\r\n");
        cmdXml.append("<CmdType>DeviceControl</CmdType>\r\n");
        cmdXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
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

        

        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent,okEvent);
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
    public void homePositionCmd(Device device, String channelId, String enabled, String resetTime, String presetIndex, SipSubscribe.Event errorEvent,SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException {

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
        cmdXml.append("<HomePosition>\r\n");
        if (NumericUtil.isInteger(enabled) && (!enabled.equals("0"))) {
            cmdXml.append("<Enabled>1</Enabled>\r\n");
            if (NumericUtil.isInteger(resetTime)) {
                cmdXml.append("<ResetTime>" + resetTime + "</ResetTime>\r\n");
            } else {
                cmdXml.append("<ResetTime>0</ResetTime>\r\n");
            }
            if (NumericUtil.isInteger(presetIndex)) {
                cmdXml.append("<PresetIndex>" + presetIndex + "</PresetIndex>\r\n");
            } else {
                cmdXml.append("<PresetIndex>0</PresetIndex>\r\n");
            }
        } else {
            cmdXml.append("<Enabled>0</Enabled>\r\n");
        }
        cmdXml.append("</HomePosition>\r\n");
        cmdXml.append("</Control>\r\n");

        

        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent,okEvent);
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
     *
     * @param device            视频设备
     * @param channelId         通道编码（可选）
     * @param name              设备/通道名称（可选）
     * @param expiration        注册过期时间（可选）
     * @param heartBeatInterval 心跳间隔时间（可选）
     * @param heartBeatCount    心跳超时次数（可选）
     */
    @Override
    public void deviceBasicConfigCmd(Device device, String channelId, String name, String expiration,
                                     String heartBeatInterval, String heartBeatCount, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Control>\r\n");
        cmdXml.append("<CmdType>DeviceConfig</CmdType>\r\n");
        cmdXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        if (ObjectUtils.isEmpty(channelId)) {
            cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        } else {
            cmdXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        }
        cmdXml.append("<BasicParam>\r\n");
        if (!ObjectUtils.isEmpty(name)) {
            cmdXml.append("<Name>" + name + "</Name>\r\n");
        }
        if (NumericUtil.isInteger(expiration)) {
            if (Integer.valueOf(expiration) > 0) {
                cmdXml.append("<Expiration>" + expiration + "</Expiration>\r\n");
            }
        }
        if (NumericUtil.isInteger(heartBeatInterval)) {
            if (Integer.valueOf(heartBeatInterval) > 0) {
                cmdXml.append("<HeartBeatInterval>" + heartBeatInterval + "</HeartBeatInterval>\r\n");
            }
        }
        if (NumericUtil.isInteger(heartBeatCount)) {
            if (Integer.valueOf(heartBeatCount) > 0) {
                cmdXml.append("<HeartBeatCount>" + heartBeatCount + "</HeartBeatCount>\r\n");
            }
        }
        cmdXml.append("</BasicParam>\r\n");
        cmdXml.append("</Control>\r\n");

        

        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent);
    }

    /**
     * 查询设备状态
     *
     * @param device 视频设备
     */
    @Override
    public void deviceStatusQuery(Device device, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException {

        String charset = device.getCharset();
        StringBuffer catalogXml = new StringBuffer(200);
        catalogXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        catalogXml.append("<Query>\r\n");
        catalogXml.append("<CmdType>DeviceStatus</CmdType>\r\n");
        catalogXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        catalogXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        catalogXml.append("</Query>\r\n");

        Request request = headerProvider.createMessageRequest(device, catalogXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent);
    }

    /**
     * 查询设备信息
     *
     * @param device 视频设备
     */
    @Override
    public void deviceInfoQuery(Device device) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer catalogXml = new StringBuffer(200);
        String charset = device.getCharset();
        catalogXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        catalogXml.append("<Query>\r\n");
        catalogXml.append("<CmdType>DeviceInfo</CmdType>\r\n");
        catalogXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        catalogXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        catalogXml.append("</Query>\r\n");

        

        Request request = headerProvider.createMessageRequest(device, catalogXml.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request);

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
                               String startTime, String endTime, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException {

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

        

        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent);
    }

    /**
     * 查询设备配置
     *
     * @param device     视频设备
     * @param channelId  通道编码（可选）
     * @param configType 配置类型：
     */
    @Override
    public void deviceConfigQuery(Device device, String channelId, String configType, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Query>\r\n");
        cmdXml.append("<CmdType>ConfigDownload</CmdType>\r\n");
        cmdXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        if (ObjectUtils.isEmpty(channelId)) {
            cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        } else {
            cmdXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        }
        cmdXml.append("<ConfigType>" + configType + "</ConfigType>\r\n");
        cmdXml.append("</Query>\r\n");

        

        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent);
    }

    /**
     * 查询设备预置位置
     *
     * @param device 视频设备
     */
    @Override
    public void presetQuery(Device device, String channelId, SipSubscribe.Event errorEvent) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = device.getCharset();
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Query>\r\n");
        cmdXml.append("<CmdType>PresetQuery</CmdType>\r\n");
        cmdXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        if (ObjectUtils.isEmpty(channelId)) {
            cmdXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        } else {
            cmdXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        }
        cmdXml.append("</Query>\r\n");


        Request request = headerProvider.createMessageRequest(device, cmdXml.toString(), null, SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent);
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
     * @param alarmType     报警类型
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
    public void dragZoomCmd(Device device, String channelId, String cmdString) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer dragXml = new StringBuffer(200);
        String charset = device.getCharset();
        dragXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        dragXml.append("<Control>\r\n");
        dragXml.append("<CmdType>DeviceControl</CmdType>\r\n");
        dragXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        if (ObjectUtils.isEmpty(channelId)) {
            dragXml.append("<DeviceID>" + device.getDeviceId() + "</DeviceID>\r\n");
        } else {
            dragXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        }
        dragXml.append(cmdString);
        dragXml.append("</Control>\r\n");
        
        Request request = headerProvider.createMessageRequest(device, dragXml.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(), null,sipSender.getNewCallIdHeader(sipLayer.getLocalIp(device.getLocalIp()),device.getTransport()));
        logger.debug("拉框信令： " + request.toString());
        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()),request);
    }


    


    /**
     * 回放暂停
     */
    @Override
    public void playPauseCmd(Device device, StreamInfo streamInfo) throws InvalidArgumentException, ParseException, SipException {
        StringBuffer content = new StringBuffer(200);
        content.append("PAUSE RTSP/1.0\r\n");
        content.append("CSeq: " + getInfoCseq() + "\r\n");
        content.append("PauseTime: now\r\n");

        playbackControlCmd(device, streamInfo, content.toString(), null, null);
    }


    /**
     * 回放恢复
     */
    @Override
    public void playResumeCmd(Device device, StreamInfo streamInfo) throws InvalidArgumentException, ParseException, SipException {
        StringBuffer content = new StringBuffer(200);
        content.append("PLAY RTSP/1.0\r\n");
        content.append("CSeq: " + getInfoCseq() + "\r\n");
        content.append("Range: npt=now-\r\n");

        playbackControlCmd(device, streamInfo, content.toString(), null, null);
    }

    /**
     * 回放拖动播放
     */
    @Override
    public void playSeekCmd(Device device, StreamInfo streamInfo, long seekTime) throws InvalidArgumentException, ParseException, SipException {
        StringBuffer content = new StringBuffer(200);
        content.append("PLAY RTSP/1.0\r\n");
        content.append("CSeq: " + getInfoCseq() + "\r\n");
        content.append("Range: npt=" + Math.abs(seekTime) + "-\r\n");

        playbackControlCmd(device, streamInfo, content.toString(), null, null);
    }

    /**
     * 回放倍速播放
     */
    @Override
    public void playSpeedCmd(Device device, StreamInfo streamInfo, Double speed) throws InvalidArgumentException, ParseException, SipException {
        StringBuffer content = new StringBuffer(200);
        content.append("PLAY RTSP/1.0\r\n");
        content.append("CSeq: " + getInfoCseq() + "\r\n");
        content.append("Scale: " + String.format("%.6f", speed) + "\r\n");

        playbackControlCmd(device, streamInfo, content.toString(), null, null);
    }

    private int getInfoCseq() {
        return (int) ((Math.random() * 9 + 1) * Math.pow(10, 8));
    }

    @Override
    public void playbackControlCmd(Device device, StreamInfo streamInfo, String content, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws SipException, InvalidArgumentException, ParseException {

        SsrcTransaction ssrcTransaction = streamSession.getSsrcTransaction(device.getDeviceId(), streamInfo.getChannelId(), null, streamInfo.getStream());
        if (ssrcTransaction == null) {
            logger.info("[回放控制]未找到视频流信息，设备：{}, 流ID: {}", device.getDeviceId(), streamInfo.getStream());
            return;
        }

        SIPRequest request = headerProvider.createInfoRequest(device, streamInfo.getChannelId(), content.toString(), ssrcTransaction.getSipTransactionInfo());
        if (request == null) {
            logger.info("[回放控制]构建Request信息失败，设备：{}, 流ID: {}", device.getDeviceId(), streamInfo.getStream());
            return;
        }

        sipSender.transmitRequest(sipLayer.getLocalIp(device.getLocalIp()), request, errorEvent, okEvent);
    }

    @Override
    public void sendAlarmMessage(Device device, DeviceAlarm deviceAlarm) throws InvalidArgumentException, SipException, ParseException {
        if (device == null) {
            return;
        }
        logger.info("[发送报警通知]设备： {}/{}->{},{}", device.getDeviceId(), deviceAlarm.getChannelId(),
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
