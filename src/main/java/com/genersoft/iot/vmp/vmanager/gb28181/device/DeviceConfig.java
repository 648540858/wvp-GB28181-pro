/**
 * 设备设置命令API接口
 * 
 * @author lawrencehj
 * @date 2021年2月2日
 */

package com.genersoft.iot.vmp.vmanager.gb28181.device;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.UUID;

@Api(tags = "国标设备配置")
@CrossOrigin
@RestController
@RequestMapping("/api/device/config")
public class DeviceConfig {

    private final static Logger logger = LoggerFactory.getLogger(DeviceQuery.class);

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private DeferredResultHolder resultHolder;

	/**
	 * 看守位控制命令API接口
	 * @param deviceId 设备ID
	 * @param channelId 通道ID
	 * @param name 名称
	 * @param expiration 到期时间
	 * @param heartBeatInterval 心跳间隔
	 * @param heartBeatCount 心跳计数
	 * @return
	 */
	@ApiOperation("基本配置设置命令")
	@GetMapping("/basicParam/{deviceId}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value ="设备ID" ,dataTypeClass = String.class),
			@ApiImplicitParam(name = "channelId", value ="通道ID",dataTypeClass = String.class ),
			@ApiImplicitParam(name = "name", value ="名称" ,dataTypeClass = String.class),
			@ApiImplicitParam(name = "expiration", value ="到期时间" ,dataTypeClass = String.class),
			@ApiImplicitParam(name = "heartBeatInterval", value ="心跳间隔" ,dataTypeClass = String.class),
			@ApiImplicitParam(name = "heartBeatCount", value ="心跳计数" ,dataTypeClass = String.class),
	})
	public DeferredResult<ResponseEntity<String>> homePositionApi(@PathVariable String deviceId,
                                                               	String channelId,
                                                                @RequestParam(required = false) String name,
																@RequestParam(required = false) String expiration,
																@RequestParam(required = false) String heartBeatInterval,
                                                                @RequestParam(required = false) String heartBeatCount) {
        if (logger.isDebugEnabled()) {
			logger.debug("报警复位API调用");
		}
		Device device = storager.queryVideoDevice(deviceId);
		String uuid = UUID.randomUUID().toString();
		String key = DeferredResultHolder.CALLBACK_CMD_DEVICECONFIG + deviceId + channelId;
		cmder.deviceBasicConfigCmd(device, channelId, name, expiration, heartBeatInterval, heartBeatCount, event -> {
			RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
			msg.setKey(key);
			msg.setData(String.format("设备配置操作失败，错误码： %s, %s", event.statusCode, event.msg));
			resultHolder.invokeResult(msg);
		});
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String>>(3 * 1000L);
		result.onTimeout(() -> {
			logger.warn(String.format("设备配置操作超时, 设备未返回应答指令"));
			// 释放rtpserver
			RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
			msg.setKey(key);
			JSONObject json = new JSONObject();
			json.put("DeviceID", deviceId);
			json.put("Status", "Timeout");
			json.put("Description", "设备配置操作超时, 设备未返回应答指令");
			msg.setData(json); //("看守位控制操作超时, 设备未返回应答指令");
			resultHolder.invokeResult(msg);
		});
		resultHolder.put(key, uuid, result);
		return result;
	}

	/**
	 * 设备配置查询请求API接口
	 * @param deviceId 设备ID
	 * @param configType 配置类型
	 * @param channelId 通道ID
	 * @return
	 */
	@ApiOperation("设备配置查询请求")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value ="设备ID" ,dataTypeClass = String.class),
			@ApiImplicitParam(name = "channelId", value ="通道ID" ,dataTypeClass = String.class),
			@ApiImplicitParam(name = "configType", value ="配置类型" ,dataTypeClass = String.class),
	})
	@GetMapping("/query/{deviceId}/{configType}")
    public DeferredResult<ResponseEntity<String>> configDownloadApi(@PathVariable String deviceId, 
                                                                @PathVariable String configType,
                                                                @RequestParam(required = false) String channelId) {
		if (logger.isDebugEnabled()) {
			logger.debug("设备状态查询API调用");
		}
		String key = DeferredResultHolder.CALLBACK_CMD_CONFIGDOWNLOAD + (StringUtils.isEmpty(channelId) ? deviceId : channelId);
		String uuid = UUID.randomUUID().toString();
		Device device = storager.queryVideoDevice(deviceId);
		cmder.deviceConfigQuery(device, channelId, configType, event -> {
			RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
			msg.setKey(key);
			msg.setData(String.format("获取设备配置失败，错误码： %s, %s", event.statusCode, event.msg));
			resultHolder.invokeResult(msg);
		});
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String >> (3 * 1000L);
		result.onTimeout(()->{
			logger.warn(String.format("获取设备配置超时"));
			// 释放rtpserver
			RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
			msg.setKey(key);
			msg.setData("Timeout. Device did not response to this command.");
			resultHolder.invokeResult(msg);
		});
		resultHolder.put(key, uuid, result);
		return result;
	}

}
