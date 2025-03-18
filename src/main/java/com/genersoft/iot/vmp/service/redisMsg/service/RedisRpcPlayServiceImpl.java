package com.genersoft.iot.vmp.service.redisMsg.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.redis.RedisRpcConfig;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcRequest;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcResponse;
import com.genersoft.iot.vmp.gb28181.bean.RecordInfo;
import com.genersoft.iot.vmp.service.bean.DownloadFileInfo;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcPlayService;
import com.genersoft.iot.vmp.vmanager.bean.AudioBroadcastResult;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RedisRpcPlayServiceImpl implements IRedisRpcPlayService {


    @Autowired
    private RedisRpcConfig redisRpcConfig;

    @Autowired
    private UserSetting userSetting;


    private RedisRpcRequest buildRequest(String uri, Object param) {
        RedisRpcRequest request = new RedisRpcRequest();
        request.setFromId(userSetting.getServerId());
        request.setParam(param);
        request.setUri(uri);
        return request;
    }

    @Override
    public void play(String serverId, Integer channelId, ErrorCallback<StreamInfo> callback) {
        RedisRpcRequest request = buildRequest("channel/play", channelId);
        request.setToId(serverId);
        RedisRpcResponse response = redisRpcConfig.request(request, userSetting.getPlayTimeout(), TimeUnit.MILLISECONDS);
        if (response == null) {
            callback.run(ErrorCode.ERROR100.getCode(), ErrorCode.ERROR100.getMsg(), null);
        }else {
            if (response.getStatusCode() == ErrorCode.SUCCESS.getCode()) {
                StreamInfo streamInfo = JSON.parseObject(response.getBody().toString(), StreamInfo.class);
                callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
            }else {
                callback.run(response.getStatusCode(), response.getBody().toString(), null);
            }
        }
    }

    @Override
    public void stop(String serverId, InviteSessionType type, int channelId, String stream) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("channelId", channelId);
        jsonObject.put("stream", stream);
        jsonObject.put("inviteSessionType", type);
        RedisRpcRequest request = buildRequest("channel/stop", jsonObject.toJSONString());
        request.setToId(serverId);
        RedisRpcResponse response = redisRpcConfig.request(request, 50, TimeUnit.MICROSECONDS);
        if (response == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), ErrorCode.ERROR100.getMsg());
        }else {
            if (response.getStatusCode() != ErrorCode.SUCCESS.getCode()) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), ErrorCode.ERROR100.getMsg());
            }
        }
    }

    @Override
    public void queryRecordInfo(String serverId, Integer channelId, String startTime, String endTime, ErrorCallback<RecordInfo> callback) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("channelId", channelId);
        jsonObject.put("startTime", startTime);
        jsonObject.put("endTime", endTime);
        RedisRpcRequest request = buildRequest("channel/queryRecordInfo", jsonObject);
        request.setToId(serverId);
        RedisRpcResponse response = redisRpcConfig.request(request, userSetting.getRecordInfoTimeout(), TimeUnit.MILLISECONDS);
        if (response == null) {
            callback.run(ErrorCode.ERROR100.getCode(), ErrorCode.ERROR100.getMsg(), null);
        }else {
            if (response.getStatusCode() == ErrorCode.SUCCESS.getCode()) {
                RecordInfo recordInfo = JSON.parseObject(response.getBody().toString(), RecordInfo.class);
                callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), recordInfo);
            }else {
                callback.run(response.getStatusCode(), response.getBody().toString(), null);
            }
        }
    }

    @Override
    public void playback(String serverId, Integer channelId, String startTime, String endTime, ErrorCallback<StreamInfo> callback) {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("channelId", channelId);
        jsonObject.put("startTime", startTime);
        jsonObject.put("endTime", endTime);
        RedisRpcRequest request = buildRequest("channel/playback", jsonObject.toString());
        request.setToId(serverId);
        RedisRpcResponse response = redisRpcConfig.request(request, userSetting.getPlayTimeout(), TimeUnit.MILLISECONDS);
        if (response == null) {
            callback.run(ErrorCode.ERROR100.getCode(), ErrorCode.ERROR100.getMsg(), null);
        }else {
            if (response.getStatusCode() == ErrorCode.SUCCESS.getCode()) {
                StreamInfo streamInfo = JSON.parseObject(response.getBody().toString(), StreamInfo.class);
                callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
            }else {
                callback.run(response.getStatusCode(), response.getBody().toString(), null);
            }
        }
    }

    @Override
    public void pauseRtp(String serverId, String streamId) {
        RedisRpcRequest request = buildRequest("channel/pauseRtp", streamId);
        request.setToId(serverId);
        RedisRpcResponse response = redisRpcConfig.request(request, 5, TimeUnit.SECONDS);
        if (response == null) {
            log.info("[RPC 暂停回放] 失败, streamId: {}", streamId);
        }else {
            if (response.getStatusCode() != ErrorCode.SUCCESS.getCode()) {
                log.info("[RPC 暂停回放] 失败, {},  streamId: {}", response.getBody(), streamId);
            }
        }
    }

    @Override
    public void resumeRtp(String serverId, String streamId) {
        RedisRpcRequest request = buildRequest("channel/resumeRtp", streamId);
        request.setToId(serverId);
        RedisRpcResponse response = redisRpcConfig.request(request, 5, TimeUnit.SECONDS);
        if (response == null) {
            log.info("[RPC 恢复回放] 失败, streamId: {}", streamId);
        }else {
            if (response.getStatusCode() != ErrorCode.SUCCESS.getCode()) {
                log.info("[RPC 恢复回放] 失败, {},  streamId: {}", response.getBody(), streamId);
            }
        }
    }

    @Override
    public void download(String serverId, Integer channelId, String startTime, String endTime, int downloadSpeed, ErrorCallback<StreamInfo> callback) {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("channelId", channelId);
        jsonObject.put("startTime", startTime);
        jsonObject.put("endTime", endTime);
        jsonObject.put("downloadSpeed", downloadSpeed);
        RedisRpcRequest request = buildRequest("channel/download", jsonObject.toString());
        request.setToId(serverId);
        RedisRpcResponse response = redisRpcConfig.request(request, userSetting.getPlayTimeout(), TimeUnit.MILLISECONDS);
        if (response == null) {
            callback.run(ErrorCode.ERROR100.getCode(), ErrorCode.ERROR100.getMsg(), null);
        }else {
            if (response.getStatusCode() == ErrorCode.SUCCESS.getCode()) {
                StreamInfo streamInfo = JSON.parseObject(response.getBody().toString(), StreamInfo.class);
                callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
            }else {
                callback.run(response.getStatusCode(), response.getBody().toString(), null);
            }
        }
    }

    @Override
    public String frontEndCommand(String serverId, Integer channelId, int cmdCode, int parameter1, int parameter2, int combindCode2) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("channelId", channelId);
        jsonObject.put("cmdCode", cmdCode);
        jsonObject.put("parameter1", parameter1);
        jsonObject.put("parameter2", parameter2);
        jsonObject.put("combindCode2", combindCode2);
        RedisRpcRequest request = buildRequest("channel/ptz/frontEndCommand", jsonObject.toString());
        request.setToId(serverId);
        RedisRpcResponse response = redisRpcConfig.request(request, userSetting.getPlayTimeout(), TimeUnit.MILLISECONDS);
        if (response == null) {
            return ErrorCode.ERROR100.getMsg();
        }else {
            if (response.getStatusCode() != ErrorCode.SUCCESS.getCode()) {
                return response.getBody().toString();
            }
        }
        return null;
    }

    @Override
    public void playPush(Integer id, ErrorCallback<StreamInfo> callback) {
        RedisRpcRequest request = buildRequest("streamPush/play", id);
        RedisRpcResponse response = redisRpcConfig.request(request, userSetting.getPlayTimeout(), TimeUnit.SECONDS);
        if (response == null) {
            callback.run(ErrorCode.ERROR100.getCode(), ErrorCode.ERROR100.getMsg(), null);
        }else {
            if (response.getStatusCode() == ErrorCode.SUCCESS.getCode()) {
                StreamInfo streamInfo = JSON.parseObject(response.getBody().toString(), StreamInfo.class);
                callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
            }else {
                callback.run(response.getStatusCode(), response.getBody().toString(), null);
            }
        }
    }

    @Override
    public StreamInfo playProxy(String serverId, int id) {
        RedisRpcRequest request = buildRequest("streamProxy/play", id);
        RedisRpcResponse response = redisRpcConfig.request(request, userSetting.getPlayTimeout(), TimeUnit.SECONDS);
        if (response != null && response.getStatusCode() == ErrorCode.SUCCESS.getCode()) {
            return JSON.parseObject(response.getBody().toString(), StreamInfo.class);
        }
        return null;
    }

    @Override
    public void stopProxy(String serverId, int id) {
        RedisRpcRequest request = buildRequest("streamProxy/stop", id);
        RedisRpcResponse response = redisRpcConfig.request(request, userSetting.getPlayTimeout(), TimeUnit.SECONDS);
        if (response != null && response.getStatusCode() == ErrorCode.SUCCESS.getCode()) {
            log.info("[rpc 拉流代理] 停止成功： id: {}", id);
        }else {
            log.info("[rpc 拉流代理] 停止失败 id: {}", id);
        }
    }

    @Override
    public DownloadFileInfo getRecordPlayUrl(String serverId, Integer recordId) {
        RedisRpcRequest request = buildRequest("cloudRecord/play", recordId);
        RedisRpcResponse response = redisRpcConfig.request(request, userSetting.getPlayTimeout(), TimeUnit.SECONDS);
        if (response != null && response.getStatusCode() == ErrorCode.SUCCESS.getCode()) {
            return JSON.parseObject(response.getBody().toString(), DownloadFileInfo.class);
        }
        return null;
    }

    @Override
    public AudioBroadcastResult audioBroadcast(String serverId, String deviceId, String channelDeviceId, Boolean broadcastMode) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("deviceId", deviceId);
        jsonObject.put("channelDeviceId", channelDeviceId);
        jsonObject.put("broadcastMode", broadcastMode);
        RedisRpcRequest request = buildRequest("devicePlay/audioBroadcast", jsonObject.toString());
        request.setToId(serverId);
        RedisRpcResponse response = redisRpcConfig.request(request, userSetting.getPlayTimeout(), TimeUnit.SECONDS);
        if (response != null && response.getStatusCode() == ErrorCode.SUCCESS.getCode()) {
            return JSON.parseObject(response.getBody().toString(), AudioBroadcastResult.class);
        }
        return null;
    }
}

