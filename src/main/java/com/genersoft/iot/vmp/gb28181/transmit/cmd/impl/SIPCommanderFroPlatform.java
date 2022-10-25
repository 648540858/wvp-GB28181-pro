package com.genersoft.iot.vmp.gb28181.transmit.cmd.impl;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.transmit.SIPSender;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.SIPRequestHeaderPlarformProvider;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.storager.dao.dto.PlatformRegisterInfo;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.message.MessageFactoryImpl;
import gov.nist.javax.sip.message.SIPRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.*;
import javax.sip.header.*;
import javax.sip.message.Request;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Component
@DependsOn("sipLayer")
public class SIPCommanderFroPlatform implements ISIPCommanderForPlatform {

    private final Logger logger = LoggerFactory.getLogger(SIPCommanderFroPlatform.class);

    @Autowired
    private SIPRequestHeaderPlarformProvider headerProviderPlatformProvider;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private SipSubscribe sipSubscribe;

    @Autowired
    private ZLMRTPServerFactory zlmrtpServerFactory;

    @Autowired
    private SipFactory sipFactory;

    @Autowired
    private SIPSender sipSender;

    @Override
    public void register(ParentPlatform parentPlatform, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent) throws InvalidArgumentException, ParseException, SipException {
        register(parentPlatform, null, null, errorEvent, okEvent, false, true);
    }

    @Override
    public void unregister(ParentPlatform parentPlatform, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent) throws InvalidArgumentException, ParseException, SipException {
        register(parentPlatform, null, null, errorEvent, okEvent, false, false);
    }

    @Override
    public void register(ParentPlatform parentPlatform, @Nullable String callId, @Nullable WWWAuthenticateHeader www,
                            SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent, boolean registerAgain, boolean isRegister) throws SipException, InvalidArgumentException, ParseException {
            Request request;
            if (!registerAgain ) {
                CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(parentPlatform.getTransport());

                request = headerProviderPlatformProvider.createRegisterRequest(parentPlatform,
                        redisCatchStorage.getCSEQ(), SipUtils.getNewFromTag(),
                        SipUtils.getNewViaTag(), callIdHeader, isRegister);
                // 将 callid 写入缓存， 等注册成功可以更新状态
                String callIdFromHeader = callIdHeader.getCallId();
                redisCatchStorage.updatePlatformRegisterInfo(callIdFromHeader, PlatformRegisterInfo.getInstance(parentPlatform.getServerGBId(), isRegister));

                sipSubscribe.addErrorSubscribe(callIdHeader.getCallId(), (event)->{
                    if (event != null) {
                        logger.info("向上级平台 [ {} ] 注册发生错误： {} ",
                                parentPlatform.getServerGBId(),
                                event.msg);
                    }
                    redisCatchStorage.delPlatformRegisterInfo(callIdFromHeader);
                    if (errorEvent != null ) {
                        errorEvent.response(event);
                    }
                });

            }else {
                CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(parentPlatform.getTransport());
                request = headerProviderPlatformProvider.createRegisterRequest(parentPlatform, SipUtils.getNewFromTag(), null, callId, www, callIdHeader, isRegister);
            }

            sipSender.transmitRequest( request, null, okEvent);
    }

    @Override
    public String keepalive(ParentPlatform parentPlatform,SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent) throws SipException, InvalidArgumentException, ParseException {
            String characterSet = parentPlatform.getCharacterSet();
            StringBuffer keepaliveXml = new StringBuffer(200);
            keepaliveXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n");
            keepaliveXml.append("<Notify>\r\n");
            keepaliveXml.append("<CmdType>Keepalive</CmdType>\r\n");
            keepaliveXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
            keepaliveXml.append("<DeviceID>" + parentPlatform.getDeviceGBId() + "</DeviceID>\r\n");
            keepaliveXml.append("<Status>OK</Status>\r\n");
            keepaliveXml.append("</Notify>\r\n");

        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(parentPlatform.getTransport());

            Request request = headerProviderPlatformProvider.createMessageRequest(
                    parentPlatform,
                    keepaliveXml.toString(),
                    SipUtils.getNewFromTag(),
                    SipUtils.getNewViaTag(),
                    callIdHeader);
            sipSender.transmitRequest( request, errorEvent, okEvent);
        return callIdHeader.getCallId();
    }

