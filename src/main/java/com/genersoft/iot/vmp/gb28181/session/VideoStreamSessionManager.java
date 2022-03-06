package com.genersoft.iot.vmp.gb28181.session;

import java.util.ArrayList;
import java.util.List;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.UserSetup;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import com.genersoft.iot.vmp.utils.SerializeUtils;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import gov.nist.javax.sip.stack.SIPDialog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**    
 * @description:视频流session管理器，管理视频预览、预览回放的通信句柄 
 * @author: swwheihei
 * @date:   2020年5月13日 下午4:03:02     
 */
@Component
public class VideoStreamSessionManager {

	@Autowired
	private RedisUtil redisUtil;

	@Autowired
	private UserSetup userSetup;

	/**
	 * 添加一个点播/回放的事务信息
	 * 后续可以通过流Id/callID
	 * @param deviceId 设备ID
	 * @param channelId 通道ID
	 * @param callId 一次请求的CallID
	 * @param stream 流名称
	 * @param mediaServerId 所使用的流媒体ID
	 * @param transaction 事务
	 */
	public void put(String deviceId, String channelId, String callId, String stream, String ssrc, String mediaServerId, ClientTransaction transaction){
		SsrcTransaction ssrcTransaction = new SsrcTransaction();
		ssrcTransaction.setDeviceId(deviceId);
		ssrcTransaction.setChannelId(channelId);
		ssrcTransaction.setStream(stream);
		byte[] transactionByteArray = SerializeUtils.serialize(transaction);
		ssrcTransaction.setTransaction(transactionByteArray);
		ssrcTransaction.setCallId(callId);
		ssrcTransaction.setSsrc(ssrc);
		ssrcTransaction.setMediaServerId(mediaServerId);

		redisUtil.set(VideoManagerConstants.MEDIA_TRANSACTION_USED_PREFIX + userSetup.getServerId()
				+ "_" +  deviceId + "_" + channelId + "_" + callId + "_" + stream, ssrcTransaction);
		redisUtil.set(VideoManagerConstants.MEDIA_TRANSACTION_USED_PREFIX + userSetup.getServerId()
				+ "_" +  deviceId + "_" + channelId + "_" + callId + "_" + stream, ssrcTransaction);
	}

	public void put(String deviceId, String channelId, String callId, Dialog dialog){
		SsrcTransaction ssrcTransaction = getSsrcTransaction(deviceId, channelId, callId, null);
		if (ssrcTransaction != null) {
			byte[] dialogByteArray = SerializeUtils.serialize(dialog);
			ssrcTransaction.setDialog(dialogByteArray);
		}
		redisUtil.set(VideoManagerConstants.MEDIA_TRANSACTION_USED_PREFIX + userSetup.getServerId()
				+ "_" +  deviceId + "_" + channelId + "_" + ssrcTransaction.getCallId() + "_"
				+ ssrcTransaction.getStream(), ssrcTransaction);
	}

	
	public ClientTransaction getTransactionByStream(String deviceId, String channelId, String stream){
		SsrcTransaction ssrcTransaction = getSsrcTransaction(deviceId, channelId, null, stream);
		if (ssrcTransaction == null) return null;
		byte[] transactionByteArray = ssrcTransaction.getTransaction();
		ClientTransaction clientTransaction = (ClientTransaction)SerializeUtils.deSerialize(transactionByteArray);
		return clientTransaction;
	}

	public SIPDialog getDialogByStream(String deviceId, String channelId, String stream){
		SsrcTransaction ssrcTransaction = getSsrcTransaction(deviceId, channelId, null, stream);
		if (ssrcTransaction == null) return null;
		byte[] dialogByteArray = ssrcTransaction.getDialog();
		if (dialogByteArray == null) return null;
		SIPDialog dialog = (SIPDialog)SerializeUtils.deSerialize(dialogByteArray);
		return dialog;
	}

	public SIPDialog getDialogByCallId(String deviceId, String channelId, String callID){
		SsrcTransaction ssrcTransaction = getSsrcTransaction(deviceId, channelId, callID, null);
		if (ssrcTransaction == null) return null;
		byte[] dialogByteArray = ssrcTransaction.getDialog();
		if (dialogByteArray == null) return null;
		SIPDialog dialog = (SIPDialog)SerializeUtils.deSerialize(dialogByteArray);
		return dialog;
	}

	public SsrcTransaction getSsrcTransaction(String deviceId, String channelId, String callId, String stream){
		if (StringUtils.isEmpty(callId)) callId ="*";
		if (StringUtils.isEmpty(stream)) stream ="*";
		String key = VideoManagerConstants.MEDIA_TRANSACTION_USED_PREFIX + userSetup.getServerId() + "_" + deviceId + "_" + channelId + "_" + callId+ "_" + stream;
		List<Object> scanResult = redisUtil.scan(key);
		if (scanResult.size() == 0) return null;
		return (SsrcTransaction)redisUtil.get((String) scanResult.get(0));
	}

	public List<SsrcTransaction> getSsrcTransactionForAll(String deviceId, String channelId, String callId, String stream){
		if (StringUtils.isEmpty(deviceId)) deviceId ="*";
		if (StringUtils.isEmpty(channelId)) channelId ="*";
		if (StringUtils.isEmpty(callId)) callId ="*";
		if (StringUtils.isEmpty(stream)) stream ="*";
		String key = VideoManagerConstants.MEDIA_TRANSACTION_USED_PREFIX + userSetup.getServerId() + "_" + deviceId + "_" + channelId + "_" + callId+ "_" + stream;
		List<Object> scanResult = redisUtil.scan(key);
		if (scanResult.size() == 0) return null;
		List<SsrcTransaction> result = new ArrayList<>();
		for (Object keyObj : scanResult) {
			result.add((SsrcTransaction)redisUtil.get((String) keyObj));
		}
		return result;
	}

	public String getMediaServerId(String deviceId, String channelId, String stream){
		SsrcTransaction ssrcTransaction = getSsrcTransaction(deviceId, channelId, null, stream);
		if (ssrcTransaction == null) return null;
		return ssrcTransaction.getMediaServerId();
	}

	public String getSSRC(String deviceId, String channelId, String stream){
		SsrcTransaction ssrcTransaction = getSsrcTransaction(deviceId, channelId, null, stream);
		if (ssrcTransaction == null) return null;
		return ssrcTransaction.getSsrc();
	}
	
	public void remove(String deviceId, String channelId, String stream) {
		SsrcTransaction ssrcTransaction = getSsrcTransaction(deviceId, channelId, null, stream);
		if (ssrcTransaction == null) return;
		redisUtil.del(VideoManagerConstants.MEDIA_TRANSACTION_USED_PREFIX + userSetup.getServerId() + "_"
				+  deviceId + "_" + channelId + "_" + ssrcTransaction.getCallId() + "_" + ssrcTransaction.getStream());
	}


	public List<SsrcTransaction> getAllSsrc() {
		List<Object> ssrcTransactionKeys = redisUtil.scan(String.format("%s_*_*_*_*", VideoManagerConstants.MEDIA_TRANSACTION_USED_PREFIX+ userSetup.getServerId() + "_" ));
		List<SsrcTransaction> result= new ArrayList<>();
		for (int i = 0; i < ssrcTransactionKeys.size(); i++) {
			String key = (String)ssrcTransactionKeys.get(i);
			SsrcTransaction ssrcTransaction = (SsrcTransaction)redisUtil.get(key);
			result.add(ssrcTransaction);
		}
		return result;
	}
}
