package com.genersoft.iot.vmp.vmanager.play;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.MediaServerConfig;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.vmanager.service.IPlayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import org.springframework.web.context.request.async.DeferredResult;

import java.text.DecimalFormat;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/api")
public class PlayController {

	private final static Logger logger = LoggerFactory.getLogger(PlayController.class);

	@Autowired
	private SIPCommander cmder;

	@Autowired
	private IVideoManagerStorager storager;

	@Autowired
	private ZLMRESTfulUtils zlmresTfulUtils;

	@Autowired
	private DeferredResultHolder resultHolder;

	@Autowired
	private IPlayService playService;

	@GetMapping("/play/{deviceId}/{channelId}")
	public DeferredResult<ResponseEntity<String>> play(@PathVariable String deviceId,
													   @PathVariable String channelId) {


		Device device = storager.queryVideoDevice(deviceId);
		StreamInfo streamInfo = storager.queryPlayByDevice(deviceId, channelId);

		UUID uuid = UUID.randomUUID();
		DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String>>();
		// 超时处理
		result.onTimeout(()->{
			logger.warn(String.format("设备点播超时，deviceId：%s ，channelId：%s", deviceId, channelId));
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_PlAY + uuid);
			msg.setData("Timeout");
			resultHolder.invokeResult(msg);
		});
		// 录像查询以channelId作为deviceId查询
		resultHolder.put(DeferredResultHolder.CALLBACK_CMD_PlAY + uuid, result);

		if (streamInfo == null) {
			// TODO playStreamCmd 超时处理
			cmder.playStreamCmd(device, channelId, (JSONObject response) -> {
				logger.info("收到订阅消息： " + response.toJSONString());
				playService.onPublishHandlerForPlay(response, deviceId, channelId, uuid.toString());
			});
		} else {
			String streamId = String.format("%08x", Integer.parseInt(streamInfo.getSsrc())).toUpperCase();
			JSONObject rtpInfo = zlmresTfulUtils.getRtpInfo(streamId);
			if (rtpInfo.getBoolean("exist")) {
				RequestMessage msg = new RequestMessage();
				msg.setId(DeferredResultHolder.CALLBACK_CMD_PlAY + uuid);
				msg.setData(JSON.toJSONString(streamInfo));
				resultHolder.invokeResult(msg);
			} else {
				storager.stopPlay(streamInfo);
				// TODO playStreamCmd 超时处理
				cmder.playStreamCmd(device, channelId, (JSONObject response) -> {
					logger.info("收到订阅消息： " + response.toJSONString());
					playService.onPublishHandlerForPlay(response, deviceId, channelId, uuid.toString());
				});
			}
		}
		return result;
	}

	@PostMapping("/play/{ssrc}/stop")
	public ResponseEntity<String> playStop(@PathVariable String ssrc) {

		cmder.streamByeCmd(ssrc);
		StreamInfo streamInfo = storager.queryPlayBySSRC(ssrc);
		if (streamInfo == null)
			return new ResponseEntity<String>("ssrc not found", HttpStatus.OK);
		storager.stopPlay(streamInfo);
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("设备预览停止API调用，ssrc：%s", ssrc));
		}

		if (ssrc != null) {
			JSONObject json = new JSONObject();
			json.put("ssrc", ssrc);
			return new ResponseEntity<String>(json.toString(), HttpStatus.OK);
		} else {
			logger.warn("设备预览停止API调用失败！");
			return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 将不是h264的视频通过ffmpeg 转码为h264 + aac
	 * @param ssrc
	 * @return
	 */
	@PostMapping("/play/{ssrc}/convert")
	public ResponseEntity<String> playConvert(@PathVariable String ssrc) {
		StreamInfo streamInfo = storager.queryPlayBySSRC(ssrc);
		if (streamInfo == null) {
			logger.warn("视频转码API调用失败！, 视频流已经停止!");
			return new ResponseEntity<String>("未找到视频流信息, 视频流可能已经停止", HttpStatus.OK);
		}
		String streamId = String.format("%08x", Integer.parseInt(ssrc)).toUpperCase();
		JSONObject rtpInfo = zlmresTfulUtils.getRtpInfo(streamId);
		if (!rtpInfo.getBoolean("exist")) {
			logger.warn("视频转码API调用失败！, 视频流已停止推流!");
			return new ResponseEntity<String>("推流信息在流媒体中不存在, 视频流可能已停止推流", HttpStatus.OK);
		} else {
			MediaServerConfig mediaInfo = storager.getMediaInfo();
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
					StreamInfo streamInfoResult = new StreamInfo();
					streamInfoResult.setRtmp(dstUrl);
					streamInfoResult.setRtsp(String.format("rtsp://%s:%s/convert/%s", mediaInfo.getWanIp(), mediaInfo.getRtspPort(), streamId));
					streamInfoResult.setStreamId(streamId);
					streamInfoResult.setFlv(String.format("http://%s:%s/convert/%s.flv", mediaInfo.getWanIp(), mediaInfo.getHttpPort(), streamId));
					streamInfoResult.setWs_flv(String.format("ws://%s:%s/convert/%s.flv", mediaInfo.getWanIp(), mediaInfo.getHttpPort(), streamId));
					streamInfoResult.setHls(String.format("http://%s:%s/convert/%s/hls.m3u8", mediaInfo.getWanIp(), mediaInfo.getHttpPort(), streamId));
					streamInfoResult.setWs_hls(String.format("ws://%s:%s/convert/%s/hls.m3u8", mediaInfo.getWanIp(), mediaInfo.getHttpPort(), streamId));
					streamInfoResult.setFmp4(String.format("http://%s:%s/convert/%s.live.mp4", mediaInfo.getWanIp(), mediaInfo.getHttpPort(), streamId));
					streamInfoResult.setWs_fmp4(String.format("ws://%s:%s/convert/%s.live.mp4", mediaInfo.getWanIp(), mediaInfo.getHttpPort(), streamId));
					streamInfoResult.setTs(String.format("http://%s:%s/convert/%s.live.ts", mediaInfo.getWanIp(), mediaInfo.getHttpPort(), streamId));
					streamInfoResult.setWs_ts(String.format("ws://%s:%s/convert/%s.live.ts", mediaInfo.getWanIp(), mediaInfo.getHttpPort(), streamId));
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
	@PostMapping("/play/convert/stop/{key}")
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
}

