package com.genersoft.iot.vmp.vmanager.device;

import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.event.DeviceOffLineDetector;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;

import javax.sip.message.Response;

@SuppressWarnings("rawtypes")
@CrossOrigin
@RestController
@RequestMapping("/api")
public class DeviceQuery {
	
	private final static Logger logger = LoggerFactory.getLogger(DeviceQuery.class);
	
	@Autowired
	private IVideoManagerStorager storager;
	
	@Autowired
	private SIPCommander cmder;
	
	@Autowired
	private DeferredResultHolder resultHolder;
	
	@Autowired
	private DeviceOffLineDetector offLineDetector;
	
	@GetMapping("/devices/{deviceId}")
	public ResponseEntity<Device> devices(@PathVariable String deviceId){
		
		if (logger.isDebugEnabled()) {
			logger.debug("查询视频设备API调用，deviceId：" + deviceId);
		}
		
		Device device = storager.queryVideoDevice(deviceId);
		return new ResponseEntity<>(device,HttpStatus.OK);
	}
	
	@GetMapping("/devices")
	public PageInfo<Device> devices(int page, int count){
		
		if (logger.isDebugEnabled()) {
			logger.debug("查询所有视频设备API调用");
		}
		
		return storager.queryVideoDeviceList(page, count);
	}

	/**
	 * 分页查询通道数
	 *
	 * @param deviceId 设备id
	 * @param page 当前页
	 * @param count 每页条数
	 * @param query 查询内容
	 * @param online 是否在线  在线 true / 离线 false
	 * @param channelType 设备 false/子目录 true
	 * @return 通道列表
	 */
	@GetMapping("/devices/{deviceId}/channels")
	public ResponseEntity<PageInfo> channels(@PathVariable String deviceId,
											   int page, int count,
											   @RequestParam(required = false) String query,
											   @RequestParam(required = false) Boolean online,
											   @RequestParam(required = false) Boolean channelType) {
		if (logger.isDebugEnabled()) {
			logger.debug("查询视频设备通道API调用");
		}
		if (StringUtils.isEmpty(query)) {
			query = null;
		}

		PageInfo pageResult = storager.queryChannelsByDeviceId(deviceId, query, channelType, online, page, count);
		return new ResponseEntity<>(pageResult,HttpStatus.OK);
	}
	
