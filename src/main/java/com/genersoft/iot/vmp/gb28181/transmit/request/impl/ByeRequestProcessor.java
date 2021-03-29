package com.genersoft.iot.vmp.gb28181.transmit.request.impl;

import javax.sip.address.SipURI;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderAddress;
import javax.sip.header.ToHeader;
import javax.sip.message.Response;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.request.SIPRequestAbstractProcessor;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**    
 * @Description: BYE请求处理器
 * @author: lawrencehj
 * @date:   2021年3月9日     
 */
public class ByeRequestProcessor extends SIPRequestAbstractProcessor {

	private ISIPCommander cmder;

	private IRedisCatchStorage redisCatchStorage;

	private ZLMRTPServerFactory zlmrtpServerFactory;

	@Autowired
	private VideoStreamSessionManager streamSession;

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
				String platformGbId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(FromHeader.NAME)).getAddress().getURI()).getUser();
				String channelId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(ToHeader.NAME)).getAddress().getURI()).getUser();
				SendRtpItem sendRtpItem =  redisCatchStorage.querySendRTPServer(platformGbId, channelId);
				String streamId = sendRtpItem.getStreamId();
				Map<String, Object> param = new HashMap<>();
				param.put("vhost","__defaultVhost__");
				param.put("app","rtp");
				param.put("stream",streamId);
				System.out.println("停止向上级推流：" + streamId);
				StreamInfo streamInfo = streamSession.getStreamInfo(channelId, streamId);
				zlmrtpServerFactory.stopSendRtpStream(param);
				redisCatchStorage.deleteSendRTPServer(platformGbId, channelId);
				if (zlmrtpServerFactory.totalReaderCount(streamId) == 0) {
					System.out.println(streamId + "无其它观看者，通知设备停止推流");
					cmder.stopStreamByeCmd(streamInfo, null);
				}
			}
		} catch (SipException e) {
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
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

	public ISIPCommander getSIPCommander() {
		return cmder;
	}

	public void setSIPCommander(ISIPCommander cmder) {
		this.cmder = cmder;
	}

}
