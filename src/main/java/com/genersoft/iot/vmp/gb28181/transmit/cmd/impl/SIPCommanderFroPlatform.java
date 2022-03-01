package com.genersoft.iot.vmp.gb28181.transmit.cmd.impl;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.SIPRequestHeaderPlarformProvider;
import com.genersoft.iot.vmp.gb28181.utils.DateUtil;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sip.*;
import javax.sip.header.CallIdHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;

@Component
@DependsOn("sipLayer")
public class SIPCommanderFroPlatform implements ISIPCommanderForPlatform {

    private final Logger logger = LoggerFactory.getLogger(SIPCommanderFroPlatform.class);

    @Autowired
    private SIPRequestHeaderPlarformProvider headerProviderPlarformProvider;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private SipSubscribe sipSubscribe;

    @Lazy
    @Autowired
    @Qualifier(value="tcpSipProvider")
    private SipProvider tcpSipProvider;

    @Lazy
    @Autowired
    @Qualifier(value="udpSipProvider")
    private SipProvider udpSipProvider;

    @Override
    public boolean register(ParentPlatform parentPlatform, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent) {
        return register(parentPlatform, null, null, errorEvent, okEvent);
    }

    @Override
    public boolean unregister(ParentPlatform parentPlatform, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent) {
        parentPlatform.setExpires("0");
        ParentPlatformCatch parentPlatformCatch = redisCatchStorage.queryPlatformCatchInfo(parentPlatform.getServerGBId());
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
            String tm = Long.toString(System.currentTimeMillis());
            if (www == null ) {
                //		//callid
                CallIdHeader callIdHeader = null;
                if(parentPlatform.getTransport().equals("TCP")) {
                    callIdHeader = tcpSipProvider.getNewCallId();
                }
                if(parentPlatform.getTransport().equals("UDP")) {
                    callIdHeader = udpSipProvider.getNewCallId();
                }

                request = headerProviderPlarformProvider.createRegisterRequest(parentPlatform, redisCatchStorage.getCSEQ(Request.REGISTER), "FromRegister" + tm, null, callIdHeader);
                // 将 callid 写入缓存， 等注册成功可以更新状态
                String callIdFromHeader = callIdHeader.getCallId();
                redisCatchStorage.updatePlatformRegisterInfo(callIdFromHeader, parentPlatform.getServerGBId());

                sipSubscribe.addErrorSubscribe(callIdHeader.getCallId(), (event)->{
                    if (event != null) {
                        logger.info("向上级平台 [ {} ] 注册发上错误： {} ",
                                parentPlatform.getServerGBId(),
                                event.msg);
                    }
                    redisCatchStorage.delPlatformRegisterInfo(callIdFromHeader);
                    if (errorEvent != null ) {
                        errorEvent.response(event);
                    }
                });

            }else {
                CallIdHeader callIdHeader = parentPlatform.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
                        : udpSipProvider.getNewCallId();
                request = headerProviderPlarformProvider.createRegisterRequest(parentPlatform, "FromRegister" + tm, null, callId, www, callIdHeader);
            }

            transmitRequest(parentPlatform, request, null, okEvent);
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
            keepaliveXml.append("<?xml version=\"1.0\"?>\r\n");
            keepaliveXml.append("<Notify>\r\n");
            keepaliveXml.append("<CmdType>Keepalive</CmdType>\r\n");
            keepaliveXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
            keepaliveXml.append("<DeviceID>" + parentPlatform.getDeviceGBId() + "</DeviceID>\r\n");
            keepaliveXml.append("<Status>OK</Status>\r\n");
            keepaliveXml.append("</Notify>\r\n");

            CallIdHeader callIdHeader = parentPlatform.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
                    : udpSipProvider.getNewCallId();

            Request request = headerProviderPlarformProvider.createKeetpaliveMessageRequest(
                    parentPlatform,
                    keepaliveXml.toString(),
                    "z9hG4bK-" + UUID.randomUUID().toString().replace("-", ""),
                    UUID.randomUUID().toString().replace("-", ""),
                    null,
                    callIdHeader);
            transmitRequest(parentPlatform, request);
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
        logger.debug("\n发送消息：\n{}", request);
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

        if ( parentPlatform ==null) {
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
            if (channel != null) {
                catalogXml.append("<DeviceID>" + channel.getChannelId() + "</DeviceID>\r\n");
                catalogXml.append("<Name>" + channel.getName() + "</Name>\r\n");
                catalogXml.append("<Manufacturer>" + channel.getManufacture() + "</Manufacturer>\r\n");
                catalogXml.append("<Model>" + channel.getModel() + "</Model>\r\n");
                catalogXml.append("<Owner>" + channel.getOwner() + "</Owner>\r\n");
                catalogXml.append("<CivilCode>" + channel.getCivilCode() + "</CivilCode>\r\n");
                catalogXml.append("<Address>" + channel.getAddress() + "</Address>\r\n");
                catalogXml.append("<Parental>" + channel.getParental() + "</Parental>\r\n");
                if (channel.getParentId() != null) {
                    catalogXml.append("<ParentID>" + channel.getParentId() + "</ParentID>\r\n");
                }
                catalogXml.append("<Secrecy>" + channel.getSecrecy() + "</Secrecy>\r\n");
                catalogXml.append("<RegisterWay>" + channel.getRegisterWay() + "</RegisterWay>\r\n");
                catalogXml.append("<Status>" + (channel.getStatus() == 0?"OFF":"ON") + "</Status>\r\n");
                catalogXml.append("<Longitude>" + channel.getLongitude() + "</Longitude>\r\n");
                catalogXml.append("<Latitude>" + channel.getLatitude() + "</Latitude>\r\n");
                catalogXml.append("<IPAddress>" + channel.getIpAddress() + "</IPAddress>\r\n");
                catalogXml.append("<Port>" + channel.getPort() + "</Port>\r\n");
                catalogXml.append("<Info>\r\n");
                catalogXml.append("<PTZType>" + channel.getPTZType() + "</PTZType>\r\n");
                catalogXml.append("</Info>\r\n");
            }


            catalogXml.append("</Item>\r\n");
            catalogXml.append("</DeviceList>\r\n");
            catalogXml.append("</Response>\r\n");

            // callid
            CallIdHeader callIdHeader = parentPlatform.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
                    : udpSipProvider.getNewCallId();

            Request request = headerProviderPlarformProvider.createMessageRequest(parentPlatform, catalogXml.toString(), fromTag, callIdHeader);
            transmitRequest(parentPlatform, request);

        } catch (SipException | ParseException | InvalidArgumentException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 向上级回复DeviceInfo查询信息
     * @param parentPlatform 平台信息
     * @param sn
     * @param fromTag
     * @return
     */
    @Override
    public boolean deviceInfoResponse(ParentPlatform parentPlatform, String sn, String fromTag) {
        if (parentPlatform == null) {
            return false;
        }
        try {
            StringBuffer deviceInfoXml = new StringBuffer(600);
            deviceInfoXml.append("<?xml version=\"1.0\" encoding=\"GB2312\"?>\r\n");
            deviceInfoXml.append("<Response>\r\n");
            deviceInfoXml.append("<CmdType>DeviceInfo</CmdType>\r\n");
            deviceInfoXml.append("<SN>" +sn + "</SN>\r\n");
            deviceInfoXml.append("<DeviceID>" + parentPlatform.getDeviceGBId() + "</DeviceID>\r\n");
            deviceInfoXml.append("<DeviceName>" + parentPlatform.getName() + "</DeviceName>\r\n");
            deviceInfoXml.append("<Manufacturer>wvp</Manufacturer>\r\n");
            deviceInfoXml.append("<Model>wvp-28181-2.0</Model>\r\n");
            deviceInfoXml.append("<Firmware>2.0.202107</Firmware>\r\n");
            deviceInfoXml.append("<Result>OK</Result>\r\n");
            deviceInfoXml.append("</Response>\r\n");

            CallIdHeader callIdHeader = parentPlatform.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
                    : udpSipProvider.getNewCallId();

            Request request = headerProviderPlarformProvider.createMessageRequest(parentPlatform, deviceInfoXml.toString(), fromTag, callIdHeader);
            transmitRequest(parentPlatform, request);

        } catch (SipException | ParseException | InvalidArgumentException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 向上级回复DeviceStatus查询信息
     * @param parentPlatform 平台信息
     * @param sn
     * @param fromTag
     * @return
     */
    @Override
    public boolean deviceStatusResponse(ParentPlatform parentPlatform, String sn, String fromTag) {
        if (parentPlatform == null) {
            return false;
        }
        try {
            StringBuffer deviceStatusXml = new StringBuffer(600);
            deviceStatusXml.append("<?xml version=\"1.0\" encoding=\"GB2312\"?>\r\n");
            deviceStatusXml.append("<Response>\r\n");
            deviceStatusXml.append("<CmdType>DeviceStatus</CmdType>\r\n");
            deviceStatusXml.append("<SN>" +sn + "</SN>\r\n");
            deviceStatusXml.append("<DeviceID>" + parentPlatform.getDeviceGBId() + "</DeviceID>\r\n");
            deviceStatusXml.append("<Result>OK</Result>\r\n");
            deviceStatusXml.append("<Online>ONLINE</Online>\r\n");
            deviceStatusXml.append("<Status>OK</Status>\r\n");
            deviceStatusXml.append("</Response>\r\n");

            CallIdHeader callIdHeader = parentPlatform.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
                    : udpSipProvider.getNewCallId();

            Request request = headerProviderPlarformProvider.createMessageRequest(parentPlatform, deviceStatusXml.toString(), fromTag, callIdHeader);
            transmitRequest(parentPlatform, request);

        } catch (SipException | ParseException | InvalidArgumentException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean sendNotifyMobilePosition(ParentPlatform parentPlatform, GPSMsgInfo gpsMsgInfo, SubscribeInfo subscribeInfo) {
        if (parentPlatform == null) {
            return false;
        }

        try {
            StringBuffer deviceStatusXml = new StringBuffer(600);
            deviceStatusXml.append("<?xml version=\"1.0\" encoding=\"GB2312\"?>\r\n");
            deviceStatusXml.append("<Notify>\r\n");
            deviceStatusXml.append("<CmdType>MobilePosition</CmdType>\r\n");
            deviceStatusXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
            deviceStatusXml.append("<DeviceID>" + gpsMsgInfo.getId() + "</DeviceID>\r\n");
            deviceStatusXml.append("<Time>" + gpsMsgInfo.getTime() + "</Time>\r\n");
            deviceStatusXml.append("<Longitude>" + gpsMsgInfo.getLng() + "</Longitude>\r\n");
            deviceStatusXml.append("<Latitude>" + gpsMsgInfo.getLat() + "</Latitude>\r\n");
            deviceStatusXml.append("<Speed>" + gpsMsgInfo.getSpeed() + "</Speed>\r\n");
            deviceStatusXml.append("<Direction>" + gpsMsgInfo.getDirection() + "</Direction>\r\n");
            deviceStatusXml.append("<Altitude>" + gpsMsgInfo.getAltitude() + "</Altitude>\r\n");
            deviceStatusXml.append("</Notify>\r\n");

            CallIdHeader callIdHeader = parentPlatform.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
                    : udpSipProvider.getNewCallId();
            callIdHeader.setCallId(subscribeInfo.getCallId());

            String tm = Long.toString(System.currentTimeMillis());

            Request request = headerProviderPlarformProvider.createNotifyRequest(parentPlatform,
                    deviceStatusXml.toString(),callIdHeader,
                    "z9hG4bK-" + UUID.randomUUID().toString().replace("-", ""),  subscribeInfo);
            transmitRequest(parentPlatform, request);

        } catch (SipException | ParseException | InvalidArgumentException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean sendNotifyForCatalogAddOrUpdate(String type, ParentPlatform parentPlatform, List<DeviceChannel> deviceChannels, SubscribeInfo subscribeInfo, Integer index) {
        if (parentPlatform == null || deviceChannels == null || deviceChannels.size() == 0 || subscribeInfo == null) {
            return false;
        }
        if (index == null) {
            index = 0;
        }

        try {
            if (index == deviceChannels.size() - 1) {
                return true;
            }
            Request request = getCatalogNotifyRequest(parentPlatform, deviceChannels.get(index), deviceChannels.size(), type, subscribeInfo);
            index += 1;
            Integer finalIndex = index;
            transmitRequest(parentPlatform, request, null, (eventResult -> {
                sendNotifyForCatalogAddOrUpdate(type, parentPlatform, deviceChannels, subscribeInfo, finalIndex);
            }));
        } catch (SipException | ParseException | InvalidArgumentException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private Request getCatalogNotifyRequest(ParentPlatform parentPlatform, DeviceChannel channel, int size, String type,
                                            SubscribeInfo subscribeInfo) throws ParseException, InvalidArgumentException,
            PeerUnavailableException {
        String catalogXmlContent = getCatalogXmlContent(parentPlatform, channel, size, type);

        CallIdHeader callIdHeader = parentPlatform.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
                : udpSipProvider.getNewCallId();
        callIdHeader.setCallId(subscribeInfo.getCallId());
        Request request = headerProviderPlarformProvider.createNotifyRequest(parentPlatform, catalogXmlContent,
                callIdHeader, "z9hG4bK-" + UUID.randomUUID().toString().replace("-", ""), subscribeInfo);
        return request;
    }

    private  String getCatalogXmlContent(ParentPlatform parentPlatform, DeviceChannel channel, int sumNum, String type) {
        StringBuffer catalogXml = new StringBuffer(600);
        catalogXml.append("<?xml version=\"1.0\" encoding=\"GB2312\"?>\r\n");
        catalogXml.append("<Notify>\r\n");
        catalogXml.append("<CmdType>Catalog</CmdType>\r\n");
        catalogXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        catalogXml.append("<DeviceID>" + parentPlatform.getDeviceGBId() + "</DeviceID>\r\n");
        catalogXml.append("<SumNum>" + sumNum + "</SumNum>\r\n");
        catalogXml.append("<DeviceList Num=\"1\">\r\n");
        catalogXml.append("<Item>\r\n");
        catalogXml.append("<DeviceID>" + channel.getChannelId() + "</DeviceID>\r\n");
        catalogXml.append("<Name>" + channel.getName() + "</Name>\r\n");
        catalogXml.append("<Manufacturer>" + channel.getManufacture() + "</Manufacturer>\r\n");
        catalogXml.append("<Model>" + channel.getModel() + "</Model>\r\n");
        catalogXml.append("<Owner>0</Owner>\r\n");
        catalogXml.append("<CivilCode>CivilCode</CivilCode>\r\n");
        catalogXml.append("<Address>" + channel.getAddress() + "</Address>\r\n");
        catalogXml.append("<Parental>" + channel.getParental() + "</Parental>\r\n");
        catalogXml.append("<ParentID>" + channel.getParentId() + "</ParentID>\r\n");
        catalogXml.append("<Secrecy>" + channel.getSecrecy() + "</Secrecy>\r\n");
        catalogXml.append("<RegisterWay>" + channel.getRegisterWay() + "</RegisterWay>\r\n");
        catalogXml.append("<Status>" + (channel.getStatus() == 0 ? "OFF" : "ON") + "</Status>\r\n");
        catalogXml.append("<Event>" + type + "</Event>\r\n");
        catalogXml.append("</Item>\r\n");
        catalogXml.append("</DeviceList>\r\n");
        catalogXml.append("</Notify>\r\n");
        return catalogXml.toString();
    }

    @Override
    public boolean sendNotifyForCatalogOther(String type, ParentPlatform parentPlatform, List<DeviceChannel> deviceChannels, SubscribeInfo subscribeInfo) {
        if (parentPlatform == null
                || deviceChannels == null
                || deviceChannels.size() == 0
                || subscribeInfo == null) {
            return false;
        }

        for (DeviceChannel channel : deviceChannels) {
            try {
                StringBuffer catalogXml = new StringBuffer(600);
                catalogXml.append("<?xml version=\"1.0\" encoding=\"GB2312\"?>\r\n");
                catalogXml.append("<Notify>\r\n");
                catalogXml.append("<CmdType>Catalog</CmdType>\r\n");
                catalogXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
                catalogXml.append("<DeviceID>" + parentPlatform.getDeviceGBId() + "</DeviceID>\r\n");
                catalogXml.append("<SumNum>" + deviceChannels.size() + "</SumNum>\r\n");
                catalogXml.append("<DeviceList Num=\"1\">\r\n");
                catalogXml.append("<Item>\r\n");
                catalogXml.append("<DeviceID>" + channel.getChannelId() + "</DeviceID>\r\n");
                catalogXml.append("<Event>" + type + "</Event>\r\n");
                catalogXml.append("</Item>\r\n");
                catalogXml.append("</DeviceList>\r\n");
                catalogXml.append("</Notify>\r\n");

                CallIdHeader callIdHeader = parentPlatform.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
                        : udpSipProvider.getNewCallId();
                callIdHeader.setCallId(subscribeInfo.getCallId());

                String tm = Long.toString(System.currentTimeMillis());

                Request request = headerProviderPlarformProvider.createNotifyRequest(parentPlatform, catalogXml.toString(),
                        callIdHeader,
                        "z9hG4bK-" + UUID.randomUUID().toString().replace("-", ""), subscribeInfo);
                transmitRequest(parentPlatform, request);
                Thread.sleep(200);
            } catch (SipException | ParseException | InvalidArgumentException e) {
                e.printStackTrace();
                return false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    @Override
    public boolean recordInfo(DeviceChannel deviceChannel, ParentPlatform parentPlatform, String fromTag, RecordInfo recordInfo) {
        if ( parentPlatform ==null) {
            return false;
        }
        try {
            StringBuffer recordXml = new StringBuffer(600);
            recordXml.append("<?xml version=\"1.0\" encoding=\"GB2312\"?>\r\n");
            recordXml.append("<Response>\r\n");
            recordXml.append("<CmdType>RecordInfo</CmdType>\r\n");
            recordXml.append("<SN>" +recordInfo.getSn() + "</SN>\r\n");
            recordXml.append("<DeviceID>" + recordInfo.getDeviceId() + "</DeviceID>\r\n");
            recordXml.append("<SumNum>" + recordInfo.getSumNum() + "</SumNum>\r\n");
            recordXml.append("<RecordList Num=\"" + recordInfo.getRecordList().size()+"\">\r\n");
            for (RecordItem recordItem : recordInfo.getRecordList()) {
                recordXml.append("<Item>\r\n");
                if (deviceChannel != null) {
                    recordXml.append("<DeviceID>" + recordItem.getDeviceId() + "</DeviceID>\r\n");
                    recordXml.append("<Name>" + recordItem.getName() + "</Name>\r\n");
                    recordXml.append("<StartTime>" + DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(recordItem.getStartTime()) + "</StartTime>\r\n");
                    recordXml.append("<EndTime>" + DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(recordItem.getEndTime()) + "</EndTime>\r\n");
                    recordXml.append("<Secrecy>" + recordItem.getSecrecy() + "</Secrecy>\r\n");
                    recordXml.append("<Type>" + recordItem.getType() + "</Type>\r\n");
                    if (!StringUtils.isEmpty(recordItem.getFileSize())) {
                        recordXml.append("<FileSize>" + recordItem.getFileSize() + "</FileSize>\r\n");
                    }
                    if (!StringUtils.isEmpty(recordItem.getFilePath())) {
                        recordXml.append("<FilePath>" + recordItem.getFilePath() + "</FilePath>\r\n");
                    }
                }
                recordXml.append("</Item>\r\n");
            }

            recordXml.append("</RecordList>\r\n");
            recordXml.append("</Response>\r\n");

            // callid
            CallIdHeader callIdHeader = parentPlatform.getTransport().equals("TCP") ? tcpSipProvider.getNewCallId()
                    : udpSipProvider.getNewCallId();
            Request request = headerProviderPlarformProvider.createMessageRequest(parentPlatform, recordXml.toString(), fromTag, callIdHeader);
            transmitRequest(parentPlatform, request);

        } catch (SipException | ParseException | InvalidArgumentException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
