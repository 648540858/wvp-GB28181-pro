package com.genersoft.iot.vmp.conf;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration("sipConfig")
@Data
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
}
