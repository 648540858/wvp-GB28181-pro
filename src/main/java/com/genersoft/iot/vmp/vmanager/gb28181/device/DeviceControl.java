/**
 * 设备控制命令API接口
 * 
 * @author lawrencehj
 * @date 2021年2月1日
 */

package com.genersoft.iot.vmp.vmanager.gb28181.device;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.UUID;

@Tag(name  = "国标设备控制")

@RestController
@RequestMapping("/api/device/control")
public class DeviceControl {

    private final static Logger logger = LoggerFactory.getLogger(DeviceQuery.class);

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private ISIPCommander cmder;

    @Autowired
    private DeferredResultHolder resultHolder;

	@Operation(summary = "远程启动控制命令", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @GetMapping("/teleboot/{deviceId}")
    public void teleBootApi(@PathVariable String deviceId) {
        if (logger.isDebugEnabled()) {
            logger.debug("设备远程启动API调用");
        }
        Device device = storager.queryVideoDevice(deviceId);
		try {
			cmder.teleBootCmd(device);
		} catch (InvalidArgumentException | SipException | ParseException e) {
			logger.error("[命令发送失败] 远程启动: {}", e.getMessage());
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
		}
    }

	@Operation(summary = "录像控制", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "command", description = "控制命令，可选值：start（手动录像），stop（停止手动录像）", required = true)
		@GetMapping("/record/{deviceId}/{channelId}")
    public DeferredResult<ResponseEntity<String>> recordApi(@PathVariable String deviceId,
															@PathVariable String channelId,
															String command) {
        if (logger.isDebugEnabled()) {
            logger.debug("开始/停止录像API调用");
        }
        Device device = storager.queryVideoDevice(deviceId);
		if (device == null) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), deviceId + "不存在");
		}
		String uuid = UUID.randomUUID().toString();
		String key = DeferredResultHolder.CALLBACK_CMD_DEVICECONTROL +  deviceId + channelId;
		DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String>>(3 * 1000L);
		result.onTimeout(() -> {
			logger.warn(String.format("开始/停止录像操作超时, 设备未返回应答指令"));
			// 释放rtpserver
			RequestMessage msg = new RequestMessage();
			msg.setKey(key);
			msg.setId(uuid);
			msg.setData("Timeout. Device did not response to this command.");
			resultHolder.invokeAllResult(msg);
		});
		if (resultHolder.exist(key, null)){
			return result;
		}
		resultHolder.put(key, uuid, result);
		boolean isRecord;
		if (command.equalsIgnoreCase("start")) {
			isRecord = true;
		}else if (command.equalsIgnoreCase("stop")) {
			isRecord = false;
		}else {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "command参数不是规定值");
		}
		try {
			cmder.recordCmd(device, channelId, isRecord, event -> {
				RequestMessage msg = new RequestMessage();
				msg.setId(uuid);
				msg.setKey(key);
				msg.setData(String.format("开始/停止录像操作失败，错误码： %s, %s", event.statusCode, event.msg));
				resultHolder.invokeAllResult(msg);
			},null);
		} catch (InvalidArgumentException | SipException | ParseException e) {
			logger.error("[命令发送失败] 开始/停止录像: {}", e.getMessage());
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
		}

		return result;
	}

	@Operation(summary = "布防/撤防命令", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "command", description = "控制命令，可选值：set（布防），reset（撤防）", required = true)
	@GetMapping("/guard/{deviceId}")
	public DeferredResult<String> guardApi(@PathVariable String deviceId, String command) {
		if (logger.isDebugEnabled()) {
			logger.debug("布防/撤防API调用");
		}
		Device device = storager.queryVideoDevice(deviceId);
		if (device == null) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), deviceId + "不存在");
		}
		String key = DeferredResultHolder.CALLBACK_CMD_DEVICECONTROL + deviceId + deviceId;
		String uuid =UUID.randomUUID().toString();
		boolean setGuard;
		if (command.equalsIgnoreCase("set")) {
			setGuard = true;
		}else if (command.equalsIgnoreCase("reset")) {
			setGuard = false;
		}else {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "command参数不是规定值");
		}
		try {
			cmder.guardCmd(device, setGuard, event -> {
				RequestMessage msg = new RequestMessage();
				msg.setId(uuid);
				msg.setKey(key);
				msg.setData(String.format("布防/撤防操作失败，错误码： %s, %s", event.statusCode, event.msg));
				resultHolder.invokeResult(msg);
			},null);
		} catch (InvalidArgumentException | SipException | ParseException e) {
			logger.error("[命令发送失败] 布防/撤防操作: {}", e.getMessage());
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送: " + e.getMessage());
		}
		DeferredResult<String> result = new DeferredResult<>(3 * 1000L);
		resultHolder.put(key, uuid, result);
		result.onTimeout(() -> {
			logger.warn(String.format("布防/撤防操作超时, 设备未返回应答指令"));
			// 释放rtpserver
			RequestMessage msg = new RequestMessage();
			msg.setKey(key);
			msg.setId(uuid);
			msg.setData("Timeout. Device did not response to this command.");
			resultHolder.invokeResult(msg);
		});

		return result;
	}

	@Operation(summary = "报警复位", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "alarmMethod", description = "报警方式, 取值0为全部," +
			"1为电话报警,2为设备报警,3为短信报警,4为GPS报警,5为视频报警,6为设备故障报警," +
			"7其他报警;" +
			"可以为直接组合如12为电话报警或设备报警", required = false)
	@Parameter(name = "alarmType", description = "报警类型, " +
			"alarmMethod为2时，取值为：1-视频丢失报警;2-设备防拆报警;3-存储设备磁盘满报警;4-设备高温报警;5-设备低温报警。" +
			"alarmMethod为5时, 取值为:1-人工视频报警;2-运动目标检测报警;3-遗留物检测报警;4-物体移除检测报警;5-绊线检测报警;" +
			"						6-入侵检测报警;7-逆行检测报警;8-徘徊检测报警;9-流量统计报警;10-密度检测报警;11-视频异常检测报警;12-快速移动报警。" +
			"alarmMethod为6时, 取值为:1-存储设备磁盘故障报警;2-存储设备风扇故障报警  ", required = false)
	@GetMapping("/reset_alarm/{deviceId}")
	public DeferredResult<ResponseEntity<String>> resetAlarmApi(@PathVariable String deviceId,
																@RequestParam(required = false) Integer alarmMethod,
																	@RequestParam(required = false) Integer alarmType) {
		if (logger.isDebugEnabled()) {
			logger.debug("报警复位API调用");
		}
		Device device = storager.queryVideoDevice(deviceId);
		if (device == null) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), deviceId + "不存在");
		}
		String uuid = UUID.randomUUID().toString();
		String key = DeferredResultHolder.CALLBACK_CMD_DEVICECONTROL + deviceId ;
		try {
			cmder.alarmCmd(device, alarmMethod, alarmType, event -> {
				RequestMessage msg = new RequestMessage();
				msg.setId(uuid);
				msg.setKey(key);
				msg.setData(String.format("报警复位操作失败，错误码： %s, %s", event.statusCode, event.msg));
				resultHolder.invokeResult(msg);
			},null);
		} catch (InvalidArgumentException | SipException | ParseException e) {
			logger.error("[命令发送失败] 报警复位: {}", e.getMessage());
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
		}
		DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String>>(3 * 1000L);
		result.onTimeout(() -> {
			logger.warn(String.format("报警复位操作超时, 设备未返回应答指令"));
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

	@Operation(summary = "强制关键帧", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号")
	@GetMapping("/i_frame/{deviceId}/{channelId}")
	public JSONObject iFrame(@PathVariable String deviceId, @PathVariable String channelId) {
		if (logger.isDebugEnabled()) {
			logger.debug("强制关键帧API调用");
		}
		Device device = storager.queryVideoDevice(deviceId);
		if (device == null) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), deviceId + "不存在");
		}
		try {
			cmder.iFrameCmd(device, channelId);
		} catch (InvalidArgumentException | SipException | ParseException e) {
			logger.error("[命令发送失败] 强制关键帧: {}", e.getMessage());
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
		}
		JSONObject json = new JSONObject();
		json.put("DeviceID", deviceId);
		json.put("ChannelID", channelId);
		json.put("Result", "OK");
		return json;
	}

	@Operation(summary = "看守位控制", security = @SecurityRequirement(name = JwtUtils.HEADER))
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "command", description = "控制命令： start-开启看守位 stop-关闭看守位", required = true)
	@Parameter(name = "presetId", description = "调用预置位编号, 取值范围为-255")
	@Parameter(name = "resetTime", description = "自动归位时间间隔，单位:秒(s)")
	@GetMapping("/home_position/{deviceId}")
	public DeferredResult<String> homePositionApi(@PathVariable String deviceId,
												  String command,
												  @RequestParam(required = false) Integer resetTime,
												  @RequestParam(required = false) Integer presetId,
												  String channelId) {
        if (logger.isDebugEnabled()) {
			logger.debug("报警复位API调用");
		}
		String key = DeferredResultHolder.CALLBACK_CMD_DEVICECONTROL + (ObjectUtils.isEmpty(channelId) ? deviceId : channelId);
		String uuid = UUID.randomUUID().toString();
		Device device = storager.queryVideoDevice(deviceId);
		if (device == null) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), deviceId + "不存在");
		}
		boolean enabled;
		if (command.equalsIgnoreCase("start")) {
			enabled = true;
		}else if (command.equalsIgnoreCase("stop")) {
			enabled = false;
		}else {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "command参数不是规定值");
		}
		try {
			cmder.homePositionCmd(device, channelId, enabled, resetTime, presetId, event -> {
				RequestMessage msg = new RequestMessage();
				msg.setId(uuid);
				msg.setKey(key);
				msg.setData(String.format("看守位控制操作失败，错误码： %s, %s", event.statusCode, event.msg));
				resultHolder.invokeResult(msg);
			},null);
		} catch (InvalidArgumentException | SipException | ParseException e) {
			logger.error("[命令发送失败] 看守位控制: {}", e.getMessage());
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
		}
		DeferredResult<String> result = new DeferredResult<>(3 * 1000L);
		result.onTimeout(() -> {
			logger.warn(String.format("看守位控制操作超时, 设备未返回应答指令"));
			// 释放rtpserver
			RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
			msg.setKey(key);
			JSONObject json = new JSONObject();
			json.put("DeviceID", deviceId);
			json.put("Status", "Timeout");
			json.put("Description", "看守位控制操作超时, 设备未返回应答指令");
			msg.setData(json); //("看守位控制操作超时, 设备未返回应答指令");
			resultHolder.invokeResult(msg);
		});
		resultHolder.put(key, uuid, result);
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
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("设备拉框放大 API调用，deviceId：%s ，channelId：%s ，length：%d ，width：%d ，midpointx：%d ，midpointy：%d ，lengthx：%d ，lengthy：%d",deviceId, channelId, length, width, midpointx, midpointy,lengthx, lengthy));
		}
		Device device = storager.queryVideoDevice(deviceId);
		if (device == null) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), deviceId + "不存在");
		}
		StringBuffer cmdXml = new StringBuffer(200);
		cmdXml.append("<DragZoomIn>\r\n");
		cmdXml.append("<Length>" + length+ "</Length>\r\n");
		cmdXml.append("<Width>" + width+ "</Width>\r\n");
		cmdXml.append("<MidPointX>" + midpointx+ "</MidPointX>\r\n");
		cmdXml.append("<MidPointY>" + midpointy+ "</MidPointY>\r\n");
		cmdXml.append("<LengthX>" + lengthx+ "</LengthX>\r\n");
		cmdXml.append("<LengthY>" + lengthy+ "</LengthY>\r\n");
		cmdXml.append("</DragZoomIn>\r\n");
		try {
			cmder.dragZoomCmd(device, channelId, cmdXml.toString());
		} catch (InvalidArgumentException | SipException | ParseException e) {
			logger.error("[命令发送失败] 拉框放大: {}", e.getMessage());
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " +  e.getMessage());
		}
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

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("设备拉框缩小 API调用，deviceId：%s ，channelId：%s ，length：%d ，width：%d ，midpointx：%d ，midpointy：%d ，lengthx：%d ，lengthy：%d",deviceId, channelId, length, width, midpointx, midpointy,lengthx, lengthy));
		}
		Device device = storager.queryVideoDevice(deviceId);
		if (device == null) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), deviceId + "不存在");
		}
		StringBuffer cmdXml = new StringBuffer(200);
		cmdXml.append("<DragZoomOut>\r\n");
		cmdXml.append("<Length>" + length+ "</Length>\r\n");
		cmdXml.append("<Width>" + width+ "</Width>\r\n");
		cmdXml.append("<MidPointX>" + midpointx+ "</MidPointX>\r\n");
		cmdXml.append("<MidPointY>" + midpointy+ "</MidPointY>\r\n");
		cmdXml.append("<LengthX>" + lengthx+ "</LengthX>\r\n");
		cmdXml.append("<LengthY>" + lengthy+ "</LengthY>\r\n");
		cmdXml.append("</DragZoomOut>\r\n");
		try {
			cmder.dragZoomCmd(device, channelId, cmdXml.toString());
		} catch (InvalidArgumentException | SipException | ParseException e) {
			logger.error("[命令发送失败] 拉框缩小: {}", e.getMessage());
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " +  e.getMessage());
		}
	}
}
