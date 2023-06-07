package com.genersoft.iot.vmp.sip.utils;

import com.genersoft.iot.vmp.sip.bean.SipServer;
import com.genersoft.iot.vmp.sip.bean.SipServerAccount;

import javax.sip.InvalidArgumentException;
import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.Request;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;

public class SIPRequestFactory {

    public static Request createRegisterRequest(SipServer server, SipServerAccount account, String callId, boolean isRegister, WWWAuthenticateHeader wwwAuthenticateHeader) throws ParseException, PeerUnavailableException, InvalidArgumentException, NoSuchAlgorithmException {
        Request request = null;
        String serverAddress = server.getServerIp()+ ":" + server.getServerPort();
        String clientAddress = server.getLocalIp()+ ":" + server.getLocalPort();
        //请求行
        SipURI requestLine = SipFactory.getInstance().createAddressFactory().createSipURI(account.getUsername(),
                serverAddress);
        //via
        ArrayList<ViaHeader> viaHeaders = new ArrayList<>();
        ViaHeader viaHeader = SipFactory.getInstance().createHeaderFactory().createViaHeader(server.getLocalIp(),
                server.getLocalPort(), server.getTransport(), SipUtils.getNewViaTag());
        viaHeader.setRPort();
        viaHeaders.add(viaHeader);
        //from
        SipURI fromSipURI = SipFactory.getInstance().createAddressFactory().createSipURI(account.getUsername(), serverAddress);
        Address fromAddress = SipFactory.getInstance().createAddressFactory().createAddress(fromSipURI);
        FromHeader fromHeader = SipFactory.getInstance().createHeaderFactory().createFromHeader(fromAddress, SipUtils.getNewFromTag());
        //to
        SipURI toSipURI = SipFactory.getInstance().createAddressFactory().createSipURI(account.getUsername(), serverAddress);
        Address toAddress = SipFactory.getInstance().createAddressFactory().createAddress(toSipURI);
        ToHeader toHeader = SipFactory.getInstance().createHeaderFactory().createToHeader(toAddress,null);

        //Forwards
        MaxForwardsHeader maxForwards = SipFactory.getInstance().createHeaderFactory().createMaxForwardsHeader(70);

        CallIdHeader callIdHeader = SipFactory.getInstance().createHeaderFactory().createCallIdHeader(callId);

        //ceq
        CSeqHeader cSeqHeader = SipFactory.getInstance().createHeaderFactory().createCSeqHeader(SipUtils.getCSEQ(), Request.REGISTER);
        request = SipFactory.getInstance().createMessageFactory().createRequest(requestLine, Request.REGISTER, callIdHeader,
                cSeqHeader,fromHeader, toHeader, viaHeaders, maxForwards);

        Address concatAddress = SipFactory.getInstance().createAddressFactory().createAddress(SipFactory.getInstance().createAddressFactory()
                .createSipURI(account.getUsername(), clientAddress));
        ContactHeader contactHeader = SipFactory.getInstance().createHeaderFactory().createContactHeader(concatAddress);
        contactHeader.setParameter("methods", "INVITE,ACK,BYE,CANCEL,OPTIONS,PRACK,MESSAGE,SUBSCRIBE,NOTIFY,REFER,UPDATE");
//        contactHeader.setQValue();
        request.addHeader(contactHeader);

        ExpiresHeader expires = SipFactory.getInstance().createHeaderFactory().createExpiresHeader(isRegister ? 180 : 0);
        request.addHeader(expires);

        AllowHeader allowHeader = SipFactory.getInstance().createHeaderFactory().createAllowHeader("INVITE, ACK, BYE, CANCEL, OPTIONS, PRACK, MESSAGE, SUBSCRIBE, NOTIFY, REFER, UPDATE");
        request.addHeader(allowHeader);

        SupportedHeader supportedHeader = SipFactory.getInstance().createHeaderFactory().createSupportedHeader("timer, 100rel, replaces, gruu, outbound");
        request.addHeader(supportedHeader);

        AuthorizationHeader authorizationHeader;
        if (wwwAuthenticateHeader != null) {
            String qop = wwwAuthenticateHeader.getQop();
            String algorithm = wwwAuthenticateHeader.getAlgorithm();
            String nonce = wwwAuthenticateHeader.getNonce();
            String realm = wwwAuthenticateHeader.getRealm();

            authorizationHeader = DigestClientAuthenticationHelper.getAuthorizationHeader(server, account, realm, nonce, algorithm, qop);
        }else {
            authorizationHeader = DigestClientAuthenticationHelper.getAuthorizationHeader(server, account);
        }

        request.addHeader(authorizationHeader);

        return request;
    }

}