	@PostMapping("/devices/{deviceId}/sync")
	public DeferredResult<ResponseEntity<Device>> devicesSync(@PathVariable String deviceId){
		
		if (logger.isDebugEnabled()) {
		}
			logger.debug("设备通道信息同步API调用，deviceId：" + deviceId);

		Device device = storager.queryVideoDevice(deviceId);
        cmder.catalogQuery(device, event -> {
			Response response = event.getResponse();
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_CATALOG+deviceId);
			msg.setData(String.format("同步通道失败，错误码： %s, %s", response.getStatusCode(), response.getReasonPhrase()));
			resultHolder.invokeResult(msg);
		});
        DeferredResult<ResponseEntity<Device>> result = new DeferredResult<ResponseEntity<Device>>(2*1000L);
		result.onTimeout(()->{
			logger.warn(String.format("设备通道信息同步超时"));
			// 释放rtpserver
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_CATALOG+deviceId);
			msg.setData("Timeout");
			resultHolder.invokeResult(msg);
		});
        resultHolder.put(DeferredResultHolder.CALLBACK_CMD_CATALOG+deviceId, result);
        return result;
	}
	
	@PostMapping("/devices/{deviceId}/delete")
	public ResponseEntity<String> delete(@PathVariable String deviceId){
		
		if (logger.isDebugEnabled()) {
			logger.debug("设备信息删除API调用，deviceId：" + deviceId);
		}
		
		if (offLineDetector.isOnline(deviceId)) {
			return new ResponseEntity<String>("不允许删除在线设备！", HttpStatus.NOT_ACCEPTABLE);
		}
		boolean isSuccess = storager.delete(deviceId);
		if (isSuccess) {
			JSONObject json = new JSONObject();
			json.put("deviceId", deviceId);
			return new ResponseEntity<>(json.toString(),HttpStatus.OK);
		} else {
			logger.warn("设备信息删除API调用失败！");
			return new ResponseEntity<String>("设备信息删除API调用失败！", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 分页查询通道数
	 * @param channelId 通道id
	 * @param page 当前页
	 * @param count 每页条数
	 * @return 子通道列表
	 */
	@GetMapping("/subChannels/{deviceId}/{channelId}/channels")
	public ResponseEntity<PageInfo> subChannels(@PathVariable String deviceId,
												  @PathVariable String channelId,
												  int page,
												  int count,
												  @RequestParam(required = false) String query,
												  @RequestParam(required = false) String online,
												  @RequestParam(required = false) Boolean channelType){

		if (logger.isDebugEnabled()) {
			logger.debug("查询所有视频通道API调用");
		}
		DeviceChannel deviceChannel = storager.queryChannel(deviceId,channelId);
		if (deviceChannel == null) {
			PageInfo<DeviceChannel> deviceChannelPageResult = new PageInfo<>();
			return new ResponseEntity<>(deviceChannelPageResult,HttpStatus.OK);
		}

		PageInfo pageResult = storager.querySubChannels(deviceId, channelId, query, channelType, online, page, count);
		return new ResponseEntity<>(pageResult,HttpStatus.OK);
	}

	@PostMapping("/channel/update/{deviceId}")
	public ResponseEntity<PageInfo> updateChannel(@PathVariable String deviceId,DeviceChannel channel){
		storager.updateChannel(deviceId, channel);
		return new ResponseEntity<>(null,HttpStatus.OK);
	}

	@GetMapping("/devices/{deviceId}/transport/{streamMode}")
	@PostMapping("/devices/{deviceId}/transport/{streamMode}")
	public ResponseEntity<PageInfo> updateTransport(@PathVariable String deviceId, @PathVariable String streamMode){
		Device device = storager.queryVideoDevice(deviceId);
		device.setStreamMode(streamMode);
		storager.updateDevice(device);
		return new ResponseEntity<>(null,HttpStatus.OK);
	}

	/**
	 * 设备状态查询请求API接口
	 * 
	 * @param deviceId
	 */
	@GetMapping("/devices/{deviceId}/status")
	public DeferredResult<ResponseEntity<String>> deviceStatusApi(@PathVariable String deviceId) {
		if (logger.isDebugEnabled()) {
			logger.debug("设备状态查询API调用");
		}
		Device device = storager.queryVideoDevice(deviceId);
		cmder.deviceStatusQuery(device, event -> {
			Response response = event.getResponse();
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_DEVICESTATUS + deviceId);
			msg.setData(String.format("获取设备状态失败，错误码： %s, %s", response.getStatusCode(), response.getReasonPhrase()));
			resultHolder.invokeResult(msg);
		});
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String>>(2*1000L);
		result.onTimeout(()->{
			logger.warn(String.format("获取设备状态超时"));
			// 释放rtpserver
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_DEVICESTATUS + deviceId);
			msg.setData("Timeout. Device did not response to this command.");
			resultHolder.invokeResult(msg);
		});
		resultHolder.put(DeferredResultHolder.CALLBACK_CMD_DEVICESTATUS + deviceId, result);
		return result;
	}

	/**
	 * 设备报警查询请求API接口
	 * 
	 * @param deviceId
	 */
	@GetMapping("/alarm/{deviceId}")
	public DeferredResult<ResponseEntity<String>> alarmApi(@PathVariable String deviceId,
														@RequestParam(required = false) String startPriority, 
														@RequestParam(required = false) String endPriority, 
														@RequestParam(required = false) String alarmMethod,
														@RequestParam(required = false) String alarmType,
														@RequestParam(required = false) String startTime,
														@RequestParam(required = false) String endTime) {
		if (logger.isDebugEnabled()) {
			logger.debug("设备报警查询API调用");
		}
		Device device = storager.queryVideoDevice(deviceId);
		cmder.alarmInfoQuery(device, startPriority, endPriority, alarmMethod, alarmType, startTime, endTime, event -> {
			Response response = event.getResponse();
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_ALARM + deviceId);
			msg.setData(String.format("设备报警查询失败，错误码： %s, %s", response.getStatusCode(), response.getReasonPhrase()));
			resultHolder.invokeResult(msg);
		});
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String >> (3 * 1000L);
		result.onTimeout(()->{
			logger.warn(String.format("设备报警查询超时"));
			// 释放rtpserver
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_ALARM + deviceId);
			msg.setData("设备报警查询超时");
			resultHolder.invokeResult(msg);
		});
		resultHolder.put(DeferredResultHolder.CALLBACK_CMD_ALARM + deviceId, result);
		return result;
	}


}
