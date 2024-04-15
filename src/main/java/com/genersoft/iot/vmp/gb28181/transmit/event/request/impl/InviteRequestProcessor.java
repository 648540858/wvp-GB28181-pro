package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.SIPSender;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.media.zlm.SendRtpPortManager;
import com.genersoft.iot.vmp.service.redisMsg.RedisPlatformPushStreamOnlineLister;
import com.genersoft.iot.vmp.media.zlm.ZLMServerFactory;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.*;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;
import com.genersoft.iot.vmp.service.*;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.service.bean.MessageForPushChannel;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.service.redisMsg.RedisPushStreamResponseListener;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import gov.nist.javax.sdp.TimeDescriptionImpl;
import gov.nist.javax.sdp.fields.TimeField;
import gov.nist.javax.sdp.fields.URIField;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

/**
 * SIP命令类型： INVITE请求
 */
@SuppressWarnings("rawtypes")
@Component
public class InviteRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

    private final static Logger logger = LoggerFactory.getLogger(InviteRequestProcessor.class);

    private final String method = "INVITE";

    @Autowired
    private ISIPCommanderForPlatform cmderFroPlatform;

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private IStreamPushService streamPushService;

    @Autowired
    private IStreamProxyService streamProxyService;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IInviteStreamService inviteStreamService;

    @Autowired
    private SSRCFactory ssrcFactory;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private RedisPushStreamResponseListener redisPushStreamResponseListener;

    @Autowired
    private IPlayService playService;

    @Autowired
    private SIPSender sipSender;

    @Autowired
    private AudioBroadcastManager audioBroadcastManager;

    @Autowired
    private ZLMServerFactory zlmServerFactory;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private ZlmHttpHookSubscribe zlmHttpHookSubscribe;

    @Autowired
    private SIPProcessorObserver sipProcessorObserver;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private RedisPlatformPushStreamOnlineLister mediaListManager;

    @Autowired
    private SipConfig config;

    @Autowired
    private VideoStreamSessionManager streamSession;

    @Autowired
    private SendRtpPortManager sendRtpPortManager;


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
        //  Invite Request消息实现，此消息一般为级联消息，上级给下级发送请求视频指令
        try {
            SIPRequest request = (SIPRequest)evt.getRequest();
            String channelIdFromSub = SipUtils.getChannelIdFromRequest(request);

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
            final String channelId = StringUtils.isNotBlank(channelIdFromSdp) ? channelIdFromSdp : channelIdFromSub;

            String requesterId = SipUtils.getUserIdFromFromHeader(request);
            CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
            if (requesterId == null || channelId == null) {
                logger.info("无法从请求中获取到平台id，返回400");
                // 参数不全， 发400，请求错误
                try {
                    responseAck(request, Response.BAD_REQUEST);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    logger.error("[命令发送失败] invite BAD_REQUEST: {}", e.getMessage());
                }
                return;
            }

            logger.info("[INVITE] requesterId: {}, callId: {}, 来自：{}：{}",
                    requesterId, callIdHeader.getCallId(), request.getRemoteAddress(), request.getRemotePort());

            // 查询请求是否来自上级平台\设备
            ParentPlatform platform = storager.queryParentPlatByServerGBId(requesterId);
            if (platform == null) {
                inviteFromDeviceHandle(request, requesterId, channelId);

            } else {
                // 查询平台下是否有该通道
                DeviceChannel channel = storager.queryChannelInParentPlatform(requesterId, channelId);
                GbStream gbStream = storager.queryStreamInParentPlatform(requesterId, channelId);
                PlatformCatalog catalog = storager.getCatalog(requesterId, channelId);

                MediaServerItem mediaServerItem = null;
                StreamPushItem streamPushItem = null;
                StreamProxyItem proxyByAppAndStream = null;
                // 不是通道可能是直播流
                if (channel != null && gbStream == null) {
                    // 通道存在，发100，TRYING
                    try {
                        responseAck(request, Response.TRYING);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        logger.error("[命令发送失败] invite TRYING: {}", e.getMessage());
                    }
                } else if (channel == null && gbStream != null) {

                    String mediaServerId = gbStream.getMediaServerId();
                    mediaServerItem = mediaServerService.getOne(mediaServerId);
                    if (mediaServerItem == null) {
                        if ("proxy".equals(gbStream.getStreamType())) {
                            logger.info("[ app={}, stream={} ]找不到zlm {}，返回410", gbStream.getApp(), gbStream.getStream(), mediaServerId);
                            try {
                                responseAck(request, Response.GONE);
                            } catch (SipException | InvalidArgumentException | ParseException e) {
                                logger.error("[命令发送失败] invite GONE: {}", e.getMessage());
                            }
                            return;
                        } else {
                            streamPushItem = streamPushService.getPush(gbStream.getApp(), gbStream.getStream());
                            if (streamPushItem != null) {
                                mediaServerItem = mediaServerService.getOne(streamPushItem.getMediaServerId());
                            }
                            if (mediaServerItem == null) {
                                mediaServerItem = mediaServerService.getDefaultMediaServer();
                            }
                        }
                    } else {
                        if ("push".equals(gbStream.getStreamType())) {
                            streamPushItem = streamPushService.getPush(gbStream.getApp(), gbStream.getStream());
                            if (streamPushItem == null) {
                                logger.info("[ app={}, stream={} ]找不到zlm {}，返回410", gbStream.getApp(), gbStream.getStream(), mediaServerId);
                                try {
                                    responseAck(request, Response.GONE);
                                } catch (SipException | InvalidArgumentException | ParseException e) {
                                    logger.error("[命令发送失败] invite GONE: {}", e.getMessage());
                                }
                                return;
                            }
                        } else if ("proxy".equals(gbStream.getStreamType())) {
                            proxyByAppAndStream = streamProxyService.getStreamProxyByAppAndStream(gbStream.getApp(), gbStream.getStream());
                            if (proxyByAppAndStream == null) {
                                logger.info("[ app={}, stream={} ]找不到zlm {}，返回410", gbStream.getApp(), gbStream.getStream(), mediaServerId);
                                try {
                                    responseAck(request, Response.GONE);
                                } catch (SipException | InvalidArgumentException | ParseException e) {
                                    logger.error("[命令发送失败] invite GONE: {}", e.getMessage());
                                }
                                return;
                            }
                        }
                    }
                    try {
                        responseAck(request, Response.CALL_IS_BEING_FORWARDED);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        logger.error("[命令发送失败] invite CALL_IS_BEING_FORWARDED: {}", e.getMessage());
                    }
                } else if (catalog != null) {
                    try {
                        // 目录不支持点播
                        responseAck(request, Response.BAD_REQUEST, "catalog channel can not play");
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        logger.error("[命令发送失败] invite 目录不支持点播: {}", e.getMessage());
                    }
                    return;
                } else {
                    logger.info("通道不存在，返回404: {}", channelId);
                    try {
                        // 通道不存在，发404，资源不存在
                        responseAck(request, Response.NOT_FOUND);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        logger.error("[命令发送失败] invite 通道不存在: {}", e.getMessage());
                    }
                    return;
                }

                Long startTime = null;
                Long stopTime = null;
                Instant start = null;
                Instant end = null;
                if (sdp.getTimeDescriptions(false) != null && sdp.getTimeDescriptions(false).size() > 0) {
                    TimeDescriptionImpl timeDescription = (TimeDescriptionImpl) (sdp.getTimeDescriptions(false).get(0));
                    TimeField startTimeFiled = (TimeField) timeDescription.getTime();
                    startTime = startTimeFiled.getStartTime();
                    stopTime = startTimeFiled.getStopTime();

                    start = Instant.ofEpochSecond(startTime);
                    end = Instant.ofEpochSecond(stopTime);
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
                    if (mediaFormats.contains("96")) {
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
                    logger.info("不支持的媒体格式，返回415");
                    // 回复不支持的格式
                    try {
                        // 不支持的格式，发415
                        responseAck(request, Response.UNSUPPORTED_MEDIA_TYPE);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        logger.error("[命令发送失败] invite 不支持的格式: {}", e.getMessage());
                    }
                    return;
                }
                String username = sdp.getOrigin().getUsername();
                String addressStr = sdp.getConnection().getAddress();


                Device device = null;
                // 通过 channel 和 gbStream 是否为null 值判断来源是直播流合适国标
                if (channel != null) {
                    device = storager.queryVideoDeviceByPlatformIdAndChannelId(requesterId, channelId);
                    if (device == null) {
                        logger.warn("点播平台{}的通道{}时未找到设备信息", requesterId, channel);
                        try {
                            responseAck(request, Response.SERVER_INTERNAL_ERROR);
                        } catch (SipException | InvalidArgumentException | ParseException e) {
                            logger.error("[命令发送失败] invite 未找到设备信息: {}", e.getMessage());
                        }
                        return;
                    }
                    mediaServerItem = playService.getNewMediaServerItem(device);
                    if (mediaServerItem == null) {
                        logger.warn("未找到可用的zlm");
                        try {
                            responseAck(request, Response.BUSY_HERE);
                        } catch (SipException | InvalidArgumentException | ParseException e) {
                            logger.error("[命令发送失败] invite BUSY_HERE: {}", e.getMessage());
                        }
                        return;
                    }

                    String ssrc;
                    if (userSetting.getUseCustomSsrcForParentInvite() || gb28181Sdp.getSsrc() == null) {
                        // 上级平台点播时不使用上级平台指定的ssrc，使用自定义的ssrc，参考国标文档-点播外域设备媒体流SSRC处理方式
                        ssrc = "Play".equalsIgnoreCase(sessionName) ? ssrcFactory.getPlaySsrc(mediaServerItem.getId()) : ssrcFactory.getPlayBackSsrc(mediaServerItem.getId());
                    }else {
                        ssrc = gb28181Sdp.getSsrc();
                    }
                    String streamTypeStr = null;
                    if (mediaTransmissionTCP) {
                        if (tcpActive) {
                            streamTypeStr = "TCP-ACTIVE";
                        } else {
                            streamTypeStr = "TCP-PASSIVE";
                        }
                    } else {
                        streamTypeStr = "UDP";
                    }
                    logger.info("[上级Invite] {}, 平台：{}， 通道：{}, 收流地址：{}:{}，收流方式：{}, ssrc：{}", sessionName, username, channelId, addressStr, port, streamTypeStr, ssrc);
                    SendRtpItem sendRtpItem = zlmServerFactory.createSendRtpItem(mediaServerItem, addressStr, port, ssrc, requesterId,
                            device.getDeviceId(), channelId, mediaTransmissionTCP, platform.isRtcp());

                    if (tcpActive != null) {
                        sendRtpItem.setTcpActive(tcpActive);
                    }
                    if (sendRtpItem == null) {
                        logger.warn("服务器端口资源不足");
                        try {
                            responseAck(request, Response.BUSY_HERE);
                        } catch (SipException | InvalidArgumentException | ParseException e) {
                            logger.error("[命令发送失败] invite 服务器端口资源不足: {}", e.getMessage());
                        }
                        return;
                    }
                    sendRtpItem.setCallId(callIdHeader.getCallId());
                    sendRtpItem.setPlayType("Play".equalsIgnoreCase(sessionName) ? InviteStreamType.PLAY : InviteStreamType.PLAYBACK);

                    Long finalStartTime = startTime;
                    Long finalStopTime = stopTime;
                    ErrorCallback<Object> hookEvent = (code, msg, data) -> {
                        StreamInfo streamInfo = (StreamInfo)data;
                        MediaServerItem mediaServerItemInUSe = mediaServerService.getOne(streamInfo.getMediaServerId());
                        logger.info("[上级Invite]下级已经开始推流。 回复200OK(SDP)， {}/{}", streamInfo.getApp(), streamInfo.getStream());
                        //     * 0 等待设备推流上来
                        //     * 1 下级已经推流，等待上级平台回复ack
                        //     * 2 推流中
                        sendRtpItem.setStatus(1);
                        redisCatchStorage.updateSendRTPSever(sendRtpItem);
                        String sdpIp = mediaServerItemInUSe.getSdpIp();
                        if (!ObjectUtils.isEmpty(platform.getSendStreamIp())) {
                            sdpIp = platform.getSendStreamIp();
                        }
                        StringBuffer content = new StringBuffer(200);
                        content.append("v=0\r\n");
                        content.append("o=" + channelId + " 0 0 IN IP4 " + sdpIp + "\r\n");
                        content.append("s=" + sessionName + "\r\n");
                        content.append("c=IN IP4 " + sdpIp + "\r\n");
                        if ("Playback".equalsIgnoreCase(sessionName)) {
                            content.append("t=" + finalStartTime + " " + finalStopTime + "\r\n");
                        } else {
                            content.append("t=0 0\r\n");
                        }
                        int localPort = sendRtpItem.getLocalPort();
                        if (localPort == 0) {
                            // 非严格模式端口不统一, 增加兼容性，修改为一个不为0的端口
                            localPort = new Random().nextInt(65535) + 1;
                        }
                        if (sendRtpItem.isTcp()) {
                            content.append("m=video " + localPort + " TCP/RTP/AVP 96\r\n");
                            if (!sendRtpItem.isTcpActive()) {
                                content.append("a=setup:active\r\n");
                            } else {
                                content.append("a=setup:passive\r\n");
                            }
                        }else {
                            content.append("m=video " + localPort + " RTP/AVP 96\r\n");
                        }
                        content.append("a=sendonly\r\n");
                        content.append("a=rtpmap:96 PS/90000\r\n");
                        content.append("y=" + sendRtpItem.getSsrc() + "\r\n");
                        content.append("f=\r\n");


                        try {
                            // 超时未收到Ack应该回复bye,当前等待时间为10秒
                            dynamicTask.startDelay(callIdHeader.getCallId(), () -> {
                                logger.info("Ack 等待超时");
                                mediaServerService.releaseSsrc(mediaServerItemInUSe.getId(), sendRtpItem.getSsrc());
                                // 回复bye
                                try {
                                    cmderFroPlatform.streamByeCmd(platform, callIdHeader.getCallId());
                                } catch (SipException | InvalidArgumentException | ParseException e) {
                                    logger.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
                                }
                            }, 60 * 1000);
                            responseSdpAck(request, content.toString(), platform);
                            // tcp主动模式，回复sdp后开启监听
                            if (sendRtpItem.isTcpActive()) {
                                MediaServerItem mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                                Map<String, Object> param = new HashMap<>(12);
                                param.put("vhost","__defaultVhost__");
                                param.put("app",sendRtpItem.getApp());
                                param.put("stream",sendRtpItem.getStream());
                                param.put("ssrc", sendRtpItem.getSsrc());
                                if (!sendRtpItem.isTcpActive()) {
                                    param.put("dst_url",sendRtpItem.getIp());
                                    param.put("dst_port", sendRtpItem.getPort());
                                }
                                String is_Udp = sendRtpItem.isTcp() ? "0" : "1";
                                param.put("is_udp", is_Udp);
                                param.put("src_port", localPort);
                                param.put("pt", sendRtpItem.getPt());
                                param.put("use_ps", sendRtpItem.isUsePs() ? "1" : "0");
                                param.put("only_audio", sendRtpItem.isOnlyAudio() ? "1" : "0");
                                if (!sendRtpItem.isTcp()) {
                                    // 开启rtcp保活
                                    param.put("udp_rtcp_timeout", sendRtpItem.isRtcp()? "1":"0");
                                }
                                JSONObject startSendRtpStreamResult = zlmServerFactory.startSendRtpPassive(mediaInfo, param);
                                if (startSendRtpStreamResult != null) {
                                    startSendRtpStreamHand(evt, sendRtpItem, null, startSendRtpStreamResult, param, callIdHeader);
                                }
                            }
                        } catch (SipException | InvalidArgumentException | ParseException e) {
                            logger.error("[命令发送失败] 国标级联 回复SdpAck", e);
                        }
                    };
                    ErrorCallback<Object> errorEvent = ((statusCode, msg, data) -> {
                        // 未知错误。直接转发设备点播的错误
                        try {
                            if (statusCode > 0) {
                                Response response = getMessageFactory().createResponse(statusCode, evt.getRequest());
                                sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
                            }
                        } catch (ParseException | SipException e) {
                            logger.error("未处理的异常 ", e);
                        }
                    });
                    sendRtpItem.setApp("rtp");
                    if ("Playback".equalsIgnoreCase(sessionName)) {
                        sendRtpItem.setPlayType(InviteStreamType.PLAYBACK);
                        String startTimeStr = DateUtil.urlFormatter.format(start);
                        String endTimeStr = DateUtil.urlFormatter.format(end);
                        String stream = device.getDeviceId() + "_" + channelId + "_" + startTimeStr + "_" + endTimeStr;
                        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(mediaServerItem, stream, null, device.isSsrcCheck(), true, 0,false, false, device.getStreamModeForParam());
                        sendRtpItem.setStream(stream);
                        // 写入redis， 超时时回复
                        redisCatchStorage.updateSendRTPSever(sendRtpItem);
                        playService.playBack(mediaServerItem, ssrcInfo, device.getDeviceId(), channelId, DateUtil.formatter.format(start),
                                DateUtil.formatter.format(end),
                                (code, msg, data) -> {
                                    if (code == InviteErrorCode.SUCCESS.getCode()) {
                                        hookEvent.run(code, msg, data);
                                    } else if (code == InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getCode() || code == InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getCode()) {
                                        logger.info("[录像回放]超时, 用户：{}， 通道：{}", username, channelId);
                                        redisCatchStorage.deleteSendRTPServer(platform.getServerGBId(), channelId, callIdHeader.getCallId(), null);
                                        errorEvent.run(code, msg, data);
                                    } else {
                                        errorEvent.run(code, msg, data);
                                    }
                                });
                    } else if ("Download".equalsIgnoreCase(sessionName)) {
                        // 获取指定的下载速度
                        Vector sdpMediaDescriptions = sdp.getMediaDescriptions(true);
                        MediaDescription mediaDescription = null;
                        String downloadSpeed = "1";
                        if (sdpMediaDescriptions.size() > 0) {
                            mediaDescription = (MediaDescription) sdpMediaDescriptions.get(0);
                        }
                        if (mediaDescription != null) {
                            downloadSpeed = mediaDescription.getAttribute("downloadspeed");
                        }

                        sendRtpItem.setPlayType(InviteStreamType.DOWNLOAD);
                        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(mediaServerItem, null, null, device.isSsrcCheck(), true, 0, false, false, device.getStreamModeForParam());
                        sendRtpItem.setStream(ssrcInfo.getStream());
                        // 写入redis， 超时时回复
                        redisCatchStorage.updateSendRTPSever(sendRtpItem);
                        playService.download(mediaServerItem, ssrcInfo, device.getDeviceId(), channelId, DateUtil.formatter.format(start),
                                DateUtil.formatter.format(end), Integer.parseInt(downloadSpeed),
                                (code, msg, data) -> {
                                    if (code == InviteErrorCode.SUCCESS.getCode()) {
                                        hookEvent.run(code, msg, data);
                                    } else if (code == InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getCode() || code == InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getCode()) {
                                        logger.info("[录像下载]超时, 用户：{}， 通道：{}", username, channelId);
                                        redisCatchStorage.deleteSendRTPServer(platform.getServerGBId(), channelId, callIdHeader.getCallId(), null);
                                        errorEvent.run(code, msg, data);
                                    } else {
                                        errorEvent.run(code, msg, data);
                                    }
                                });
                    } else {
                        sendRtpItem.setPlayType(InviteStreamType.PLAY);
                        String streamId = String.format("%s_%s", device.getDeviceId(), channelId);
                        sendRtpItem.setStream(streamId);
                        redisCatchStorage.updateSendRTPSever(sendRtpItem);
                        SSRCInfo ssrcInfo = playService.play(mediaServerItem, device.getDeviceId(), channelId, ssrc, ((code, msg, data) -> {
                            if (code == InviteErrorCode.SUCCESS.getCode()) {
                                hookEvent.run(code, msg, data);
                            } else if (code == InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getCode() || code == InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getCode()) {
                                logger.info("[上级点播]超时, 用户：{}， 通道：{}", username, channelId);
                                redisCatchStorage.deleteSendRTPServer(platform.getServerGBId(), channelId, callIdHeader.getCallId(), null);
                                errorEvent.run(code, msg, data);
                            } else {
                                errorEvent.run(code, msg, data);
                            }
                        }));
                        sendRtpItem.setSsrc(ssrcInfo.getSsrc());
                        redisCatchStorage.updateSendRTPSever(sendRtpItem);

                    }
                } else if (gbStream != null) {
                    SendRtpItem sendRtpItem = new SendRtpItem();
                    if (!userSetting.getUseCustomSsrcForParentInvite() && gb28181Sdp.getSsrc() != null) {
                        sendRtpItem.setSsrc(gb28181Sdp.getSsrc());
                    }

                    if (tcpActive != null) {
                        sendRtpItem.setTcpActive(tcpActive);
                    }
                    sendRtpItem.setTcp(mediaTransmissionTCP);
                    sendRtpItem.setRtcp(platform.isRtcp());
                    sendRtpItem.setPlatformName(platform.getName());
                    sendRtpItem.setPlatformId(platform.getServerGBId());
                    sendRtpItem.setMediaServerId(mediaServerItem.getId());
                    sendRtpItem.setChannelId(channelId);
                    sendRtpItem.setIp(addressStr);
                    sendRtpItem.setPort(port);
                    sendRtpItem.setUsePs(true);
                    sendRtpItem.setApp(gbStream.getApp());
                    sendRtpItem.setStream(gbStream.getStream());
                    sendRtpItem.setCallId(callIdHeader.getCallId());
                    sendRtpItem.setFromTag(request.getFromTag());
                    sendRtpItem.setOnlyAudio(false);
                    sendRtpItem.setStatus(0);
                    sendRtpItem.setSessionName(sessionName);

                    if ("push".equals(gbStream.getStreamType())) {
                        if (streamPushItem != null) {
                            // 从redis查询是否正在接收这个推流
                            OnStreamChangedHookParam pushListItem = redisCatchStorage.getPushListItem(gbStream.getApp(), gbStream.getStream());

                            if (pushListItem != null) {
                                sendRtpItem.setServerId(pushListItem.getSeverId());
                                StreamPushItem transform = streamPushService.transform(pushListItem);
                                transform.setSelf(userSetting.getServerId().equals(pushListItem.getSeverId()));
                                // 推流状态
                                sendPushStream(sendRtpItem, mediaServerItem, platform, request);
                            }else {
                                if (!platform.isStartOfflinePush()) {
                                    // 平台设置中关闭了拉起离线的推流则直接回复
                                    try {
                                        logger.info("[上级点播] 失败，推流设备未推流，channel: {}, app: {}, stream: {}", sendRtpItem.getChannelId(), sendRtpItem.getApp(), sendRtpItem.getStream());
                                        responseAck(request, Response.TEMPORARILY_UNAVAILABLE, "channel stream not pushing");
                                    } catch (SipException | InvalidArgumentException | ParseException e) {
                                        logger.error("[命令发送失败] invite 通道未推流: {}", e.getMessage());
                                    }
                                    return;
                                }
                                notifyPushStreamOnline(sendRtpItem, mediaServerItem, platform, request);
                            }
                        }
                    } else if ("proxy".equals(gbStream.getStreamType())) {
                        if (null != proxyByAppAndStream) {
                            if (sendRtpItem.getSsrc() == null) {
                                // 上级平台点播时不使用上级平台指定的ssrc，使用自定义的ssrc，参考国标文档-点播外域设备媒体流SSRC处理方式
                                String ssrc = "Play".equalsIgnoreCase(sessionName) ? ssrcFactory.getPlaySsrc(mediaServerItem.getId()) : ssrcFactory.getPlayBackSsrc(mediaServerItem.getId());
                                sendRtpItem.setSsrc(ssrc);
                            }
                            if (proxyByAppAndStream.isStatus()) {
                                sendProxyStream(sendRtpItem, mediaServerItem, platform, request);
                            } else {
                                //开启代理拉流
                                notifyProxyStreamOnline(sendRtpItem, mediaServerItem, platform, request);
                            }
                        }
                    }
                }
            }
        } catch (SdpParseException e) {
            logger.error("sdp解析错误", e);
        } catch (SdpException e) {
            logger.error("未处理的异常 ", e);
        }
    }

    private void startSendRtpStreamHand(RequestEvent evt, SendRtpItem sendRtpItem, ParentPlatform parentPlatform,
                                        JSONObject jsonObject, Map<String, Object> param, CallIdHeader callIdHeader) {
        if (jsonObject == null) {
            logger.error("下级TCP被动启动监听失败: 请检查ZLM服务");
        } else if (jsonObject.getInteger("code") == 0) {
            logger.info("调用ZLM-TCP被动推流接口, 结果： {}",  jsonObject);
            logger.info("启动监听TCP被动推流成功[ {}/{} ]，{}->{}:{}, " ,param.get("app"), param.get("stream"), jsonObject.getString("local_port"), param.get("dst_url"), param.get("dst_port"));
        } else {
            logger.error("启动监听TCP被动推流失败: {}, 参数：{}",jsonObject.getString("msg"), JSON.toJSONString(param));
        }
    }

    /**
     * 安排推流
     */
    private void sendProxyStream(SendRtpItem sendRtpItem, MediaServerItem mediaServerItem, ParentPlatform platform, SIPRequest request) {
            Boolean streamReady = zlmServerFactory.isStreamReady(mediaServerItem, sendRtpItem.getApp(), sendRtpItem.getStream());
            if (streamReady != null && streamReady) {
                // 自平台内容
                int localPort = sendRtpPortManager.getNextPort(mediaServerItem);
                if (localPort == 0) {
                    logger.warn("服务器端口资源不足");
                    try {
                        responseAck(request, Response.BUSY_HERE);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        logger.error("[命令发送失败] invite 服务器端口资源不足: {}", e.getMessage());
                    }
                    return;
                }
            sendRtpItem.setPlayType(InviteStreamType.PROXY);
            // 写入redis， 超时时回复
            sendRtpItem.setStatus(1);
            sendRtpItem.setLocalIp(mediaServerItem.getSdpIp());

            SIPResponse response = sendStreamAck(request, sendRtpItem, platform);
            if (response != null) {
                sendRtpItem.setToTag(response.getToTag());
            }
            redisCatchStorage.updateSendRTPSever(sendRtpItem);
        }
    }

    private void sendPushStream(SendRtpItem sendRtpItem, MediaServerItem mediaServerItem, ParentPlatform platform, SIPRequest request) {
        // 推流
        if (sendRtpItem.getServerId().equals(userSetting.getServerId())) {
            Boolean streamReady = zlmServerFactory.isStreamReady(mediaServerItem, sendRtpItem.getApp(), sendRtpItem.getStream());
            if (streamReady != null && streamReady) {
                // 自平台内容
                int localPort = sendRtpPortManager.getNextPort(mediaServerItem);
                if (localPort == 0) {
                    logger.warn("服务器端口资源不足");
                    try {
                        responseAck(request, Response.BUSY_HERE);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        logger.error("[命令发送失败] invite 服务器端口资源不足: {}", e.getMessage());
                    }
                    return;
                }
                // 写入redis， 超时时回复
                sendRtpItem.setStatus(1);
                sendRtpItem.setFromTag(request.getFromTag());
                sendRtpItem.setLocalIp(mediaServerItem.getSdpIp());
                SIPResponse response = sendStreamAck(request, sendRtpItem, platform);
                if (response != null) {
                    sendRtpItem.setToTag(response.getToTag());
                }
                if (sendRtpItem.getSsrc() == null) {
                    // 上级平台点播时不使用上级平台指定的ssrc，使用自定义的ssrc，参考国标文档-点播外域设备媒体流SSRC处理方式
                    String ssrc = "Play".equalsIgnoreCase(sendRtpItem.getSessionName()) ? ssrcFactory.getPlaySsrc(mediaServerItem.getId()) : ssrcFactory.getPlayBackSsrc(mediaServerItem.getId());
                    sendRtpItem.setSsrc(ssrc);
                }
                redisCatchStorage.updateSendRTPSever(sendRtpItem);

            } else {
                // 不在线 拉起
                notifyPushStreamOnline(sendRtpItem, mediaServerItem, platform, request);
            }
        } else {
            // 其他平台内容
            otherWvpPushStream(sendRtpItem, request, platform);
        }
    }

    /**
     * 通知流上线
     */
    private void notifyProxyStreamOnline(SendRtpItem sendRtpItem, MediaServerItem mediaServerItem, ParentPlatform platform, SIPRequest request) {
        // TODO 控制启用以使设备上线
        logger.info("[ app={}, stream={} ]通道未推流，启用流后开始推流", sendRtpItem.getApp(), sendRtpItem.getStream());
        // 监听流上线
        HookSubscribeForStreamChange hookSubscribe = HookSubscribeFactory.on_stream_changed(sendRtpItem.getApp(), sendRtpItem.getStream(), true, "rtsp", mediaServerItem.getId());
        zlmHttpHookSubscribe.addSubscribe(hookSubscribe, (mediaServerItemInUSe, hookParam) -> {
            OnStreamChangedHookParam streamChangedHookParam = (OnStreamChangedHookParam)hookParam;
            logger.info("[上级点播]拉流代理已经就绪， {}/{}", streamChangedHookParam.getApp(), streamChangedHookParam.getStream());
            dynamicTask.stop(sendRtpItem.getCallId());
            sendProxyStream(sendRtpItem, mediaServerItem, platform, request);
        });
        dynamicTask.startDelay(sendRtpItem.getCallId(), () -> {
            logger.info("[ app={}, stream={} ] 等待拉流代理流超时", sendRtpItem.getApp(), sendRtpItem.getStream());
            zlmHttpHookSubscribe.removeSubscribe(hookSubscribe);
        }, userSetting.getPlatformPlayTimeout());
        boolean start = streamProxyService.start(sendRtpItem.getApp(), sendRtpItem.getStream());
        if (!start) {
            try {
                responseAck(request, Response.BUSY_HERE, "channel [" + sendRtpItem.getChannelId() + "] offline");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                logger.error("[命令发送失败] invite 通道未推流: {}", e.getMessage());
            }
            zlmHttpHookSubscribe.removeSubscribe(hookSubscribe);
            dynamicTask.stop(sendRtpItem.getCallId());
        }
    }

    /**
     * 通知流上线
     */
    private void notifyPushStreamOnline(SendRtpItem sendRtpItem, MediaServerItem mediaServerItem, ParentPlatform platform, SIPRequest request) {
        // 发送redis消息以使设备上线，流上线后被
        logger.info("[ app={}, stream={} ]通道未推流，发送redis信息控制设备开始推流", sendRtpItem.getApp(), sendRtpItem.getStream());
        MessageForPushChannel messageForPushChannel = MessageForPushChannel.getInstance(1,
                sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getChannelId(), sendRtpItem.getPlatformId(),
                platform.getName(), userSetting.getServerId(), sendRtpItem.getMediaServerId());
        redisCatchStorage.sendStreamPushRequestedMsg(messageForPushChannel);
        // 设置超时
        dynamicTask.startDelay(sendRtpItem.getCallId(), () -> {
            logger.info("[ app={}, stream={} ] 等待设备开始推流超时", sendRtpItem.getApp(), sendRtpItem.getStream());
            try {
                redisPushStreamResponseListener.removeEvent(sendRtpItem.getApp(), sendRtpItem.getStream());
                mediaListManager.removedChannelOnlineEventLister(sendRtpItem.getApp(), sendRtpItem.getStream());
                responseAck(request, Response.REQUEST_TIMEOUT); // 超时
            } catch (SipException | InvalidArgumentException | ParseException e) {
                logger.error("未处理的异常 ", e);
            }
        }, userSetting.getPlatformPlayTimeout());
        redisCatchStorage.addWaiteSendRtpItem(sendRtpItem, userSetting.getPlatformPlayTimeout());
        // 添加上线的通知
        mediaListManager.addChannelOnlineEventLister(sendRtpItem.getApp(), sendRtpItem.getStream(), (sendRtpItemFromRedis) -> {
            dynamicTask.stop(sendRtpItem.getCallId());
            redisPushStreamResponseListener.removeEvent(sendRtpItem.getApp(), sendRtpItem.getStream());
            if (sendRtpItemFromRedis.getServerId().equals(userSetting.getServerId())) {

                int localPort = sendRtpPortManager.getNextPort(mediaServerItem);
                if (localPort == 0) {
                    logger.warn("上级点时创建sendRTPItem失败，可能是服务器端口资源不足");
                    try {
                        responseAck(request, Response.BUSY_HERE);
                    } catch (SipException e) {
                        logger.error("未处理的异常 ", e);
                    } catch (InvalidArgumentException e) {
                        logger.error("未处理的异常 ", e);
                    } catch (ParseException e) {
                        logger.error("未处理的异常 ", e);
                    }
                    return;
                }
                sendRtpItem.setLocalPort(localPort);
                if (!ObjectUtils.isEmpty(platform.getSendStreamIp())) {
                    sendRtpItem.setLocalIp(platform.getSendStreamIp());
                }

                // 写入redis， 超时时回复
                sendRtpItem.setStatus(1);
                SIPResponse response = sendStreamAck(request, sendRtpItem, platform);
                if (response != null) {
                    sendRtpItem.setToTag(response.getToTag());
                }
                redisCatchStorage.updateSendRTPSever(sendRtpItem);
            } else {
                // 其他平台内容
                otherWvpPushStream(sendRtpItemFromRedis, request, platform);
            }
        });

        // 添加回复的拒绝或者错误的通知
        redisPushStreamResponseListener.addEvent(sendRtpItem.getApp(), sendRtpItem.getStream(), response -> {
            if (response.getCode() != 0) {
                dynamicTask.stop(sendRtpItem.getCallId());
                mediaListManager.removedChannelOnlineEventLister(sendRtpItem.getApp(), sendRtpItem.getStream());
                try {
                    responseAck(request, Response.TEMPORARILY_UNAVAILABLE, response.getMsg());
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    logger.error("[命令发送失败] 国标级联 点播回复: {}", e.getMessage());
                }
            }
        });
    }



    /**
     * 来自其他wvp的推流
     */
    private void otherWvpPushStream(SendRtpItem sendRtpItem, SIPRequest request, ParentPlatform platform) {
        logger.info("[级联点播]直播流来自其他平台，发送redis消息");
        // 发送redis消息
        redisCatchStorage.sendStartSendRtp(sendRtpItem);
        // 写入redis， 超时时回复
        sendRtpItem.setStatus(1);
        sendRtpItem.setCallId(request.getCallIdHeader().getCallId());
        sendRtpItem.setFromTag(request.getFromTag());
        SIPResponse response = sendStreamAck(request, sendRtpItem, platform);
        if (response != null) {
            sendRtpItem.setToTag(response.getToTag());
        }
        redisCatchStorage.updateSendRTPSever(sendRtpItem);
    }

    public SIPResponse sendStreamAck(SIPRequest request, SendRtpItem sendRtpItem, ParentPlatform platform) {

        String sdpIp = sendRtpItem.getLocalIp();
        if (!ObjectUtils.isEmpty(platform.getSendStreamIp())) {
            sdpIp = platform.getSendStreamIp();
        }
        StringBuffer content = new StringBuffer(200);
        content.append("v=0\r\n");
        content.append("o=" + sendRtpItem.getChannelId() + " 0 0 IN IP4 " + sdpIp + "\r\n");
        content.append("s=Play\r\n");
        content.append("c=IN IP4 " + sdpIp + "\r\n");
        content.append("t=0 0\r\n");
        // 非严格模式端口不统一, 增加兼容性，修改为一个不为0的端口
        int localPort = sendRtpItem.getLocalPort();
        if (localPort == 0) {
            localPort = new Random().nextInt(65535) + 1;
        }
        content.append("m=video " + localPort + " RTP/AVP 96\r\n");
        content.append("a=sendonly\r\n");
        content.append("a=rtpmap:96 PS/90000\r\n");
        if (sendRtpItem.isTcp()) {
            content.append("a=connection:new\r\n");
            if (!sendRtpItem.isTcpActive()) {
                content.append("a=setup:active\r\n");
            } else {
                content.append("a=setup:passive\r\n");
            }
        }
        content.append("y=" + sendRtpItem.getSsrc() + "\r\n");
        content.append("f=\r\n");

        try {
            return responseSdpAck(request, content.toString(), platform);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            logger.error("未处理的异常 ", e);
        }
        return null;
    }

    public void inviteFromDeviceHandle(SIPRequest request, String requesterId, String channelId) {

        String realChannelId = null;

        // 非上级平台请求，查询是否设备请求（通常为接收语音广播的设备）
        Device device = redisCatchStorage.getDevice(requesterId);
        // 判断requesterId是设备还是通道
        if (device == null) {
            device = storager.queryVideoDeviceByChannelId(requesterId);
            realChannelId = requesterId;
        }else {
            realChannelId = channelId;
        }
        if (device == null) {
            // 检查channelID是否可用
            device = redisCatchStorage.getDevice(channelId);
            if (device == null) {
                device = storager.queryVideoDeviceByChannelId(channelId);
                realChannelId = channelId;
            }
        }

        if (device == null) {
            logger.warn("来自设备的Invite请求，无法从请求信息中确定所属设备，已忽略，requesterId： {}/{}", requesterId, channelId);
            try {
                responseAck(request, Response.FORBIDDEN);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                logger.error("[命令发送失败] 来自设备的Invite请求，无法从请求信息中确定所属设备 FORBIDDEN: {}", e.getMessage());
            }
            return;
        }

        AudioBroadcastCatch broadcastCatch = audioBroadcastManager.get(device.getDeviceId(), realChannelId);
        if (broadcastCatch == null) {
            logger.warn("来自设备的Invite请求非语音广播，已忽略，requesterId： {}/{}", requesterId, channelId);
            try {
                responseAck(request, Response.FORBIDDEN);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                logger.error("[命令发送失败] 来自设备的Invite请求非语音广播 FORBIDDEN: {}", e.getMessage());
            }
            return;
        }
        if (device != null) {
            logger.info("收到设备" + requesterId + "的语音广播Invite请求");
            String key = VideoManagerConstants.BROADCAST_WAITE_INVITE + device.getDeviceId();
            if (!SipUtils.isFrontEnd(device.getDeviceId())) {
                key += broadcastCatch.getChannelId();
            }
            dynamicTask.stop(key);
            try {
                responseAck(request, Response.TRYING);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                logger.error("[命令发送失败] invite BAD_REQUEST: {}", e.getMessage());
                playService.stopAudioBroadcast(device.getDeviceId(), broadcastCatch.getChannelId());
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
                    logger.info("不支持的媒体格式，返回415");
                    // 回复不支持的格式
                    try {
                        responseAck(request, Response.UNSUPPORTED_MEDIA_TYPE); // 不支持的格式，发415
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        logger.error("[命令发送失败] invite 不支持的媒体格式: {}", e.getMessage());
                        playService.stopAudioBroadcast(device.getDeviceId(), broadcastCatch.getChannelId());
                        return;
                    }
                    return;
                }
                String addressStr = sdp.getOrigin().getAddress();
                logger.info("设备{}请求语音流，地址：{}:{}，ssrc：{}, {}", requesterId, addressStr, port, gb28181Sdp.getSsrc(),
                        mediaTransmissionTCP ? (tcpActive ? "TCP主动" : "TCP被动") : "UDP");

                MediaServerItem mediaServerItem = broadcastCatch.getMediaServerItem();
                if (mediaServerItem == null) {
                    logger.warn("未找到语音喊话使用的zlm");
                    try {
                        responseAck(request, Response.BUSY_HERE);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        logger.error("[命令发送失败] invite 未找到可用的zlm: {}", e.getMessage());
                        playService.stopAudioBroadcast(device.getDeviceId(), broadcastCatch.getChannelId());
                    }
                    return;
                }
                logger.info("设备{}请求语音流， 收流地址：{}:{}，ssrc：{}, {}, 对讲方式：{}", requesterId, addressStr, port, gb28181Sdp.getSsrc(),
                        mediaTransmissionTCP ? (tcpActive ? "TCP主动" : "TCP被动") : "UDP", sdp.getSessionName().getValue());
                CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);

                SendRtpItem sendRtpItem = zlmServerFactory.createSendRtpItem(mediaServerItem, addressStr, port, gb28181Sdp.getSsrc(), requesterId,
                        device.getDeviceId(), broadcastCatch.getChannelId(),
                        mediaTransmissionTCP, false);

                if (sendRtpItem == null) {
                    logger.warn("服务器端口资源不足");
                    try {
                        responseAck(request, Response.BUSY_HERE);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        logger.error("[命令发送失败] invite 服务器端口资源不足: {}", e.getMessage());
                        playService.stopAudioBroadcast(device.getDeviceId(), broadcastCatch.getChannelId());
                        return;
                    }
                    return;
                }


                sendRtpItem.setPlayType(InviteStreamType.BROADCAST);
                sendRtpItem.setCallId(callIdHeader.getCallId());
                sendRtpItem.setPlatformId(requesterId);
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

                redisCatchStorage.updateSendRTPSever(sendRtpItem);

                Boolean streamReady = zlmServerFactory.isStreamReady(mediaServerItem, broadcastCatch.getApp(), broadcastCatch.getStream());
                if (streamReady) {
                    sendOk(device, sendRtpItem, sdp, request, mediaServerItem, mediaTransmissionTCP, gb28181Sdp.getSsrc());
                } else {
                    logger.warn("[语音通话]， 未发现待推送的流,app={},stream={}", broadcastCatch.getApp(), broadcastCatch.getStream());
                    try {
                        responseAck(request, Response.GONE);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        logger.error("[命令发送失败] 语音通话 回复410失败， {}", e.getMessage());
                        return;
                    }
                    playService.stopAudioBroadcast(device.getDeviceId(), broadcastCatch.getChannelId());
                }
            } catch (SdpException e) {
                logger.error("[SDP解析异常]", e);
                playService.stopAudioBroadcast(device.getDeviceId(), broadcastCatch.getChannelId());
            }
        } else {
            logger.warn("来自无效设备/平台的请求");
            try {
                responseAck(request, Response.BAD_REQUEST);
                ; // 不支持的格式，发415
            } catch (SipException | InvalidArgumentException | ParseException e) {
                logger.error("[命令发送失败] invite 来自无效设备/平台的请求， {}", e.getMessage());
            }
        }
    }

    SIPResponse sendOk(Device device, SendRtpItem sendRtpItem, SessionDescription sdp, SIPRequest request, MediaServerItem mediaServerItem, boolean mediaTransmissionTCP, String ssrc) {
        SIPResponse sipResponse = null;
        try {
            sendRtpItem.setStatus(2);
            redisCatchStorage.updateSendRTPSever(sendRtpItem);
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

            ParentPlatform parentPlatform = new ParentPlatform();
            parentPlatform.setServerIP(device.getIp());
            parentPlatform.setServerPort(device.getPort());
            parentPlatform.setServerGBId(device.getDeviceId());

            sipResponse = responseSdpAck(request, content.toString(), parentPlatform);

            AudioBroadcastCatch audioBroadcastCatch = audioBroadcastManager.get(device.getDeviceId(), sendRtpItem.getChannelId());

            audioBroadcastCatch.setStatus(AudioBroadcastCatchStatus.Ok);
            audioBroadcastCatch.setSipTransactionInfoByRequset(sipResponse);
            audioBroadcastManager.update(audioBroadcastCatch);
            streamSession.put(device.getDeviceId(), sendRtpItem.getChannelId(), request.getCallIdHeader().getCallId(), sendRtpItem.getStream(), sendRtpItem.getSsrc(), sendRtpItem.getMediaServerId(), sipResponse, InviteSessionType.BROADCAST);
            // 开启发流，大华在收到200OK后就会开始建立连接
            if (!device.isBroadcastPushAfterAck()) {
                logger.info("[语音喊话] 回复200OK后发现 BroadcastPushAfterAck为False，现在开始推流");
                playService.startPushStream(sendRtpItem, sipResponse, parentPlatform, request.getCallIdHeader());
            }

        } catch (SipException | InvalidArgumentException | ParseException | SdpParseException e) {
            logger.error("[命令发送失败] 语音喊话 回复200OK（SDP）: {}", e.getMessage());
        }
        return sipResponse;
    }
}
