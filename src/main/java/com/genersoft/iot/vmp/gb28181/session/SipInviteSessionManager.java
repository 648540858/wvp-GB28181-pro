package com.genersoft.iot.vmp.gb28181.session;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 视频流session管理器，管理视频预览、预览回放的通信句柄
 */
@Component
public class SipInviteSessionManager {

	@Autowired
	private UserSetting userSetting;

	@Autowired
	private RedisTemplate<Object, Object> redisTemplate;

	/**
	 * 添加一个点播/回放的事务信息
	 */
	public void put(SsrcTransaction ssrcTransaction){
		redisTemplate.opsForHash().put(VideoManagerConstants.SIP_INVITE_SESSION_STREAM + userSetting.getServerId()
				, ssrcTransaction.getApp() + ssrcTransaction.getStream(), ssrcTransaction);

		redisTemplate.opsForHash().put(VideoManagerConstants.SIP_INVITE_SESSION_CALL_ID + userSetting.getServerId()
				, ssrcTransaction.getCallId(), ssrcTransaction);
	}

	public SsrcTransaction getSsrcTransactionByStream(String app, String stream){
		String key = VideoManagerConstants.SIP_INVITE_SESSION_STREAM + userSetting.getServerId();
		return (SsrcTransaction)redisTemplate.opsForHash().get(key, app + stream);
	}

	public SsrcTransaction getSsrcTransactionByCallId(String callId){
		String key = VideoManagerConstants.SIP_INVITE_SESSION_CALL_ID + userSetting.getServerId();
		return (SsrcTransaction)redisTemplate.opsForHash().get(key, callId);
	}

	public List<SsrcTransaction> getSsrcTransactionByDeviceId(String deviceId){
		String key = VideoManagerConstants.SIP_INVITE_SESSION_CALL_ID + userSetting.getServerId();
		List<Object> values = redisTemplate.opsForHash().values(key);
		List<SsrcTransaction> result = new ArrayList<>();
		for (Object value : values) {
			SsrcTransaction ssrcTransaction = (SsrcTransaction) value;
			if (ssrcTransaction != null && deviceId.equals(ssrcTransaction.getDeviceId())) {
				result.add(ssrcTransaction);
			}
		}
		return result;
	}
	
	public void removeByStream(String app, String stream) {
		SsrcTransaction ssrcTransaction = getSsrcTransactionByStream(app, stream);
		if (ssrcTransaction == null ) {
			return;
		}
		redisTemplate.opsForHash().delete(VideoManagerConstants.SIP_INVITE_SESSION_STREAM + userSetting.getServerId(), app + stream);
		if (ssrcTransaction.getCallId() != null) {
			redisTemplate.opsForHash().delete(VideoManagerConstants.SIP_INVITE_SESSION_CALL_ID + userSetting.getServerId(), ssrcTransaction.getCallId());
		}
	}

	public void removeByCallId(String callId) {
		SsrcTransaction ssrcTransaction = getSsrcTransactionByCallId(callId);
		if (ssrcTransaction == null ) {
			return;
		}
		redisTemplate.opsForHash().delete(VideoManagerConstants.SIP_INVITE_SESSION_CALL_ID + userSetting.getServerId(), callId);
		if (ssrcTransaction.getStream() != null) {
			redisTemplate.opsForHash().delete(VideoManagerConstants.SIP_INVITE_SESSION_STREAM + userSetting.getServerId(), ssrcTransaction.getApp() + ssrcTransaction.getStream());
		}
	}

	public List<SsrcTransaction> getAll() {
		String key = VideoManagerConstants.SIP_INVITE_SESSION_CALL_ID + userSetting.getServerId();
		List<Object> values = redisTemplate.opsForHash().values(key);
		List<SsrcTransaction> result = new ArrayList<>();
		for (Object value : values) {
			result.add((SsrcTransaction) value);
		}
		return result;
	}
}
