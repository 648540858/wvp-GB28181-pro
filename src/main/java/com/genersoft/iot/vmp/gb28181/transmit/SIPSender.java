package com.genersoft.iot.vmp.gb28181.transmit;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.event.sip.SipEvent;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.utils.GitUtil;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.SipException;
import javax.sip.ClientTransaction;
import javax.sip.header.*;
import javax.sip.message.Message;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * 发送SIP消息
 *
 * @author lin
 */
@Slf4j
@Component
public class SIPSender {

    // 默认T1为500毫秒时，非INVITE事务Timer F约32秒；应用不能先于协议重传窗口遗忘BYE。
    private static final long BYE_RESPONSE_TIMEOUT_MILLIS = 35_000L;

    @Autowired
    private SipLayer sipLayer;

    @Autowired
    private GitUtil gitUtil;

    @Autowired
    private SipSubscribe sipSubscribe;

    @Autowired
    private SipConfig sipConfig;

    public void transmitRequest(String ip, Message message) throws SipException, ParseException {
        transmitRequest(ip, message, null, null, null);
    }

    public void transmitRequest(String ip, Message message, SipSubscribe.Event errorEvent) throws SipException, ParseException {
        transmitRequest(ip, message, errorEvent, null, null);
    }

    public void transmitRequest(String ip, Message message, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws SipException {
        transmitRequest(ip, message, errorEvent, okEvent, null);
    }

    // BYE交给JAIN客户端事务重传和收尾，其余消息沿用原发送行为及应用超时。
    public void transmitRequest(String ip, Message message, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent, Long timeout) throws SipException {
        boolean reliableBye = message instanceof Request && Request.BYE.equals(((Request) message).getMethod());
        ViaHeader viaHeader = (ViaHeader) message.getHeader(ViaHeader.NAME);
        String transport = "UDP";
        if (viaHeader == null) {
            log.warn("[消息头缺失]： ViaHeader， 使用默认的UDP方式处理数据");
        } else {
            transport = viaHeader.getTransport();
        }
        if (message.getHeader(UserAgentHeader.NAME) == null) {
            try {
                message.addHeader(SipUtils.createUserAgentHeader(gitUtil));
            } catch (ParseException e) {
                log.error("添加UserAgentHeader失败", e);
            }
        }
        CallIdHeader callIdHeader = (CallIdHeader) message.getHeader(CallIdHeader.NAME);
        CSeqHeader cSeqHeader = (CSeqHeader) message.getHeader(CSeqHeader.NAME);
        String key = callIdHeader.getCallId() + cSeqHeader.getSeqNumber();
        if (okEvent != null || errorEvent != null) {

            FromHeader fromHeader = (FromHeader) message.getHeader(FromHeader.NAME);
            long responseTimeout = timeout == null ? sipConfig.getTimeout() : timeout;
            if (reliableBye) {
                // 自定义更长T1的部署仍可传更长timeout；这里只覆盖JAIN默认Timer F，不改全局SIP时钟。
                responseTimeout = Math.max(responseTimeout, BYE_RESPONSE_TIMEOUT_MILLIS);
            }
            SipEvent sipEvent = SipEvent.getInstance(key, eventResult -> {
                sipSubscribe.removeSubscribe(key);
                if(okEvent != null) {
                    okEvent.response(eventResult);
                }
            }, (eventResult -> {
                sipSubscribe.removeSubscribe(key);
                if (errorEvent != null) {
                    errorEvent.response(eventResult);
                }
            }), responseTimeout);
            SipTransactionInfo sipTransactionInfo = new SipTransactionInfo();
            sipTransactionInfo.setFromTag(fromHeader.getTag());
            sipTransactionInfo.setCallId(callIdHeader.getCallId());

            if (message instanceof SIPResponse) {
                SIPResponse response = (SIPResponse) message;
                sipTransactionInfo.setToTag(response.getToHeader().getTag());
                sipTransactionInfo.setViaBranch(response.getTopmostViaHeader().getBranch());
            }else if (message instanceof SIPRequest) {
                SIPRequest request = (SIPRequest) message;
                sipTransactionInfo.setViaBranch(request.getTopmostViaHeader().getBranch());
                SipUri sipUri = (SipUri)request.getRequestLine().getUri();
                sipTransactionInfo.setUser(sipUri.getUser());
            }

            ExpiresHeader expiresHeader = (ExpiresHeader) message.getHeader(ExpiresHeader.NAME);
            if (expiresHeader != null) {
                sipTransactionInfo.setExpires(expiresHeader.getExpires());
            }
            sipEvent.setSipTransactionInfo(sipTransactionInfo);
            sipSubscribe.addSubscribe(key, sipEvent);
        }
        try {
            if ("TCP".equals(transport)) {
                SipProviderImpl tcpSipProvider = sipLayer.getTcpSipProvider(ip);
                if (tcpSipProvider == null) {
                    log.error("[发送信息失败] 未找到tcp://{}的监听信息", ip);
                    if (reliableBye) {
                        throw new SipException("BYE发送失败，未找到TCP监听器");
                    }
                    return;
                }
                if (message instanceof Request) {
                    if (reliableBye) {
                        ClientTransaction transaction = tcpSipProvider.getNewClientTransaction((Request) message);
                        transaction.sendRequest();
                    } else {
                        tcpSipProvider.sendRequest((Request) message);
                    }
                } else if (message instanceof Response) {
                    tcpSipProvider.sendResponse((Response) message);
                }

            } else if ("UDP".equals(transport)) {
                SipProviderImpl sipProvider = sipLayer.getUdpSipProvider(ip);
                if (sipProvider == null) {
                    log.error("[发送信息失败] 未找到udp://{}的监听信息", ip);
                    if (reliableBye) {
                        throw new SipException("BYE发送失败，未找到UDP监听器");
                    }
                    return;
                }
                if (message instanceof Request) {
                    if (reliableBye) {
                        // 同一请求只建立一次事务；UDP重发由JAIN复用原Call-ID、CSeq和Via branch。
                        ClientTransaction transaction = sipProvider.getNewClientTransaction((Request) message);
                        transaction.sendRequest();
                    } else {
                        sipProvider.sendRequest((Request) message);
                    }
                } else if (message instanceof Response) {
                    sipProvider.sendResponse((Response) message);
                }
            } else if (reliableBye) {
                throw new SipException("BYE发送失败，不支持的信令传输方式：" + transport);
            }
        }catch (SipException e) {
            sipSubscribe.removeSubscribe(key);
            throw e;
        }
    }

    public CallIdHeader getNewCallIdHeader(String ip, String transport) {
        if (ObjectUtils.isEmpty(transport)) {
            return sipLayer.getUdpSipProvider() != null ? sipLayer.getUdpSipProvider().getNewCallId() : sipLayer.getTcpSipProvider().getNewCallId();
        }
        SipProviderImpl sipProvider;
        if (ObjectUtils.isEmpty(ip)) {
            sipProvider = transport.equalsIgnoreCase("TCP") ? sipLayer.getTcpSipProvider()
                    : sipLayer.getUdpSipProvider();
        } else {
            sipProvider = transport.equalsIgnoreCase("TCP") ? sipLayer.getTcpSipProvider(ip)
                    : sipLayer.getUdpSipProvider(ip);
        }

        if (sipProvider == null) {
            sipProvider = transport.equalsIgnoreCase("TCP") ? sipLayer.getTcpSipProvider()
                    : sipLayer.getUdpSipProvider();
        }

        if (sipProvider != null) {
            return sipProvider.getNewCallId();
        } else {
            log.warn("[新建CallIdHeader失败]， ip={}, transport={}", ip, transport);
            return null;
        }
    }


}
