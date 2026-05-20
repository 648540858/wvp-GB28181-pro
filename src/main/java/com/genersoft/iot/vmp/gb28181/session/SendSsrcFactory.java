package com.genersoft.iot.vmp.gb28181.session;

import com.genersoft.iot.vmp.conf.SipConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class SendSsrcFactory {

    @Autowired
    private SipConfig sipConfig;

    private String domainPart;

    @PostConstruct
    public void init() {
        String sipDomain = sipConfig.getDomain();
        domainPart = sipDomain.length() >= 8 ? sipDomain.substring(3, 8) : sipDomain;
    }

    public String getSendSsrc(String prefix) {
        return prefix + domainPart + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
