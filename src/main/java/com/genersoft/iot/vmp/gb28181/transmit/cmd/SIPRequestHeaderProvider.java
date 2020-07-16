package com.genersoft.iot.vmp.gb28181.transmit.cmd;

import java.text.ParseException;
import java.util.ArrayList;

import javax.sip.InvalidArgumentException;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.Host;

/**
 * @Description:摄像头命令request创造器 TODO 冗余代码太多待优化
 * @author: swwheihei
 * @date: 2020年5月6日 上午9:29:02
 */
@Component
public class SIPRequestHeaderProvider {

	@Autowired
	private SipLayer layer;

	@Autowired
	private SipConfig sipConfig;
	
	public Request createMessageRequest(Device device, String content, String viaTag, String fromTag, String toTag) throws ParseException, InvalidArgumentException {
		Request request = null;
		Host host = device.getHost();
		// sipuri
		SipURI requestURI = layer.getAddressFactory().createSipURI(device.getDeviceId(), host.getAddress());
		// via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = layer.getHeaderFactory().createViaHeader(sipConfig.getSipIp(), sipConfig.getSipPort(),
				device.getTransport(), viaTag);
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		// from
		SipURI fromSipURI = layer.getAddressFactory().createSipURI(sipConfig.getSipId(),
				sipConfig.getSipIp() + ":" + sipConfig.getSipPort());
		Address fromAddress = layer.getAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = layer.getHeaderFactory().createFromHeader(fromAddress, fromTag);
		// to
		SipURI toSipURI = layer.getAddressFactory().createSipURI(device.getDeviceId(), sipConfig.getSipDomain());
		Address toAddress = layer.getAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = layer.getHeaderFactory().createToHeader(toAddress, toTag);
		// callid
		CallIdHeader callIdHeader = device.getTransport().equals("TCP") ? layer.getTcpSipProvider().getNewCallId()
				: layer.getUdpSipProvider().getNewCallId();
		// Forwards
		MaxForwardsHeader maxForwards = layer.getHeaderFactory().createMaxForwardsHeader(70);
		// ceq
		CSeqHeader cSeqHeader = layer.getHeaderFactory().createCSeqHeader(1L, Request.MESSAGE);

		request = layer.getMessageFactory().createRequest(requestURI, Request.MESSAGE, callIdHeader, cSeqHeader, fromHeader,
				toHeader, viaHeaders, maxForwards);
		ContentTypeHeader contentTypeHeader = layer.getHeaderFactory().createContentTypeHeader("Application", "MANSCDP+xml");
		request.setContent(content, contentTypeHeader);
		return request;
	}
	
	public Request createInviteRequest(Device device, String channelId, String content, String viaTag, String fromTag, String toTag) throws ParseException, InvalidArgumentException {
		Request request = null;
		Host host = device.getHost();
		//请求行
		SipURI requestLine = layer.getAddressFactory().createSipURI(channelId, host.getAddress());
		//via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = layer.getHeaderFactory().createViaHeader(sipConfig.getSipIp(), sipConfig.getSipPort(), device.getTransport(), viaTag);
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		//from
		SipURI fromSipURI = layer.getAddressFactory().createSipURI(sipConfig.getSipId(),sipConfig.getSipDomain());
		Address fromAddress = layer.getAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = layer.getHeaderFactory().createFromHeader(fromAddress, fromTag); //必须要有标记，否则无法创建会话，无法回应ack
		//to
		SipURI toSipURI = layer.getAddressFactory().createSipURI(channelId,sipConfig.getSipDomain()); 
		Address toAddress = layer.getAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = layer.getHeaderFactory().createToHeader(toAddress,null);

		//callid
		CallIdHeader callIdHeader = null;
		if(device.getTransport().equals("TCP")) {
			callIdHeader = layer.getTcpSipProvider().getNewCallId();
		}
		if(device.getTransport().equals("UDP")) {
			callIdHeader = layer.getUdpSipProvider().getNewCallId();
		}
		
		//Forwards
		MaxForwardsHeader maxForwards = layer.getHeaderFactory().createMaxForwardsHeader(70);
		
		//ceq
		CSeqHeader cSeqHeader = layer.getHeaderFactory().createCSeqHeader(1L, Request.INVITE);
		request = layer.getMessageFactory().createRequest(requestLine, Request.INVITE, callIdHeader, cSeqHeader,fromHeader, toHeader, viaHeaders, maxForwards);
		
		Address concatAddress = layer.getAddressFactory().createAddress(layer.getAddressFactory().createSipURI(sipConfig.getSipId(), sipConfig.getSipIp()+":"+sipConfig.getSipPort()));
		request.addHeader(layer.getHeaderFactory().createContactHeader(concatAddress));
		
		ContentTypeHeader contentTypeHeader = layer.getHeaderFactory().createContentTypeHeader("Application", "SDP");
		request.setContent(content, contentTypeHeader);
		return request;
	}
	
	public Request createPlaybackInviteRequest(Device device, String channelId, String content, String viaTag, String fromTag, String toTag) throws ParseException, InvalidArgumentException {
		Request request = null;
		Host host = device.getHost();
		//请求行
		SipURI requestLine = layer.getAddressFactory().createSipURI(device.getDeviceId(), host.getAddress());
		//via
		ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		ViaHeader viaHeader = layer.getHeaderFactory().createViaHeader(sipConfig.getSipIp(), sipConfig.getSipPort(), device.getTransport(), viaTag);
		viaHeader.setRPort();
		viaHeaders.add(viaHeader);
		//from
		SipURI fromSipURI = layer.getAddressFactory().createSipURI(sipConfig.getSipId(),sipConfig.getSipDomain());
		Address fromAddress = layer.getAddressFactory().createAddress(fromSipURI);
		FromHeader fromHeader = layer.getHeaderFactory().createFromHeader(fromAddress, fromTag); //必须要有标记，否则无法创建会话，无法回应ack
		//to
		SipURI toSipURI = layer.getAddressFactory().createSipURI(channelId,sipConfig.getSipDomain()); 
		Address toAddress = layer.getAddressFactory().createAddress(toSipURI);
		ToHeader toHeader = layer.getHeaderFactory().createToHeader(toAddress,null);

		//callid
		CallIdHeader callIdHeader = null;
		if(device.getTransport().equals("TCP")) {
			callIdHeader = layer.getTcpSipProvider().getNewCallId();
		}
		if(device.getTransport().equals("UDP")) {
			callIdHeader = layer.getUdpSipProvider().getNewCallId();
		}
		
		//Forwards
		MaxForwardsHeader maxForwards = layer.getHeaderFactory().createMaxForwardsHeader(70);
		
		//ceq
		CSeqHeader cSeqHeader = layer.getHeaderFactory().createCSeqHeader(1L, Request.INVITE);
		request = layer.getMessageFactory().createRequest(requestLine, Request.INVITE, callIdHeader, cSeqHeader,fromHeader, toHeader, viaHeaders, maxForwards);
		
		Address concatAddress = layer.getAddressFactory().createAddress(layer.getAddressFactory().createSipURI(sipConfig.getSipId(), sipConfig.getSipIp()+":"+sipConfig.getSipPort()));
		request.addHeader(layer.getHeaderFactory().createContactHeader(concatAddress));
		
		ContentTypeHeader contentTypeHeader = layer.getHeaderFactory().createContentTypeHeader("Application", "SDP");
		request.setContent(content, contentTypeHeader);
		return request;
	}
}
