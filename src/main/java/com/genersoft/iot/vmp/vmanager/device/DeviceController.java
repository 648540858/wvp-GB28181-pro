package com.genersoft.iot.vmp.vmanager.device;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;

@RestController
@RequestMapping("/api")
public class DeviceController {
	
	private final static Logger logger = LoggerFactory.getLogger(DeviceController.class);
	
	@Autowired
	private IVideoManagerStorager storager;
	
	@GetMapping("/devices/{deviceId}")
	public ResponseEntity<List<Device>> devices(@PathVariable String deviceId){
		
		if (logger.isDebugEnabled()) {
			logger.debug("查询视频设备API调用，deviceId：" + deviceId);
		}
		
		List<Device> deviceList = new ArrayList<>();
		deviceList.add(storager.queryVideoDevice(deviceId));
		return new ResponseEntity<>(deviceList,HttpStatus.OK);
	}
	
	@GetMapping("/devices")
	public ResponseEntity<List<Device>> devices(){
		
		if (logger.isDebugEnabled()) {
			logger.debug("查询所有视频设备API调用");
		}
		
		List<Device> deviceList = storager.queryVideoDeviceList(null);
		return new ResponseEntity<>(deviceList,HttpStatus.OK);
	}
}
