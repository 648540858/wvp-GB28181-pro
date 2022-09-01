package com.genersoft.iot.vmp.gb28181.session;

import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.stack.MessageChannel;
import gov.nist.javax.sip.stack.SIPMessageValve;

import javax.sip.SipStack;
import javax.sip.message.Response;

public class SipMessagePreprocessing implements SIPMessageValve {
    @Override
    public boolean processRequest(SIPRequest sipRequest, MessageChannel messageChannel) {
        return false;
    }

    @Override
    public boolean processResponse(Response response, MessageChannel messageChannel) {
        return false;
    }

    @Override
    public void init(SipStack sipStack) {

    }

    @Override
    public void destroy() {

    }
}
