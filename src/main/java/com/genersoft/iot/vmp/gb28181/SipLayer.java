package com.genersoft.iot.vmp.gb28181;

import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.ListeningPoint;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionAlreadyExistsException;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.TransactionUnavailableException;
import javax.sip.address.AddressFactory;
import javax.sip.header.HeaderFactory;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorFactory;
import com.genersoft.iot.vmp.gb28181.transmit.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.response.ISIPResponseProcessor;

import gov.nist.javax.sip.SipStackImpl;

@Component
public class SipLayer implements SipListener, Runnable {

	private final static Logger logger = LoggerFactory.getLogger(SipLayer.class);

	@Autowired
	private SipConfig sipConfig;

	private SipProvider tcpSipProvider;

	private SipProvider udpSipProvider;

	@Autowired
	private SIPProcessorFactory processorFactory;

	private SipStack sipStack;

	private AddressFactory addressFactory;
	private HeaderFactory headerFactory;
	private MessageFactory messageFactory;

	@PostConstruct
	private void initSipServer() {
		Thread thread=new Thread(this);
        thread.setDaemon(true);
        thread.setName("sip server thread start");
        thread.start();
	}

	@Override
	public void run() {
		SipFactory sipFactory = SipFactory.getInstance();
		sipFactory.setPathName("gov.nist");
		try {
			headerFactory = sipFactory.createHeaderFactory();

			addressFactory = sipFactory.createAddressFactory();
			messageFactory = sipFactory.createMessageFactory();

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

			startTcpListener();
			startUdpListener();
		} catch (Exception e) {
			logger.error("Sip Server 启动失败！ port {" + sipConfig.getSipPort() + "}");
			e.printStackTrace();
		}
		logger.info("Sip Server 启动成功 port {" + sipConfig.getSipPort() + "}");
	}

	private void startTcpListener() throws Exception {
		ListeningPoint tcpListeningPoint = sipStack.createListeningPoint(sipConfig.getSipIp(), sipConfig.getSipPort(), "TCP");
		tcpSipProvider = sipStack.createSipProvider(tcpListeningPoint);
		tcpSipProvider.addSipListener(this);
	}

	private void startUdpListener() throws Exception {
		ListeningPoint udpListeningPoint = sipStack.createListeningPoint(sipConfig.getSipIp(), sipConfig.getSipPort(), "UDP");
		udpSipProvider = sipStack.createSipProvider(udpListeningPoint);
		udpSipProvider.addSipListener(this);
	}

	/**
	 * SIP服务端接收消息的方法 Content 里面是GBK编码 This method is called by the SIP stack when a
	 * new request arrives.
	 */
	@Override
	public void processRequest(RequestEvent evt) {
		ISIPRequestProcessor processor = processorFactory.createRequestProcessor(evt);
		processor.process(evt, this, getServerTransaction(evt));
	}

	@Override
	public void processResponse(ResponseEvent evt) {
		Response response = evt.getResponse();
		int status = response.getStatusCode();
		if ((status >= 200) && (status < 300)) { // Success!
			ISIPResponseProcessor processor = processorFactory.createResponseProcessor(evt);
			processor.process(evt, this, sipConfig);
		} else {
			logger.warn("接收到失败的response响应！status：" + status + ",message:" + response.getContent().toString());
		}
		// trying不会回复
		if (status == Response.TRYING) {

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

	private ServerTransaction getServerTransaction(RequestEvent evt) {
		Request request = evt.getRequest();
		ServerTransaction serverTransaction = evt.getServerTransaction();
		// 判断TCP还是UDP
		boolean isTcp = false;
		ViaHeader reqViaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
		String transport = reqViaHeader.getTransport();
		if (transport.equals("TCP")) {
			isTcp = true;
		}

		if (serverTransaction == null) {
			try {
				if (isTcp) {
					serverTransaction = tcpSipProvider.getNewServerTransaction(request);
				} else {
					serverTransaction = udpSipProvider.getNewServerTransaction(request);
				}
			} catch (TransactionAlreadyExistsException e) {
				e.printStackTrace();
			} catch (TransactionUnavailableException e) {
				e.printStackTrace();
			}
		}
		return serverTransaction;
	}

	public AddressFactory getAddressFactory() {
		return addressFactory;
	}

	public HeaderFactory getHeaderFactory() {
		return headerFactory;
	}

	public MessageFactory getMessageFactory() {
		return messageFactory;
	}

	public SipProvider getTcpSipProvider() {
		return tcpSipProvider;
	}

	public SipProvider getUdpSipProvider() {
		return udpSipProvider;
	}

}
