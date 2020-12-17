package com.genersoft.iot.vmp.gb28181.transmit.cmd.impl;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.SIPRequestHeaderProvider;
import com.genersoft.iot.vmp.media.zlm.ZLMUtils;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.sip.*;
import javax.sip.message.Request;
import java.text.ParseException;

@Component
public class SIPCommanderFroPlatform implements ISIPCommanderForPlatform {

    @Autowired
    private SipConfig sipConfig;

    @Autowired
    private SIPRequestHeaderProvider headerProvider;

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

    @Autowired
    private ZLMUtils zlmUtils;

    @Value("${media.rtp.enable}")
    private boolean rtpEnable;

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

    private void transmitRequest(ParentPlatform parentPlatform, Request request) throws SipException {
        if("TCP".equals(parentPlatform.getTransport())) {
            tcpSipProvider.sendRequest(request);
        } else if("UDP".equals(parentPlatform.getTransport())) {
            udpSipProvider.sendRequest(request);
        }
    }
}
