package com.genersoft.iot.vmp.vmanager.ptz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;

@CrossOrigin
@RestController
@RequestMapping("/api")
public class PtzController {
	
	private final static Logger logger = LoggerFactory.getLogger(PtzController.class);
	
	@Autowired
	private SIPCommander cmder;
	
	@Autowired
	private IVideoManagerStorager storager;

	/***
	 * http://localhost:8080/api/ptz/34020000001320000002_34020000001320000008?leftRight=1&upDown=0&inOut=0&moveSpeed=50&zoomSpeed=0
	 * @param deviceId
	 * @param channelId
	 * @param leftRight
	 * @param upDown
	 * @param inOut
	 * @param moveSpeed
	 * @param zoomSpeed
	 * @return
	 */
	@PostMapping("/ptz/{deviceId}/{channelId}")
	public ResponseEntity<String> ptz(@PathVariable String deviceId,@PathVariable String channelId,int cmdCode, int horizonSpeed, int verticalSpeed, int zoomSpeed){
		
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("设备云台控制 API调用，deviceId：%s ，channelId：%s ，cmdCode：%d ，horizonSpeed：%d ，verticalSpeed：%d ，zoomSpeed：%d",deviceId, channelId, cmdCode, horizonSpeed, verticalSpeed, zoomSpeed));
		}
		Device device = storager.queryVideoDevice(deviceId);
		
		cmder.frontEndCmd(device, channelId, cmdCode, horizonSpeed, verticalSpeed, zoomSpeed);
		return new ResponseEntity<String>("success",HttpStatus.OK);
	}
	// public ResponseEntity<String> ptz(@PathVariable String deviceId,@PathVariable String channelId,int leftRight, int upDown, int inOut, int moveSpeed, int zoomSpeed){
		
	// 	if (logger.isDebugEnabled()) {
	// 		logger.debug(String.format("设备云台控制 API调用，deviceId：%s ，channelId：%s ，leftRight：%d ，upDown：%d ，inOut：%d ，moveSpeed：%d ，zoomSpeed：%d",deviceId, channelId, leftRight, upDown, inOut, moveSpeed, zoomSpeed));
	// 	}
	// 	Device device = storager.queryVideoDevice(deviceId);
		
	// 	cmder.ptzCmd(device, channelId, leftRight, upDown, inOut, moveSpeed, zoomSpeed);
	// 	return new ResponseEntity<String>("success",HttpStatus.OK);
	// }
	@PostMapping("/frontEndCommand/{deviceId}/{channelId}")
	public ResponseEntity<String> frontEndCommand(@PathVariable String deviceId,@PathVariable String channelId,int cmdCode, int parameter1, int parameter2, int combindCode2){
		
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("设备云台控制 API调用，deviceId：%s ，channelId：%s ，cmdCode：%d parameter1：%d parameter2：%d",deviceId, channelId, cmdCode, parameter1, parameter2));
		}
		Device device = storager.queryVideoDevice(deviceId);
		
		cmder.frontEndCmd(device, channelId, cmdCode, parameter1, parameter2, combindCode2);
		return new ResponseEntity<String>("success",HttpStatus.OK);
	}
}
