package com.genersoft.iot.vmp.streamProxy.service.impl;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.hook.Hook;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.hook.HookType;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcPlayService;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxy;
import com.genersoft.iot.vmp.streamProxy.dao.StreamProxyMapper;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyPlayService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import javax.validation.constraints.NotNull;
import java.util.UUID;

/**
 * 视频代理业务
 */
@Slf4j
@Service
public class StreamProxyPlayServiceImpl implements IStreamProxyPlayService {

    @Autowired
    private StreamProxyMapper streamProxyMapper;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private HookSubscribe subscribe;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IRedisRpcPlayService redisRpcPlayService;

    @Override
    public void start(int id, Boolean record, ErrorCallback<StreamInfo> callback) {
        log.info("[拉流代理]， 开始拉流，ID：{}", id);
        StreamProxy streamProxy = streamProxyMapper.select(id);
        if (streamProxy == null) {
            throw new ControllerException(ErrorCode.ERROR404.getCode(), "代理信息未找到");
        }
        log.info("[拉流代理] 类型： {}， app：{}, stream: {}, 流地址： {}", streamProxy.getType(), streamProxy.getApp(), streamProxy.getStream(), streamProxy.getSrcUrl());
        if (record != null) {
            streamProxy.setEnableMp4(record);
        }

        startProxy(streamProxy, callback);
    }

    @Override
    public void startProxy(@NotNull StreamProxy streamProxy, ErrorCallback<StreamInfo> callback){
        if (!streamProxy.isEnable()) {
            callback.run(ErrorCode.ERROR100.getCode(), "代理未启用", null);
            return;
        }
        if (streamProxy.getServerId() == null) {
            streamProxy.setServerId(userSetting.getServerId());
        }
        if (!userSetting.getServerId().equals(streamProxy.getServerId())) {
            log.info("[拉流代理] 由其他服务{}管理", streamProxy.getServerId());
            redisRpcPlayService.playProxy(streamProxy.getServerId(), streamProxy.getId(), callback);
            return;
        }

        if (streamProxy.getMediaServerId() != null) {
            StreamInfo streamInfo = mediaServerService.getStreamInfoByAppAndStreamWithCheck(streamProxy.getApp(), streamProxy.getStream(), streamProxy.getMediaServerId(), null, false);
            if (streamInfo != null) {
                callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
                return;
            }
        }

        MediaServer mediaServer;
        String mediaServerId = streamProxy.getRelatesMediaServerId();
        if (mediaServerId == null) {
            mediaServer = mediaServerService.getMediaServerForMinimumLoad(null);
        }else {
            mediaServer = mediaServerService.getOne(mediaServerId);
        }
        if (mediaServer == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), mediaServerId == null?"未找到可用的媒体节点":"未找到节点" + mediaServerId);
        }

        // 设置流超时的定时任务
        String timeOutTaskKey = UUID.randomUUID().toString();
        Hook rtpHook = Hook.getInstance(HookType.on_media_arrival, streamProxy.getApp(), streamProxy.getStream(), mediaServer.getId());
        dynamicTask.startDelay(timeOutTaskKey, () -> {
            log.info("[拉流代理] 收流超时，app：{}，stream: {}", streamProxy.getApp(), streamProxy.getStream());
            // 收流超时
            subscribe.removeSubscribe(rtpHook);
            callback.run(InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getCode(), InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getMsg(), null);
        }, userSetting.getPlayTimeout());

        // 开启流到来的监听
        subscribe.addSubscribe(rtpHook, (hookData) -> {
            log.info("[拉流代理] 收流成功，app：{}，stream: {}", hookData.getApp(), hookData.getStream());
            dynamicTask.stop(timeOutTaskKey);
            StreamInfo streamInfo = mediaServerService.getStreamInfoByAppAndStream(mediaServer, hookData.getApp(), hookData.getStream(), hookData.getMediaInfo(), null);
            // hook响应
            callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
            subscribe.removeSubscribe(rtpHook);
            streamProxy.setPulling(true);
            streamProxyMapper.updateStream(streamProxy);
        });

        String key = mediaServerService.startProxy(mediaServer, streamProxy);
        streamProxy.setStreamKey(key);
        streamProxy.setMediaServerId(mediaServer.getId());
        streamProxyMapper.updateStream(streamProxy);
    }

    @Override
    public void stop(int id) {
        StreamProxy streamProxy = streamProxyMapper.select(id);
        if (streamProxy == null) {
            throw new ControllerException(ErrorCode.ERROR404.getCode(), "代理信息未找到");
        }
        if (!userSetting.getServerId().equals(streamProxy.getServerId())) {
            redisRpcPlayService.stopProxy(streamProxy.getServerId(), streamProxy.getId());
            return;
        }
        stopProxy(streamProxy);
    }

    @Override
    public void stopProxy(StreamProxy streamProxy){

        String mediaServerId = streamProxy.getMediaServerId();
        Assert.notNull(mediaServerId, "代理节点不存在");

        MediaServer mediaServer = mediaServerService.getOne(mediaServerId);
        if (mediaServer == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "媒体节点不存在");
        }
        if (ObjectUtils.isEmpty(streamProxy.getStreamKey())) {
            mediaServerService.closeStreams(mediaServer, streamProxy.getApp(), streamProxy.getStream());
        }else {
            mediaServerService.stopProxy(mediaServer, streamProxy.getStreamKey(), streamProxy.getType());
        }
        streamProxyMapper.removeStream(streamProxy.getId());
    }

}
