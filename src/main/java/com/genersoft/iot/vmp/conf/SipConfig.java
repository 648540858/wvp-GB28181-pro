package com.genersoft.iot.vmp.conf;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration("sipConfig")
public class SipConfig {

	@Value("${sip.ip}")
	private String sipIp;
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

}
