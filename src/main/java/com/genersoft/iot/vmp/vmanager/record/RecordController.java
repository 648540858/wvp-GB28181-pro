package com.genersoft.iot.vmp.vmanager.record;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.RecordInfo;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;

@CrossOrigin
@RestController
@RequestMapping("/api")
public class RecordController {
	
	private final static Logger logger = LoggerFactory.getLogger(RecordController.class);
	
	@Autowired
	private SIPCommander cmder;
	
	@Autowired
	private IVideoManagerStorager storager;
	
	@Autowired
	private DeferredResultHolder resultHolder;
	
	@GetMapping("/record/{deviceId}/{channelId}")
	public DeferredResult<ResponseEntity<RecordInfo>> recordinfo(@PathVariable String deviceId,@PathVariable String channelId, String startTime,  String endTime){
		
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("录像信息查询 API调用，deviceId：%s ，startTime：%s， startTime：%s",deviceId, startTime, endTime));
		}
		
		Device device = storager.queryVideoDevice(deviceId);
		cmder.recordInfoQuery(device, channelId, startTime, endTime);
		DeferredResult<ResponseEntity<RecordInfo>> result = new DeferredResult<ResponseEntity<RecordInfo>>();
		// 录像查询以channelId作为deviceId查询
		resultHolder.put(DeferredResultHolder.CALLBACK_CMD_RECORDINFO+channelId, result);
        return result;
	}
}
