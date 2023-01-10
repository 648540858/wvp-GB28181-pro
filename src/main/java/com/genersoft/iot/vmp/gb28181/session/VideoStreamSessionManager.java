package com.genersoft.iot.vmp.gb28181.session;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import gov.nist.javax.sip.message.SIPResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

/**    
 * @description:视频流session管理器，管理视频预览、预览回放的通信句柄 
 * @author: swwheihei
 * @date:   2020年5月13日 下午4:03:02     
 */
@Component
public class VideoStreamSessionManager {

	@Autowired
	private UserSetting userSetting;

	public enum SessionType {
		play,
		playback,
		download
	}

	/**
	 * 添加一个点播/回放的事务信息
	 * 后续可以通过流Id/callID
	 * @param deviceId 设备ID
	 * @param channelId 通道ID
	 * @param callId 一次请求的CallID
	 * @param stream 流名称
	 * @param mediaServerId 所使用的流媒体ID
	 * @param response 回复
	 */
	public void put(String deviceId, String channelId, String callId, String stream, String ssrc, String mediaServerId, SIPResponse response, SessionType type){
		SsrcTransaction ssrcTransaction = new SsrcTransaction();
		ssrcTransaction.setDeviceId(deviceId);
		ssrcTransaction.setChannelId(channelId);
		ssrcTransaction.setStream(stream);
		ssrcTransaction.setSipTransactionInfo(new SipTransactionInfo(response));
		ssrcTransaction.setCallId(callId);
		ssrcTransaction.setSsrc(ssrc);
		ssrcTransaction.setMediaServerId(mediaServerId);
		ssrcTransaction.setType(type);

		RedisUtil.set(VideoManagerConstants.MEDIA_TRANSACTION_USED_PREFIX + userSetting.getServerId()
				+ "_" +  deviceId + "_" + channelId + "_" + callId + "_" + stream, ssrcTransaction);
	}

	public SsrcTransaction getSsrcTransaction(String deviceId, String channelId, String callId, String stream){

		if (ObjectUtils.isEmpty(deviceId)) {
			deviceId ="*";
		}
		if (ObjectUtils.isEmpty(channelId)) {
			channelId ="*";
		}
		if (ObjectUtils.isEmpty(callId)) {
			callId ="*";
		}
		if (ObjectUtils.isEmpty(stream)) {
			stream ="*";
		}
		String key = VideoManagerConstants.MEDIA_TRANSACTION_USED_PREFIX + userSetting.getServerId() + "_" + deviceId + "_" + channelId + "_" + callId+ "_" + stream;
		List<Object> scanResult = RedisUtil.scan(key, 1);
		if (scanResult.size() == 0) {
			return null;
		}
		return (SsrcTransaction)RedisUtil.get((String) scanResult.get(0));
	}

	public List<SsrcTransaction> getSsrcTransactionForAll(String deviceId, String channelId, String callId, String stream){
		if (ObjectUtils.isEmpty(deviceId)) {
			deviceId ="*";
		}
		if (ObjectUtils.isEmpty(channelId)) {
			channelId ="*";
		}
		if (ObjectUtils.isEmpty(callId)) {
			callId ="*";
		}
		if (ObjectUtils.isEmpty(stream)) {
			stream ="*";
		}
		String key = VideoManagerConstants.MEDIA_TRANSACTION_USED_PREFIX + userSetting.getServerId() + "_" + deviceId + "_" + channelId + "_" + callId+ "_" + stream;
		List<Object> scanResult = RedisUtil.scan(key);
		if (scanResult.size() == 0) {
			return null;
		}
		List<SsrcTransaction> result = new ArrayList<>();
		for (Object keyObj : scanResult) {
			result.add((SsrcTransaction)RedisUtil.get((String) keyObj));
		}
		return result;
	}

	public String getMediaServerId(String deviceId, String channelId, String stream){
		SsrcTransaction ssrcTransaction = getSsrcTransaction(deviceId, channelId, null, stream);
		if (ssrcTransaction == null) {
			return null;
		}
		return ssrcTransaction.getMediaServerId();
	}

	public String getSSRC(String deviceId, String channelId, String stream){
		SsrcTransaction ssrcTransaction = getSsrcTransaction(deviceId, channelId, null, stream);
		if (ssrcTransaction == null) {
			return null;
		}
		return ssrcTransaction.getSsrc();
	}
	
	public void remove(String deviceId, String channelId, String stream) {
		SsrcTransaction ssrcTransaction = getSsrcTransaction(deviceId, channelId, null, stream);
		if (ssrcTransaction == null) {
			return;
		}
		RedisUtil.del(VideoManagerConstants.MEDIA_TRANSACTION_USED_PREFIX + userSetting.getServerId() + "_"
				+  deviceId + "_" + channelId + "_" + ssrcTransaction.getCallId() + "_" + ssrcTransaction.getStream());
	}


	public List<SsrcTransaction> getAllSsrc() {
		List<Object> ssrcTransactionKeys = RedisUtil.scan(String.format("%s_*_*_*_*", VideoManagerConstants.MEDIA_TRANSACTION_USED_PREFIX+ userSetting.getServerId()));
		List<SsrcTransaction> result= new ArrayList<>();
		for (int i = 0; i < ssrcTransactionKeys.size(); i++) {
			String key = (String)ssrcTransactionKeys.get(i);
			SsrcTransaction ssrcTransaction = (SsrcTransaction)RedisUtil.get(key);
			result.add(ssrcTransaction);
		}
		return result;
	}
}
