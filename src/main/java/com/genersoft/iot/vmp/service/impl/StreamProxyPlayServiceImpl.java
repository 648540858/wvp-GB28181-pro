package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.GeneralCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeFactory;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForStreamChange;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxy;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OriginType;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.service.IStreamProxyPlayService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.StreamProxyMapper;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;

import java.util.UUID;

@Service
public class StreamProxyPlayServiceImpl implements IStreamProxyPlayService {

    private final static Logger logger = LoggerFactory.getLogger(StreamProxyPlayServiceImpl.class);

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private IMediaService mediaService;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private ZlmHttpHookSubscribe hookSubscribe;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    TransactionDefinition transactionDefinition;

    @Autowired
    IRedisCatchStorage redisCatchStorage;

    @Autowired
    IMediaServerService mediaServerService;

    @Autowired
    StreamProxyMapper streamProxyMapper;

    @Override
    public void startProxy(StreamProxy streamProxy, MediaServerItem mediaInfo, GeneralCallback<StreamInfo> callback) {
        logger.info("[开始拉流代理] {}/{}", streamProxy.getApp(), streamProxy.getStream());

        OnStreamChangedHookParam streamChangedHookParam = redisCatchStorage.getProxyStreamInfo(streamProxy.getApp(), streamProxy.getStream(), null);
        if (streamChangedHookParam != null) {
            MediaServerItem serverItemInCatch = mediaServerService.getOne(streamChangedHookParam.getMediaServerId());
            if (serverItemInCatch != null) {
                // 检测是否在线
                boolean ready = mediaService.isReady(serverItemInCatch, streamProxy.getApp(), streamProxy.getStream());
                if (ready) {
                    StreamInfo streamInfo = mediaService.getStreamInfoByAppAndStream(
                            mediaInfo, streamProxy.getApp(), streamProxy.getStream(), null, null);
                    logger.info("[开始拉流代理] 已拉起，直接返回 {}/{}", streamProxy.getApp(), streamProxy.getStream());
                    if (callback != null) {
                        callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
                    }
                    return;
                }else {
                    redisCatchStorage.removeStream(streamChangedHookParam.getMediaServerId(), "PULL", streamChangedHookParam.getApp(),
                            streamChangedHookParam.getStream());
                }
            }else {
                redisCatchStorage.removeStream(streamChangedHookParam.getMediaServerId(), "PULL", streamChangedHookParam.getApp(),
                        streamChangedHookParam.getStream());
            }
        }

        if (streamProxy.getStreamKey() != null) {
            zlmresTfulUtils.delStreamProxy(mediaInfo, streamProxy.getStreamKey());
        }

        String delayTalkKey = UUID.randomUUID().toString();

        HookSubscribeForStreamChange hookSubscribeForStreamChange = HookSubscribeFactory.on_stream_changed(streamProxy.getApp(), streamProxy.getStream(), true, "rtsp", mediaInfo.getId());
        hookSubscribe.addSubscribe(hookSubscribeForStreamChange, (mediaServerItem, response) -> {
            dynamicTask.stop(delayTalkKey);
            streamProxy.setPulling(true);
            StreamInfo streamInfo = mediaService.getStreamInfoByAppAndStream(
                    mediaInfo, streamProxy.getApp(), streamProxy.getStream(), null, null);
            logger.info("[开始拉流代理] 成功： {}/{}", streamProxy.getApp(), streamProxy.getStream());
            if (callback != null) {
                callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
            }
            streamProxyMapper.update(streamProxy);
        });

        dynamicTask.startDelay(delayTalkKey, ()->{
            hookSubscribe.removeSubscribe(hookSubscribeForStreamChange);
            dynamicTask.stop(delayTalkKey);
            if (callback != null) {
                callback.run(ErrorCode.ERROR100.getCode(), "启用超时，请检查源地址是否可用", null);
            }
            streamProxy.setProxyError("启用超时");
            streamProxyMapper.update(streamProxy);
        }, 10000);
        JSONObject result;
        if ("ffmpeg".equalsIgnoreCase(streamProxy.getType())){
            result = zlmresTfulUtils.addFFmpegSource(mediaInfo, streamProxy.getUrl().trim(), streamProxy.getDstUrl(),
                    streamProxy.getTimeoutMs() + "", streamProxy.isEnableAudio(), streamProxy.isEnableMp4(),
                    streamProxy.getFfmpegCmdKey());
        }else {
            result = zlmresTfulUtils.addStreamProxy(mediaInfo, streamProxy.getApp(), streamProxy.getStream(), streamProxy.getUrl().trim(),
                    streamProxy.isEnableAudio(), streamProxy.isEnableMp4(), streamProxy.getRtpType());
        }
        if (result == null) {
            if (callback != null) {
                callback.run(ErrorCode.ERROR100.getCode(), "接口调用失败", null);
            }
            return;
        }
        if (result.getInteger("code") != 0) {
            hookSubscribe.removeSubscribe(hookSubscribeForStreamChange);
            dynamicTask.stop(delayTalkKey);
            if (callback != null) {
                callback.run(result.getInteger("code"), result.getString("msg"), null);
            }
        }else {
            JSONObject data = result.getJSONObject("data");
            if (data == null) {
                logger.warn("[获取拉流代理的结果数据Data] 失败： {}", result );
                if (callback != null) {
                    callback.run(result.getInteger("code"), result.getString("msg"), null);
                }
                return;
            }
            String key = data.getString("key");
            if (key == null) {
                logger.warn("[获取拉流代理的结果数据Data中的KEY] 失败： {}", result );
                if (callback != null) {
                    callback.run(ErrorCode.ERROR100.getCode(), "获取代理流结果中的KEY失败", null);
                }
                return;
            }
            streamProxy.setStreamKey(key);
            streamProxyMapper.update(streamProxy);
        }
    }

    @Override
    public void stopProxy(StreamProxy streamProxy, MediaServerItem mediaInfo, GeneralCallback<StreamInfo> callback) {
        logger.info("[停止拉流代理] {}/{}", streamProxy.getApp(), streamProxy.getStream());
        boolean ready = mediaService.isReady(mediaInfo, streamProxy.getApp(), streamProxy.getStream());
        if (ready) {
            if ("ffmpeg".equalsIgnoreCase(streamProxy.getType())){
                zlmresTfulUtils.delFFmpegSource(mediaInfo, streamProxy.getStreamKey());
            }else {
                zlmresTfulUtils.delStreamProxy(mediaInfo, streamProxy.getStreamKey());
            }
            mediaService.closeStream(mediaInfo, streamProxy.getApp(), streamProxy.getStream());
        }
        // 检查redis内容是否正确
        String key = VideoManagerConstants.WVP_SERVER_STREAM_PREFIX  + userSetting.getServerId() + "_"
                + OriginType.PULL + "_" + streamProxy.getApp() + "_" + streamProxy.getStream() + "_"
                + mediaInfo.getId();

        if (redisTemplate.opsForValue().get(key) == null) {
            redisTemplate.delete(key);
        }
        logger.info("[停止拉流代理] 成功 {}/{}", streamProxy.getApp(), streamProxy.getStream());
        if (callback != null) {
            callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), null);
        }
    }
}
