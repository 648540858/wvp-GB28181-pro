package com.genersoft.iot.vmp.service.redisMsg.control;

import com.alibaba.fastjson2.JSON;
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
        DeferredResult<WVPResult<String>> deferredResult = deviceService.deviceConfigQuery(device, channelId, configType);
        deferredResult.onCompletion(() ->{
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
            response.setBody(deferredResult.getResult());
            // 手动发送结果
            sendResponse(response);
        });
        deferredResult.onTimeout(() -> {
            log.warn("[设备配置]操作超时, 设备未返回应答指令, {}", deviceId);
            response.setStatusCode(ErrorCode.SUCCESS.getCode());
            response.setBody(WVPResult.fail(ErrorCode.ERROR100.getCode(), "超时"));
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
            DeferredResult<WVPResult<String>> deferredResult = deviceService.record(device, channelId, recordCmdStr);
            deferredResult.onCompletion(() ->{
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
                response.setBody(deferredResult.getResult());
                // 手动发送结果
                sendResponse(response);
            });
            deferredResult.onTimeout(() -> {
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
                response.setBody(WVPResult.fail(ErrorCode.ERROR100.getCode(), "操作超时, 设备未应答"));
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
            DeferredResult<WVPResult<String>> deferredResult = deviceService.guard(device, guardCmdStr);
            deferredResult.onCompletion(() ->{
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
                response.setBody(deferredResult.getResult());
                // 手动发送结果
                sendResponse(response);
            });
            deferredResult.onTimeout(() -> {
                log.warn("[布防/撤防]操作超时, 设备未返回应答指令");
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
                response.setBody(WVPResult.fail(ErrorCode.ERROR100.getCode(), "操作超时, 设备未应答"));
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
            DeferredResult<WVPResult<String>> deferredResult = deviceService.resetAlarm(device, channelId, alarmMethod, alarmType);
            deferredResult.onCompletion(() ->{
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
                response.setBody(deferredResult.getResult());
                // 手动发送结果
                sendResponse(response);
            });
            deferredResult.onTimeout(() -> {
                log.warn("[报警重置] 操作超时, 设备未返回应答指令");
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
                response.setBody(WVPResult.fail(ErrorCode.ERROR100.getCode(), "操作超时, 设备未应答"));
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
            DeferredResult<WVPResult<String>> deferredResult = deviceService.homePosition(device, channelId, enabled, resetTime, presetIndex);
            deferredResult.onCompletion(() ->{
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
                response.setBody(deferredResult.getResult());
                // 手动发送结果
                sendResponse(response);
            });
            deferredResult.onTimeout(() -> {
                log.warn("[看守位控制] 操作超时, 设备未返回应答指令");
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
                response.setBody(WVPResult.fail(ErrorCode.ERROR100.getCode(), "操作超时, 设备未应答"));
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
            deviceService.dragZoomIn(device, channelId, length, width, midpointx, midpointy, lengthx, lengthy);
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
            deviceService.dragZoomOut(device, channelId, length, width, midpointx, midpointy, lengthx, lengthy);
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
            DeferredResult<WVPResult<String>> deferredResult = deviceService.deviceStatus(device);
            deferredResult.onCompletion(() ->{
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
                response.setBody(deferredResult.getResult());
                // 手动发送结果
                sendResponse(response);
            });
            deferredResult.onTimeout(() -> {
                log.warn("[获取设备状态] 操作超时, 设备未返回应答指令");
                response.setStatusCode(ErrorCode.SUCCESS.getCode());
                response.setBody(WVPResult.fail(ErrorCode.ERROR100.getCode(), "操作超时, 设备未应答"));
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
