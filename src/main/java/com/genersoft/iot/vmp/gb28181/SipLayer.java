package com.genersoft.iot.vmp.gb28181;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.GbStringMsgParserFactory;
import com.genersoft.iot.vmp.gb28181.conf.DefaultProperties;
import com.genersoft.iot.vmp.gb28181.transmit.ISIPProcessorObserver;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.SipStackImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(value=10)
public class SipLayer implements CommandLineRunner {

	private final static Logger logger = LoggerFactory.getLogger(SipLayer.class);

	@Autowired
	private SipConfig sipConfig;

	@Autowired
	private ISIPProcessorObserver sipProcessorObserver;

	@Autowired
	private UserSetting userSetting;

	private final Map<String, SipProviderImpl> tcpSipProviderMap = new ConcurrentHashMap<>();
	private final Map<String, SipProviderImpl> udpSipProviderMap = new ConcurrentHashMap<>();

	@Override
	public void run(String... args) {
		List<String> monitorIps = new ArrayList<>();
		// 使用逗号分割多个ip
		String separator = ",";
		if (sipConfig.getIp().indexOf(separator) > 0) {
			String[] split = sipConfig.getIp().split(separator);
			monitorIps.addAll(Arrays.asList(split));
		}else {
			monitorIps.add(sipConfig.getIp());
		}

		SipFactory.getInstance().setPathName("gov.nist");
		if (monitorIps.size() > 0) {
			for (String monitorIp : monitorIps) {
				addListeningPoint(monitorIp, sipConfig.getPort());
			}
			if (udpSipProviderMap.size() + tcpSipProviderMap.size() == 0) {
				System.exit(1);
			}
		}
	}

	private void addListeningPoint(String monitorIp, int port){
		SipStackImpl sipStack;
		try {
			sipStack = (SipStackImpl)SipFactory.getInstance().createSipStack(DefaultProperties.getProperties("GB28181_SIP", userSetting.getSipLog()));
			sipStack.setMessageParserFactory(new GbStringMsgParserFactory());
		} catch (PeerUnavailableException e) {
			logger.error("[SIP SERVER] SIP服务启动失败， 监听地址{}失败,请检查ip是否正确", monitorIp);
			return;
		}

		try {
			ListeningPoint tcpListeningPoint = sipStack.createListeningPoint(monitorIp, port, "TCP");
			SipProviderImpl tcpSipProvider = (SipProviderImpl)sipStack.createSipProvider(tcpListeningPoint);

			tcpSipProvider.setDialogErrorsAutomaticallyHandled();
			tcpSipProvider.addSipListener(sipProcessorObserver);
			tcpSipProviderMap.put(monitorIp, tcpSipProvider);
			logger.info("[SIP SERVER] tcp://{}:{} 启动成功", monitorIp, port);
		} catch (TransportNotSupportedException
				 | TooManyListenersException
				 | ObjectInUseException
				 | InvalidArgumentException e) {
			logger.error("[SIP SERVER] tcp://{}:{} SIP服务启动失败,请检查端口是否被占用或者ip是否正确"
					, monitorIp, port);
		}

		try {
			ListeningPoint udpListeningPoint = sipStack.createListeningPoint(monitorIp, port, "UDP");

			SipProviderImpl udpSipProvider = (SipProviderImpl)sipStack.createSipProvider(udpListeningPoint);
			udpSipProvider.addSipListener(sipProcessorObserver);

			udpSipProviderMap.put(monitorIp, udpSipProvider);

			logger.info("[SIP SERVER] udp://{}:{} 启动成功", monitorIp, port);
		} catch (TransportNotSupportedException
				 | TooManyListenersException
				 | ObjectInUseException
				 | InvalidArgumentException e) {
			logger.error("[SIP SERVER] udp://{}:{} SIP服务启动失败,请检查端口是否被占用或者ip是否正确"
					, monitorIp, port);
		}
	}

	public SipProviderImpl getUdpSipProvider(String ip) {
		if (ObjectUtils.isEmpty(ip)) {
			return null;
		}
		return udpSipProviderMap.get(ip);
	}

	public SipProviderImpl getUdpSipProvider() {
		if (udpSipProviderMap.size() != 1) {
			return null;
		}
		return udpSipProviderMap.values().stream().findFirst().get();
	}

	public SipProviderImpl getTcpSipProvider() {
		if (tcpSipProviderMap.size() != 1) {
			return null;
		}
		return tcpSipProviderMap.values().stream().findFirst().get();
	}

	public SipProviderImpl getTcpSipProvider(String ip) {
		if (ObjectUtils.isEmpty(ip)) {
			return null;
		}
		return tcpSipProviderMap.get(ip);
	}

	public String getLocalIp(String deviceLocalIp) {
		if (!ObjectUtils.isEmpty(deviceLocalIp)) {
			return deviceLocalIp;
		}
		return getUdpSipProvider().getListeningPoint().getIPAddress();
	}
}
