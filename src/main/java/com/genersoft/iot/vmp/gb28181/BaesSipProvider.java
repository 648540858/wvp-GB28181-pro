package com.genersoft.iot.vmp.gb28181;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.sip.SipProvider;

@Component
public class BaesSipProvider {

    // 注：这里使用注解会导致循环依赖注入，暂用springBean
    @Autowired
    @Lazy
    @Qualifier(value="tcpSipProvider")
    public SipProvider tcpSipProvider;

    // 注：这里使用注解会导致循环依赖注入，暂用springBean
    @Autowired
    @Lazy
    @Qualifier(value="udpSipProvider")
    public SipProvider udpSipProvider;
}
