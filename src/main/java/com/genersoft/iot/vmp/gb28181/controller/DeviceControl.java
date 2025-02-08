/**
 * 设备控制命令API接口
 * 
 * @author lawrencehj
 * @date 2021年2月1日
 */

package com.genersoft.iot.vmp.gb28181.controller;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.UUID;

@Tag(name  = "国标设备控制")
@Slf4j
@RestController
@RequestMapping("/api/device/control")
public class DeviceControl {

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private ISIPCommander cmder;

    @Autowired
    private DeferredResultHolder resultHolder;

    /**
     * 远程启动控制命令API接口
     * 
     * @param deviceId 设备ID
     */
	@Operation(summary = "远程启动控制命令", security = @SecurityRequirement(name = JwtUtils.HEADER))
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

    /**
     * 录像控制命令API接口
     * 
     * @param deviceId 设备ID
     * @param recordCmdStr  Record：手动录像，StopRecord：停止手动录像
     * @param channelId     通道编码（可选）
     */
	@Operation(summary = "录像控制", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "recordCmdStr", description = "命令， 可选值：Record（手动录像），StopRecord（停止手动录像）", required = true)
    @GetMapping("/record/{deviceId}/{recordCmdStr}")
    public DeferredResult<WVPResult<String>> recordApi(@PathVariable String deviceId,
															   @PathVariable String recordCmdStr, String channelId) {
        if (log.isDebugEnabled()) {
            log.debug("开始/停止录像API调用");
        }
        Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "设备不存在");
		DeferredResult<WVPResult<String>> result = deviceService.record(device, channelId, recordCmdStr);
		result.onTimeout(() -> {
			log.warn("[开始/停止录像] 操作超时, 设备未返回应答指令, {}", deviceId);
			result.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "操作超时, 设备未应答"));
		});
		return result;
	}

	/**
	 * 报警布防/撤防命令API接口
	 * 
	 * @param	deviceId 设备ID
	 * @param	guardCmdStr SetGuard：布防，ResetGuard：撤防
	 */
	@Operation(summary = "布防/撤防命令", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "guardCmdStr", description = "命令， 可选值：SetGuard（布防），ResetGuard（撤防）", required = true)
	@GetMapping("/guard/{deviceId}/{guardCmdStr}")
	public DeferredResult<WVPResult<String>> guardApi(@PathVariable String deviceId, @PathVariable String guardCmdStr) {
		if (log.isDebugEnabled()) {
			log.debug("布防/撤防API调用");
		}
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "设备不存在");
		DeferredResult<WVPResult<String>> result = deviceService.guard(device, guardCmdStr);
		result.onTimeout(() -> {
			log.warn("[布防/撤防] 操作超时, 设备未返回应答指令, {}", deviceId);
			result.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "操作超时, 设备未应答"));
		});
		return result;
	}

	/**
	 * 报警复位API接口
	 * 
	 * @param	deviceId 设备ID
	 * @param	alarmMethod 报警方式（可选）
	 * @param	alarmType   报警类型（可选）
	 */
	@Operation(summary = "报警复位", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "alarmMethod", description = "报警方式")
	@Parameter(name = "alarmType", description = "报警类型")
	@GetMapping("/reset_alarm/{deviceId}")
	public DeferredResult<WVPResult<String>> resetAlarmApi(@PathVariable String deviceId, String channelId,
																@RequestParam(required = false) String alarmMethod,
																@RequestParam(required = false) String alarmType) {
		if (log.isDebugEnabled()) {
			log.debug("报警复位API调用");
		}
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "设备不存在");
		DeferredResult<WVPResult<String>> result = deviceService.resetAlarm(device, channelId, alarmMethod, alarmType);
		result.onTimeout(() -> {
			log.warn("[布防/撤防] 操作超时, 设备未返回应答指令, {}", deviceId);
			result.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "操作超时, 设备未应答"));
		});
		return result;
	}

	/**
	 * 强制关键帧API接口
	 * 
	 * @param	deviceId 设备ID
	 * @param	channelId  通道ID
	 */
	@Operation(summary = "强制关键帧", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号")
	@GetMapping("/i_frame/{deviceId}")
	public void iFrame(@PathVariable String deviceId,
										@RequestParam(required = false) String channelId) {
		if (log.isDebugEnabled()) {
			log.debug("强制关键帧API调用");
		}
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "设备不存在");
		deviceService.iFrame(device, channelId);
	}

	/**
	 * 看守位控制命令API接口
	 * 
	 * @param deviceId 设备ID
	 * @param enabled       看守位使能1:开启,0:关闭
	 * @param resetTime     自动归位时间间隔（可选）
     * @param presetIndex   调用预置位编号（可选）
     * @param channelId     通道编码（可选）
	 */
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
		DeferredResult<WVPResult<String>> result = deviceService.homePosition(device, channelId, enabled, resetTime, presetIndex);
		result.onTimeout(() -> {
			log.warn("[看守位控制] 操作超时, 设备未返回应答指令, {}", deviceId);
			result.setResult(WVPResult.fail(ErrorCode.ERROR100.getCode(), "操作超时, 设备未应答"));
		});
		return result;
	}

	/**
	 * 拉框放大
	 * @param deviceId 设备id
	 * @param channelId 通道id
	 * @param length 播放窗口长度像素值
	 * @param width 播放窗口宽度像素值
	 * @param midpointx 拉框中心的横轴坐标像素值
	 * @param midpointy 拉框中心的纵轴坐标像素值
	 * @param lengthx 拉框长度像素值
	 * @param lengthy 拉框宽度像素值
	 * @return
	 */
	@Operation(summary = "拉框放大", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "length", description = "播放窗口长度像素值", required = true)
	@Parameter(name = "midpointx", description = "拉框中心的横轴坐标像素值", required = true)
	@Parameter(name = "midpointy", description = "拉框中心的纵轴坐标像素值", required = true)
	@Parameter(name = "lengthx", description = "拉框长度像素值", required = true)
	@Parameter(name = "lengthy", description = "lengthy", required = true)
	@GetMapping("drag_zoom/zoom_in")
	public void dragZoomIn(@RequestParam String deviceId,
											 @RequestParam(required = false) String channelId,
											 @RequestParam int length,
											 @RequestParam int width,
											 @RequestParam int midpointx,
											 @RequestParam int midpointy,
											 @RequestParam int lengthx,
											 @RequestParam int lengthy) throws RuntimeException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("设备拉框放大 API调用，deviceId：%s ，channelId：%s ，length：%d ，width：%d ，midpointx：%d ，midpointy：%d ，lengthx：%d ，lengthy：%d",deviceId, channelId, length, width, midpointx, midpointy,lengthx, lengthy));
		}
		Device device = deviceService.getDeviceByDeviceId(deviceId);
		Assert.notNull(device, "设备不存在");
		deviceService.dragZoomIn(device, channelId, length, width, midpointx, midpointy, lengthx,lengthy);
	}

	/**
	 * 拉框缩小
	 * @param deviceId 设备id
	 * @param channelId 通道id
	 * @param length 播放窗口长度像素值
	 * @param width 播放窗口宽度像素值
	 * @param midpointx 拉框中心的横轴坐标像素值
	 * @param midpointy 拉框中心的纵轴坐标像素值
	 * @param lengthx 拉框长度像素值
	 * @param lengthy 拉框宽度像素值
	 * @return
	 */
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
	public void dragZoomOut(@RequestParam String deviceId,
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
		deviceService.dragZoomOut(device, channelId, length, width, midpointx, midpointy, lengthx,lengthy);
	}
}
