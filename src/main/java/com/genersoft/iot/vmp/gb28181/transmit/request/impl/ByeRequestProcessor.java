package com.genersoft.iot.vmp.gb28181.transmit.request.impl;

import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;

import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.gb28181.transmit.request.SIPRequestAbstractProcessor;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**    
 * @Description: BYE请求处理器
 * @author: swwheihei
 * @date:   2020年5月3日 下午5:32:05     
 */
public class ByeRequestProcessor extends SIPRequestAbstractProcessor {

    private IRedisCatchStorage redisCatchStorage;

	private ZLMRTPServerFactory zlmrtpServerFactory;

	/**
	 * 处理BYE请求
	 * @param evt
	 */
	@Override
	public void process(RequestEvent evt) {
		try {
			responseAck(evt);
			Dialog dialog = evt.getDialog();
			if (dialog == null) return;
			if (dialog.getState().equals(DialogState.TERMINATED)) {
				String remoteUri = dialog.getRemoteParty().getURI().toString();
				String localUri = dialog.getLocalParty().getURI().toString();
				String platformGbId = remoteUri.substring(remoteUri.indexOf(":") + 1, remoteUri.indexOf("@"));
				String channelId = localUri.substring(remoteUri.indexOf(":") + 1, remoteUri.indexOf("@"));
				SendRtpItem sendRtpItem =  redisCatchStorage.querySendRTPServer(platformGbId, channelId);
				String streamId = sendRtpItem.getStreamId();
				Map<String, Object> param = new HashMap<>();
				param.put("vhost","__defaultVhost__");
				param.put("app","rtp");
				param.put("stream",streamId);
				System.out.println("停止向上级推流：" + streamId);
				zlmrtpServerFactory.stopSendRtpStream(param);
			}
		} catch (SipException e) {
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		// TODO 优先级99 Bye Request消息实现，此消息一般为级联消息，上级给下级发送视频停止指令
		
	}

	/***
	 * 回复200 OK
	 * @param evt
	 * @throws SipException
	 * @throws InvalidArgumentException
	 * @throws ParseException
	 */
	private void responseAck(RequestEvent evt) throws SipException, InvalidArgumentException, ParseException {
		Response response = getMessageFactory().createResponse(Response.OK, evt.getRequest());
		getServerTransaction(evt).sendResponse(response);
	}

	public IRedisCatchStorage getRedisCatchStorage() {
		return redisCatchStorage;
	}

	public void setRedisCatchStorage(IRedisCatchStorage redisCatchStorage) {
		this.redisCatchStorage = redisCatchStorage;
	}

	public ZLMRTPServerFactory getZlmrtpServerFactory() {
		return zlmrtpServerFactory;
	}

	public void setZlmrtpServerFactory(ZLMRTPServerFactory zlmrtpServerFactory) {
		this.zlmrtpServerFactory = zlmrtpServerFactory;
	}
}