    /**
     * 向上级回复通道信息
     * @param channel 通道信息
     * @param parentPlatform 平台信息
     * @return
     */
    @Override
    public void catalogQuery(DeviceChannel channel, ParentPlatform parentPlatform, String sn, String fromTag, int size) throws SipException, InvalidArgumentException, ParseException {

        if ( parentPlatform ==null) {
            return ;
        }
        List<DeviceChannel> channels = new ArrayList<>();
        if (channel != null) {
            channels.add(channel);
        }
        String catalogXml = getCatalogXml(channels, sn, parentPlatform, size);

        // callid
        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(parentPlatform.getTransport());

        Request request = headerProviderPlatformProvider.createMessageRequest(parentPlatform, catalogXml.toString(), fromTag, SipUtils.getNewViaTag(), callIdHeader);
        sipSender.transmitRequest( request);

    }

    @Override
    public void catalogQuery(List<DeviceChannel> channels, ParentPlatform parentPlatform, String sn, String fromTag) throws InvalidArgumentException, ParseException, SipException {
        if ( parentPlatform ==null) {
            return ;
        }
        sendCatalogResponse(channels, parentPlatform, sn, fromTag, 0);
    }
    private String getCatalogXml(List<DeviceChannel> channels, String sn, ParentPlatform parentPlatform, int size) {
        String characterSet = parentPlatform.getCharacterSet();
        StringBuffer catalogXml = new StringBuffer(600);
        catalogXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet +"\"?>\r\n");
        catalogXml.append("<Response>\r\n");
        catalogXml.append("<CmdType>Catalog</CmdType>\r\n");
        catalogXml.append("<SN>" +sn + "</SN>\r\n");
        catalogXml.append("<DeviceID>" + parentPlatform.getDeviceGBId() + "</DeviceID>\r\n");
        catalogXml.append("<SumNum>" + size + "</SumNum>\r\n");
        catalogXml.append("<DeviceList Num=\"" + channels.size() +"\">\r\n");
        if (channels.size() > 0) {
            for (DeviceChannel channel : channels) {
                if (parentPlatform.getServerGBId().equals(channel.getParentId())) {
                    channel.setParentId(parentPlatform.getDeviceGBId());
                }
                catalogXml.append("<Item>\r\n");
                // 行政区划分组只需要这两项就可以
                catalogXml.append("<DeviceID>" + channel.getChannelId() + "</DeviceID>\r\n");
                catalogXml.append("<Name>" + channel.getName() + "</Name>\r\n");
                if (channel.getParentId() != null) {
                    // 业务分组加上这一项即可，提高兼容性，
                    catalogXml.append("<ParentID>" + channel.getParentId() + "</ParentID>\r\n");
//                    catalogXml.append("<ParentID>" + parentPlatform.getDeviceGBId() + "/" + channel.getParentId() + "</ParentID>\r\n");
                }
                if (channel.getChannelId().length() == 20 && Integer.parseInt(channel.getChannelId().substring(10, 13)) == 216) {
                    // 虚拟组织增加BusinessGroupID字段
                    catalogXml.append("<BusinessGroupID>" + channel.getParentId() + "</BusinessGroupID>\r\n");
                }
                if (!channel.getChannelId().equals(parentPlatform.getDeviceGBId())) {
                    catalogXml.append("<Parental>" + channel.getParental() + "</Parental>\r\n");
                    if (channel.getParental() == 0) {
                        catalogXml.append("<Status>" + (channel.getStatus() == 0 ? "OFF" : "ON") + "</Status>\r\n");
                    }
                }
                if (channel.getParental() == 0) {
                    // 通道项
                    catalogXml.append("<Manufacturer>" + channel.getManufacture() + "</Manufacturer>\r\n");
                    catalogXml.append("<Secrecy>" + channel.getSecrecy() + "</Secrecy>\r\n");
                    catalogXml.append("<RegisterWay>" + channel.getRegisterWay() + "</RegisterWay>\r\n");
                    String civilCode = channel.getCivilCode() == null?parentPlatform.getAdministrativeDivision() : channel.getCivilCode();
                    if (channel.getChannelType() != 2) {  // 业务分组/虚拟组织/行政区划 不设置以下属性
                        catalogXml.append("<Model>" + channel.getModel() + "</Model>\r\n");
                        catalogXml.append("<Owner>" + parentPlatform.getDeviceGBId()+ "</Owner>\r\n");
                        catalogXml.append("<CivilCode>" + civilCode + "</CivilCode>\r\n");
                        if (channel.getAddress() == null) {
                            catalogXml.append("<Address></Address>\r\n");
                        }else {
                            catalogXml.append("<Address>" + channel.getAddress() + "</Address>\r\n");
                        }
                    }
                }
                catalogXml.append("</Item>\r\n");
            }
        }

