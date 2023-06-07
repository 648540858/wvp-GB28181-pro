package com.genersoft.iot.vmp.sip;

import com.genersoft.iot.vmp.sip.bean.SipEvent;
import com.genersoft.iot.vmp.sip.bean.SipServer;
import com.genersoft.iot.vmp.sip.bean.SipServerAccount;
import com.genersoft.iot.vmp.sip.utils.SIPRequestFactory;
import com.genersoft.iot.vmp.sip.utils.SipUtils;
import gov.nist.javax.sip.SipProviderImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;

@Component
public class SipCommander {

    @Autowired
    private SipSender sipSender;

    public void register(SipServer server, SipServerAccount account, SipProviderImpl sipProvider, boolean isRegister,
                         SipEvent okEvent, SipEvent errorEvent)
            throws InvalidArgumentException, SipException, ParseException, NoSuchAlgorithmException {
        register(server, account, sipProvider, null, isRegister, okEvent, errorEvent);
    }

    public void register(SipServer server, SipServerAccount account, SipProviderImpl sipProvider,
                         WWWAuthenticateHeader wwwAuthenticateHeader, boolean isRegister,
                         SipEvent okEvent, SipEvent errorEvent)
            throws InvalidArgumentException, SipException, ParseException, NoSuchAlgorithmException {
        if (server == null || account == null || sipProvider == null) {
            return;
        }
        String callId = SipUtils.getNewCallId(sipProvider);
        Request request = SIPRequestFactory.createRegisterRequest(server, account, callId, isRegister, wwwAuthenticateHeader);
        sipSender.transmitRequest(sipProvider, request, okEvent, errorEvent);
    }
}
