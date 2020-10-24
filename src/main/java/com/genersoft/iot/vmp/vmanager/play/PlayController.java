package com.genersoft.iot.vmp.vmanager.play;

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
public class PlayController {
	
	private final static Logger logger = LoggerFactory.getLogger(PlayController.class);
	
	@Autowired
	private SIPCommander cmder;
	
	@Autowired
	private IVideoManagerStorager storager;

	@Autowired
	private ZLMRESTfulUtils zlmresTfulUtils;
	
	@GetMapping("/play/{deviceId}/{channelId}")
	public ResponseEntity<String> play(@PathVariable String deviceId,@PathVariable String channelId){
		
		Device device = storager.queryVideoDevice(deviceId);
		StreamInfo streamInfo = storager.queryPlayByDevice(deviceId, channelId);

		if (streamInfo == null) {
			streamInfo = cmder.playStreamCmd(device, channelId);
		}else {
			String streamId = String.format("%08x", Integer.parseInt(streamInfo.getSsrc())).toUpperCase();
			JSONObject rtpInfo = zlmresTfulUtils.getRtpInfo(streamId);
			if (rtpInfo.getBoolean("exist")) {
				return new ResponseEntity<String>(JSON.toJSONString(streamInfo),HttpStatus.OK);
			}else {
				storager.stopPlay(streamInfo);
				streamInfo = cmder.playStreamCmd(device, channelId);
			}

		}
		String streamId = String.format("%08x", Integer.parseInt(streamInfo.getSsrc())).toUpperCase();
		// 等待推流, TODO 默认超时30s
		boolean lockFlag = true;
		long startTime = System.currentTimeMillis();

		while (lockFlag) {
			try {
				if (System.currentTimeMillis() - startTime > 30 * 1000) {
					storager.stopPlay(streamInfo);
					return new ResponseEntity<String>("timeout",HttpStatus.OK);
				}else {
					streamInfo = storager.queryPlayByDevice(deviceId, channelId);
					JSONObject rtpInfo = zlmresTfulUtils.getRtpInfo(streamId);
					if (rtpInfo != null && rtpInfo.getBoolean("exist") && streamInfo.getFlv() != null){
						JSONObject mediaInfo = zlmresTfulUtils.getMediaInfo("rtp", "rtmp", streamId);
						if (mediaInfo.getInteger("code") == 0 && mediaInfo.getBoolean("online")) {
							lockFlag = false;
							JSONArray tracks = mediaInfo.getJSONArray("tracks");
							streamInfo.setTracks(tracks);
							storager.startPlay(streamInfo);
						}else {

						}
					}else {
						Thread.sleep(2000);
						continue;
					};
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("设备预览 API调用，deviceId：%s ，channelId：%s",deviceId, channelId));
			logger.debug("设备预览 API调用，ssrc："+streamInfo.getSsrc()+",ZLMedia streamId:"+Integer.toHexString(Integer.parseInt(streamInfo.getSsrc())));
		}
		
		if(streamInfo!=null) {
			return new ResponseEntity<String>(JSON.toJSONString(streamInfo),HttpStatus.OK);
		} else {
			logger.warn("设备预览API调用失败！");
			return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@PostMapping("/play/{ssrc}/stop")
	public ResponseEntity<String> playStop(@PathVariable String ssrc){
		
		cmder.streamByeCmd(ssrc);
		StreamInfo streamInfo = storager.queryPlayBySSRC(ssrc);
		if (streamInfo == null) return new ResponseEntity<String>(HttpStatus.PAYMENT_REQUIRED);
		storager.stopPlay(streamInfo);
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("设备预览停止API调用，ssrc：%s", ssrc));
		}
		
		if(ssrc!=null) {
			JSONObject json = new JSONObject();
			json.put("ssrc", ssrc);
			return new ResponseEntity<String>(json.toString(),HttpStatus.OK);
		} else {
			logger.warn("设备预览停止API调用失败！");
			return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
