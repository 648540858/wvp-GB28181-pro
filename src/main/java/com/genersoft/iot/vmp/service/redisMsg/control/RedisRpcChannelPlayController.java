package com.genersoft.iot.vmp.service.redisMsg.control;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.redis.RedisRpcConfig;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcMessage;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcRequest;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcResponse;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpInfo;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelPlayService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.hook.Hook;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.hook.HookType;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.service.redisMsg.dto.RedisRpcController;
import com.genersoft.iot.vmp.service.redisMsg.dto.RedisRpcMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import javax.sip.message.Response;

@Slf4j
@RedisRpcController("channel")
public class RedisRpcChannelPlayController {

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

}
