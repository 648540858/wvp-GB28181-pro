package com.genersoft.iot.vmp.vmanager.gb28181.record;

import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
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

import java.util.UUID;

@Api(tags = "国标录像")
@CrossOrigin
@RestController
@RequestMapping("/api/gb_record")
public class GBRecordController {

	private final static Logger logger = LoggerFactory.getLogger(GBRecordController.class);

	@Autowired
	private SIPCommander cmder;

	@Autowired
	private IVideoManagerStorager storager;

	@Autowired
	private DeferredResultHolder resultHolder;

	@ApiOperation("录像查询")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value = "设备ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "channelId", value = "通道ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "startTime", value = "开始时间", dataTypeClass = String.class),
			@ApiImplicitParam(name = "endTime", value = "结束时间", dataTypeClass = String.class),
	})
	@GetMapping("/query/{deviceId}/{channelId}")
	public DeferredResult<ResponseEntity<RecordInfo>> recordinfo(@PathVariable String deviceId,@PathVariable String channelId, String startTime,  String endTime){

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("录像信息查询 API调用，deviceId：%s ，startTime：%s， startTime：%s",deviceId, startTime, endTime));
		}

		Device device = storager.queryVideoDevice(deviceId);
		// 指定超时时间 1分钟30秒
		DeferredResult<ResponseEntity<RecordInfo>> result = new DeferredResult<>(90*1000L);
		String uuid = UUID.randomUUID().toString();
		int sn  =  (int)((Math.random()*9+1)*100000);
		String key = DeferredResultHolder.CALLBACK_CMD_RECORDINFO + deviceId + sn;
		RequestMessage msg = new RequestMessage();
		msg.setId(uuid);
		msg.setKey(key);
		cmder.recordInfoQuery(device, channelId, startTime, endTime, sn, (eventResult -> {
			msg.setData("查询录像失败, status: " +  eventResult.statusCode + ", message: " + eventResult.msg );
			resultHolder.invokeResult(msg);
		}));

		// 录像查询以channelId作为deviceId查询
		resultHolder.put(key, uuid, result);
		result.onTimeout(()->{
			msg.setData("timeout");
			resultHolder.invokeResult(msg);
		});
        return result;
	}
}
