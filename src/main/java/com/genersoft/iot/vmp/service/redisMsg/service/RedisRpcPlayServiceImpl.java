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
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcPlayService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sip.message.Response;
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
            if (response.getStatusCode() == Response.OK) {
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
            if (response.getStatusCode() != Response.OK) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), ErrorCode.ERROR100.getMsg());
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
            if (response.getStatusCode() == Response.OK) {
                StreamInfo streamInfo = JSON.parseObject(response.getBody().toString(), StreamInfo.class);
                callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
            }else {
                callback.run(response.getStatusCode(), response.getBody().toString(), null);
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
            if (response.getStatusCode() == Response.OK) {
                StreamInfo streamInfo = JSON.parseObject(response.getBody().toString(), StreamInfo.class);
                callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
            }else {
                callback.run(response.getStatusCode(), response.getBody().toString(), null);
            }
        }
    }
}

