/**
 * 设备控制命令API接口
 * 
 * @author lawrencehj
 * @date 2021年2月1日
 */

package com.genersoft.iot.vmp.gb28181.controller;

import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

@Tag(name  = "国标设备控制")
@Slf4j
@RestController
@RequestMapping("/api/device/control")
public class DeviceControl {

    @Autowired
    private IDeviceService deviceService;


	@Operation(summary = "远程启动", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @GetMapping("/teleboot/{deviceId}")
    public void teleBootApi(@PathVariable String deviceId) {
        if (log.isDebugEnabled()) {
            log.debug("设备远程启动API调用");
        }
        Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "设备不存在");
		deviceService.teleboot(device);
    }


	@Operation(summary = "录像控制", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "recordCmdStr", description = "命令， 可选值：Record（手动录像），StopRecord（停止手动录像）", required = true)
    @GetMapping("/record")
    public DeferredResult<WVPResult<String>> recordApi(String deviceId, String recordCmdStr, String channelId) {
        if (log.isDebugEnabled()) {
            log.debug("开始/停止录像API调用");
        }
        Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "设备不存在");
		DeferredResult<WVPResult<String>> deferredResult = new DeferredResult<>();

		deviceService.record(device, channelId, recordCmdStr, (code, msg, data) -> {
			deferredResult.setResult(new WVPResult<>(code, msg, data));
		});
		deferredResult.onTimeout(() -> {
			log.warn("[开始/停止录像] 操作超时, 设备未返回应答指令, {}", deviceId);
			deferredResult.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "操作超时, 设备未应答"));
		});
		return deferredResult;
	}

	@Operation(summary = "布防/撤防", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "guardCmdStr", description = "命令， 可选值：SetGuard（布防），ResetGuard（撤防）", required = true)
	@GetMapping("/guard")
	public DeferredResult<WVPResult<String>> guardApi(String deviceId, String guardCmdStr) {
		if (log.isDebugEnabled()) {
			log.debug("布防/撤防API调用");
		}
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "设备不存在");
		DeferredResult<WVPResult<String>> result = new DeferredResult<>();
		deviceService.guard(device, guardCmdStr, (code, msg, data) -> {
			result.setResult(new WVPResult<>(code, msg, data));
		});
		result.onTimeout(() -> {
			log.warn("[布防/撤防] 操作超时, 设备未返回应答指令, {}", deviceId);
			result.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "操作超时, 设备未应答"));
		});
		return result;
	}

	@Operation(summary = "报警复位", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "alarmMethod", description = "报警方式, 报警方式条件(可选),取值0为全部,1为电话报警,2为设备报警,3为短信报警,4为\n" +
			"GPS报警,5为视频报警,6为设备故障报警,7其他报警;可以为直接组合如12为电话报警或设备报警")
	@Parameter(name = "alarmType", description = "报警类型, " +
			"报警类型。" +
			"报警方式为2时,不携带 AlarmType为默认的报警设备报警," +
			"携带 AlarmType取值及对应报警类型如下:" +
			"1-视频丢失报警;2-设备防拆报警;3-存储设备磁盘满报警;4-设备高温报警;5-设备低温报警。" +
			"报警方式为5时,取值如下:" +
			"1-人工视频报警;2-运动目标检测报警;3-遗留物检测报警;4-物体移除检测报警;5-绊线检测报警;" +
			"6-入侵检测报警;7-逆行检测报警;8-徘徊检测报警;9-流量统计报警;10-密度检测报警;" +
			"11-视频异常检测报警;12-快速移动报警。" +
			"报警方式为6时,取值如下:" +
			"1-存储设备磁盘故障报警;2-存储设备风扇故障报警")
	@GetMapping("/reset_alarm")
	public DeferredResult<WVPResult<String>> resetAlarm(String deviceId, String channelId,
																@RequestParam(required = false) String alarmMethod,
																@RequestParam(required = false) String alarmType) {
		if (log.isDebugEnabled()) {
			log.debug("报警复位API调用");
		}
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "设备不存在");
		DeferredResult<WVPResult<String>> result = new DeferredResult<>();
		deviceService.resetAlarm(device, channelId, alarmMethod, alarmType, (code, msg, data) -> {
			result.setResult(new WVPResult<>(code, msg, data));
		});
		result.onTimeout(() -> {
			log.warn("[布防/撤防] 操作超时, 设备未返回应答指令, {}", deviceId);
			result.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "操作超时, 设备未应答"));
		});
		return result;
	}

	@Operation(summary = "强制关键帧", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号")
	@GetMapping("/i_frame")
	public void iFrame(String deviceId, @RequestParam(required = false) String channelId) {
		if (log.isDebugEnabled()) {
			log.debug("强制关键帧API调用");
		}
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "设备不存在");
		deviceService.iFrame(device, channelId);
	}

	@Operation(summary = "看守位控制", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "enabled", description = "是否开启看守位", required = true)
	@Parameter(name = "presetIndex", description = "调用预置位编号")
	@Parameter(name = "resetTime", description = "自动归位时间间隔 单位：秒")
	@GetMapping("/home_position")
	public DeferredResult<WVPResult<String>> homePositionApi(String deviceId, String channelId, Boolean enabled,
												  @RequestParam(required = false) Integer resetTime,
												  @RequestParam(required = false) Integer presetIndex) {
        if (log.isDebugEnabled()) {
			log.debug("看守位控制API调用");
		}
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "设备不存在");
		DeferredResult<WVPResult<String>> result = new DeferredResult<>();
		deviceService.homePosition(device, channelId, enabled, resetTime, presetIndex, (code, msg, data) -> {
			result.setResult(new WVPResult<>(code, msg, data));
		});
		result.onTimeout(() -> {
			log.warn("[看守位控制] 操作超时, 设备未返回应答指令, {}", deviceId);
			result.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "操作超时, 设备未应答"));
		});
		return result;
	}

	@Operation(summary = "拉框放大", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "length", description = "播放窗口长度像素值", required = true)
	@Parameter(name = "width", description = "播放窗口宽度像素值", required = true)
	@Parameter(name = "midpointx", description = "拉框中心的横轴坐标像素值", required = true)
	@Parameter(name = "midpointy", description = "拉框中心的纵轴坐标像素值", required = true)
	@Parameter(name = "lengthx", description = "拉框长度像素值", required = true)
	@Parameter(name = "lengthy", description = "拉框宽度像素值", required = true)
	@GetMapping("drag_zoom/zoom_in")
	public DeferredResult<WVPResult<String>> dragZoomIn(@RequestParam String deviceId, String channelId,
											 @RequestParam int length,
											 @RequestParam int width,
											 @RequestParam int midpointx,
											 @RequestParam int midpointy,
											 @RequestParam int lengthx,
											 @RequestParam int lengthy) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("设备拉框放大 API调用，deviceId：%s ，channelId：%s ，length：%d ，width：%d ，midpointx：%d ，midpointy：%d ，lengthx：%d ，lengthy：%d",deviceId, channelId, length, width, midpointx, midpointy,lengthx, lengthy));
		}
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "设备不存在");
		DeferredResult<WVPResult<String>> result = new DeferredResult<>();
		deviceService.dragZoomIn(device, channelId, length, width, midpointx, midpointy, lengthx,lengthy, (code, msg, data) -> {
			result.setResult(new WVPResult<>(code, msg, data));
		});
		result.onTimeout(() -> {
			log.warn("[设备拉框放大] 操作超时, 设备未返回应答指令, {}", deviceId);
			result.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "操作超时, 设备未应答"));
		});
		return result;
	}

	@Operation(summary = "拉框缩小", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号")
	@Parameter(name = "length", description = "播放窗口长度像素值", required = true)
	@Parameter(name = "width", description = "拉框中心的横轴坐标像素值", required = true)
	@Parameter(name = "midpointx", description = "拉框中心的横轴坐标像素值", required = true)
	@Parameter(name = "midpointy", description = "拉框中心的纵轴坐标像素值", required = true)
	@Parameter(name = "lengthx", description = "拉框长度像素值", required = true)
	@Parameter(name = "lengthy", description = "拉框宽度像素值", required = true)
	@GetMapping("/drag_zoom/zoom_out")
	public DeferredResult<WVPResult<String>> dragZoomOut(@RequestParam String deviceId,
											  @RequestParam(required = false) String channelId,
											  @RequestParam int length,
											  @RequestParam int width,
											  @RequestParam int midpointx,
											  @RequestParam int midpointy,
											  @RequestParam int lengthx,
											  @RequestParam int lengthy){

		if (log.isDebugEnabled()) {
			log.debug(String.format("设备拉框缩小 API调用，deviceId：%s ，channelId：%s ，length：%d ，width：%d ，midpointx：%d ，midpointy：%d ，lengthx：%d ，lengthy：%d",deviceId, channelId, length, width, midpointx, midpointy,lengthx, lengthy));
		}
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "设备不存在");
		DeferredResult<WVPResult<String>> result = new DeferredResult<>();
		deviceService.dragZoomOut(device, channelId, length, width, midpointx, midpointy, lengthx,lengthy, (code, msg, data) -> {
			result.setResult(new WVPResult<>(code, msg, data));
		});
		result.onTimeout(() -> {
			log.warn("[设备拉框放大] 操作超时, 设备未返回应答指令, {}", deviceId);
			result.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "操作超时, 设备未应答"));
		});
		return result;
	}
}
