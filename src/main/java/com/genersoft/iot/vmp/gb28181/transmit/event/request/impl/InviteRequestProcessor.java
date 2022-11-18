package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.SIPSender;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.media.zlm.ZLMMediaListManager;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.*;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.service.IPlayService;
import com.genersoft.iot.vmp.service.IStreamProxyService;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.service.bean.MessageForPushChannel;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.service.redisMsg.RedisGbPlayMsgListener;
import com.genersoft.iot.vmp.service.redisMsg.RedisPushStreamResponseListener;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.AudioBroadcastResult;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import gov.nist.javax.sdp.TimeDescriptionImpl;
import gov.nist.javax.sdp.fields.TimeField;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
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
    private ZLMRTPServerFactory zlmrtpServerFactory;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private IMediaService mediaService;

	@Autowired
	private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private ZlmHttpHookSubscribe zlmHttpHookSubscribe;

    @Autowired
    private SIPProcessorObserver sipProcessorObserver;

    @Autowired
    private VideoStreamSessionManager sessionManager;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private ZLMMediaListManager mediaListManager;

	@Autowired
	private DeferredResultHolder resultHolder;

	@Autowired
	private ZlmHttpHookSubscribe subscribe;

	@Autowired
	private SipConfig config;



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
        //  Invite Request消息实现，此消息一般为级联消息，上级给下级发送请求视频指令
        try {
            SIPRequest request = (SIPRequest)evt.getRequest();
            String channelId = SipUtils.getChannelIdFromRequest(request);
            String requesterId = SipUtils.getUserIdFromFromHeader(request);
            CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
            if (requesterId == null || channelId == null) {
                logger.info("无法从FromHeader的Address中获取到平台id，返回400");
                // 参数不全， 发400，请求错误
                try {
                    responseAck(request, Response.BAD_REQUEST);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    logger.error("[命令发送失败] invite BAD_REQUEST: {}", e.getMessage());
                }
                return;
            }
            String ssrc = null;
            SessionDescription sdp = null;
            String ssrcDefault = "0000000000";
            if (channelId == null) {
                // 解析sdp消息, 使用jainsip 自带的sdp解析方式
                String contentString = new String(request.getRawContent());

                // jainSip不支持y=字段， 移除以解析。
                int ssrcIndex = contentString.indexOf("y=");

                if (ssrcIndex >= 0) {
                    //ssrc规定长度为10个字节，不取余下长度以避免后续还有“f=”字段
                    ssrc = contentString.substring(ssrcIndex + 2, ssrcIndex + 12);
                    String substring = contentString.substring(0, contentString.indexOf("y="));
                    sdp = SdpFactory.getInstance().createSessionDescription(substring);
                } else {
                    ssrc = ssrcDefault;
                    sdp = SdpFactory.getInstance().createSessionDescription(contentString);
                }
                channelId = sdp.getOrigin().getUsername();
            }


            // 查询请求是否来自上级平台\设备
            ParentPlatform platform = storager.queryParentPlatByServerGBId(requesterId);
            if (platform == null) {
                inviteFromDeviceHandle(request, requesterId, channelId);

            } else {
                // 查询平台下是否有该通道
                DeviceChannel channel = storager.queryChannelInParentPlatform(requesterId, channelId);
                GbStream gbStream = storager.queryStreamInParentPlatform(requesterId, channelId);
                PlatformCatalog catalog = storager.getCatalog(channelId);

                MediaServerItem mediaServerItem = null;
                StreamPushItem streamPushItem = null;
                StreamProxyItem proxyByAppAndStream =null;
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
                            if (streamPushItem == null || streamPushItem.getServerId().equals(userSetting.getServerId())) {
                                logger.info("[ app={}, stream={} ]找不到zlm {}，返回410", gbStream.getApp(), gbStream.getStream(), mediaServerId);
                                try {
                                    responseAck(request, Response.GONE);
                                } catch (SipException | InvalidArgumentException | ParseException e) {
                                    logger.error("[命令发送失败] invite GONE: {}", e.getMessage());
                                }
                                return;
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
                        }else if("proxy".equals(gbStream.getStreamType())){
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
                    logger.info("通道不存在，返回404");
                    try {
                        // 通道不存在，发404，资源不存在
                        responseAck(request, Response.NOT_FOUND);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        logger.error("[命令发送失败] invite 通道不存在: {}", e.getMessage());
                    }
                    return;
                }
                if (sdp == null || ssrc == null) {
                    // 解析sdp消息, 使用jainsip 自带的sdp解析方式
                    String contentString = new String(request.getRawContent());

                    // jainSip不支持y=字段， 移除以解析。
                    int ssrcIndex = contentString.indexOf("y=");
                    if (ssrcIndex >= 0) {
                        //ssrc规定长度为10个字节，不取余下长度以避免后续还有“f=”字段
                        ssrc = contentString.substring(ssrcIndex + 2, ssrcIndex + 12);
                        String substring = contentString.substring(0, contentString.indexOf("y="));
                        sdp = SdpFactory.getInstance().createSessionDescription(substring);
                    } else {
                        ssrc = ssrcDefault;
                        sdp = SdpFactory.getInstance().createSessionDescription(contentString);
                    }
                }

                String sessionName = sdp.getSessionName().getValue();

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
                String addressStr = sdp.getOrigin().getAddress();

                logger.info("[上级点播]用户：{}， 通道：{}, 地址：{}:{}， ssrc：{}", username, channelId, addressStr, port, ssrc);
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
                    SendRtpItem sendRtpItem = zlmrtpServerFactory.createSendRtpItem(mediaServerItem, addressStr, port, ssrc, requesterId,
                            device.getDeviceId(), channelId,
                            mediaTransmissionTCP);

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
                    String finalChannelId = channelId;
                    ZlmHttpHookSubscribe.Event hookEvent = (mediaServerItemInUSe, responseJSON) -> {
                        String app = responseJSON.getString("app");
                        String stream = responseJSON.getString("stream");
                        logger.info("[上级点播]下级已经开始推流。 回复200OK(SDP)， {}/{}", app, stream);
                        //     * 0 等待设备推流上来
                        //     * 1 下级已经推流，等待上级平台回复ack
                        //     * 2 推流中
                        sendRtpItem.setStatus(1);
                        redisCatchStorage.updateSendRTPSever(sendRtpItem);

                        StringBuffer content = new StringBuffer(200);
                        content.append("v=0\r\n");
                        content.append("o=" + finalChannelId + " 0 0 IN IP4 " + mediaServerItemInUSe.getSdpIp() + "\r\n");
                        content.append("s=" + sessionName + "\r\n");
                        content.append("c=IN IP4 " + mediaServerItemInUSe.getSdpIp() + "\r\n");
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

                        } catch (SipException e) {
                            e.printStackTrace();
                        } catch (InvalidArgumentException e) {
                            e.printStackTrace();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    };
                    SipSubscribe.Event errorEvent = ((event) -> {
                        // 未知错误。直接转发设备点播的错误
                        try {
                            Response response = getMessageFactory().createResponse(event.statusCode, evt.getRequest());
                            sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
                        } catch (ParseException | SipException  e) {
                            e.printStackTrace();
                        }
                    });
                    sendRtpItem.setApp("rtp");
                    if ("Playback".equalsIgnoreCase(sessionName)) {
                        sendRtpItem.setPlayType(InviteStreamType.PLAYBACK);
                        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(mediaServerItem, null, device.isSsrcCheck(), true);
                        sendRtpItem.setStreamId(ssrcInfo.getStream());
                        // 写入redis， 超时时回复
                        redisCatchStorage.updateSendRTPSever(sendRtpItem);
                        playService.playBack(mediaServerItem, ssrcInfo, device.getDeviceId(), channelId, DateUtil.formatter.format(start),
                                DateUtil.formatter.format(end), null, result -> {
                                    if (result.getCode() != 0) {
                                        logger.warn("录像回放失败");
                                        if (result.getEvent() != null) {
                                            errorEvent.response(result.getEvent());
                                        }
                                        redisCatchStorage.deleteSendRTPServer(platform.getServerGBId(), finalChannelId, callIdHeader.getCallId(), null);
                                        try {
                                            responseAck(request, Response.REQUEST_TIMEOUT);
                                        } catch (SipException | InvalidArgumentException | ParseException e) {
                                            logger.error("[命令发送失败] 国标级联 录像回放 发送REQUEST_TIMEOUT: {}", e.getMessage());
                                        }
                                    } else {
                                        if (result.getMediaServerItem() != null) {
                                            hookEvent.response(result.getMediaServerItem(), result.getResponse());
                                        }
                                    }
                                });
                    } else {
                        sendRtpItem.setPlayType(InviteStreamType.PLAY);
                        SsrcTransaction playTransaction = sessionManager.getSsrcTransaction(device.getDeviceId(), channelId, "play", null);
                        if (playTransaction != null) {
                            Boolean streamReady = zlmrtpServerFactory.isStreamReady(mediaServerItem, "rtp", playTransaction.getStream());
                            if (!streamReady) {
                                boolean hasRtpServer = mediaServerService.checkRtpServer(mediaServerItem, "rtp", playTransaction.getStream());
                                if (hasRtpServer) {
                                    logger.info("[上级点播]已经开启rtpServer但是尚未收到流，开启监听流的到来");
                                    HookSubscribeForStreamChange hookSubscribe = HookSubscribeFactory.on_stream_changed("rtp", playTransaction.getStream(), true, "rtsp", mediaServerItem.getId());
                                    zlmHttpHookSubscribe.addSubscribe(hookSubscribe, hookEvent);
                                }else {
                                    playTransaction = null;
                                }
                            }
                        }
                        if (playTransaction == null) {
                            String streamId = null;
                            if (mediaServerItem.isRtpEnable()) {
                                streamId = String.format("%s_%s", device.getDeviceId(), channelId);
                            }
                            SSRCInfo ssrcInfo = mediaServerService.openRTPServer(mediaServerItem, streamId, null, device.isSsrcCheck(), false);
                            logger.info(JSONObject.toJSONString(ssrcInfo));
                            sendRtpItem.setStreamId(ssrcInfo.getStream());
                            sendRtpItem.setSsrc(ssrc.equals(ssrcDefault) ? ssrcInfo.getSsrc() : ssrc);

                            // 写入redis， 超时时回复
                            redisCatchStorage.updateSendRTPSever(sendRtpItem);
                            MediaServerItem finalMediaServerItem = mediaServerItem;
                            playService.play(mediaServerItem, ssrcInfo, device, channelId, hookEvent, errorEvent, (code, msg) -> {
                                logger.info("[上级点播]超时, 用户：{}， 通道：{}", username, channelId);
                                redisCatchStorage.deleteSendRTPServer(platform.getServerGBId(), channelId, callIdHeader.getCallId(), null);
                            });
                        } else {
                            sendRtpItem.setStreamId(playTransaction.getStream());
                            // 写入redis， 超时时回复
                            redisCatchStorage.updateSendRTPSever(sendRtpItem);
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("app", sendRtpItem.getApp());
                            jsonObject.put("stream", sendRtpItem.getStreamId());
                            hookEvent.response(mediaServerItem, jsonObject);
                        }
                    }
                } else if (gbStream != null) {
                    if("push".equals(gbStream.getStreamType())) {
                        if (streamPushItem != null && streamPushItem.isPushIng()) {
                            // 推流状态
                            pushStream(evt, request, gbStream, streamPushItem, platform, callIdHeader, mediaServerItem, port, tcpActive,
                                    mediaTransmissionTCP, channelId, addressStr, ssrc, requesterId);
                        } else {
                            // 未推流 拉起
                            notifyStreamOnline(evt, request,gbStream, streamPushItem, platform, callIdHeader, mediaServerItem, port, tcpActive,
                                    mediaTransmissionTCP, channelId, addressStr, ssrc, requesterId);
                        }
                    }else if ("proxy".equals(gbStream.getStreamType())){
                        if (null != proxyByAppAndStream) {
                            if(proxyByAppAndStream.isStatus()){
                                pushProxyStream(evt, request, gbStream,  platform, callIdHeader, mediaServerItem, port, tcpActive,
                                        mediaTransmissionTCP, channelId, addressStr, ssrc, requesterId);
                            }else{
                                //开启代理拉流
                                notifyStreamOnline(evt, request,gbStream, null, platform, callIdHeader, mediaServerItem, port, tcpActive,
                                        mediaTransmissionTCP, channelId, addressStr, ssrc, requesterId);
                            }
                        }


                    }
                }
            }
        } catch (SdpParseException e) {
            logger.error("sdp解析错误", e);
        } catch (SdpException e) {
            e.printStackTrace();
        }
    }

    /**
     * 安排推流
     */
    private void pushProxyStream(RequestEvent evt, SIPRequest request, GbStream gbStream, ParentPlatform platform,
                            CallIdHeader callIdHeader, MediaServerItem mediaServerItem,
                            int port, Boolean tcpActive, boolean mediaTransmissionTCP,
                            String channelId, String addressStr, String ssrc, String requesterId) {
            Boolean streamReady = zlmrtpServerFactory.isStreamReady(mediaServerItem, gbStream.getApp(), gbStream.getStream());
            if (streamReady) {
                // 自平台内容
                SendRtpItem sendRtpItem = zlmrtpServerFactory.createSendRtpItem(mediaServerItem, addressStr, port, ssrc, requesterId,
                        gbStream.getApp(), gbStream.getStream(), channelId,
                        mediaTransmissionTCP);

                if (sendRtpItem == null) {
                    logger.warn("服务器端口资源不足");
                    try {
                        responseAck(request, Response.BUSY_HERE);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        logger.error("[命令发送失败] invite 服务器端口资源不足: {}", e.getMessage());
                    }
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
            Boolean streamReady = zlmrtpServerFactory.isStreamReady(mediaServerItem, gbStream.getApp(), gbStream.getStream());
            if (streamReady) {
                // 自平台内容
                SendRtpItem sendRtpItem = zlmrtpServerFactory.createSendRtpItem(mediaServerItem, addressStr, port, ssrc, requesterId,
                        gbStream.getApp(), gbStream.getStream(), channelId,
                        mediaTransmissionTCP);

                if (sendRtpItem == null) {
                    logger.warn("服务器端口资源不足");
                    try {
                        responseAck(request, Response.BUSY_HERE);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        logger.error("[命令发送失败] invite 服务器端口资源不足: {}", e.getMessage());
                    }
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
            zlmHttpHookSubscribe.addSubscribe(hookSubscribe, (mediaServerItemInUSe, responseJSON) -> {
                String app = responseJSON.getString("app");
                String stream = responseJSON.getString("stream");
                logger.info("[上级点播]拉流代理已经就绪， {}/{}", app, stream);
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
                    e.printStackTrace();
                } catch (InvalidArgumentException e) {
                    e.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }, userSetting.getPlatformPlayTimeout());
            // 添加监听
            int finalPort = port;
            Boolean finalTcpActive = tcpActive;

            // 添加在本机上线的通知
            mediaListManager.addChannelOnlineEventLister(gbStream.getApp(), gbStream.getStream(), (app, stream, serverId) -> {
                dynamicTask.stop(callIdHeader.getCallId());
                if (serverId.equals(userSetting.getServerId())) {
                    SendRtpItem sendRtpItem = zlmrtpServerFactory.createSendRtpItem(mediaServerItem, addressStr, finalPort, ssrc, requesterId,
                            app, stream, channelId, mediaTransmissionTCP);

                    if (sendRtpItem == null) {
                        logger.warn("上级点时创建sendRTPItem失败，可能是服务器端口资源不足");
                        try {
                            responseAck(request, Response.BUSY_HERE);
                        } catch (SipException e) {
                            e.printStackTrace();
                        } catch (InvalidArgumentException e) {
                            e.printStackTrace();
                        } catch (ParseException e) {
                            e.printStackTrace();
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
                channelId, mediaTransmissionTCP, null, responseSendItemMsg -> {
                    SendRtpItem sendRtpItem = responseSendItemMsg.getSendRtpItem();
                    if (sendRtpItem == null || responseSendItemMsg.getMediaServerItem() == null) {
                        logger.warn("服务器端口资源不足");
                        try {
                            responseAck(request, Response.BUSY_HERE);
                        } catch (SipException e) {
                            e.printStackTrace();
                        } catch (InvalidArgumentException e) {
                            e.printStackTrace();
                        } catch (ParseException e) {
                            e.printStackTrace();
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
        content.append("m=video " + sendRtpItem.getLocalPort() + " RTP/AVP 96\r\n");
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
            e.printStackTrace();
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void inviteFromDeviceHandle(SIPRequest request, String requesterId, String channelId) {

        // 非上级平台请求，查询是否设备请求（通常为接收语音广播的设备）
        Device device = redisCatchStorage.getDevice(requesterId);
        AudioBroadcastCatch audioBroadcastCatch = audioBroadcastManager.get(requesterId, channelId);
        if (audioBroadcastCatch == null) {
            logger.warn("来自设备的Invite请求非语音广播，已忽略，requesterId： {}/{}", requesterId, channelId);
            try {
                responseAck(serverTransaction, Response.FORBIDDEN);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                logger.error("[命令发送失败] 来自设备的Invite请求非语音广播 FORBIDDEN: {}", e.getMessage());
            }
            return;
        }
        Request request = serverTransaction.getRequest();
        if (device != null) {
            logger.info("收到设备" + requesterId + "的语音广播Invite请求");
            try {
                responseAck(request, Response.TRYING);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                logger.error("[命令发送失败] invite BAD_REQUEST: {}", e.getMessage());
            }
            String contentString = new String(request.getRawContent());
            // jainSip不支持y=字段， 移除移除以解析。
            String substring = contentString;
            String ssrc = "0000000404";
            int ssrcIndex = contentString.indexOf("y=");
            if (ssrcIndex > 0) {
                substring = contentString.substring(0, ssrcIndex);
                ssrc = contentString.substring(ssrcIndex + 2, ssrcIndex + 12).trim();
            }
            ssrcIndex = substring.indexOf("f=");
            if (ssrcIndex > 0) {
                substring = contentString.substring(0, ssrcIndex);
            }
            try {
                SessionDescription sdp = SdpFactory.getInstance().createSessionDescription(substring);

                //  获取支持的格式
                Vector mediaDescriptions = sdp.getMediaDescriptions(true);

                // 查看是否支持PS 负载96
                int port = -1;
                boolean mediaTransmissionTCP = false;
                Boolean tcpActive = null;
                for (int i = 0; i < mediaDescriptions.size(); i++) {
                    MediaDescription mediaDescription = (MediaDescription)mediaDescriptions.get(i);
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
                    try {
                        responseAck(request, Response.UNSUPPORTED_MEDIA_TYPE); // 不支持的格式，发415
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        logger.error("[命令发送失败] invite 不支持的媒体格式: {}", e.getMessage());
                    }
                    return;
                }
                String addressStr = sdp.getOrigin().getAddress();
                logger.info("设备{}请求语音流，地址：{}:{}，ssrc：{}", requesterId, addressStr, port, ssrc);

                MediaServerItem mediaServerItem = playService.getNewMediaServerItem(device);
                if (mediaServerItem == null) {
                    logger.warn("未找到可用的zlm");
                    try {
                        responseAck(serverTransaction, Response.BUSY_HERE);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        logger.error("[命令发送失败] invite 未找到可用的zlm: {}", e.getMessage());
                    }
                    return;
                }
                SendRtpItem sendRtpItem = zlmrtpServerFactory.createSendRtpItem(mediaServerItem, addressStr, port, ssrc, requesterId,
                        device.getDeviceId(), audioBroadcastCatch.getChannelId(),
                        mediaTransmissionTCP);
                if (sendRtpItem == null) {
                    logger.warn("服务器端口资源不足");
                    try {
                        responseAck(serverTransaction, Response.BUSY_HERE);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        logger.error("[命令发送失败] invite 服务器端口资源不足: {}", e.getMessage());
                    }
                    return;
                }
                sendRtpItem.setTcp(mediaTransmissionTCP);
                if (tcpActive != null) {
                    sendRtpItem.setTcpActive(tcpActive);
                }
                String app = "broadcast";
                String stream = device.getDeviceId() + "_" + audioBroadcastCatch.getChannelId();

                CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
                sendRtpItem.setPlayType(InviteStreamType.PLAY);
                sendRtpItem.setCallId(callIdHeader.getCallId());
                sendRtpItem.setPlatformId(requesterId);
                sendRtpItem.setStatus(1);
                sendRtpItem.setApp(app);
                sendRtpItem.setStreamId(stream);
                sendRtpItem.setPt(8);
                sendRtpItem.setUsePs(false);
                sendRtpItem.setOnlyAudio(true);
                redisCatchStorage.updateSendRTPSever(sendRtpItem);

                Boolean streamReady = zlmrtpServerFactory.isStreamReady(mediaServerItem, app, stream);
                if (streamReady) {
                    sendOk(device, sendRtpItem, sdp, serverTransaction, mediaServerItem, mediaTransmissionTCP, ssrc);
                }else {
                    logger.warn("[语音通话]， 未发现待推送的流,app={},stream={}", app, stream);
                    playService.stopAudioBroadcast(device.getDeviceId(), audioBroadcastCatch.getChannelId());
                }
            } catch (SdpException e) {
                logger.error("[SDP解析异常]", e);
            }
        } else {
            logger.warn("来自无效设备/平台的请求");
            try {
                responseAck(request, Response.BAD_REQUEST);; // 不支持的格式，发415
            } catch (SipException | InvalidArgumentException | ParseException e) {
                logger.error("[命令发送失败] invite 来自无效设备/平台的请求， {}", e.getMessage());
            }
        }
    }

    void sendOk(Device device, SendRtpItem sendRtpItem, SessionDescription sdp, ServerTransaction serverTransaction,  MediaServerItem mediaServerItem, boolean mediaTransmissionTCP, String ssrc){
        try {
            sendRtpItem.setStatus(2);
            redisCatchStorage.updateSendRTPSever(sendRtpItem);
            StringBuffer content = new StringBuffer(200);
            content.append("v=0\r\n");
            content.append("o="+ config.getId() +" "+ sdp.getOrigin().getSessionId() +" " + sdp.getOrigin().getSessionVersion()  + " IN IP4 "+mediaServerItem.getSdpIp()+"\r\n");
            content.append("s=Play\r\n");
            content.append("c=IN IP4 "+mediaServerItem.getSdpIp()+"\r\n");
            content.append("t=0 0\r\n");

            if (mediaTransmissionTCP) {
                content.append("m=audio "+ sendRtpItem.getLocalPort()+" TCP/RTP/AVP 8\r\n");
            }else {
                content.append("m=audio "+ sendRtpItem.getLocalPort()+" RTP/AVP 8\r\n");
            }

            content.append("a=rtpmap:8 PCMA/8000/1\r\n");

            content.append("a=sendonly\r\n");
            if (sendRtpItem.isTcp()) {
                content.append("a=connection:new\r\n");
                if (!sendRtpItem.isTcpActive()) {
                    content.append("a=setup:active\r\n");
                }else {
                    content.append("a=setup:passive\r\n");
                }
            }
            content.append("y="+ ssrc + "\r\n");
            content.append("f=v/////a/1/8/1\r\n");

            ParentPlatform parentPlatform = new ParentPlatform();
            parentPlatform.setServerIP(device.getIp());
            parentPlatform.setServerPort(device.getPort());
            parentPlatform.setServerGBId(device.getDeviceId());

            SIPResponse sipResponse = responseSdpAck(serverTransaction, content.toString(), parentPlatform);

            AudioBroadcastCatch audioBroadcastCatch = audioBroadcastManager.get(device.getDeviceId(), sendRtpItem.getChannelId());

            audioBroadcastCatch.setStatus(AudioBroadcastCatchStatus.Ok);
            audioBroadcastCatch.setSipTransactionInfoByRequset(sipResponse);
            audioBroadcastManager.update(audioBroadcastCatch);
        } catch (SipException | InvalidArgumentException | ParseException | SdpParseException e) {
            logger.error("[命令发送失败] 语音对讲 回复200OK（SDP）: {}", e.getMessage());
        }
    }
}
