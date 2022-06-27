package com.genersoft.iot.vmp.vmanager.gb28181.record;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IPlayService;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
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

@Api(tags = "国标录像")
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

	@Autowired
	private IMediaServerService mediaServerService;

	@ApiOperation("录像查询")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value = "设备ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "channelId", value = "通道ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "startTime", value = "开始时间", dataTypeClass = String.class),
			@ApiImplicitParam(name = "endTime", value = "结束时间", dataTypeClass = String.class),
	})
	@GetMapping("/query/{deviceId}/{channelId}")
	public DeferredResult<ResponseEntity<WVPResult<RecordInfo>>> recordinfo(@PathVariable String deviceId, @PathVariable String channelId, String startTime, String endTime){

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("录像信息查询 API调用，deviceId：%s ，startTime：%s， endTime：%s",deviceId, startTime, endTime));
		}
		DeferredResult<ResponseEntity<WVPResult<RecordInfo>>> result = new DeferredResult<>();
		if (!DateUtil.verification(startTime, DateUtil.formatter)){
			WVPResult<RecordInfo> wvpResult = new WVPResult<>();
			wvpResult.setCode(-1);
			wvpResult.setMsg("startTime error, format is " + DateUtil.PATTERN);

			ResponseEntity<WVPResult<RecordInfo>> resultResponseEntity = new ResponseEntity<>(wvpResult, HttpStatus.OK);
			result.setResult(resultResponseEntity);
			return result;
		}
		if (!DateUtil.verification(endTime, DateUtil.formatter)){
			WVPResult<RecordInfo> wvpResult = new WVPResult<>();
			wvpResult.setCode(-1);
			wvpResult.setMsg("endTime error, format is " + DateUtil.PATTERN);
			ResponseEntity<WVPResult<RecordInfo>> resultResponseEntity = new ResponseEntity<>(wvpResult, HttpStatus.OK);
			result.setResult(resultResponseEntity);
			return result;
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
			wvpResult.setCode(-1);
			wvpResult.setMsg("查询录像失败, status: " +  eventResult.statusCode + ", message: " + eventResult.msg);
			msg.setData(wvpResult);
			resultHolder.invokeResult(msg);
		}));

		// 录像查询以channelId作为deviceId查询
		resultHolder.put(key, uuid, result);
		result.onTimeout(()->{
			msg.setData("timeout");
			WVPResult<RecordInfo> wvpResult = new WVPResult<>();
			wvpResult.setCode(-1);
			wvpResult.setMsg("timeout");
			msg.setData(wvpResult);
			resultHolder.invokeResult(msg);
		});
        return result;
	}

	@ApiOperation("开始历史媒体下载")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value = "设备ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "channelId", value = "通道ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "startTime", value = "开始时间", dataTypeClass = String.class),
			@ApiImplicitParam(name = "endTime", value = "结束时间", dataTypeClass = String.class),
			@ApiImplicitParam(name = "downloadSpeed", value = "下载倍速", dataTypeClass = String.class),
	})
	@GetMapping("/download/start/{deviceId}/{channelId}")
	public DeferredResult<ResponseEntity<String>> download(@PathVariable String deviceId, @PathVariable String channelId,
													   String startTime, String endTime, String downloadSpeed) {

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("历史媒体下载 API调用，deviceId：%s，channelId：%s，downloadSpeed：%s", deviceId, channelId, downloadSpeed));
		}
//		String key = DeferredResultHolder.CALLBACK_CMD_DOWNLOAD + deviceId + channelId;
//		String uuid = UUID.randomUUID().toString();
//		DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String>>(30000L);
//		// 超时处理
//		result.onTimeout(()->{
//			logger.warn(String.format("设备下载响应超时，deviceId：%s ，channelId：%s", deviceId, channelId));
//			RequestMessage msg = new RequestMessage();
//			msg.setId(uuid);
//			msg.setKey(key);
//			msg.setData("Timeout");
//			resultHolder.invokeAllResult(msg);
//		});
//		if(resultHolder.exist(key, null)) {
//			return result;
//		}
//		resultHolder.put(key, uuid, result);
//		Device device = storager.queryVideoDevice(deviceId);
//
//		MediaServerItem newMediaServerItem = playService.getNewMediaServerItem(device);
//		if (newMediaServerItem == null) {
//			logger.warn(String.format("设备下载响应超时，deviceId：%s ，channelId：%s", deviceId, channelId));
//			RequestMessage msg = new RequestMessage();
//			msg.setId(uuid);
//			msg.setKey(key);
//			msg.setData("Timeout");
//			resultHolder.invokeAllResult(msg);
//			return result;
//		}
//
//		SSRCInfo ssrcInfo = mediaServerService.openRTPServer(newMediaServerItem, null, true);
//
//		cmder.downloadStreamCmd(newMediaServerItem, ssrcInfo, device, channelId, startTime, endTime, downloadSpeed, (InviteStreamInfo inviteStreamInfo) -> {
//			logger.info("收到订阅消息： " + inviteStreamInfo.getResponse().toJSONString());
//			playService.onPublishHandlerForDownload(inviteStreamInfo, deviceId, channelId, uuid);
//		}, event -> {
//			RequestMessage msg = new RequestMessage();
//			msg.setId(uuid);
//			msg.setKey(key);
//			msg.setData(String.format("回放失败， 错误码： %s, %s", event.statusCode, event.msg));
//			resultHolder.invokeAllResult(msg);
//		});

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("设备回放 API调用，deviceId：%s ，channelId：%s", deviceId, channelId));
		}

		DeferredResult<ResponseEntity<String>> result = playService.download(deviceId, channelId, startTime, endTime, Integer.parseInt(downloadSpeed), null, hookCallBack->{
			resultHolder.invokeResult(hookCallBack.getData());
		});

		return result;
	}

	@ApiOperation("停止历史媒体下载")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value = "设备ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "channelId", value = "通道ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "stream", value = "流ID", dataTypeClass = String.class),
	})
	@GetMapping("/download/stop/{deviceId}/{channelId}/{stream}")
	public ResponseEntity<String> playStop(@PathVariable String deviceId, @PathVariable String channelId, @PathVariable String stream) {

		cmder.streamByeCmd(deviceId, channelId, stream, null);

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("设备历史媒体下载停止 API调用，deviceId/channelId：%s_%s", deviceId, channelId));
		}

		if (deviceId != null && channelId != null) {
			JSONObject json = new JSONObject();
			json.put("deviceId", deviceId);
			json.put("channelId", channelId);
			return new ResponseEntity<String>(json.toString(), HttpStatus.OK);
		} else {
			logger.warn("设备历史媒体下载停止API调用失败！");
			return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@ApiOperation("获取历史媒体下载进度")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value = "设备ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "channelId", value = "通道ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "stream", value = "流ID", dataTypeClass = String.class),
	})
	@GetMapping("/download/progress/{deviceId}/{channelId}/{stream}")
	public ResponseEntity<StreamInfo> getProgress(@PathVariable String deviceId, @PathVariable String channelId, @PathVariable String stream) {

		StreamInfo streamInfo = playService.getDownLoadInfo(deviceId, channelId, stream);
		return new ResponseEntity<>(streamInfo, HttpStatus.OK);
	}
}
