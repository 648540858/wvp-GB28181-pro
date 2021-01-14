package com.genersoft.iot.vmp.gb28181.transmit.cmd.impl;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatformCatch;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.SIPRequestHeaderPlarformProvider;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.SIPRequestHeaderProvider;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.sip.*;
import javax.sip.header.CallIdHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import java.nio.channels.Channel;
import java.text.ParseException;
import java.util.List;
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
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private SipSubscribe sipSubscribe;

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
    public boolean unregister(ParentPlatform parentPlatform, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent) {
        parentPlatform.setExpires("0");
        ParentPlatformCatch parentPlatformCatch = redisCatchStorage.queryPlatformCatchInfo(parentPlatform.getDeviceGBId());
        if (parentPlatformCatch != null) {
            parentPlatformCatch.setParentPlatform(parentPlatform);
            redisCatchStorage.updatePlatformCatchInfo(parentPlatformCatch);
        }

        return register(parentPlatform, null, null, errorEvent, okEvent);
    }

    @Override
    public boolean register(ParentPlatform parentPlatform, @Nullable String callId, @Nullable WWWAuthenticateHeader www, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent) {
        try {
            Request request = null;

            if (www == null ) {
                request = headerProviderPlarformProvider.createRegisterRequest(parentPlatform, 1L, null, null);
            }else {
                request = headerProviderPlarformProvider.createRegisterRequest(parentPlatform, null, null, callId, www);
            }

            transmitRequest(parentPlatform, request, errorEvent, okEvent);
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
        transmitRequest(parentPlatform, request, null, null);
    }

    private void transmitRequest(ParentPlatform parentPlatform, Request request, SipSubscribe.Event errorEvent) throws SipException {
        transmitRequest(parentPlatform, request, errorEvent, null);
    }

    private void transmitRequest(ParentPlatform parentPlatform, Request request, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent) throws SipException {
        if("TCP".equals(parentPlatform.getTransport())) {
            tcpSipProvider.sendRequest(request);
        } else if("UDP".equals(parentPlatform.getTransport())) {
            udpSipProvider.sendRequest(request);
        }

        CallIdHeader callIdHeader = (CallIdHeader)request.getHeader(CallIdHeader.NAME);
        // 添加错误订阅
        if (errorEvent != null) {
            sipSubscribe.addErrorSubscribe(callIdHeader.getCallId(), errorEvent);
        }
        // 添加订阅
        if (okEvent != null) {
            sipSubscribe.addOkSubscribe(callIdHeader.getCallId(), okEvent);
        }

    }

    /**
     * 向上级回复通道信息
     * @param channel 通道信息
     * @param parentPlatform 平台信息
     * @return
     */
    @Override
    public boolean catalogQuery(DeviceChannel channel, ParentPlatform parentPlatform, String sn, String fromTag, int size) {

        if (channel == null  || parentPlatform ==null) {
            return false;
        }
        try {
            StringBuffer catalogXml = new StringBuffer(600);
            catalogXml.append("<?xml version=\"1.0\" encoding=\"GB2312\"?>\r\n");
            catalogXml.append("<Response>\r\n");
            catalogXml.append("<CmdType>Catalog</CmdType>\r\n");
            catalogXml.append("<SN>" +sn + "</SN>\r\n");
            catalogXml.append("<DeviceID>" + parentPlatform.getDeviceGBId() + "</DeviceID>\r\n");
            catalogXml.append("<SumNum>" + size + "</SumNum>\r\n");
            catalogXml.append("<DeviceList Num=\"1\">\r\n");
            catalogXml.append("<Item>\r\n");

            catalogXml.append("<DeviceID>" + channel.getChannelId() + "</DeviceID>\r\n");
            catalogXml.append("<Name>" + channel.getName() + "</Name>\r\n");
            catalogXml.append("<Manufacturer>" + channel.getManufacture() + "</Manufacturer>\r\n");
            catalogXml.append("<Model>" + channel.getModel() + "</Model>\r\n");
            catalogXml.append("<Owner>" + channel.getOwner() + "</Owner>\r\n");
            catalogXml.append("<CivilCode>" + channel.getCivilCode() + "</CivilCode>\r\n");
            catalogXml.append("<Address>" + channel.getAddress() + "</Address>\r\n");
            catalogXml.append("<Parental>" + channel.getParental() + "</Parental>\r\n");// TODO 当前不能添加分组， 所以暂时没有父节点
            catalogXml.append("<ParentID>" + channel.getParentId() + "</ParentID>\r\n"); // TODO 当前不能添加分组， 所以暂时没有父节点
            catalogXml.append("<Secrecy>" + channel.getSecrecy() + "</Secrecy>\r\n");
            catalogXml.append("<RegisterWay>" + channel.getRegisterWay() + "</RegisterWay>\r\n");
            catalogXml.append("<Status>" + (channel.getStatus() == 0?"OFF":"ON") + "</Status>\r\n");
            catalogXml.append("<Info></Info>\r\n");

            catalogXml.append("</Item>\r\n");
            catalogXml.append("</DeviceList>\r\n");
            catalogXml.append("</Response>\r\n");
            Request request = headerProviderPlarformProvider.createMessageRequest(parentPlatform, catalogXml.toString(), fromTag);
            transmitRequest(parentPlatform, request);

        } catch (SipException | ParseException | InvalidArgumentException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
