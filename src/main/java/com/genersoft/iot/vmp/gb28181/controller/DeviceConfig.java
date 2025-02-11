/**
 * 设备设置命令API接口
 * 
 * @author lawrencehj
 * @date 2021年2月2日
 */

package com.genersoft.iot.vmp.gb28181.controller;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.UUID;

@Slf4j
@Tag(name = "国标设备配置")
@RestController
@RequestMapping("/api/device/config")
public class DeviceConfig {

    @Autowired
    private IDeviceService deviceService;

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
	@Operation(summary = "基本配置设置命令", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "name", description = "名称")
	@Parameter(name = "expiration", description = "到期时间")
	@Parameter(name = "heartBeatInterval", description = "心跳间隔")
	@Parameter(name = "heartBeatCount", description = "心跳计数")
	public DeferredResult<String> homePositionApi(@PathVariable String deviceId,
												  @RequestParam(required = false) String channelId,
                                                                @RequestParam(required = false) String name,
																@RequestParam(required = false) String expiration,
																@RequestParam(required = false) String heartBeatInterval,
                                                                @RequestParam(required = false) String heartBeatCount) {
        if (log.isDebugEnabled()) {
			log.debug("报警复位API调用");
		}
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "设备不存在");
		DeferredResult<String> result = deviceService.deviceBasicConfig(device, channelId, name, expiration, heartBeatInterval, heartBeatCount);

		result.onTimeout(() -> {
			log.warn(String.format("设备配置操作超时, 设备未返回应答指令"));
			JSONObject json = new JSONObject();
			json.put("DeviceID", device.getDeviceId());
			json.put("Status", "Timeout");
			json.put("Description", "设备配置操作超时, 设备未返回应答指令");
			result.setResult(json.toString());
		});
		return result;

	}

	/**
	 * 设备配置查询请求API接口
	 * @param deviceId 设备ID
	 * @param configType 配置类型
	 * @param channelId 通道ID
	 * @return
	 */
	@Operation(summary = "设备配置查询请求", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "configType", description = "配置类型")
	@GetMapping("/query/{deviceId}/{configType}")
    public DeferredResult<WVPResult<String>> configDownloadApi(@PathVariable String deviceId,
													   @PathVariable String configType,
													   @RequestParam(required = false) String channelId) {
		if (log.isDebugEnabled()) {
			log.debug("设备状态查询API调用");
		}
		String key = DeferredResultHolder.CALLBACK_CMD_CONFIGDOWNLOAD + (ObjectUtils.isEmpty(channelId) ? deviceId : deviceId + channelId);
		String uuid = UUID.randomUUID().toString();
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "设备不存在");

		DeferredResult<WVPResult<String>> result = deviceService.deviceConfigQuery(device, channelId, configType);

		result.onTimeout(() -> {
			log.warn("[获取设备配置] 超时, {}", device.getDeviceId());
			result.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "超时"));
		});
		return result;
	}

}
