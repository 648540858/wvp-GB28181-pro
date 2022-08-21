package com.genersoft.iot.vmp.vmanager.gb28181.record;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IPlayService;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name  = "国标录像")
@CrossOrigin
@RestController
@RequestMapping("/api/gb_record")
public class GBRecordController {

	private final static Logger logger = LoggerFactory.getLogger(GBRecordController.class);

	@Autowired
	private SIPCommander cmder;

	@Autowired
	private IVideoManagerStorage storager;

	@Autowired
	private DeferredResultHolder resultHolder;

	@Autowired
	private IPlayService playService;

	@Operation(summary = "录像查询")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "startTime", description = "开始时间", required = true)
	@Parameter(name = "endTime", description = "结束时间", required = true)
	@GetMapping("/query/{deviceId}/{channelId}")
	public DeferredResult<WVPResult<RecordInfo>> recordinfo(@PathVariable String deviceId, @PathVariable String channelId, String startTime, String endTime){

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("录像信息查询 API调用，deviceId：%s ，startTime：%s， endTime：%s",deviceId, startTime, endTime));
		}
		DeferredResult<WVPResult<RecordInfo>> result = new DeferredResult<>();
		if (!DateUtil.verification(startTime, DateUtil.formatter)){
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "startTime error, format is " + DateUtil.PATTERN);
		}
		if (!DateUtil.verification(endTime, DateUtil.formatter)){
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "endTime error, format is " + DateUtil.PATTERN);
		}

		Device device = storager.queryVideoDevice(deviceId);
		// 指定超时时间 1分钟30秒
		String uuid = UUID.randomUUID().toString();
		int sn  =  (int)((Math.random()*9+1)*100000);
		String key = DeferredResultHolder.CALLBACK_CMD_RECORDINFO + deviceId + sn;
		RequestMessage msg = new RequestMessage();
		msg.setId(uuid);
		msg.setKey(key);
		cmder.recordInfoQuery(device, channelId, startTime, endTime, sn, null, null, null, (eventResult -> {
			WVPResult<RecordInfo> wvpResult = new WVPResult<>();
			wvpResult.setCode(ErrorCode.ERROR100.getCode());
			wvpResult.setMsg("查询录像失败, status: " +  eventResult.statusCode + ", message: " + eventResult.msg);
			msg.setData(wvpResult);
			resultHolder.invokeResult(msg);
		}));

		// 录像查询以channelId作为deviceId查询
		resultHolder.put(key, uuid, result);
		result.onTimeout(()->{
			msg.setData("timeout");
			WVPResult<RecordInfo> wvpResult = new WVPResult<>();
			wvpResult.setCode(ErrorCode.ERROR100.getCode());
			wvpResult.setMsg("timeout");
			msg.setData(wvpResult);
			resultHolder.invokeResult(msg);
		});
        return result;
	}


	@Operation(summary = "开始历史媒体下载")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "startTime", description = "开始时间", required = true)
	@Parameter(name = "endTime", description = "结束时间", required = true)
	@Parameter(name = "downloadSpeed", description = "下载倍速", required = true)
	@GetMapping("/download/start/{deviceId}/{channelId}")
	public DeferredResult<String> download(@PathVariable String deviceId, @PathVariable String channelId,
													   String startTime, String endTime, String downloadSpeed) {

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("历史媒体下载 API调用，deviceId：%s，channelId：%s，downloadSpeed：%s", deviceId, channelId, downloadSpeed));
		}

		DeferredResult<String> result = playService.download(deviceId, channelId, startTime, endTime, Integer.parseInt(downloadSpeed), null, hookCallBack->{
			resultHolder.invokeResult(hookCallBack.getData());
		});

		return result;
	}

	@Operation(summary = "停止历史媒体下载")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "stream", description = "流ID", required = true)
	@GetMapping("/download/stop/{deviceId}/{channelId}/{stream}")
	public void playStop(@PathVariable String deviceId, @PathVariable String channelId, @PathVariable String stream) {

		cmder.streamByeCmd(deviceId, channelId, stream, null);

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("设备历史媒体下载停止 API调用，deviceId/channelId：%s_%s", deviceId, channelId));
		}

		if (deviceId == null || channelId == null) {
			throw new ControllerException(ErrorCode.ERROR100);
		}
	}

	@Operation(summary = "获取历史媒体下载进度")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "stream", description = "流ID", required = true)
	@GetMapping("/download/progress/{deviceId}/{channelId}/{stream}")
	public StreamInfo getProgress(@PathVariable String deviceId, @PathVariable String channelId, @PathVariable String stream) {
		return playService.getDownLoadInfo(deviceId, channelId, stream);
	}
}
