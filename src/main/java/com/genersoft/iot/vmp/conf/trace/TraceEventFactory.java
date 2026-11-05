package com.genersoft.iot.vmp.conf.trace;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.gb28181.bean.AlarmChannelMessage;
import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarmNotify;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Message;
import java.nio.charset.StandardCharsets;

/**
 * 统一构造链路追踪事件。
 * <p>
 * 所有埋点都通过 AOP 调用这里，业务代码不直接依赖 TraceEvent。
 */
@Slf4j
@Component
public class TraceEventFactory {

    @Autowired
    private TracePublisher tracePublisher;

    public void recordSipSend(Message message) {
        if (!tracePublisher.isEnabled() || !(message instanceof SIPRequest)) {
            return;
        }
        try {
            SIPRequest request = (SIPRequest) message;
            String body = getBody(request);
            if (!isAlarmMessage(request, body)) {
                return;
            }
            DeviceAlarmNotify alarm = parseAlarm(body);
            if (alarm == null) {
                return;
            }
            CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
            tracePublisher.publish(TraceEvent.builder()
                    .traceId(TraceIdUtil.extract(alarm.getAlarmDescription()))
                    .stage("SIP_SEND")
                    .targetType("sip")
                    .target(getTarget(request))
                    .gbId(alarm.getChannelId())
                    .alarmSn(alarm.getAlarmMethod())
                    .alarmType(alarm.getAlarmType())
                    .description(alarm.getAlarmDescription())
                    .sipCallId(callIdHeader != null ? callIdHeader.getCallId() : null)
                    .direction("OUT")
                    .build());
        } catch (Exception e) {
            log.debug("[Trace] 记录 SIP_SEND 失败: {}", e.getMessage());
        }
    }

    public void recordSipRecv(RequestEvent evt) {
        if (!tracePublisher.isEnabled() || evt == null || !(evt.getRequest() instanceof SIPRequest)) {
            return;
        }
        try {
            SIPRequest request = (SIPRequest) evt.getRequest();
            String body = getBody(request);
            if (!isAlarmMessage(request, body)) {
                return;
            }
            DeviceAlarmNotify alarm = parseAlarm(body);
            if (alarm == null) {
                return;
            }
            CallIdHeader callIdHeader = request.getCallIdHeader();
            tracePublisher.publish(TraceEvent.builder()
                    .traceId(TraceIdUtil.extract(alarm.getAlarmDescription()))
                    .stage("SIP_RECV")
                    .targetType("sip")
                    .target(SipUtils.getUserIdFromFromHeader(request))
                    .gbId(alarm.getChannelId())
                    .alarmSn(alarm.getAlarmMethod())
                    .alarmType(alarm.getAlarmType())
                    .description(alarm.getAlarmDescription())
                    .sipCallId(callIdHeader != null ? callIdHeader.getCallId() : null)
                    .direction("IN")
                    .build());
        } catch (Exception e) {
            log.debug("[Trace] 记录 SIP_RECV 失败: {}", e.getMessage());
        }
    }

    public void recordRedisIn(byte[] body) {
        if (!tracePublisher.isEnabled() || body == null) {
            return;
        }
        try {
            AlarmChannelMessage msg = JSON.parseObject(new String(body, StandardCharsets.UTF_8), AlarmChannelMessage.class);
            if (msg == null) {
                return;
            }
            tracePublisher.publish(TraceEvent.builder()
                    .traceId(TraceIdUtil.extract(msg.getAlarmDescription()))
                    .stage("REDIS_IN")
                    .gbId(msg.getGbId())
                    .alarmSn(msg.getAlarmSn())
                    .alarmType(msg.getAlarmType())
                    .description(msg.getAlarmDescription())
                    .direction("IN")
                    .build());
        } catch (Exception e) {
            log.debug("[Trace] 记录 REDIS_IN 失败: {}", e.getMessage());
        }
    }

    public void recordRedisOut(AlarmChannelMessage msg) {
        if (!tracePublisher.isEnabled() || msg == null) {
            return;
        }
        try {
            tracePublisher.publish(TraceEvent.builder()
                    .traceId(TraceIdUtil.extract(msg.getAlarmDescription()))
                    .stage("REDIS_OUT")
                    .gbId(msg.getGbId())
                    .alarmSn(msg.getAlarmSn())
                    .alarmType(msg.getAlarmType())
                    .description(msg.getAlarmDescription())
                    .direction("OUT")
                    .build());
        } catch (Exception e) {
            log.debug("[Trace] 记录 REDIS_OUT 失败: {}", e.getMessage());
        }
    }

    public void recordForward(String targetType, String target, DeviceAlarmNotify alarm) {
        if (!tracePublisher.isEnabled() || alarm == null) {
            return;
        }
        try {
            tracePublisher.publish(TraceEvent.builder()
                    .traceId(TraceIdUtil.extract(alarm.getAlarmDescription()))
                    .stage("FORWARD")
                    .targetType(targetType)
                    .target(target)
                    .gbId(alarm.getChannelId())
                    .alarmSn(alarm.getAlarmMethod())
                    .alarmType(alarm.getAlarmType())
                    .description(alarm.getAlarmDescription())
                    .direction("OUT")
                    .build());
        } catch (Exception e) {
            log.debug("[Trace] 记录 FORWARD 失败: {}", e.getMessage());
        }
    }

    private boolean isAlarmMessage(SIPRequest request, String body) {
        return request != null
                && "MESSAGE".equalsIgnoreCase(request.getMethod())
                && body != null
                && body.contains("<CmdType>Alarm</CmdType>");
    }

    private String getBody(SIPRequest request) {
        Object content = request.getContent();
        if (content == null) {
            return null;
        }
        if (content instanceof byte[]) {
            return new String((byte[]) content, StandardCharsets.UTF_8);
        }
        return content.toString();
    }

    private DeviceAlarmNotify parseAlarm(String body) throws Exception {
        Document document = DocumentHelper.parseText(body);
        Element root = document.getRootElement();
        return DeviceAlarmNotify.fromXml(root);
    }

    private String getTarget(SIPRequest request) {
        try {
            SipUri uri = (SipUri) request.getRequestLine().getUri();
            String user = uri.getUser();
            if (user != null && !user.isEmpty()) {
                return user;
            }
        } catch (Exception ignored) {
        }
        return request.getRequestURI() != null ? request.getRequestURI().toString() : "";
    }
}
