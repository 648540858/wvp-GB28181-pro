package com.genersoft.iot.vmp.vmanager.playback;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
//import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.vmanager.service.IPlayService;
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

@CrossOrigin
@RestController
@RequestMapping("/api")
public class PlaybackController {

	private final static Logger logger = LoggerFactory.getLogger(PlaybackController.class);

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

	@GetMapping("/playback/{deviceId}/{channelId}")
	public DeferredResult<ResponseEntity<String>> play(@PathVariable String deviceId, @PathVariable String channelId, String startTime,
													   String endTime) {

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("设备回放 API调用，deviceId：%s ，channelId：%s", deviceId, channelId));
		}
		UUID uuid = UUID.randomUUID();
		DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String>>();
		// 超时处理
		result.onTimeout(()->{
			logger.warn(String.format("设备回放超时，deviceId：%s ，channelId：%s", deviceId, channelId));
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_PlAY + uuid);
			msg.setData("Timeout");
			resultHolder.invokeResult(msg);
		});
		Device device = storager.queryVideoDevice(deviceId);
		StreamInfo streamInfo = redisCatchStorage.queryPlaybackByDevice(deviceId, channelId);
		if (streamInfo != null) {
			// 停止之前的回放
			cmder.streamByeCmd(streamInfo.getStreamId());
		}
		resultHolder.put(DeferredResultHolder.CALLBACK_CMD_PlAY + uuid, result);
		cmder.playbackStreamCmd(device, channelId, startTime, endTime, (JSONObject response) -> {
			logger.info("收到订阅消息： " + response.toJSONString());
			playService.onPublishHandlerForPlayBack(response, deviceId, channelId, uuid.toString());
		}, event -> {
			Response response = event.getResponse();
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_PlAY + uuid);
			msg.setData(String.format("回放失败， 错误码： %s, %s", response.getStatusCode(), response.getReasonPhrase()));
			resultHolder.invokeResult(msg);
		});

		return result;
	}

	@RequestMapping("/playback/{ssrc}/stop")
	public ResponseEntity<String> playStop(@PathVariable String ssrc) {

		cmder.streamByeCmd(ssrc);

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("设备录像回放停止 API调用，ssrc：%s", ssrc));
		}

		if (ssrc != null) {
			JSONObject json = new JSONObject();
			json.put("ssrc", ssrc);
			return new ResponseEntity<String>(json.toString(), HttpStatus.OK);
		} else {
			logger.warn("设备录像回放停止API调用失败！");
			return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
