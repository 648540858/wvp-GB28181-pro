package com.genersoft.iot.vmp.conf.trace.aspect;

import com.genersoft.iot.vmp.conf.trace.TraceEventFactory;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;
import javax.sip.message.Message;

/**
 * SIP 收发埋点。
 * <p>
 * 只对报警 MESSAGE 生效，业务代码无需感知。
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(prefix = "trace", name = "enabled", havingValue = "true")
public class TraceSipAspect {

    @Autowired
    private TraceEventFactory traceEventFactory;

    /**
     * SIP 发送统一出口，Call-ID 在这里获取，避免侵入 SIPCommander。
     */
    @Before("execution(* com.genersoft.iot.vmp.gb28181.transmit.SIPSender.transmitRequest(..))")
    public void onSipSend(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length < 2 || !(args[1] instanceof Message)) {
            return;
        }
        traceEventFactory.recordSipSend((Message) args[1]);
    }

    /**
     * SIP 请求统一入口。
     * <p>
     * 不能切 AlarmNotifyMessageHandler：它在 afterPropertiesSet 中把目标对象注册到
     * MessageRequestProcessor，后续调用会绕过 AOP 代理。
     */
    @Before("execution(* com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver.processRequest(..))")
    public void onSipRecv(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length < 1 || !(args[0] instanceof RequestEvent)) {
            return;
        }
        traceEventFactory.recordSipRecv((RequestEvent) args[0]);
    }
}
