package com.genersoft.iot.vmp.gb28181.transmit;

import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipProvider;
import javax.sip.header.CSeqHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.gb28181.transmit.response.impl.*;
import com.genersoft.iot.vmp.service.IPlayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.auth.RegisterLogicHandler;
import com.genersoft.iot.vmp.gb28181.event.DeviceOffLineDetector;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.request.impl.AckRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.request.impl.ByeRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.request.impl.CancelRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.request.impl.InviteRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.request.impl.MessageRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.request.impl.NotifyRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.request.impl.OtherRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.request.impl.RegisterRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.request.impl.SubscribeRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.response.ISIPResponseProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.response.impl.ByeResponseProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.response.impl.CancelResponseProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.response.impl.InviteResponseProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.response.impl.OtherResponseProcessor;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.utils.SpringBeanFactory;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;

/**    
 * @Description: SIP信令处理分配
 * @author: swwheihei
 * @date:   2020年5月3日 下午4:24:37     
 */
@Component
public class SIPProcessorFactory {
	
	// private final static Logger logger = LoggerFactory.getLogger(SIPProcessorFactory.class);
	
	@Autowired
	private SipConfig sipConfig;
	
	@Autowired
	private RegisterLogicHandler handler;
	
	@Autowired
	private IVideoManagerStorager storager;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;

	@Autowired
	private EventPublisher publisher;
	
	@Autowired
	private SIPCommander cmder;

	@Autowired
	private SIPCommanderFroPlatform cmderFroPlatform;

	@Autowired
	private RedisUtil redis;
	
	@Autowired
	private DeferredResultHolder deferredResultHolder;
	
	@Autowired
	private DeviceOffLineDetector offLineDetector;
	
	@Autowired
	private InviteResponseProcessor inviteResponseProcessor;
	
	@Autowired
	private ByeResponseProcessor byeResponseProcessor;
	
	@Autowired
	private CancelResponseProcessor cancelResponseProcessor;

	@Autowired
	@Lazy
	private RegisterResponseProcessor registerResponseProcessor;


	@Autowired
	private OtherResponseProcessor otherResponseProcessor;

	@Autowired
	private IPlayService playService;

	@Autowired
	private ZLMRTPServerFactory zlmrtpServerFactory;

	@Autowired
	private IMediaServerService mediaServerService;

	// 注：这里使用注解会导致循环依赖注入，暂用springBean
	private SipProvider tcpSipProvider;
		
	// 注：这里使用注解会导致循环依赖注入，暂用springBean
	private SipProvider udpSipProvider;
	
	public ISIPRequestProcessor createRequestProcessor(RequestEvent evt) {
		Request request = evt.getRequest();
		String method = request.getMethod();
//		logger.info("接收到消息："+request.getMethod());
//		sipSubscribe.getSubscribe(evt.getServerTransaction().getBranchId()).response(evt);
		if (Request.INVITE.equals(method)) {
			InviteRequestProcessor processor = new InviteRequestProcessor();
			processor.setRequestEvent(evt);
			processor.setTcpSipProvider(getTcpSipProvider());
			processor.setUdpSipProvider(getUdpSipProvider());

			processor.setCmder(cmder);
			processor.setCmderFroPlatform(cmderFroPlatform);
			processor.setPlayService(playService);
			processor.setStorager(storager);
			processor.setRedisCatchStorage(redisCatchStorage);
			processor.setZlmrtpServerFactory(zlmrtpServerFactory);
			processor.setMediaServerService(mediaServerService);
			return processor;
		} else if (Request.REGISTER.equals(method)) {
			RegisterRequestProcessor processor = new RegisterRequestProcessor();
			processor.setRequestEvent(evt);
			processor.setTcpSipProvider(getTcpSipProvider());
			processor.setUdpSipProvider(getUdpSipProvider());
			processor.setHandler(handler);
			processor.setPublisher(publisher);
			processor.setSipConfig(sipConfig);
			processor.setVideoManagerStorager(storager);
			return processor;
		} else if (Request.SUBSCRIBE.equals(method)) {
			SubscribeRequestProcessor processor = new SubscribeRequestProcessor();
			processor.setRequestEvent(evt);
			return processor;
		} else if (Request.ACK.equals(method)) {
			AckRequestProcessor processor = new AckRequestProcessor();
			processor.setRequestEvent(evt);
			processor.setRedisCatchStorage(redisCatchStorage);
			processor.setZlmrtpServerFactory(zlmrtpServerFactory);
			processor.setMediaServerService(mediaServerService);
			return processor;
		} else if (Request.BYE.equals(method)) {
			ByeRequestProcessor processor = new ByeRequestProcessor();
			processor.setRequestEvent(evt);
			processor.setRedisCatchStorage(redisCatchStorage);
			processor.setZlmrtpServerFactory(zlmrtpServerFactory);
			processor.setSIPCommander(cmder);
			processor.setMediaServerService(mediaServerService);
			return processor;
		} else if (Request.CANCEL.equals(method)) {
			CancelRequestProcessor processor = new CancelRequestProcessor();
			processor.setRequestEvent(evt);
			return processor;
		} else if (Request.MESSAGE.equals(method)) {
			MessageRequestProcessor processor = new MessageRequestProcessor();
			processor.setRequestEvent(evt);
			processor.setTcpSipProvider(getTcpSipProvider());
			processor.setUdpSipProvider(getUdpSipProvider());
			processor.setPublisher(publisher);
			processor.setRedis(redis);
			processor.setDeferredResultHolder(deferredResultHolder);
			processor.setOffLineDetector(offLineDetector);
			processor.setCmder(cmder);
			processor.setCmderFroPlatform(cmderFroPlatform);
			processor.setStorager(storager);
			processor.setRedisCatchStorage(redisCatchStorage);
			return processor;
		} else if (Request.NOTIFY.equalsIgnoreCase(method)) {
			NotifyRequestProcessor processor = new NotifyRequestProcessor();
			processor.setRequestEvent(evt);
			processor.setTcpSipProvider(getTcpSipProvider());
			processor.setUdpSipProvider(getUdpSipProvider());
			processor.setPublisher(publisher);
			processor.setRedis(redis);
			processor.setDeferredResultHolder(deferredResultHolder);
			processor.setOffLineDetector(offLineDetector);
			processor.setCmder(cmder);
			processor.setStorager(storager);
			processor.setRedisCatchStorage(redisCatchStorage);
			return processor;
		} else {
			OtherRequestProcessor processor = new OtherRequestProcessor();
			processor.setRequestEvent(evt);
			return processor;
		}
	}
	
	public ISIPResponseProcessor createResponseProcessor(ResponseEvent evt) {

		Response response = evt.getResponse();
		CSeqHeader cseqHeader = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
		String method = cseqHeader.getMethod();
		if(Request.INVITE.equals(method)){
			return inviteResponseProcessor;
		} else if (Request.BYE.equals(method)) {
			return byeResponseProcessor;
		} else if (Request.CANCEL.equals(method)) {
			return cancelResponseProcessor;
		}else if (Request.REGISTER.equals(method)) {
			return registerResponseProcessor;
		} else {
			return otherResponseProcessor;
		}
	}
	
	private SipProvider getTcpSipProvider() {
		if (tcpSipProvider == null) {
			tcpSipProvider = (SipProvider) SpringBeanFactory.getBean("tcpSipProvider");
		}
		return tcpSipProvider;
	}
	
	private SipProvider getUdpSipProvider() {
		if (udpSipProvider == null) {
			udpSipProvider = (SipProvider) SpringBeanFactory.getBean("udpSipProvider");
		}
		return udpSipProvider;
	}
	
}
