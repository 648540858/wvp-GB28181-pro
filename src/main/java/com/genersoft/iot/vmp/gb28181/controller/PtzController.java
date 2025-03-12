package com.genersoft.iot.vmp.gb28181.controller;


import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IPTZService;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

@Tag(name  = "前端设备控制")
@Slf4j
@RestController
@RequestMapping("/api/front-end")
public class PtzController {

	@Autowired
	private SIPCommander cmder;

	@Autowired
	private IDeviceService deviceService;

	@Autowired
	private IPTZService ptzService;

	@Autowired
	private DeferredResultHolder resultHolder;

	@Operation(summary = "通用前端控制命令(参考国标文档A.3.1指令格式)", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "cmdCode", description = "指令码(对应国标文档指令格式中的字节4)", required = true)
	@Parameter(name = "parameter1", description = "数据一(对应国标文档指令格式中的字节5, 范围0-255)", required = true)
	@Parameter(name = "parameter2", description = "数据二(对应国标文档指令格式中的字节6, 范围0-255)", required = true)
	@Parameter(name = "combindCode2", description = "组合码二(对应国标文档指令格式中的字节7, 范围0-15)", required = true)
	@GetMapping("/common/{deviceId}/{channelId}")
	public void frontEndCommand(@PathVariable String deviceId,@PathVariable String channelId,Integer cmdCode, Integer parameter1, Integer parameter2, Integer combindCode2){

		if (log.isDebugEnabled()) {
			log.debug(String.format("设备云台控制 API调用，deviceId：%s ，channelId：%s ，cmdCode：%d parameter1：%d parameter2：%d",deviceId, channelId, cmdCode, parameter1, parameter2));
		}

		if (parameter1 == null || parameter1 < 0 || parameter1 > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "parameter1 为 0-255的数字");
		}
		if (parameter2 == null || parameter2 < 0 || parameter2 > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "parameter2 为 0-255的数字");
		}
		if (combindCode2 == null || combindCode2 < 0 || combindCode2 > 15) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "combindCode2 为 0-15的数字");
		}

		Device device = deviceService.getDeviceByDeviceId(deviceId);

		Assert.notNull(device, "设备[" + deviceId + "]不存在");

		ptzService.frontEndCommand(device, channelId, cmdCode, parameter1, parameter2, combindCode2);
	}

	@Operation(summary = "云台控制", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "command", description = "控制指令,允许值: left, right, up, down, upleft, upright, downleft, downright, zoomin, zoomout, stop", required = true)
	@Parameter(name = "horizonSpeed", description = "水平速度(0-255)", required = true)
	@Parameter(name = "verticalSpeed", description = "垂直速度(0-255)", required = true)
	@Parameter(name = "zoomSpeed", description = "缩放速度(0-15)", required = true)
	@GetMapping("/ptz/{deviceId}/{channelId}")
	public void ptz(@PathVariable String deviceId,@PathVariable String channelId, String command, Integer horizonSpeed, Integer verticalSpeed, Integer zoomSpeed){

		if (log.isDebugEnabled()) {
			log.debug(String.format("设备云台控制 API调用，deviceId：%s ，channelId：%s ，command：%s ，horizonSpeed：%d ，verticalSpeed：%d ，zoomSpeed：%d",deviceId, channelId, command, horizonSpeed, verticalSpeed, zoomSpeed));
		}
		if (horizonSpeed == null) {
			horizonSpeed = 100;
		}else if (horizonSpeed < 0 || horizonSpeed > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "horizonSpeed 为 0-255的数字");
		}
		if (verticalSpeed == null) {
			verticalSpeed = 100;
		}else if (verticalSpeed < 0 || verticalSpeed > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "verticalSpeed 为 0-255的数字");
		}
		if (zoomSpeed == null) {
			zoomSpeed = 16;
		}else if (zoomSpeed < 0 || zoomSpeed > 15) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "zoomSpeed 为 0-15的数字");
		}

		int cmdCode = 0;
		switch (command){
			case "left":
				cmdCode = 2;
				break;
			case "right":
				cmdCode = 1;
				break;
			case "up":
				cmdCode = 8;
				break;
			case "down":
				cmdCode = 4;
				break;
			case "upleft":
				cmdCode = 10;
				break;
			case "upright":
				cmdCode = 9;
				break;
			case "downleft":
				cmdCode = 6;
				break;
			case "downright":
				cmdCode = 5;
				break;
			case "zoomin":
				cmdCode = 16;
				break;
			case "zoomout":
				cmdCode = 32;
				break;
			case "stop":
				horizonSpeed = 0;
				verticalSpeed = 0;
				zoomSpeed = 0;
				break;
			default:
				break;
		}
		frontEndCommand(deviceId, channelId, cmdCode, horizonSpeed, verticalSpeed, zoomSpeed);
	}


	@Operation(summary = "光圈控制", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "command", description = "控制指令,允许值: in, out, stop", required = true)
	@Parameter(name = "speed", description = "光圈速度(0-255)", required = true)
	@GetMapping("/fi/iris/{deviceId}/{channelId}")
	public void iris(@PathVariable String deviceId,@PathVariable String channelId, String command, Integer speed){

		if (log.isDebugEnabled()) {
			log.debug("设备光圈控制 API调用，deviceId：{} ，channelId：{} ，command：{} ，speed：{} ",deviceId, channelId, command, speed);
		}

		if (speed == null) {
			speed = 100;
		}else if (speed < 0 || speed > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "speed 为 0-255的数字");
		}

		int cmdCode = 0x40;
		switch (command){
			case "in":
				cmdCode = 0x44;
				break;
			case "out":
				cmdCode = 0x48;
				break;
			case "stop":
				speed = 0;
				break;
			default:
				break;
		}
		frontEndCommand(deviceId, channelId, cmdCode, 0, speed, 0);
	}

	@Operation(summary = "聚焦控制", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "command", description = "控制指令,允许值: near, far, stop", required = true)
	@Parameter(name = "speed", description = "聚焦速度(0-255)", required = true)
	@GetMapping("/fi/focus/{deviceId}/{channelId}")
	public void focus(@PathVariable String deviceId,@PathVariable String channelId, String command, Integer speed){

		if (log.isDebugEnabled()) {
			log.debug("设备聚焦控制 API调用，deviceId：{} ，channelId：{} ，command：{} ，speed：{} ",deviceId, channelId, command, speed);
		}

		if (speed == null) {
			speed = 100;
		}else if (speed < 0 || speed > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "speed 为 0-255的数字");
		}

		int cmdCode = 0x40;
		switch (command){
			case "near":
				cmdCode = 0x42;
				break;
			case "far":
				cmdCode = 0x41;
				break;
			case "stop":
				speed = 0;
				break;
			default:
				break;
		}
		frontEndCommand(deviceId, channelId, cmdCode, speed, 0, 0);
	}

	@Operation(summary = "查询预置位", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@GetMapping("/preset/query/{deviceId}/{channelId}")
	public DeferredResult<WVPResult<Object>> queryPreset(@PathVariable String deviceId, @PathVariable String channelId) {
		if (log.isDebugEnabled()) {
			log.debug("设备预置位查询API调用");
		}
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "设备不存在");
		DeferredResult<WVPResult<Object>> deferredResult = new DeferredResult<> (3 * 1000L);
		deviceService.queryPreset(device, channelId, (code, msg, data) -> {
			deferredResult.setResult(new WVPResult<>(code, msg, data));
		});

		deferredResult.onTimeout(()->{
			log.warn("[获取设备预置位] 超时, {}", device.getDeviceId());
			deferredResult.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "超时"));
		});
		return deferredResult;
	}

	@Operation(summary = "预置位指令-设置预置位", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "presetId", description = "预置位编号(1-255)", required = true)
	@GetMapping("/preset/add/{deviceId}/{channelId}")
	public void addPreset(@PathVariable String deviceId, @PathVariable String channelId, Integer presetId) {
		if (presetId == null || presetId < 1 || presetId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "预置位编号必须为1-255之间的数字");
		}
		frontEndCommand(deviceId, channelId, 0x81, 1, presetId, 0);
	}

	@Operation(summary = "预置位指令-调用预置位", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "presetId", description = "预置位编号(1-255)", required = true)
	@GetMapping("/preset/call/{deviceId}/{channelId}")
	public void callPreset(@PathVariable String deviceId, @PathVariable String channelId, Integer presetId) {
		if (presetId == null || presetId < 1 || presetId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "预置位编号必须为1-255之间的数字");
		}
		frontEndCommand(deviceId, channelId, 0x82, 1, presetId, 0);
	}

	@Operation(summary = "预置位指令-删除预置位", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "presetId", description = "预置位编号(1-255)", required = true)
	@GetMapping("/preset/delete/{deviceId}/{channelId}")
	public void deletePreset(@PathVariable String deviceId, @PathVariable String channelId, Integer presetId) {
		if (presetId == null || presetId < 1 || presetId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "预置位编号必须为1-255之间的数字");
		}
		frontEndCommand(deviceId, channelId, 0x83, 1, presetId, 0);
	}

	@Operation(summary = "巡航指令-加入巡航点", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "cruiseId", description = "巡航组号(0-255)", required = true)
	@Parameter(name = "presetId", description = "预置位编号(1-255)", required = true)
	@GetMapping("/cruise/point/add/{deviceId}/{channelId}")
	public void addCruisePoint(@PathVariable String deviceId, @PathVariable String channelId, Integer cruiseId, Integer presetId) {
		if (presetId == null || cruiseId == null || presetId < 1 || presetId > 255 || cruiseId < 0 || cruiseId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "编号必须为1-255之间的数字");
		}
		frontEndCommand(deviceId, channelId, 0x84, cruiseId, presetId, 0);
	}

	@Operation(summary = "巡航指令-删除一个巡航点", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "cruiseId", description = "巡航组号(1-255)", required = true)
	@Parameter(name = "presetId", description = "预置位编号(0-255, 为0时删除整个巡航)", required = true)
	@GetMapping("/cruise/point/delete/{deviceId}/{channelId}")
	public void deleteCruisePoint(@PathVariable String deviceId, @PathVariable String channelId, Integer cruiseId, Integer presetId) {
		if (presetId == null || presetId < 0 || presetId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "预置位编号必须为0-255之间的数字, 为0时删除整个巡航");
		}
		if (cruiseId == null || cruiseId < 0 || cruiseId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "巡航组号必须为0-255之间的数字");
		}
		frontEndCommand(deviceId, channelId, 0x85, cruiseId, presetId, 0);
	}

	@Operation(summary = "巡航指令-设置巡航速度", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "cruiseId", description = "巡航组号(0-255)", required = true)
	@Parameter(name = "speed", description = "巡航速度(1-4095)", required = true)
	@GetMapping("/cruise/speed/{deviceId}/{channelId}")
	public void setCruiseSpeed(@PathVariable String deviceId, @PathVariable String channelId, Integer cruiseId, Integer speed) {
		if (cruiseId == null || cruiseId < 0 || cruiseId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "巡航组号必须为0-255之间的数字");
		}
		if (speed == null || speed < 1 || speed > 4095) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "巡航速度必须为1-4095之间的数字");
		}
		int parameter2 = speed & 0xFF;
		int combindCode2 =  speed >> 8;
		frontEndCommand(deviceId, channelId, 0x86, cruiseId, parameter2, combindCode2);
	}

	@Operation(summary = "巡航指令-设置巡航停留时间", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "cruiseId", description = "巡航组号", required = true)
	@Parameter(name = "time", description = "巡航停留时间(1-4095)", required = true)
	@GetMapping("/cruise/time/{deviceId}/{channelId}")
	public void setCruiseTime(@PathVariable String deviceId, @PathVariable String channelId, Integer cruiseId, Integer time) {
		if (cruiseId == null || cruiseId < 0 || cruiseId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "巡航组号必须为0-255之间的数字");
		}
		if (time == null || time < 1 || time > 4095) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "巡航停留时间必须为1-4095之间的数字");
		}
		int parameter2 = time & 0xFF;
		int combindCode2 =  time >> 8;
		frontEndCommand(deviceId, channelId, 0x87, cruiseId, parameter2, combindCode2);
	}

	@Operation(summary = "巡航指令-开始巡航", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "cruiseId", description = "巡航组号)", required = true)
	@GetMapping("/cruise/start/{deviceId}/{channelId}")
	public void startCruise(@PathVariable String deviceId, @PathVariable String channelId, Integer cruiseId) {
		if (cruiseId == null || cruiseId < 0 || cruiseId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "巡航组号必须为0-255之间的数字");
		}
		frontEndCommand(deviceId, channelId, 0x88, cruiseId, 0, 0);
	}

	@Operation(summary = "巡航指令-停止巡航", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "cruiseId", description = "巡航组号", required = true)
	@GetMapping("/cruise/stop/{deviceId}/{channelId}")
	public void stopCruise(@PathVariable String deviceId, @PathVariable String channelId, Integer cruiseId) {
		if (cruiseId == null || cruiseId < 0 || cruiseId > 255) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "巡航组号必须为0-255之间的数字");
		}
		frontEndCommand(deviceId, channelId, 0, 0, 0, 0);
	}

	@Operation(summary = "扫描指令-开始自动扫描", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "scanId", description = "扫描组号(0-255)", required = true)
	@GetMapping("/scan/start/{deviceId}/{channelId}")
	public void startScan(@PathVariable String deviceId, @PathVariable String channelId, Integer scanId) {
		if (scanId == null || scanId < 0 || scanId > 255 ) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "扫描组号必须为0-255之间的数字");
		}
		frontEndCommand(deviceId, channelId, 0x89, scanId, 0, 0);
	}

	@Operation(summary = "扫描指令-停止自动扫描", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "scanId", description = "扫描组号(0-255)", required = true)
	@GetMapping("/scan/stop/{deviceId}/{channelId}")
	public void stopScan(@PathVariable String deviceId, @PathVariable String channelId, Integer scanId) {
		if (scanId == null || scanId < 0 || scanId > 255 ) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "扫描组号必须为0-255之间的数字");
		}
		frontEndCommand(deviceId, channelId, 0, 0, 0, 0);
	}

	@Operation(summary = "扫描指令-设置自动扫描左边界", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "scanId", description = "扫描组号(0-255)", required = true)
	@GetMapping("/scan/set/left/{deviceId}/{channelId}")
	public void setScanLeft(@PathVariable String deviceId, @PathVariable String channelId, Integer scanId) {
		if (scanId == null || scanId < 0 || scanId > 255 ) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "扫描组号必须为0-255之间的数字");
		}
		frontEndCommand(deviceId, channelId, 0x89, scanId, 1, 0);
	}

	@Operation(summary = "扫描指令-设置自动扫描右边界", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "scanId", description = "扫描组号(0-255)", required = true)
	@GetMapping("/scan/set/right/{deviceId}/{channelId}")
	public void setScanRight(@PathVariable String deviceId, @PathVariable String channelId, Integer scanId) {
		if (scanId == null || scanId < 0 || scanId > 255 ) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "扫描组号必须为0-255之间的数字");
		}
		frontEndCommand(deviceId, channelId, 0x89, scanId, 2, 0);
	}


	@Operation(summary = "扫描指令-设置自动扫描速度", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "scanId", description = "扫描组号(0-255)", required = true)
	@Parameter(name = "speed", description = "自动扫描速度(1-4095)", required = true)
	@GetMapping("/scan/set/speed/{deviceId}/{channelId}")
	public void setScanSpeed(@PathVariable String deviceId, @PathVariable String channelId, Integer scanId, Integer speed) {
		if (scanId == null || scanId < 0 || scanId > 255 ) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "扫描组号必须为0-255之间的数字");
		}
		if (speed == null || speed < 1 || speed > 4095) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "自动扫描速度必须为1-4095之间的数字");
		}
		int parameter2 = speed & 0xFF;
		int combindCode2 =  speed >> 8;
		frontEndCommand(deviceId, channelId, 0x8A, scanId, parameter2, combindCode2);
	}


	@Operation(summary = "辅助开关控制指令-雨刷控制", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "command", description = "控制指令,允许值: on, off", required = true)
	@GetMapping("/wiper/{deviceId}/{channelId}")
	public void wiper(@PathVariable String deviceId,@PathVariable String channelId, String command){

		if (log.isDebugEnabled()) {
			log.debug("辅助开关控制指令-雨刷控制 API调用，deviceId：{} ，channelId：{} ，command：{}",deviceId, channelId, command);
		}

		int cmdCode = 0;
		switch (command){
			case "on":
				cmdCode = 0x8c;
				break;
			case "off":
				cmdCode = 0x8d;
				break;
			default:
				break;
		}
		frontEndCommand(deviceId, channelId, cmdCode, 1, 0, 0);
	}

	@Operation(summary = "辅助开关控制指令", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "command", description = "控制指令,允许值: on, off", required = true)
	@Parameter(name = "switchId", description = "开关编号", required = true)
	@GetMapping("/auxiliary/{deviceId}/{channelId}")
	public void auxiliarySwitch(@PathVariable String deviceId,@PathVariable String channelId, String command, Integer switchId){

		if (log.isDebugEnabled()) {
			log.debug("辅助开关控制指令-雨刷控制 API调用，deviceId：{} ，channelId：{} ，command：{}, switchId: {}",deviceId, channelId, command, switchId);
		}

		int cmdCode = 0;
		switch (command){
			case "on":
				cmdCode = 0x8c;
				break;
			case "off":
				cmdCode = 0x8d;
				break;
			default:
				break;
		}
		frontEndCommand(deviceId, channelId, cmdCode, switchId, 0, 0);
	}
}
