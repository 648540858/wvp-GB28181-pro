package com.genersoft.iot.vmp.gb28181.session;

import java.util.concurrent.ConcurrentHashMap;

import javax.sip.ClientTransaction;

import org.springframework.stereotype.Component;

/**    
 * @Description:视频流session管理器，管理视频预览、预览回放的通信句柄 
 * @author: swwheihei
 * @date:   2020年5月13日 下午4:03:02     
 */
@Component
public class VideoStreamSessionManager {

	private ConcurrentHashMap<String, ClientTransaction> sessionMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, String> ssrcMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, String> streamIdMap = new ConcurrentHashMap<>();

	public String createPlaySsrc(){
		return SsrcUtil.getPlaySsrc();
	}
	
	public String createPlayBackSsrc(){
		return SsrcUtil.getPlayBackSsrc();
	}
	
	public void put(String deviceId, String channelId ,String ssrc, String streamId, ClientTransaction transaction){
		sessionMap.put(deviceId + "_" + channelId, transaction);
		ssrcMap.put(deviceId + "_" + channelId, ssrc);
		streamIdMap.put(deviceId + "_" + channelId, streamId);
	}
	
	public ClientTransaction getTransaction(String deviceId, String channelId){
		return sessionMap.get(deviceId + "_" + channelId);
	}

	public String getStreamId(String deviceId, String channelId){
		return streamIdMap.get(deviceId + "_" + channelId);
	}
	
	public void remove(String deviceId, String channelId) {
		sessionMap.remove(deviceId + "_" + channelId);
		if (ssrcMap.get(deviceId + "_" + channelId) != null) {
			SsrcUtil.releaseSsrc(ssrcMap.get(deviceId + "_" + channelId));
		}
		ssrcMap.remove(deviceId + "_" + channelId);
		streamIdMap.remove(deviceId + "_" + channelId);
	}

	public ConcurrentHashMap<String, ClientTransaction> getSessionMap() {
		return sessionMap;
	}

	public ConcurrentHashMap<String, String> getSsrcMap() {
		return ssrcMap;
	}

	public ConcurrentHashMap<String, String> getStreamIdMap() {
		return streamIdMap;
	}
}
