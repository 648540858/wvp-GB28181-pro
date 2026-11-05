package com.genersoft.iot.vmp.conf;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "sip", ignoreInvalidFields = true)
@Order(0)
@Data
public class SipConfig {

	private String ip;

	private String showIp;

	private List<String> monitorIps;

	private Integer port;

	private String domain;

	private String id;

	private String password;

	private boolean alarm = true;

	private long timeout = 1000;

	/**
	 * SIP(TCP) 是否使用协议栈的 NIO 消息处理器。
	 * 默认 false：使用协议栈默认的阻塞 IO(TCPMessageProcessor)，与历史稳定版本一致。
	 * 注意：不要使用带同步连通性预检的自定义处理器，它会在协议栈分发线程上阻塞建链、失败时直接丢弃消息。
	 */
	private boolean nioMessageProcessor = false;
}
