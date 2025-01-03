package com.genersoft.iot.vmp.service.redisMsg.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.redis.RedisRpcConfig;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcRequest;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcResponse;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpInfo;
import com.genersoft.iot.vmp.gb28181.bean.SyncStatus;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.media.event.hook.Hook;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.hook.HookType;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RedisRpcServiceImpl implements IRedisRpcService {


    @Autowired
    private RedisRpcConfig redisRpcConfig;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private HookSubscribe hookSubscribe;

    @Autowired
    private SSRCFactory ssrcFactory;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private ISendRtpServerService sendRtpServerService;

    private RedisRpcRequest buildRequest(String uri, Object param) {
        RedisRpcRequest request = new RedisRpcRequest();
        request.setFromId(userSetting.getServerId());
        request.setParam(param);
        request.setUri(uri);
        return request;
    }

    @Override
    public SendRtpInfo getSendRtpItem(String callId) {
        RedisRpcRequest request = buildRequest("sendRtp/getSendRtpItem", callId);
        RedisRpcResponse response = redisRpcConfig.request(request, 10, TimeUnit.MILLISECONDS);
        if (response.getBody() == null) {
            return null;
        }
        return (SendRtpInfo)redisTemplate.opsForValue().get(response.getBody().toString());
    }

    @Override
    public WVPResult startSendRtp(String callId, SendRtpInfo sendRtpItem) {
        log.info("[请求其他WVP] 开始推流，wvp：{}， {}/{}", sendRtpItem.getServerId(), sendRtpItem.getApp(), sendRtpItem.getStream());
        RedisRpcRequest request = buildRequest("sendRtp/startSendRtp", callId);
        request.setToId(sendRtpItem.getServerId());
        RedisRpcResponse response = redisRpcConfig.request(request, 10, TimeUnit.MILLISECONDS);
        return JSON.parseObject(response.getBody().toString(), WVPResult.class);
    }

    @Override
    public WVPResult stopSendRtp(String callId) {
        SendRtpInfo sendRtpItem = (SendRtpInfo)redisTemplate.opsForValue().get(callId);
        if (sendRtpItem == null) {
            log.info("[请求其他WVP] 停止推流, 未找到redis中的发流信息， key：{}", callId);
            return WVPResult.fail(ErrorCode.ERROR100.getCode(), "未找到发流信息");
        }
        log.info("[请求其他WVP] 停止推流，wvp：{}， {}/{}", sendRtpItem.getServerId(), sendRtpItem.getApp(), sendRtpItem.getStream());
        RedisRpcRequest request = buildRequest("sendRtp/stopSendRtp", callId);
        request.setToId(sendRtpItem.getServerId());
        RedisRpcResponse response = redisRpcConfig.request(request, 10, TimeUnit.MILLISECONDS);
        return JSON.parseObject(response.getBody().toString(), WVPResult.class);
    }

    @Override
    public long waitePushStreamOnline(SendRtpInfo sendRtpItem, CommonCallback<Integer> callback) {
        log.info("[请求所有WVP监听流上线] {}/{}", sendRtpItem.getApp(), sendRtpItem.getStream());
        // 监听流上线。 流上线直接发送sendRtpItem消息给实际的信令处理者
        Hook hook = Hook.getInstance(HookType.on_media_arrival, sendRtpItem.getApp(), sendRtpItem.getStream(), null);
        RedisRpcRequest request = buildRequest("streamPush/waitePushStreamOnline", sendRtpItem);
        request.setToId(sendRtpItem.getServerId());
        hookSubscribe.addSubscribe(hook, (hookData) -> {

            // 读取redis中的上级点播信息，生成sendRtpItm发送出去
            if (sendRtpItem.getSsrc() == null) {
                // 上级平台点播时不使用上级平台指定的ssrc，使用自定义的ssrc，参考国标文档-点播外域设备媒体流SSRC处理方式
                String ssrc = "Play".equalsIgnoreCase(sendRtpItem.getSessionName()) ? ssrcFactory.getPlaySsrc(hookData.getMediaServer().getId()) : ssrcFactory.getPlayBackSsrc(hookData.getMediaServer().getId());
                sendRtpItem.setSsrc(ssrc);
            }
            sendRtpItem.setMediaServerId(hookData.getMediaServer().getId());
            sendRtpItem.setLocalIp(hookData.getMediaServer().getSdpIp());
            sendRtpItem.setServerId(userSetting.getServerId());
            sendRtpServerService.update(sendRtpItem);
            if (callback != null) {
                callback.run(sendRtpItem.getChannelId());
            }
            hookSubscribe.removeSubscribe(hook);
            redisRpcConfig.removeCallback(request.getSn());
        });

        redisRpcConfig.request(request, response -> {
            if (response.getBody() == null) {
                log.info("[请求所有WVP监听流上线] 流上线,但是未找到发流信息：{}/{}", sendRtpItem.getApp(), sendRtpItem.getStream());
                return;
            }
            log.info("[请求所有WVP监听流上线] 流上线 {}/{}->{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.toString());

            if (callback != null) {
                callback.run(Integer.parseInt(response.getBody().toString()));
            }
            hookSubscribe.removeSubscribe(hook);
        });
        return request.getSn();
    }

    @Override
    public void stopWaitePushStreamOnline(SendRtpInfo sendRtpItem) {
        log.info("[停止WVP监听流上线] {}/{}", sendRtpItem.getApp(), sendRtpItem.getStream());
        Hook hook = Hook.getInstance(HookType.on_media_arrival, sendRtpItem.getApp(), sendRtpItem.getStream(), null);
        hookSubscribe.removeSubscribe(hook);
        RedisRpcRequest request = buildRequest("streamPush/stopWaitePushStreamOnline", sendRtpItem);
        request.setToId(sendRtpItem.getServerId());
        redisRpcConfig.request(request, 10, TimeUnit.MILLISECONDS);
    }

    @Override
    public void rtpSendStopped(String callId) {
        SendRtpInfo sendRtpItem = (SendRtpInfo)redisTemplate.opsForValue().get(callId);
        if (sendRtpItem == null) {
            log.info("[停止WVP监听流上线] 未找到redis中的发流信息， key：{}", callId);
            return;
        }
        RedisRpcRequest request = buildRequest("streamPush/rtpSendStopped", callId);
        request.setToId(sendRtpItem.getServerId());
        redisRpcConfig.request(request, 10, TimeUnit.MILLISECONDS);
    }

    @Override
    public void removeCallback(long key) {
        redisRpcConfig.removeCallback(key);
    }

    @Override
    public long onStreamOnlineEvent(String app, String stream, CommonCallback<StreamInfo> callback) {

        log.info("[请求所有WVP监听流上线] {}/{}", app, stream);
        // 监听流上线。 流上线直接发送sendRtpItem消息给实际的信令处理者
        Hook hook = Hook.getInstance(HookType.on_media_arrival, app, stream);
        StreamInfo streamInfoParam = new StreamInfo();
        streamInfoParam.setApp(app);
        streamInfoParam.setStream(stream);
        RedisRpcRequest request = buildRequest("streamPush/onStreamOnlineEvent", streamInfoParam);
        hookSubscribe.addSubscribe(hook, (hookData) -> {
            log.info("[请求所有WVP监听流上线] 监听流上线 {}/{}", app, stream);
            if (callback != null) {
                callback.run(mediaServerService.getStreamInfoByAppAndStream(hookData.getMediaServer(),
                        app, stream, hookData.getMediaInfo(),
                        hookData.getMediaInfo() != null ? hookData.getMediaInfo().getCallId() : null));
            }
            hookSubscribe.removeSubscribe(hook);
            redisRpcConfig.removeCallback(request.getSn());
        });

        redisRpcConfig.request(request, response -> {
            if (response.getBody() == null) {
                log.info("[请求所有WVP监听流上线] 流上线,但是未找到发流信息：{}/{}", app, stream);
                return;
            }
            log.info("[请求所有WVP监听流上线] 流上线 {}/{}", app, stream);

            if (callback != null) {
                callback.run(JSON.parseObject(response.getBody().toString(), StreamInfo.class));
            }
            hookSubscribe.removeSubscribe(hook);
        });
        return request.getSn();
    }

    @Override
    public void unPushStreamOnlineEvent(String app, String stream) {
        StreamInfo streamInfoParam = new StreamInfo();
        streamInfoParam.setApp(app);
        streamInfoParam.setStream(stream);
        RedisRpcRequest request = buildRequest("streamPush/unPushStreamOnlineEvent", streamInfoParam);
        redisRpcConfig.request(request, 10, TimeUnit.MILLISECONDS);
    }

    @Override
    public void subscribeCatalog(int id, int cycle) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("cycle", cycle);
        RedisRpcRequest request = buildRequest("device/subscribeCatalog", jsonObject);
        redisRpcConfig.request(request, 10, TimeUnit.MILLISECONDS);
    }

    @Override
    public void subscribeMobilePosition(int id, int cycle, int interval) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("cycle", cycle);
        jsonObject.put("interval", cycle);
        RedisRpcRequest request = buildRequest("device/subscribeMobilePosition", jsonObject);
        redisRpcConfig.request(request, 10, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean updatePlatform(String serverId, Platform platform) {
        RedisRpcRequest request = buildRequest("platform/update", platform);
        request.setToId(serverId);
        RedisRpcResponse response = redisRpcConfig.request(request, 40, TimeUnit.MILLISECONDS);
        return Boolean.parseBoolean(response.getBody().toString());
    }

    @Override
    public void catalogEventPublish(String serverId, CatalogEvent event) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("platform", event.getPlatform());
        jsonObject.put("channels", event.getChannels());
        jsonObject.put("type", event.getType());
        RedisRpcRequest request = buildRequest("platform/catalogEventPublish", jsonObject);
        if (serverId != null) {
            request.setToId(serverId);
        }
        redisRpcConfig.request(request, 10, TimeUnit.MILLISECONDS);
    }

    @Override
    public WVPResult<SyncStatus> devicesSync(String serverId, String deviceId) {
        RedisRpcRequest request = buildRequest("device/devicesSync", deviceId);
        request.setToId(serverId);
        RedisRpcResponse response = redisRpcConfig.request(request, 100, TimeUnit.MILLISECONDS);
        return JSON.parseObject(response.getBody().toString(), WVPResult.class);
    }
}
