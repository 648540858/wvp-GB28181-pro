package com.genersoft.iot.vmp.gb28181.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sip.*;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Response;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SipSubscribe {

    private final Logger logger = LoggerFactory.getLogger(SipSubscribe.class);

    private Map<String, SipSubscribe.Event> errorSubscribes = new ConcurrentHashMap<>();

    private Map<String, SipSubscribe.Event> okSubscribes = new ConcurrentHashMap<>();

    private Map<String, Date> timeSubscribes = new ConcurrentHashMap<>();

//    @Scheduled(cron="*/5 * * * * ?")   //每五秒执行一次
//    @Scheduled(fixedRate= 100 * 60 * 60 )
    @Scheduled(cron="0 0 * * * ?")   //每小时执行一次， 每个整点
    public void execute(){
        logger.info("[定时任务] 清理过期的订阅信息");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.HOUR, calendar.get(Calendar.HOUR) - 1);
        for (String key : timeSubscribes.keySet()) {
            if (timeSubscribes.get(key).before(calendar.getTime())){
                logger.info("[定时任务] 清理过期的订阅信息： {}", key);
                errorSubscribes.remove(key);
                okSubscribes.remove(key);
                timeSubscribes.remove(key);
            }
        }
    }

    public interface Event {
        void response(EventResult eventResult);
    }

    public static class EventResult<EventObject>{
        public int statusCode;
        public String type;
        public String msg;
        public String callId;
        public Dialog dialog;
        public EventObject event;

        public EventResult() {
        }

        public EventResult(EventObject event) {
            this.event = event;
            if (event instanceof ResponseEvent) {
                ResponseEvent responseEvent = (ResponseEvent)event;
                Response response = responseEvent.getResponse();
                this.dialog = responseEvent.getDialog();
                this.type = "response";
                if (response != null) {
                    this.msg = response.getReasonPhrase();
                    this.statusCode = response.getStatusCode();
                }
                this.callId = ((CallIdHeader)response.getHeader(CallIdHeader.NAME)).getCallId();

            }else if (event instanceof TimeoutEvent) {
                TimeoutEvent timeoutEvent = (TimeoutEvent)event;
                this.type = "timeout";
                this.msg = "消息超时未回复";
                this.statusCode = -1024;
                this.callId = timeoutEvent.getClientTransaction().getDialog().getCallId().getCallId();
                this.dialog = timeoutEvent.getClientTransaction().getDialog();
            }else if (event instanceof TransactionTerminatedEvent) {
                TransactionTerminatedEvent transactionTerminatedEvent = (TransactionTerminatedEvent)event;
                this.type = "transactionTerminated";
                this.msg = "事务已结束";
                this.statusCode = -1024;
                this.callId = transactionTerminatedEvent.getClientTransaction().getDialog().getCallId().getCallId();
                this.dialog = transactionTerminatedEvent.getClientTransaction().getDialog();
            }else if (event instanceof DialogTerminatedEvent) {
                DialogTerminatedEvent dialogTerminatedEvent = (DialogTerminatedEvent)event;
                this.type = "dialogTerminated";
                this.msg = "会话已结束";
                this.statusCode = -1024;
                this.callId = dialogTerminatedEvent.getDialog().getCallId().getCallId();
                this.dialog = dialogTerminatedEvent.getDialog();
            }
        }
    }

    public void addErrorSubscribe(String key, SipSubscribe.Event event) {
        errorSubscribes.put(key, event);
        timeSubscribes.put(key, new Date());
    }

    public void addOkSubscribe(String key, SipSubscribe.Event event) {
        okSubscribes.put(key, event);
        timeSubscribes.put(key, new Date());
    }

    public SipSubscribe.Event getErrorSubscribe(String key) {
        return errorSubscribes.get(key);
    }

    public void removeErrorSubscribe(String key) {
        errorSubscribes.remove(key);
        timeSubscribes.remove(key);
    }

    public SipSubscribe.Event getOkSubscribe(String key) {
        return okSubscribes.get(key);
    }

    public void removeOkSubscribe(String key) {
        okSubscribes.remove(key);
        timeSubscribes.remove(key);
    }
    public int getErrorSubscribesSize(){
        return errorSubscribes.size();
    }
    public int getOkSubscribesSize(){
        return okSubscribes.size();
    }
}
