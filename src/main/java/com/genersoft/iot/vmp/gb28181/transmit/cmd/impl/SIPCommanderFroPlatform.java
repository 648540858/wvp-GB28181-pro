package com.genersoft.iot.vmp.gb28181.transmit.cmd.impl;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.SIPRequestHeaderPlarformProvider;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.SIPRequestHeaderProvider;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.sip.*;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Request;
import java.text.ParseException;
import java.util.UUID;

@Component
public class SIPCommanderFroPlatform implements ISIPCommanderForPlatform {

    @Autowired
    private SipConfig sipConfig;

    @Autowired
    private SIPRequestHeaderProvider headerProvider;

    @Autowired
    private SIPRequestHeaderPlarformProvider headerProviderPlarformProvider;

    @Autowired
    private VideoStreamSessionManager streamSession;

    @Autowired
    private IVideoManagerStorager storager;

    @Autowired
    @Qualifier(value="tcpSipProvider")
    private SipProvider tcpSipProvider;

    @Autowired
    @Qualifier(value="udpSipProvider")
    private SipProvider udpSipProvider;

    @Value("${media.rtp.enable}")
    private boolean rtpEnable;

    @Override
    public boolean register(ParentPlatform parentPlatform) {
        return register(parentPlatform, null, null, null, null);
    }

    @Override
    public boolean register(ParentPlatform parentPlatform, @Nullable String callId, @Nullable String realm, @Nullable String nonce, @Nullable String scheme ) {
        try {
            Request request = null;
            if (realm == null || nonce == null) {
                request = headerProvider.createRegisterRequest(parentPlatform, 1L, null, null);
            }else {
                request = headerProvider.createRegisterRequest(parentPlatform, null, null, callId, realm, nonce, scheme);
            }

            transmitRequest(parentPlatform, request);
            return true;
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        } catch (PeerUnavailableException e) {
            e.printStackTrace();
        } catch (SipException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public String keepalive(ParentPlatform parentPlatform) {
        String callId = null;
        try {

            StringBuffer keepaliveXml = new StringBuffer(200);
            keepaliveXml.append("<?xml version=\"1.0\" encoding=\"GB2312\" ?>\r\n");
            keepaliveXml.append("<Notify>\r\n");
            keepaliveXml.append("<CmdType>Keepalive</CmdType>\r\n");
            keepaliveXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
            keepaliveXml.append("<DeviceID>" + parentPlatform.getServerGBId() + "</DeviceID>\r\n");
            keepaliveXml.append("<Status>OK</Status>\r\n");
            keepaliveXml.append("</Notify>\r\n");

            Request request = headerProviderPlarformProvider.createKeetpaliveMessageRequest(
                    parentPlatform,
                    keepaliveXml.toString(),
                    UUID.randomUUID().toString().replace("-", ""),
                    UUID.randomUUID().toString().replace("-", ""),
                    null);
            transmitRequest(parentPlatform, request);
            CallIdHeader callIdHeader = (CallIdHeader)request.getHeader(CallIdHeader.NAME);
            callId = callIdHeader.getCallId();
        } catch (ParseException | InvalidArgumentException | SipException e) {
            e.printStackTrace();
        }
        return callId;
    }

    private void transmitRequest(ParentPlatform parentPlatform, Request request) throws SipException {
        if("TCP".equals(parentPlatform.getTransport())) {
            tcpSipProvider.sendRequest(request);
        } else if("UDP".equals(parentPlatform.getTransport())) {
            udpSipProvider.sendRequest(request);
        }
    }
}
