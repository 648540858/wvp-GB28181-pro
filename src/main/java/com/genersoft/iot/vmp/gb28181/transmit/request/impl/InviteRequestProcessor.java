package com.genersoft.iot.vmp.gb28181.transmit.request.impl;

import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;

import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.transmit.request.ISIPRequestProcessor;

/**    
 * @Description:处理INVITE请求
 * @author: songww
 * @date:   2020年5月3日 下午4:43:52     
 */
@Component
public class InviteRequestProcessor implements ISIPRequestProcessor {

	/**
	 * 处理invite请求
	 * 
	 * @param request
	 *            请求消息
	 */ 
	@Override
	public void process(RequestEvent evt, SipLayer layer) {
		// TODO Auto-generated method stub
//		Request request = requestEvent.getRequest();
//
//		try {
//			// 发送100 Trying
//			ServerTransaction serverTransaction = getServerTransaction(requestEvent);
//			// 查询目标地址
//			URI reqUri = request.getRequestURI();
//			URI contactURI = currUser.get(reqUri);
//
//			System.out.println("processInvite rqStr=" + reqUri + " contact=" + contactURI);
//
//			// 根据Request uri来路由，后续的响应消息通过VIA来路由
//			Request cliReq = messageFactory.createRequest(request.toString());
//			cliReq.setRequestURI(contactURI);
//
//			HeaderFactory headerFactory = SipFactory.getInstance().createHeaderFactory();
//			Via callerVia = (Via) request.getHeader(Via.NAME);
//			Via via = (Via) headerFactory.createViaHeader(SIPMain.ip, SIPMain.port, "UDP",
//					callerVia.getBranch() + "sipphone");
//
//			// FIXME 需要测试是否能够通过设置VIA头域来修改VIA头域值
//			cliReq.removeHeader(Via.NAME);
//			cliReq.addHeader(via);
//
//			// 更新contact的地址
//			ContactHeader contactHeader = headerFactory.createContactHeader();
//			Address address = SipFactory.getInstance().createAddressFactory()
//					.createAddress("sip:sipsoft@" + SIPMain.ip + ":" + SIPMain.port);
//			contactHeader.setAddress(address);
//			contactHeader.setExpires(3600);
//			cliReq.setHeader(contactHeader);
//
//			clientTransactionId = sipProvider.getNewClientTransaction(cliReq);
//			clientTransactionId.sendRequest();
//
//			System.out.println("processInvite clientTransactionId=" + clientTransactionId.toString());
//
//			System.out.println("send invite to callee: " + cliReq);
//		} catch (TransactionUnavailableException e1) {
//			e1.printStackTrace();
//		} catch (SipException e) {
//			e.printStackTrace();
//		} catch (ParseException e) {
//			e.printStackTrace();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}

}
