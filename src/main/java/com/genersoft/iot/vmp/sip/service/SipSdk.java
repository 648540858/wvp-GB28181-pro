package com.genersoft.iot.vmp.sip.service;

import com.genersoft.iot.vmp.sip.bean.ResultCallback;
import com.genersoft.iot.vmp.sip.bean.SipServer;
import com.genersoft.iot.vmp.sip.bean.SipServerAccount;
import gov.nist.javax.sip.SipProviderImpl;

import javax.sip.InvalidArgumentException;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.TransportNotSupportedException;
import java.util.TooManyListenersException;

/**
 * java实现SIP SDK
 */
public interface SipSdk {

    void register(SipServer sipServer, SipServerAccount account, ResultCallback<Object> callback) throws PeerUnavailableException, TransportNotSupportedException, InvalidArgumentException, ObjectInUseException, TooManyListenersException;

    SipProviderImpl getProvider(String ip, int port);
}
