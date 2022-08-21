package com.genersoft.iot.vmp.vmanager.gb28181.playback;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.service.IPlayService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import org.springframework.web.context.request.async.DeferredResult;

@Tag(name = "视频回放")
@CrossOrigin
@RestController
@RequestMapping("/api/playback")
public class PlaybackController {

	private final static Logger logger = LoggerFactory.getLogger(PlaybackController.class);

	@Autowired
	private SIPCommander cmder;

	@Autowired
	private IVideoManagerStorage storager;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;

	@Autowired
	private IPlayService playService;

	@Autowired
	private DeferredResultHolder resultHolder;

	@Operation(summary = "开始视频回放")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "startTime", description = "开始时间", required = true)
	@Parameter(name = "endTime", description = "结束时间", required = true)
	@GetMapping("/start/{deviceId}/{channelId}")
	public DeferredResult<String> play(@PathVariable String deviceId, @PathVariable String channelId,
													   String startTime,String endTime) {

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("设备回放 API调用，deviceId：%s ，channelId：%s", deviceId, channelId));
		}

		DeferredResult<String> result = playService.playBack(deviceId, channelId, startTime, endTime, null, wvpResult->{
			resultHolder.invokeResult(wvpResult.getData());
		});

		return result;
	}


	@Operation(summary = "停止视频回放")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@Parameter(name = "stream", description = "流ID", required = true)
	@GetMapping("/stop/{deviceId}/{channelId}/{stream}")
	public void playStop(
			@PathVariable String deviceId,
			@PathVariable String channelId,
			@PathVariable String stream) {
		if (ObjectUtils.isEmpty(deviceId) || ObjectUtils.isEmpty(channelId) || ObjectUtils.isEmpty(stream)) {
			throw new ControllerException(ErrorCode.ERROR400);
		}
		cmder.streamByeCmd(deviceId, channelId, stream, null);
	}


	@Operation(summary = "回放暂停")
	@Parameter(name = "streamId", description = "回放流ID", required = true)
	@GetMapping("/pause/{streamId}")
	public void playPause(@PathVariable String streamId) {
		logger.info("playPause: "+streamId);
		StreamInfo streamInfo = redisCatchStorage.queryPlayback(null, null, streamId, null);
		if (null == streamInfo) {
			logger.warn("streamId不存在!");
			throw new ControllerException(ErrorCode.ERROR400.getCode(), "streamId不存在");
		}
		Device device = storager.queryVideoDevice(streamInfo.getDeviceID());
		cmder.playPauseCmd(device, streamInfo);
	}


	@Operation(summary = "回放恢复")
	@Parameter(name = "streamId", description = "回放流ID", required = true)
	@GetMapping("/resume/{streamId}")
	public void playResume(@PathVariable String streamId) {
		logger.info("playResume: "+streamId);
		JSONObject json = new JSONObject();
		StreamInfo streamInfo = redisCatchStorage.queryPlayback(null, null, streamId, null);
		if (null == streamInfo) {
			json.put("msg", "streamId不存在");
			logger.warn("streamId不存在!");
			throw new ControllerException(ErrorCode.ERROR400.getCode(), "streamId不存在");
		}
		Device device = storager.queryVideoDevice(streamInfo.getDeviceID());
		cmder.playResumeCmd(device, streamInfo);
	}


	@Operation(summary = "回放拖动播放")
	@Parameter(name = "streamId", description = "回放流ID", required = true)
	@Parameter(name = "seekTime", description = "拖动偏移量，单位s", required = true)
	@GetMapping("/seek/{streamId}/{seekTime}")
	public void playSeek(@PathVariable String streamId, @PathVariable long seekTime) {
		logger.info("playSeek: "+streamId+", "+seekTime);
		StreamInfo streamInfo = redisCatchStorage.queryPlayback(null, null, streamId, null);
		if (null == streamInfo) {
			logger.warn("streamId不存在!");
			throw new ControllerException(ErrorCode.ERROR400.getCode(), "streamId不存在");
		}
		Device device = storager.queryVideoDevice(streamInfo.getDeviceID());
		cmder.playSeekCmd(device, streamInfo, seekTime);
	}

	@Operation(summary = "回放倍速播放")
	@Parameter(name = "streamId", description = "回放流ID", required = true)
	@Parameter(name = "speed", description = "倍速0.25 0.5 1、2、4", required = true)
	@GetMapping("/speed/{streamId}/{speed}")
	public void playSpeed(@PathVariable String streamId, @PathVariable Double speed) {
		logger.info("playSpeed: "+streamId+", "+speed);
		StreamInfo streamInfo = redisCatchStorage.queryPlayback(null, null, streamId, null);
		if (null == streamInfo) {
			logger.warn("streamId不存在!");
			throw new ControllerException(ErrorCode.ERROR400.getCode(), "streamId不存在");
		}
		if(speed != 0.25 && speed != 0.5 && speed != 1 && speed != 2.0 && speed != 4.0) {
			logger.warn("不支持的speed： " + speed);
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "不支持的speed（0.25 0.5 1、2、4）");
		}
		Device device = storager.queryVideoDevice(streamInfo.getDeviceID());
		cmder.playSpeedCmd(device, streamInfo, speed);
	}
}
