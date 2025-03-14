package com.genersoft.iot.vmp.service.redisMsg.control;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.redis.RedisRpcConfig;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcMessage;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcRequest;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcResponse;
import com.genersoft.iot.vmp.service.redisMsg.dto.RedisRpcController;
import com.genersoft.iot.vmp.service.redisMsg.dto.RedisRpcMapping;
import com.genersoft.iot.vmp.service.redisMsg.dto.RpcController;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxy;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyPlayService;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RedisRpcController("streamProxy")
public class RedisRpcStreamProxyController extends RpcController {

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private IStreamProxyPlayService streamProxyPlayService;

    @Autowired
    private IStreamProxyService streamProxyService;


    private void sendResponse(RedisRpcResponse response){
        log.info("[redis-rpc] >> {}", response);
        response.setToId(userSetting.getServerId());
        RedisRpcMessage message = new RedisRpcMessage();
        message.setResponse(response);
        redisTemplate.convertAndSend(RedisRpcConfig.REDIS_REQUEST_CHANNEL_KEY, message);
    }

    /**
     * 播放
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
        StreamProxy streamProxy = streamProxyService.getStreamProxy(id);
        if (streamProxy == null) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        StreamInfo streamInfo = streamProxyPlayService.startProxy(streamProxy);
        response.setStatusCode(ErrorCode.SUCCESS.getCode());
        response.setBody(JSONObject.toJSONString(streamInfo));
        return response;
    }

    /**
     * 停止
     */
    @RedisRpcMapping("stop")
    public RedisRpcResponse stop(RedisRpcRequest request) {
        int id = Integer.parseInt(request.getParam().toString());
        RedisRpcResponse response = request.getResponse();
        if (id <= 0) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        StreamProxy streamProxy = streamProxyService.getStreamProxy(id);
        if (streamProxy == null) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        streamProxyPlayService.stopProxy(streamProxy);
        response.setStatusCode(ErrorCode.SUCCESS.getCode());
        return response;
    }

}
