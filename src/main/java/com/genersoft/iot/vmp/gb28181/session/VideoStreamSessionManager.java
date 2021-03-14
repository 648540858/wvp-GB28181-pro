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

	public String createPlaySsrc(){
		return SsrcUtil.getPlaySsrc();
	}
	
	public String createPlayBackSsrc(){
		return SsrcUtil.getPlayBackSsrc();
	}
	
	public void put(String streamId,String ssrc,ClientTransaction transaction){
		sessionMap.put(streamId, transaction);
		ssrcMap.put(streamId, ssrc);
	}
	
	public ClientTransaction get(String streamId){
		return sessionMap.get(streamId);
	}
	
	public void remove(String streamId) {
		sessionMap.remove(streamId);
		SsrcUtil.releaseSsrc(ssrcMap.get(streamId));
		ssrcMap.remove(streamId);
	}
}
