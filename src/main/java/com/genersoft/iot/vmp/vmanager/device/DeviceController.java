package com.genersoft.iot.vmp.vmanager.device;

import java.util.List;

import com.genersoft.iot.vmp.common.PageResult;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.event.DeviceOffLineDetector;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;

@CrossOrigin
@RestController
@RequestMapping("/api")
public class DeviceController {
	
	private final static Logger logger = LoggerFactory.getLogger(DeviceController.class);
	
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
	public PageResult<Device> devices(int page, int count){
		
		if (logger.isDebugEnabled()) {
			logger.debug("查询所有视频设备API调用");
		}
		
		return storager.queryVideoDeviceList(null, page, count);
	}

	/**
	 * 分页查询通道数
	 * @param deviceId 设备id
	 * @param page 当前页
	 * @param count 每页条数
	 * @return 通道列表
	 */
	@GetMapping("/devices/{deviceId}/channels")
	public ResponseEntity<PageResult> channels(@PathVariable String deviceId,
											   int page, int count,
											   @RequestParam(required = false) String query,
											   @RequestParam(required = false) String online,
											   @RequestParam(required = false) Boolean channelType
	){

		if (logger.isDebugEnabled()) {
			logger.debug("查询所有视频设备API调用");
		}
		PageResult pageResult = storager.queryChannelsByDeviceId(deviceId, query, channelType, online, page, count);
		return new ResponseEntity<>(pageResult,HttpStatus.OK);
	}
	
	@PostMapping("/devices/{deviceId}/sync")
	public DeferredResult<ResponseEntity<Device>> devicesSync(@PathVariable String deviceId){
		
		if (logger.isDebugEnabled()) {
		}
			logger.debug("设备信息同步API调用，deviceId：" + deviceId);

		Device device = storager.queryVideoDevice(deviceId);
        cmder.catalogQuery(device);
        DeferredResult<ResponseEntity<Device>> result = new DeferredResult<ResponseEntity<Device>>();
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
			logger.warn("设备预览API调用失败！");
			return new ResponseEntity<String>("设备预览API调用失败！", HttpStatus.INTERNAL_SERVER_ERROR);
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
	public ResponseEntity<PageResult> subChannels(@PathVariable String deviceId,
												  @PathVariable String channelId,
												  int page,
												  int count,
												  @RequestParam(required = false) String query,
												  @RequestParam(required = false) String online,
												  @RequestParam(required = false) Boolean channelType){

		if (logger.isDebugEnabled()) {
			logger.debug("查询所有视频设备API调用");
		}
		DeviceChannel deviceChannel = storager.queryChannel(deviceId,channelId);
		if (deviceChannel == null) {
			PageResult<DeviceChannel> deviceChannelPageResult = new PageResult<>();
			return new ResponseEntity<>(deviceChannelPageResult,HttpStatus.OK);
		}

		PageResult pageResult = storager.querySubChannels(deviceId, channelId, query, channelType, online, page, count);
		return new ResponseEntity<>(pageResult,HttpStatus.OK);
	}

	@PostMapping("/channel/update/{deviceId}")
	public ResponseEntity<PageResult> updateChannel(@PathVariable String deviceId,DeviceChannel channel){
		storager.updateChannel(deviceId, channel);
		return new ResponseEntity<>(null,HttpStatus.OK);
	}

	@GetMapping("/devices/{deviceId}/transport/{streamMode}")
	@PostMapping("/devices/{deviceId}/transport/{streamMode}")
	public ResponseEntity<PageResult> updateTransport(@PathVariable String deviceId, @PathVariable String streamMode){
		Device device = storager.queryVideoDevice(deviceId);
		device.setStreamMode(streamMode);
		storager.updateDevice(device);
		return new ResponseEntity<>(null,HttpStatus.OK);
	}
}
