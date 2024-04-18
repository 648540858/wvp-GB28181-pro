package com.genersoft.iot.vmp.service.redisMsg.service;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.redis.RedisRpcConfig;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcRequest;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcResponse;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeFactory;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForStreamChange;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.hook.HookParam;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisRpcServiceImpl implements IRedisRpcService {

    private final static Logger logger = LoggerFactory.getLogger(RedisRpcServiceImpl.class);

    @Autowired
    private RedisRpcConfig redisRpcConfig;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private ZlmHttpHookSubscribe hookSubscribe;

    @Autowired
    private SSRCFactory ssrcFactory;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    private RedisRpcRequest buildRequest(String uri, Object param) {
        RedisRpcRequest request = new RedisRpcRequest();
        request.setFromId(userSetting.getServerId());
        request.setParam(param);
        request.setUri(uri);
        return request;
    }

    @Override
    public SendRtpItem getSendRtpItem(String sendRtpItemKey) {
        RedisRpcRequest request = buildRequest("getSendRtpItem", sendRtpItemKey);
        RedisRpcResponse response = redisRpcConfig.request(request, 10);
        if (response.getBody() == null) {
            return null;
        }
        return (SendRtpItem)redisTemplate.opsForValue().get(response.getBody().toString());
    }

    @Override
    public WVPResult startSendRtp(String sendRtpItemKey, SendRtpItem sendRtpItem) {
        logger.info("[请求其他WVP] 开始推流，wvp：{}， {}/{}", sendRtpItem.getServerId(), sendRtpItem.getApp(), sendRtpItem.getStream());
        RedisRpcRequest request = buildRequest("startSendRtp", sendRtpItemKey);
        request.setToId(sendRtpItem.getServerId());
        RedisRpcResponse response = redisRpcConfig.request(request, 10);
        return JSON.parseObject(response.getBody().toString(), WVPResult.class);
    }

    @Override
    public WVPResult stopSendRtp(String sendRtpItemKey) {
        SendRtpItem sendRtpItem = (SendRtpItem)redisTemplate.opsForValue().get(sendRtpItemKey);
        if (sendRtpItem == null) {
            logger.info("[请求其他WVP] 停止推流, 未找到redis中的发流信息， key：{}", sendRtpItemKey);
            return WVPResult.fail(ErrorCode.ERROR100.getCode(), "未找到发流信息");
        }
        logger.info("[请求其他WVP] 停止推流，wvp：{}， {}/{}", sendRtpItem.getServerId(), sendRtpItem.getApp(), sendRtpItem.getStream());
        RedisRpcRequest request = buildRequest("stopSendRtp", sendRtpItemKey);
        request.setToId(sendRtpItem.getServerId());
        RedisRpcResponse response = redisRpcConfig.request(request, 10);
        return JSON.parseObject(response.getBody().toString(), WVPResult.class);
    }

    @Override
    public void waitePushStreamOnline(SendRtpItem sendRtpItem, CommonCallback<String> callback) {
        logger.info("[请求所有WVP监听流上线] {}/{}", sendRtpItem.getApp(), sendRtpItem.getStream());
        // 监听流上线。 流上线直接发送sendRtpItem消息给实际的信令处理者
        HookSubscribeForStreamChange hook = HookSubscribeFactory.on_stream_changed(
                sendRtpItem.getApp(), sendRtpItem.getStream(), true, "rtsp", null);
        hookSubscribe.addSubscribe(hook, (MediaServerItem mediaServerItemInUse, HookParam hookParam) -> {

            // 读取redis中的上级点播信息，生成sendRtpItm发送出去
            if (sendRtpItem.getSsrc() == null) {
                // 上级平台点播时不使用上级平台指定的ssrc，使用自定义的ssrc，参考国标文档-点播外域设备媒体流SSRC处理方式
                String ssrc = "Play".equalsIgnoreCase(sendRtpItem.getSessionName()) ? ssrcFactory.getPlaySsrc(mediaServerItemInUse.getId()) : ssrcFactory.getPlayBackSsrc(mediaServerItemInUse.getId());
                sendRtpItem.setSsrc(ssrc);
            }
            sendRtpItem.setMediaServerId(mediaServerItemInUse.getId());
            sendRtpItem.setLocalIp(mediaServerItemInUse.getSdpIp());
            sendRtpItem.setServerId(userSetting.getServerId());
            redisTemplate.opsForValue().set(sendRtpItem.getRedisKey(), sendRtpItem);
            if (callback != null) {
                callback.run(sendRtpItem.getRedisKey());
            }
            hookSubscribe.removeSubscribe(hook);
        });
        RedisRpcRequest request = buildRequest("waitePushStreamOnline", sendRtpItem);
        request.setToId(sendRtpItem.getServerId());
        redisRpcConfig.request(request, response -> {
            if (response.getBody() == null) {
                logger.info("[请求所有WVP监听流上线] 流上线,但是未找到发流信息：{}/{}", sendRtpItem.getApp(), sendRtpItem.getStream());
                return;
            }
            logger.info("[请求所有WVP监听流上线] 流上线 {}/{}->{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.toString());

            if (callback != null) {
                callback.run(response.getBody().toString());
            }
        });

    }

    @Override
    public void stopWaitePushStreamOnline(String sendRtpItemKey, SendRtpItem sendRtpItem) {
        logger.info("[停止WVP监听流上线] {}/{}， key：{}", sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItemKey);
        HookSubscribeForStreamChange hook = HookSubscribeFactory.on_stream_changed(
                sendRtpItem.getApp(), sendRtpItem.getStream(), true, "rtsp", null);
        hookSubscribe.removeSubscribe(hook);
        RedisRpcRequest request = buildRequest("stopWaitePushStreamOnline", sendRtpItemKey);
        request.setToId(sendRtpItem.getServerId());
        redisRpcConfig.request(request, 10);
    }

    @Override
    public void rtpSendStopped(String sendRtpItemKey) {
        SendRtpItem sendRtpItem = (SendRtpItem)redisTemplate.opsForValue().get(sendRtpItemKey);
        if (sendRtpItem == null) {
            logger.info("[停止WVP监听流上线] 未找到redis中的发流信息， key：{}", sendRtpItemKey);
            return;
        }
        RedisRpcRequest request = buildRequest("rtpSendStopped", sendRtpItemKey);
        request.setToId(sendRtpItem.getServerId());
        redisRpcConfig.request(request, 10);
    }
}
