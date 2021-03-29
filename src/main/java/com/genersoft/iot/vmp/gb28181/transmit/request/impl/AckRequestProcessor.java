package com.genersoft.iot.vmp.gb28181.transmit.request.impl;

import java.util.HashMap;
import java.util.Map;

import javax.sip.*;
import javax.sip.address.SipURI;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderAddress;
import javax.sip.header.ToHeader;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.request.SIPRequestAbstractProcessor;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import org.springframework.beans.factory.annotation.Autowired;

/**    
 * @Description:ACK请求处理器  
 * @author: swwheihei
 * @date:   2020年5月3日 下午5:31:45     
 */
public class AckRequestProcessor extends SIPRequestAbstractProcessor {

    private IRedisCatchStorage redisCatchStorage;

	private ZLMRTPServerFactory zlmrtpServerFactory;

	@Autowired
	private VideoStreamSessionManager streamSession;

	/**   
	 * 处理  ACK请求
	 * 
	 * @param evt
	 */
	@Override
	public void process(RequestEvent evt) {
		//Request request = evt.getRequest();
		Dialog dialog = evt.getDialog();
		if (dialog == null) {
			return;
		}
		//DialogState state = dialog.getState();
		if (/*request.getMecodewwthod().equals(Request.INVITE) &&*/ dialog.getState()== DialogState.CONFIRMED) {
			String platformGbId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(FromHeader.NAME)).getAddress().getURI()).getUser();
			String channelId = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(ToHeader.NAME)).getAddress().getURI()).getUser();
			SendRtpItem sendRtpItem =  redisCatchStorage.querySendRTPServer(platformGbId, channelId);
			String is_Udp = sendRtpItem.isTcp() ? "0" : "1";
			String deviceId = sendRtpItem.getDeviceId();
			StreamInfo streamInfo = redisCatchStorage.queryPlayByChannel(channelId);
			sendRtpItem.setStreamId(streamInfo.getStreamId());
			redisCatchStorage.updateSendRTPSever(sendRtpItem);
			System.out.println(platformGbId);
			System.out.println(channelId);
			Map<String, Object> param = new HashMap<>();
			param.put("vhost","__defaultVhost__");
			param.put("app","rtp");
			param.put("stream",streamInfo.getStreamId());
			param.put("ssrc", sendRtpItem.getSsrc());
			param.put("dst_url",sendRtpItem.getIp());
			param.put("dst_port", sendRtpItem.getPort());
			param.put("is_udp", is_Udp);
			//param.put ("src_port", sendRtpItem.getLocalPort());
			// 设备推流查询，成功后才能转推
			boolean rtpPushed = false;
			long startTime = System.currentTimeMillis();
			while (!rtpPushed) {
				try {
					if (System.currentTimeMillis() - startTime < 30 * 1000) {
						if (zlmrtpServerFactory.isRtpReady(streamInfo.getStreamId())) {
							rtpPushed = true;
							System.out.println("已获取设备推流，开始向上级推流");
							zlmrtpServerFactory.startSendRtpStream(param);
						} else {
							System.out.println("等待设备推流.......");
							Thread.sleep(2000);
							continue;
						}
					} else {
						rtpPushed = true;
						System.out.println("设备推流超时，终止向上级推流");
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		// try {
		// 	Request ackRequest = null;
		// 	CSeq csReq = (CSeq) request.getHeader(CSeq.NAME);
		// 	ackRequest = dialog.createAck(csReq.getSeqNumber());
		// 	dialog.sendAck(ackRequest);
		// 	System.out.println("send ack to callee:" + ackRequest.toString());
		// } catch (SipException e) {
		// 	e.printStackTrace();
		// } catch (InvalidArgumentException e) {
		// 	e.printStackTrace();
		// }
		
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
