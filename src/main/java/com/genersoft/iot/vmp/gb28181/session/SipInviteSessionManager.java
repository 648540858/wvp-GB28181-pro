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
	private RedisTemplate<String, Object> redisTemplate;

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
			// stream 索引在同一通道重新点播时会被新事务覆盖，这里必须确认索引仍然指向当前事务再删除，
			// 否则清理旧 Call-ID 会顺带删掉新 Dialog 的 stream 索引，导致后续 stop 找不到事务、无法发送 BYE。
			SsrcTransaction indexed = getSsrcTransactionByStream(ssrcTransaction.getApp(), ssrcTransaction.getStream());
			if (isStreamIndexOwnedBy(indexed, callId)) {
				redisTemplate.opsForHash().delete(VideoManagerConstants.SIP_INVITE_SESSION_STREAM + userSetting.getServerId(), ssrcTransaction.getApp() + ssrcTransaction.getStream());
			}
		}
	}

	/**
	 * 判断 stream 索引当前是否仍属于指定的事务(Call-ID)
	 */
	static boolean isStreamIndexOwnedBy(SsrcTransaction indexed, String callId) {
		return indexed != null && callId != null && callId.equals(indexed.getCallId());
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
