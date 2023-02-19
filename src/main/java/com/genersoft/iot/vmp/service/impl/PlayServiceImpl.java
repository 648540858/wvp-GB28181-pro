package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.exception.ServiceException;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.media.zlm.AssistRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeFactory;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForRtpServerTimeout;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForStreamChange;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IDeviceService;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.service.IPlayService;
import com.genersoft.iot.vmp.service.bean.*;
import com.genersoft.iot.vmp.service.redisMsg.RedisGbPlayMsgListener;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import com.genersoft.iot.vmp.vmanager.bean.AudioBroadcastResult;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.genersoft.iot.vmp.vmanager.gb28181.play.bean.AudioEvent;
import gov.nist.javax.sip.message.SIPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.header.CallIdHeader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.*;

@SuppressWarnings(value = {"rawtypes", "unchecked"})
@Service
public class PlayServiceImpl implements IPlayService {

    private final static Logger logger = LoggerFactory.getLogger(PlayServiceImpl.class);

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private AudioBroadcastManager audioBroadcastManager;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private ISIPCommanderForPlatform sipCommanderFroPlatform;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private ZLMRTPServerFactory zlmrtpServerFactory;

    @Autowired
    private DeferredResultHolder resultHolder;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private AssistRESTfulUtils assistRESTfulUtils;

    @Autowired
    private IMediaService mediaService;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private VideoStreamSessionManager streamSession;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private SipConfig sipConfig;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private ZlmHttpHookSubscribe subscribe;

    @Autowired
    private ISIPCommanderForPlatform commanderForPlatform;


    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private RedisGbPlayMsgListener redisGbPlayMsgListener;

    @Autowired
    private ZlmHttpHookSubscribe hookSubscribe;


    @Override
    public void play(MediaServerItem mediaServerItem, String deviceId, String channelId,
                     ZlmHttpHookSubscribe.Event hookEvent, SipSubscribe.Event errorEvent,
                     Runnable timeoutCallback) {
        if (mediaServerItem == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到可用的zlm");
        }
        String key = DeferredResultHolder.CALLBACK_CMD_PLAY + deviceId + channelId;

        RequestMessage msg = new RequestMessage();
        msg.setKey(key);

        Device device = redisCatchStorage.getDevice(deviceId);
        StreamInfo streamInfo = redisCatchStorage.queryPlayByDevice(deviceId, channelId);

        if (streamInfo != null) {
            String streamId = streamInfo.getStream();
            if (streamId == null) {
                WVPResult wvpResult = new WVPResult();
                wvpResult.setCode(ErrorCode.ERROR100.getCode());
                wvpResult.setMsg("点播失败， redis缓存streamId等于null");
                msg.setData(wvpResult);
                resultHolder.invokeAllResult(msg);
                return;
            }
            String mediaServerId = streamInfo.getMediaServerId();
            MediaServerItem mediaInfo = mediaServerService.getOne(mediaServerId);

            JSONObject rtpInfo = zlmresTfulUtils.getRtpInfo(mediaInfo, streamId);
            if (rtpInfo.getInteger("code") == 0) {
                if (rtpInfo.getBoolean("exist")) {
                    int localPort = rtpInfo.getInteger("local_port");
                    if (localPort == 0) {
                        logger.warn("[点播]，点播时发现rtpServerC存在，但是尚未开始推流");
                        // 此时说明rtpServer已经创建但是流还没有推上来
                        WVPResult wvpResult = new WVPResult();
                        wvpResult.setCode(ErrorCode.ERROR100.getCode());
                        wvpResult.setMsg("点播已经在进行中，请稍候重试");
                        msg.setData(wvpResult);

                        resultHolder.invokeAllResult(msg);
                        return;
                    } else {
                        WVPResult wvpResult = new WVPResult();
                        wvpResult.setCode(ErrorCode.SUCCESS.getCode());
                        wvpResult.setMsg(ErrorCode.SUCCESS.getMsg());
                        wvpResult.setData(streamInfo);
                        msg.setData(wvpResult);
                        resultHolder.invokeAllResult(msg);
                        if (hookEvent != null) {
                            hookEvent.response(mediaServerItem, JSON.parseObject(JSON.toJSONString(streamInfo)));
                        }
                    }

                } else {
                    redisCatchStorage.stopPlay(streamInfo);
                    storager.stopPlay(streamInfo.getDeviceID(), streamInfo.getChannelId());
                    streamInfo = null;
                }
            } else {
                //zlm连接失败
                redisCatchStorage.stopPlay(streamInfo);
                storager.stopPlay(streamInfo.getDeviceID(), streamInfo.getChannelId());
                streamInfo = null;
            }
        }
        if (streamInfo == null) {
            String streamId = null;
            if (mediaServerItem.isRtpEnable()) {
                streamId = String.format("%s_%s", device.getDeviceId(), channelId);
            }
            SSRCInfo ssrcInfo = mediaServerService.openRTPServer(mediaServerItem, streamId, device.isSsrcCheck(), false);
            if (ssrcInfo == null) {
                WVPResult wvpResult = new WVPResult();
                wvpResult.setCode(ErrorCode.ERROR100.getCode());
                wvpResult.setMsg("开启收流失败");
                msg.setData(wvpResult);

                resultHolder.invokeAllResult(msg);
                return;
            }
            play(mediaServerItem, ssrcInfo, device, channelId, (mediaServerItemInUse, response) -> {
                if (hookEvent != null) {
                    hookEvent.response(mediaServerItem, response);
                }
            }, event -> {
                // sip error错误
                WVPResult wvpResult = new WVPResult();
                wvpResult.setCode(ErrorCode.ERROR100.getCode());
                wvpResult.setMsg(String.format("点播失败， 错误码： %s, %s", event.statusCode, event.msg));
                msg.setData(wvpResult);
                resultHolder.invokeAllResult(msg);
                if (errorEvent != null) {
                    errorEvent.response(event);
                }
            }, (code, msgStr) -> {
                // invite点播超时
                WVPResult wvpResult = new WVPResult();
                wvpResult.setCode(ErrorCode.ERROR100.getCode());
                if (code == 0) {
                    wvpResult.setMsg("点播超时，请稍候重试");
                } else if (code == 1) {
                    wvpResult.setMsg("收流超时，请稍候重试");
                }
                msg.setData(wvpResult);
                // 回复之前所有的点播请求
                resultHolder.invokeAllResult(msg);
            });
        }
    }

