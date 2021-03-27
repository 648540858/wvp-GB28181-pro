package com.genersoft.iot.vmp.gb28181;

import java.text.ParseException;
import java.util.Properties;
import java.util.TooManyListenersException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.sip.*;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Response;

import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorFactory;
import com.genersoft.iot.vmp.gb28181.transmit.response.ISIPResponseProcessor;

import gov.nist.javax.sip.SipStackImpl;

@Component
public class SipLayer implements SipListener {

	private final static Logger logger = LoggerFactory.getLogger(SipLayer.class);

	@Autowired
	private SipConfig sipConfig;

	@Autowired
	private SIPProcessorFactory processorFactory;

	@Autowired
	private SipSubscribe sipSubscribe;

	private SipStack sipStack;

	private SipFactory sipFactory;

	/**   
	 * 消息处理器线程池
	 */
	private ThreadPoolExecutor processThreadPool;

	@Bean("initSipServer")
	private ThreadPoolExecutor initSipServer() {
		
		int processThreadNum = Runtime.getRuntime().availableProcessors() * 10;
		LinkedBlockingQueue<Runnable> processQueue = new LinkedBlockingQueue<Runnable>(10000);
		processThreadPool = new ThreadPoolExecutor(processThreadNum,processThreadNum,
				0L,TimeUnit.MILLISECONDS,processQueue,
				new ThreadPoolExecutor.CallerRunsPolicy());
		return processThreadPool;
	}
	
	@Bean("sipFactory")
	@DependsOn("initSipServer")
	private SipFactory createSipFactory() {
		sipFactory = SipFactory.getInstance();
		sipFactory.setPathName("gov.nist");
		return sipFactory;
	}
	
	@Bean("sipStack")
	@DependsOn({"initSipServer", "sipFactory"})
	private SipStack createSipStack() throws PeerUnavailableException {
		Properties properties = new Properties();
		properties.setProperty("javax.sip.STACK_NAME", "GB28181_SIP");
		properties.setProperty("javax.sip.IP_ADDRESS", sipConfig.getSipIp());
		properties.setProperty("gov.nist.javax.sip.LOG_MESSAGE_CONTENT", "false");
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

	@Bean("tcpSipProvider")
	@DependsOn("sipStack")
	private SipProvider startTcpListener() {
		ListeningPoint tcpListeningPoint = null;
		SipProvider tcpSipProvider  = null;
		try {
			tcpListeningPoint = sipStack.createListeningPoint(sipConfig.getSipIp(), sipConfig.getSipPort(), "TCP");
			tcpSipProvider = sipStack.createSipProvider(tcpListeningPoint);
			tcpSipProvider.addSipListener(this);
			logger.info("Sip Server TCP 启动成功 port {" + sipConfig.getSipPort() + "}");
		} catch (TransportNotSupportedException | InvalidArgumentException | TooManyListenersException | ObjectInUseException e) {
			logger.error(String.format("创建SIP服务失败: %s", e.getMessage()));
		}
		return tcpSipProvider;
	}
	
	@Bean("udpSipProvider")
	@DependsOn("sipStack")
	private SipProvider startUdpListener() throws Exception {
		ListeningPoint udpListeningPoint = sipStack.createListeningPoint(sipConfig.getSipIp(), sipConfig.getSipPort(), "UDP");
		SipProvider udpSipProvider = sipStack.createSipProvider(udpListeningPoint);
		udpSipProvider.addSipListener(this);
		logger.info("Sip Server UDP 启动成功 port {" + sipConfig.getSipPort() + "}");
		return udpSipProvider;
	}

	/**
	 * SIP服务端接收消息的方法 Content 里面是GBK编码 This method is called by the SIP stack when a
	 * new request arrives.
	 */
	@Override
	public void processRequest(RequestEvent evt) {
		logger.debug(evt.getRequest().toString());
		// 由于jainsip是单线程程序，为提高性能并发处理
		processThreadPool.execute(() -> {
			if (processorFactory != null) {
				processorFactory.createRequestProcessor(evt).process();
			}
		});
	}

	@Override
	public void processResponse(ResponseEvent evt) {
		Response response = evt.getResponse();
		logger.debug(evt.getResponse().toString());
		int status = response.getStatusCode();
		if (((status >= 200) && (status < 300)) || status == 401) { // Success!
			ISIPResponseProcessor processor = processorFactory.createResponseProcessor(evt);
			try {
				processor.process(evt, this, sipConfig);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (evt.getResponse() != null && sipSubscribe.getOkSubscribesSize() > 0 ) {
				CallIdHeader callIdHeader = (CallIdHeader)evt.getResponse().getHeader(CallIdHeader.NAME);
				if (callIdHeader != null) {
					SipSubscribe.Event subscribe = sipSubscribe.getOkSubscribe(callIdHeader.getCallId());
					if (subscribe != null) {
						subscribe.response(evt);
					}
				}
			}
		} else if ((status >= 100) && (status < 200)) {
			// 增加其它无需回复的响应，如101、180等
		} else {
			logger.warn("接收到失败的response响应！status：" + status + ",message:" + response.getReasonPhrase()/* .getContent().toString()*/);
			if (evt.getResponse() != null && sipSubscribe.getErrorSubscribesSize() > 0 ) {
				CallIdHeader callIdHeader = (CallIdHeader)evt.getResponse().getHeader(CallIdHeader.NAME);
				if (callIdHeader != null) {
					SipSubscribe.Event subscribe = sipSubscribe.getErrorSubscribe(callIdHeader.getCallId());
					if (subscribe != null) {
						subscribe.response(evt);
					}
				}
			}
		}



	}

	/**
	 * <p>
	 * Title: processTimeout
	 * </p>
	 * <p>
	 * Description:
	 * </p>
	 * 
	 * @param timeoutEvent
	 */
	@Override
	public void processTimeout(TimeoutEvent timeoutEvent) {
		// TODO Auto-generated method stub

	}

	/**
	 * <p>
	 * Title: processIOException
	 * </p>
	 * <p>
	 * Description:
	 * </p>
	 * 
	 * @param exceptionEvent
	 */
	@Override
	public void processIOException(IOExceptionEvent exceptionEvent) {
		// TODO Auto-generated method stub

	}

	/**
	 * <p>
	 * Title: processTransactionTerminated
	 * </p>
	 * <p>
	 * Description:
	 * </p>
	 * 
	 * @param transactionTerminatedEvent
	 */
	@Override
	public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
		// TODO Auto-generated method stub

	}

	/**
	 * <p>
	 * Title: processDialogTerminated
	 * </p>
	 * <p>
	 * Description:
	 * </p>
	 * 
	 * @param dialogTerminatedEvent
	 */
	@Override
	public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
		// TODO Auto-generated method stub

	}

}
