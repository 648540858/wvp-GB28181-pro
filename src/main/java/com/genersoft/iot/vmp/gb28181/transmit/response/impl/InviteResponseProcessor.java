package com.genersoft.iot.vmp.gb28181.transmit.response.impl;

import java.text.ParseException;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorFactory;
import com.genersoft.iot.vmp.gb28181.transmit.response.ISIPResponseProcessor;

/**    
 * @Description:处理INVITE响应
 * @author: swwheihei
 * @date:   2020年5月3日 下午4:43:52     
 */
@Component
public class InviteResponseProcessor implements ISIPResponseProcessor {

	private final static Logger logger = LoggerFactory.getLogger(SIPProcessorFactory.class);
	
	/**
	 * 处理invite响应
	 * 
	 * @param evt
	 *            响应消息
	 */ 
	@Override
	public void process(ResponseEvent evt, SipLayer layer, SipConfig config) {
		try {
			Response response = evt.getResponse();
			int statusCode = response.getStatusCode();
			//trying不会回复
			if(statusCode == Response.TRYING){

			}
			//成功响应
			//下发ack
			if(statusCode == Response.OK){
//				ClientTransaction clientTransaction = evt.getClientTransaction();
//				if(clientTransaction == null){
//					logger.error("回复ACK时，clientTransaction为null >>> {}",response);
//					return;
//				}
//				Dialog clientDialog = clientTransaction.getDialog();
//
//				CSeqHeader clientCSeqHeader = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
//				long cseqId = clientCSeqHeader.getSeqNumber();
//				/*
//				createAck函数，创建的ackRequest，会采用Invite响应的200OK，中的contact字段中的地址，作为目标地址。
//				有的终端传上来的可能还是内网地址，会造成ack发送不出去。接受不到音视频流
//				所以在此处统一替换地址。和响应消息的Via头中的地址保持一致。
//				 */
//				Request ackRequest = clientDialog.createAck(cseqId);
//				SipURI requestURI = (SipURI) ackRequest.getRequestURI();
//				ViaHeader viaHeader = (ViaHeader) response.getHeader(ViaHeader.NAME);
//				requestURI.setHost(viaHeader.getHost());
//				requestURI.setPort(viaHeader.getPort());
//				clientDialog.sendAck(ackRequest);
				
				Dialog dialog = evt.getDialog();
				Request reqAck =dialog.createAck(1L);
				dialog.sendAck(reqAck);
			}
		} catch (InvalidArgumentException | SipException e) {
			e.printStackTrace();
		}
	}

}
