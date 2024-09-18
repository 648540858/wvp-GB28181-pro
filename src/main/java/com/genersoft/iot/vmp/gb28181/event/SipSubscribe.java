package com.genersoft.iot.vmp.gb28181.event;

import com.genersoft.iot.vmp.gb28181.bean.DeviceNotFoundEvent;
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
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author lin
 */
@Slf4j
@Component
public class SipSubscribe {

    private final Map<String, SipSubscribe.Event> errorSubscribes = new ConcurrentHashMap<>();

    private final Map<String, SipSubscribe.Event> okSubscribes = new ConcurrentHashMap<>();

    private final Map<String, Instant> okTimeSubscribes = new ConcurrentHashMap<>();

    private final Map<String, Instant> errorTimeSubscribes = new ConcurrentHashMap<>();

    //    @Scheduled(cron="*/5 * * * * ?")   //每五秒执行一次
    //    @Scheduled(fixedRate= 100 * 60 * 60 )
    @Scheduled(cron="0 0/5 * * * ?")   //每5分钟执行一次
    public void execute(){
        if(log.isDebugEnabled()){
            log.info("[定时任务] 清理过期的SIP订阅信息");
        }

        Instant instant = Instant.now().minusMillis(TimeUnit.MINUTES.toMillis(5));

        for (String key : okTimeSubscribes.keySet()) {
            if (okTimeSubscribes.get(key).isBefore(instant)){
                okSubscribes.remove(key);
                okTimeSubscribes.remove(key);
            }
        }
        for (String key : errorTimeSubscribes.keySet()) {
            if (errorTimeSubscribes.get(key).isBefore(instant)){
                errorSubscribes.remove(key);
                errorTimeSubscribes.remove(key);
            }
        }
        if(log.isDebugEnabled()){
            log.debug("okTimeSubscribes.size:{}",okTimeSubscribes.size());
            log.debug("okSubscribes.size:{}",okSubscribes.size());
            log.debug("errorTimeSubscribes.size:{}",errorTimeSubscribes.size());
            log.debug("errorSubscribes.size:{}",errorSubscribes.size());
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
        failedToGetPort
    }

    public static class EventResult<EventObject>{
        public int statusCode;
        public EventResultType type;
        public String msg;
        public String callId;
        public EventObject event;

        public EventResult() {
        }

        public EventResult(EventObject event) {
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

    public void addErrorSubscribe(String key, SipSubscribe.Event event) {
        errorSubscribes.put(key, event);
        errorTimeSubscribes.put(key, Instant.now());
    }

    public void addOkSubscribe(String key, SipSubscribe.Event event) {
        okSubscribes.put(key, event);
        okTimeSubscribes.put(key, Instant.now());
    }

    public SipSubscribe.Event getErrorSubscribe(String key) {
        return errorSubscribes.get(key);
    }

    public void removeErrorSubscribe(String key) {
        if(key == null){
            return;
        }
        errorSubscribes.remove(key);
        errorTimeSubscribes.remove(key);
    }

    public SipSubscribe.Event getOkSubscribe(String key) {
        return okSubscribes.get(key);
    }

    public void removeOkSubscribe(String key) {
        if(key == null){
            return;
        }
        okSubscribes.remove(key);
        okTimeSubscribes.remove(key);
    }
    public int getErrorSubscribesSize(){
        return errorSubscribes.size();
    }
    public int getOkSubscribesSize(){
        return okSubscribes.size();
    }
}
