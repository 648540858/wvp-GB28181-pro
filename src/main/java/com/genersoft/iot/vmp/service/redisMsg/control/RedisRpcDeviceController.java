package com.genersoft.iot.vmp.service.redisMsg.control;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.redis.RedisRpcConfig;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcMessage;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcRequest;
import com.genersoft.iot.vmp.conf.redis.bean.RedisRpcResponse;
import com.genersoft.iot.vmp.gb28181.bean.BasicParam;
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
        BasicParam basicParam = JSONObject.parseObject(request.getParam().toString(), BasicParam.class);

        Device device = deviceService.getDeviceByDeviceId(basicParam.getDeviceId());

        RedisRpcResponse response = request.getResponse();
        if (device == null || !userSetting.getServerId().equals(device.getServerId())) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        deviceService.deviceBasicConfig(device, basicParam, (code, msg, data) -> {
                    response.setStatusCode(code);
                    response.setBody(new WVPResult<>(code, msg, data));
                    // 手动发送结果
                    sendResponse(response);
                });
        return null;
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
        deviceService.deviceConfigQuery(device, channelId, configType, (code, msg, data) -> {
            response.setStatusCode(code);
            response.setBody(new WVPResult<>(code, msg, data));
            // 手动发送结果
            sendResponse(response);
        });
        return null;
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
            response.setBody(WVPResult.fail(ErrorCode.ERROR100.getCode(), e.getMsg()));
            return response;
        }
        response.setStatusCode(ErrorCode.SUCCESS.getCode());
        response.setBody(WVPResult.success());
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
            deviceService.record(device, channelId, recordCmdStr, (code, msg, data) -> {
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
                response.setBody(new WVPResult<>(code, msg, data));
                // 手动发送结果
                sendResponse(response);
            });
        }catch (ControllerException e) {
            response.setStatusCode(e.getCode());
            response.setBody(WVPResult.fail(ErrorCode.ERROR100.getCode(), e.getMsg()));
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
            deviceService.guard(device, guardCmdStr, (code, msg, data) -> {
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
                response.setBody(new WVPResult<>(code, msg, data));
                // 手动发送结果
                sendResponse(response);
            });
        }catch (ControllerException e) {
            response.setStatusCode(e.getCode());
            response.setBody(WVPResult.fail(ErrorCode.ERROR100.getCode(), e.getMsg()));
            sendResponse(response);
        }
        return null;
    }

    @RedisRpcMapping("resetAlarm")
    public RedisRpcResponse resetAlarm(RedisRpcRequest request) {
        JSONObject paramJson = JSONObject.parseObject(request.getParam().toString());
        String deviceId = paramJson.getString("deviceId");
        String channelId = paramJson.getString("channelId");
        String alarmMethod = paramJson.getString("alarmMethod");
        String alarmType = paramJson.getString("alarmType");

        Device device = deviceService.getDeviceByDeviceId(deviceId);

        RedisRpcResponse response = request.getResponse();
        if (device == null || !userSetting.getServerId().equals(device.getServerId())) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        try {
            deviceService.resetAlarm(device, channelId, alarmMethod, alarmType, (code, msg, data) -> {
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
                response.setBody(new WVPResult<>(code, msg, data));
                // 手动发送结果
                sendResponse(response);
            });
        }catch (ControllerException e) {
            response.setStatusCode(e.getCode());
            response.setBody(WVPResult.fail(ErrorCode.ERROR100.getCode(), e.getMsg()));
            sendResponse(response);
        }
        return null;
    }

    @RedisRpcMapping("iFrame")
    public RedisRpcResponse iFrame(RedisRpcRequest request) {
        JSONObject paramJson = JSONObject.parseObject(request.getParam().toString());
        String deviceId = paramJson.getString("deviceId");
        String channelId = paramJson.getString("channelId");

        Device device = deviceService.getDeviceByDeviceId(deviceId);

        RedisRpcResponse response = request.getResponse();
        if (device == null || !userSetting.getServerId().equals(device.getServerId())) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        try {
            deviceService.iFrame(device, channelId);
        }catch (ControllerException e) {
            response.setStatusCode(e.getCode());
            response.setBody(WVPResult.fail(ErrorCode.ERROR100.getCode(), e.getMsg()));
            sendResponse(response);
        }
        return null;
    }

    @RedisRpcMapping("homePosition")
    public RedisRpcResponse homePosition(RedisRpcRequest request) {
        JSONObject paramJson = JSONObject.parseObject(request.getParam().toString());
        String deviceId = paramJson.getString("deviceId");
        String channelId = paramJson.getString("channelId");

        Boolean enabled = paramJson.getBoolean("enabled");
        Integer resetTime = paramJson.getInteger("resetTime");
        Integer presetIndex = paramJson.getInteger("presetIndex");

        Device device = deviceService.getDeviceByDeviceId(deviceId);

        RedisRpcResponse response = request.getResponse();
        if (device == null || !userSetting.getServerId().equals(device.getServerId())) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        try {
            deviceService.homePosition(device, channelId, enabled, resetTime, presetIndex, (code, msg, data) -> {
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
                response.setBody(new WVPResult<>(code, msg, data));
                // 手动发送结果
                sendResponse(response);
            });
        }catch (ControllerException e) {
            response.setStatusCode(e.getCode());
            response.setBody(WVPResult.fail(ErrorCode.ERROR100.getCode(), e.getMsg()));
            sendResponse(response);
        }
        return null;
    }

    @RedisRpcMapping("dragZoomIn")
    public RedisRpcResponse dragZoomIn(RedisRpcRequest request) {
        JSONObject paramJson = JSONObject.parseObject(request.getParam().toString());
        String deviceId = paramJson.getString("deviceId");
        String channelId = paramJson.getString("channelId");
        Integer length = paramJson.getInteger("length");
        Integer width = paramJson.getInteger("width");
        Integer midpointx = paramJson.getInteger("midpointx");
        Integer midpointy = paramJson.getInteger("midpointy");
        Integer lengthx = paramJson.getInteger("lengthx");
        Integer lengthy = paramJson.getInteger("lengthy");

        Device device = deviceService.getDeviceByDeviceId(deviceId);

        RedisRpcResponse response = request.getResponse();
        if (device == null || !userSetting.getServerId().equals(device.getServerId())) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        try {
            deviceService.dragZoomIn(device, channelId, length, width, midpointx, midpointy, lengthx, lengthy, (code, msg, data) -> {
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
                response.setBody(new WVPResult<>(code, msg, data));
                // 手动发送结果
                sendResponse(response);
            });
        }catch (ControllerException e) {
            response.setStatusCode(e.getCode());
            response.setBody(WVPResult.fail(ErrorCode.ERROR100.getCode(), e.getMsg()));
            sendResponse(response);
        }
        return null;
    }

    @RedisRpcMapping("dragZoomOut")
    public RedisRpcResponse dragZoomOut(RedisRpcRequest request) {
        JSONObject paramJson = JSONObject.parseObject(request.getParam().toString());
        String deviceId = paramJson.getString("deviceId");
        String channelId = paramJson.getString("channelId");
        Integer length = paramJson.getInteger("length");
        Integer width = paramJson.getInteger("width");
        Integer midpointx = paramJson.getInteger("midpointx");
        Integer midpointy = paramJson.getInteger("midpointy");
        Integer lengthx = paramJson.getInteger("lengthx");
        Integer lengthy = paramJson.getInteger("lengthy");

        Device device = deviceService.getDeviceByDeviceId(deviceId);

        RedisRpcResponse response = request.getResponse();
        if (device == null || !userSetting.getServerId().equals(device.getServerId())) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        try {
            deviceService.dragZoomOut(device, channelId, length, width, midpointx, midpointy, lengthx, lengthy, (code, msg, data) -> {
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
                response.setBody(new WVPResult<>(code, msg, data));
                // 手动发送结果
                sendResponse(response);
            });
        }catch (ControllerException e) {
            response.setStatusCode(e.getCode());
            response.setBody(WVPResult.fail(ErrorCode.ERROR100.getCode(), e.getMsg()));
            sendResponse(response);
        }
        return null;
    }

    @RedisRpcMapping("alarm")
    public RedisRpcResponse alarm(RedisRpcRequest request) {

        JSONObject paramJson = JSONObject.parseObject(request.getParam().toString());
        String deviceId = paramJson.getString("deviceId");
        String startPriority = paramJson.getString("startPriority");
        String endPriority = paramJson.getString("endPriority");
        String alarmMethod = paramJson.getString("alarmMethod");
        String alarmType = paramJson.getString("alarmType");
        String startTime = paramJson.getString("startTime");
        String endTime = paramJson.getString("endTime");

        Device device = deviceService.getDeviceByDeviceId(deviceId);

        RedisRpcResponse response = request.getResponse();
        if (device == null || !userSetting.getServerId().equals(device.getServerId())) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        try {
            deviceService.alarm(device, startPriority, endPriority, alarmMethod, alarmType, startTime, endTime, (code, msg, data) -> {
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
                response.setBody(new WVPResult<>(code, msg, data));
                // 手动发送结果
                sendResponse(response);
            });
        }catch (ControllerException e) {
            response.setStatusCode(e.getCode());
            response.setBody(WVPResult.fail(ErrorCode.ERROR100.getCode(), e.getMsg()));
            sendResponse(response);
        }
        return null;
    }

    @RedisRpcMapping("deviceStatus")
    public RedisRpcResponse deviceStatus(RedisRpcRequest request) {
        String deviceId = request.getParam().toString();

        Device device = deviceService.getDeviceByDeviceId(deviceId);

        RedisRpcResponse response = request.getResponse();
        if (device == null || !userSetting.getServerId().equals(device.getServerId())) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        try {
            deviceService.deviceStatus(device, (code, msg, data) -> {
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
                response.setBody(new WVPResult<>(code, msg, data));
                // 手动发送结果
                sendResponse(response);
            });
        }catch (ControllerException e) {
            response.setStatusCode(e.getCode());
            response.setBody(WVPResult.fail(ErrorCode.ERROR100.getCode(), e.getMsg()));
            sendResponse(response);
        }
        return null;
    }

    @RedisRpcMapping("info")
    public RedisRpcResponse info(RedisRpcRequest request) {
        String deviceId = request.getParam().toString();

        Device device = deviceService.getDeviceByDeviceId(deviceId);

        RedisRpcResponse response = request.getResponse();
        if (device == null || !userSetting.getServerId().equals(device.getServerId())) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        try {
            deviceService.deviceInfo(device, (code, msg, data) -> {
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
                response.setBody(new WVPResult<>(code, msg, data));
                // 手动发送结果
                sendResponse(response);
            });
        }catch (ControllerException e) {
            response.setStatusCode(e.getCode());
            response.setBody(WVPResult.fail(ErrorCode.ERROR100.getCode(), e.getMsg()));
            sendResponse(response);
        }
        return null;
    }

    @RedisRpcMapping("info")
    public RedisRpcResponse queryPreset(RedisRpcRequest request) {
        JSONObject paramJson = JSONObject.parseObject(request.getParam().toString());
        String deviceId = paramJson.getString("deviceId");
        String channelId = paramJson.getString("channelId");

        Device device = deviceService.getDeviceByDeviceId(deviceId);

        RedisRpcResponse response = request.getResponse();
        if (device == null || !userSetting.getServerId().equals(device.getServerId())) {
            response.setStatusCode(ErrorCode.ERROR400.getCode());
            response.setBody("param error");
            return response;
        }
        try {
            deviceService.queryPreset(device, channelId, (code, msg, data) -> {
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
                response.setBody(new WVPResult<>(code, msg, data));
                // 手动发送结果
                sendResponse(response);
            });
        }catch (ControllerException e) {
            response.setStatusCode(e.getCode());
            response.setBody(WVPResult.fail(ErrorCode.ERROR100.getCode(), e.getMsg()));
            sendResponse(response);
        }
        return null;
    }


}
