package com.genersoft.iot.vmp.vmanager.gb28181.play;

import com.alibaba.fastjson.JSONArray;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
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

import java.util.List;
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
	private VideoStreamSessionManager streamSession;

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

	@Autowired
	private IMediaServerService mediaServerService;

	@ApiOperation("开始点播")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value = "设备ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "channelId", value = "通道ID", dataTypeClass = String.class),
	})
	@GetMapping("/start/{deviceId}/{channelId}")
	public DeferredResult<ResponseEntity<String>> play(@PathVariable String deviceId,
													   @PathVariable String channelId) {

		// 获取可用的zlm
		Device device = storager.queryVideoDevice(deviceId);
		MediaServerItem newMediaServerItem = playService.getNewMediaServerItem(device);
		PlayResult playResult = playService.play(newMediaServerItem, deviceId, channelId, null, null);

		return playResult.getResult();
	}

	@ApiOperation("停止点播")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "deviceId", value = "设备ID", dataTypeClass = String.class),
			@ApiImplicitParam(name = "channelId", value = "通道ID", dataTypeClass = String.class),
	})
	@GetMapping("/stop/{deviceId}/{channelId}")
	public DeferredResult<ResponseEntity<String>> playStop(@PathVariable String deviceId, @PathVariable String channelId) {

		logger.debug(String.format("设备预览/回放停止API调用，streamId：%s_%s", deviceId, channelId ));

		String uuid = UUID.randomUUID().toString();
		DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String>>();

		// 录像查询以channelId作为deviceId查询
		String key = DeferredResultHolder.CALLBACK_CMD_STOP + deviceId + channelId;
		resultHolder.put(key, uuid, result);
		Device device = storager.queryVideoDevice(deviceId);
		cmder.streamByeCmd(deviceId, channelId, (event) -> {
			StreamInfo streamInfo = redisCatchStorage.queryPlayByDevice(deviceId, channelId);
			if (streamInfo == null) {
				RequestMessage msg = new RequestMessage();
				msg.setId(uuid);
				msg.setKey(key);
				msg.setData("点播未找到");
				resultHolder.invokeAllResult(msg);
				storager.stopPlay(deviceId, channelId);
			}else {
				redisCatchStorage.stopPlay(streamInfo);
				storager.stopPlay(streamInfo.getDeviceID(), streamInfo.getChannelId());
				RequestMessage msg = new RequestMessage();
				msg.setId(uuid);
				msg.setKey(key);
				//Response response = event.getResponse();
				msg.setData(String.format("success"));
				resultHolder.invokeAllResult(msg);
			}
			mediaServerService.closeRTPServer(device, channelId);
		});

		if (deviceId != null || channelId != null) {
			JSONObject json = new JSONObject();
			json.put("deviceId", deviceId);
			json.put("channelId", channelId);
			RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
			msg.setKey(key);
			msg.setData(json.toString());
			resultHolder.invokeAllResult(msg);
		} else {
			logger.warn("设备预览/回放停止API调用失败！");
			RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
			msg.setKey(key);
			msg.setData("streamId null");
			resultHolder.invokeAllResult(msg);
		}

		// 超时处理
		result.onTimeout(()->{
			logger.warn(String.format("设备预览/回放停止超时，deviceId/channelId：%s_%s ", deviceId, channelId));
			RequestMessage msg = new RequestMessage();
			msg.setId(uuid);
			msg.setKey(key);
			msg.setData("Timeout");
			resultHolder.invokeAllResult(msg);
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
			streamInfo = redisCatchStorage.queryPlaybackByStreamId(streamId);
		}
		if (streamInfo == null) {
			logger.warn("视频转码API调用失败！, 视频流已经停止!");
			return new ResponseEntity<String>("未找到视频流信息, 视频流可能已经停止", HttpStatus.OK);
		}
		MediaServerItem mediaInfo = mediaServerService.getOne(streamInfo.getMediaServerId());
		JSONObject rtpInfo = zlmresTfulUtils.getRtpInfo(mediaInfo, streamId);
		if (!rtpInfo.getBoolean("exist")) {
			logger.warn("视频转码API调用失败！, 视频流已停止推流!");
			return new ResponseEntity<String>("推流信息在流媒体中不存在, 视频流可能已停止推流", HttpStatus.OK);
		} else {
			String dstUrl = String.format("rtmp://%s:%s/convert/%s", "127.0.0.1", mediaInfo.getRtmpPort(),
					streamId );
			String srcUrl = String.format("rtsp://%s:%s/rtp/%s", "127.0.0.1", mediaInfo.getRtspPort(), streamId);
			JSONObject jsonObject = zlmresTfulUtils.addFFmpegSource(mediaInfo, srcUrl, dstUrl, "1000000", true, false, null);
			logger.info(jsonObject.toJSONString());
			JSONObject result = new JSONObject();
			if (jsonObject != null && jsonObject.getInteger("code") == 0) {
				   result.put("code", 0);
				JSONObject data = jsonObject.getJSONObject("data");
				if (data != null) {
				   	result.put("key", data.getString("key"));
					StreamInfo streamInfoResult = mediaService.getStreamInfoByAppAndStreamWithCheck("convert", streamId, mediaInfo.getId());
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
	public ResponseEntity<String> playConvertStop(@PathVariable String key, String mediaServerId) {
		JSONObject result = new JSONObject();
		if (mediaServerId == null) {
			result.put("code", 400);
			result.put("msg", "mediaServerId is null");
			return new ResponseEntity<String>( result.toJSONString(), HttpStatus.BAD_REQUEST);
		}
		MediaServerItem mediaInfo = mediaServerService.getOne(mediaServerId);
		if (mediaInfo == null) {
			result.put("code", 0);
			result.put("msg", "使用的流媒体已经停止运行");
			return new ResponseEntity<String>( result.toJSONString(), HttpStatus.OK);
		}else {
			JSONObject jsonObject = zlmresTfulUtils.delFFmpegSource(mediaInfo, key);
			logger.info(jsonObject.toJSONString());
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
		DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String>>(3 * 1000L);
		String key  = DeferredResultHolder.CALLBACK_CMD_BROADCAST + deviceId;
		if (resultHolder.exist(key, null)) {
			result.setResult(new ResponseEntity<>("设备使用中",HttpStatus.OK));
			return result;
		}
		String uuid  = UUID.randomUUID().toString();
        if (device == null) {

			resultHolder.put(key, key,  result);
			RequestMessage msg = new RequestMessage();
			msg.setKey(key);
			msg.setId(uuid);
			JSONObject json = new JSONObject();
			json.put("DeviceID", deviceId);
			json.put("CmdType", "Broadcast");
			json.put("Result", "Failed");
			json.put("Description", "Device 不存在");
			msg.setData(json);
			resultHolder.invokeResult(msg);
			return result;
		}
		cmder.audioBroadcastCmd(device, (event) -> {
			RequestMessage msg = new RequestMessage();
			msg.setKey(key);
			msg.setId(uuid);
			JSONObject json = new JSONObject();
			json.put("DeviceID", deviceId);
			json.put("CmdType", "Broadcast");
			json.put("Result", "Failed");
			json.put("Description", String.format("语音广播操作失败，错误码： %s, %s", event.statusCode, event.msg));
			msg.setData(json);
			resultHolder.invokeResult(msg);
		});

		result.onTimeout(() -> {
			logger.warn(String.format("语音广播操作超时, 设备未返回应答指令"));
			RequestMessage msg = new RequestMessage();
			msg.setKey(key);
			msg.setId(uuid);
			JSONObject json = new JSONObject();
			json.put("DeviceID", deviceId);
			json.put("CmdType", "Broadcast");
			json.put("Result", "Failed");
			json.put("Error", "Timeout. Device did not response to broadcast command.");
			msg.setData(json);
			resultHolder.invokeResult(msg);
		});
		resultHolder.put(key, uuid, result);
		return result;
	}

	@ApiOperation("获取所有的ssrc")
	@GetMapping("/ssrc")
	public WVPResult<JSONObject> getSSRC() {
		if (logger.isDebugEnabled()) {
			logger.debug("获取所有的ssrc");
		}
		JSONArray objects = new JSONArray();
		List<SsrcTransaction> allSsrc = streamSession.getAllSsrc();
		for (SsrcTransaction transaction : allSsrc) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("deviceId", transaction.getDeviceId());
			jsonObject.put("channelId", transaction.getChannelId());
			jsonObject.put("ssrc", transaction.getSsrc());
			jsonObject.put("streamId", transaction.getStreamId());
			objects.add(jsonObject);
		}

		WVPResult<JSONObject> result = new WVPResult<>();
		result.setCode(0);
		result.setMsg("success");
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("data", objects);
		jsonObject.put("count", objects.size());
		result.setData(jsonObject);
		return result;
	}

}

