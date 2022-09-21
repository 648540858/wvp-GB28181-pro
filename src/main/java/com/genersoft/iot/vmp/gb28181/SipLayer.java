package com.genersoft.iot.vmp.gb28181;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.conf.DefaultProperties;
import com.genersoft.iot.vmp.gb28181.transmit.ISIPProcessorObserver;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.SipStackImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sip.*;
import java.util.Properties;
import java.util.TooManyListenersException;

@Configuration
public class SipLayer{

	private final static Logger logger = LoggerFactory.getLogger(SipLayer.class);

	@Autowired
	private SipConfig sipConfig;

	@Autowired
	private ISIPProcessorObserver sipProcessorObserver;

	private SipStackImpl sipStack;

	private SipFactory sipFactory;


	@Bean("sipFactory")
	SipFactory createSipFactory() {
		sipFactory = SipFactory.getInstance();
		sipFactory.setPathName("gov.nist");
		return sipFactory;
	}
	
	@Bean("sipStack")
	@DependsOn({"sipFactory"})
	SipStack createSipStack() throws PeerUnavailableException {
		sipStack = ( SipStackImpl )sipFactory.createSipStack(DefaultProperties.getProperties(sipConfig.getMonitorIp(), false));
		return sipStack;
	}

	@Bean(name = "tcpSipProvider")
	@DependsOn("sipStack")
	SipProviderImpl startTcpListener() {
		ListeningPoint tcpListeningPoint = null;
		SipProviderImpl tcpSipProvider  = null;
		try {
			tcpListeningPoint = sipStack.createListeningPoint(sipConfig.getMonitorIp(), sipConfig.getPort(), "TCP");
			tcpSipProvider = (SipProviderImpl)sipStack.createSipProvider(tcpListeningPoint);
			tcpSipProvider.setDialogErrorsAutomaticallyHandled();
			tcpSipProvider.addSipListener(sipProcessorObserver);
//			tcpSipProvider.setAutomaticDialogSupportEnabled(false);
			logger.info("[Sip Server] TCP 启动成功 {}:{}", sipConfig.getMonitorIp(), sipConfig.getPort());
		} catch (TransportNotSupportedException e) {
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			logger.error("[Sip Server]  无法使用 [ {}:{} ]作为SIP[ TCP ]服务，可排查: 1. sip.monitor-ip 是否为本机网卡IP; 2. sip.port 是否已被占用"
					, sipConfig.getMonitorIp(), sipConfig.getPort());
		} catch (TooManyListenersException e) {
			e.printStackTrace();
		} catch (ObjectInUseException e) {
			e.printStackTrace();
		}
		return tcpSipProvider;
	}
	
	@Bean(name = "udpSipProvider")
	@DependsOn("sipStack")
	SipProviderImpl startUdpListener() {
		ListeningPoint udpListeningPoint = null;
		SipProviderImpl udpSipProvider = null;
		try {
			udpListeningPoint = sipStack.createListeningPoint(sipConfig.getMonitorIp(), sipConfig.getPort(), "UDP");
			udpSipProvider = (SipProviderImpl)sipStack.createSipProvider(udpListeningPoint);
			udpSipProvider.addSipListener(sipProcessorObserver);
//			udpSipProvider.setAutomaticDialogSupportEnabled(false);
		} catch (TransportNotSupportedException e) {
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			logger.error("[Sip Server]  无法使用 [ {}:{} ]作为SIP[ UDP ]服务，可排查: 1. sip.monitor-ip 是否为本机网卡IP; 2. sip.port 是否已被占用"
					, sipConfig.getMonitorIp(), sipConfig.getPort());
		} catch (TooManyListenersException e) {
			e.printStackTrace();
		} catch (ObjectInUseException e) {
			e.printStackTrace();
		}
		logger.info("[Sip Server] UDP 启动成功 {}:{}", sipConfig.getMonitorIp(), sipConfig.getPort());
		return udpSipProvider;
	}

}
