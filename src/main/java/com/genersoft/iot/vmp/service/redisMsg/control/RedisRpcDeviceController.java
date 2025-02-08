package com.genersoft.iot.vmp.service.redisMsg.control;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.redis.RedisRpcConfig;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcMessage;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcRequest;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcResponse;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.SyncStatus;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.service.redisMsg.dto.RedisRpcController;
import com.genersoft.iot.vmp.service.redisMsg.dto.RedisRpcMapping;
import com.genersoft.iot.vmp.service.redisMsg.dto.RpcController;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

@Component
@Slf4j
@RedisRpcController("device")
public class RedisRpcDeviceController extends RpcController {

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private IDeviceService deviceService;

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
     * 通道同步
     */
    @RedisRpcMapping("devicesSync")
    public RedisRpcResponse devicesSync(RedisRpcRequest request) {
        String deviceId = request.getParam().toString();
        Device device = deviceService.getDeviceByDeviceId(deviceId);

        RedisRpcResponse response = request.getResponse();
        if (device == null || !userSetting.getServerId().equals(device.getServerId())) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        WVPResult<SyncStatus> result = deviceService.devicesSync(device);
        response.setStatusCode(ErrorCode.SUCCESS.getCode());
        response.setBody(JSONObject.toJSONString(result));
        return response;
    }

    /**
     * 获取通道同步状态
     */
    @RedisRpcMapping("getChannelSyncStatus")
    public RedisRpcResponse getChannelSyncStatus(RedisRpcRequest request) {
        String deviceId = request.getParam().toString();

        Device device = deviceService.getDeviceByDeviceId(deviceId);

        RedisRpcResponse response = request.getResponse();
        if (device == null || !userSetting.getServerId().equals(device.getServerId())) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        SyncStatus channelSyncStatus = deviceService.getChannelSyncStatus(deviceId);
        response.setStatusCode(ErrorCode.SUCCESS.getCode());
        response.setBody(JSONObject.toJSONString(channelSyncStatus));
        return response;
    }

    @RedisRpcMapping("deviceBasicConfig")
    public RedisRpcResponse deviceBasicConfig(RedisRpcRequest request) {
        JSONObject paramJson = JSONObject.parseObject(request.getParam().toString());
        String deviceId = paramJson.getString("deviceId");
        String channelId = paramJson.getString("channelId");
        String name = paramJson.getString("configType");
        String expiration = paramJson.getString("expiration");
        String heartBeatInterval = paramJson.getString("heartBeatInterval");

        Device device = deviceService.getDeviceByDeviceId(deviceId);

        RedisRpcResponse response = request.getResponse();
        if (device == null || !userSetting.getServerId().equals(device.getServerId())) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        DeferredResult<String> deferredResult = deviceService.deviceBasicConfig(device, channelId, name, expiration, heartBeatInterval, heartBeatInterval);
        deferredResult.onCompletion(() ->{
            String resultStr = (String)deferredResult.getResult();
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
            response.setBody(resultStr);
            // 手动发送结果
            sendResponse(response);
        });
        deferredResult.onTimeout(() -> {
            log.warn(String.format("设备配置操作超时, 设备未返回应答指令"));
            JSONObject json = new JSONObject();
            json.put("DeviceID", device.getDeviceId());
            json.put("Status", "Timeout");
            json.put("Description", "设备配置操作超时, 设备未返回应答指令");
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
            response.setBody(json);
            // 手动发送结果
            sendResponse(response);
        });
        return response;
    }