    private void talk(MediaServerItem mediaServerItem, Device device, String channelId, String stream,
                      ZlmHttpHookSubscribe.Event hookEvent, SipSubscribe.Event errorEvent,
                      Runnable timeoutCallback, AudioEvent audioEvent) {

        String playSsrc = mediaServerItem.getSsrcConfig().getPlaySsrc();
        if (playSsrc == null) {
            audioEvent.call("ssrc已经用尽");
            return;
        }
        SendRtpItem sendRtpItem = new SendRtpItem();
        sendRtpItem.setApp("talk");
        sendRtpItem.setStream(stream);
        sendRtpItem.setSsrc(playSsrc);
        sendRtpItem.setDeviceId(device.getDeviceId());
        sendRtpItem.setPlatformId(device.getDeviceId());
        sendRtpItem.setChannelId(channelId);
        sendRtpItem.setRtcp(false);
        sendRtpItem.setMediaServerId(mediaServerItem.getId());
        sendRtpItem.setOnlyAudio(true);
        sendRtpItem.setPlayType(InviteStreamType.TALK);
        sendRtpItem.setPt(8);
        sendRtpItem.setStatus(1);
        sendRtpItem.setTcpActive(false);
        sendRtpItem.setTcp(true);
        sendRtpItem.setUsePs(false);
        sendRtpItem.setReceiveStream(stream);


        int port = zlmrtpServerFactory.keepPort(mediaServerItem, playSsrc);
        //端口获取失败的ssrcInfo 没有必要发送点播指令
        if (port <= 0) {
            logger.info("[语音对讲] 端口分配异常，deviceId={},channelId={}", device.getDeviceId(), channelId);
            audioEvent.call("端口分配异常");
            return;
        }
        sendRtpItem.setLocalPort(port);
        sendRtpItem.setPort(port);
        logger.info("[语音对讲]开始 deviceId: {}, channelId: {},收流端口： {}, 收流模式：{}, SSRC: {}, SSRC校验：{}", device.getDeviceId(), channelId, sendRtpItem.getLocalPort(), device.getStreamMode(), sendRtpItem.getSsrc(), false);
        // 超时处理
        String timeOutTaskKey = UUID.randomUUID().toString();
        dynamicTask.startDelay(timeOutTaskKey, () -> {

            logger.info("[语音对讲] 收流超时 deviceId: {}, channelId: {}，端口：{}, SSRC: {}", device.getDeviceId(), channelId, sendRtpItem.getPort(), sendRtpItem.getSsrc());
            timeoutCallback.run();
            // 点播超时回复BYE 同时释放ssrc以及此次点播的资源
            try {
                cmder.streamByeCmd(device, channelId, sendRtpItem.getStream(), null);
            } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
                logger.error("[语音对讲]超时， 发送BYE失败 {}", e.getMessage());
            } finally {
                timeoutCallback.run();
                mediaServerService.releaseSsrc(mediaServerItem.getId(), sendRtpItem.getSsrc());
                streamSession.remove(device.getDeviceId(), channelId, sendRtpItem.getStream());
            }
        }, userSetting.getPlayTimeout());

        String callId = SipUtils.getNewCallId();

        zlmrtpServerFactory.releasePort(mediaServerItem, playSsrc);
        Map<String, Object> param = new HashMap<>(12);
        param.put("vhost","__defaultVhost__");
        param.put("app", sendRtpItem.getApp());
        param.put("stream", sendRtpItem.getStream());
        param.put("ssrc", sendRtpItem.getSsrc());
        param.put("src_port", sendRtpItem.getLocalPort());
        param.put("pt", sendRtpItem.getPt());
        param.put("use_ps", sendRtpItem.isUsePs() ? "1" : "0");
        param.put("only_audio", sendRtpItem.isOnlyAudio() ? "1" : "0");
        param.put("is_udp", sendRtpItem.isTcp() ? "0" : "1");
        param.put("recv_stream_id", sendRtpItem.getReceiveStream());
        param.put("close_delay_ms", userSetting.getPlayTimeout() * 1000);

        zlmrtpServerFactory.startSendRtpPassive(mediaServerItem, param, jsonObject -> {
            if (jsonObject == null || jsonObject.getInteger("code") != 0 ) {
                mediaServerService.releaseSsrc(mediaServerItem.getId(), sendRtpItem.getSsrc());
                logger.info("[语音对讲]失败 deviceId: {}, channelId: {}", device.getDeviceId(), channelId);
                audioEvent.call("失败, " + jsonObject.getString("msg"));
                // 查看是否已经建立了通道，存在则发送bye
                stopTalk(device, channelId);
            }
        });


