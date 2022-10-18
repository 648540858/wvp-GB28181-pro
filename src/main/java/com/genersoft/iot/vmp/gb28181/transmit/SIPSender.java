package com.genersoft.iot.vmp.gb28181.transmit;

import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.utils.GitUtil;
import gov.nist.javax.sip.SipProviderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.header.CallIdHeader;
import javax.sip.header.UserAgentHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Message;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * 发送SIP消息
 * @author lin
 */
@Component
public class SIPSender {

    private final Logger logger = LoggerFactory.getLogger(SIPSender.class);

    @Autowired
    @Qualifier(value = "tcpSipProvider")
    private SipProviderImpl tcpSipProvider;

    @Autowired
    @Qualifier(value = "udpSipProvider")
    private SipProviderImpl udpSipProvider;

    @Autowired
    private SipFactory sipFactory;

    @Autowired
    private GitUtil gitUtil;

    @Autowired
    private SipSubscribe sipSubscribe;

    public void transmitRequest(Message message) throws SipException, ParseException {
        transmitRequest(message, null, null);
    }

    public void transmitRequest(Message message, SipSubscribe.Event errorEvent) throws SipException, ParseException {
        transmitRequest(message, errorEvent, null);
    }

    public void transmitRequest(Message message, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws SipException, ParseException {
        ViaHeader viaHeader = (ViaHeader)message.getHeader(ViaHeader.NAME);
        String transport = "UDP";
        if (viaHeader == null) {
            logger.warn("[消息头缺失]： ViaHeader");
        }else {
            transport = viaHeader.getTransport();
        }
        if (message.getHeader(UserAgentHeader.NAME) == null) {
            try {
                message.addHeader(SipUtils.createUserAgentHeader(sipFactory, gitUtil));
            } catch (ParseException e) {
                logger.error("添加UserAgentHeader失败", e);
            }
        }

        CallIdHeader callIdHeader = (CallIdHeader) message.getHeader(CallIdHeader.NAME);
        // 添加错误订阅
        if (errorEvent != null) {
            sipSubscribe.addErrorSubscribe(callIdHeader.getCallId(), (eventResult -> {
                errorEvent.response(eventResult);
                sipSubscribe.removeErrorSubscribe(eventResult.callId);
                sipSubscribe.removeOkSubscribe(eventResult.callId);
            }));
        }
        // 添加订阅
        if (okEvent != null) {
            sipSubscribe.addOkSubscribe(callIdHeader.getCallId(), eventResult -> {
                okEvent.response(eventResult);
                sipSubscribe.removeOkSubscribe(eventResult.callId);
                sipSubscribe.removeErrorSubscribe(eventResult.callId);
            });
        }
        if ("TCP".equals(transport)) {
            if (message instanceof Request) {
                tcpSipProvider.sendRequest((Request)message);
            }else if (message instanceof Response) {
                tcpSipProvider.sendResponse((Response)message);
            }

        } else if ("UDP".equals(transport)) {
            if (message instanceof Request) {
                udpSipProvider.sendRequest((Request)message);
            }else if (message instanceof Response) {
                udpSipProvider.sendResponse((Response)message);
            }
        }
    }

    public CallIdHeader getNewCallIdHeader(String transport){
        return  transport.equalsIgnoreCase("TCP") ? tcpSipProvider.getNewCallId()
                : udpSipProvider.getNewCallId();
    }
}
