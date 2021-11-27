package com.genersoft.iot.vmp.vmanager.gb28181.playback;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
//import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.service.IPlayService;
import com.genersoft.iot.vmp.vmanager.gb28181.session.InfoCseqCache;
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

@Api(tags = "视频回放")
@CrossOrigin
@RestController
@RequestMapping("/api/playback")
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

	@Autowired
	private IMediaServerService mediaServerService;

	@ApiOperation("开始视频回放")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value = "设备ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "channelId", value = "通道ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "startTime", value = "开始时间", dataTypeClass = String.class),
			@ApiImplicitParam(name = "endTime", value = "结束时间", dataTypeClass = String.class),
	})
	@GetMapping("/start/{deviceId}/{channelId}")
	public DeferredResult<ResponseEntity<String>> play(@PathVariable String deviceId, @PathVariable String channelId,
													   String startTime,String endTime) {

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("设备回放 API调用，deviceId：%s ，channelId：%s", deviceId, channelId));
		}
		String uuid = UUID.randomUUID().toString();
		String key = DeferredResultHolder.CALLBACK_CMD_PLAYBACK + deviceId + channelId;
		DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String>>(30000L);
		Device device = storager.queryVideoDevice(deviceId);
		if (device == null) {
			result.setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
			RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
			msg.setKey(key);
			msg.setData("设备不存在");
			resultHolder.invokeAllResult(msg);
			return result;
		}
		//判断设备是否在线，不在线直接返回
		if(device.getOnline()==0)
		{
			result.setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
			RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
			msg.setKey(key);
			msg.setData("设备不在线");
			resultHolder.invokeAllResult(msg);
			return result;
		}
		MediaServerItem newMediaServerItem = playService.getNewMediaServerItem(device);
		SSRCInfo ssrcInfo = mediaServerService.openRTPServer(newMediaServerItem, null, true);

		// 超时处理
		result.onTimeout(()->{
			logger.warn(String.format("设备回放超时，deviceId：%s ，channelId：%s", deviceId, channelId));
			RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
			msg.setKey(key);
			msg.setData("Timeout");
			resultHolder.invokeResult(msg);
		});

		StreamInfo streamInfo = redisCatchStorage.queryPlaybackByDevice(deviceId, channelId);
		if (streamInfo != null) {
			// 停止之前的回放
			cmder.streamByeCmd(deviceId, channelId);
		}
		resultHolder.put(DeferredResultHolder.CALLBACK_CMD_PLAY + deviceId + channelId, uuid, result);

		if (newMediaServerItem == null) {
			logger.warn(String.format("设备回放超时，deviceId：%s ，channelId：%s", deviceId, channelId));
			RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
			msg.setKey(key);
			msg.setData("Timeout");
			resultHolder.invokeResult(msg);
			return result;
		}

		cmder.playbackStreamCmd(newMediaServerItem, ssrcInfo, device, channelId, startTime, endTime, (MediaServerItem mediaServerItem, JSONObject response) -> {
			logger.info("收到订阅消息： " + response.toJSONString());
			playService.onPublishHandlerForPlayBack(mediaServerItem, response, deviceId, channelId, uuid.toString());
		}, event -> {
			RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
			msg.setKey(key);
			msg.setData(String.format("回放失败， 错误码： %s, %s", event.statusCode, event.msg));
			resultHolder.invokeResult(msg);
		});

		return result;
	}

	@ApiOperation("停止视频回放")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value = "设备ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "channelId", value = "通道ID", dataTypeClass = String.class),
	})
	@GetMapping("/stop/{deviceId}/{channelId}")
	public ResponseEntity<String> playStop(@PathVariable String deviceId, @PathVariable String channelId) {

		cmder.streamByeCmd(deviceId, channelId);

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("设备录像回放停止 API调用，deviceId/channelId：%s/%s", deviceId, channelId));
		}

		if (deviceId != null && channelId != null) {
			JSONObject json = new JSONObject();
			json.put("deviceId", deviceId);
			json.put("channelId", channelId);
			return new ResponseEntity<String>(json.toString(), HttpStatus.OK);
		} else {
			logger.warn("设备录像回放停止API调用失败！");
			return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@ApiOperation("回放暂停")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "streamId", value = "回放流ID", dataTypeClass = String.class),
	})
	@GetMapping("/pause/{streamId}")
	public ResponseEntity<String> playPause(@PathVariable String streamId) {
		logger.info("playPause: "+streamId);
		JSONObject json = new JSONObject();
		StreamInfo streamInfo = redisCatchStorage.queryPlaybackByStreamId(streamId);
		if (null == streamInfo) {
			json.put("msg", "streamId不存在");
			logger.warn("streamId不存在!");
			return new ResponseEntity<String>(json.toString(), HttpStatus.BAD_REQUEST);
		}
		setCseq(streamId);
		Device device = storager.queryVideoDevice(streamInfo.getDeviceID());
		cmder.playPauseCmd(device, streamInfo);
		json.put("msg", "ok");
		return new ResponseEntity<String>(json.toString(), HttpStatus.OK);
	}

	@ApiOperation("回放恢复")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "streamId", value = "回放流ID", dataTypeClass = String.class),
	})
	@GetMapping("/resume/{streamId}")
	public ResponseEntity<String> playResume(@PathVariable String streamId) {
		logger.info("playResume: "+streamId);
		JSONObject json = new JSONObject();
		StreamInfo streamInfo = redisCatchStorage.queryPlaybackByStreamId(streamId);
		if (null == streamInfo) {
			json.put("msg", "streamId不存在");
			logger.warn("streamId不存在!");
			return new ResponseEntity<String>(json.toString(), HttpStatus.BAD_REQUEST);
		}
		setCseq(streamId);
		Device device = storager.queryVideoDevice(streamInfo.getDeviceID());
		cmder.playResumeCmd(device, streamInfo);
		json.put("msg", "ok");
		return new ResponseEntity<String>(json.toString(), HttpStatus.OK);
	}

	@ApiOperation("回放拖动播放")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "streamId", value = "回放流ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "seekTime", value = "拖动偏移量，单位s", dataTypeClass = Long.class),
	})
	@GetMapping("/seek/{streamId}/{seekTime}")
	public ResponseEntity<String> playSeek(@PathVariable String streamId, @PathVariable long seekTime) {
		logger.info("playSeek: "+streamId+", "+seekTime);
		JSONObject json = new JSONObject();
		StreamInfo streamInfo = redisCatchStorage.queryPlaybackByStreamId(streamId);
		if (null == streamInfo) {
			json.put("msg", "streamId不存在");
			logger.warn("streamId不存在!");
			return new ResponseEntity<String>(json.toString(), HttpStatus.BAD_REQUEST);
		}
		setCseq(streamId);
		Device device = storager.queryVideoDevice(streamInfo.getDeviceID());
		cmder.playSeekCmd(device, streamInfo, seekTime);
		json.put("msg", "ok");
		return new ResponseEntity<String>(json.toString(), HttpStatus.OK);
	}

	@ApiOperation("回放倍速播放")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "streamId", value = "回放流ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "speed", value = "倍速0.25 0.5 1、2、4", dataTypeClass = Double.class),
	})
	@GetMapping("/speed/{streamId}/{speed}")
	public ResponseEntity<String> playSpeed(@PathVariable String streamId, @PathVariable Double speed) {
		logger.info("playSpeed: "+streamId+", "+speed);
		JSONObject json = new JSONObject();
		StreamInfo streamInfo = redisCatchStorage.queryPlaybackByStreamId(streamId);
		if (null == streamInfo) {
			json.put("msg", "streamId不存在");
			logger.warn("streamId不存在!");
			return new ResponseEntity<String>(json.toString(), HttpStatus.BAD_REQUEST);
		}
		if(speed != 0.25 && speed != 0.5 && speed != 1 && speed != 2.0 && speed != 4.0) {
			json.put("msg", "不支持的speed（0.25 0.5 1、2、4）");
			logger.warn("不支持的speed： " + speed);
			return new ResponseEntity<String>(json.toString(), HttpStatus.BAD_REQUEST);
		}
		setCseq(streamId);
		Device device = storager.queryVideoDevice(streamInfo.getDeviceID());
		cmder.playSpeedCmd(device, streamInfo, speed);
		json.put("msg", "ok");
		return new ResponseEntity<String>(json.toString(), HttpStatus.OK);
	}

	public void setCseq(String streamId) {
		if (InfoCseqCache.CSEQCACHE.containsKey(streamId)) {
			InfoCseqCache.CSEQCACHE.put(streamId, InfoCseqCache.CSEQCACHE.get(streamId) + 1);
		} else {
			InfoCseqCache.CSEQCACHE.put(streamId, 2L);
		}
	}
}
