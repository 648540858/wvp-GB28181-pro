/**
 * 设备控制命令API接口
 * 
 * @author lawrencehj
 * @date 2021年2月1日
 */

package com.genersoft.iot.vmp.vmanager.device;

import javax.sip.message.Response;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

@CrossOrigin
@RestController
@RequestMapping("/api")
public class DeviceControl {

    private final static Logger logger = LoggerFactory.getLogger(DeviceQuery.class);

    @Autowired
    private IVideoManagerStorager storager;

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private DeferredResultHolder resultHolder;

    /**
     * 远程启动控制命令API接口
     * 
     * @param deviceId
     */
    @GetMapping("/control/{deviceId}/teleboot")
    @PostMapping("/control/{deviceId}/teleboot")
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
     * @param deviceId
     * @param recordCmdStr  Record：手动录像，StopRecord：停止手动录像
     * @param channelId     通道编码（可选）
     */
    @GetMapping("/control/{deviceId}/record/{recordCmdStr}")
    public DeferredResult<ResponseEntity<String>> recordApi(@PathVariable String deviceId,
            @PathVariable String recordCmdStr, @RequestParam(required = false) String channelId) {
        if (logger.isDebugEnabled()) {
            logger.debug("开始/停止录像API调用");
        }
        Device device = storager.queryVideoDevice(deviceId);
        cmder.recordCmd(device, channelId, recordCmdStr, event -> {
            Response response = event.getResponse();
            RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_DEVICECONTROL + (XmlUtil.isEmpty(channelId) ? deviceId : channelId));
			msg.setData(String.format("开始/停止录像操作失败，错误码： %s, %s", response.getStatusCode(), response.getReasonPhrase()));
			resultHolder.invokeResult(msg);
		});
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String>>(3 * 1000L);
		result.onTimeout(() -> {
			logger.warn(String.format("开始/停止录像操作超时, 设备未返回应答指令"));
			// 释放rtpserver
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_DEVICECONTROL + (XmlUtil.isEmpty(channelId) ? deviceId : channelId));
			msg.setData("Timeout. Device did not response to this command.");
			resultHolder.invokeResult(msg);
		});
		resultHolder.put(DeferredResultHolder.CALLBACK_CMD_DEVICECONTROL + (XmlUtil.isEmpty(channelId) ? deviceId : channelId), result);
		return result;
	}

	/**
	 * 报警布防/撤防命令API接口
	 * 
	 * @param	deviceId
	 * @param	guardCmdStr SetGuard：布防，ResetGuard：撤防
	 */
	@GetMapping("/control/{deviceId}/guard/{guardCmdStr}")
	public DeferredResult<ResponseEntity<String>> guardApi(@PathVariable String deviceId, @PathVariable String guardCmdStr) {
		if (logger.isDebugEnabled()) {
			logger.debug("布防/撤防API调用");
		}
		Device device = storager.queryVideoDevice(deviceId);
		cmder.guardCmd(device, guardCmdStr, event -> {
			Response response = event.getResponse();
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_DEVICECONTROL + deviceId);
			msg.setData(String.format("布防/撤防操作失败，错误码： %s, %s", response.getStatusCode(), response.getReasonPhrase()));
			resultHolder.invokeResult(msg);
		});
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String>>(3 * 1000L);
		result.onTimeout(() -> {
			logger.warn(String.format("布防/撤防操作超时, 设备未返回应答指令"));
			// 释放rtpserver
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_DEVICECONTROL + deviceId);
			msg.setData("Timeout. Device did not response to this command.");
			resultHolder.invokeResult(msg);
		});
		resultHolder.put(DeferredResultHolder.CALLBACK_CMD_DEVICECONTROL + deviceId, result);
		return result;
	}

	/**
	 * 报警复位API接口
	 * 
	 * @param	deviceId
	 * @param	alarmMethod 报警方式（可选）
	 * @param	alarmType   报警类型（可选）
	 */
	@GetMapping("/control/{deviceId}/resetAlarm")
	public DeferredResult<ResponseEntity<String>> resetAlarmApi(@PathVariable String deviceId, 
																@RequestParam(required = false) String alarmMethod,
																@RequestParam(required = false) String alarmType) {
		if (logger.isDebugEnabled()) {
			logger.debug("报警复位API调用");
		}
		Device device = storager.queryVideoDevice(deviceId);
		cmder.alarmCmd(device, alarmMethod, alarmType, event -> {
			Response response = event.getResponse();
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_DEVICECONTROL + deviceId);
			msg.setData(String.format("报警复位操作失败，错误码： %s, %s", response.getStatusCode(), response.getReasonPhrase()));
			resultHolder.invokeResult(msg);
		});
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String>>(3 * 1000L);
		result.onTimeout(() -> {
			logger.warn(String.format("报警复位操作超时, 设备未返回应答指令"));
			// 释放rtpserver
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_DEVICECONTROL + deviceId);
			msg.setData("Timeout. Device did not response to this command.");
			resultHolder.invokeResult(msg);
		});
		resultHolder.put(DeferredResultHolder.CALLBACK_CMD_DEVICECONTROL + deviceId, result);
		return result;
	}

	/**
	 * 强制关键帧API接口
	 * 
	 * @param	deviceId
	 * @param	channelId 
	 */
	@GetMapping("/control/{deviceId}/iFrame")
	@PostMapping("/control/{deviceId}/iFrame")
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
	 * @param deviceId
	 * @param enabled       看守位使能1:开启,0:关闭
	 * @param resetTime     自动归位时间间隔（可选）
     * @param presetIndex   调用预置位编号（可选）
     * @param channelId     通道编码（可选）
	 */
	@GetMapping("/control/{deviceId}/homePosition/{enabled}")
	public DeferredResult<ResponseEntity<String>> homePositionApi(@PathVariable String deviceId,
																@PathVariable String enabled,
																@RequestParam(required = false) String resetTime,
																@RequestParam(required = false) String presetIndex,
                                                                @RequestParam(required = false) String channelId) {
        if (logger.isDebugEnabled()) {
			logger.debug("报警复位API调用");
		}
		Device device = storager.queryVideoDevice(deviceId);
		cmder.homePositionCmd(device, channelId, enabled, resetTime, presetIndex, event -> {
			Response response = event.getResponse();
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_DEVICECONTROL + (XmlUtil.isEmpty(channelId) ? deviceId : channelId));
			msg.setData(String.format("看守位控制操作失败，错误码： %s, %s", response.getStatusCode(), response.getReasonPhrase()));
			resultHolder.invokeResult(msg);
		});
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String>>(3 * 1000L);
		result.onTimeout(() -> {
			logger.warn(String.format("看守位控制操作超时, 设备未返回应答指令"));
			// 释放rtpserver
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_DEVICECONTROL + (XmlUtil.isEmpty(channelId) ? deviceId : channelId));
			JSONObject json = new JSONObject();
			json.put("DeviceID", deviceId);
			json.put("Status", "Timeout");
			json.put("Description", "看守位控制操作超时, 设备未返回应答指令");
			msg.setData(json); //("看守位控制操作超时, 设备未返回应答指令");
			resultHolder.invokeResult(msg);
		});
		resultHolder.put(DeferredResultHolder.CALLBACK_CMD_DEVICECONTROL + (XmlUtil.isEmpty(channelId) ? deviceId : channelId), result);
		return result;
	}
}
