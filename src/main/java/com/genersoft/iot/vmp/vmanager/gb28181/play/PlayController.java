package com.genersoft.iot.vmp.vmanager.gb28181.play;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.MediaServerConfig;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.vmanager.gb28181.play.bean.PlayResult;
import com.genersoft.iot.vmp.service.IMediaService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.UUID;

import javax.sip.message.Response;

@Api(tags = "国标设备点播")
@CrossOrigin
@RestController
@RequestMapping("/api/play")
public class PlayController {

	private final static Logger logger = LoggerFactory.getLogger(PlayController.class);

	@Autowired
	private SIPCommander cmder;

	@Autowired
	private IVideoManagerStorager storager;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;

	@Autowired
	private ZLMRESTfulUtils zlmresTfulUtils;

	@Autowired
	private DeferredResultHolder resultHolder;

	@Autowired
	private IPlayService playService;

	@Autowired
	private IMediaService mediaService;

	@ApiOperation("开始点播")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value = "设备ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "channelId", value = "通道ID", dataTypeClass = String.class),
	})
	@GetMapping("/start/{deviceId}/{channelId}")
	public DeferredResult<ResponseEntity<String>> play(@PathVariable String deviceId,
													   @PathVariable String channelId) {

		PlayResult playResult = playService.play(deviceId, channelId, null, null);

		// 超时处理
		playResult.getResult().onTimeout(()->{
			logger.warn(String.format("设备点播超时，deviceId：%s ，channelId：%s", deviceId, channelId));
			// 释放rtpserver
			cmder.closeRTPServer(playResult.getDevice(), channelId);
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_PlAY + playResult.getUuid());
			msg.setData("Timeout");
			resultHolder.invokeResult(msg);
		});
		return playResult.getResult();
	}

	@ApiOperation("停止点播")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "streamId", value = "视频流ID", dataTypeClass = String.class),
	})
	@GetMapping("/stop/{streamId}")
	public DeferredResult<ResponseEntity<String>> playStop(@PathVariable String streamId) {

		logger.debug(String.format("设备预览/回放停止API调用，streamId：%s", streamId));

		UUID uuid = UUID.randomUUID();
		DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String>>();

		// 录像查询以channelId作为deviceId查询
		resultHolder.put(DeferredResultHolder.CALLBACK_CMD_STOP + uuid, result);

		cmder.streamByeCmd(streamId, event -> {
			StreamInfo streamInfo = redisCatchStorage.queryPlayByStreamId(streamId);
			if (streamInfo == null) {
				RequestMessage msg = new RequestMessage();
				msg.setId(DeferredResultHolder.CALLBACK_CMD_PlAY + uuid);
				msg.setData("streamId not found");
				resultHolder.invokeResult(msg);
			}else {
				redisCatchStorage.stopPlay(streamInfo);
				storager.stopPlay(streamInfo.getDeviceID(), streamInfo.getChannelId());
				RequestMessage msg = new RequestMessage();
				msg.setId(DeferredResultHolder.CALLBACK_CMD_STOP + uuid);
				//Response response = event.getResponse();
				msg.setData(String.format("success"));
				resultHolder.invokeResult(msg);
			}
		});

		if (streamId != null) {
			JSONObject json = new JSONObject();
			json.put("streamId", streamId);
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_PlAY + uuid);
			msg.setData(json.toString());
			resultHolder.invokeResult(msg);
		} else {
			logger.warn("设备预览/回放停止API调用失败！");
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_PlAY + uuid);
			msg.setData("streamId null");
			resultHolder.invokeResult(msg);
		}

		// 超时处理
		result.onTimeout(()->{
			logger.warn(String.format("设备预览/回放停止超时，streamId：%s ", streamId));
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_STOP + uuid);
			msg.setData("Timeout");
			resultHolder.invokeResult(msg);
		});
		return result;
	}

	/**
	 * 将不是h264的视频通过ffmpeg 转码为h264 + aac
	 * @param streamId 流ID
	 * @return
	 */
	@ApiOperation("将不是h264的视频通过ffmpeg 转码为h264 + aac")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "streamId", value = "视频流ID", dataTypeClass = String.class),
	})
	@PostMapping("/convert/{streamId}")
	public ResponseEntity<String> playConvert(@PathVariable String streamId) {
		StreamInfo streamInfo = redisCatchStorage.queryPlayByStreamId(streamId);
		if (streamInfo == null) {
			logger.warn("视频转码API调用失败！, 视频流已经停止!");
			return new ResponseEntity<String>("未找到视频流信息, 视频流可能已经停止", HttpStatus.OK);
		}
		JSONObject rtpInfo = zlmresTfulUtils.getRtpInfo(streamId);
		if (!rtpInfo.getBoolean("exist")) {
			logger.warn("视频转码API调用失败！, 视频流已停止推流!");
			return new ResponseEntity<String>("推流信息在流媒体中不存在, 视频流可能已停止推流", HttpStatus.OK);
		} else {
			MediaServerConfig mediaInfo = redisCatchStorage.getMediaInfo();
			String dstUrl = String.format("rtmp://%s:%s/convert/%s", "127.0.0.1", mediaInfo.getRtmpPort(),
					streamId );
			String srcUrl = String.format("rtsp://%s:%s/rtp/%s", "127.0.0.1", mediaInfo.getRtspPort(), streamId);
			JSONObject jsonObject = zlmresTfulUtils.addFFmpegSource(srcUrl, dstUrl, "1000000");
			System.out.println(jsonObject);
			JSONObject result = new JSONObject();
			if (jsonObject != null && jsonObject.getInteger("code") == 0) {
				   result.put("code", 0);
				JSONObject data = jsonObject.getJSONObject("data");
				if (data != null) {
				   	result.put("key", data.getString("key"));
					StreamInfo streamInfoResult = mediaService.getStreamInfoByAppAndStreamWithCheck("convert", streamId);
					result.put("data", streamInfoResult);
				}
			}else {
				result.put("code", 1);
				result.put("msg", "cover fail");
			}
			return new ResponseEntity<String>( result.toJSONString(), HttpStatus.OK);
		}
	}

	/**
	 * 结束转码
	 * @param key
	 * @return
	 */
	@ApiOperation("结束转码")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "key", value = "视频流key", dataTypeClass = String.class),
	})
	@PostMapping("/convertStop/{key}")
	public ResponseEntity<String> playConvertStop(@PathVariable String key) {

		JSONObject jsonObject = zlmresTfulUtils.delFFmpegSource(key);
		System.out.println(jsonObject);
		JSONObject result = new JSONObject();
		if (jsonObject != null && jsonObject.getInteger("code") == 0) {
			result.put("code", 0);
			JSONObject data = jsonObject.getJSONObject("data");
			if (data != null && data.getBoolean("flag")) {
				result.put("code", "0");
				result.put("msg", "success");
			}else {

			}
		}else {
			result.put("code", 1);
			result.put("msg", "delFFmpegSource fail");
		}
		return new ResponseEntity<String>( result.toJSONString(), HttpStatus.OK);
	}

	@ApiOperation("语音广播命令")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value = "设备Id", dataTypeClass = String.class),
	})
    @GetMapping("/broadcast/{deviceId}")
    @PostMapping("/broadcast/{deviceId}")
    public DeferredResult<ResponseEntity<String>> broadcastApi(@PathVariable String deviceId) {
        if (logger.isDebugEnabled()) {
            logger.debug("语音广播API调用");
        }
        Device device = storager.queryVideoDevice(deviceId);
		cmder.audioBroadcastCmd(device, event -> {
			Response response = event.getResponse();
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_BROADCAST + deviceId);
			JSONObject json = new JSONObject();
			json.put("DeviceID", deviceId);
			json.put("CmdType", "Broadcast");
			json.put("Result", "Failed");
			json.put("Description", String.format("语音广播操作失败，错误码： %s, %s", response.getStatusCode(), response.getReasonPhrase()));
			msg.setData(json);
			resultHolder.invokeResult(msg);
		});
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String>>(3 * 1000L);
		result.onTimeout(() -> {
			logger.warn(String.format("语音广播操作超时, 设备未返回应答指令"));
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_BROADCAST + deviceId);
			JSONObject json = new JSONObject();
			json.put("DeviceID", deviceId);
			json.put("CmdType", "Broadcast");
			json.put("Result", "Failed");
			json.put("Error", "Timeout. Device did not response to broadcast command.");
			msg.setData(json);
			resultHolder.invokeResult(msg);
		});
		resultHolder.put(DeferredResultHolder.CALLBACK_CMD_BROADCAST + deviceId, result);
		return result;
	}

}