    @RedisRpcMapping("deviceConfigQuery")
    public RedisRpcResponse deviceConfigQuery(RedisRpcRequest request) {
        JSONObject paramJson = JSONObject.parseObject(request.getParam().toString());
        String deviceId = paramJson.getString("deviceId");
        String channelId = paramJson.getString("channelId");
        String configType = paramJson.getString("configType");

        Device device = deviceService.getDeviceByDeviceId(deviceId);

        RedisRpcResponse response = request.getResponse();
        if (device == null || !userSetting.getServerId().equals(device.getServerId())) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        DeferredResult<String> deferredResult = deviceService.deviceConfigQuery(device, channelId, configType);
        deferredResult.onCompletion(() ->{
            String resultStr = (String)deferredResult.getResult();
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
            response.setBody(resultStr);
            // 手动发送结果
            sendResponse(response);
        });
        deferredResult.onTimeout(() -> {
            log.warn(String.format("设备配置操作超时, 设备未返回应答指令"));
            JSONObject json = new JSONObject();
            json.put("DeviceID", device.getDeviceId());
            json.put("Status", "Timeout");
            json.put("Description", "设备配置操作超时, 设备未返回应答指令");
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
            response.setBody(json);
            // 手动发送结果
            sendResponse(response);
        });
        return response;
    }

    @RedisRpcMapping("teleboot")
    public RedisRpcResponse teleboot(RedisRpcRequest request) {
        String deviceId = request.getParam().toString();

        Device device = deviceService.getDeviceByDeviceId(deviceId);

        RedisRpcResponse response = request.getResponse();
        if (device == null || !userSetting.getServerId().equals(device.getServerId())) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        try {
            deviceService.teleboot(device);
        }catch (ControllerException e) {
            response.setStatusCode(e.getCode());
            response.setBody(e.getMsg());
            return response;
        }
        response.setStatusCode(ErrorCode.SUCCESS.getCode());
        response.setBody(ErrorCode.SUCCESS.getMsg());
        return response;
    }

    @RedisRpcMapping("record")
    public RedisRpcResponse record(RedisRpcRequest request) {
        JSONObject paramJson = JSONObject.parseObject(request.getParam().toString());
        String deviceId = paramJson.getString("deviceId");
        String channelId = paramJson.getString("channelId");
        String recordCmdStr = paramJson.getString("recordCmdStr");

        Device device = deviceService.getDeviceByDeviceId(deviceId);

        RedisRpcResponse response = request.getResponse();
        if (device == null || !userSetting.getServerId().equals(device.getServerId())) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        try {
            DeferredResult<String> deferredResult = deviceService.record(device, channelId, recordCmdStr);
            deferredResult.onCompletion(() ->{
                String resultStr = (String)deferredResult.getResult();
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
                response.setBody(resultStr);
                // 手动发送结果
                sendResponse(response);
            });
            deferredResult.onTimeout(() -> {
                log.warn("设备录像控制操作超时, 设备未返回应答指令");
                JSONObject json = new JSONObject();
                json.put("DeviceID", device.getDeviceId());
                json.put("Status", "Timeout");
                json.put("Description", "设备录像控制操作超时, 设备未返回应答指令");
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
                response.setBody(json);
                // 手动发送结果
                sendResponse(response);
            });
        }catch (ControllerException e) {
            response.setStatusCode(e.getCode());
            response.setBody(e.getMsg());
            sendResponse(response);
        }
        return null;
    }

    @RedisRpcMapping("guard")
    public RedisRpcResponse guard(RedisRpcRequest request) {
        JSONObject paramJson = JSONObject.parseObject(request.getParam().toString());
        String deviceId = paramJson.getString("deviceId");
        String guardCmdStr = paramJson.getString("guardCmdStr");

        Device device = deviceService.getDeviceByDeviceId(deviceId);

        RedisRpcResponse response = request.getResponse();
        if (device == null || !userSetting.getServerId().equals(device.getServerId())) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        try {
            DeferredResult<WVPResult<String>> deferredResult = deviceService.guard(device, guardCmdStr);
            deferredResult.onCompletion(() ->{
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
                response.setBody(deferredResult.getResult());
                // 手动发送结果
                sendResponse(response);
            });
            deferredResult.onTimeout(() -> {
                log.warn("设备录像控制操作超时, 设备未返回应答指令");
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
                response.setBody(WVPResult.fail(ErrorCode.ERROR100.getCode(), "操作超时, 设备未应答"));
                // 手动发送结果
                sendResponse(response);
            });
        }catch (ControllerException e) {
            response.setStatusCode(e.getCode());
            response.setBody(e.getMsg());
            sendResponse(response);
        }
        return null;
    }



}
