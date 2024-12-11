package com.genersoft.iot.vmp.service.redisMsg.service;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.redis.RedisRpcConfig;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcRequest;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcResponse;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcPlayService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sip.message.Response;

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
        RedisRpcRequest request = buildRequest("playChannel", channelId);
        request.setToId(serverId);
        RedisRpcResponse response = redisRpcConfig.request(request, userSetting.getPlayTimeout());
        if (response == null) {
            callback.run(ErrorCode.ERROR100.getCode(), ErrorCode.ERROR100.getMsg(), null);
        }else {
            if (response.getStatusCode() == Response.OK) {
                StreamInfo streamInfo = JSON.parseObject(response.getBody().toString(), StreamInfo.class);
                callback.run(response.getStatusCode(), "success", streamInfo);
            }else {
                callback.run(response.getStatusCode(), response.getBody().toString(), null);
            }
        }
    }
}

