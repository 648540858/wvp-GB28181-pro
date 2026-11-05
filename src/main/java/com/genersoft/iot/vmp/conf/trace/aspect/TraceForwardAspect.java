package com.genersoft.iot.vmp.conf.trace.aspect;

import com.genersoft.iot.vmp.conf.trace.TraceEventFactory;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarmNotify;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 报警转发埋点。
 * <p>
 * 在 commander 调用前记录 FORWARD，业务代码不需要写 traceForward。
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(prefix = "trace", name = "enabled", havingValue = "true")
public class TraceForwardAspect {

    @Autowired
    private TraceEventFactory traceEventFactory;

    @Before("execution(* com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderForPlatform.sendAlarmMessage(..))")
    public void onPlatformForward(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length < 2 || !(args[0] instanceof Platform) || !(args[1] instanceof DeviceAlarmNotify)) {
            return;
        }
        Platform platform = (Platform) args[0];
        DeviceAlarmNotify alarm = (DeviceAlarmNotify) args[1];
        traceEventFactory.recordForward("platform",
                platform.getServerGBId() + "/" + platform.getDeviceGBId(), alarm);
    }

    @Before("execution(* com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander.sendAlarmMessage(..))")
    public void onDeviceForward(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length < 2 || !(args[0] instanceof Device) || !(args[1] instanceof DeviceAlarmNotify)) {
            return;
        }
        Device device = (Device) args[0];
        DeviceAlarmNotify alarm = (DeviceAlarmNotify) args[1];
        traceEventFactory.recordForward("device", device.getDeviceId(), alarm);
    }
}
