package com.genersoft.iot.vmp.gb28181.event;

import com.genersoft.iot.vmp.gb28181.bean.DeviceNotFoundEvent;
import com.genersoft.iot.vmp.gb28181.event.sip.SipEvent;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sip.DialogTerminatedEvent;
import javax.sip.ResponseEvent;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.header.WarningHeader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;

/**
 * @author lin
 */
@Slf4j
@Component
public class SipSubscribe {

    private final Map<String, SipEvent> subscribes = new ConcurrentHashMap<>();

    private final DelayQueue<SipEvent> delayQueue = new DelayQueue<>();


    @Scheduled(fixedDelay = 200)   //每200毫秒执行
    public void execute(){
        while (!delayQueue.isEmpty()) {
            try {
                SipEvent take = delayQueue.take();
                // 出现超时异常
                if(take.getErrorEvent() != null) {
                    EventResult<Object> eventResult = new EventResult<>();
                    eventResult.type = EventResultType.timeout;
                    eventResult.msg = "消息超时未回复";
                    eventResult.statusCode = -1024;
                    take.getErrorEvent().response(eventResult);
                }
                subscribes.remove(take.getKey());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void updateTimeout(String callId) {
        SipEvent sipEvent = subscribes.get(callId);
        if (sipEvent != null) {
            delayQueue.remove(sipEvent);
            delayQueue.offer(sipEvent);
        }
    }

    public interface Event { void response(EventResult eventResult);
    }

    /**
     *
     */
    public enum EventResultType{
        // 超时
        timeout,
        // 回复
        response,
        // 事务已结束
        transactionTerminated,
        // 会话已结束
        dialogTerminated,
        // 设备未找到
        deviceNotFoundEvent,
        // 消息发送失败
        cmdSendFailEvent,
        // 消息发送失败
        failedToGetPort,
        // 收到失败的回复
        failedResult
    }

    public static class EventResult<T>{
        public int statusCode;
        public EventResultType type;
        public String msg;
        public String callId;
        public T event;

        public EventResult() {
        }

        public EventResult(T event) {
            this.event = event;
            if (event instanceof ResponseEvent) {
                ResponseEvent responseEvent = (ResponseEvent)event;
                SIPResponse response = (SIPResponse)responseEvent.getResponse();
                this.type = EventResultType.response;
                if (response != null) {
                    WarningHeader warningHeader = (WarningHeader)response.getHeader(WarningHeader.NAME);
                    if (warningHeader != null && !ObjectUtils.isEmpty(warningHeader.getText())) {
                        this.msg = "";
                        if (warningHeader.getCode() > 0) {
                            this.msg += warningHeader.getCode() + ":";
                        }
                        if (warningHeader.getAgent() != null) {
                            this.msg += warningHeader.getCode() + ":";
                        }
                        if (warningHeader.getText() != null) {
                            this.msg += warningHeader.getText();
                        }
                    }else {
                        this.msg = response.getReasonPhrase();
                    }
                    this.statusCode = response.getStatusCode();
                    this.callId = response.getCallIdHeader().getCallId();
                }
            }else if (event instanceof TimeoutEvent) {
                TimeoutEvent timeoutEvent = (TimeoutEvent)event;
                this.type = EventResultType.timeout;
                this.msg = "消息超时未回复";
                this.statusCode = -1024;
                if (timeoutEvent.isServerTransaction()) {
                    this.callId = ((SIPRequest)timeoutEvent.getServerTransaction().getRequest()).getCallIdHeader().getCallId();
                }else {
                    this.callId = ((SIPRequest)timeoutEvent.getClientTransaction().getRequest()).getCallIdHeader().getCallId();
                }
            }else if (event instanceof TransactionTerminatedEvent) {
                TransactionTerminatedEvent transactionTerminatedEvent = (TransactionTerminatedEvent)event;
                this.type = EventResultType.transactionTerminated;
                this.msg = "事务已结束";
                this.statusCode = -1024;
                if (transactionTerminatedEvent.isServerTransaction()) {
                    this.callId = ((SIPRequest)transactionTerminatedEvent.getServerTransaction().getRequest()).getCallIdHeader().getCallId();
                }else {
                    this.callId = ((SIPRequest)transactionTerminatedEvent.getClientTransaction().getRequest()).getCallIdHeader().getCallId();
                }
            }else if (event instanceof DialogTerminatedEvent) {
                DialogTerminatedEvent dialogTerminatedEvent = (DialogTerminatedEvent)event;
                this.type = EventResultType.dialogTerminated;
                this.msg = "会话已结束";
                this.statusCode = -1024;
                this.callId = dialogTerminatedEvent.getDialog().getCallId().getCallId();
            }else if (event instanceof DeviceNotFoundEvent) {
                this.type = EventResultType.deviceNotFoundEvent;
                this.msg = "设备未找到";
                this.statusCode = -1024;
                this.callId = ((DeviceNotFoundEvent) event).getCallId();
            }
        }
    }


    public void addSubscribe(String key, SipEvent event) {
        SipEvent sipEvent = subscribes.get(key);
        if (sipEvent != null) {
            subscribes.remove(key);
            delayQueue.remove(sipEvent);
        }
        subscribes.put(key, event);
        delayQueue.offer(event);
    }

    public SipEvent getSubscribe(String key) {
        return subscribes.get(key);
    }

    public void removeSubscribe(String key) {
        if(key == null){
            return;
        }
        SipEvent sipEvent = subscribes.get(key);
        if (sipEvent != null) {
            subscribes.remove(key);
            delayQueue.remove(sipEvent);
        }
    }

    public boolean isEmpty(){
        return subscribes.isEmpty();
    }

    public Integer size() {
        return subscribes.size();
    }
}
