/**
 * 设备设置命令API接口
 * 
 * @author lawrencehj
 * @date 2021年2月2日
 */

package com.genersoft.iot.vmp.vmanager.gb28181.device;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.UUID;

@Tag(name = "国标设备配置")
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
	@GetMapping("/basicParam/{deviceId}")
	@Operation(summary = "基本配置设置命令")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "name", description = "名称")
	@Parameter(name = "expiration", description = "到期时间")
	@Parameter(name = "heartBeatInterval", description = "心跳间隔")
	@Parameter(name = "heartBeatCount", description = "心跳计数")
	public DeferredResult<String> homePositionApi(@PathVariable String deviceId,
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
		try {
			cmder.deviceBasicConfigCmd(device, channelId, name, expiration, heartBeatInterval, heartBeatCount, event -> {
				RequestMessage msg = new RequestMessage();
				msg.setId(uuid);
				msg.setKey(key);
				msg.setData(String.format("设备配置操作失败，错误码： %s, %s", event.statusCode, event.msg));
				resultHolder.invokeResult(msg);
			});
		} catch (InvalidArgumentException | SipException | ParseException e) {
			logger.error("[命令发送失败] 设备配置: {}", e.getMessage());
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
		}
		DeferredResult<String> result = new DeferredResult<String>(3 * 1000L);
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
	@Operation(summary = "设备配置查询请求")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "configType", description = "配置类型")
	@GetMapping("/query/{deviceId}/{configType}")
    public DeferredResult<String> configDownloadApi(@PathVariable String deviceId,
                                                                @PathVariable String configType,
                                                                @RequestParam(required = false) String channelId) {
		if (logger.isDebugEnabled()) {
			logger.debug("设备状态查询API调用");
		}
		String key = DeferredResultHolder.CALLBACK_CMD_CONFIGDOWNLOAD + (ObjectUtils.isEmpty(channelId) ? deviceId : channelId);
		String uuid = UUID.randomUUID().toString();
		Device device = storager.queryVideoDevice(deviceId);
		try {
			cmder.deviceConfigQuery(device, channelId, configType, event -> {
				RequestMessage msg = new RequestMessage();
				msg.setId(uuid);
				msg.setKey(key);
				msg.setData(String.format("获取设备配置失败，错误码： %s, %s", event.statusCode, event.msg));
				resultHolder.invokeResult(msg);
			});
		} catch (InvalidArgumentException | SipException | ParseException e) {
			logger.error("[命令发送失败] 获取设备配置: {}", e.getMessage());
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
		}
		DeferredResult<String> result = new DeferredResult<String > (3 * 1000L);
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
