/**
 * 设备控制命令API接口
 * 
 * @author lawrencehj
 * @date 2021年2月1日
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.UUID;

@Api(tags = "国标设备控制")
@CrossOrigin
@RestController
@RequestMapping("/api/device/control")
public class DeviceControl {

    private final static Logger logger = LoggerFactory.getLogger(DeviceQuery.class);

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private DeferredResultHolder resultHolder;

    /**
     * 远程启动控制命令API接口
     * 
     * @param deviceId 设备ID
     */
	@ApiOperation("远程启动控制命令")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value ="设备ID", required = true, dataTypeClass = String.class),
	})
    @GetMapping("/teleboot/{deviceId}")
    public ResponseEntity<String> teleBootApi(@PathVariable String deviceId) {
        if (logger.isDebugEnabled()) {
            logger.debug("设备远程启动API调用");
        }
        Device device = storager.queryVideoDevice(deviceId);
        boolean sucsess = cmder.teleBootCmd(device);
        if (sucsess) {
            JSONObject json = new JSONObject();
            json.put("DeviceID", deviceId);
            json.put("Result", "OK");
            return new ResponseEntity<>(json.toJSONString(), HttpStatus.OK);
        } else {
            logger.warn("设备远程启动API调用失败！");
            return new ResponseEntity<String>("设备远程启动API调用失败！", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 录像控制命令API接口
     * 
     * @param deviceId 设备ID
     * @param recordCmdStr  Record：手动录像，StopRecord：停止手动录像
     * @param channelId     通道编码（可选）
     */
    @ApiOperation("录像控制命令")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value ="设备ID", required = true, dataTypeClass = String.class),
			@ApiImplicitParam(name = "channelId", value ="通道编码" ,dataTypeClass = String.class),
			@ApiImplicitParam(name = "recordCmdStr", value ="命令， 可选值：Record（手动录像），StopRecord（停止手动录像）",
					required = true ,dataTypeClass = String.class),
	})
    @GetMapping("/record/{deviceId}/{recordCmdStr}")
    public DeferredResult<ResponseEntity<String>> recordApi(@PathVariable String deviceId,
            @PathVariable String recordCmdStr, String channelId) {
        if (logger.isDebugEnabled()) {
            logger.debug("开始/停止录像API调用");
        }
        Device device = storager.queryVideoDevice(deviceId);
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
		cmder.recordCmd(device, channelId, recordCmdStr, event -> {
            RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
			msg.setKey(key);
			msg.setData(String.format("开始/停止录像操作失败，错误码： %s, %s", event.statusCode, event.msg));
			resultHolder.invokeAllResult(msg);
		});

		return result;
	}

	/**
	 * 报警布防/撤防命令API接口
	 * 
	 * @param	deviceId 设备ID
	 * @param	guardCmdStr SetGuard：布防，ResetGuard：撤防
	 */
	@ApiOperation("布防/撤防命令")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value = "设备ID", required = true, dataTypeClass = String.class),
			@ApiImplicitParam(name = "channelId", value ="通道编码" ,dataTypeClass = String.class),
			@ApiImplicitParam(name = "guardCmdStr", value ="命令， 可选值：SetGuard（布防），ResetGuard（撤防）", required = true,
					dataTypeClass = String.class)
	})
	@GetMapping("/guard/{deviceId}/{guardCmdStr}")
	public DeferredResult<ResponseEntity<String>> guardApi(@PathVariable String deviceId, String channelId, @PathVariable String guardCmdStr) {
		if (logger.isDebugEnabled()) {
			logger.debug("布防/撤防API调用");
		}
		Device device = storager.queryVideoDevice(deviceId);
		String key = DeferredResultHolder.CALLBACK_CMD_DEVICECONTROL + deviceId + channelId;
		String uuid =UUID.randomUUID().toString();
		cmder.guardCmd(device, guardCmdStr, event -> {
			RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
			msg.setKey(key);
			msg.setData(String.format("布防/撤防操作失败，错误码： %s, %s", event.statusCode, event.msg));
			resultHolder.invokeResult(msg);
		});
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String>>(3 * 1000L);
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

	/**
	 * 报警复位API接口
	 * 
	 * @param	deviceId 设备ID
	 * @param	alarmMethod 报警方式（可选）
	 * @param	alarmType   报警类型（可选）
	 */
	@ApiOperation("报警复位")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value = "设备ID", required = true, dataTypeClass = String.class),
			@ApiImplicitParam(name = "channelId", value ="通道编码" ,dataTypeClass = String.class),
			@ApiImplicitParam(name = "alarmMethod", value ="报警方式", dataTypeClass = String.class),
			@ApiImplicitParam(name = "alarmType", value ="报警类型", dataTypeClass = String.class),
	})
	@GetMapping("/reset_alarm/{deviceId}")
	public DeferredResult<ResponseEntity<String>> resetAlarmApi(@PathVariable String deviceId, String channelId,
																@RequestParam(required = false) String alarmMethod,
																@RequestParam(required = false) String alarmType) {
		if (logger.isDebugEnabled()) {
			logger.debug("报警复位API调用");
		}
		Device device = storager.queryVideoDevice(deviceId);
		String uuid = UUID.randomUUID().toString();
		String key = DeferredResultHolder.CALLBACK_CMD_DEVICECONTROL + deviceId + channelId;
		cmder.alarmCmd(device, alarmMethod, alarmType, event -> {
			RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
			msg.setKey(key);
			msg.setData(String.format("报警复位操作失败，错误码： %s, %s", event.statusCode, event.msg));
			resultHolder.invokeResult(msg);
		});
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

	/**
	 * 强制关键帧API接口
	 * 
	 * @param	deviceId 设备ID
	 * @param	channelId  通道ID
	 */
	@ApiOperation("强制关键帧")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value = "设备ID", required = true, dataTypeClass = String.class),
			@ApiImplicitParam(name = "channelId", value ="通道ID", required = true, dataTypeClass = String.class),
	})
	@GetMapping("/i_frame/{deviceId}")
	public ResponseEntity<String> iFrame(@PathVariable String deviceId,
										@RequestParam(required = false) String channelId) {
		if (logger.isDebugEnabled()) {
			logger.debug("强制关键帧API调用");
		}
		Device device = storager.queryVideoDevice(deviceId);
		boolean sucsess = cmder.iFrameCmd(device, channelId);
		if (sucsess) {
			JSONObject json = new JSONObject();
			json.put("DeviceID", deviceId);
			json.put("ChannelID", channelId);
			json.put("Result", "OK");
			return new ResponseEntity<>(json.toJSONString(), HttpStatus.OK);
		} else {
			logger.warn("强制关键帧API调用失败！");
			return new ResponseEntity<String>("强制关键帧API调用失败！", HttpStatus.INTERNAL_SERVER_ERROR);
		}
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
	@ApiOperation("看守位控制")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value = "设备ID", required = true, dataTypeClass = String.class),
			@ApiImplicitParam(name = "channelId", value ="通道编码" ,dataTypeClass = String.class),
			@ApiImplicitParam(name = "enabled", value = "是否开启看守位 1:开启,0:关闭", required = true, dataTypeClass = String.class),
			@ApiImplicitParam(name = "resetTime", value = "自动归位时间间隔", dataTypeClass = String.class),
			@ApiImplicitParam(name = "presetIndex", value = "调用预置位编号", dataTypeClass = String.class),
			@ApiImplicitParam(name = "channelId", value ="通道ID", dataTypeClass = String.class),
	})
	@GetMapping("/home_position/{deviceId}/{enabled}")
	public DeferredResult<ResponseEntity<String>> homePositionApi(@PathVariable String deviceId,
																@PathVariable String enabled,
																@RequestParam(required = false) String resetTime,
																@RequestParam(required = false) String presetIndex,
                                                                String channelId) {
        if (logger.isDebugEnabled()) {
			logger.debug("报警复位API调用");
		}
		String key = DeferredResultHolder.CALLBACK_CMD_DEVICECONTROL + (StringUtils.isEmpty(channelId) ? deviceId : channelId);
		String uuid = UUID.randomUUID().toString();
		Device device = storager.queryVideoDevice(deviceId);
		cmder.homePositionCmd(device, channelId, enabled, resetTime, presetIndex, event -> {
			RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
			msg.setKey(key);
			msg.setData(String.format("看守位控制操作失败，错误码： %s, %s", event.statusCode, event.msg));
			resultHolder.invokeResult(msg);
		});
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String>>(3 * 1000L);
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
	@ApiOperation("拉框放大")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value = "设备ID", required = true, dataTypeClass = String.class),
			@ApiImplicitParam(name = "channelId", value = "通道ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "length", value = "播放窗口长度像素值", required = true, dataTypeClass = Integer.class),
			@ApiImplicitParam(name = "width", value = "播放窗口宽度像素值", required = true, dataTypeClass = Integer.class),
			@ApiImplicitParam(name = "midpointx", value = "拉框中心的横轴坐标像素值", required = true, dataTypeClass = Integer.class),
			@ApiImplicitParam(name = "midpointy", value = "拉框中心的纵轴坐标像素值", required = true, dataTypeClass = Integer.class),
			@ApiImplicitParam(name = "lengthx", value = "拉框长度像素值", required = true, dataTypeClass = Integer.class),
			@ApiImplicitParam(name = "lengthy", value = "拉框宽度像素值", required = true, dataTypeClass = Integer.class),
	})
	@GetMapping("drag_zoom/zoom_in")
	public ResponseEntity<String> dragZoomIn(@RequestParam String deviceId,
											 @RequestParam(required = false) String channelId,
											 @RequestParam int length,
											 @RequestParam int width,
											 @RequestParam int midpointx,
											 @RequestParam int midpointy,
											 @RequestParam int lengthx,
											 @RequestParam int lengthy){
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("设备拉框放大 API调用，deviceId：%s ，channelId：%s ，length：%d ，width：%d ，midpointx：%d ，midpointy：%d ，lengthx：%d ，lengthy：%d",deviceId, channelId, length, width, midpointx, midpointy,lengthx, lengthy));
		}
		Device device = storager.queryVideoDevice(deviceId);
		StringBuffer cmdXml = new StringBuffer(200);
		cmdXml.append("<DragZoomIn>\r\n");
		cmdXml.append("<Length>" + length+ "</Length>\r\n");
		cmdXml.append("<Width>" + width+ "</Width>\r\n");
		cmdXml.append("<MidPointX>" + midpointx+ "</MidPointX>\r\n");
		cmdXml.append("<MidPointY>" + midpointy+ "</MidPointY>\r\n");
		cmdXml.append("<LengthX>" + lengthx+ "</LengthX>\r\n");
		cmdXml.append("<LengthY>" + lengthy+ "</LengthY>\r\n");
		cmdXml.append("</DragZoomIn>\r\n");
		cmder.dragZoomCmd(device, channelId, cmdXml.toString());
		return new ResponseEntity<String>("success", HttpStatus.OK);
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
	@ApiOperation("拉框缩小")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value = "设备ID", required = true, dataTypeClass = String.class),
			@ApiImplicitParam(name = "channelId", value = "通道ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "length", value = "播放窗口长度像素值", required = true, dataTypeClass = Integer.class),
			@ApiImplicitParam(name = "width", value = "播放窗口宽度像素值", required = true, dataTypeClass = Integer.class),
			@ApiImplicitParam(name = "midpointx", value = "拉框中心的横轴坐标像素值", required = true, dataTypeClass = Integer.class),
			@ApiImplicitParam(name = "midpointy", value = "拉框中心的纵轴坐标像素值", required = true, dataTypeClass = Integer.class),
			@ApiImplicitParam(name = "lengthx", value = "拉框长度像素值", required = true, dataTypeClass = Integer.class),
			@ApiImplicitParam(name = "lengthy", value = "拉框宽度像素值", required = true, dataTypeClass = Integer.class),
	})
	@GetMapping("/drag_zoom/zoom_out")
	public ResponseEntity<String> dragZoomOut(@RequestParam String deviceId,
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
		StringBuffer cmdXml = new StringBuffer(200);
		cmdXml.append("<DragZoomOut>\r\n");
		cmdXml.append("<Length>" + length+ "</Length>\r\n");
		cmdXml.append("<Width>" + width+ "</Width>\r\n");
		cmdXml.append("<MidPointX>" + midpointx+ "</MidPointX>\r\n");
		cmdXml.append("<MidPointY>" + midpointy+ "</MidPointY>\r\n");
		cmdXml.append("<LengthX>" + lengthx+ "</LengthX>\r\n");
		cmdXml.append("<LengthY>" + lengthy+ "</LengthY>\r\n");
		cmdXml.append("</DragZoomOut>\r\n");
		cmder.dragZoomCmd(device, channelId, cmdXml.toString());
		return new ResponseEntity<String>("success",HttpStatus.OK);
	}
}
