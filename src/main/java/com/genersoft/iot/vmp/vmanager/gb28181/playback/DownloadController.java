package com.genersoft.iot.vmp.vmanager.gb28181.playback;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.service.IPlayService;
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

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import org.springframework.web.context.request.async.DeferredResult;

import javax.sip.message.Response;
import java.util.UUID;

@Api(tags = "历史媒体下载")
@CrossOrigin
@RestController
@RequestMapping("/api/download")
public class DownloadController {

	private final static Logger logger = LoggerFactory.getLogger(DownloadController.class);

	@Autowired
	private SIPCommander cmder;

	@Autowired
	private IVideoManagerStorager storager;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;

	// @Autowired
	// private ZLMRESTfulUtils zlmresTfulUtils;

	@Autowired
	private IPlayService playService;

	@Autowired
	private DeferredResultHolder resultHolder;

	@Autowired
	private IMediaServerService mediaServerService;

	@ApiOperation("开始历史媒体下载")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value = "设备ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "channelId", value = "通道ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "startTime", value = "开始时间", dataTypeClass = String.class),
			@ApiImplicitParam(name = "endTime", value = "结束时间", dataTypeClass = String.class),
			@ApiImplicitParam(name = "downloadSpeed", value = "下载倍速", dataTypeClass = String.class),
	})
	@GetMapping("/start/{deviceId}/{channelId}")
	public DeferredResult<ResponseEntity<String>> play(@PathVariable String deviceId, @PathVariable String channelId,
													   String startTime, String endTime, String downloadSpeed) {

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("历史媒体下载 API调用，deviceId：%s，channelId：%s，downloadSpeed：%s", deviceId, channelId, downloadSpeed));
		}
		String key = DeferredResultHolder.CALLBACK_CMD_DOWNLOAD + deviceId + channelId;
		String uuid = UUID.randomUUID().toString();
		DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String>>(30000L);
		// 超时处理
		result.onTimeout(()->{
			logger.warn(String.format("设备下载响应超时，deviceId：%s ，channelId：%s", deviceId, channelId));
			RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
			msg.setKey(key);
			msg.setData("Timeout");
			resultHolder.invokeAllResult(msg);
		});
		if(resultHolder.exist(key, null)) {
			return result;
		}
		resultHolder.put(key, uuid, result);
		Device device = storager.queryVideoDevice(deviceId);
		StreamInfo streamInfo = redisCatchStorage.queryPlaybackByDevice(deviceId, channelId);
		if (streamInfo != null) {
			// 停止之前的下载
			cmder.streamByeCmd(deviceId, channelId, streamInfo.getStream());
		}

		MediaServerItem newMediaServerItem = playService.getNewMediaServerItem(device);
		if (newMediaServerItem == null) {
			logger.warn(String.format("设备下载响应超时，deviceId：%s ，channelId：%s", deviceId, channelId));
			RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
			msg.setKey(key);
			msg.setData("Timeout");
			resultHolder.invokeAllResult(msg);
			return result;
		}

		SSRCInfo ssrcInfo = mediaServerService.openRTPServer(newMediaServerItem, null, true);

		cmder.downloadStreamCmd(newMediaServerItem, ssrcInfo, device, channelId, startTime, endTime, downloadSpeed, (MediaServerItem mediaServerItem, JSONObject response) -> {
			logger.info("收到订阅消息： " + response.toJSONString());
			playService.onPublishHandlerForDownload(mediaServerItem, response, deviceId, channelId, uuid);
		}, event -> {
			RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
			msg.setKey(key);
			msg.setData(String.format("回放失败， 错误码： %s, %s", event.statusCode, event.msg));
			resultHolder.invokeAllResult(msg);
		});

		return result;
	}

	@ApiOperation("停止历史媒体下载")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value = "设备ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "channelId", value = "通道ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "stream", value = "流ID", dataTypeClass = String.class),
	})
	@GetMapping("/stop/{deviceId}/{channelId}/{stream}")
	public ResponseEntity<String> playStop(@PathVariable String deviceId, @PathVariable String channelId, @PathVariable String stream) {

		cmder.streamByeCmd(deviceId, channelId, stream);

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
}
