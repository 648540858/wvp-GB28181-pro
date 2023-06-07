package com.genersoft.iot.vmp.sip;

import com.genersoft.iot.vmp.sip.bean.SipEvent;
import com.genersoft.iot.vmp.sip.service.SipSubscribe;
import com.genersoft.iot.vmp.sip.utils.SipUtils;
import com.genersoft.iot.vmp.utils.GitUtil;
import gov.nist.javax.sip.SipProviderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.SipException;
import javax.sip.header.CallIdHeader;
import javax.sip.header.UserAgentHeader;
import javax.sip.message.Message;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * 消息发送器
 * @author lin
 */
@Component
public class SipSender {

    private Logger logger = LoggerFactory.getLogger(SipSender.class);

    @Autowired
    private GitUtil gitUtil;

    public void transmitRequest(SipProviderImpl sipProvider, Message message) throws SipException, ParseException {
        transmitRequest(sipProvider, message, null, null);
    }

    public void transmitRequest(SipProviderImpl sipProvider, String ip, Message message, SipEvent errorEvent) throws SipException, ParseException {
        transmitRequest(sipProvider, message, null, errorEvent );
    }

    public void transmitRequest(SipProviderImpl sipProvider, Message message, SipEvent okEvent,  SipEvent errorEvent) throws SipException, ParseException {
        if (message.getHeader(UserAgentHeader.NAME) == null) {
            try {
                message.addHeader(SipUtils.createUserAgentHeader(gitUtil));
            } catch (ParseException e) {
                logger.error("添加UserAgentHeader失败", e);
            }
        }

        CallIdHeader callIdHeader = (CallIdHeader) message.getHeader(CallIdHeader.NAME);
        // 添加错误订阅
        if (errorEvent != null) {
            SipSubscribe.getInstance().addErrorSubscribe(callIdHeader.getCallId(), (code, msg, data) -> {
                errorEvent.response(code, msg, data);
                SipSubscribe.getInstance().removeErrorSubscribe(callIdHeader.getCallId());
                SipSubscribe.getInstance().removeOkSubscribe(callIdHeader.getCallId());
            });
        }
        // 添加订阅
        if (okEvent != null) {
            SipSubscribe.getInstance().addOkSubscribe(callIdHeader.getCallId(), (code, msg, data) -> {
                okEvent.response(code, msg, data);
                SipSubscribe.getInstance().removeOkSubscribe(callIdHeader.getCallId());
                SipSubscribe.getInstance().removeErrorSubscribe(callIdHeader.getCallId());
            });
        }
        if (message instanceof Request) {
            sipProvider.sendRequest((Request)message);
        }else if (message instanceof Response) {
            sipProvider.sendResponse((Response)message);
        }
    }
}
