package com.genersoft.iot.vmp.service.redisMsg.control;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.redis.RedisRpcConfig;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcMessage;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcRequest;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcResponse;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.hook.Hook;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.hook.HookType;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.SendRtpPortManager;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;

/**
 * 其他wvp发起的rpc调用，这里的方法被 RedisRpcConfig 通过反射寻找对应的方法名称调用
 */
@Slf4j
@Component
public class RedisRpcController {

    @Autowired
    private SSRCFactory ssrcFactory;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private SendRtpPortManager sendRtpPortManager;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private HookSubscribe hookSubscribe;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;


    @Autowired
    private ISIPCommanderForPlatform commanderFroPlatform;


    @Autowired
    private IVideoManagerStorage storager;


    /**
     * 获取发流的信息
     */
    public RedisRpcResponse getSendRtpItem(RedisRpcRequest request) {
        String sendRtpItemKey = request.getParam().toString();
        SendRtpItem sendRtpItem = (SendRtpItem) redisTemplate.opsForValue().get(sendRtpItemKey);
        if (sendRtpItem == null) {
            log.info("[redis-rpc] 获取发流的信息, 未找到redis中的发流信息， key：{}", sendRtpItemKey);
            RedisRpcResponse response = request.getResponse();
            response.setStatusCode(200);
            return response;
        }
        log.info("[redis-rpc] 获取发流的信息： {}/{}, 目标地址： {}：{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort());
        // 查询本级是否有这个流
        MediaServer mediaServerItem = mediaServerService.getMediaServerByAppAndStream(sendRtpItem.getApp(), sendRtpItem.getStream());
        if (mediaServerItem == null) {
            RedisRpcResponse response = request.getResponse();
            response.setStatusCode(200);
        }
        // 自平台内容
        int localPort = sendRtpPortManager.getNextPort(mediaServerItem);
        if (localPort == 0) {
            log.info("[redis-rpc] getSendRtpItem->服务器端口资源不足" );
            RedisRpcResponse response = request.getResponse();
            response.setStatusCode(200);
        }
        // 写入redis， 超时时回复
        sendRtpItem.setStatus(1);
        sendRtpItem.setServerId(userSetting.getServerId());
        sendRtpItem.setLocalIp(mediaServerItem.getSdpIp());
        if (sendRtpItem.getSsrc() == null) {
            // 上级平台点播时不使用上级平台指定的ssrc，使用自定义的ssrc，参考国标文档-点播外域设备媒体流SSRC处理方式
            String ssrc = "Play".equalsIgnoreCase(sendRtpItem.getSessionName()) ? ssrcFactory.getPlaySsrc(mediaServerItem.getId()) : ssrcFactory.getPlayBackSsrc(mediaServerItem.getId());
            sendRtpItem.setSsrc(ssrc);
        }
        redisCatchStorage.updateSendRTPSever(sendRtpItem);
        redisTemplate.opsForValue().set(sendRtpItemKey, sendRtpItem);
        RedisRpcResponse response = request.getResponse();
        response.setStatusCode(200);
        response.setBody(sendRtpItemKey);
        return response;
    }

    /**
     * 监听流上线
     */
    public RedisRpcResponse waitePushStreamOnline(RedisRpcRequest request) {
        SendRtpItem sendRtpItem = JSONObject.parseObject(request.getParam().toString(), SendRtpItem.class);
        log.info("[redis-rpc] 监听流上线： {}/{}, 目标地址： {}：{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort());
        // 查询本级是否有这个流
        MediaServer mediaServer = mediaServerService.getMediaServerByAppAndStream(sendRtpItem.getApp(), sendRtpItem.getStream());
        if (mediaServer != null) {
            log.info("[redis-rpc] 监听流上线时发现流已存在直接返回： {}/{}, 目标地址： {}：{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort() );
            // 读取redis中的上级点播信息，生成sendRtpItm发送出去
            if (sendRtpItem.getSsrc() == null) {
                // 上级平台点播时不使用上级平台指定的ssrc，使用自定义的ssrc，参考国标文档-点播外域设备媒体流SSRC处理方式
                String ssrc = "Play".equalsIgnoreCase(sendRtpItem.getSessionName()) ? ssrcFactory.getPlaySsrc(mediaServer.getId()) : ssrcFactory.getPlayBackSsrc(mediaServer.getId());
                sendRtpItem.setSsrc(ssrc);
            }
            sendRtpItem.setMediaServerId(mediaServer.getId());
            sendRtpItem.setLocalIp(mediaServer.getSdpIp());
            sendRtpItem.setServerId(userSetting.getServerId());

            redisTemplate.opsForValue().set(sendRtpItem.getRedisKey(), sendRtpItem);
            RedisRpcResponse response = request.getResponse();
            response.setBody(sendRtpItem.getRedisKey());
            response.setStatusCode(200);
        }
        // 监听流上线。 流上线直接发送sendRtpItem消息给实际的信令处理者
        Hook hook = Hook.getInstance(HookType.on_media_arrival, sendRtpItem.getApp(), sendRtpItem.getStream(), null);
        hookSubscribe.addSubscribe(hook, (hookData) -> {
            log.info("[redis-rpc] 监听流上线，流已上线： {}/{}, 目标地址： {}：{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort());
            // 读取redis中的上级点播信息，生成sendRtpItm发送出去
            if (sendRtpItem.getSsrc() == null) {
                // 上级平台点播时不使用上级平台指定的ssrc，使用自定义的ssrc，参考国标文档-点播外域设备媒体流SSRC处理方式
                String ssrc = "Play".equalsIgnoreCase(sendRtpItem.getSessionName()) ? ssrcFactory.getPlaySsrc(hookData.getMediaServer().getId()) : ssrcFactory.getPlayBackSsrc(hookData.getMediaServer().getId());
                sendRtpItem.setSsrc(ssrc);
            }
            sendRtpItem.setMediaServerId(hookData.getMediaServer().getId());
            sendRtpItem.setLocalIp(hookData.getMediaServer().getSdpIp());
            sendRtpItem.setServerId(userSetting.getServerId());

            redisTemplate.opsForValue().set(sendRtpItem.getRedisKey(), sendRtpItem);
            RedisRpcResponse response = request.getResponse();
            response.setBody(sendRtpItem.getRedisKey());
            response.setStatusCode(200);
            // 手动发送结果
            sendResponse(response);
            hookSubscribe.removeSubscribe(hook);

        });
        return null;
    }

    /**
     * 监听流上线
     */
    public RedisRpcResponse onStreamOnlineEvent(RedisRpcRequest request) {
        StreamInfo streamInfo = JSONObject.parseObject(request.getParam().toString(), StreamInfo.class);
        log.info("[redis-rpc] 监听流信息，等待流上线： {}/{}", streamInfo.getApp(), streamInfo.getStream());
        // 查询本级是否有这个流
        StreamInfo streamInfoInServer = mediaServerService.getMediaByAppAndStream(streamInfo.getApp(), streamInfo.getStream());
        if (streamInfoInServer != null) {
            log.info("[redis-rpc] 监听流上线时发现流已存在直接返回： {}/{}", streamInfo.getApp(), streamInfo.getStream());
            RedisRpcResponse response = request.getResponse();
            response.setBody(JSONObject.toJSONString(streamInfoInServer));
            response.setStatusCode(200);
            return response;
        }
        // 监听流上线。 流上线直接发送sendRtpItem消息给实际的信令处理者
        Hook hook = Hook.getInstance(HookType.on_media_arrival, streamInfo.getApp(), streamInfo.getStream());
        hookSubscribe.addSubscribe(hook, (hookData) -> {
            log.info("[redis-rpc] 监听流上线，流已上线： {}/{}", streamInfo.getApp(), streamInfo.getStream());
            // 读取redis中的上级点播信息，生成sendRtpItm发送出去
            RedisRpcResponse response = request.getResponse();
            StreamInfo streamInfoByAppAndStream = mediaServerService.getStreamInfoByAppAndStream(hookData.getMediaServer(),
                    streamInfo.getApp(), streamInfo.getStream(), hookData.getMediaInfo(),
                    hookData.getMediaInfo() != null ? hookData.getMediaInfo().getCallId() : null);
            response.setBody(JSONObject.toJSONString(streamInfoByAppAndStream));
            response.setStatusCode(200);
            // 手动发送结果
            sendResponse(response);
            hookSubscribe.removeSubscribe(hook);
        });
        return null;
    }

    /**
     * 停止监听流上线
     */
    public RedisRpcResponse stopWaitePushStreamOnline(RedisRpcRequest request) {
        SendRtpItem sendRtpItem = JSONObject.parseObject(request.getParam().toString(), SendRtpItem.class);
        log.info("[redis-rpc] 停止监听流上线： {}/{}, 目标地址： {}：{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort() );
        // 监听流上线。 流上线直接发送sendRtpItem消息给实际的信令处理者
        Hook hook = Hook.getInstance(HookType.on_media_arrival, sendRtpItem.getApp(), sendRtpItem.getStream(), null);
        hookSubscribe.removeSubscribe(hook);
        RedisRpcResponse response = request.getResponse();
        response.setStatusCode(200);
        return response;
    }

    /**
     * 停止监听流上线
     */
    public RedisRpcResponse unPushStreamOnlineEvent(RedisRpcRequest request) {
        StreamInfo streamInfo = JSONObject.parseObject(request.getParam().toString(), StreamInfo.class);
        log.info("[redis-rpc] 停止监听流上线： {}/{}", streamInfo.getApp(), streamInfo.getStream());
        // 监听流上线。 流上线直接发送sendRtpItem消息给实际的信令处理者
        Hook hook = Hook.getInstance(HookType.on_media_arrival, streamInfo.getApp(), streamInfo.getStream(), null);
        hookSubscribe.removeSubscribe(hook);
        RedisRpcResponse response = request.getResponse();
        response.setStatusCode(200);
        return response;
    }


    /**
     * 开始发流
     */
    public RedisRpcResponse startSendRtp(RedisRpcRequest request) {
        String sendRtpItemKey = request.getParam().toString();
        SendRtpItem sendRtpItem = (SendRtpItem) redisTemplate.opsForValue().get(sendRtpItemKey);
        RedisRpcResponse response = request.getResponse();
        response.setStatusCode(200);
        if (sendRtpItem == null) {
            log.info("[redis-rpc] 开始发流, 未找到redis中的发流信息， key：{}", sendRtpItemKey);
            WVPResult wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "未找到redis中的发流信息");
            response.setBody(wvpResult);
            return response;
        }
        log.info("[redis-rpc] 开始发流： {}/{}, 目标地址： {}：{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort());
        MediaServer mediaServer = mediaServerService.getOne(sendRtpItem.getMediaServerId());
        if (mediaServer == null) {
            log.info("[redis-rpc] startSendRtp->未找到MediaServer： {}", sendRtpItem.getMediaServerId() );
            WVPResult wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "未找到MediaServer");
            response.setBody(wvpResult);
            return response;
        }
        MediaInfo mediaInfo = mediaServerService.getMediaInfo(mediaServer, sendRtpItem.getApp(), sendRtpItem.getStream());
        if (mediaInfo != null) {
            log.info("[redis-rpc] startSendRtp->流不在线： {}/{}", sendRtpItem.getApp(), sendRtpItem.getStream() );
            WVPResult wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "流不在线");
            response.setBody(wvpResult);
            return response;
        }
        try {
            mediaServerService.startSendRtp(mediaServer, sendRtpItem);
        }catch (ControllerException exception) {
            log.info("[redis-rpc] 发流失败： {}/{}, 目标地址： {}：{}， {}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort(), exception.getMsg());
            WVPResult wvpResult = WVPResult.fail(exception.getCode(), exception.getMsg());
            response.setBody(wvpResult);
            return response;
        }
        log.info("[redis-rpc] 发流成功： {}/{}, 目标地址： {}：{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort());
        WVPResult wvpResult = WVPResult.success();
        response.setBody(wvpResult);
        return response;
    }

    /**
     * 停止发流
     */
    public RedisRpcResponse stopSendRtp(RedisRpcRequest request) {
        String sendRtpItemKey = request.getParam().toString();
        SendRtpItem sendRtpItem = (SendRtpItem) redisTemplate.opsForValue().get(sendRtpItemKey);
        RedisRpcResponse response = request.getResponse();
        response.setStatusCode(200);
        if (sendRtpItem == null) {
            log.info("[redis-rpc] 停止推流, 未找到redis中的发流信息， key：{}", sendRtpItemKey);
            WVPResult wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "未找到redis中的发流信息");
            response.setBody(wvpResult);
            return response;
        }
        log.info("[redis-rpc] 停止推流： {}/{}, 目标地址： {}：{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort() );
        MediaServer mediaServer = mediaServerService.getOne(sendRtpItem.getMediaServerId());
        if (mediaServer == null) {
            log.info("[redis-rpc] stopSendRtp->未找到MediaServer： {}", sendRtpItem.getMediaServerId() );
            WVPResult wvpResult = WVPResult.fail(ErrorCode.ERROR100.getCode(), "未找到MediaServer");
            response.setBody(wvpResult);
            return response;
        }
        try {
            mediaServerService.stopSendRtp(mediaServer, sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getSsrc());
        }catch (ControllerException exception) {
            log.info("[redis-rpc] 停止推流失败： {}/{}, 目标地址： {}：{}， code： {}, msg: {}", sendRtpItem.getApp(),
                    sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort(), exception.getCode(), exception.getMsg() );
            response.setBody(WVPResult.fail(exception.getCode(), exception.getMsg()));
            return response;
        }
        log.info("[redis-rpc] 停止推流成功： {}/{}, 目标地址： {}：{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort() );
        response.setBody(WVPResult.success());
        return response;
    }

    /**
     * 其他wvp通知推流已经停止了
     */
    public RedisRpcResponse rtpSendStopped(RedisRpcRequest request) {
        String sendRtpItemKey = request.getParam().toString();
        SendRtpItem sendRtpItem = (SendRtpItem) redisTemplate.opsForValue().get(sendRtpItemKey);
        RedisRpcResponse response = request.getResponse();
        response.setStatusCode(200);
        if (sendRtpItem == null) {
            log.info("[redis-rpc] 推流已经停止, 未找到redis中的发流信息， key：{}", sendRtpItemKey);
            return response;
        }
        log.info("[redis-rpc] 推流已经停止： {}/{}, 目标地址： {}：{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort() );
        String platformId = sendRtpItem.getPlatformId();
        Platform platform = storager.queryParentPlatByServerGBId(platformId);
        if (platform == null) {
            return response;
        }
        try {
            commanderFroPlatform.streamByeCmd(platform, sendRtpItem);
            redisCatchStorage.deleteSendRTPServer(platformId, sendRtpItem.getChannelId(),
                    sendRtpItem.getCallId(), sendRtpItem.getStream());
            redisCatchStorage.sendPlatformStopPlayMsg(sendRtpItem, platform);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 发送BYE: {}", e.getMessage());
        }
        return response;
    }

    private void sendResponse(RedisRpcResponse response){
        log.info("[redis-rpc] >> {}", response);
        response.setToId(userSetting.getServerId());
        RedisRpcMessage message = new RedisRpcMessage();
        message.setResponse(response);
        redisTemplate.convertAndSend(RedisRpcConfig.REDIS_REQUEST_CHANNEL_KEY, message);
    }
}
