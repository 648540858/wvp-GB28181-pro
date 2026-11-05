package com.genersoft.iot.vmp.conf.trace.aspect;

import com.genersoft.iot.vmp.conf.trace.TraceEventFactory;
import com.genersoft.iot.vmp.gb28181.bean.AlarmChannelMessage;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.stereotype.Component;

/**
 * Redis 报警消息埋点。
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(prefix = "trace", name = "enabled", havingValue = "true")
public class TraceRedisAspect {

    @Autowired
    private TraceEventFactory traceEventFactory;

    /**
     * 本地 Redis 收到报警。
     */
    @Before("bean(redisAlarmMsgListener) && execution(* onMessage(..))")
    public void onRedisIn(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length < 1 || !(args[0] instanceof Message)) {
            return;
        }
        traceEventFactory.recordRedisIn(((Message) args[0]).getBody());
    }

    /**
     * 报警重新投递到 Redis。
     */
    @Before("bean(redisCatchStorageImpl) && execution(* sendAlarmMsg(..))")
    public void onRedisOut(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length < 1 || !(args[0] instanceof AlarmChannelMessage)) {
            return;
        }
        traceEventFactory.recordRedisOut((AlarmChannelMessage) args[0]);
    }
}
