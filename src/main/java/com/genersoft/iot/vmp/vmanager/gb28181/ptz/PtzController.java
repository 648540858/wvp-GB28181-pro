package com.genersoft.iot.vmp.vmanager.gb28181.ptz;


import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.PresetItem;
import com.genersoft.iot.vmp.gb28181.session.PresetDataCatch;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
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
import java.util.List;
import java.util.UUID;

@Tag(name  = "云台控制")

@RestController
@RequestMapping("/api/ptz")
public class PtzController {

	private final static Logger logger = LoggerFactory.getLogger(PtzController.class);

	@Autowired
	private ISIPCommander cmder;

	@Autowired
	private IVideoManagerStorage storager;

	@Autowired
	private DeferredResultHolder resultHolder;

	@Autowired
	private PresetDataCatch presetDataCatch;

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
	@RequestMapping(value = "/control/{deviceId}/{channelId}", method = {RequestMethod.GET, RequestMethod.POST})
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
	public DeferredResult<List<PresetItem>> presetQueryApi(@PathVariable String deviceId, @PathVariable String channelId) {
		if (logger.isDebugEnabled()) {
			logger.debug("设备预置位查询API调用");
		}
		Device device = storager.queryVideoDevice(deviceId);
		if (device == null) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), deviceId + "不存在");
		}
		int sn = SipUtils.getNewSn();
		String msgId = sn + "";
		String key =  DeferredResultHolder.CALLBACK_CMD_PRESETQUERY + sn;
		DeferredResult<List<PresetItem>> result = new DeferredResult<> (30 * 1000L);
		result.onTimeout(()->{
			logger.warn(String.format("获取设备预置位超时"));
			// 释放rtpserver
			RequestMessage msg = new RequestMessage();
			msg.setId(msgId);
			msg.setKey(key);
			msg.setData("获取设备预置位超时");
			resultHolder.invokeResult(msg);

		});
		if (resultHolder.exist(key, null)) {
			return result;
		}
		resultHolder.put(key, msgId, result);
		try {
			presetDataCatch.addReady(sn);
			cmder.presetQuery(device, channelId, sn, event -> {
				RequestMessage msg = new RequestMessage();
				msg.setId(msgId);
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
	@Operation(summary = "预置位控制")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "command", description = "控制指令 允许值: set, goto, delete", required = true)
	@Parameter(name = "presetId", description = "预置位编号", required = true)
	@GetMapping("/preset/control/{deviceId}/{channelId}")
	public void presetControlApi(@PathVariable String deviceId, @PathVariable String channelId,
											String command, int presetId) {
		if (logger.isDebugEnabled()) {
			logger.debug("设备预置位控制API调用");
		}
		Device device = storager.queryVideoDevice(deviceId);
		if (device == null) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), deviceId + "不存在");
		}
		int cmdCode = 0;
		switch (command){
			case "set":
				cmdCode = 129;
				break;
			case "goto":
				cmdCode = 130;
				break;
			case "delete":
				cmdCode = 131;
				break;
			default:
				break;
		}
		try {
			cmder.frontEndCmd(device, channelId, cmdCode, 0, presetId, 2);
		} catch (SipException | InvalidArgumentException | ParseException e) {
			logger.error("[命令发送失败] 云台控制: {}", e.getMessage());
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
		}
	}

	@Operation(summary = "巡航组控制")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "command", description = "控制指令 允许值: add, remove, setSpeed, setStay, start, stop", required = true)
	@Parameter(name = "cruiseId", description = "巡航组编号", required = true)
	@Parameter(name = "presetId", description = "预置位编号", required = true)
	@Parameter(name = "speed", description = "巡航速度（1~4095）", required = true)
	@Parameter(name = "stay", description = "巡航停留时间(1~4095)", required = true)
	@GetMapping("/cruise/control/{deviceId}/{channelId}")
	public void cruiseControlApi(@PathVariable String deviceId, @PathVariable String channelId,
								 String command, Integer cruiseId,
								 @Parameter(required = false) Integer presetId,
								 @Parameter(required = false)Integer speed,
								 @Parameter(required = false)Integer stay) {
		if (logger.isDebugEnabled()) {
			logger.debug("设备巡航组控制API调用");
		}
		Device device = storager.queryVideoDevice(deviceId);
		if (device == null) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), deviceId + "不存在");
		}
		int cmdCode = 0;
		int param1 = cruiseId;
		int param2 = presetId != null? presetId: 0;
		int param3 = 0;
		switch (command){
			case "add":
				cmdCode = 132;
				break;
			case "remove":
				cmdCode = 133;
				break;
			case "setSpeed":
				cmdCode = 134;
				param3= speed;
				break;
			case "setStay":
				cmdCode = 135;
				param3= stay;
				break;
			case "start":
				cmdCode = 136;
				break;
			case "stop":
				param1 = 0;
				param2 = 0;
				break;
			default:
				break;
		}
		try {
			cmder.frontEndCmd(device, channelId, cmdCode, param1, param2, param3);
		} catch (SipException | InvalidArgumentException | ParseException e) {
			logger.error("[命令发送失败] 巡航组控制: {}", e.getMessage());
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
		}
	}

	@Operation(summary = "扫描组控制")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "command", description = "控制指令 允许值: setLeft, setRight, start, stop", required = true)
	@Parameter(name = "scanId", description = "扫描组编号", required = true)
	@Parameter(name = "speed", description = "自动扫描速度(1~4095)", required = false)
	@GetMapping("/scan/control/{deviceId}/{channelId}")
	public void scanControlApi(@PathVariable String deviceId, @PathVariable String channelId,
								 String command, Integer scanId, @Parameter(required = false)Integer speed) {
		if (logger.isDebugEnabled()) {
			logger.debug("设备扫描组API调用");
		}
		Device device = storager.queryVideoDevice(deviceId);
		if (device == null) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), deviceId + "不存在");
		}
		int cmdCode = 0;
		int param1 = scanId;
		int param2 = 0;
		int param3 = 0;
		switch (command){
			case "setSpeed":
				cmdCode = 138;
				if (speed == null || speed == 0) {
					throw new ControllerException(ErrorCode.ERROR100.getCode(), "speed值不正确");
				}
				param3 = speed;
				break;
			case "setLeft":
				cmdCode = 137;
				break;
			case "setRight":
				cmdCode = 137;
				break;
			case "start":
				cmdCode = 137;
				break;
			case "stop":
				param1 = 0;
				param2 = 0;
				break;
			default:
				break;
		}
		try {
			cmder.frontEndCmd(device, channelId, cmdCode, param1, param2, param3);
		} catch (SipException | InvalidArgumentException | ParseException e) {
			logger.error("[命令发送失败] 扫描组控制: {}", e.getMessage());
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
		}
	}

	@Operation(summary = "雨刷开关")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "command", description = "控制指令 允许值: on off", required = true)
	@GetMapping("/wiper/control/{deviceId}/{channelId}")
	public void wiperControlApi(@PathVariable String deviceId, @PathVariable String channelId, String command) {
		if (logger.isDebugEnabled()) {
			logger.debug("设备雨刷开关控制API调用");
		}
		Device device = storager.queryVideoDevice(deviceId);
		if (device == null) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), deviceId + "不存在");
		}
		int cmdCode = 0;
		switch (command){
			case "on":
				cmdCode = 140;
				break;
			case "off":
				cmdCode = 141;
				break;
			default:
				break;
		}
		try {
			cmder.frontEndCmd(device, channelId, cmdCode, 1, 0, 0);
		} catch (SipException | InvalidArgumentException | ParseException e) {
			logger.error("[命令发送失败] 雨刷开关控制: {}", e.getMessage());
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
		}
	}

	@Operation(summary = "辅助控制开关")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "command", description = "控制指令 允许值: on off", required = true)
	@Parameter(name = "auxiliaryId", description = "开关ID（2-255）", required = true)
	@GetMapping("/auxiliary/control/{deviceId}/{channelId}")
	public void auxiliaryControlApi(@PathVariable String deviceId, @PathVariable String channelId, String command, int auxiliaryId) {
		if (logger.isDebugEnabled()) {
			logger.debug("设备雨刷开关控制API调用");
		}
		Device device = storager.queryVideoDevice(deviceId);
		if (device == null) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), deviceId + "不存在");
		}
		int cmdCode = 0;
		int param2 = 0;
		int param3 = 0;
		switch (command){
			case "on":
				cmdCode = 140;
				break;
			case "off":
				cmdCode = 141;
				break;
			default:
				break;
		}
		try {
			cmder.frontEndCmd(device, channelId, cmdCode, auxiliaryId, param2, param3);
		} catch (SipException | InvalidArgumentException | ParseException e) {
			logger.error("[命令发送失败] 雨刷开关控制: {}", e.getMessage());
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
		}
	}
}
