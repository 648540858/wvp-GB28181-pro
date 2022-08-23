package com.genersoft.iot.vmp.vmanager.gb28181.play;

import com.alibaba.fastjson.JSONArray;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.vmanager.bean.AudioBroadcastResult;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.genersoft.iot.vmp.vmanager.gb28181.play.bean.PlayResult;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.service.IPlayService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.UUID;


/**
 * @author lin
 */
@Tag(name  = "国标设备点播")
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
	private IVideoManagerStorage storager;

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

	@Operation(summary = "开始点播")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@GetMapping("/start/{deviceId}/{channelId}")
	public DeferredResult<WVPResult<String>> play(@PathVariable String deviceId,
													   @PathVariable String channelId) {

		// 获取可用的zlm
		Device device = storager.queryVideoDevice(deviceId);
		MediaServerItem newMediaServerItem = playService.getNewMediaServerItem(device);
		PlayResult playResult = playService.play(newMediaServerItem, deviceId, channelId, null, null, null);

		return playResult.getResult();
	}


	@Operation(summary = "停止点播")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@GetMapping("/stop/{deviceId}/{channelId}")
	public JSONObject playStop(@PathVariable String deviceId, @PathVariable String channelId) {

		logger.debug(String.format("设备预览/回放停止API调用，streamId：%s_%s", deviceId, channelId ));

		if (deviceId == null || channelId == null) {
			throw new ControllerException(ErrorCode.ERROR400);
		}

		StreamInfo streamInfo = redisCatchStorage.queryPlayByDevice(deviceId, channelId);
		if (streamInfo == null) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "点播未找到");
		}

		cmder.streamByeCmd(deviceId, channelId, streamInfo.getStream(), null, null);
		redisCatchStorage.stopPlay(streamInfo);

		storager.stopPlay(streamInfo.getDeviceID(), streamInfo.getChannelId());
		JSONObject json = new JSONObject();
		json.put("deviceId", deviceId);
		json.put("channelId", channelId);
		return json;

	}

	/**
	 * 将不是h264的视频通过ffmpeg 转码为h264 + aac
	 * @param streamId 流ID
	 */
	@Operation(summary = "将不是h264的视频通过ffmpeg 转码为h264 + aac")
	@Parameter(name = "streamId", description = "视频流ID", required = true)
	@PostMapping("/convert/{streamId}")
	public JSONObject playConvert(@PathVariable String streamId) {
		StreamInfo streamInfo = redisCatchStorage.queryPlayByStreamId(streamId);
		if (streamInfo == null) {
			streamInfo = redisCatchStorage.queryPlayback(null, null, streamId, null);
		}
		if (streamInfo == null) {
			logger.warn("视频转码API调用失败！, 视频流已经停止!");
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到视频流信息, 视频流可能已经停止");
		}
		MediaServerItem mediaInfo = mediaServerService.getOne(streamInfo.getMediaServerId());
		JSONObject rtpInfo = zlmresTfulUtils.getRtpInfo(mediaInfo, streamId);
		if (!rtpInfo.getBoolean("exist")) {
			logger.warn("视频转码API调用失败！, 视频流已停止推流!");
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到视频流信息, 视频流可能已停止推流");
		} else {
			String dstUrl = String.format("rtmp://%s:%s/convert/%s", "127.0.0.1", mediaInfo.getRtmpPort(),
					streamId );
			String srcUrl = String.format("rtsp://%s:%s/rtp/%s", "127.0.0.1", mediaInfo.getRtspPort(), streamId);
			JSONObject jsonObject = zlmresTfulUtils.addFFmpegSource(mediaInfo, srcUrl, dstUrl, "1000000", true, false, null);
			logger.info(jsonObject.toJSONString());
			if (jsonObject != null && jsonObject.getInteger("code") == 0) {
				JSONObject data = jsonObject.getJSONObject("data");
				if (data != null) {
					JSONObject result = new JSONObject();
					result.put("key", data.getString("key"));
					StreamInfo streamInfoResult = mediaService.getStreamInfoByAppAndStreamWithCheck("convert", streamId, mediaInfo.getId(), false);
					result.put("StreamInfo", streamInfoResult);
					return result;
				}else {
					throw new ControllerException(ErrorCode.ERROR100.getCode(), "转码失败");
				}
			}else {
				throw new ControllerException(ErrorCode.ERROR100.getCode(), "转码失败");
			}
		}
	}

	/**
	 * 结束转码
	 */
	@Operation(summary = "结束转码")
	@Parameter(name = "key", description = "视频流key", required = true)
	@Parameter(name = "mediaServerId", description = "流媒体服务ID", required = true)
	@PostMapping("/convertStop/{key}")
	public void playConvertStop(@PathVariable String key, String mediaServerId) {
		if (mediaServerId == null) {
			throw new ControllerException(ErrorCode.ERROR400.getCode(), "流媒体：" + mediaServerId + "不存在" );
		}
		MediaServerItem mediaInfo = mediaServerService.getOne(mediaServerId);
		if (mediaInfo == null) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "使用的流媒体已经停止运行" );
		}else {
			JSONObject jsonObject = zlmresTfulUtils.delFFmpegSource(mediaInfo, key);
			logger.info(jsonObject.toJSONString());
			if (jsonObject != null && jsonObject.getInteger("code") == 0) {
				JSONObject data = jsonObject.getJSONObject("data");
				if (data == null || data.getBoolean("flag") == null || !data.getBoolean("flag")) {
					throw new ControllerException(ErrorCode.ERROR100 );
				}
			}else {
				throw new ControllerException(ErrorCode.ERROR100 );
			}
		}
	}

	@Operation(summary = "语音广播命令")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "deviceId", description = "通道国标编号", required = true)
	@Parameter(name = "timeout", description = "推流超时时间(秒)", required = true)
	@GetMapping("/broadcast/{deviceId}/{channelId}")
	@PostMapping("/broadcast/{deviceId}/{channelId}")
    public DeferredResult<WVPResult<AudioBroadcastResult>> broadcastApi(@PathVariable String deviceId, @PathVariable String channelId, Integer timeout) {
        if (logger.isDebugEnabled()) {
            logger.debug("语音广播API调用");
        }
        Device device = storager.queryVideoDevice(deviceId);
		if (device == null) {
			WVPResult<AudioBroadcastResult> result = new WVPResult<>();
			result.setCode(-1);
			result.setMsg("未找到设备： " + deviceId);
			DeferredResult<WVPResult<AudioBroadcastResult>> deferredResult = new DeferredResult<>();
			deferredResult.setResult(result);
			return deferredResult;
		}
		if (channelId == null) {
			WVPResult<AudioBroadcastResult> result = new WVPResult<>();
			result.setCode(-1);
			result.setMsg("未找到通道： " + channelId);
			DeferredResult<WVPResult<AudioBroadcastResult>> deferredResult = new DeferredResult<>();
			deferredResult.setResult(result);
			return deferredResult;
		}

		String key = DeferredResultHolder.CALLBACK_CMD_BROADCAST + deviceId;
		if (resultHolder.exist(key, null)) {
			WVPResult<AudioBroadcastResult> wvpResult = new WVPResult<>();
			wvpResult.setCode(-1);
			wvpResult.setMsg("设备使用中");
			DeferredResult<WVPResult<AudioBroadcastResult>> deferredResult = new DeferredResult<>();
			deferredResult.setResult(wvpResult);
			return deferredResult;
		}
		if (timeout == null){
			timeout = 30;
		}
		DeferredResult<WVPResult<AudioBroadcastResult>> result = new DeferredResult<>(timeout.longValue()*1000 + 2000);
		String uuid  = UUID.randomUUID().toString();
		result.onTimeout(()->{
			WVPResult<AudioBroadcastResult> wvpResult = new WVPResult<>();
			wvpResult.setCode(-1);
			wvpResult.setMsg("请求超时");
			RequestMessage requestMessage = new RequestMessage();
			requestMessage.setKey(key);
			requestMessage.setData(wvpResult);
			resultHolder.invokeAllResult(requestMessage);
		});
		playService.audioBroadcast(device, channelId, timeout, (msg)->{
			WVPResult<AudioBroadcastResult> wvpResult = new WVPResult<>();
			wvpResult.setCode(-1);
			wvpResult.setMsg(msg);
			RequestMessage requestMessage = new RequestMessage();
			requestMessage.setKey(key);
			requestMessage.setData(wvpResult);
			resultHolder.invokeAllResult(requestMessage);
		});
		resultHolder.put(key, uuid, result);

		return result;
	}


	@Operation(summary = "停止语音广播")
	@Parameter(name = "deviceId", description = "设备Id", required = true)
	@Parameter(name = "channelId", description = "通道Id", required = true)
	@GetMapping("/broadcast/stop/{deviceId}/{channelId}")
	@PostMapping("/broadcast/stop/{deviceId}/{channelId}")
	public WVPResult<String> stopBroadcastA(@PathVariable String deviceId, @PathVariable String channelId) {
		if (logger.isDebugEnabled()) {
			logger.debug("停止语音广播API调用");
		}
		playService.stopAudioBroadcast(deviceId, channelId);
		return new WVPResult<>(0, "success", null);
	}

	@Operation(summary = "获取所有的ssrc")
	@GetMapping("/ssrc")
	public JSONObject getSSRC() {
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
			jsonObject.put("streamId", transaction.getStream());
			objects.add(jsonObject);
		}

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("data", objects);
		jsonObject.put("count", objects.size());
		return jsonObject;
	}

}

