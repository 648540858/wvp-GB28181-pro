package com.genersoft.iot.vmp.gb28181;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.transmit.ISIPProcessorObserver;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.SipStackImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.sip.*;
import java.util.Properties;
import java.util.TooManyListenersException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class SipLayer{

	private final static Logger logger = LoggerFactory.getLogger(SipLayer.class);

	@Autowired
	private SipConfig sipConfig;

	@Autowired
	private ISIPProcessorObserver sipProcessorObserver;

	private SipStackImpl sipStack;

	private SipFactory sipFactory;


	@Bean("sipFactory")
	private SipFactory createSipFactory() {
		sipFactory = SipFactory.getInstance();
		sipFactory.setPathName("gov.nist");
		return sipFactory;
	}
	
	@Bean("sipStack")
	@DependsOn({"sipFactory"})
	private SipStack createSipStack() throws PeerUnavailableException {
		Properties properties = new Properties();
		properties.setProperty("javax.sip.STACK_NAME", "GB28181_SIP");
		properties.setProperty("javax.sip.IP_ADDRESS", sipConfig.getMonitorIp());
		properties.setProperty("gov.nist.javax.sip.LOG_MESSAGE_CONTENT", "true");
		/**
		 * sip_server_log.log 和 sip_debug_log.log public static final int TRACE_NONE =
		 * 0; public static final int TRACE_MESSAGES = 16; public static final int
		 * TRACE_EXCEPTION = 17; public static final int TRACE_DEBUG = 32;
		 */
		properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "0");
		properties.setProperty("gov.nist.javax.sip.SERVER_LOG", "sip_server_log");
		properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "sip_debug_log");
		sipStack = (SipStackImpl) sipFactory.createSipStack(properties);

		return sipStack;
	}

	@Bean(name = "tcpSipProvider")
	@DependsOn("sipStack")
	private SipProviderImpl startTcpListener() {
		ListeningPoint tcpListeningPoint = null;
		SipProviderImpl tcpSipProvider  = null;
		try {
			tcpListeningPoint = sipStack.createListeningPoint(sipConfig.getMonitorIp(), sipConfig.getPort(), "TCP");
			tcpSipProvider = (SipProviderImpl)sipStack.createSipProvider(tcpListeningPoint);
			tcpSipProvider.setDialogErrorsAutomaticallyHandled();
			tcpSipProvider.addSipListener(sipProcessorObserver);
//			tcpSipProvider.setAutomaticDialogSupportEnabled(false);
			logger.info("Sip Server TCP 启动成功 port {" + sipConfig.getMonitorIp() + ":" + sipConfig.getPort() + "}");
		} catch (TransportNotSupportedException e) {
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			logger.error("无法使用 [ {}:{} ]作为SIP[ TCP ]服务，可排查: 1. sip.monitor-ip 是否为本机网卡IP; 2. sip.port 是否已被占用"
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
	private SipProviderImpl startUdpListener() {
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
			logger.error("无法使用 [ {}:{} ]作为SIP[ UDP ]服务，可排查: 1. sip.monitor-ip 是否为本机网卡IP; 2. sip.port 是否已被占用"
					, sipConfig.getMonitorIp(), sipConfig.getPort());
		} catch (TooManyListenersException e) {
			e.printStackTrace();
		} catch (ObjectInUseException e) {
			e.printStackTrace();
		}
		logger.info("Sip Server UDP 启动成功 port [" + sipConfig.getMonitorIp() + ":" + sipConfig.getPort() + "]");
		return udpSipProvider;
	}

}
