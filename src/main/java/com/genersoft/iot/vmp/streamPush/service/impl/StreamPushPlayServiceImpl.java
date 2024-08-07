package com.genersoft.iot.vmp.streamPush.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.hook.Hook;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.hook.HookType;
import com.genersoft.iot.vmp.media.event.media.MediaArrivalEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.service.bean.MessageForPushChannel;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.streamPush.bean.StreamPush;
import com.genersoft.iot.vmp.streamPush.dao.StreamPushMapper;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushPlayService;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.UUID;

@Service
@Slf4j
@DS("master")
public class StreamPushPlayServiceImpl implements IStreamPushPlayService {

    @Autowired
    private StreamPushMapper streamPushMapper;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private UserSetting userSetting;
    
    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private HookSubscribe subscribe;

    @Override
    public void start(Integer id, CommonCallback<StreamInfo> callback) {
        StreamPush streamPush = streamPushMapper.queryOne(id);
        Assert.notNull(streamPush, "推流信息未找到");
//        if (streamPush.isPushing() && streamPush.getMediaServerId() != null) {
//            // 检查流是否准备就绪
//            MediaServer mediaServer = mediaServerService.getOne(streamPush.getMediaServerId());
//            if (mediaServer != null) {
//                Boolean streamReady = mediaServerService.isStreamReady(mediaServer, streamPush.getApp(), streamPush.getStream());
//                if (streamReady != null && streamReady) {
//                    String callId = null;
//                    StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(streamPush.getApp(), streamPush.getStream());
//                    if (streamAuthorityInfo != null) {
//                        callId = streamAuthorityInfo.getCallId();
//                    }
//                    callback.run(mediaServerService.getStreamInfoByAppAndStream(mediaServer,
//                            streamPush.getApp(), streamPush.getStream(), null, callId));
//                    return;
//                }
//            }
//        }
//        Assert.isTrue(streamPush.isAutoPushChannel(), "通道未推流");
//        // 发送redis消息，通知流上线
//        String timeOutTaskKey = UUID.randomUUID().toString();
//        Hook rtpHook = Hook.getInstance(HookType.on_media_arrival, streamPush.getApp(), streamPush.getStream(), null);
//        // 开启流上线监听
//        subscribe.addSubscribe(rtpHook, (hookData) -> {
//            dynamicTask.stop(timeOutTaskKey);
//            subscribe.removeSubscribe(rtpHook);
//            if (hookData == null) {
//                return;
//            }
//            String callId = null;
//            StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(streamPush.getApp(), streamPush.getStream());
//            if (streamAuthorityInfo != null) {
//                callId = streamAuthorityInfo.getCallId();
//            }
//            callback.run(mediaServerService.getStreamInfoByAppAndStream(hookData.getMediaServer(),
//                    streamPush.getApp(), streamPush.getStream(), null, callId));
//        });
//        // 设置超时事件
//        dynamicTask.startDelay(timeOutTaskKey, () -> {
//            // 取消流监听
//            subscribe.removeSubscribe(rtpHook);
//        }, userSetting.getPlayTimeout());
        // 发送redis消息， 同时监听可能返回的拒绝消息


        MediaArrivalEvent pushListItem = redisCatchStorage.getPushListItem(streamPush.getApp(), streamPush.getStream());
        if (pushListItem != null) {
            String callId = null;
            StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(streamPush.getApp(), streamPush.getStream());
            if (streamAuthorityInfo != null) {
                callId = streamAuthorityInfo.getCallId();
            }
            callback.run(mediaServerService.getStreamInfoByAppAndStream(pushListItem.getMediaServer(),
                    streamPush.getApp(), streamPush.getStream(), null, callId));
            return;
        }
        Assert.isTrue(streamPush.isAutoPushChannel(), "通道未推流");
        // 发送redis消息以使设备上线，流上线后被
        log.info("[ app={}, stream={} ]通道未推流，发送redis信息控制设备开始推流", streamPush.getApp(), streamPush.getStream());
        MessageForPushChannel messageForPushChannel = MessageForPushChannel.getInstance(1,
                streamPush.getApp(), streamPush.getStream(), sendRtpItem.getChannelId(), sendRtpItem.getPlatformId(),
                platform.getName(), userSetting.getServerId(), sendRtpItem.getMediaServerId());
        redisCatchStorage.sendStreamPushRequestedMsg(messageForPushChannel);
        // 设置超时
        dynamicTask.startDelay(sendRtpItem.getCallId(), () -> {
            redisRpcService.stopWaitePushStreamOnline(sendRtpItem);
            log.info("[ app={}, stream={} ] 等待设备开始推流超时", streamPush.getApp(), streamPush.getStream());
            try {
                responseAck(request, Response.REQUEST_TIMEOUT); // 超时
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("未处理的异常 ", e);
            }
        }, userSetting.getPlatformPlayTimeout());
        //
        long key = redisRpcService.waitePushStreamOnline(sendRtpItem, (sendRtpItemKey) -> {
            dynamicTask.stop(sendRtpItem.getCallId());
            if (sendRtpItemKey == null) {
                log.warn("[级联点播] 等待推流得到结果未空： {}/{}", streamPush.getApp(), streamPush.getStream());
                try {
                    responseAck(request, Response.BUSY_HERE);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("未处理的异常 ", e);
                }
                return;
            }
            SendRtpItem sendRtpItemFromRedis = (SendRtpItem)redisTemplate.opsForValue().get(sendRtpItemKey);
            if (sendRtpItemFromRedis == null) {
                log.warn("[级联点播] 等待推流, 未找到redis中缓存的发流信息： {}/{}", streamPush.getApp(), streamPush.getStream());
                try {
                    responseAck(request, Response.BUSY_HERE);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("未处理的异常 ", e);
                }
                return;
            }
            if (sendRtpItemFromRedis.getServerId().equals(userSetting.getServerId())) {
                log.info("[级联点播] 等待的推流在本平台上线 {}/{}", streamPush.getApp(), streamPush.getStream());
                int localPort = sendRtpPortManager.getNextPort(mediaServerItem);
                if (localPort == 0) {
                    log.warn("上级点时创建sendRTPItem失败，可能是服务器端口资源不足");
                    try {
                        responseAck(request, Response.BUSY_HERE);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        log.error("未处理的异常 ", e);
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
        // redis消息例如： PUBLISH VM_MSG_STREAM_PUSH_RESPONSE  '{"code":1,"msg":"失败","app":"1","stream":"2"}'
        redisPushStreamResponseListener.addEvent(streamPush.getApp(), streamPush.getStream(), response -> {
            if (response.getCode() != 0) {
                dynamicTask.stop(sendRtpItem.getCallId());
                redisRpcService.stopWaitePushStreamOnline(sendRtpItem);
                redisRpcService.removeCallback(key);
                try {
                    responseAck(request, Response.TEMPORARILY_UNAVAILABLE, response.getMsg());
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] 国标级联 点播回复: {}", e.getMessage());
                }
            }
        });
        
    }
}