        catalogXml.append("</DeviceList>\r\n");
        catalogXml.append("</Response>\r\n");
        return catalogXml.toString();
    }

    private void sendCatalogResponse(List<DeviceChannel> channels, ParentPlatform parentPlatform, String sn, String fromTag, int index) throws SipException, InvalidArgumentException, ParseException {
        if (index >= channels.size()) {
            return;
        }
        List<DeviceChannel> deviceChannels;
        if (index + parentPlatform.getCatalogGroup() < channels.size()) {
            deviceChannels = channels.subList(index, index + parentPlatform.getCatalogGroup());
        }else {
            deviceChannels = channels.subList(index, channels.size());
        }
        String catalogXml = getCatalogXml(deviceChannels, sn, parentPlatform, channels.size());
        // callid
        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(parentPlatform.getTransport());

        Request request = headerProviderPlatformProvider.createMessageRequest(parentPlatform, catalogXml, fromTag, SipUtils.getNewViaTag(), callIdHeader);
        sipSender.transmitRequest( request, null, eventResult -> {
            int indexNext = index + parentPlatform.getCatalogGroup();
            try {
                sendCatalogResponse(channels, parentPlatform, sn, fromTag, indexNext);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                logger.error("[命令发送失败] 国标级联 目录查询回复: {}", e.getMessage());
            }
        });
    }

    /**
     * 向上级回复DeviceInfo查询信息
     * @param parentPlatform 平台信息
     * @param sn
     * @param fromTag
     * @return
     */
    @Override
    public void deviceInfoResponse(ParentPlatform parentPlatform, String sn, String fromTag) throws SipException, InvalidArgumentException, ParseException {
        if (parentPlatform == null) {
            return;
        }
        String characterSet = parentPlatform.getCharacterSet();
        StringBuffer deviceInfoXml = new StringBuffer(600);
        deviceInfoXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n");
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

        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(parentPlatform.getTransport());

        Request request = headerProviderPlatformProvider.createMessageRequest(parentPlatform, deviceInfoXml.toString(), fromTag, SipUtils.getNewViaTag(), callIdHeader);
        sipSender.transmitRequest( request);
    }

    /**
     * 向上级回复DeviceStatus查询信息
     * @param parentPlatform 平台信息
     * @param sn
     * @param fromTag
     * @return
     */
    @Override
    public void deviceStatusResponse(ParentPlatform parentPlatform, String sn, String fromTag) throws SipException, InvalidArgumentException, ParseException {
        if (parentPlatform == null) {
            return ;
        }
        String characterSet = parentPlatform.getCharacterSet();
        StringBuffer deviceStatusXml = new StringBuffer(600);
        deviceStatusXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n");
        deviceStatusXml.append("<Response>\r\n");
        deviceStatusXml.append("<CmdType>DeviceStatus</CmdType>\r\n");
        deviceStatusXml.append("<SN>" +sn + "</SN>\r\n");
        deviceStatusXml.append("<DeviceID>" + parentPlatform.getDeviceGBId() + "</DeviceID>\r\n");
        deviceStatusXml.append("<Result>OK</Result>\r\n");
        deviceStatusXml.append("<Online>ONLINE</Online>\r\n");
        deviceStatusXml.append("<Status>OK</Status>\r\n");
        deviceStatusXml.append("</Response>\r\n");

        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(parentPlatform.getTransport());

        Request request = headerProviderPlatformProvider.createMessageRequest(parentPlatform, deviceStatusXml.toString(), fromTag, SipUtils.getNewViaTag(), callIdHeader);
        sipSender.transmitRequest( request);

    }

    @Override
    public void sendNotifyMobilePosition(ParentPlatform parentPlatform, GPSMsgInfo gpsMsgInfo, SubscribeInfo subscribeInfo) throws InvalidArgumentException, ParseException, NoSuchFieldException, SipException, IllegalAccessException {
        if (parentPlatform == null) {
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("[发送 移动位置订阅] {}/{}->{},{}", parentPlatform.getServerGBId(), gpsMsgInfo.getId(), gpsMsgInfo.getLng(), gpsMsgInfo.getLat());
        }

        String characterSet = parentPlatform.getCharacterSet();
        StringBuffer deviceStatusXml = new StringBuffer(600);
        deviceStatusXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n");
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

       sendNotify(parentPlatform, deviceStatusXml.toString(), subscribeInfo, eventResult -> {
            logger.error("发送NOTIFY通知消息失败。错误：{} {}", eventResult.statusCode, eventResult.msg);
        }, null);

    }

    @Override
    public void sendAlarmMessage(ParentPlatform parentPlatform, DeviceAlarm deviceAlarm) throws SipException, InvalidArgumentException, ParseException {
        if (parentPlatform == null) {
            return;
        }
        logger.info("[发送报警通知] {}/{}->{},{}: {}", parentPlatform.getServerGBId(), deviceAlarm.getChannelId(),
                deviceAlarm.getLongitude(), deviceAlarm.getLatitude(), JSONObject.toJSON(deviceAlarm));
        String characterSet = parentPlatform.getCharacterSet();
        StringBuffer deviceStatusXml = new StringBuffer(600);
        deviceStatusXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n");
        deviceStatusXml.append("<Notify>\r\n");
        deviceStatusXml.append("<CmdType>Alarm</CmdType>\r\n");
        deviceStatusXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
        deviceStatusXml.append("<DeviceID>" + deviceAlarm.getChannelId() + "</DeviceID>\r\n");
        deviceStatusXml.append("<AlarmPriority>" + deviceAlarm.getAlarmPriority() + "</AlarmPriority>\r\n");
        deviceStatusXml.append("<AlarmMethod>" + deviceAlarm.getAlarmMethod() + "</AlarmMethod>\r\n");
        deviceStatusXml.append("<AlarmTime>" + deviceAlarm.getAlarmTime() + "</AlarmTime>\r\n");
        deviceStatusXml.append("<AlarmDescription>" + deviceAlarm.getAlarmDescription() + "</AlarmDescription>\r\n");
        deviceStatusXml.append("<Longitude>" + deviceAlarm.getLongitude() + "</Longitude>\r\n");
        deviceStatusXml.append("<Latitude>" + deviceAlarm.getLatitude() + "</Latitude>\r\n");
        deviceStatusXml.append("<info>\r\n");
        deviceStatusXml.append("<AlarmType>" + deviceAlarm.getAlarmType() + "</AlarmType>\r\n");
        deviceStatusXml.append("</info>\r\n");
        deviceStatusXml.append("</Notify>\r\n");

        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(parentPlatform.getTransport());

        Request request = headerProviderPlatformProvider.createMessageRequest(parentPlatform, deviceStatusXml.toString(), SipUtils.getNewFromTag(), SipUtils.getNewViaTag(), callIdHeader);
        sipSender.transmitRequest( request);

    }

    @Override
    public void sendNotifyForCatalogAddOrUpdate(String type, ParentPlatform parentPlatform, List<DeviceChannel> deviceChannels, SubscribeInfo subscribeInfo, Integer index) throws InvalidArgumentException, ParseException, NoSuchFieldException, SipException, IllegalAccessException {
        if (parentPlatform == null || deviceChannels == null || deviceChannels.size() == 0 || subscribeInfo == null) {
            return;
        }
        if (index == null) {
            index = 0;
        }
        if (index >= deviceChannels.size()) {
            return;
        }
        List<DeviceChannel> channels;
        if (index + parentPlatform.getCatalogGroup() < deviceChannels.size()) {
            channels = deviceChannels.subList(index, index + parentPlatform.getCatalogGroup());
        }else {
            channels = deviceChannels.subList(index, deviceChannels.size());
        }
        Integer finalIndex = index;
        String catalogXmlContent = getCatalogXmlContentForCatalogAddOrUpdate(parentPlatform, channels,
                deviceChannels.size(), type, subscribeInfo);
        sendNotify(parentPlatform, catalogXmlContent, subscribeInfo, eventResult -> {
            logger.error("发送NOTIFY通知消息失败。错误：{} {}", eventResult.statusCode, eventResult.msg);
        }, (eventResult -> {
            try {
                sendNotifyForCatalogAddOrUpdate(type, parentPlatform, deviceChannels, subscribeInfo,
                        finalIndex + parentPlatform.getCatalogGroup());
            } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                     IllegalAccessException e) {
                logger.error("[命令发送失败] 国标级联 NOTIFY通知: {}", e.getMessage());
            }
        }));
    }

    private void sendNotify(ParentPlatform parentPlatform, String catalogXmlContent,
                                   SubscribeInfo subscribeInfo, SipSubscribe.Event errorEvent,  SipSubscribe.Event okEvent )
            throws SipException, ParseException, InvalidArgumentException {
		MessageFactoryImpl messageFactory = (MessageFactoryImpl) sipFactory.createMessageFactory();
        String characterSet = parentPlatform.getCharacterSet();
 		// 设置编码， 防止中文乱码
		messageFactory.setDefaultContentEncodingCharset(characterSet);

        SIPRequest notifyRequest = headerProviderPlatformProvider.createNotifyRequest(parentPlatform, catalogXmlContent, subscribeInfo);

        sipSender.transmitRequest( notifyRequest);
    }

    private  String getCatalogXmlContentForCatalogAddOrUpdate(ParentPlatform parentPlatform, List<DeviceChannel> channels, int sumNum, String type, SubscribeInfo subscribeInfo) {
        StringBuffer catalogXml = new StringBuffer(600);

        String characterSet = parentPlatform.getCharacterSet();
        catalogXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n");
        catalogXml.append("<Notify>\r\n");
        catalogXml.append("<CmdType>Catalog</CmdType>\r\n");
        catalogXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        catalogXml.append("<DeviceID>" + parentPlatform.getDeviceGBId() + "</DeviceID>\r\n");
        catalogXml.append("<SumNum>1</SumNum>\r\n");
        catalogXml.append("<DeviceList Num=\"" + channels.size() + "\">\r\n");
        if (channels.size() > 0) {
            for (DeviceChannel channel : channels) {
                if (parentPlatform.getServerGBId().equals(channel.getParentId())) {
                    channel.setParentId(parentPlatform.getDeviceGBId());
                }
                catalogXml.append("<Item>\r\n");
                // 行政区划分组只需要这两项就可以
                catalogXml.append("<DeviceID>" + channel.getChannelId() + "</DeviceID>\r\n");
                catalogXml.append("<Name>" + channel.getName() + "</Name>\r\n");
                if (channel.getParentId() != null) {
                    // 业务分组加上这一项即可，提高兼容性，
                    catalogXml.append("<ParentID>" + channel.getParentId() + "</ParentID>\r\n");
                }
                if (channel.getChannelId().length() == 20 && Integer.parseInt(channel.getChannelId().substring(10, 13)) == 216) {
                    // 虚拟组织增加BusinessGroupID字段
                    catalogXml.append("<BusinessGroupID>" + channel.getParentId() + "</BusinessGroupID>\r\n");
                }
                catalogXml.append("<Parental>" + channel.getParental() + "</Parental>\r\n");
                if (channel.getParental() == 0) {
                    // 通道项
                    catalogXml.append("<Manufacturer>" + channel.getManufacture() + "</Manufacturer>\r\n");
                    catalogXml.append("<Secrecy>" + channel.getSecrecy() + "</Secrecy>\r\n");
                    catalogXml.append("<RegisterWay>" + channel.getRegisterWay() + "</RegisterWay>\r\n");
                    catalogXml.append("<Status>" + (channel.getStatus() == 0 ? "OFF" : "ON") + "</Status>\r\n");

                    if (channel.getChannelType() != 2) {  // 业务分组/虚拟组织/行政区划 不设置以下属性
                        catalogXml.append("<Model>" + channel.getModel() + "</Model>\r\n");
                        catalogXml.append("<Owner> " + channel.getOwner()+ "</Owner>\r\n");
                        catalogXml.append("<CivilCode>" + channel.getCivilCode() + "</CivilCode>\r\n");
                        catalogXml.append("<Address>" + channel.getAddress() + "</Address>\r\n");
                    }
                    if (!"presence".equals(subscribeInfo.getEventType())) {
                        catalogXml.append("<Event>" + type + "</Event>\r\n");
                    }

                }
                catalogXml.append("</Item>\r\n");
            }
        }
        catalogXml.append("</DeviceList>\r\n");
        catalogXml.append("</Notify>\r\n");
        return catalogXml.toString();
    }

    @Override
    public void sendNotifyForCatalogOther(String type, ParentPlatform parentPlatform, List<DeviceChannel> deviceChannels,
                                             SubscribeInfo subscribeInfo, Integer index) throws InvalidArgumentException, ParseException, NoSuchFieldException, SipException, IllegalAccessException {
        if (parentPlatform == null
                || deviceChannels == null
                || deviceChannels.size() == 0
                || subscribeInfo == null) {
            logger.warn("[缺少必要参数]");
            return;
        }

        if (index == null) {
            index = 0;
        }
        if (index >= deviceChannels.size()) {
            return;
        }
        List<DeviceChannel> channels;
        if (index + parentPlatform.getCatalogGroup() < deviceChannels.size()) {
            channels = deviceChannels.subList(index, index + parentPlatform.getCatalogGroup());
        }else {
            channels = deviceChannels.subList(index, deviceChannels.size());
        }
        Integer finalIndex = index;
        String catalogXmlContent = getCatalogXmlContentForCatalogOther(parentPlatform, channels, type);
        sendNotify(parentPlatform, catalogXmlContent, subscribeInfo, eventResult -> {
            logger.error("发送NOTIFY通知消息失败。错误：{} {}", eventResult.statusCode, eventResult.msg);
        }, eventResult -> {
            try {
                sendNotifyForCatalogOther(type, parentPlatform, deviceChannels, subscribeInfo,
                        finalIndex + parentPlatform.getCatalogGroup());
            } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                     IllegalAccessException e) {
                logger.error("[命令发送失败] 国标级联 NOTIFY通知: {}", e.getMessage());
            }
        });
    }

    private String getCatalogXmlContentForCatalogOther(ParentPlatform parentPlatform, List<DeviceChannel> channels, String type) {

        String characterSet = parentPlatform.getCharacterSet();
        StringBuffer catalogXml = new StringBuffer(600);
        catalogXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n");
        catalogXml.append("<Notify>\r\n");
        catalogXml.append("<CmdType>Catalog</CmdType>\r\n");
        catalogXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        catalogXml.append("<DeviceID>" + parentPlatform.getDeviceGBId() + "</DeviceID>\r\n");
        catalogXml.append("<SumNum>1</SumNum>\r\n");
        catalogXml.append("<DeviceList Num=\" " + channels.size() + " \">\r\n");
        if (channels.size() > 0) {
            for (DeviceChannel channel : channels) {
                if (parentPlatform.getServerGBId().equals(channel.getParentId())) {
                    channel.setParentId(parentPlatform.getDeviceGBId());
                }
                catalogXml.append("<Item>\r\n");
                catalogXml.append("<DeviceID>" + channel.getChannelId() + "</DeviceID>\r\n");
                catalogXml.append("<Event>" + type + "</Event>\r\n");
                catalogXml.append("</Item>\r\n");
            }
        }
        catalogXml.append("</DeviceList>\r\n");
        catalogXml.append("</Notify>\r\n");
        return catalogXml.toString();
    }
    @Override
    public void recordInfo(DeviceChannel deviceChannel, ParentPlatform parentPlatform, String fromTag, RecordInfo recordInfo) throws SipException, InvalidArgumentException, ParseException {
        if ( parentPlatform ==null) {
            return ;
        }
        String characterSet = parentPlatform.getCharacterSet();
        StringBuffer recordXml = new StringBuffer(600);
        recordXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n");
        recordXml.append("<Response>\r\n");
        recordXml.append("<CmdType>RecordInfo</CmdType>\r\n");
        recordXml.append("<SN>" +recordInfo.getSn() + "</SN>\r\n");
        recordXml.append("<DeviceID>" + recordInfo.getDeviceId() + "</DeviceID>\r\n");
        recordXml.append("<SumNum>" + recordInfo.getSumNum() + "</SumNum>\r\n");
        if (recordInfo.getRecordList() == null ) {
            recordXml.append("<RecordList Num=\"0\">\r\n");
        }else {
            recordXml.append("<RecordList Num=\"" + recordInfo.getRecordList().size()+"\">\r\n");
            if (recordInfo.getRecordList().size() > 0) {
                for (RecordItem recordItem : recordInfo.getRecordList()) {
                    recordXml.append("<Item>\r\n");
                    if (deviceChannel != null) {
                        recordXml.append("<DeviceID>" + recordItem.getDeviceId() + "</DeviceID>\r\n");
                        recordXml.append("<Name>" + recordItem.getName() + "</Name>\r\n");
                        recordXml.append("<StartTime>" + DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(recordItem.getStartTime()) + "</StartTime>\r\n");
                        recordXml.append("<EndTime>" + DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(recordItem.getEndTime()) + "</EndTime>\r\n");
                        recordXml.append("<Secrecy>" + recordItem.getSecrecy() + "</Secrecy>\r\n");
                        recordXml.append("<Type>" + recordItem.getType() + "</Type>\r\n");
                        if (!ObjectUtils.isEmpty(recordItem.getFileSize())) {
                            recordXml.append("<FileSize>" + recordItem.getFileSize() + "</FileSize>\r\n");
                        }
                        if (!ObjectUtils.isEmpty(recordItem.getFilePath())) {
                            recordXml.append("<FilePath>" + recordItem.getFilePath() + "</FilePath>\r\n");
                        }
                    }
                    recordXml.append("</Item>\r\n");
                }
            }
        }

        recordXml.append("</RecordList>\r\n");
        recordXml.append("</Response>\r\n");

        // callid
        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(parentPlatform.getTransport());

        Request request = headerProviderPlatformProvider.createMessageRequest(parentPlatform, recordXml.toString(), fromTag, SipUtils.getNewViaTag(), callIdHeader);
        sipSender.transmitRequest( request);

    }

    @Override
    public void sendMediaStatusNotify(ParentPlatform platform, SendRtpItem sendRtpItem) throws SipException, InvalidArgumentException, ParseException {
        if (sendRtpItem == null || platform == null) {
            return;
        }


        String characterSet = platform.getCharacterSet();
        StringBuffer mediaStatusXml = new StringBuffer(200);
        mediaStatusXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n");
        mediaStatusXml.append("<Notify>\r\n");
        mediaStatusXml.append("<CmdType>MediaStatus</CmdType>\r\n");
        mediaStatusXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
        mediaStatusXml.append("<DeviceID>" + sendRtpItem.getChannelId() + "</DeviceID>\r\n");
        mediaStatusXml.append("<NotifyType>121</NotifyType>\r\n");
        mediaStatusXml.append("</Notify>\r\n");

        SIPRequest messageRequest = (SIPRequest)headerProviderPlatformProvider.createMessageRequest(platform, mediaStatusXml.toString(),
                sendRtpItem);

        sipSender.transmitRequest(messageRequest);

    }

    @Override
    public void streamByeCmd(ParentPlatform platform, String callId) throws SipException, InvalidArgumentException, ParseException {
        if (platform == null) {
            return;
        }
        SendRtpItem sendRtpItem = redisCatchStorage.querySendRTPServer(platform.getServerGBId(), null, null, callId);
        if (sendRtpItem != null) {
            streamByeCmd(platform, sendRtpItem);
        }
    }

    @Override
    public void streamByeCmd(ParentPlatform platform, SendRtpItem sendRtpItem) throws SipException, InvalidArgumentException, ParseException {
        if (sendRtpItem == null ) {
            logger.info("[向上级发送BYE]， sendRtpItem 为NULL");
            return;
        }
        if (platform == null) {
            logger.info("[向上级发送BYE]， platform 为NULL");
            return;
        }
        logger.info("[向上级发送BYE]， {}/{}", platform.getServerGBId(), sendRtpItem.getChannelId());
        String mediaServerId = sendRtpItem.getMediaServerId();
        MediaServerItem mediaServerItem = mediaServerService.getOne(mediaServerId);
        if (mediaServerItem != null) {
            mediaServerService.releaseSsrc(mediaServerItem.getId(), sendRtpItem.getSsrc());
            zlmrtpServerFactory.closeRTPServer(mediaServerItem, sendRtpItem.getStreamId());
        }
        SIPRequest byeRequest = headerProviderPlatformProvider.createByeRequest(platform, sendRtpItem);
        if (byeRequest == null) {
            logger.warn("[向上级发送bye]：无法创建 byeRequest");
        }
        sipSender.transmitRequest(byeRequest);
    }
}
