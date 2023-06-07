package com.genersoft.iot.vmp.sip.service.Impl;


import com.genersoft.iot.vmp.sip.bean.SipEvent;
import com.genersoft.iot.vmp.sip.service.ISIPResponseProcessor;
import com.genersoft.iot.vmp.sip.service.SipSubscribe;
import gov.nist.javax.sip.message.SIPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.sip.*;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Response;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 监听sip消息
 * @author lin
 */

@Service
public class SipListenerImpl implements SipListener {


    private final static Logger log = LoggerFactory.getLogger(SipListenerImpl.class);

    private Map<String, ISIPResponseProcessor> responseProcessorMap = new ConcurrentHashMap<>();

    @Override
    @Async("taskExecutor")
    public void processRequest(RequestEvent requestEvent) {
        String method = requestEvent.getRequest().getMethod();
        // TODO 暂不处理
//        if (method.equalsIgnoreCase("")) {
//
//        }

    }

    @Override
    @Async("taskExecutor")
    public void processResponse(ResponseEvent responseEvent) {
        SIPResponse response = (SIPResponse) responseEvent.getResponse();
        int status = response.getStatusCode();

        // Success
        if (((status >= Response.OK) && (status < Response.MULTIPLE_CHOICES)) || status == Response.UNAUTHORIZED) {
            CSeqHeader cseqHeader = (CSeqHeader) responseEvent.getResponse().getHeader(CSeqHeader.NAME);
            String method = cseqHeader.getMethod();
            ISIPResponseProcessor sipRequestProcessor = responseProcessorMap.get(method);
            if (sipRequestProcessor != null) {
                sipRequestProcessor.process(responseEvent);
            }
            if (status != Response.UNAUTHORIZED && responseEvent.getResponse() != null && SipSubscribe.getInstance().getOkSubscribesSize() > 0 ) {
                CallIdHeader callIdHeader = (CallIdHeader)responseEvent.getResponse().getHeader(CallIdHeader.NAME);
                if (callIdHeader != null) {
                    SipEvent subscribe = SipSubscribe.getInstance().getOkSubscribe(callIdHeader.getCallId());
                    if (subscribe != null) {
                        SipSubscribe.getInstance().removeOkSubscribe(callIdHeader.getCallId());
                        subscribe.response(response.getStatusCode(), response.getReasonPhrase(), responseEvent);
                    }
                }
            }
        } else if ((status >= Response.TRYING) && (status < Response.OK)) {
            // 增加其它无需回复的响应，如101、180等
        } else {
            log.warn("[SIP] 接收到失败的response响应！status：" + status + ",message:" + response.getReasonPhrase());
            if (responseEvent.getResponse() != null && SipSubscribe.getInstance().getErrorSubscribesSize() > 0 ) {
                CallIdHeader callIdHeader = (CallIdHeader)responseEvent.getResponse().getHeader(CallIdHeader.NAME);
                if (callIdHeader != null) {
                    SipEvent subscribe = SipSubscribe.getInstance().getErrorSubscribe(callIdHeader.getCallId());
                    if (subscribe != null) {
                        SipSubscribe.getInstance().removeErrorSubscribe(callIdHeader.getCallId());
                        subscribe.response(response.getStatusCode(), response.getReasonPhrase(), null);
                    }
                }
            }
            if (responseEvent.getDialog() != null) {
                responseEvent.getDialog().delete();
            }
        }
    }

    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {

    }

    @Override
    public void processIOException(IOExceptionEvent exceptionEvent) {

    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {

    }

    @Override
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {

    }
}