        // 查看设备是否已经在推流
        try {
            cmder.talkStreamCmd(mediaServerItem, sendRtpItem, device, channelId, callId, (MediaServerItem mediaServerItemInuse, JSONObject response) -> {
                logger.info("[语音对讲] 流已生成， 开始推流： " + response.toJSONString());
                dynamicTask.stop(timeOutTaskKey);
                // TODO 暂不做处理
            }, (MediaServerItem mediaServerItemInuse, JSONObject json) -> {
                logger.info("[语音对讲] 设备开始推流： " + json.toJSONString());
                dynamicTask.stop(timeOutTaskKey);

            }, (event) -> {
                dynamicTask.stop(timeOutTaskKey);

                if (event.event instanceof ResponseEvent) {
                    ResponseEvent responseEvent = (ResponseEvent) event.event;
                    if (responseEvent.getResponse() instanceof SIPResponse) {
                        SIPResponse response = (SIPResponse) responseEvent.getResponse();
                        sendRtpItem.setFromTag(response.getFromTag());
                        sendRtpItem.setToTag(response.getToTag());
                        sendRtpItem.setCallId(response.getCallIdHeader().getCallId());
                        redisCatchStorage.updateSendRTPSever(sendRtpItem);

                        streamSession.put(device.getDeviceId(), channelId, response.getCallIdHeader().getCallId(),
                                sendRtpItem.getStream(), sendRtpItem.getSsrc(), sendRtpItem.getMediaServerId(),
                                response, VideoStreamSessionManager.SessionType.talk);
                    } else {
                        logger.error("[语音对讲]收到的消息错误，response不是SIPResponse");
                    }
                } else {
                    logger.error("[语音对讲]收到的消息错误，event不是ResponseEvent");
                }

            }, (event) -> {
                dynamicTask.stop(timeOutTaskKey);
                mediaServerService.closeRTPServer(mediaServerItem, sendRtpItem.getStream());
                // 释放ssrc
                mediaServerService.releaseSsrc(mediaServerItem.getId(), sendRtpItem.getSsrc());
                streamSession.remove(device.getDeviceId(), channelId, sendRtpItem.getStream());
                errorEvent.response(event);
            });
        } catch (InvalidArgumentException | SipException | ParseException e) {

            logger.error("[命令发送失败] 对讲消息: {}", e.getMessage());
            dynamicTask.stop(timeOutTaskKey);
            mediaServerService.closeRTPServer(mediaServerItem, sendRtpItem.getStream());
            // 释放ssrc
            mediaServerService.releaseSsrc(mediaServerItem.getId(), sendRtpItem.getSsrc());

            streamSession.remove(device.getDeviceId(), channelId, sendRtpItem.getStream());
            SipSubscribe.EventResult eventResult = new SipSubscribe.EventResult(new CmdSendFailEvent(null));
            eventResult.msg = "命令发送失败";
            errorEvent.response(eventResult);
        }
//        }

    }



    @Override
    public void play(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo, Device device, String channelId,
                     ZlmHttpHookSubscribe.Event hookEvent, SipSubscribe.Event errorEvent,
                     InviteTimeOutCallback timeoutCallback) {

        logger.info("[点播开始] deviceId: {}, channelId: {},收流端口： {}, 收流模式：{}, SSRC: {}, SSRC校验：{}", device.getDeviceId(), channelId, ssrcInfo.getPort(), device.getStreamMode(), ssrcInfo.getSsrc(), device.isSsrcCheck());
        // 超时处理
        String timeOutTaskKey = UUID.randomUUID().toString();
        dynamicTask.startDelay(timeOutTaskKey, () -> {
            // 执行超时任务时查询是否已经成功，成功了则不执行超时任务，防止超时任务取消失败的情况
            if (redisCatchStorage.queryPlayByDevice(device.getDeviceId(), channelId) == null) {
                logger.info("[点播超时] 收流超时 deviceId: {}, channelId: {}，端口：{}, SSRC: {}", device.getDeviceId(), channelId, ssrcInfo.getPort(), ssrcInfo.getSsrc());
                // 点播超时回复BYE 同时释放ssrc以及此次点播的资源
                try {
                    cmder.streamByeCmd(device, channelId, ssrcInfo.getStream(), null);
                } catch (InvalidArgumentException | ParseException | SipException |
                         SsrcTransactionNotFoundException e) {
                    logger.error("[点播超时]， 发送BYE失败 {}", e.getMessage());
                } finally {
                    timeoutCallback.run(1, "收流超时");
                    mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                    mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
                    streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());
                    mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
                    // 取消订阅消息监听
                    HookSubscribeForStreamChange hookSubscribe = HookSubscribeFactory.on_stream_changed("rtp", ssrcInfo.getStream(), true, "rtsp", mediaServerItem.getId());
                    subscribe.removeSubscribe(hookSubscribe);
                }
            }
        }, userSetting.getPlayTimeout());
        //端口获取失败的ssrcInfo 没有必要发送点播指令
        if (ssrcInfo.getPort() <= 0) {
            logger.info("[点播端口分配异常]，deviceId={},channelId={},ssrcInfo={}", device.getDeviceId(), channelId, ssrcInfo);
            dynamicTask.stop(timeOutTaskKey);
            // 释放ssrc
            mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
            streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());

            RequestMessage msg = new RequestMessage();
            msg.setKey(DeferredResultHolder.CALLBACK_CMD_PLAY + device.getDeviceId() + channelId);
            msg.setData(WVPResult.fail(ErrorCode.ERROR100.getCode(), "点播端口分配异常"));
            resultHolder.invokeAllResult(msg);
            return;
        }
        try {
            cmder.playStreamCmd(mediaServerItem, ssrcInfo, device, channelId, (MediaServerItem mediaServerItemInuse, JSONObject response) -> {
                logger.info("收到订阅消息： " + response.toJSONString());
                dynamicTask.stop(timeOutTaskKey);

                // hook响应
                onPublishHandlerForPlay(mediaServerItemInuse, response, device.getDeviceId(), channelId);
                hookEvent.response(mediaServerItemInuse, response);
                logger.info("[点播成功] deviceId: {}, channelId: {}", device.getDeviceId(), channelId);
                String streamUrl = String.format("http://127.0.0.1:%s/%s/%s.live.flv", mediaServerItemInuse.getHttpPort(), "rtp", ssrcInfo.getStream());
                String path = "snap";
                String fileName = device.getDeviceId() + "_" + channelId + ".jpg";
                // 请求截图
                logger.info("[请求截图]: " + fileName);
                zlmresTfulUtils.getSnap(mediaServerItemInuse, streamUrl, 15, 1, path, fileName);

            }, (event) -> {
                ResponseEvent responseEvent = (ResponseEvent) event.event;
                String contentString = new String(responseEvent.getResponse().getRawContent());
                // 获取ssrc
                int ssrcIndex = contentString.indexOf("y=");
                // 检查是否有y字段
                if (ssrcIndex >= 0) {
                    //ssrc规定长度为10字节，不取余下长度以避免后续还有“f=”字段 TODO 后续对不规范的非10位ssrc兼容
                    String ssrcInResponse = contentString.substring(ssrcIndex + 2, ssrcIndex + 12);
                    // 查询到ssrc不一致且开启了ssrc校验则需要针对处理
                    if (ssrcInfo.getSsrc().equals(ssrcInResponse)) {
                        return;
                    }
                    logger.info("[点播消息] 收到invite 200, 发现下级自定义了ssrc: {}", ssrcInResponse);
                    if (!mediaServerItem.isRtpEnable() || device.isSsrcCheck()) {
                        logger.info("[点播消息] SSRC修正 {}->{}", ssrcInfo.getSsrc(), ssrcInResponse);

                        if (!mediaServerItem.getSsrcConfig().checkSsrc(ssrcInResponse)) {
                            // ssrc 不可用
                            // 释放ssrc
                            mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                            streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());
                            event.msg = "下级自定义了ssrc,但是此ssrc不可用";
                            event.statusCode = 400;
                            errorEvent.response(event);
                            return;
                        }

                        // 单端口模式streamId也有变化，需要重新设置监听
                        if (!mediaServerItem.isRtpEnable()) {
                            // 添加订阅
                            HookSubscribeForStreamChange hookSubscribe = HookSubscribeFactory.on_stream_changed("rtp", ssrcInfo.getStream(), true, "rtsp", mediaServerItem.getId());
                            subscribe.removeSubscribe(hookSubscribe);
                            hookSubscribe.getContent().put("stream", String.format("%08x", Integer.parseInt(ssrcInResponse)).toUpperCase());
                            subscribe.addSubscribe(hookSubscribe, (MediaServerItem mediaServerItemInUse, JSONObject response) -> {
                                logger.info("[ZLM HOOK] ssrc修正后收到订阅消息： " + response.toJSONString());
                                dynamicTask.stop(timeOutTaskKey);
                                // hook响应
                                onPublishHandlerForPlay(mediaServerItemInUse, response, device.getDeviceId(), channelId);
                                hookEvent.response(mediaServerItemInUse, response);
                            });
                        }
                        // 关闭rtp server
                        mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
                        // 重新开启ssrc server
                        mediaServerService.openRTPServer(mediaServerItem, ssrcInfo.getStream(), ssrcInResponse, device.isSsrcCheck(), false, ssrcInfo.getPort());

                    }
                }
            }, (event) -> {
                dynamicTask.stop(timeOutTaskKey);
                mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
                // 释放ssrc
                mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());

                streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());
                errorEvent.response(event);
            });
        } catch (InvalidArgumentException | SipException | ParseException e) {

            logger.error("[命令发送失败] 点播消息: {}", e.getMessage());
            dynamicTask.stop(timeOutTaskKey);
            mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
            // 释放ssrc
            mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());

            streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());
            SipSubscribe.EventResult eventResult = new SipSubscribe.EventResult(new CmdSendFailEvent(null));
            eventResult.msg = "命令发送失败";
            errorEvent.response(eventResult);
        }
    }

    @Override
    public void onPublishHandlerForPlay(MediaServerItem mediaServerItem, JSONObject response, String deviceId, String channelId) {
        StreamInfo streamInfo = onPublishHandler(mediaServerItem, response, deviceId, channelId);
        RequestMessage msg = new RequestMessage();
        msg.setKey(DeferredResultHolder.CALLBACK_CMD_PLAY + deviceId + channelId);
        if (streamInfo != null) {
            DeviceChannel deviceChannel = storager.queryChannel(deviceId, channelId);
            if (deviceChannel != null) {
                deviceChannel.setStreamId(streamInfo.getStream());
                storager.startPlay(deviceId, channelId, streamInfo.getStream());
            }
            redisCatchStorage.startPlay(streamInfo);

            WVPResult wvpResult = new WVPResult();
            wvpResult.setCode(ErrorCode.SUCCESS.getCode());
            wvpResult.setMsg(ErrorCode.SUCCESS.getMsg());
            wvpResult.setData(streamInfo);

            msg.setData(wvpResult);
            resultHolder.invokeAllResult(msg);

        } else {
            logger.warn("设备预览API调用失败！");
            msg.setData(WVPResult.fail(ErrorCode.ERROR100.getCode(), "设备预览API调用失败！"));
            resultHolder.invokeAllResult(msg);
        }
    }

    private void onPublishHandlerForPlayback(MediaServerItem mediaServerItem, JSONObject response, String deviceId, String channelId, PlayBackCallback playBackCallback) {

        StreamInfo streamInfo = onPublishHandler(mediaServerItem, response, deviceId, channelId);
        PlayBackResult<StreamInfo> playBackResult = new PlayBackResult<>();
        if (streamInfo != null) {
            DeviceChannel deviceChannel = storager.queryChannel(deviceId, channelId);
            if (deviceChannel != null) {
                deviceChannel.setStreamId(streamInfo.getStream());
                storager.startPlay(deviceId, channelId, streamInfo.getStream());
            }
            redisCatchStorage.startPlay(streamInfo);


            playBackResult.setCode(ErrorCode.SUCCESS.getCode());
            playBackResult.setMsg(ErrorCode.SUCCESS.getMsg());
            playBackResult.setData(streamInfo);
            playBackCallback.call(playBackResult);
        } else {
            logger.warn("录像回放调用失败！");
            playBackResult.setCode(ErrorCode.ERROR100.getCode());
            playBackResult.setMsg("录像回放调用失败！");
            playBackCallback.call(playBackResult);
        }
    }

    @Override
    public MediaServerItem getNewMediaServerItem(Device device) {
        if (device == null) {
            return null;
        }
        MediaServerItem mediaServerItem;
        if (ObjectUtils.isEmpty(device.getMediaServerId()) || "auto".equals(device.getMediaServerId())) {
            mediaServerItem = mediaServerService.getMediaServerForMinimumLoad(null);
        } else {
            mediaServerItem = mediaServerService.getOne(device.getMediaServerId());
        }
        if (mediaServerItem == null) {
            logger.warn("点播时未找到可使用的ZLM...");
        }
        return mediaServerItem;
    }

    @Override
    public MediaServerItem getNewMediaServerItemHasAssist(Device device) {
        if (device == null) {
            return null;
        }
        MediaServerItem mediaServerItem;
        if (ObjectUtils.isEmpty(device.getMediaServerId()) || "auto".equals(device.getMediaServerId())) {
            mediaServerItem = mediaServerService.getMediaServerForMinimumLoad(true);
        } else {
            mediaServerItem = mediaServerService.getOne(device.getMediaServerId());
        }
        if (mediaServerItem == null) {
            logger.warn("[获取可用的ZLM节点]未找到可使用的ZLM...");
        }
        return mediaServerItem;
    }

    @Override
    public void playBack(String deviceId, String channelId, String startTime,
                         String endTime, InviteStreamCallback inviteStreamCallback,
                         PlayBackCallback callback) {
        Device device = storager.queryVideoDevice(deviceId);
        if (device == null) {
            return;
        }
        MediaServerItem newMediaServerItem = getNewMediaServerItem(device);
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(newMediaServerItem, null, device.isSsrcCheck(), true);

        playBack(newMediaServerItem, ssrcInfo, deviceId, channelId, startTime, endTime, inviteStreamCallback, callback);
    }

    @Override
    public void playBack(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo,
                         String deviceId, String channelId, String startTime,
                         String endTime, InviteStreamCallback infoCallBack,
                         PlayBackCallback playBackCallback) {
        if (mediaServerItem == null || ssrcInfo == null) {
            return;
        }

        Device device = storager.queryVideoDevice(deviceId);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "设备： " + deviceId + "不存在");
        }

        PlayBackResult<StreamInfo> playBackResult = new PlayBackResult<>();
        String playBackTimeOutTaskKey = UUID.randomUUID().toString();
        dynamicTask.startDelay(playBackTimeOutTaskKey, () -> {
            logger.warn(String.format("设备回放超时，deviceId：%s ，channelId：%s", deviceId, channelId));
            playBackResult.setCode(ErrorCode.ERROR100.getCode());
            playBackResult.setMsg("回放超时");

            try {
                cmder.streamByeCmd(device, channelId, ssrcInfo.getStream(), null);
            } catch (InvalidArgumentException | ParseException | SipException e) {
                logger.error("[录像流]回放超时 发送BYE失败 {}", e.getMessage());
            } catch (SsrcTransactionNotFoundException e) {
                // 点播超时回复BYE 同时释放ssrc以及此次点播的资源
                mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
                streamSession.remove(deviceId, channelId, ssrcInfo.getStream());
            }
            // 回复之前所有的点播请求
            playBackCallback.call(playBackResult);
        }, userSetting.getPlayTimeout());

        SipSubscribe.Event errorEvent = event -> {
            dynamicTask.stop(playBackTimeOutTaskKey);
            playBackResult.setCode(ErrorCode.ERROR100.getCode());
            playBackResult.setMsg(String.format("回放失败， 错误码： %s, %s", event.statusCode, event.msg));
            playBackResult.setEvent(event);
            playBackCallback.call(playBackResult);
            streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());
        };

        InviteStreamCallback hookEvent = (InviteStreamInfo inviteStreamInfo) -> {
            logger.info("收到回放订阅消息： " + inviteStreamInfo.getResponse().toJSONString());
            dynamicTask.stop(playBackTimeOutTaskKey);
            StreamInfo streamInfo = onPublishHandler(inviteStreamInfo.getMediaServerItem(), inviteStreamInfo.getResponse(), deviceId, channelId);
            if (streamInfo == null) {
                logger.warn("设备回放API调用失败！");
                playBackResult.setCode(ErrorCode.ERROR100.getCode());
                playBackResult.setMsg("设备回放API调用失败！");
                playBackCallback.call(playBackResult);
                return;
            }
            redisCatchStorage.startPlayback(streamInfo, inviteStreamInfo.getCallId());
            playBackResult.setCode(ErrorCode.SUCCESS.getCode());
            playBackResult.setMsg(ErrorCode.SUCCESS.getMsg());
            playBackResult.setData(streamInfo);
            playBackResult.setMediaServerItem(inviteStreamInfo.getMediaServerItem());
            playBackResult.setResponse(inviteStreamInfo.getResponse());
            playBackCallback.call(playBackResult);
        };

        try {
            cmder.playbackStreamCmd(mediaServerItem, ssrcInfo, device, channelId, startTime, endTime, infoCallBack,
                    hookEvent, eventResult -> {
                        if (eventResult.type == SipSubscribe.EventResultType.response) {
                            ResponseEvent responseEvent = (ResponseEvent) eventResult.event;
                            String contentString = new String(responseEvent.getResponse().getRawContent());
                            // 获取ssrc
                            int ssrcIndex = contentString.indexOf("y=");
                            // 检查是否有y字段
                            if (ssrcIndex >= 0) {
                                //ssrc规定长度为10字节，不取余下长度以避免后续还有“f=”字段 TODO 后续对不规范的非10位ssrc兼容
                                String ssrcInResponse = contentString.substring(ssrcIndex + 2, ssrcIndex + 12);
                                // 查询到ssrc不一致且开启了ssrc校验则需要针对处理
                                if (ssrcInfo.getSsrc().equals(ssrcInResponse)) {
                                    return;
                                }
                                logger.info("[回放消息] 收到invite 200, 发现下级自定义了ssrc: {}", ssrcInResponse);
                                if (!mediaServerItem.isRtpEnable() || device.isSsrcCheck()) {
                                    logger.info("[回放消息] SSRC修正 {}->{}", ssrcInfo.getSsrc(), ssrcInResponse);

                                    if (!mediaServerItem.getSsrcConfig().checkSsrc(ssrcInResponse)) {
                                        // ssrc 不可用
                                        // 释放ssrc
                                        mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                                        streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());
                                        eventResult.msg = "下级自定义了ssrc,但是此ssrc不可用";
                                        eventResult.statusCode = 400;
                                        errorEvent.response(eventResult);
                                        return;
                                    }

                                    // 单端口模式streamId也有变化，需要重新设置监听
                                    if (!mediaServerItem.isRtpEnable()) {
                                        // 添加订阅
                                        HookSubscribeForStreamChange hookSubscribe = HookSubscribeFactory.on_stream_changed("rtp", ssrcInfo.getStream(), true, "rtsp", mediaServerItem.getId());
                                        subscribe.removeSubscribe(hookSubscribe);
                                        hookSubscribe.getContent().put("stream", String.format("%08x", Integer.parseInt(ssrcInResponse)).toUpperCase());
                                        subscribe.addSubscribe(hookSubscribe, (MediaServerItem mediaServerItemInUse, JSONObject response) -> {
                                            logger.info("[ZLM HOOK] ssrc修正后收到订阅消息： " + response.toJSONString());
                                            dynamicTask.stop(playBackTimeOutTaskKey);
                                            // hook响应
                                            onPublishHandlerForPlayback(mediaServerItemInUse, response, device.getDeviceId(), channelId, playBackCallback);
                                            hookEvent.call(new InviteStreamInfo(mediaServerItem, null, eventResult.callId, "rtp", ssrcInfo.getStream()));
                                        });
                                    }
                                    // 关闭rtp server
                                    mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
                                    // 重新开启ssrc server
                                    mediaServerService.openRTPServer(mediaServerItem, ssrcInfo.getStream(), ssrcInResponse, device.isSsrcCheck(), true, ssrcInfo.getPort());
                                }
                            }
                        }

                    }, errorEvent);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            logger.error("[命令发送失败] 回放: {}", e.getMessage());

            SipSubscribe.EventResult eventResult = new SipSubscribe.EventResult(new CmdSendFailEvent(null));
            eventResult.msg = "命令发送失败";
            errorEvent.response(eventResult);
        }
    }


    @Override
    public void download(String deviceId, String channelId, String startTime, String endTime, int downloadSpeed, InviteStreamCallback infoCallBack, PlayBackCallback playBackCallback) {
        Device device = storager.queryVideoDevice(deviceId);
        if (device == null) {
            return;
        }
        MediaServerItem newMediaServerItem = getNewMediaServerItemHasAssist(device);
        if (newMediaServerItem == null) {
            PlayBackResult<StreamInfo> downloadResult = new PlayBackResult<>();
            downloadResult.setCode(ErrorCode.ERROR100.getCode());
            downloadResult.setMsg("未找到assist服务");
            playBackCallback.call(downloadResult);
            return;
        }
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(newMediaServerItem, null, device.isSsrcCheck(), true);

        download(newMediaServerItem, ssrcInfo, deviceId, channelId, startTime, endTime, downloadSpeed, infoCallBack, playBackCallback);
    }


    @Override
    public void download(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo, String deviceId, String channelId, String startTime, String endTime, int downloadSpeed, InviteStreamCallback infoCallBack, PlayBackCallback hookCallBack) {
        if (mediaServerItem == null || ssrcInfo == null) {
            return;
        }

        Device device = storager.queryVideoDevice(deviceId);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "设备：" + deviceId + "不存在");
        }
        PlayBackResult<StreamInfo> downloadResult = new PlayBackResult<>();

        String downLoadTimeOutTaskKey = UUID.randomUUID().toString();
        dynamicTask.startDelay(downLoadTimeOutTaskKey, () -> {
            logger.warn(String.format("录像下载请求超时，deviceId：%s ，channelId：%s", deviceId, channelId));
            downloadResult.setCode(ErrorCode.ERROR100.getCode());
            downloadResult.setMsg("录像下载请求超时");
            hookCallBack.call(downloadResult);

            // 点播超时回复BYE 同时释放ssrc以及此次点播的资源
            try {
                cmder.streamByeCmd(device, channelId, ssrcInfo.getStream(), null);
            } catch (InvalidArgumentException | ParseException | SipException e) {
                logger.error("[录像流]录像下载请求超时， 发送BYE失败 {}", e.getMessage());
            } catch (SsrcTransactionNotFoundException e) {
                mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                mediaServerService.closeRTPServer(mediaServerItem, ssrcInfo.getStream());
                streamSession.remove(deviceId, channelId, ssrcInfo.getStream());
            }
        }, userSetting.getPlayTimeout());

        SipSubscribe.Event errorEvent = event -> {
            dynamicTask.stop(downLoadTimeOutTaskKey);
            downloadResult.setCode(ErrorCode.ERROR100.getCode());
            downloadResult.setMsg(String.format("录像下载失败， 错误码： %s, %s", event.statusCode, event.msg));
            downloadResult.setEvent(event);
            hookCallBack.call(downloadResult);
            streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());
        };

        try {
            cmder.downloadStreamCmd(mediaServerItem, ssrcInfo, device, channelId, startTime, endTime, downloadSpeed, infoCallBack,
                    inviteStreamInfo -> {
                        logger.info("收到订阅消息： " + inviteStreamInfo.getResponse().toJSONString());
                        dynamicTask.stop(downLoadTimeOutTaskKey);
                        StreamInfo streamInfo = onPublishHandler(inviteStreamInfo.getMediaServerItem(), inviteStreamInfo.getResponse(), deviceId, channelId);
                        streamInfo.setStartTime(startTime);
                        streamInfo.setEndTime(endTime);
                        redisCatchStorage.startDownload(streamInfo, inviteStreamInfo.getCallId());
                        downloadResult.setCode(ErrorCode.SUCCESS.getCode());
                        downloadResult.setMsg(ErrorCode.SUCCESS.getMsg());
                        downloadResult.setData(streamInfo);
                        downloadResult.setMediaServerItem(inviteStreamInfo.getMediaServerItem());
                        downloadResult.setResponse(inviteStreamInfo.getResponse());
                        hookCallBack.call(downloadResult);
                    }, errorEvent);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            logger.error("[命令发送失败] 录像下载: {}", e.getMessage());

            SipSubscribe.EventResult eventResult = new SipSubscribe.EventResult(new CmdSendFailEvent(null));
            eventResult.msg = "命令发送失败";
            errorEvent.response(eventResult);
        }
    }

    @Override
    public StreamInfo getDownLoadInfo(String deviceId, String channelId, String stream) {
        StreamInfo streamInfo = redisCatchStorage.queryDownload(deviceId, channelId, stream, null);
        if (streamInfo != null) {
            if (streamInfo.getProgress() == 1) {
                return streamInfo;
            }

            // 获取当前已下载时长
            String mediaServerId = streamInfo.getMediaServerId();
            MediaServerItem mediaServerItem = mediaServerService.getOne(mediaServerId);
            if (mediaServerItem == null) {
                logger.warn("查询录像信息时发现节点已离线");
                return null;
            }
            if (mediaServerItem.getRecordAssistPort() > 0) {
                JSONObject jsonObject = assistRESTfulUtils.fileDuration(mediaServerItem, streamInfo.getApp(), streamInfo.getStream(), null);
                if (jsonObject == null) {
                    throw new ControllerException(ErrorCode.ERROR100.getCode(), "连接Assist服务失败");
                }
                if (jsonObject.getInteger("code") == 0) {
                    long duration = jsonObject.getLong("data");

                    if (duration == 0) {
                        streamInfo.setProgress(0);
                    } else {
                        String startTime = streamInfo.getStartTime();
                        String endTime = streamInfo.getEndTime();
                        long start = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime);
                        long end = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime);

                        BigDecimal currentCount = new BigDecimal(duration / 1000);
                        BigDecimal totalCount = new BigDecimal(end - start);
                        BigDecimal divide = currentCount.divide(totalCount, 2, RoundingMode.HALF_UP);
                        double process = divide.doubleValue();
                        streamInfo.setProgress(process);
                    }
                }
            }
        }
        return streamInfo;
    }

    @Override
    public void onPublishHandlerForDownload(InviteStreamInfo inviteStreamInfo, String deviceId, String channelId, String uuid) {
        RequestMessage msg = new RequestMessage();
        msg.setKey(DeferredResultHolder.CALLBACK_CMD_DOWNLOAD + deviceId + channelId);
        msg.setId(uuid);
        StreamInfo streamInfo = onPublishHandler(inviteStreamInfo.getMediaServerItem(), inviteStreamInfo.getResponse(), deviceId, channelId);
        if (streamInfo != null) {
            redisCatchStorage.startDownload(streamInfo, inviteStreamInfo.getCallId());
            msg.setData(JSON.toJSONString(streamInfo));
            resultHolder.invokeResult(msg);
        } else {
            logger.warn("设备预览API调用失败！");
            msg.setData(WVPResult.fail(ErrorCode.ERROR100.getCode(), "设备预览API调用失败！"));
            resultHolder.invokeResult(msg);
        }
    }


    public StreamInfo onPublishHandler(MediaServerItem mediaServerItem, JSONObject resonse, String deviceId, String channelId) {
        String streamId = resonse.getString("stream");
        JSONArray tracks = resonse.getJSONArray("tracks");
        StreamInfo streamInfo = mediaService.getStreamInfoByAppAndStream(mediaServerItem, "rtp", streamId, tracks, null);
        streamInfo.setDeviceID(deviceId);
        streamInfo.setChannelId(channelId);
        return streamInfo;
    }

    @Override
    public void zlmServerOffline(String mediaServerId) {
        // 处理正在向上推流的上级平台
        List<SendRtpItem> sendRtpItems = redisCatchStorage.querySendRTPServer(null);
        if (sendRtpItems.size() > 0) {
            for (SendRtpItem sendRtpItem : sendRtpItems) {
                if (sendRtpItem.getMediaServerId().equals(mediaServerId)) {
                    ParentPlatform platform = storager.queryParentPlatByServerGBId(sendRtpItem.getPlatformId());
                    try {
                        sipCommanderFroPlatform.streamByeCmd(platform, sendRtpItem.getCallId());
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        logger.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
                    }
                }
            }
        }
        // 处理正在观看的国标设备
        List<SsrcTransaction> allSsrc = streamSession.getAllSsrc();
        if (allSsrc.size() > 0) {
            for (SsrcTransaction ssrcTransaction : allSsrc) {
                if (ssrcTransaction.getMediaServerId().equals(mediaServerId)) {
                    Device device = deviceService.getDevice(ssrcTransaction.getDeviceId());
                    if (device == null) {
                        continue;
                    }
                    try {
                        cmder.streamByeCmd(device, ssrcTransaction.getChannelId(),
                                ssrcTransaction.getStream(), null);
                    } catch (InvalidArgumentException | ParseException | SipException |
                             SsrcTransactionNotFoundException e) {
                        logger.error("[zlm离线]为正在使用此zlm的设备， 发送BYE失败 {}", e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public AudioBroadcastResult audioBroadcast(Device device, String channelId) {
        // TODO 必须多端口模式才支持语音喊话鹤语音对讲
        if (device == null || channelId == null) {
            return null;
        }
        logger.info("[语音喊话] device： {}, channel: {}", device.getDeviceId(), channelId);
        DeviceChannel deviceChannel = storager.queryChannel(device.getDeviceId(), channelId);
        if (deviceChannel == null) {
            logger.warn("开启语音广播的时候未找到通道： {}", channelId);
            return null;
        }
        MediaServerItem mediaServerItem = mediaServerService.getMediaServerForMinimumLoad(null);
        String app = "broadcast";
        // TODO 从sip user agent中判断是什么品牌设备，大华默认使用talk模式，其他使用broadcast模式
//        String app = "talk";
        String stream = device.getDeviceId() + "_" + channelId;
        StreamInfo broadcast = mediaService.getStreamInfoByAppAndStream(mediaServerItem, "broadcast", stream, null, null, null, false);
        AudioBroadcastResult audioBroadcastResult = new AudioBroadcastResult();
        audioBroadcastResult.setApp(app);
        audioBroadcastResult.setStream(stream);
        audioBroadcastResult.setStreamInfo(new StreamContent(mediaService.getStreamInfoByAppAndStream(mediaServerItem, app, stream, null, null, null, false)));
        audioBroadcastResult.setCodec("G.711");
        return audioBroadcastResult;
    }

    @Override
    public void audioBroadcastCmd(Device device, String channelId, MediaServerItem mediaServerItem, int timeout, AudioEvent event) throws InvalidArgumentException, ParseException, SipException {
        if (device == null || channelId == null) {
            return;
        }
        logger.info("[语音喊话] device： {}, channel: {}", device.getDeviceId(), channelId);
        DeviceChannel deviceChannel = storager.queryChannel(device.getDeviceId(), channelId);
        if (deviceChannel == null) {
            logger.warn("开启语音广播的时候未找到通道： {}", channelId);
            event.call("开启语音广播的时候未找到通道");
            return;
        }
        // 查询通道使用状态
        if (audioBroadcastManager.exit(device.getDeviceId(), channelId)) {
            SendRtpItem sendRtpItem = redisCatchStorage.querySendRTPServer(device.getDeviceId(), channelId, null, null);
            if (sendRtpItem != null && sendRtpItem.isOnlyAudio()) {
                // 查询流是否存在，不存在则认为是异常状态
                MediaServerItem mediaServer = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                Boolean streamReady = zlmrtpServerFactory.isStreamReady(mediaServer, sendRtpItem.getApp(), sendRtpItem.getStream());
                if (streamReady) {
                    logger.warn("语音广播已经开启： {}", channelId);
                    event.call("语音广播已经开启");
                    return;
                } else {
                    stopAudioBroadcast(device.getDeviceId(), channelId);
                }
            }
        }
        SendRtpItem sendRtpItem = redisCatchStorage.querySendRTPServer(device.getDeviceId(), channelId, null, null);
        if (sendRtpItem != null) {
            MediaServerItem mediaServer = mediaServerService.getOne(sendRtpItem.getMediaServerId());
            Boolean streamReady = zlmrtpServerFactory.isStreamReady(mediaServer, "rtp", sendRtpItem.getReceiveStream());
            if (streamReady) {
                logger.warn("[语音对讲] 进行中： {}", channelId);
                event.call("语音对讲进行中");
                return;
            } else {
                stopTalk(device, channelId);
            }
        }

        // 发送通知
        cmder.audioBroadcastCmd(device, channelId, eventResultForOk -> {
            // 发送成功
            AudioBroadcastCatch audioBroadcastCatch = new AudioBroadcastCatch(device.getDeviceId(), channelId, AudioBroadcastCatchStatus.Ready, mediaServerItem);
            audioBroadcastManager.update(audioBroadcastCatch);
        }, eventResultForError -> {
            // 发送失败
            logger.error("语音广播发送失败： {}:{}", channelId, eventResultForError.msg);
            event.call("语音广播发送失败");
            stopAudioBroadcast(device.getDeviceId(), channelId);
        });
    }


    @Override
    public void stopAudioBroadcast(String deviceId, String channelId) {
        List<AudioBroadcastCatch> audioBroadcastCatchList = new ArrayList<>();
        if (channelId == null) {
            audioBroadcastCatchList.addAll(audioBroadcastManager.get(deviceId));
        } else {
            audioBroadcastCatchList.add(audioBroadcastManager.get(deviceId, channelId));
        }
        if (audioBroadcastCatchList.size() > 0) {
            for (AudioBroadcastCatch audioBroadcastCatch : audioBroadcastCatchList) {
                Device device = deviceService.getDevice(deviceId);
                if (device == null || audioBroadcastCatch == null) {
                    return;
                }
                SendRtpItem sendRtpItem = redisCatchStorage.querySendRTPServer(deviceId, audioBroadcastCatch.getChannelId(), null, null);
                if (sendRtpItem != null) {
                    redisCatchStorage.deleteSendRTPServer(deviceId, sendRtpItem.getChannelId(), null, null);
                    MediaServerItem mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                    Map<String, Object> param = new HashMap<>();
                    param.put("vhost", "__defaultVhost__");
                    param.put("app", sendRtpItem.getApp());
                    param.put("stream", sendRtpItem.getStream());
                    zlmresTfulUtils.stopSendRtp(mediaInfo, param);
                    try {
                        cmder.streamByeCmd(device, sendRtpItem.getChannelId(), audioBroadcastCatch.getSipTransactionInfo(), null);
                    } catch (InvalidArgumentException | ParseException | SipException |
                             SsrcTransactionNotFoundException e) {
                        logger.error("[消息发送失败] 发送语音喊话BYE失败");
                    }
                }

                audioBroadcastManager.del(deviceId, channelId);
            }
        }
    }

    @Override
    public void zlmServerOnline(String mediaServerId) {
        // TODO 查找之前的点播，流如果不存在则给下级发送bye
//        MediaServerItem mediaServerItem = mediaServerService.getOne(mediaServerId);
//        zlmresTfulUtils.getMediaList(mediaServerItem, (mediaList ->{
//            Integer code = mediaList.getInteger("code");
//            if (code == 0) {
//                JSONArray data = mediaList.getJSONArray("data");
//                if (data == null || data.size() == 0) {
//                    zlmServerOffline(mediaServerId);
//                }else {
//                    Map<String, JSONObject> mediaListMap = new HashMap<>();
//                    for (int i = 0; i < data.size(); i++) {
//                        JSONObject json = data.getJSONObject(i);
//                        String app = json.getString("app");
//                        if ("rtp".equals(app)) {
//                            String stream = json.getString("stream");
//                            if (mediaListMap.get(stream) != null) {
//                                continue;
//                            }
//                            mediaListMap.put(stream, json);
//                            // 处理正在观看的国标设备
//                            List<SsrcTransaction> ssrcTransactions = streamSession.getSsrcTransactionForAll(null, null, null, stream);
//                            if (ssrcTransactions.size() > 0) {
//                                for (SsrcTransaction ssrcTransaction : ssrcTransactions) {
//                                    if(ssrcTransaction.getMediaServerId().equals(mediaServerId)) {
//                                        cmder.streamByeCmd(ssrcTransaction.getDeviceId(), ssrcTransaction.getChannelId(),
//                                                ssrcTransaction.getStream(), null);
//                                    }
//                                }
//                            }
//                        }
//                    }
//                    if (mediaListMap.size() > 0 ) {
//                        // 处理正在向上推流的上级平台
//                        List<SendRtpItem> sendRtpItems = redisCatchStorage.querySendRTPServer(null);
//                        if (sendRtpItems.size() > 0) {
//                            for (SendRtpItem sendRtpItem : sendRtpItems) {
//                                if (sendRtpItem.getMediaServerId().equals(mediaServerId)) {
//                                    if (mediaListMap.get(sendRtpItem.getStreamId()) == null) {
//                                        ParentPlatform platform = storager.queryPlatformByServerGBId(sendRtpItem.getPlatformId());
//                                        sipCommanderFroPlatform.streamByeCmd(platform, sendRtpItem.getCallId());
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }));
    }

    @Override
    public void pauseRtp(String streamId) throws ServiceException, InvalidArgumentException, ParseException, SipException {
        String key = redisCatchStorage.queryPlaybackForKey(null, null, streamId, null);
        StreamInfo streamInfo = redisCatchStorage.queryPlayback(null, null, streamId, null);
        if (null == streamInfo) {
            logger.warn("streamId不存在!");
            throw new ServiceException("streamId不存在");
        }
        streamInfo.setPause(true);
        RedisUtil.set(key, streamInfo);
        MediaServerItem mediaServerItem = mediaServerService.getOne(streamInfo.getMediaServerId());
        if (null == mediaServerItem) {
            logger.warn("mediaServer 不存在!");
            throw new ServiceException("mediaServer不存在");
        }
        // zlm 暂停RTP超时检查
        JSONObject jsonObject = zlmresTfulUtils.pauseRtpCheck(mediaServerItem, streamId);
        if (jsonObject == null || jsonObject.getInteger("code") != 0) {
            throw new ServiceException("暂停RTP接收失败");
        }
        Device device = storager.queryVideoDevice(streamInfo.getDeviceID());
        cmder.playPauseCmd(device, streamInfo);
    }

    @Override
    public void resumeRtp(String streamId) throws ServiceException, InvalidArgumentException, ParseException, SipException {
        String key = redisCatchStorage.queryPlaybackForKey(null, null, streamId, null);
        StreamInfo streamInfo = redisCatchStorage.queryPlayback(null, null, streamId, null);
        if (null == streamInfo) {
            logger.warn("streamId不存在!");
            throw new ServiceException("streamId不存在");
        }
        streamInfo.setPause(false);
        RedisUtil.set(key, streamInfo);
        MediaServerItem mediaServerItem = mediaServerService.getOne(streamInfo.getMediaServerId());
        if (null == mediaServerItem) {
            logger.warn("mediaServer 不存在!");
            throw new ServiceException("mediaServer不存在");
        }
        // zlm 暂停RTP超时检查
        JSONObject jsonObject = zlmresTfulUtils.resumeRtpCheck(mediaServerItem, streamId);
        if (jsonObject == null || jsonObject.getInteger("code") != 0) {
            throw new ServiceException("继续RTP接收失败");
        }
        Device device = storager.queryVideoDevice(streamInfo.getDeviceID());
        cmder.playResumeCmd(device, streamInfo);
    }

    @Override
    public void startPushStream(SendRtpItem sendRtpItem, SIPResponse sipResponse, ParentPlatform platform, CallIdHeader callIdHeader) {

        // 开始发流
        // 取消设置的超时任务
//			String channelId = request.getCallIdHeader().getCallId();

        String is_Udp = sendRtpItem.isTcp() ? "0" : "1";
        MediaServerItem mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
        logger.info("收到ACK，rtp/{}开始向上级推流, 目标={}:{}，SSRC={}, RTCP={}", sendRtpItem.getStream(),
                sendRtpItem.getIp(), sendRtpItem.getPort(), sendRtpItem.getSsrc(), sendRtpItem.isRtcp());
        Map<String, Object> param = new HashMap<>(12);
        param.put("vhost", "__defaultVhost__");
        param.put("app", sendRtpItem.getApp());
        param.put("stream", sendRtpItem.getStream());
        param.put("ssrc", sendRtpItem.getSsrc());
        param.put("src_port", sendRtpItem.getLocalPort());
        param.put("pt", sendRtpItem.getPt());
        param.put("use_ps", sendRtpItem.isUsePs() ? "1" : "0");
        param.put("only_audio", sendRtpItem.isOnlyAudio() ? "1" : "0");
        param.put("is_udp", is_Udp);
        if (!sendRtpItem.isTcp()) {
            // udp模式下开启rtcp保活
            param.put("udp_rtcp_timeout", sendRtpItem.isRtcp() ? "1" : "0");
        }

        if (mediaInfo == null) {
            RequestPushStreamMsg requestPushStreamMsg = RequestPushStreamMsg.getInstance(
                    sendRtpItem.getMediaServerId(), sendRtpItem.getApp(), sendRtpItem.getStream(),
                    sendRtpItem.getIp(), sendRtpItem.getPort(), sendRtpItem.getSsrc(), sendRtpItem.isTcp(),
                    sendRtpItem.getLocalPort(), sendRtpItem.getPt(), sendRtpItem.isUsePs(), sendRtpItem.isOnlyAudio());
            redisGbPlayMsgListener.sendMsgForStartSendRtpStream(sendRtpItem.getServerId(), requestPushStreamMsg, json -> {
                startSendRtpStreamHand(sendRtpItem, platform, json, param, callIdHeader);
            });
        } else {
            // 如果是非严格模式，需要关闭端口占用
            JSONObject startSendRtpStreamResult = null;
            if (sendRtpItem.getLocalPort() != 0) {
                HookSubscribeForRtpServerTimeout hookSubscribeForRtpServerTimeout = HookSubscribeFactory.on_rtp_server_timeout(sendRtpItem.getSsrc(), null, mediaInfo.getId());
                hookSubscribe.removeSubscribe(hookSubscribeForRtpServerTimeout);
                if (zlmrtpServerFactory.releasePort(mediaInfo, sendRtpItem.getSsrc())) {
                    if (sendRtpItem.isTcpActive()) {
                        startSendRtpStreamResult = zlmrtpServerFactory.startSendRtpPassive(mediaInfo, param);
                    } else {
                        param.put("dst_url", sendRtpItem.getIp());
                        param.put("dst_port", sendRtpItem.getPort());
                        startSendRtpStreamResult = zlmrtpServerFactory.startSendRtpStream(mediaInfo, param);
                    }
                }
            } else {
                if (sendRtpItem.isTcpActive()) {
                    startSendRtpStreamResult = zlmrtpServerFactory.startSendRtpPassive(mediaInfo, param);
                } else {
                    param.put("dst_url", sendRtpItem.getIp());
                    param.put("dst_port", sendRtpItem.getPort());
                    startSendRtpStreamResult = zlmrtpServerFactory.startSendRtpStream(mediaInfo, param);
                }
            }
            if (startSendRtpStreamResult != null) {
                startSendRtpStreamHand(sendRtpItem, platform, startSendRtpStreamResult, param, callIdHeader);
            }
        }
    }

    @Override
    public void startSendRtpStreamHand(SendRtpItem sendRtpItem, ParentPlatform parentPlatform,
                                       JSONObject jsonObject, Map<String, Object> param, CallIdHeader callIdHeader) {
        if (jsonObject == null) {
            logger.error("RTP推流失败: 请检查ZLM服务");
        } else if (jsonObject.getInteger("code") == 0) {
            logger.info("调用ZLM推流接口, 结果： {}", jsonObject);
            logger.info("RTP推流成功[ {}/{} ]，{}->{}:{}, ", param.get("app"), param.get("stream"), jsonObject.getString("local_port"), param.get("dst_url"), param.get("dst_port"));
        } else {
            logger.error("RTP推流失败: {}, 参数：{}", jsonObject.getString("msg"), JSON.toJSONString(param));
            if (sendRtpItem.isOnlyAudio()) {
                Device device = deviceService.getDevice(sendRtpItem.getDeviceId());
                AudioBroadcastCatch audioBroadcastCatch = audioBroadcastManager.get(sendRtpItem.getDeviceId(), sendRtpItem.getChannelId());
                if (audioBroadcastCatch != null) {
                    try {
                        cmder.streamByeCmd(device, sendRtpItem.getChannelId(), audioBroadcastCatch.getSipTransactionInfo(), null);
                    } catch (SipException | ParseException | InvalidArgumentException |
                             SsrcTransactionNotFoundException e) {
                        logger.error("[命令发送失败] 停止语音对讲: {}", e.getMessage());
                    }
                }
            } else {
                // 向上级平台
                try {
                    commanderForPlatform.streamByeCmd(parentPlatform, callIdHeader.getCallId());
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    logger.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
                }
            }
        }
    }

    @Override
    public void talkCmd(Device device, String channelId, MediaServerItem mediaServerItem, String stream, AudioEvent event) {
        if (device == null || channelId == null) {
            return;
        }
        // TODO 必须多端口模式才支持语音喊话鹤语音对讲
        logger.info("[语音对讲] device： {}, channel: {}", device.getDeviceId(), channelId);
        DeviceChannel deviceChannel = storager.queryChannel(device.getDeviceId(), channelId);
        if (deviceChannel == null) {
            logger.warn("开启语音对讲的时候未找到通道： {}", channelId);
            event.call("开启语音对讲的时候未找到通道");
            return;
        }
        // 查询通道使用状态
        if (audioBroadcastManager.exit(device.getDeviceId(), channelId)) {
            SendRtpItem sendRtpItem = redisCatchStorage.querySendRTPServer(device.getDeviceId(), channelId, null, null);
            if (sendRtpItem != null && sendRtpItem.isOnlyAudio()) {
                // 查询流是否存在，不存在则认为是异常状态
                MediaServerItem mediaServer = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                Boolean streamReady = zlmrtpServerFactory.isStreamReady(mediaServer, sendRtpItem.getApp(), sendRtpItem.getStream());
                if (streamReady) {
                    logger.warn("[语音对讲] 正在语音广播，无法开启语音通话： {}", channelId);
                    event.call("正在语音广播");
                    return;
                } else {
                    stopAudioBroadcast(device.getDeviceId(), channelId);
                }
            }
        }

        SendRtpItem sendRtpItem = redisCatchStorage.querySendRTPServer(device.getDeviceId(), channelId, stream, null);
        if (sendRtpItem != null) {
            MediaServerItem mediaServer = mediaServerService.getOne(sendRtpItem.getMediaServerId());
            Boolean streamReady = zlmrtpServerFactory.isStreamReady(mediaServer, "rtp", sendRtpItem.getReceiveStream());
            if (streamReady) {
                logger.warn("[语音对讲] 进行中： {}", channelId);
                event.call("语音对讲进行中");
                return;
            } else {
                stopTalk(device, channelId);
            }
        }

        talk(mediaServerItem, device, channelId, stream, (MediaServerItem mediaServerItem1, JSONObject response) -> {
            logger.info("[语音对讲] 收到设备发来的流");
        }, eventResult -> {
            logger.warn("[语音对讲] 失败，{}/{}, 错误码 {} {}", device.getDeviceId(), channelId, eventResult.statusCode, eventResult.msg);
            event.call("失败，错误码 " + eventResult.statusCode + ", " + eventResult.msg);
        }, () -> {
            logger.warn("[语音对讲] 失败，{}/{} 超时", device.getDeviceId(), channelId);
            event.call("失败，超时 ");
            stopTalk(device, channelId);
        }, errorMsg -> {
            logger.warn("[语音对讲] 失败，{}/{} {}", device.getDeviceId(), channelId, errorMsg);
            event.call(errorMsg);
            stopTalk(device, channelId);
        });
    }

    private void stopTalk(Device device, String channelId) {
        stopTalk(device, channelId, null);
    }

    @Override
    public void stopTalk(Device device, String channelId, Boolean streamIsReady) {
        logger.info("[语音对讲] 停止， {}/{}", device.getDeviceId(), channelId);
        SendRtpItem sendRtpItem = redisCatchStorage.querySendRTPServer(device.getDeviceId(), channelId, null, null);
        if (sendRtpItem == null) {
            logger.info("[语音对讲] 停止失败， 未找到发送信息，可能已经停止");
            return;
        }
        // 停止向设备推流
        String mediaServerId = sendRtpItem.getMediaServerId();
        if (mediaServerId == null) {
            return;
        }

        MediaServerItem mediaServer = mediaServerService.getOne(mediaServerId);

        if (streamIsReady == null || streamIsReady) {
            Map<String, Object> param = new HashMap<>();
            param.put("vhost", "__defaultVhost__");
            param.put("app", sendRtpItem.getApp());
            param.put("stream", sendRtpItem.getStream());
            param.put("ssrc", sendRtpItem.getSsrc());
            zlmrtpServerFactory.stopSendRtpStream(mediaServer, param);
        }

        mediaServer.getSsrcConfig().releaseSsrc(sendRtpItem.getSsrc());

        SsrcTransaction ssrcTransaction = streamSession.getSsrcTransaction(device.getDeviceId(), channelId, null, sendRtpItem.getStream());
        if (ssrcTransaction != null) {
            try {
                cmder.streamByeCmd(device, channelId, sendRtpItem.getStream(), null);
            } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException  e) {
                logger.info("[语音对讲] 停止消息发送失败，可能已经停止");
            }
        }
        redisCatchStorage.deleteSendRTPServer(device.getDeviceId(), channelId,null, null);
    }
}
