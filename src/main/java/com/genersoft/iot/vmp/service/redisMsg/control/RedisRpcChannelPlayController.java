package com.genersoft.iot.vmp.service.redisMsg.control;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.redis.RedisRpcConfig;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcMessage;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcRequest;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcResponse;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.InviteMessageInfo;
import com.genersoft.iot.vmp.gb28181.bean.SyncStatus;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelPlayService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.gb28181.service.IPTZService;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.service.redisMsg.dto.RedisRpcController;
import com.genersoft.iot.vmp.service.redisMsg.dto.RedisRpcMapping;
import com.genersoft.iot.vmp.service.redisMsg.dto.RpcController;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
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

    @Autowired
    private IPTZService iptzService;

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
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        // 获取对应的设备和通道信息
        CommonGBChannel channel = channelService.getOne(channelId);
        if (channel == null) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }

        InviteMessageInfo inviteInfo = new InviteMessageInfo();
        inviteInfo.setSessionName("Play");
        channelPlayService.start(channel, inviteInfo, null, (code, msg, data) ->{
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
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
     * 点播国标设备
     */
    @RedisRpcMapping("queryRecordInfo")
    public RedisRpcResponse queryRecordInfo(RedisRpcRequest request) {
        JSONObject paramJson = JSONObject.parseObject(request.getParam().toString());
        int channelId = paramJson.getIntValue("channelId");
        String startTime = paramJson.getString("startTime");
        String endTime = paramJson.getString("endTime");
        RedisRpcResponse response = request.getResponse();

        if (channelId <= 0) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        // 获取对应的设备和通道信息
        CommonGBChannel channel = channelService.getOne(channelId);
        if (channel == null) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        
        try {
            channelService.queryRecordInfo(channel, startTime, endTime, (code, msg, data) ->{
                if (code == InviteErrorCode.SUCCESS.getCode()) {
                    response.setStatusCode(code);
                    response.setBody(data);
                }else {
                    response.setStatusCode(code);
                }
                // 手动发送结果
                sendResponse(response);
            });
        }catch (ControllerException e) {
            response.setStatusCode(ErrorCode.ERROR100.getCode());
            response.setBody(e.getMessage());
        }
        
        return null;
    }

    /**
     * 暂停录像回放
     */
    @RedisRpcMapping("pauseRtp")
    public RedisRpcResponse pauseRtp(RedisRpcRequest request) {
        String streamId = request.getParam().toString();
        RedisRpcResponse response = request.getResponse();

        if (streamId == null) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }

        try {
            channelPlayService.pauseRtp(streamId);
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
        }catch (ControllerException e) {
            response.setStatusCode(ErrorCode.ERROR100.getCode());
            response.setBody(e.getMessage());
        }

        return response;
    }

    /**
     * 恢复录像回放
     */
    @RedisRpcMapping("resumeRtp")
    public RedisRpcResponse resumeRtp(RedisRpcRequest request) {
        String streamId = request.getParam().toString();
        RedisRpcResponse response = request.getResponse();

        if (streamId == null) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }

        try {
            channelPlayService.resumeRtp(streamId);
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
        }catch (ControllerException e) {
            response.setStatusCode(ErrorCode.ERROR100.getCode());
            response.setBody(e.getMessage());
        }

        return response;
    }


    /**
     * 停止点播国标设备
     */
    @RedisRpcMapping("stop")
    public RedisRpcResponse stop(RedisRpcRequest request) {
        JSONObject jsonObject = JSONObject.parseObject(request.getParam().toString());

        RedisRpcResponse response = request.getResponse();

        Integer channelId = jsonObject.getIntValue("channelId");
        if (channelId == null || channelId <= 0) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }

        String stream = jsonObject.getString("stream");
        InviteSessionType type = jsonObject.getObject("inviteSessionType", InviteSessionType.class);

        // 获取对应的设备和通道信息
        CommonGBChannel channel = channelService.getOne(channelId);
        if (channel == null) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        try {
            channelPlayService.stopPlay(type, channel, stream);
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
        }catch (Exception e){
            response.setStatusCode(Response.SERVER_INTERNAL_ERROR);
            response.setBody(e.getMessage());
        }
        return response;
    }

    /**
     * 录像回放国标设备
     */
    @RedisRpcMapping("playback")
    public RedisRpcResponse playbackChannel(RedisRpcRequest request) {
        JSONObject paramJson = JSONObject.parseObject(request.getParam().toString());
        int channelId = paramJson.getIntValue("channelId");
        String startTime = paramJson.getString("startTime");
        String endTime = paramJson.getString("endTime");
        RedisRpcResponse response = request.getResponse();

        if (channelId <= 0) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        // 获取对应的设备和通道信息
        CommonGBChannel channel = channelService.getOne(channelId);
        if (channel == null) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }

        InviteMessageInfo inviteInfo = new InviteMessageInfo();
        inviteInfo.setSessionName("Playback");
        inviteInfo.setStartTime(DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime));
        inviteInfo.setStopTime(DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime));
        channelPlayService.start(channel, inviteInfo, null, (code, msg, data) ->{
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
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
     * 录像回放国标设备
     */
    @RedisRpcMapping("download")
    public RedisRpcResponse downloadChannel(RedisRpcRequest request) {
        JSONObject paramJson = JSONObject.parseObject(request.getParam().toString());
        int channelId = paramJson.getIntValue("channelId");
        String startTime = paramJson.getString("startTime");
        String endTime = paramJson.getString("endTime");
        int downloadSpeed = paramJson.getIntValue("downloadSpeed");
        RedisRpcResponse response = request.getResponse();

        if (channelId <= 0) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        // 获取对应的设备和通道信息
        CommonGBChannel channel = channelService.getOne(channelId);
        if (channel == null) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }

        InviteMessageInfo inviteInfo = new InviteMessageInfo();
        inviteInfo.setSessionName("Download");
        inviteInfo.setStartTime(DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime));
        inviteInfo.setStopTime(DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime));
        inviteInfo.setDownloadSpeed(downloadSpeed + "");
        channelPlayService.start(channel, inviteInfo, null, (code, msg, data) ->{
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
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
     * 云台控制
     */
    @RedisRpcMapping("ptz/frontEndCommand")
    public RedisRpcResponse frontEndCommand(RedisRpcRequest request) {
        JSONObject paramJson = JSONObject.parseObject(request.getParam().toString());
        int channelId = paramJson.getIntValue("channelId");
        int cmdCode = paramJson.getIntValue("cmdCode");
        int parameter1 = paramJson.getIntValue("parameter1");
        int parameter2 = paramJson.getIntValue("parameter2");
        int combindCode2 = paramJson.getIntValue("combindCode2");

        RedisRpcResponse response = request.getResponse();

        if (channelId <= 0 || cmdCode < 0 || parameter1 < 0 || parameter2 < 0 || combindCode2 < 0) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        // 获取对应的设备和通道信息
        CommonGBChannel channel = channelService.getOne(channelId);
        if (channel == null) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        try {
            iptzService.frontEndCommand(channel, cmdCode, parameter1, parameter2, combindCode2);
        }catch (ControllerException e) {
            response.setStatusCode(ErrorCode.ERROR100.getCode());
            response.setBody(e.getMessage());
            return response;
        }
        response.setStatusCode(ErrorCode.SUCCESS.getCode());
        return response;
    }
}
