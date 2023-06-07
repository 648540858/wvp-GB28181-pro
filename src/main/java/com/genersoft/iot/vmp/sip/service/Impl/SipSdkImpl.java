package com.genersoft.iot.vmp.sip.service.Impl;

import com.genersoft.iot.vmp.gb28181.conf.DefaultProperties;
import com.genersoft.iot.vmp.sip.SipCommander;
import com.genersoft.iot.vmp.sip.bean.ResultCallback;
import com.genersoft.iot.vmp.sip.bean.SipEvent;
import com.genersoft.iot.vmp.sip.bean.SipServer;
import com.genersoft.iot.vmp.sip.bean.SipServerAccount;
import com.genersoft.iot.vmp.sip.service.SipSdk;
import com.genersoft.iot.vmp.sip.utils.SipUtils;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.message.SIPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sip.*;
import javax.sip.header.WWWAuthenticateHeader;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Map;
import java.util.TooManyListenersException;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SipSdkImpl implements SipSdk {

    private Logger logger = LoggerFactory.getLogger(SipSdkImpl.class);

    private Map<String, SipProviderImpl> sipProviderMap = new ConcurrentHashMap<>();

    @Autowired
    private SipListener sipListenerImpl;

    @Autowired
    private SipCommander sipCommander;

    @Override
    public void register(SipServer sipServer, SipServerAccount account, ResultCallback<Object> callback) throws PeerUnavailableException, TransportNotSupportedException, InvalidArgumentException, ObjectInUseException, TooManyListenersException {

        if (sipServer == null || account == null) {
            return;
        }
        if (sipServer.getLocalIp() == null) {
            sipServer.setLocalIp("0.0.0.0");
        }
        if (sipServer.getLocalPort() == null) {
            if (sipServer.getTransport().equalsIgnoreCase("UDP")) {
                sipServer.setLocalPort(SipUtils.getRandomUdpPort());
            }else {
                sipServer.setLocalPort(SipUtils.getRandomTcpPort());
            }
        }
        SipProviderImpl sipProvider = initSipLister(sipServer);
        try {
            SipEvent successEvent =  (code, msg, data)->{
                // 注册成功后开启定时任务
            };

            sipCommander.register(sipServer, account, sipProvider, true, (code, msg, data)->{
                if (code == 200) {
                    // 注册成功
                    if (callback != null) {
                        callback.run(code, msg, data);
                    }
                    successEvent.response(code, msg, data);
                }else if (code == 401) {
                    ResponseEvent responseEvent = (ResponseEvent) data;
                    SIPResponse response = (SIPResponse) responseEvent.getResponse();
                    WWWAuthenticateHeader authenticateHeader = (WWWAuthenticateHeader) response.getHeader(WWWAuthenticateHeader.NAME);
                    try {
                        sipCommander.register(sipServer, account, sipProvider, authenticateHeader, true, (code1, msg1, data1)->{
                            if (code1 == 200) {
                                // 注册成功
                                if (callback != null) {
                                    callback.run(code1, msg1, data1);
                                }
                                successEvent.response(code1, msg1, data1);
                            }else {
                                // 注册失败
                                if (callback != null) {
                                    callback.run(code1, msg1, data1);
                                }
                            }

                        }, (code2, msg2, data2)->{
                            // 注册失败
                            if (callback != null) {
                                callback.run(code2, msg2, data2);
                            }
                        });
                    } catch (SipException | ParseException | NoSuchAlgorithmException| InvalidArgumentException e) {
                        logger.warn("[SIP] 发送注册消息失败", e);
                        if (callback != null) {
                            callback.run(-1, e.getMessage(), null);
                        }
                    }
                }else {
                    // 注册失败
                    if (callback != null) {
                        callback.run(code, msg, data);
                    }
                }

            }, (code, msg, data)->{

                // 注册失败
                if (callback != null) {
                    callback.run(code, msg, data);
                }
            });
        } catch (SipException | ParseException | NoSuchAlgorithmException e) {
            logger.warn("[SIP] 发送注册消息失败", e);
            if (callback != null) {
                callback.run(-1, e.getMessage(), null);
            }
        }

    }

    private SipProviderImpl initSipLister(SipServer sipServer) throws PeerUnavailableException, TransportNotSupportedException, InvalidArgumentException, ObjectInUseException, TooManyListenersException {
        SipFactory.getInstance().setPathName("gov.nist");
        SipStackImpl sipStack = (SipStackImpl)SipFactory.getInstance().createSipStack(
                DefaultProperties.getProperties(sipServer.getLocalIp(), false));

        ListeningPoint listeningPoint = sipStack.createListeningPoint(sipServer.getLocalIp(), sipServer.getLocalPort(), sipServer.getTransport());
        SipProviderImpl sipProvider = (SipProviderImpl)sipStack.createSipProvider(listeningPoint);
        sipProvider.setDialogErrorsAutomaticallyHandled();
        sipProvider.addSipListener(sipListenerImpl);
        sipProviderMap.put(sipServer.getLocalIp() + ":" + sipServer.getLocalPort(), sipProvider);
        return sipProvider;
    }

    @Override
    public SipProviderImpl getProvider(String ip, int port) {
        return sipProviderMap.get(ip + ":" + port);
    }
}
