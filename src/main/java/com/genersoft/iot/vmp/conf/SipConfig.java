package com.genersoft.iot.vmp.conf;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration("sipConfig")
public class SipConfig {

	@Value("${sip.ip}")
	private String sipIp;

	/**
	 * 默认使用sip.ip
	 */
	@Value("${sip.monitor-ip:0.0.0.0}")
	private String monitorIp;

	@Value("${sip.port}")
	private Integer sipPort;

	@Value("${sip.domain}")
	private String sipDomain;

	@Value("${sip.id}")
	private String sipId;

	@Value("${sip.password}")
	private String sipPassword;
	
	@Value("${sip.ptz.speed:50}")
	Integer speed;

	@Value("${sip.keepalive-timeout:180}")
	Integer keepaliveTimeOut;

	@Value("${sip.register-time-interval:60}")
	Integer registerTimeInterval;

	public String getMonitorIp() {
		return monitorIp;
	}

	public String getSipIp() {
		return sipIp;
	}


	public Integer getSipPort() {
		return sipPort;
	}


	public String getSipDomain() {
		return sipDomain;
	}


	public String getSipId() {
		return sipId;
	}

	public String getSipPassword() {
		return sipPassword;
	}


	public Integer getSpeed() {
		return speed;
	}

	public Integer getKeepaliveTimeOut() {
		return keepaliveTimeOut;
	}

	public Integer getRegisterTimeInterval() {
		return registerTimeInterval;
	}
}
