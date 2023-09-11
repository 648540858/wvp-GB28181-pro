package com.genersoft.iot.vmp.vmanager.gb28181.ptz;


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

@Tag(name  = "云台控制")

@RestController
@RequestMapping("/api/ptz")
public class PtzController {

	private final static Logger logger = LoggerFactory.getLogger(PtzController.class);

	@Autowired
	private SIPCommander cmder;

	@Autowired
	private IVideoManagerStorage storager;

	@Autowired
	private DeferredResultHolder resultHolder;

	/***
	 * 云台控制
	 * @param deviceId 设备id
	 * @param channelId 通道id
	 * @param command	控制指令
	 * @param horizonSpeed	水平移动速度
	 * @param verticalSpeed	垂直移动速度
	 * @param zoomSpeed	    缩放速度
	 */

	@Operation(summary = "云台控制")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "command", description = "控制指令,允许值: left, right, up, down, upleft, upright, downleft, downright, zoomin, zoomout, stop", required = true)
	@Parameter(name = "horizonSpeed", description = "水平速度", required = true)
	@Parameter(name = "verticalSpeed", description = "垂直速度", required = true)
	@Parameter(name = "zoomSpeed", description = "缩放速度", required = true)
	@PostMapping("/control/{deviceId}/{channelId}")
	public void ptz(@PathVariable String deviceId,@PathVariable String channelId, String command, int horizonSpeed, int verticalSpeed, int zoomSpeed){

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("设备云台控制 API调用，deviceId：%s ，channelId：%s ，command：%s ，horizonSpeed：%d ，verticalSpeed：%d ，zoomSpeed：%d",deviceId, channelId, command, horizonSpeed, verticalSpeed, zoomSpeed));
		}
		Device device = storager.queryVideoDevice(deviceId);
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
		try {
			cmder.frontEndCmd(device, channelId, cmdCode, horizonSpeed, verticalSpeed, zoomSpeed);
		} catch (SipException | InvalidArgumentException | ParseException e) {
			logger.error("[命令发送失败] 云台控制: {}", e.getMessage());
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
		}
	}


	@Operation(summary = "通用前端控制命令")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "cmdCode", description = "指令码", required = true)
	@Parameter(name = "parameter1", description = "数据一", required = true)
	@Parameter(name = "parameter2", description = "数据二", required = true)
	@Parameter(name = "combindCode2", description = "组合码二", required = true)
	@PostMapping("/front_end_command/{deviceId}/{channelId}")
	public void frontEndCommand(@PathVariable String deviceId,@PathVariable String channelId,int cmdCode, int parameter1, int parameter2, int combindCode2){

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("设备云台控制 API调用，deviceId：%s ，channelId：%s ，cmdCode：%d parameter1：%d parameter2：%d",deviceId, channelId, cmdCode, parameter1, parameter2));
		}
		Device device = storager.queryVideoDevice(deviceId);

		try {
			cmder.frontEndCmd(device, channelId, cmdCode, parameter1, parameter2, combindCode2);
		} catch (SipException | InvalidArgumentException | ParseException e) {
			logger.error("[命令发送失败] 前端控制: {}", e.getMessage());
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
		}
	}


	@Operation(summary = "预置位查询")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@GetMapping("/preset/query/{deviceId}/{channelId}")
	public DeferredResult<String> presetQueryApi(@PathVariable String deviceId, @PathVariable String channelId) {
		if (logger.isDebugEnabled()) {
			logger.debug("设备预置位查询API调用");
		}
		Device device = storager.queryVideoDevice(deviceId);
		String uuid =  UUID.randomUUID().toString();
		String key =  DeferredResultHolder.CALLBACK_CMD_PRESETQUERY + (ObjectUtils.isEmpty(channelId) ? deviceId : channelId);
		DeferredResult<String> result = new DeferredResult<String> (3 * 1000L);
		result.onTimeout(()->{
			logger.warn(String.format("获取设备预置位超时"));
			// 释放rtpserver
			RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
			msg.setKey(key);
			msg.setData("获取设备预置位超时");
			resultHolder.invokeResult(msg);
		});
		if (resultHolder.exist(key, null)) {
			return result;
		}
		resultHolder.put(key, uuid, result);
		try {
			cmder.presetQuery(device, channelId, event -> {
				RequestMessage msg = new RequestMessage();
				msg.setId(uuid);
				msg.setKey(key);
				msg.setData(String.format("获取设备预置位失败，错误码： %s, %s", event.statusCode, event.msg));
				resultHolder.invokeResult(msg);
			});
		} catch (InvalidArgumentException | SipException | ParseException e) {
			logger.error("[命令发送失败] 获取设备预置位: {}", e.getMessage());
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
		}
		return result;
	}
}
