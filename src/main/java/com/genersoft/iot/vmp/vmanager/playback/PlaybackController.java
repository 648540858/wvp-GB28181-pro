package com.genersoft.iot.vmp.vmanager.playback;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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
	private ZLMRESTfulUtils zlmresTfulUtils;

	@Value("${media.closeWaitRTPInfo}")
	private boolean closeWaitRTPInfo;

	@GetMapping("/playback/{deviceId}/{channelId}")
	public ResponseEntity<String> play(@PathVariable String deviceId, @PathVariable String channelId, String startTime,
			String endTime) {

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("设备回放 API调用，deviceId：%s ，channelId：%s", deviceId, channelId));
		}

		if (StringUtils.isEmpty(deviceId) || StringUtils.isEmpty(channelId)) {
			String log = String.format("设备回放 API调用失败，deviceId：%s ，channelId：%s", deviceId, channelId);
			logger.warn(log);
			return new ResponseEntity<String>(log, HttpStatus.BAD_REQUEST);
		}

		Device device = storager.queryVideoDevice(deviceId);
		StreamInfo streamInfo = storager.queryPlaybackByDevice(deviceId, channelId);

		if (streamInfo != null) {
			cmder.streamByeCmd(streamInfo.getSsrc());
		}

		// }else {
		// String streamId = String.format("%08x",
		// Integer.parseInt(streamInfo.getSsrc())).toUpperCase();
		// JSONObject rtpInfo = zlmresTfulUtils.getRtpInfo(streamId);
		// if (rtpInfo.getBoolean("exist")) {
		// return new
		// ResponseEntity<String>(JSON.toJSONString(streamInfo),HttpStatus.OK);
		// }else {
		// storager.stopPlayback(streamInfo);
		// streamInfo = cmder.playbackStreamCmd(device, channelId, startTime, endTime);
		// }
		// }
		streamInfo = cmder.playbackStreamCmd(device, channelId, startTime, endTime);

		String streamId = String.format("%08x", Integer.parseInt(streamInfo.getSsrc())).toUpperCase();

		if (logger.isDebugEnabled()) {
			logger.debug("设备回放 API调用，ssrc：" + streamInfo.getSsrc() + ",ZLMedia streamId:" + streamId);
		}
		// 等待推流, TODO 默认超时15s
		boolean lockFlag = true;
		boolean rtpPushed = false;
		long lockStartTime = System.currentTimeMillis();
		JSONObject rtpInfo = null;

		if (closeWaitRTPInfo) {
			String flv = storager.getMediaInfo().getWanIp() + ":" + storager.getMediaInfo().getHttpPort() + "/rtp/"
					+ streamId + ".flv";
			streamInfo.setFlv("http://" + flv);
			streamInfo.setWs_flv("ws://" + flv);
			storager.startPlayback(streamInfo);
		} else {
			while (lockFlag) {
				try {
					if (System.currentTimeMillis() - lockStartTime > 75 * 1000) {
						storager.stopPlayback(streamInfo);
						logger.info("播放等待超时");
						return new ResponseEntity<String>("timeout", HttpStatus.OK);
					} else {
						streamInfo = storager.queryPlaybackByDevice(deviceId, channelId);
						if (!rtpPushed) {
							logger.info("查询RTP推流信息...");
							rtpInfo = zlmresTfulUtils.getRtpInfo(streamId);
						}
						if (rtpInfo != null && rtpInfo.getBoolean("exist") && streamInfo != null
								&& streamInfo.getFlv() != null) {
							logger.info("查询流编码信息：" + streamInfo.getFlv());
							rtpPushed = true;
							Thread.sleep(2000);
							JSONObject mediaInfo = zlmresTfulUtils.getMediaInfo("rtp", "rtmp", streamId);
							if (mediaInfo.getInteger("code") == 0 && mediaInfo.getBoolean("online")) {
								lockFlag = false;
								logger.info("流编码信息已获取");
								JSONArray tracks = mediaInfo.getJSONArray("tracks");
								streamInfo.setTracks(tracks);
								storager.startPlayback(streamInfo);
							} else {
								logger.info("流编码信息未获取，2秒后重试...");
							}
						} else {
							Thread.sleep(2000);
							continue;
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		if (streamInfo != null) {
			return new ResponseEntity<String>(JSON.toJSONString(streamInfo), HttpStatus.OK);
		} else {
			logger.warn("设备回放API调用失败！");
			return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
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
