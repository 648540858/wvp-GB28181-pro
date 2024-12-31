package com.genersoft.iot.vmp.service.redisMsg.control;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.redis.RedisRpcConfig;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcMessage;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcRequest;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcResponse;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpInfo;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.hook.Hook;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.hook.HookType;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.service.redisMsg.dto.RedisRpcController;
import com.genersoft.iot.vmp.service.redisMsg.dto.RedisRpcMapping;
import com.genersoft.iot.vmp.service.redisMsg.dto.RpcController;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushPlayService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RedisRpcController("streamPush")
public class RedisRpcStreamPushController extends RpcController {

    @Autowired
    private SSRCFactory ssrcFactory;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private ISendRtpServerService sendRtpServerService;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private HookSubscribe hookSubscribe;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private IStreamPushPlayService streamPushPlayService;


    private void sendResponse(RedisRpcResponse response){
        log.info("[redis-rpc] >> {}", response);
        response.setToId(userSetting.getServerId());
        RedisRpcMessage message = new RedisRpcMessage();
        message.setResponse(response);
        redisTemplate.convertAndSend(RedisRpcConfig.REDIS_REQUEST_CHANNEL_KEY, message);
    }

    /**
     * 监听流上线
     */
    @RedisRpcMapping("waitePushStreamOnline")
    public RedisRpcResponse waitePushStreamOnline(RedisRpcRequest request) {
        SendRtpInfo sendRtpItem = JSONObject.parseObject(request.getParam().toString(), SendRtpInfo.class);
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

            sendRtpServerService.update(sendRtpItem);
            RedisRpcResponse response = request.getResponse();
            response.setBody(sendRtpItem.getChannelId());
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
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

            redisTemplate.opsForValue().set(sendRtpItem.getChannelId(), sendRtpItem);
            RedisRpcResponse response = request.getResponse();
            response.setBody(sendRtpItem.getChannelId());
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
            // 手动发送结果
            sendResponse(response);
            hookSubscribe.removeSubscribe(hook);

        });
        return null;
    }

    /**
     * 监听流上线
     */
    @RedisRpcMapping("onStreamOnlineEvent")
    public RedisRpcResponse onStreamOnlineEvent(RedisRpcRequest request) {
        StreamInfo streamInfo = JSONObject.parseObject(request.getParam().toString(), StreamInfo.class);
        log.info("[redis-rpc] 监听流信息，等待流上线： {}/{}", streamInfo.getApp(), streamInfo.getStream());
        // 查询本级是否有这个流
        StreamInfo streamInfoInServer = mediaServerService.getMediaByAppAndStream(streamInfo.getApp(), streamInfo.getStream());
        if (streamInfoInServer != null) {
            log.info("[redis-rpc] 监听流上线时发现流已存在直接返回： {}/{}", streamInfo.getApp(), streamInfo.getStream());
            RedisRpcResponse response = request.getResponse();
            response.setBody(JSONObject.toJSONString(streamInfoInServer));
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
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
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
            // 手动发送结果
            sendResponse(response);
            hookSubscribe.removeSubscribe(hook);
        });
        return null;
    }

    /**
     * 停止监听流上线
     */
    @RedisRpcMapping("stopWaitePushStreamOnline")
    public RedisRpcResponse stopWaitePushStreamOnline(RedisRpcRequest request) {
        SendRtpInfo sendRtpItem = JSONObject.parseObject(request.getParam().toString(), SendRtpInfo.class);
        log.info("[redis-rpc] 停止监听流上线： {}/{}, 目标地址： {}：{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort() );
        // 监听流上线。 流上线直接发送sendRtpItem消息给实际的信令处理者
        Hook hook = Hook.getInstance(HookType.on_media_arrival, sendRtpItem.getApp(), sendRtpItem.getStream(), null);
        hookSubscribe.removeSubscribe(hook);
        RedisRpcResponse response = request.getResponse();
        response.setStatusCode(ErrorCode.SUCCESS.getCode());
        return response;
    }

    /**
     * 停止监听流上线
     */
    @RedisRpcMapping("unPushStreamOnlineEvent")
    public RedisRpcResponse unPushStreamOnlineEvent(RedisRpcRequest request) {
        StreamInfo streamInfo = JSONObject.parseObject(request.getParam().toString(), StreamInfo.class);
        log.info("[redis-rpc] 停止监听流上线： {}/{}", streamInfo.getApp(), streamInfo.getStream());
        // 监听流上线。 流上线直接发送sendRtpItem消息给实际的信令处理者
        Hook hook = Hook.getInstance(HookType.on_media_arrival, streamInfo.getApp(), streamInfo.getStream(), null);
        hookSubscribe.removeSubscribe(hook);
        RedisRpcResponse response = request.getResponse();
        response.setStatusCode(ErrorCode.SUCCESS.getCode());
        return response;
    }

    /**
     * 停止监听流上线
     */
    @RedisRpcMapping("play")
    public RedisRpcResponse play(RedisRpcRequest request) {
        int id = Integer.parseInt(request.getParam().toString());
        RedisRpcResponse response = request.getResponse();
        if (id <= 0) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        streamPushPlayService.start(id, (code, msg, data) -> {
            if (code == ErrorCode.SUCCESS.getCode()) {
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
                response.setBody(JSONObject.toJSONString(data));
                sendResponse(response);
            }
        }, null, null);
        return null;
    }

}
