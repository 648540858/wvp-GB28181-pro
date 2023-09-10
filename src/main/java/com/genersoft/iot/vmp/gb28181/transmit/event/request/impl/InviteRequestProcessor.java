package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import java.text.ParseException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import javax.sdp.*;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Response;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.SIPSender;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.media.zlm.ZLMMediaListManager;
import com.genersoft.iot.vmp.media.zlm.ZLMServerFactory;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.*;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;
import com.genersoft.iot.vmp.service.*;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.service.bean.MessageForPushChannel;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.service.redisMsg.RedisGbPlayMsgListener;
import com.genersoft.iot.vmp.service.redisMsg.RedisPushStreamResponseListener;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.DateUtil;

import gov.nist.javax.sdp.TimeDescriptionImpl;
import gov.nist.javax.sdp.fields.TimeField;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;

/**
 * SIP命令类型： INVITE请求
 */
@SuppressWarnings("rawtypes")
@Component
public class InviteRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

    private final static Logger logger = LoggerFactory.getLogger(InviteRequestProcessor.class);

    private final String method = "INVITE";

    @Autowired
    private SIPCommanderFroPlatform cmderFroPlatform;

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
    private ZLMMediaListManager mediaListManager;


    @Autowired
    private RedisGbPlayMsgListener redisGbPlayMsgListener;


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
        // Invite Request消息实现，此消息一般为级联消息，上级给下级发送请求视频指令
        try {
            SIPRequest request = (SIPRequest)evt.getRequest();
            String channelId = SipUtils.getChannelIdFromRequest(request);
            String requesterId = SipUtils.getUserIdFromFromHeader(request);
            CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
            if (requesterId == null || channelId == null) {
                logger.info("无法从FromHeader的Address中获取到平台id，返回400");
                // 参数不全， 发400，请求错误
                responseAckType(request, Response.BAD_REQUEST, "[命令发送失败] invite BAD_REQUEST: {}");
                return;
            }

            // 查询请求是否来自上级平台\设备
            ParentPlatform platform = storager.queryParentPlatByServerGBId(requesterId);
            if (platform == null) {
                inviteFromDeviceHandle(request, requesterId);
                return;
            }

            // 查询平台下是否有该通道
            DeviceChannel channel = storager.queryChannelInParentPlatform(requesterId, channelId);
            GbStream gbStream = storager.queryStreamInParentPlatform(requesterId, channelId);
            PlatformCatalog catalog = storager.getCatalog(requesterId, channelId);

            MediaServerItem mediaServerItem = null;
            StreamPushItem streamPushItem = null;
            StreamProxyItem proxyByAppAndStream = null;
            if (channel != null && gbStream == null) {
                // 不是通道可能是直播流
                // 通道存在，发100，TRYING
                responseAckType(request, Response.TRYING, "[命令发送失败] invite TRYING: {}");
            } else if (channel == null && gbStream != null) {
                // 通道不存在，但是国标存在，标志直播流
                String mediaServerId = gbStream.getMediaServerId();
                mediaServerItem = mediaServerService.getOne(mediaServerId);
                if (mediaServerItem == null) {
                    if ("proxy".equals(gbStream.getStreamType())) {
                        // 代理流，但是没有找到zlm
                        logger.info("[ app={}, stream={} ]找不到zlm {}，返回410", gbStream.getApp(), gbStream.getStream(), mediaServerId);
                        responseAckType(request, Response.GONE, "[命令发送失败] invite GONE: {}");
                        return;
                    } else {
                        // 非代理拉流 获取推流的流媒体
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
                            responseAckType(request, Response.GONE, "[命令发送失败] invite GONE: {}");
                            return;
                        }
                    } else if ("proxy".equals(gbStream.getStreamType())) {
                        proxyByAppAndStream = streamProxyService.getStreamProxyByAppAndStream(gbStream.getApp(), gbStream.getStream());
                        if (proxyByAppAndStream == null) {
                            logger.info("[ app={}, stream={} ]找不到zlm {}，返回410", gbStream.getApp(), gbStream.getStream(), mediaServerId);
                            responseAckType(request, Response.GONE, "[命令发送失败] invite GONE: {}");
                            return;
                        }
                    }
                }
                responseAckType(request, Response.CALL_IS_BEING_FORWARDED, "[命令发送失败] invite CALL_IS_BEING_FORWARDED: {}");
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
                responseAckType(request, Response.NOT_FOUND, "[命令发送失败] invite 通道不存在: {}");
                return;
            }
            // 解析sdp消息, 使用jainsip 自带的sdp解析方式
            String contentString = new String(request.getRawContent());

            Gb28181Sdp gb28181Sdp = SipUtils.parseSDP(contentString);
            SessionDescription sdp = gb28181Sdp.getBaseSdb();
            String sessionName = sdp.getSessionName().getValue();

            Long startTime = null;
            Long stopTime = null;
            Instant start = null;
            Instant end = null;
            if (CollectionUtils.isNotEmpty(sdp.getTimeDescriptions(false))) {
                TimeDescriptionImpl timeDescription = (TimeDescriptionImpl) (sdp.getTimeDescriptions(false).get(0));
                TimeField startTimeFiled = (TimeField) timeDescription.getTime();
                startTime = startTimeFiled.getStartTime();
                stopTime = startTimeFiled.getStopTime();

                start = Instant.ofEpochSecond(startTime);
                end = Instant.ofEpochSecond(stopTime);
            }
            // 获取支持的格式
            Vector mediaDescriptions = sdp.getMediaDescriptions(true);
            // 查看是否支持PS 负载96
            // String ip = null;
            int port = -1;
            boolean mediaTransmissionTCP = false;
            Boolean tcpActive = null;
            for (Object description : mediaDescriptions) {
                MediaDescription mediaDescription = (MediaDescription) description;
                Media media = mediaDescription.getMedia();

                Vector mediaFormats = media.getMediaFormats(false);
                if (mediaFormats.contains("96")) {
                    port = media.getMediaPort();
                    // String mediaType = media.getMediaType();
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
                responseAckType(request, Response.UNSUPPORTED_MEDIA_TYPE, "[命令发送失败] invite 不支持的格式: {}");
                return;
            }
            String username = sdp.getOrigin().getUsername();
            String addressStr = sdp.getConnection().getAddress();

            // 通过 channel 和 gbStream 是否为null 值判断来源是直播流还是国标
            if (channel != null) {
                Device device = storager.queryVideoDeviceByPlatformIdAndChannelId(requesterId, channelId);
                if (device == null) {
                    logger.warn("点播平台{}的通道{}时未找到设备信息", requesterId, channel);
                    responseAckType(request, Response.SERVER_INTERNAL_ERROR, "[命令发送失败] invite 未找到设备信息: {}");
                    return;
                }
                mediaServerItem = playService.getNewMediaServerItem(device);
                if (mediaServerItem == null) {
                    logger.warn("未找到可用的zlm");
                    responseAckType(request, Response.BUSY_HERE, "[命令发送失败] invite BUSY_HERE: {}");
                    return;
                }

                String ssrc;
                if (userSetting.getUseCustomSsrcForParentInvite() || gb28181Sdp.getSsrc() == null) {
                    // 上级平台点播时不使用上级平台指定的ssrc，使用自定义的ssrc，参考国标文档-点播外域设备媒体流SSRC处理方式
                    ssrc = "Play".equalsIgnoreCase(sessionName) ? ssrcFactory.getPlaySsrc(mediaServerItem.getId())
                            : ssrcFactory.getPlayBackSsrc(mediaServerItem.getId());
                } else {
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
                logger.info("[上级Invite] {}, 平台：{}， 通道：{}, 收流地址：{}:{}，收流方式：{}, ssrc：{}", sessionName, username, channelId, addressStr, port,
                        streamTypeStr, ssrc);
                SendRtpItem sendRtpItem = zlmServerFactory.createSendRtpItem(mediaServerItem, addressStr, port, ssrc, requesterId,
                        device.getDeviceId(), channelId, mediaTransmissionTCP, platform.isRtcp());

                if (sendRtpItem == null) {
                    logger.warn("服务器端口资源不足");
                    responseAckType(request, Response.BUSY_HERE, "[命令发送失败] invite 服务器端口资源不足: {}");
                    return;
                }
                if (tcpActive != null) {
                    sendRtpItem.setTcpActive(tcpActive);
                }
                sendRtpItem.setCallId(callIdHeader.getCallId());
                sendRtpItem.setPlayType("Play".equalsIgnoreCase(sessionName) ? InviteStreamType.PLAY : InviteStreamType.PLAYBACK);

                // 获取ACK返回参数，点播推流参数
                String content = getContent(channelId, mediaServerItem.getSdpIp(), sessionName, startTime, stopTime, sendRtpItem);

                ErrorCallback<Object> hookEvent = getHookSuccessCallback(sendRtpItem, content, callIdHeader.getCallId(), platform, request);
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

                // 创建回调
                ErrorCallback<Object> callback = callback(channelId, hookEvent, username, platform, callIdHeader, errorEvent);

                if ("Playback".equalsIgnoreCase(sessionName)) {
                    if (start == null || end == null) {
                        logger.info("[录像回放]未指定开始结束时间, 用户：{}， 通道：{}", username, channelId);
                        responseAckType(request, Response.BAD_REQUEST, "[命令发送失败] invite BAD_REQUEST: {}");
                        return;
                    }

                    // 回放
                    playback(sendRtpItem, start, end, device, channelId, mediaServerItem, callback);
                } else if ("Download".equalsIgnoreCase(sessionName)) {
                    // 获取指定的下载速度下载
                    download(sdp, sendRtpItem, mediaServerItem, device, channelId, start, end, callback);
                } else {
                    // 点播
                    play(mediaServerItem, device, channelId, ssrc, callback, sendRtpItem);
                }
            }

            if (gbStream != null) {
                String ssrc;
                if (userSetting.getUseCustomSsrcForParentInvite() || gb28181Sdp.getSsrc() == null) {
                    // 上级平台点播时不使用上级平台指定的ssrc，使用自定义的ssrc，参考国标文档-点播外域设备媒体流SSRC处理方式
                    ssrc = "Play".equalsIgnoreCase(sessionName) ? ssrcFactory.getPlaySsrc(mediaServerItem.getId())
                            : ssrcFactory.getPlayBackSsrc(mediaServerItem.getId());
                } else {
                    ssrc = gb28181Sdp.getSsrc();
                }

                if ("push".equals(gbStream.getStreamType())) {
                    if (streamPushItem != null && streamPushItem.isPushIng()) {
                        // 推流状态
                        pushStream(evt, request, gbStream, streamPushItem, platform, callIdHeader, mediaServerItem, port, tcpActive,
                                mediaTransmissionTCP, channelId, addressStr, ssrc, requesterId);
                    } else {
                        // 未推流 拉起
                        notifyStreamOnline(evt, request, gbStream, streamPushItem, platform, callIdHeader, mediaServerItem, port, tcpActive,
                                mediaTransmissionTCP, channelId, addressStr, ssrc, requesterId);
                    }
                } else if ("proxy".equals(gbStream.getStreamType())) {
                    if (null != proxyByAppAndStream) {
                        if (proxyByAppAndStream.isStatus()) {
                            pushProxyStream(evt, request, gbStream, platform, callIdHeader, mediaServerItem, port, tcpActive,
                                    mediaTransmissionTCP, channelId, addressStr, ssrc, requesterId);
                        } else {
                            // 开启代理拉流
                            notifyStreamOnline(evt, request, gbStream, null, platform, callIdHeader, mediaServerItem, port, tcpActive,
                                    mediaTransmissionTCP, channelId, addressStr, ssrc, requesterId);
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

    /**
     * 国标点播
     *
     * @param mediaServerItem 流媒体服务器
     * @param device          设备
     * @param channelId       通道id
     * @param ssrc            ssrc
     * @param sendRtpItem     发送rtp参数
     */
    private void play(MediaServerItem mediaServerItem, Device device, String channelId, String ssrc, ErrorCallback<Object> callback, SendRtpItem sendRtpItem) {
        SSRCInfo ssrcInfo = playService.play(mediaServerItem, device.getDeviceId(), channelId, ssrc, callback);
        sendRtpItem.setPlayType(InviteStreamType.PLAY);
        String streamId = String.format("%s_%s", device.getDeviceId(), channelId);
        sendRtpItem.setStreamId(streamId);
        sendRtpItem.setSsrc(ssrcInfo.getSsrc());
        redisCatchStorage.updateSendRTPSever(sendRtpItem);
    }

    /**
     * 下载请求
     *
     * @param sdp             下载速度
     * @param sendRtpItem     发送rtp参数
     * @param mediaServerItem 流媒体服务器
     * @param device          设备
     * @param channelId       通道id
     * @param start           开始时间
     * @param end             结束时间
     * @throws SdpException sdp解析异常
     */
    private void download(SessionDescription sdp, SendRtpItem sendRtpItem, MediaServerItem mediaServerItem, Device device, String channelId, Instant start, Instant end, ErrorCallback<Object> callback) throws SdpException {
        Vector sdpMediaDescriptions = sdp.getMediaDescriptions(true);
        MediaDescription mediaDescription = null;
        String downloadSpeed = "1";
        if (CollectionUtils.isNotEmpty(sdpMediaDescriptions)) {
            mediaDescription = (MediaDescription) sdpMediaDescriptions.get(0);
        }
        if (mediaDescription != null) {
            downloadSpeed = mediaDescription.getAttribute("downloadspeed");
        }

        sendRtpItem.setPlayType(InviteStreamType.DOWNLOAD);
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(mediaServerItem, null, null, device.isSsrcCheck(), true, 0, false,
                device.getStreamModeForParam());
        sendRtpItem.setStreamId(ssrcInfo.getStream());
        // 写入redis， 超时时回复
        redisCatchStorage.updateSendRTPSever(sendRtpItem);
        playService.download(mediaServerItem, ssrcInfo, device.getDeviceId(), channelId, DateUtil.formatter.format(start),
                DateUtil.formatter.format(end), Integer.parseInt(downloadSpeed),
                callback);
    }

    /**
     * 创建回调
     * @param channelId 通道id
     * @param hookEvent 回调事件
     * @param username 用户名
     * @param platform 平台
     * @param callIdHeader callId
     * @param errorEvent 错误回调
     * @return
     */
    @NotNull
    private ErrorCallback<Object> callback(String channelId, ErrorCallback<Object> hookEvent, String username, ParentPlatform platform, CallIdHeader callIdHeader, ErrorCallback<Object> errorEvent) {
        return (code, msg, data) -> {
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                hookEvent.run(code, msg, data);
            } else if (code == InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getCode()
                    || code == InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getCode()) {
                logger.info("[录像下载]超时, 用户：{}， 通道：{}", username, channelId);
                redisCatchStorage.deleteSendRTPServer(platform.getServerGBId(), channelId, callIdHeader.getCallId(), null);
                errorEvent.run(code, msg, data);
            } else {
                errorEvent.run(code, msg, data);
            }
        };
    }

    private void playback(SendRtpItem sendRtpItem, Instant start, Instant end, Device device, String channelId, MediaServerItem mediaServerItem, ErrorCallback<Object> callback) {
        sendRtpItem.setPlayType(InviteStreamType.PLAYBACK);
        String startTimeStr = DateUtil.urlFormatter.format(start);
        String endTimeStr = DateUtil.urlFormatter.format(end);
        String stream = device.getDeviceId() + "_" + channelId + "_" + startTimeStr + "_" + endTimeStr;
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(mediaServerItem, stream, null, device.isSsrcCheck(), true, 0, false,
                device.getStreamModeForParam());
        sendRtpItem.setStreamId(ssrcInfo.getStream());
        // 写入redis， 超时时回复
        redisCatchStorage.updateSendRTPSever(sendRtpItem);
        playService.playBack(mediaServerItem, ssrcInfo, device.getDeviceId(), channelId, DateUtil.formatter.format(start),
                DateUtil.formatter.format(end), callback);
    }


    @NotNull
    private ErrorCallback<Object> getHookSuccessCallback(SendRtpItem sendRtpItem, String content, String callId, ParentPlatform platform, SIPRequest request) {
        ErrorCallback<Object> hookEvent = (code, msg, data) -> {
            StreamInfo streamInfo = (StreamInfo) data;
            MediaServerItem originMediaServer = mediaServerService.getOne(streamInfo.getMediaServerId());
            logger.info("[上级Invite]下级已经开始推流。 回复200OK(SDP)， {}/{}", streamInfo.getApp(), streamInfo.getStream());
            // * 0 等待设备推流上来
            // * 1 下级已经推流，等待上级平台回复ack
            // * 2 推流中
            sendRtpItem.setStatus(1);
            redisCatchStorage.updateSendRTPSever(sendRtpItem);

            int localPort = sendRtpItem.getLocalPort();

            try {
                // 超时未收到Ack应该回复bye,当前等待时间为10秒
                dynamicTask.startDelay(callId, () -> {
                    logger.info("Ack 等待超时");
                    mediaServerService.releaseSsrc(originMediaServer.getId(), sendRtpItem.getSsrc());
                    // 回复bye
                    try {
                        cmderFroPlatform.streamByeCmd(platform, callId);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        logger.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
                    }
                }, 60 * 1000);
                responseSdpAck(request, content, platform);
                // tcp主动模式，回复sdp后开启监听
                if (sendRtpItem.isTcpActive()) {
                    MediaServerItem mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                    // 构造参数
                    Map<String, Object> param = getParam(sendRtpItem, localPort);

                    JSONObject startSendRtpStreamResult = zlmServerFactory.startSendRtpStreamForPassive(mediaInfo, param);
                    if (startSendRtpStreamResult != null) {
                        startSendRtpStreamHand(sendRtpItem, null, startSendRtpStreamResult, param);
                    }
                }
            } catch (SipException | InvalidArgumentException | ParseException e) {
                logger.error("[命令发送失败] 国标级联 回复SdpAck", e);
            }
        };
        return hookEvent;
    }

    public Map<String, Object> getParam(SendRtpItem sendRtpItem, int localPort) {
        Map<String, Object> param = new HashMap<>(12);
        param.put("vhost", "__defaultVhost__");
        param.put("app", sendRtpItem.getApp());
        param.put("stream", sendRtpItem.getStreamId());
        param.put("ssrc", sendRtpItem.getSsrc());
        if (!sendRtpItem.isTcpActive()) {
            param.put("dst_url", sendRtpItem.getIp());
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
            param.put("udp_rtcp_timeout", sendRtpItem.isRtcp() ? "1" : "0");
        }
        return param;
    }

    public String getContent(String channelId, String sdpIp, String sessionName, Long finalStartTime, Long finalStopTime, SendRtpItem sendRtpItem) {
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
        content.append("m=video " + localPort + " RTP/AVP 96\r\n");
        content.append("a=sendonly\r\n");
        content.append("a=rtpmap:96 PS/90000\r\n");
        content.append("y=" + sendRtpItem.getSsrc() + "\r\n");
        content.append("f=\r\n");

        return content.toString();
    }

    private void responseAckType(SIPRequest request, int trying, String format) {
        try {
            responseAck(request, trying);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            logger.error(format, e.getMessage());
        }
    }

    private void startSendRtpStreamHand(SendRtpItem sendRtpItem, ParentPlatform parentPlatform,
                                        JSONObject jsonObject, Map<String, Object> param) {
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
    private void pushProxyStream(RequestEvent evt, SIPRequest request, GbStream gbStream, ParentPlatform platform,
                            CallIdHeader callIdHeader, MediaServerItem mediaServerItem,
                            int port, Boolean tcpActive, boolean mediaTransmissionTCP,
                            String channelId, String addressStr, String ssrc, String requesterId) {
            Boolean streamReady = zlmServerFactory.isStreamReady(mediaServerItem, gbStream.getApp(), gbStream.getStream());
            if (streamReady != null && streamReady) {
                // 自平台内容
                SendRtpItem sendRtpItem = zlmServerFactory.createSendRtpItem(mediaServerItem, addressStr, port, ssrc, requesterId,
                        gbStream.getApp(), gbStream.getStream(), channelId, mediaTransmissionTCP, platform.isRtcp());

                if (sendRtpItem == null) {
                    logger.warn("服务器端口资源不足");
                    responseAckType(request, Response.BUSY_HERE, "[命令发送失败] invite 服务器端口资源不足: {}");
                    return;
                }
                if (tcpActive != null) {
                    sendRtpItem.setTcpActive(tcpActive);
                }
                sendRtpItem.setPlayType(InviteStreamType.PUSH);
                // 写入redis， 超时时回复
                sendRtpItem.setStatus(1);
                sendRtpItem.setCallId(callIdHeader.getCallId());
                sendRtpItem.setFromTag(request.getFromTag());

                SIPResponse response = sendStreamAck(mediaServerItem, request, sendRtpItem, platform, evt);
                if (response != null) {
                    sendRtpItem.setToTag(response.getToTag());
                }
                redisCatchStorage.updateSendRTPSever(sendRtpItem);

        }

    }
    private void pushStream(RequestEvent evt, SIPRequest request, GbStream gbStream, StreamPushItem streamPushItem, ParentPlatform platform,
                            CallIdHeader callIdHeader, MediaServerItem mediaServerItem,
                            int port, Boolean tcpActive, boolean mediaTransmissionTCP,
                            String channelId, String addressStr, String ssrc, String requesterId) {
        // 推流
        if (streamPushItem.isSelf()) {
            Boolean streamReady = zlmServerFactory.isStreamReady(mediaServerItem, gbStream.getApp(), gbStream.getStream());
            if (streamReady != null && streamReady) {
                // 自平台内容
                SendRtpItem sendRtpItem = zlmServerFactory.createSendRtpItem(mediaServerItem, addressStr, port, ssrc, requesterId,
                        gbStream.getApp(), gbStream.getStream(), channelId, mediaTransmissionTCP, platform.isRtcp());

                if (sendRtpItem == null) {
                    logger.warn("服务器端口资源不足");
                    responseAckType(request, Response.BUSY_HERE, "[命令发送失败] invite 服务器端口资源不足: {}");
                    return;
                }
                if (tcpActive != null) {
                    sendRtpItem.setTcpActive(tcpActive);
                }
                sendRtpItem.setPlayType(InviteStreamType.PUSH);
                // 写入redis， 超时时回复
                sendRtpItem.setStatus(1);
                sendRtpItem.setCallId(callIdHeader.getCallId());

                sendRtpItem.setFromTag(request.getFromTag());
                SIPResponse response = sendStreamAck(mediaServerItem, request, sendRtpItem, platform, evt);
                if (response != null) {
                    sendRtpItem.setToTag(response.getToTag());
                }

                redisCatchStorage.updateSendRTPSever(sendRtpItem);

            } else {
                // 不在线 拉起
                notifyStreamOnline(evt, request,gbStream, streamPushItem, platform, callIdHeader, mediaServerItem, port, tcpActive,
                        mediaTransmissionTCP, channelId, addressStr, ssrc, requesterId);
            }

        } else {
            // 其他平台内容
            otherWvpPushStream(evt, request, gbStream, streamPushItem, platform, callIdHeader, mediaServerItem, port, tcpActive,
                    mediaTransmissionTCP, channelId, addressStr, ssrc, requesterId);
        }
    }


    /**
     * 通知流上线
     */
    private void notifyStreamOnline(RequestEvent evt, SIPRequest request, GbStream gbStream, StreamPushItem streamPushItem, ParentPlatform platform,
                                    CallIdHeader callIdHeader, MediaServerItem mediaServerItem,
                                    int port, Boolean tcpActive, boolean mediaTransmissionTCP,
                                    String channelId, String addressStr, String ssrc, String requesterId) {
        if ("proxy".equals(gbStream.getStreamType())) {
            // TODO 控制启用以使设备上线
            logger.info("[ app={}, stream={} ]通道未推流，启用流后开始推流", gbStream.getApp(), gbStream.getStream());
            // 监听流上线
            HookSubscribeForStreamChange hookSubscribe = HookSubscribeFactory.on_stream_changed(gbStream.getApp(), gbStream.getStream(), true, "rtsp", mediaServerItem.getId());
            zlmHttpHookSubscribe.addSubscribe(hookSubscribe, (mediaServerItemInUSe, hookParam) -> {
                OnStreamChangedHookParam streamChangedHookParam = (OnStreamChangedHookParam)hookParam;
                logger.info("[上级点播]拉流代理已经就绪， {}/{}", streamChangedHookParam.getApp(), streamChangedHookParam.getStream());
                dynamicTask.stop(callIdHeader.getCallId());
                pushProxyStream(evt, request, gbStream,  platform, callIdHeader, mediaServerItem, port, tcpActive,
                        mediaTransmissionTCP, channelId, addressStr, ssrc, requesterId);
            });
            dynamicTask.startDelay(callIdHeader.getCallId(), () -> {
                logger.info("[ app={}, stream={} ] 等待拉流代理流超时", gbStream.getApp(), gbStream.getStream());
                zlmHttpHookSubscribe.removeSubscribe(hookSubscribe);
            }, userSetting.getPlatformPlayTimeout());
            boolean start = streamProxyService.start(gbStream.getApp(), gbStream.getStream());
            if (!start) {
                try {
                    responseAck(request, Response.BUSY_HERE, "channel [" + gbStream.getGbId() + "] offline");
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    logger.error("[命令发送失败] invite 通道未推流: {}", e.getMessage());
                }
                zlmHttpHookSubscribe.removeSubscribe(hookSubscribe);
                dynamicTask.stop(callIdHeader.getCallId());
            }



        } else if ("push".equals(gbStream.getStreamType())) {
            if (!platform.isStartOfflinePush()) {
                // 平台设置中关闭了拉起离线的推流则直接回复
                try {
                    logger.info("[上级点播] 失败，推流设备未推流，channel: {}, app: {}, stream: {}", gbStream.getGbId(), gbStream.getApp(), gbStream.getStream());
                    responseAck(request, Response.TEMPORARILY_UNAVAILABLE, "channel stream not pushing");
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    logger.error("[命令发送失败] invite 通道未推流: {}", e.getMessage());
                }
                return;
            }
            // 发送redis消息以使设备上线
            logger.info("[ app={}, stream={} ]通道未推流，发送redis信息控制设备开始推流", gbStream.getApp(), gbStream.getStream());

            MessageForPushChannel messageForPushChannel = MessageForPushChannel.getInstance(1,
                    gbStream.getApp(), gbStream.getStream(), gbStream.getGbId(), gbStream.getPlatformId(),
                    platform.getName(), null, gbStream.getMediaServerId());
            redisCatchStorage.sendStreamPushRequestedMsg(messageForPushChannel);
            // 设置超时
            dynamicTask.startDelay(callIdHeader.getCallId(), () -> {
                logger.info("[ app={}, stream={} ] 等待设备开始推流超时", gbStream.getApp(), gbStream.getStream());
                try {
                    mediaListManager.removedChannelOnlineEventLister(gbStream.getApp(), gbStream.getStream());
                    responseAck(request, Response.REQUEST_TIMEOUT); // 超时
                } catch (SipException e) {
                    logger.error("未处理的异常 ", e);
                } catch (InvalidArgumentException e) {
                    logger.error("未处理的异常 ", e);
                } catch (ParseException e) {
                    logger.error("未处理的异常 ", e);
                }
            }, userSetting.getPlatformPlayTimeout());
            // 添加监听
            int finalPort = port;
            Boolean finalTcpActive = tcpActive;

            // 添加在本机上线的通知
            mediaListManager.addChannelOnlineEventLister(gbStream.getApp(), gbStream.getStream(), (app, stream, serverId) -> {
                dynamicTask.stop(callIdHeader.getCallId());
                if (serverId.equals(userSetting.getServerId())) {
                    SendRtpItem sendRtpItem = zlmServerFactory.createSendRtpItem(mediaServerItem, addressStr, finalPort, ssrc, requesterId,
                            app, stream, channelId, mediaTransmissionTCP, platform.isRtcp());

                    if (sendRtpItem == null) {
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
                    if (finalTcpActive != null) {
                        sendRtpItem.setTcpActive(finalTcpActive);
                    }
                    sendRtpItem.setPlayType(InviteStreamType.PUSH);
                    // 写入redis， 超时时回复
                    sendRtpItem.setStatus(1);
                    sendRtpItem.setCallId(callIdHeader.getCallId());

                    sendRtpItem.setFromTag(request.getFromTag());
                    SIPResponse response = sendStreamAck(mediaServerItem, request, sendRtpItem, platform, evt);
                    if (response != null) {
                        sendRtpItem.setToTag(response.getToTag());
                    }
                    redisCatchStorage.updateSendRTPSever(sendRtpItem);
                } else {
                    // 其他平台内容
                    otherWvpPushStream(evt, request, gbStream, streamPushItem, platform, callIdHeader, mediaServerItem, port, tcpActive,
                            mediaTransmissionTCP, channelId, addressStr, ssrc, requesterId);
                }
            });

            // 添加回复的拒绝或者错误的通知
            redisPushStreamResponseListener.addEvent(gbStream.getApp(), gbStream.getStream(), response -> {
                if (response.getCode() != 0) {
                    dynamicTask.stop(callIdHeader.getCallId());
                    mediaListManager.removedChannelOnlineEventLister(gbStream.getApp(), gbStream.getStream());
                    try {
                        responseAck(request, Response.TEMPORARILY_UNAVAILABLE, response.getMsg());
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        logger.error("[命令发送失败] 国标级联 点播回复: {}", e.getMessage());
                    }
                }
            });
        }
    }

    /**
     * 来自其他wvp的推流
     */
    private void otherWvpPushStream(RequestEvent evt, SIPRequest request, GbStream gbStream, StreamPushItem streamPushItem, ParentPlatform platform,
                                    CallIdHeader callIdHeader, MediaServerItem mediaServerItem,
                                    int port, Boolean tcpActive, boolean mediaTransmissionTCP,
                                    String channelId, String addressStr, String ssrc, String requesterId) {
        logger.info("[级联点播]直播流来自其他平台，发送redis消息");
        // 发送redis消息
        redisGbPlayMsgListener.sendMsg(streamPushItem.getServerId(), streamPushItem.getMediaServerId(),
                streamPushItem.getApp(), streamPushItem.getStream(), addressStr, port, ssrc, requesterId,
                channelId, mediaTransmissionTCP, platform.isRtcp(),null, responseSendItemMsg -> {
                    SendRtpItem sendRtpItem = responseSendItemMsg.getSendRtpItem();
                    if (sendRtpItem == null || responseSendItemMsg.getMediaServerItem() == null) {
                        logger.warn("服务器端口资源不足");
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
                    // 收到sendItem
                    if (tcpActive != null) {
                        sendRtpItem.setTcpActive(tcpActive);
                    }
                    sendRtpItem.setPlayType(InviteStreamType.PUSH);
                    // 写入redis， 超时时回复
                    sendRtpItem.setStatus(1);
                    sendRtpItem.setCallId(callIdHeader.getCallId());

                    sendRtpItem.setFromTag(request.getFromTag());
                    SIPResponse response = sendStreamAck(responseSendItemMsg.getMediaServerItem(), request,sendRtpItem, platform, evt);
                    if (response != null) {
                        sendRtpItem.setToTag(response.getToTag());
                    }
                    redisCatchStorage.updateSendRTPSever(sendRtpItem);
                }, (wvpResult) -> {

                    // 错误
                    if (wvpResult.getCode() == RedisGbPlayMsgListener.ERROR_CODE_OFFLINE) {
                        // 离线
                        // 查询是否在本机上线了
                        StreamPushItem currentStreamPushItem = streamPushService.getPush(streamPushItem.getApp(), streamPushItem.getStream());
                        if (currentStreamPushItem.isPushIng()) {
                            // 在线状态
                            pushStream(evt, request, gbStream, streamPushItem, platform, callIdHeader, mediaServerItem, port, tcpActive,
                                    mediaTransmissionTCP, channelId, addressStr, ssrc, requesterId);

                        } else {
                            // 不在线 拉起
                            notifyStreamOnline(evt, request, gbStream, streamPushItem, platform, callIdHeader, mediaServerItem, port, tcpActive,
                                    mediaTransmissionTCP, channelId, addressStr, ssrc, requesterId);
                        }
                    }
                    try {
                        responseAck(request, Response.BUSY_HERE);
                    } catch (InvalidArgumentException | ParseException | SipException e) {
                        logger.error("[命令发送失败] 国标级联 点播回复 BUSY_HERE: {}", e.getMessage());
                    }
                });
    }

    public SIPResponse sendStreamAck(MediaServerItem mediaServerItem, SIPRequest request, SendRtpItem sendRtpItem, ParentPlatform platform, RequestEvent evt) {

        StringBuffer content = new StringBuffer(200);
        content.append("v=0\r\n");
        content.append("o=" + sendRtpItem.getChannelId() + " 0 0 IN IP4 " + mediaServerItem.getSdpIp() + "\r\n");
        content.append("s=Play\r\n");
        content.append("c=IN IP4 " + mediaServerItem.getSdpIp() + "\r\n");
        content.append("t=0 0\r\n");
        // 非严格模式端口不统一, 增加兼容性，修改为一个不为0的端口
        int localPort = sendRtpItem.getLocalPort();
        if(localPort == 0)
        {
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
        } catch (SipException e) {
            logger.error("未处理的异常 ", e);
        } catch (InvalidArgumentException e) {
            logger.error("未处理的异常 ", e);
        } catch (ParseException e) {
            logger.error("未处理的异常 ", e);
        }
        return null;
    }

    public void inviteFromDeviceHandle(SIPRequest request, String requesterId) {

        // 非上级平台请求，查询是否设备请求（通常为接收语音广播的设备）
        Device device = redisCatchStorage.getDevice(requesterId);
        if (device != null) {
            logger.info("收到设备" + requesterId + "的语音广播Invite请求");
            responseAckType(request, Response.TRYING, "[命令发送失败] invite BAD_REQUEST: {}");
            String contentString = new String(request.getRawContent());
            // jainSip不支持y=字段， 移除移除以解析。
            String ssrc = "0000000404";

            try {
                Gb28181Sdp gb28181Sdp = SipUtils.parseSDP(contentString);
                SessionDescription sdp = gb28181Sdp.getBaseSdb();
                //  获取支持的格式
                Vector mediaDescriptions = sdp.getMediaDescriptions(true);
                // 查看是否支持PS 负载96
                int port = -1;
                //boolean recvonly = false;
                boolean mediaTransmissionTCP = false;
                Boolean tcpActive = null;
                for (int i = 0; i < mediaDescriptions.size(); i++) {
                    MediaDescription mediaDescription = (MediaDescription) mediaDescriptions.get(i);
                    Media media = mediaDescription.getMedia();

                    Vector mediaFormats = media.getMediaFormats(false);
                    if (mediaFormats.contains("8")) {
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
                    }
                }
                if (port == -1) {
                    logger.info("不支持的媒体格式，返回415");
                    // 回复不支持的格式
                    responseAckType(request, Response.UNSUPPORTED_MEDIA_TYPE, "[命令发送失败] invite 不支持的媒体格式，返回415， {}");
                    return;
                }
                String username = sdp.getOrigin().getUsername();
                String addressStr = sdp.getConnection().getAddress();
                logger.info("设备{}请求语音流，地址：{}:{}，ssrc：{}", username, addressStr, port, ssrc);
            } catch (SdpException e) {
                logger.error("[SDP解析异常]", e);
            }


        } else {
            logger.warn("来自无效设备/平台的请求");
            responseAckType(request, Response.BAD_REQUEST, "[命令发送失败] invite 来自无效设备/平台的请求， {}");
        }
    }
}
