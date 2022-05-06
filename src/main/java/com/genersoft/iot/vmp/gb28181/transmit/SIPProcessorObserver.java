package com.genersoft.iot.vmp.gb28181.transmit;

import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.response.ISIPResponseProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.timeout.ITimeoutProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.sip.*;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Response;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description: SIP信令处理类观察者
 * @author: panlinlin
 * @date:   2021年11月5日 下午15：32
 */
@Component
public class SIPProcessorObserver implements ISIPProcessorObserver {

    private final static Logger logger = LoggerFactory.getLogger(SIPProcessorObserver.class);

    private static Map<String, ISIPRequestProcessor> requestProcessorMap = new ConcurrentHashMap<>();
    private static Map<String, ISIPResponseProcessor> responseProcessorMap = new ConcurrentHashMap<>();
    private static ITimeoutProcessor timeoutProcessor;

    @Autowired
    private SipSubscribe sipSubscribe;


//    @Autowired
//    @Qualifier(value = "taskExecutor")
//    private ThreadPoolTaskExecutor poolTaskExecutor;

    /**
     * 添加 request订阅
     * @param method 方法名
     * @param processor 处理程序
     */
    public void addRequestProcessor(String method, ISIPRequestProcessor processor) {
        requestProcessorMap.put(method, processor);
    }

    /**
     * 添加 response订阅
     * @param method 方法名
     * @param processor 处理程序
     */
    public void addResponseProcessor(String method, ISIPResponseProcessor processor) {
        responseProcessorMap.put(method, processor);
    }

    /**
     * 添加 超时事件订阅
     * @param processor 处理程序
     */
    public void addTimeoutProcessor(ITimeoutProcessor processor) {
        timeoutProcessor = processor;
    }

    /**
     * 分发RequestEvent事件
     * @param requestEvent RequestEvent事件
     */
    @Override
    @Async
    public void processRequest(RequestEvent requestEvent) {
        logger.debug("\n收到请求：\n{}", requestEvent.getRequest());
        String method = requestEvent.getRequest().getMethod();
        ISIPRequestProcessor sipRequestProcessor = requestProcessorMap.get(method);
        if (sipRequestProcessor == null) {
            logger.warn("不支持方法{}的request", method);
            return;
        }
        requestProcessorMap.get(method).process(requestEvent);

    }

    /**
     * 分发ResponseEvent事件
     * @param responseEvent responseEvent事件
     */
    @Override
    @Async
    public void processResponse(ResponseEvent responseEvent) {
        Response response = responseEvent.getResponse();
        logger.debug("\n收到响应：\n{}", responseEvent.getResponse());
        int status = response.getStatusCode();

        if (((status >= 200) && (status < 300)) || status == Response.UNAUTHORIZED) { // Success!
            CSeqHeader cseqHeader = (CSeqHeader) responseEvent.getResponse().getHeader(CSeqHeader.NAME);
            String method = cseqHeader.getMethod();
            ISIPResponseProcessor sipRequestProcessor = responseProcessorMap.get(method);
            if (sipRequestProcessor != null) {
                sipRequestProcessor.process(responseEvent);
            }
            if (status != Response.UNAUTHORIZED && responseEvent.getResponse() != null && sipSubscribe.getOkSubscribesSize() > 0 ) {
                CallIdHeader callIdHeader = (CallIdHeader)responseEvent.getResponse().getHeader(CallIdHeader.NAME);
                if (callIdHeader != null) {
                    SipSubscribe.Event subscribe = sipSubscribe.getOkSubscribe(callIdHeader.getCallId());
                    if (subscribe != null) {
                        SipSubscribe.EventResult eventResult = new SipSubscribe.EventResult(responseEvent);
                        sipSubscribe.removeOkSubscribe(callIdHeader.getCallId());
                        subscribe.response(eventResult);
                    }
                }
            }
        } else if ((status >= 100) && (status < 200)) {
            // 增加其它无需回复的响应，如101、180等
        } else {
            logger.warn("接收到失败的response响应！status：" + status + ",message:" + response.getReasonPhrase()/* .getContent().toString()*/);
            if (responseEvent.getResponse() != null && sipSubscribe.getErrorSubscribesSize() > 0 ) {
                CallIdHeader callIdHeader = (CallIdHeader)responseEvent.getResponse().getHeader(CallIdHeader.NAME);
                if (callIdHeader != null) {
                    SipSubscribe.Event subscribe = sipSubscribe.getErrorSubscribe(callIdHeader.getCallId());
                    if (subscribe != null) {
                        SipSubscribe.EventResult eventResult = new SipSubscribe.EventResult(responseEvent);
                        subscribe.response(eventResult);
                        sipSubscribe.removeErrorSubscribe(callIdHeader.getCallId());
                    }
                }
            }
            if (responseEvent.getDialog() != null) {
                responseEvent.getDialog().delete();
            }
        }


    }

    /**
     * 向超时订阅发送消息
     * @param timeoutEvent timeoutEvent事件
     */
    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {
        if(timeoutProcessor != null) {
            timeoutProcessor.process(timeoutEvent);
        }
    }

    @Override
    public void processIOException(IOExceptionEvent exceptionEvent) {
        System.out.println("processIOException");
    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
//        Transaction transaction = null;
//        System.out.println("processTransactionTerminated");
//        if (transactionTerminatedEvent.isServerTransaction()) {
//            transaction = transactionTerminatedEvent.getServerTransaction();
//        }else {
//            transaction = transactionTerminatedEvent.getClientTransaction();
//        }
//
//        System.out.println(transaction.getBranchId());
//        System.out.println(transaction.getState());
//        System.out.println(transaction.getRequest().getMethod());
//        CallIdHeader header = (CallIdHeader)transaction.getRequest().getHeader(CallIdHeader.NAME);
//        SipSubscribe.EventResult<TransactionTerminatedEvent> terminatedEventEventResult = new SipSubscribe.EventResult<>(transactionTerminatedEvent);

//        sipSubscribe.getErrorSubscribe(header.getCallId()).response(terminatedEventEventResult);
    }

    @Override
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
        CallIdHeader callId = dialogTerminatedEvent.getDialog().getCallId();
    }


}
