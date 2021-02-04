/**
 * 设备设置命令API接口
 * 
 * @author lawrencehj
 * @date 2021年2月2日
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
public class DeviceConfig {

    private final static Logger logger = LoggerFactory.getLogger(DeviceQuery.class);

    @Autowired
    private IVideoManagerStorager storager;

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private DeferredResultHolder resultHolder;

	/**
	 * 看守位控制命令API接口
	 * 
	 * @param deviceId
	 * @param enabled       看守位使能1:开启,0:关闭
	 * @param resetTime     自动归位时间间隔（可选）
     * @param presetIndex   调用预置位编号（可选）
     * @param channelId     通道编码（可选）
	 */
	@GetMapping("/config/{deviceId}/basicParam")
	public DeferredResult<ResponseEntity<String>> homePositionApi(@PathVariable String deviceId,
                                                                @RequestParam(required = false) String channelId,
                                                                @RequestParam(required = false) String name,
																@RequestParam(required = false) String expiration,
																@RequestParam(required = false) String heartBeatInterval,
                                                                @RequestParam(required = false) String heartBeatCount) {
        if (logger.isDebugEnabled()) {
			logger.debug("报警复位API调用");
		}
		Device device = storager.queryVideoDevice(deviceId);
		cmder.deviceBasicConfigCmd(device, channelId, name, expiration, heartBeatInterval, heartBeatCount, event -> {
			Response response = event.getResponse();
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_DEVICECONFIG + (XmlUtil.isEmpty(channelId) ? deviceId : channelId));
			msg.setData(String.format("设备配置操作失败，错误码： %s, %s", response.getStatusCode(), response.getReasonPhrase()));
			resultHolder.invokeResult(msg);
		});
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String>>(3 * 1000L);
		result.onTimeout(() -> {
			logger.warn(String.format("设备配置操作超时, 设备未返回应答指令"));
			// 释放rtpserver
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_DEVICECONFIG + (XmlUtil.isEmpty(channelId) ? deviceId : channelId));
			JSONObject json = new JSONObject();
			json.put("DeviceID", deviceId);
			json.put("Status", "Timeout");
			json.put("Description", "设备配置操作超时, 设备未返回应答指令");
			msg.setData(json); //("看守位控制操作超时, 设备未返回应答指令");
			resultHolder.invokeResult(msg);
		});
		resultHolder.put(DeferredResultHolder.CALLBACK_CMD_DEVICECONFIG + (XmlUtil.isEmpty(channelId) ? deviceId : channelId), result);
		return result;
	}

	/**
	 * 设备配置查询请求API接口
	 * 
	 * @param deviceId
	 */
	@GetMapping("/config/{deviceId}/query/{configType}")
    public DeferredResult<ResponseEntity<String>> configDownloadApi(@PathVariable String deviceId, 
                                                                @PathVariable String configType,
                                                                @RequestParam(required = false) String channelId) {
		if (logger.isDebugEnabled()) {
			logger.debug("设备状态查询API调用");
		}
		Device device = storager.queryVideoDevice(deviceId);
		cmder.deviceConfigQuery(device, channelId, configType, event -> {
			Response response = event.getResponse();
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_CONFIGDOWNLOAD + (XmlUtil.isEmpty(channelId) ? deviceId : channelId));
			msg.setData(String.format("获取设备配置失败，错误码： %s, %s", response.getStatusCode(), response.getReasonPhrase()));
			resultHolder.invokeResult(msg);
		});
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String >> (3 * 1000L);
		result.onTimeout(()->{
			logger.warn(String.format("获取设备配置超时"));
			// 释放rtpserver
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_CONFIGDOWNLOAD + (XmlUtil.isEmpty(channelId) ? deviceId : channelId));
			msg.setData("Timeout. Device did not response to this command.");
			resultHolder.invokeResult(msg);
		});
		resultHolder.put(DeferredResultHolder.CALLBACK_CMD_CONFIGDOWNLOAD + (XmlUtil.isEmpty(channelId) ? deviceId : channelId), result);
		return result;
	}

}
