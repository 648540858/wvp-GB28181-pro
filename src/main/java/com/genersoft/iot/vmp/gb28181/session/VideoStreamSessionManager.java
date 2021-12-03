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

	public void put(String deviceId, String channelId ,String ssrc, String streamId, String mediaServerId, ClientTransaction transaction){
		SsrcTransaction ssrcTransaction = new SsrcTransaction();
		ssrcTransaction.setDeviceId(deviceId);
		ssrcTransaction.setChannelId(channelId);
		ssrcTransaction.setStreamId(streamId);
		byte[] transactionByteArray = SerializeUtils.serialize(transaction);
		ssrcTransaction.setTransaction(transactionByteArray);
		ssrcTransaction.setSsrc(ssrc);
		ssrcTransaction.setMediaServerId(mediaServerId);

		redisUtil.set(VideoManagerConstants.MEDIA_TRANSACTION_USED_PREFIX + userSetup.getServerId() + "_" +  deviceId + "_" + channelId, ssrcTransaction);
	}

	public void put(String deviceId, String channelId , Dialog dialog){
		SsrcTransaction ssrcTransaction = getSsrcTransaction(deviceId, channelId);
		if (ssrcTransaction != null) {
			byte[] dialogByteArray = SerializeUtils.serialize(dialog);
			ssrcTransaction.setDialog(dialogByteArray);
		}
		redisUtil.set(VideoManagerConstants.MEDIA_TRANSACTION_USED_PREFIX + userSetup.getServerId() + "_" +  deviceId + "_" + channelId, ssrcTransaction);
	}

	
	public ClientTransaction getTransaction(String deviceId, String channelId){
		SsrcTransaction ssrcTransaction = getSsrcTransaction(deviceId, channelId);
		if (ssrcTransaction == null) return null;
		byte[] transactionByteArray = ssrcTransaction.getTransaction();
		ClientTransaction clientTransaction = (ClientTransaction)SerializeUtils.deSerialize(transactionByteArray);
		return clientTransaction;
	}

	public SIPDialog getDialog(String deviceId, String channelId){
		SsrcTransaction ssrcTransaction = getSsrcTransaction(deviceId, channelId);
		if (ssrcTransaction == null) return null;
		byte[] dialogByteArray = ssrcTransaction.getDialog();
		if (dialogByteArray == null) return null;
		SIPDialog dialog = (SIPDialog)SerializeUtils.deSerialize(dialogByteArray);
		return dialog;
	}

	public SsrcTransaction getSsrcTransaction(String deviceId, String channelId){
		SsrcTransaction ssrcTransaction = (SsrcTransaction)redisUtil.get(VideoManagerConstants.MEDIA_TRANSACTION_USED_PREFIX + userSetup.getServerId() + "_" + deviceId + "_" + channelId);
		return ssrcTransaction;
	}

	public String getStreamId(String deviceId, String channelId){
		SsrcTransaction ssrcTransaction = getSsrcTransaction(deviceId, channelId);
		if (ssrcTransaction == null) return null;
		return ssrcTransaction.getStreamId();
	}
	public String getMediaServerId(String deviceId, String channelId){
		SsrcTransaction ssrcTransaction = getSsrcTransaction(deviceId, channelId);
		if (ssrcTransaction == null) return null;
		return ssrcTransaction.getMediaServerId();
	}

	public String getSSRC(String deviceId, String channelId){
		SsrcTransaction ssrcTransaction = getSsrcTransaction(deviceId, channelId);
		if (ssrcTransaction == null) return null;
		return ssrcTransaction.getSsrc();
	}
	
	public void remove(String deviceId, String channelId) {
		SsrcTransaction ssrcTransaction = getSsrcTransaction(deviceId, channelId);
		if (ssrcTransaction == null) return;
		redisUtil.del(VideoManagerConstants.MEDIA_TRANSACTION_USED_PREFIX + userSetup.getServerId() + "_" +  deviceId + "_" + channelId);
	}

	public List<SsrcTransaction> getAllSsrc() {
		List<Object> ssrcTransactionKeys = redisUtil.scan(String.format("%s_*_*", VideoManagerConstants.MEDIA_TRANSACTION_USED_PREFIX+ userSetup.getServerId() + "_" ));
		List<SsrcTransaction> result= new ArrayList<>();
		for (int i = 0; i < ssrcTransactionKeys.size(); i++) {
			String key = (String)ssrcTransactionKeys.get(i);
			SsrcTransaction ssrcTransaction = (SsrcTransaction)redisUtil.get(key);
			result.add(ssrcTransaction);
		}
		return result;
	}
}
