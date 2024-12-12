package com.genersoft.iot.vmp.service.redisMsg.control;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.redis.RedisRpcConfig;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcMessage;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcRequest;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcResponse;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelPlayService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.service.redisMsg.dto.RedisRpcController;
import com.genersoft.iot.vmp.service.redisMsg.dto.RedisRpcMapping;
import com.genersoft.iot.vmp.service.redisMsg.dto.RpcController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.sip.message.Response;

@Component
@Slf4j
@RedisRpcController("channel")
public class RedisRpcChannelPlayController extends RpcController {

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private IGbChannelService channelService;

    @Autowired
    private IGbChannelPlayService channelPlayService;

    private void sendResponse(RedisRpcResponse response){
        log.info("[redis-rpc] >> {}", response);
        response.setToId(userSetting.getServerId());
        RedisRpcMessage message = new RedisRpcMessage();
        message.setResponse(response);
        redisTemplate.convertAndSend(RedisRpcConfig.REDIS_REQUEST_CHANNEL_KEY, message);
    }


    /**
     * 点播国标设备
     */
    @RedisRpcMapping("play")
    public RedisRpcResponse playChannel(RedisRpcRequest request) {
        int channelId = Integer.parseInt(request.getParam().toString());
        RedisRpcResponse response = request.getResponse();

        if (channelId <= 0) {
            response.setStatusCode(Response.BAD_REQUEST);
            response.setBody("param error");
            return response;
        }
        // 获取对应的设备和通道信息
        CommonGBChannel channel = channelService.getOne(channelId);
        if (channel == null) {
            response.setStatusCode(Response.BAD_REQUEST);
            response.setBody("param error");
            return response;
        }

        channelPlayService.play(channel, null, (code, msg, data) ->{
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                response.setStatusCode(Response.OK);
                response.setBody(data);
            }else {
                response.setStatusCode(code);
            }
            // 手动发送结果
            sendResponse(response);
        });
        return null;
    }


    /**
     * 停止点播国标设备
     */
    @RedisRpcMapping("stop")
    public RedisRpcResponse stop(RedisRpcRequest request) {
        System.out.println(request.getParam().toString());
        JSONObject jsonObject = JSONObject.parseObject(request.getParam().toString());

        RedisRpcResponse response = request.getResponse();

        Integer channelId = jsonObject.getIntValue("channelId");
        if (channelId == null || channelId <= 0) {
            response.setStatusCode(Response.BAD_REQUEST);
            response.setBody("param error");
            return response;
        }

        String stream = jsonObject.getString("stream");
        InviteSessionType type = jsonObject.getObject("inviteSessionType", InviteSessionType.class);

        // 获取对应的设备和通道信息
        CommonGBChannel channel = channelService.getOne(channelId);
        if (channel == null) {
            response.setStatusCode(Response.BAD_REQUEST);
            response.setBody("param error");
            return response;
        }
        try {
            channelPlayService.stopPlay(type, channel, stream);
            response.setStatusCode(Response.OK);
        }catch (Exception e){
            response.setStatusCode(Response.SERVER_INTERNAL_ERROR);
            response.setBody(e.getMessage());
        }
        return response;
    }

}
