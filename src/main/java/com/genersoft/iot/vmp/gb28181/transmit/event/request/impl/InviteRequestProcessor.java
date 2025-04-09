package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.service.*;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import gov.nist.javax.sdp.TimeDescriptionImpl;
import gov.nist.javax.sdp.fields.TimeField;
import gov.nist.javax.sdp.fields.URIField;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sdp.*;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.List;
import java.util.Vector;

/**
 * SIP命令类型： INVITE请求
 */
@Slf4j
@SuppressWarnings("rawtypes")
@Component
public class InviteRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

    private final String method = "INVITE";

    @Autowired
    private ISIPCommanderForPlatform cmderFroPlatform;

    @Autowired
    private IDeviceChannelService deviceChannelService;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IGbChannelService channelService;

    @Autowired
    private IGbChannelPlayService channelPlayService;

    @Autowired
    private ISendRtpServerService sendRtpServerService;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private IPlayService playService;

    @Autowired
    private IPlatformService platformService;

    @Autowired
    private AudioBroadcastManager audioBroadcastManager;

    @Autowired
    private SIPProcessorObserver sipProcessorObserver;

    @Autowired
    private SipConfig config;

    @Autowired
    private SipInviteSessionManager sessionManager;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private SSRCFactory ssrcFactory;


    @Override
    public void afterPropertiesSet() throws Exception {
        // 添加消息处理的订阅
        sipProcessorObserver.addRequestProcessor(method, this);
    }

    /**
     * 处理invite请求
     *
     * @param evt 请求消息
     */
    @Override
    public void process(RequestEvent evt) {

        SIPRequest request = (SIPRequest)evt.getRequest();
        try {
            InviteMessageInfo inviteInfo = decode(evt);

            // 查询请求是否来自上级平台\设备
            Platform platform = platformService.queryPlatformByServerGBId(inviteInfo.getRequesterId());
            if (platform == null) {
                inviteFromDeviceHandle(request, inviteInfo);
            } else {
                // 查询平台下是否有该通道
                CommonGBChannel channel= channelService.queryOneWithPlatform(platform.getId(), inviteInfo.getTargetChannelId());
                if (channel == null) {
                    log.info("[上级INVITE] 通道不存在，返回404: {}", inviteInfo.getTargetChannelId());
                    try {
                        // 通道不存在，发404，资源不存在
                        responseAck(request, Response.NOT_FOUND);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        log.error("[命令发送失败] invite 通道不存在: {}", e.getMessage());
                    }
                    return;
                }
                log.info("[上级Invite] 平台：{}， 通道：{}({}), 收流地址：{}:{}，收流方式：{}, 点播类型：{},  ssrc：{}",
                        platform.getName(), channel.getGbName(), channel.getGbDeviceId(), inviteInfo.getIp(),
                        inviteInfo.getPort(), inviteInfo.isTcp()?(inviteInfo.isTcpActive()?"TCP主动":"TCP被动"): "UDP",
                        inviteInfo.getSessionName(), inviteInfo.getSsrc());
                if(!userSetting.getUseCustomSsrcForParentInvite() && ObjectUtils.isEmpty(inviteInfo.getSsrc())) {
                    log.warn("[上级INVITE] 点播失败, 上级为携带SSRC, 并且本级未设置使用自定义ssrc");
                    // 通道存在，发100，TRYING
                    try {
                        responseAck(request, Response.BAD_REQUEST);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        log.error("[命令发送失败] 上级Invite TRYING: {}", e.getMessage());
                    }
                    return;
                }
                // 通道存在，发100，TRYING
                try {
                    responseAck(request, Response.TRYING);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] 上级Invite TRYING: {}", e.getMessage());
                }

                channelPlayService.start(channel, inviteInfo, platform, ((code, msg, streamInfo) -> {
                    if (code != InviteErrorCode.SUCCESS.getCode()) {
                        try {
                            responseAck(request, Response.BUSY_HERE , msg);
                        } catch (SipException | InvalidArgumentException | ParseException e) {
                            log.error("[命令发送失败] 上级Invite 点播失败: {}", e.getMessage());
                        }
                    }else {
                        // 点播成功， TODO 可以在此处检测cancel命令是否存在，存在则不发送
                        if (userSetting.getUseCustomSsrcForParentInvite()) {
                            // 上级平台点播时不使用上级平台指定的ssrc，使用自定义的ssrc，参考国标文档-点播外域设备媒体流SSRC处理方式
                            String ssrc = "Play".equalsIgnoreCase(inviteInfo.getSessionName())
                                        ? ssrcFactory.getPlaySsrc(streamInfo.getMediaServer().getId())
                                    : ssrcFactory.getPlayBackSsrc(streamInfo.getMediaServer().getId());
                            inviteInfo.setSsrc(ssrc);
                        }
                        // 构建sendRTP内容
                        SendRtpInfo sendRtpItem = sendRtpServerService.createSendRtpInfo(streamInfo.getMediaServer(),
                                inviteInfo.getIp(), inviteInfo.getPort(), inviteInfo.getSsrc(), platform.getServerGBId(),
                                streamInfo.getApp(), streamInfo.getStream(),
                                channel.getGbId(), inviteInfo.isTcp(), platform.isRtcp());
                        if (inviteInfo.isTcp() && inviteInfo.isTcpActive()) {
                            sendRtpItem.setTcpActive(true);
                        }
                        sendRtpItem.setStatus(1);
                        sendRtpItem.setCallId(inviteInfo.getCallId());
                        sendRtpItem.setPlayType("Play".equalsIgnoreCase(inviteInfo.getSessionName()) ? InviteStreamType.PLAY : InviteStreamType.PLAYBACK);
                        sendRtpItem.setServerId(streamInfo.getServerId());
                        sendRtpServerService.update(sendRtpItem);
                        String sdpIp = streamInfo.getMediaServer().getSdpIp();
                        if (!ObjectUtils.isEmpty(platform.getSendStreamIp())) {
                            sdpIp = platform.getSendStreamIp();
                        }
                        String content = createSendSdp(sendRtpItem, inviteInfo, sdpIp);
                        // 超时未收到Ack应该回复bye,当前等待时间为10秒
                        dynamicTask.startDelay(inviteInfo.getCallId(), () -> {
                            log.info("[Ack ] 等待超时, {}/{}", inviteInfo.getCallId(), channel.getGbDeviceId());
                            mediaServerService.releaseSsrc(streamInfo.getMediaServer().getId(), sendRtpItem.getSsrc());
                            // 回复bye
                            sendBye(platform, inviteInfo.getCallId());
                        }, 60 * 1000);
                        try {
                            responseSdpAck(request, content, platform);
                        } catch (SipException | InvalidArgumentException | ParseException e) {
                            log.error("[命令发送失败] 上级Invite 发送 200（SDP）: {}", e.getMessage());
                        }

                        // tcp主动模式，回复sdp后开启监听
                        if (sendRtpItem.isTcpActive()) {
                            MediaServer mediaServer = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                            try {
                                mediaServerService.startSendRtpPassive(mediaServer, sendRtpItem, 5);
                                DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(sendRtpItem.getChannelId());
                                if (deviceChannel != null) {
                                    redisCatchStorage.sendPlatformStartPlayMsg(sendRtpItem, deviceChannel, platform);
                                }
                            }catch (ControllerException e) {
                                log.warn("[上级Invite] tcp主动模式 发流失败", e);
                                sendBye(platform, inviteInfo.getCallId());
                            }
                        }
                    }
                }));
            }
        } catch (SdpException e) {
            // 参数不全， 发400，请求错误
            try {
                responseAck(request, Response.BAD_REQUEST);
            } catch (SipException | InvalidArgumentException | ParseException sendException) {
                log.error("[命令发送失败] invite BAD_REQUEST: {}", sendException.getMessage());
            }
        } catch (InviteDecodeException e) {
            try {
                responseAck(request, e.getCode(), e.getMsg());
            } catch (SipException | InvalidArgumentException | ParseException sendException) {
                log.error("[命令发送失败] invite BAD_REQUEST: {}", sendException.getMessage());
            }
        }catch (PlayException e) {
            try {
                responseAck(request, e.getCode(), e.getMsg());
            } catch (SipException | InvalidArgumentException | ParseException sendException) {
                log.error("[命令发送失败] invite 点播失败: {}", sendException.getMessage());
            }
        }
    }

    private InviteMessageInfo decode(RequestEvent evt) throws SdpException {

        InviteMessageInfo inviteInfo = new InviteMessageInfo();
        SIPRequest request = (SIPRequest)evt.getRequest();
        String[] channelIdArrayFromSub = SipUtils.getChannelIdFromRequest(request);

        // 解析sdp消息, 使用jainsip 自带的sdp解析方式
        String contentString = new String(request.getRawContent());
        Gb28181Sdp gb28181Sdp = SipUtils.parseSDP(contentString);
        SessionDescription sdp = gb28181Sdp.getBaseSdb();
        String sessionName = sdp.getSessionName().getValue();
        String channelIdFromSdp = null;
        if(StringUtils.equalsIgnoreCase("Playback", sessionName)){
            URIField uriField = (URIField)sdp.getURI();
            channelIdFromSdp = uriField.getURI().split(":")[0];
        }
        final String channelId = StringUtils.isNotBlank(channelIdFromSdp) ? channelIdFromSdp :
                (channelIdArrayFromSub != null? channelIdArrayFromSub[0]: null);
        String requesterId = SipUtils.getUserIdFromFromHeader(request);
        CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
        if (requesterId == null || channelId == null) {
            log.warn("[解析INVITE消息] 无法从请求中获取到来源id，返回400错误");
            throw new InviteDecodeException(Response.BAD_REQUEST, "request decode fail");
        }
        log.info("[INVITE] 来源ID: {}, callId: {}, 来自：{}：{}",
                requesterId, callIdHeader.getCallId(), request.getRemoteAddress(), request.getRemotePort());
        inviteInfo.setRequesterId(requesterId);
        inviteInfo.setTargetChannelId(channelId);
        if (channelIdArrayFromSub != null && channelIdArrayFromSub.length == 2) {
            inviteInfo.setSourceChannelId(channelIdArrayFromSub[1]);
        }
        inviteInfo.setSessionName(sessionName);
        inviteInfo.setSsrc(gb28181Sdp.getSsrc());
        inviteInfo.setCallId(callIdHeader.getCallId());

        // 如果是录像回放，则会存在录像的开始时间与结束时间
        Long startTime = null;
        Long stopTime = null;
        if (sdp.getTimeDescriptions(false) != null && !sdp.getTimeDescriptions(false).isEmpty()) {
            TimeDescriptionImpl timeDescription = (TimeDescriptionImpl) (sdp.getTimeDescriptions(false).get(0));
            TimeField startTimeFiled = (TimeField) timeDescription.getTime();
            startTime = startTimeFiled.getStartTime();
            stopTime = startTimeFiled.getStopTime();
        }
        //  获取支持的格式
        Vector mediaDescriptions = sdp.getMediaDescriptions(true);
        // 查看是否支持PS 负载96
        //String ip = null;
        int port = -1;
        boolean mediaTransmissionTCP = false;
        Boolean tcpActive = null;
        for (Object description : mediaDescriptions) {
            MediaDescription mediaDescription = (MediaDescription) description;
            Media media = mediaDescription.getMedia();

            Vector mediaFormats = media.getMediaFormats(false);
            if (mediaFormats.contains("96") || mediaFormats.contains("8")) {
                port = media.getMediaPort();
                //String mediaType = media.getMediaType();
                String protocol = media.getProtocol();

                // 区分TCP发流还是udp， 当前默认udp
                if ("TCP/RTP/AVP".equalsIgnoreCase(protocol)) {
                    String setup = mediaDescription.getAttribute("setup");
                    if (setup != null) {
                        mediaTransmissionTCP = true;
                        if ("active".equalsIgnoreCase(setup)) {
                            tcpActive = true;
                        } else if ("passive".equalsIgnoreCase(setup)) {
                            tcpActive = false;
                        }
                    }
                }
                break;
            }
        }
        if (port == -1) {
            log.info("[解析INVITE消息]  不支持的媒体格式，返回415");
            throw new InviteDecodeException(Response.UNSUPPORTED_MEDIA_TYPE, "unsupported media type");
        }
        inviteInfo.setTcp(mediaTransmissionTCP);
        inviteInfo.setTcpActive(tcpActive != null? tcpActive: false);
        inviteInfo.setStartTime(startTime);
        inviteInfo.setStopTime(stopTime);

        Vector sdpMediaDescriptions = sdp.getMediaDescriptions(true);
        MediaDescription mediaDescription = null;
        String downloadSpeed = "1";
        if (!sdpMediaDescriptions.isEmpty()) {
            mediaDescription = (MediaDescription) sdpMediaDescriptions.get(0);
        }
        if (mediaDescription != null) {
            downloadSpeed = mediaDescription.getAttribute("downloadspeed");
        }
        inviteInfo.setIp(sdp.getConnection().getAddress());
        inviteInfo.setPort(port);
        inviteInfo.setDownloadSpeed(downloadSpeed);

        return inviteInfo;

    }

    private String createSendSdp(SendRtpInfo sendRtpItem, InviteMessageInfo inviteInfo, String sdpIp) {
        StringBuilder content = new StringBuilder(200);
        content.append("v=0\r\n");
        content.append("o=" + inviteInfo.getTargetChannelId() + " 0 0 IN IP4 " + sdpIp + "\r\n");
        content.append("s=" + inviteInfo.getSessionName() + "\r\n");
        content.append("c=IN IP4 " + sdpIp + "\r\n");
        if ("Playback".equalsIgnoreCase(inviteInfo.getSessionName())) {
            content.append("t=" + inviteInfo.getStartTime() + " " + inviteInfo.getStopTime() + "\r\n");
        } else {
            content.append("t=0 0\r\n");
        }
        if (sendRtpItem.isTcp()) {
            content.append("m=video " + sendRtpItem.getLocalPort() + " TCP/RTP/AVP 96\r\n");
            if (!sendRtpItem.isTcpActive()) {
                content.append("a=setup:active\r\n");
            } else {
                content.append("a=setup:passive\r\n");
            }
        }else {
            content.append("m=video " + sendRtpItem.getLocalPort() + " RTP/AVP 96\r\n");
        }
        content.append("a=sendonly\r\n");
        content.append("a=rtpmap:96 PS/90000\r\n");
        content.append("y=" + sendRtpItem.getSsrc() + "\r\n");
        content.append("f=\r\n");
        return content.toString();
    }

    private void sendBye(Platform platform, String callId) {
        try {
            SendRtpInfo sendRtpItem = sendRtpServerService.queryByCallId(callId);
            if (sendRtpItem == null) {
                return;
            }
            CommonGBChannel channel = channelService.getOne(sendRtpItem.getChannelId());
            if (channel == null) {
                return;
            }
            cmderFroPlatform.streamByeCmd(platform, sendRtpItem, channel);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 上级Invite 发送BYE: {}", e.getMessage());
        }
    }

    public void inviteFromDeviceHandle(SIPRequest request, InviteMessageInfo inviteInfo) {

        if (inviteInfo.getSourceChannelId() == null) {
            log.warn("来自设备的Invite请求，无法从请求信息中确定请求来自的通道，已忽略，requesterId： {}", inviteInfo.getRequesterId());
            try {
                responseAck(request, Response.FORBIDDEN);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 来自设备的Invite请求，无法从请求信息中确定所属设备 FORBIDDEN: {}", e.getMessage());
            }
            return;
        }
        // 非上级平台请求，查询是否设备请求（通常为接收语音广播的设备）
        Device device = redisCatchStorage.getDevice(inviteInfo.getRequesterId());
        // 判断requesterId是设备还是通道
        if (device == null) {
            device = deviceService.getDeviceBySourceChannelDeviceId(inviteInfo.getRequesterId());
        }
        if (device == null) {
            // 检查channelID是否可用
            device = deviceService.getDeviceBySourceChannelDeviceId(inviteInfo.getSourceChannelId());
        }

        if (device == null) {
            log.warn("来自设备的Invite请求，无法从请求信息中确定所属设备，已忽略，requesterId： {}/{}", inviteInfo.getRequesterId(),
                    inviteInfo.getSourceChannelId());
            try {
                responseAck(request, Response.FORBIDDEN);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 来自设备的Invite请求，无法从请求信息中确定所属设备 FORBIDDEN: {}", e.getMessage());
            }
            return;
        }
        DeviceChannel deviceChannel = deviceChannelService.getOne(device.getDeviceId(), inviteInfo.getSourceChannelId());
        if (deviceChannel == null) {
            List<AudioBroadcastCatch> audioBroadcastCatchList = audioBroadcastManager.getByDeviceId(device.getDeviceId());
            if (audioBroadcastCatchList.isEmpty()) {
                log.warn("来自设备的Invite请求，无法从请求信息中确定所属通道，已忽略，requesterId： {}/{}", inviteInfo.getRequesterId(), inviteInfo.getSourceChannelId());
                try {
                    responseAck(request, Response.FORBIDDEN);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] 来自设备的Invite请求，无法从请求信息中确定所属设备 FORBIDDEN: {}", e.getMessage());
                }
                return;
            }else {
                deviceChannel = deviceChannelService.getOneForSourceById(audioBroadcastCatchList.get(0).getChannelId());
            }
        }
        AudioBroadcastCatch broadcastCatch = audioBroadcastManager.get(deviceChannel.getId());
        if (broadcastCatch == null) {
            log.warn("来自设备的Invite请求非语音广播，已忽略，requesterId： {}/{}", inviteInfo.getRequesterId(), inviteInfo.getSourceChannelId());
            try {
                responseAck(request, Response.FORBIDDEN);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 来自设备的Invite请求非语音广播 FORBIDDEN: {}", e.getMessage());
            }
            return;
        }
        log.info("收到设备" + inviteInfo.getRequesterId() + "的语音广播Invite请求");
        String key = VideoManagerConstants.BROADCAST_WAITE_INVITE + device.getDeviceId();
        if (!SipUtils.isFrontEnd(device.getDeviceId())) {
            key += broadcastCatch.getChannelId();
        }
        dynamicTask.stop(key);
        try {
            responseAck(request, Response.TRYING);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] invite BAD_REQUEST: {}", e.getMessage());
            playService.stopAudioBroadcast(device, deviceChannel);
            return;
        }
        String contentString = new String(request.getRawContent());

        try {
            Gb28181Sdp gb28181Sdp = SipUtils.parseSDP(contentString);
            SessionDescription sdp = gb28181Sdp.getBaseSdb();
            //  获取支持的格式
            Vector mediaDescriptions = sdp.getMediaDescriptions(true);

            // 查看是否支持PS 负载96
            int port = -1;
            boolean mediaTransmissionTCP = false;
            Boolean tcpActive = null;
            for (int i = 0; i < mediaDescriptions.size(); i++) {
                MediaDescription mediaDescription = (MediaDescription) mediaDescriptions.get(i);
                Media media = mediaDescription.getMedia();

                Vector mediaFormats = media.getMediaFormats(false);
//                    if (mediaFormats.contains("8")) {
                port = media.getMediaPort();
                String protocol = media.getProtocol();
                // 区分TCP发流还是udp， 当前默认udp
                if ("TCP/RTP/AVP".equals(protocol)) {
                    String setup = mediaDescription.getAttribute("setup");
                    if (setup != null) {
                        mediaTransmissionTCP = true;
                        if ("active".equals(setup)) {
                            tcpActive = true;
                        } else if ("passive".equals(setup)) {
                            tcpActive = false;
                        }
                    }
                }
                break;
//                    }
            }
            if (port == -1) {
                log.info("不支持的媒体格式，返回415");
                // 回复不支持的格式
                try {
                    responseAck(request, Response.UNSUPPORTED_MEDIA_TYPE); // 不支持的格式，发415
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] invite 不支持的媒体格式: {}", e.getMessage());
                    playService.stopAudioBroadcast(device, deviceChannel);
                    return;
                }
                return;
            }
            String addressStr = sdp.getOrigin().getAddress();
            log.info("设备{}请求语音流，地址：{}:{}，ssrc：{}, {}", inviteInfo.getRequesterId(), addressStr, port, gb28181Sdp.getSsrc(),
                    mediaTransmissionTCP ? (tcpActive ? "TCP主动" : "TCP被动") : "UDP");

            MediaServer mediaServerItem = broadcastCatch.getMediaServerItem();
            if (mediaServerItem == null) {
                log.warn("未找到语音喊话使用的zlm");
                try {
                    responseAck(request, Response.BUSY_HERE);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] invite 未找到可用的zlm: {}", e.getMessage());
                    playService.stopAudioBroadcast(device, deviceChannel);
                }
                return;
            }
            log.info("设备{}请求语音流， 收流地址：{}:{}，ssrc：{}, {}, 对讲方式：{}", inviteInfo.getRequesterId(), addressStr, port, gb28181Sdp.getSsrc(),
                    mediaTransmissionTCP ? (tcpActive ? "TCP主动" : "TCP被动") : "UDP", sdp.getSessionName().getValue());
            CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);

            SendRtpInfo sendRtpItem = sendRtpServerService.createSendRtpInfo(mediaServerItem, addressStr, port, gb28181Sdp.getSsrc(), inviteInfo.getRequesterId(),
                    device.getDeviceId(), deviceChannel.getId(),
                    mediaTransmissionTCP, false);

            if (sendRtpItem == null) {
                log.warn("服务器端口资源不足");
                try {
                    responseAck(request, Response.BUSY_HERE);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] invite 服务器端口资源不足: {}", e.getMessage());
                    playService.stopAudioBroadcast(device, deviceChannel);
                    return;
                }
                return;
            }

            sendRtpItem.setPlayType(InviteStreamType.BROADCAST);
            sendRtpItem.setCallId(callIdHeader.getCallId());
            sendRtpItem.setStatus(1);
            sendRtpItem.setApp(broadcastCatch.getApp());
            sendRtpItem.setStream(broadcastCatch.getStream());
            sendRtpItem.setPt(8);
            sendRtpItem.setUsePs(false);
            sendRtpItem.setRtcp(false);
            sendRtpItem.setOnlyAudio(true);
            sendRtpItem.setTcp(mediaTransmissionTCP);
            if (tcpActive != null) {
                sendRtpItem.setTcpActive(tcpActive);
            }

            sendRtpServerService.update(sendRtpItem);

            Boolean streamReady = mediaServerService.isStreamReady(mediaServerItem, broadcastCatch.getApp(), broadcastCatch.getStream());
            if (streamReady) {
                sendOk(device, deviceChannel, sendRtpItem, sdp, request, mediaServerItem, mediaTransmissionTCP, gb28181Sdp.getSsrc());
            } else {
                log.warn("[语音通话]， 未发现待推送的流,app={},stream={}", broadcastCatch.getApp(), broadcastCatch.getStream());
                try {
                    responseAck(request, Response.GONE);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] 语音通话 回复410失败， {}", e.getMessage());
                    return;
                }
                playService.stopAudioBroadcast(device, deviceChannel);
            }
        } catch (SdpException e) {
            log.error("[SDP解析异常]", e);
            playService.stopAudioBroadcast(device, deviceChannel);
        }
    }

    SIPResponse sendOk(Device device, DeviceChannel channel,  SendRtpInfo sendRtpItem, SessionDescription sdp, SIPRequest request, MediaServer mediaServerItem, boolean mediaTransmissionTCP, String ssrc) {
        SIPResponse sipResponse = null;
        try {
            sendRtpItem.setStatus(2);
            sendRtpServerService.update(sendRtpItem);
            StringBuffer content = new StringBuffer(200);
            content.append("v=0\r\n");
            content.append("o=" + config.getId() + " " + sdp.getOrigin().getSessionId() + " " + sdp.getOrigin().getSessionVersion() + " IN IP4 " + mediaServerItem.getSdpIp() + "\r\n");
            content.append("s=Play\r\n");
            content.append("c=IN IP4 " + mediaServerItem.getSdpIp() + "\r\n");
            content.append("t=0 0\r\n");

            if (mediaTransmissionTCP) {
                content.append("m=audio " + sendRtpItem.getLocalPort() + " TCP/RTP/AVP 8\r\n");
            } else {
                content.append("m=audio " + sendRtpItem.getLocalPort() + " RTP/AVP 8\r\n");
            }

            content.append("a=rtpmap:8 PCMA/8000/1\r\n");

            content.append("a=sendonly\r\n");
            if (sendRtpItem.isTcp()) {
                content.append("a=connection:new\r\n");
                if (!sendRtpItem.isTcpActive()) {
                    content.append("a=setup:active\r\n");
                } else {
                    content.append("a=setup:passive\r\n");
                }
            }
            content.append("y=" + ssrc + "\r\n");
            content.append("f=v/////a/1/8/1\r\n");

            Platform parentPlatform = new Platform();
            parentPlatform.setServerIp(device.getIp());
            parentPlatform.setServerPort(device.getPort());
            parentPlatform.setServerGBId(device.getDeviceId());

            sipResponse = responseSdpAck(request, content.toString(), parentPlatform);

            AudioBroadcastCatch audioBroadcastCatch = audioBroadcastManager.get(sendRtpItem.getChannelId());

            audioBroadcastCatch.setStatus(AudioBroadcastCatchStatus.Ok);
            audioBroadcastCatch.setSipTransactionInfoByRequest(sipResponse);
            audioBroadcastManager.update(audioBroadcastCatch);
            SsrcTransaction ssrcTransaction = SsrcTransaction.buildForDevice(device.getDeviceId(), sendRtpItem.getChannelId(),
                    request.getCallIdHeader().getCallId(), sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getSsrc(), sendRtpItem.getMediaServerId(), sipResponse, InviteSessionType.BROADCAST);
            sessionManager.put(ssrcTransaction);
            // 开启发流，大华在收到200OK后就会开始建立连接
            if (sendRtpItem.isTcpActive() || !device.isBroadcastPushAfterAck()) {
                if (sendRtpItem.isTcpActive()) {
                    log.info("[语音喊话] 监听端口等待设备连接后推流");
                }else {
                    log.info("[语音喊话] 回复200OK后发现 BroadcastPushAfterAck为False，现在开始推流");
                }

                playService.startPushStream(sendRtpItem, channel, sipResponse, parentPlatform, request.getCallIdHeader());
            }

        } catch (SipException | InvalidArgumentException | ParseException | SdpParseException e) {
            log.error("[命令发送失败] 语音喊话 回复200OK（SDP）: {}", e.getMessage());
        }
        return sipResponse;
    }
}
