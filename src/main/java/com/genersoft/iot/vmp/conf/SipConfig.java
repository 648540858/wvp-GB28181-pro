package com.genersoft.iot.vmp.conf;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration("sipConfig")
public class SipConfig {

	@Value("${sip.ip}")
	String sipIp;
	@Value("${sip.port}")
	Integer sipPort;
	@Value("${sip.domain}")
	String sipDomain;
	@Value("${sip.id}")
	String sipId;
	@Value("${sip.password}")
	String sipPassword;
	
	@Value("${sip.ptz.speed:50}")
	Integer speed;

	public String getSipIp() {
		return sipIp;
	}

	public void setSipIp(String sipIp) {
		this.sipIp = sipIp;
	}

	public Integer getSipPort() {
		return sipPort;
	}

	public void setSipPort(Integer sipPort) {
		this.sipPort = sipPort;
	}

	public String getSipDomain() {
		return sipDomain;
	}

	public void setSipDomain(String sipDomain) {
		this.sipDomain = sipDomain;
	}

	public String getSipId() {
		return sipId;
	}

	public void setSipId(String sipId) {
		this.sipId = sipId;
	}

	public String getSipPassword() {
		return sipPassword;
	}

	public void setSipPassword(String sipPassword) {
		this.sipPassword = sipPassword;
	}

	public Integer getSpeed() {
		return speed;
	}

	public void setSpeed(Integer speed) {
		this.speed = speed;
	}
}
