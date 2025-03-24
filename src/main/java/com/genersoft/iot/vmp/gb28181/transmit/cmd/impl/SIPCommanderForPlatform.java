package com.genersoft.iot.vmp.gb28181.transmit.cmd.impl;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.SIPSender;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.SIPRequestHeaderPlarformProvider;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.hook.Hook;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.hook.HookType;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.dto.PlatformRegisterInfo;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.utils.GitUtil;
import gov.nist.javax.sip.message.MessageFactoryImpl;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.header.CallIdHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@DependsOn("sipLayer")
public class SIPCommanderForPlatform implements ISIPCommanderForPlatform {

    @Autowired
    private SIPRequestHeaderPlarformProvider headerProviderPlatformProvider;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private SipLayer sipLayer;

    @Autowired
    private SIPSender sipSender;

    @Autowired
    private HookSubscribe subscribe;

    @Autowired
    private UserSetting userSetting;


    @Autowired
    private SipInviteSessionManager sessionManager;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private GitUtil gitUtil;

    @Override
    public void register(Platform parentPlatform, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent) throws InvalidArgumentException, ParseException, SipException {
        register(parentPlatform, null, null, errorEvent, okEvent, true);
    }

    @Override
    public void register(Platform parentPlatform, SipTransactionInfo sipTransactionInfo, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent) throws InvalidArgumentException, ParseException, SipException {

        register(parentPlatform, sipTransactionInfo, null, errorEvent, okEvent, true);
    }

    @Override
    public void unregister(Platform parentPlatform, SipTransactionInfo sipTransactionInfo, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent) throws InvalidArgumentException, ParseException, SipException {
        register(parentPlatform, sipTransactionInfo, null, errorEvent, okEvent, false);
    }

    @Override
    public void register(Platform parentPlatform, @Nullable SipTransactionInfo sipTransactionInfo, @Nullable WWWAuthenticateHeader www,
                         SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent, boolean isRegister) throws SipException, InvalidArgumentException, ParseException {
            Request request;

            CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(parentPlatform.getDeviceIp(),parentPlatform.getTransport());
            String fromTag = SipUtils.getNewFromTag();
            String toTag = null;
            if (sipTransactionInfo != null ) {
                if (sipTransactionInfo.getCallId() != null) {
                    callIdHeader.setCallId(sipTransactionInfo.getCallId());
                }
                if (sipTransactionInfo.getFromTag() != null) {
                    fromTag = sipTransactionInfo.getFromTag();
                }
                if (sipTransactionInfo.getToTag() != null) {
                    toTag = sipTransactionInfo.getToTag();
                }
            }

            if (www == null ) {
                request = headerProviderPlatformProvider.createRegisterRequest(parentPlatform,
                        redisCatchStorage.getCSEQ(), fromTag,
                        toTag, callIdHeader, isRegister? parentPlatform.getExpires() : 0);
                // 将 callid 写入缓存， 等注册成功可以更新状态
                String callIdFromHeader = callIdHeader.getCallId();
                redisCatchStorage.updatePlatformRegisterInfo(callIdFromHeader, PlatformRegisterInfo.getInstance(parentPlatform.getServerGBId(), isRegister));
            }else {
                request = headerProviderPlatformProvider.createRegisterRequest(parentPlatform, fromTag, toTag, www, callIdHeader, isRegister? parentPlatform.getExpires() : 0);
            }

            sipSender.transmitRequest(parentPlatform.getDeviceIp(), request, (event)->{
                if (event != null) {
                    log.info("[国标级联]：{},  注册失败: {} ", parentPlatform.getServerGBId(), event.msg);
                }
                redisCatchStorage.delPlatformRegisterInfo(callIdHeader.getCallId());
                if (errorEvent != null ) {
                    errorEvent.response(event);
                }
            }, okEvent, 2000L);
    }

    @Override
    public String keepalive(Platform parentPlatform, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent) throws SipException, InvalidArgumentException, ParseException {
            String characterSet = parentPlatform.getCharacterSet();
            StringBuffer keepaliveXml = new StringBuffer(200);
            keepaliveXml.append("<?xml version=\"1.0\" encoding=\"")
                    .append(characterSet).append("\"?>\r\n")
                    .append("<Notify>\r\n")
                    .append("<CmdType>Keepalive</CmdType>\r\n")
                    .append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n")
                    .append("<DeviceID>" + parentPlatform.getDeviceGBId() + "</DeviceID>\r\n")
                    .append("<Status>OK</Status>\r\n")
                    .append("</Notify>\r\n");

        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(parentPlatform.getDeviceIp(),parentPlatform.getTransport());

        Request request = headerProviderPlatformProvider.createMessageRequest(
                parentPlatform,
                keepaliveXml.toString(),
                SipUtils.getNewFromTag(),
                SipUtils.getNewViaTag(),
                callIdHeader);

        sipSender.transmitRequest(parentPlatform.getDeviceIp(), request, errorEvent, okEvent);
        return callIdHeader.getCallId();
    }

    /**
     * 向上级回复通道信息
     * @param channel 通道信息
     * @param parentPlatform 平台信息
     */
    @Override
    public void catalogQuery(CommonGBChannel channel, Platform parentPlatform, String sn, String fromTag, int size) throws SipException, InvalidArgumentException, ParseException {

        if ( parentPlatform ==null) {
            return ;
        }
        List<CommonGBChannel> channels = new ArrayList<>();
        if (channel != null) {
            channels.add(channel);
        }
        String catalogXml = getCatalogXml(channels, sn, parentPlatform, size);

        // callid
        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(parentPlatform.getDeviceIp(),parentPlatform.getTransport());

        Request request = headerProviderPlatformProvider.createMessageRequest(parentPlatform, catalogXml.toString(), fromTag, SipUtils.getNewViaTag(), callIdHeader);
        sipSender.transmitRequest(parentPlatform.getDeviceIp(), request);

    }

    @Override
    public void catalogQuery(List<CommonGBChannel> channels, Platform parentPlatform, String sn, String fromTag) throws InvalidArgumentException, ParseException, SipException {
        if ( parentPlatform ==null) {
            return ;
        }
        sendCatalogResponse(channels, parentPlatform, sn, fromTag, 0, true);
    }
    private String getCatalogXml(List<CommonGBChannel> channels, String sn, Platform platform, int size) {
        String characterSet = platform.getCharacterSet();
        StringBuffer catalogXml = new StringBuffer(600);
        catalogXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet +"\"?>\r\n")
                .append("<Response>\r\n")
                .append("<CmdType>Catalog</CmdType>\r\n")
                .append("<SN>" +sn + "</SN>\r\n")
                .append("<DeviceID>" + platform.getDeviceGBId() + "</DeviceID>\r\n")
                .append("<SumNum>" + size + "</SumNum>\r\n")
                .append("<DeviceList Num=\"" + channels.size() +"\">\r\n");
        if (!channels.isEmpty()) {
            for (CommonGBChannel channel : channels) {
                catalogXml.append(channel.encode(platform.getDeviceGBId()));
            }
        }

        catalogXml.append("</DeviceList>\r\n");
        catalogXml.append("</Response>\r\n");
        return catalogXml.toString();
    }

    private void sendCatalogResponse(List<CommonGBChannel> channels, Platform parentPlatform, String sn, String fromTag, int index, boolean sendAfterResponse) throws SipException, InvalidArgumentException, ParseException {
        if (index > channels.size()) {
            return;
        }
        List<CommonGBChannel> deviceChannels;
        if (index + parentPlatform.getCatalogGroup() < channels.size()) {
            deviceChannels = channels.subList(index, index + parentPlatform.getCatalogGroup());
        }else {
            deviceChannels = channels.subList(index, channels.size());
        }
        if(deviceChannels.isEmpty()) {
            return;
        }
        String catalogXml = getCatalogXml(deviceChannels, sn, parentPlatform, channels.size());
        // callid
        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(parentPlatform.getDeviceIp(),parentPlatform.getTransport());

        SIPRequest request = (SIPRequest)headerProviderPlatformProvider.createMessageRequest(parentPlatform, catalogXml, fromTag, SipUtils.getNewViaTag(), callIdHeader);

        String timeoutTaskKey = "catalog_task_" + parentPlatform.getServerGBId() + sn;

        String callId = request.getCallIdHeader().getCallId();

        log.info("[命令发送] 国标级联{} 目录查询回复: 共{}条，已发送{}条", parentPlatform.getServerGBId(),
                channels.size(), Math.min(index + parentPlatform.getCatalogGroup(), channels.size()));
        log.debug(catalogXml);
        if (sendAfterResponse) {
            // 默认按照收到200回复后发送下一条， 如果超时收不到回复，就以30毫秒的间隔直接发送。
            sipSender.transmitRequest(parentPlatform.getDeviceIp(), request, eventResult -> {
                if (eventResult.type.equals(SipSubscribe.EventResultType.timeout)) {
                    // 消息发送超时, 以30毫秒的间隔直接发送
                    int indexNext = index + parentPlatform.getCatalogGroup();
                    try {
                        sendCatalogResponse(channels, parentPlatform, sn, fromTag, indexNext, false);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        log.error("[命令发送失败] 国标级联 目录查询回复: {}", e.getMessage());
                    }
                    return;
                }
                log.error("[目录推送失败] 国标级联 platform : {}, code: {}, msg: {}, 停止发送", parentPlatform.getServerGBId(), eventResult.statusCode, eventResult.msg);
                dynamicTask.stop(timeoutTaskKey);
            }, eventResult -> {
                dynamicTask.stop(timeoutTaskKey);
                int indexNext = index + parentPlatform.getCatalogGroup();
                try {
                    sendCatalogResponse(channels, parentPlatform, sn, fromTag, indexNext, true);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] 国标级联 目录查询回复: {}", e.getMessage());
                }
            });
        }else {
            sipSender.transmitRequest(parentPlatform.getDeviceIp(), request, eventResult -> {
                log.error("[目录推送失败] 国标级联 platform : {}, code: {}, msg: {}, 停止发送", parentPlatform.getServerGBId(), eventResult.statusCode, eventResult.msg);
                dynamicTask.stop(timeoutTaskKey);
            }, null);
            dynamicTask.startDelay(timeoutTaskKey, ()->{
                int indexNext = index + parentPlatform.getCatalogGroup();
                try {
                    sendCatalogResponse(channels, parentPlatform, sn, fromTag, indexNext, false);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] 国标级联 目录查询回复: {}", e.getMessage());
                }
            }, 100);
        }
    }

    /**
     * 向上级回复DeviceInfo查询信息
     * @param parentPlatform 平台信息
     * @param sn
     * @param fromTag
     * @return
     */
    @Override
    public void deviceInfoResponse(Platform parentPlatform, Device device, String sn, String fromTag) throws SipException, InvalidArgumentException, ParseException {
        if (parentPlatform == null) {
            return;
        }
        String deviceId = device == null ? parentPlatform.getDeviceGBId() : device.getDeviceId();
        String deviceName = device == null ? parentPlatform.getName() : device.getName();
        String manufacturer = device == null ? "WVP-28181-PRO" : device.getManufacturer();
        String model = device == null ? "platform" : device.getModel();
        String firmware = device == null ? gitUtil.getBuildVersion() : device.getFirmware();
        String characterSet = parentPlatform.getCharacterSet();
        StringBuffer deviceInfoXml = new StringBuffer(600);
        deviceInfoXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n");
        deviceInfoXml.append("<Response>\r\n");
        deviceInfoXml.append("<CmdType>DeviceInfo</CmdType>\r\n");
        deviceInfoXml.append("<SN>" +sn + "</SN>\r\n");
        deviceInfoXml.append("<DeviceID>" + deviceId + "</DeviceID>\r\n");
        deviceInfoXml.append("<DeviceName>" + deviceName + "</DeviceName>\r\n");
        deviceInfoXml.append("<Manufacturer>" + manufacturer + "</Manufacturer>\r\n");
        deviceInfoXml.append("<Model>" + model + "</Model>\r\n");
        deviceInfoXml.append("<Firmware>" + firmware + "</Firmware>\r\n");
        deviceInfoXml.append("<Result>OK</Result>\r\n");
        deviceInfoXml.append("</Response>\r\n");

        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(parentPlatform.getDeviceIp(),parentPlatform.getTransport());

        Request request = headerProviderPlatformProvider.createMessageRequest(parentPlatform, deviceInfoXml.toString(), fromTag, SipUtils.getNewViaTag(), callIdHeader);
        sipSender.transmitRequest(parentPlatform.getDeviceIp(), request);
    }


    /**
     * 向上级回复DeviceStatus查询信息
     * @param parentPlatform 平台信息
     * @param sn
     * @param fromTag
     * @return
     */
    @Override
    public void deviceStatusResponse(Platform parentPlatform, String channelId, String sn, String fromTag, boolean status) throws SipException, InvalidArgumentException, ParseException {
        if (parentPlatform == null) {
            return ;
        }
        String statusStr = (status)?"ONLINE":"OFFLINE";
        String characterSet = parentPlatform.getCharacterSet();
        StringBuffer deviceStatusXml = new StringBuffer(600);
        deviceStatusXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n")
                .append("<Response>\r\n")
                .append("<CmdType>DeviceStatus</CmdType>\r\n")
                .append("<SN>" +sn + "</SN>\r\n")
                .append("<DeviceID>" + channelId + "</DeviceID>\r\n")
                .append("<Result>OK</Result>\r\n")
                .append("<Online>"+statusStr+"</Online>\r\n")
                .append("<Status>OK</Status>\r\n")
                .append("</Response>\r\n");

        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(parentPlatform.getDeviceIp(),parentPlatform.getTransport());

        Request request = headerProviderPlatformProvider.createMessageRequest(parentPlatform, deviceStatusXml.toString(), fromTag, SipUtils.getNewViaTag(), callIdHeader);
        sipSender.transmitRequest(parentPlatform.getDeviceIp(), request);
    }

    @Override
    public void sendNotifyMobilePosition(Platform parentPlatform, GPSMsgInfo gpsMsgInfo, CommonGBChannel channel, SubscribeInfo subscribeInfo) throws InvalidArgumentException, ParseException, NoSuchFieldException, SipException, IllegalAccessException {
        if (parentPlatform == null) {
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("[发送 移动位置订阅] {}/{}->{},{}", parentPlatform.getServerGBId(), gpsMsgInfo.getId(), gpsMsgInfo.getLng(), gpsMsgInfo.getLat());
        }

        String characterSet = parentPlatform.getCharacterSet();
        StringBuffer deviceStatusXml = new StringBuffer(600);
        deviceStatusXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n")
                .append("<Notify>\r\n")
                .append("<CmdType>MobilePosition</CmdType>\r\n")
                .append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n")
                .append("<DeviceID>" + channel.getGbDeviceId() + "</DeviceID>\r\n")
                .append("<Time>" + DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(gpsMsgInfo.getTime()) + "</Time>\r\n")
                .append("<Longitude>" + gpsMsgInfo.getLng() + "</Longitude>\r\n")
                .append("<Latitude>" + gpsMsgInfo.getLat() + "</Latitude>\r\n")
                .append("<Speed>" + gpsMsgInfo.getSpeed() + "</Speed>\r\n")
                .append("<Direction>" + gpsMsgInfo.getDirection() + "</Direction>\r\n")
                .append("<Altitude>" + gpsMsgInfo.getAltitude() + "</Altitude>\r\n")
                .append("</Notify>\r\n");

       sendNotify(parentPlatform, deviceStatusXml.toString(), subscribeInfo, eventResult -> {
            log.error("发送NOTIFY通知消息失败。错误：{} {}", eventResult.statusCode, eventResult.msg);
        }, null);

    }

    @Override
    public void sendAlarmMessage(Platform parentPlatform, DeviceAlarm deviceAlarm) throws SipException, InvalidArgumentException, ParseException {
        if (parentPlatform == null) {
            return;
        }
        log.info("[发送报警通知]平台： {}/{}->{},{}: {}", parentPlatform.getServerGBId(), deviceAlarm.getChannelId(),
                deviceAlarm.getLongitude(), deviceAlarm.getLatitude(), JSON.toJSONString(deviceAlarm));
        String characterSet = parentPlatform.getCharacterSet();
        StringBuffer deviceStatusXml = new StringBuffer(600);
        deviceStatusXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n")
                .append("<Notify>\r\n")
                .append("<CmdType>Alarm</CmdType>\r\n")
                .append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n")
                .append("<DeviceID>" + deviceAlarm.getChannelId() + "</DeviceID>\r\n")
                .append("<AlarmPriority>" + deviceAlarm.getAlarmPriority() + "</AlarmPriority>\r\n")
                .append("<AlarmMethod>" + deviceAlarm.getAlarmMethod() + "</AlarmMethod>\r\n")
                .append("<AlarmTime>" + DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(deviceAlarm.getAlarmTime()) + "</AlarmTime>\r\n")
                .append("<AlarmDescription>" + deviceAlarm.getAlarmDescription() + "</AlarmDescription>\r\n")
                .append("<Longitude>" + deviceAlarm.getLongitude() + "</Longitude>\r\n")
                .append("<Latitude>" + deviceAlarm.getLatitude() + "</Latitude>\r\n")
                .append("<info>\r\n")
                .append("<AlarmType>" + deviceAlarm.getAlarmType() + "</AlarmType>\r\n")
                .append("</info>\r\n")
                .append("</Notify>\r\n");

        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(parentPlatform.getDeviceIp(),parentPlatform.getTransport());

        Request request = headerProviderPlatformProvider.createMessageRequest(parentPlatform, deviceStatusXml.toString(), SipUtils.getNewFromTag(), SipUtils.getNewViaTag(), callIdHeader);
        sipSender.transmitRequest(parentPlatform.getDeviceIp(), request);

    }

    @Override
    public void sendNotifyForCatalogAddOrUpdate(String type, Platform parentPlatform, List<CommonGBChannel> deviceChannels, SubscribeInfo subscribeInfo, Integer index) throws InvalidArgumentException, ParseException, NoSuchFieldException, SipException, IllegalAccessException {
        if (parentPlatform == null || deviceChannels == null || deviceChannels.isEmpty() || subscribeInfo == null) {
            return;
        }
        if (index == null) {
            index = 0;
        }
        if (index >= deviceChannels.size()) {
            return;
        }
        List<CommonGBChannel> channels;
        if (index + parentPlatform.getCatalogGroup() < deviceChannels.size()) {
            channels = deviceChannels.subList(index, index + parentPlatform.getCatalogGroup());
        }else {
            channels = deviceChannels.subList(index, deviceChannels.size());
        }
        Integer finalIndex = index;
        String catalogXmlContent = getCatalogXmlContentForCatalogAddOrUpdate(parentPlatform, channels,
                deviceChannels.size(), type, subscribeInfo);
        log.info("[发送NOTIFY通知]类型： {}，发送数量： {}", type, channels.size());
        sendNotify(parentPlatform, catalogXmlContent, subscribeInfo, eventResult -> {
            log.error("发送NOTIFY通知消息失败。错误：{} {}", eventResult.statusCode, eventResult.msg);
            log.error(catalogXmlContent);
        }, (eventResult -> {
            try {
                sendNotifyForCatalogAddOrUpdate(type, parentPlatform, deviceChannels, subscribeInfo,
                        finalIndex + parentPlatform.getCatalogGroup());
            } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                     IllegalAccessException e) {
                log.error("[命令发送失败] 国标级联 NOTIFY通知: {}", e.getMessage());
            }
        }));
    }

    private void sendNotify(Platform parentPlatform, String catalogXmlContent,
                            SubscribeInfo subscribeInfo, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent )
            throws SipException, ParseException, InvalidArgumentException {
        MessageFactoryImpl messageFactory = (MessageFactoryImpl) SipFactory.getInstance().createMessageFactory();
        String characterSet = parentPlatform.getCharacterSet();
        // 设置编码， 防止中文乱码
        messageFactory.setDefaultContentEncodingCharset(characterSet);

        SIPRequest notifyRequest = headerProviderPlatformProvider.createNotifyRequest(parentPlatform, catalogXmlContent, subscribeInfo);

        sipSender.transmitRequest(parentPlatform.getDeviceIp(), notifyRequest, errorEvent, okEvent);
    }

    private  String getCatalogXmlContentForCatalogAddOrUpdate(Platform platform, List<CommonGBChannel> channels, int sumNum, String type, SubscribeInfo subscribeInfo) {
        StringBuffer catalogXml = new StringBuffer(600);
        String characterSet = platform.getCharacterSet();
        catalogXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n")
                .append("<Notify>\r\n")
                .append("<CmdType>Catalog</CmdType>\r\n")
                .append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n")
                .append("<DeviceID>" + platform.getDeviceGBId() + "</DeviceID>\r\n")
                .append("<SumNum>"+ sumNum +"</SumNum>\r\n")
                .append("<DeviceList Num=\"" + channels.size() + "\">\r\n");
        if (!channels.isEmpty()) {
            for (CommonGBChannel channel : channels) {
                catalogXml.append(channel.encode(type, platform.getDeviceGBId()));
            }
        }
        catalogXml.append("</DeviceList>\r\n")
                .append("</Notify>\r\n");
        return catalogXml.toString();
    }

    @Override
    public void sendNotifyForCatalogOther(String type, Platform parentPlatform, List<CommonGBChannel> deviceChannels,
                                          SubscribeInfo subscribeInfo, Integer index) throws InvalidArgumentException, ParseException, NoSuchFieldException, SipException, IllegalAccessException {
        if (parentPlatform == null
                || deviceChannels == null
                || deviceChannels.size() == 0
                || subscribeInfo == null) {
            log.warn("[缺少必要参数]");
            return;
        }

        if (index == null) {
            index = 0;
        }
        if (index >= deviceChannels.size()) {
            return;
        }
        List<CommonGBChannel> channels;
        if (index + parentPlatform.getCatalogGroup() < deviceChannels.size()) {
            channels = deviceChannels.subList(index, index + parentPlatform.getCatalogGroup());
        }else {
            channels = deviceChannels.subList(index, deviceChannels.size());
        }
        log.info("[发送NOTIFY通知]类型： {}，发送数量： {}", type, channels.size());
        Integer finalIndex = index;
        String catalogXmlContent = getCatalogXmlContentForCatalogOther(parentPlatform, channels, type);
        sendNotify(parentPlatform, catalogXmlContent, subscribeInfo, eventResult -> {
            log.error("发送NOTIFY通知消息失败。错误：{} {}", eventResult.statusCode, eventResult.msg);
        }, eventResult -> {
            try {
                sendNotifyForCatalogOther(type, parentPlatform, deviceChannels, subscribeInfo,
                        finalIndex + parentPlatform.getCatalogGroup());
            } catch (InvalidArgumentException | ParseException | NoSuchFieldException | SipException |
                     IllegalAccessException e) {
                log.error("[命令发送失败] 国标级联 NOTIFY通知: {}", e.getMessage());
            }
        });
    }

    private String getCatalogXmlContentForCatalogOther(Platform platform, List<CommonGBChannel> channels, String type) {

        String characterSet = platform.getCharacterSet();
        StringBuffer catalogXml = new StringBuffer(600);
        catalogXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n")
                .append("<Notify>\r\n")
                .append("<CmdType>Catalog</CmdType>\r\n")
                .append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n")
                .append("<DeviceID>" + platform.getDeviceGBId() + "</DeviceID>\r\n")
                .append("<SumNum>1</SumNum>\r\n")
                .append("<DeviceList Num=\" " + channels.size() + " \">\r\n");
        if (!channels.isEmpty()) {
            for (CommonGBChannel channel : channels) {
               catalogXml.append(channel.encode(type, platform.getDeviceGBId()));
            }
        }
        catalogXml.append("</DeviceList>\r\n")
                .append("</Notify>\r\n");
        return catalogXml.toString();
    }
    @Override
    public void recordInfo(CommonGBChannel deviceChannel, Platform parentPlatform, String fromTag, RecordInfo recordInfo) throws SipException, InvalidArgumentException, ParseException {
        if ( parentPlatform ==null) {
            return ;
        }
        log.info("[国标级联] 发送录像数据通道： {}", recordInfo.getChannelId());
        String characterSet = parentPlatform.getCharacterSet();
        StringBuffer recordXml = new StringBuffer(600);
        recordXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n")
                .append("<Response>\r\n")
                .append("<CmdType>RecordInfo</CmdType>\r\n")
                .append("<SN>" +recordInfo.getSn() + "</SN>\r\n")
                .append("<DeviceID>" + deviceChannel.getGbDeviceId() + "</DeviceID>\r\n")
                .append("<SumNum>" + recordInfo.getSumNum() + "</SumNum>\r\n");
        if (recordInfo.getRecordList() == null ) {
            recordXml.append("<RecordList Num=\"0\">\r\n");
        }else {
            recordXml.append("<RecordList Num=\"" + recordInfo.getRecordList().size()+"\">\r\n");
            if (recordInfo.getRecordList().size() > 0) {
                for (RecordItem recordItem : recordInfo.getRecordList()) {
                    recordXml.append("<Item>\r\n");
                    if (deviceChannel != null) {
                        recordXml.append("<DeviceID>" + deviceChannel.getGbDeviceId() + "</DeviceID>\r\n")
                                .append("<Name>" + recordItem.getName() + "</Name>\r\n")
                                .append("<StartTime>" + DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(recordItem.getStartTime()) + "</StartTime>\r\n")
                                .append("<EndTime>" + DateUtil.yyyy_MM_dd_HH_mm_ssToISO8601(recordItem.getEndTime()) + "</EndTime>\r\n")
                                .append("<Secrecy>" + recordItem.getSecrecy() + "</Secrecy>\r\n")
                                .append("<Type>" + recordItem.getType() + "</Type>\r\n");
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

        recordXml.append("</RecordList>\r\n")
                .append("</Response>\r\n");
        log.debug("[国标级联] 发送录像数据通道：{}, 内容： {}", recordInfo.getChannelId(), recordXml);
        // callid
        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(parentPlatform.getDeviceIp(),parentPlatform.getTransport());

        Request request = headerProviderPlatformProvider.createMessageRequest(parentPlatform, recordXml.toString(), fromTag, SipUtils.getNewViaTag(), callIdHeader);
        sipSender.transmitRequest(parentPlatform.getDeviceIp(), request, null, eventResult -> {
            log.info("[国标级联] 发送录像数据通道：{}, 发送成功", recordInfo.getChannelId());
        });

    }

    @Override
    public void sendMediaStatusNotify(Platform parentPlatform, SendRtpInfo sendRtpInfo, CommonGBChannel channel) throws SipException, InvalidArgumentException, ParseException {
        if (channel == null || parentPlatform == null) {
            return;
        }

        String characterSet = parentPlatform.getCharacterSet();
        StringBuffer mediaStatusXml = new StringBuffer(200);
        mediaStatusXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n")
                .append("<Notify>\r\n")
                .append("<CmdType>MediaStatus</CmdType>\r\n")
                .append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n")
                .append("<DeviceID>" + channel.getGbDeviceId() + "</DeviceID>\r\n")
                .append("<NotifyType>121</NotifyType>\r\n")
                .append("</Notify>\r\n");

        SIPRequest messageRequest = (SIPRequest)headerProviderPlatformProvider.createMessageRequest(parentPlatform, mediaStatusXml.toString(),
                sendRtpInfo);

        sipSender.transmitRequest(parentPlatform.getDeviceIp(),messageRequest);

    }

    @Override
    public synchronized void streamByeCmd(Platform platform, SendRtpInfo sendRtpItem, CommonGBChannel channel) throws SipException, InvalidArgumentException, ParseException {
        if (sendRtpItem == null ) {
            log.info("[向上级发送BYE]， sendRtpItem 为NULL");
            return;
        }
        if (platform == null) {
            log.info("[向上级发送BYE]， platform 为NULL");
            return;
        }
        log.info("[向上级发送BYE]， {}/{}", platform.getServerGBId(), sendRtpItem.getChannelId());
        String mediaServerId = sendRtpItem.getMediaServerId();
        MediaServer mediaServerItem = mediaServerService.getOne(mediaServerId);
        if (mediaServerItem != null) {
            mediaServerService.releaseSsrc(mediaServerItem.getId(), sendRtpItem.getSsrc());
            mediaServerService.closeRTPServer(mediaServerItem, sendRtpItem.getStream());
        }
        SIPRequest byeRequest = headerProviderPlatformProvider.createByeRequest(platform, sendRtpItem, channel);
        if (byeRequest == null) {
            log.warn("[向上级发送bye]：无法创建 byeRequest");
        }
        sipSender.transmitRequest(platform.getDeviceIp(),byeRequest);
    }

    @Override
    public void streamByeCmd(Platform platform, CommonGBChannel channel, String app, String stream, String callId, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException, SsrcTransactionNotFoundException {

        SsrcTransaction ssrcTransaction = null;
        if (callId != null) {
            ssrcTransaction = sessionManager.getSsrcTransactionByCallId(callId);
        }else if (stream != null) {
            ssrcTransaction = sessionManager.getSsrcTransactionByStream(app, stream);
        }
        if (ssrcTransaction == null) {
            throw new SsrcTransactionNotFoundException(platform.getServerGBId(), channel.getGbDeviceId(), callId, stream);
        }

        mediaServerService.releaseSsrc(ssrcTransaction.getMediaServerId(), ssrcTransaction.getSsrc());
        mediaServerService.closeRTPServer(ssrcTransaction.getMediaServerId(), ssrcTransaction.getStream());
        sessionManager.removeByStream(ssrcTransaction.getApp(), ssrcTransaction.getStream());

        Request byteRequest = headerProviderPlatformProvider.createByteRequest(platform, channel.getGbDeviceId(), ssrcTransaction.getSipTransactionInfo());
        sipSender.transmitRequest(sipLayer.getLocalIp(platform.getDeviceIp()), byteRequest, null, okEvent);
    }

    @Override
    public void broadcastResultCmd(Platform platform, CommonGBChannel deviceChannel, String sn, boolean result, SipSubscribe.Event errorEvent, SipSubscribe.Event okEvent) throws InvalidArgumentException, SipException, ParseException {
        if (platform == null || deviceChannel == null) {
            return;
        }
        String characterSet = platform.getCharacterSet();
        StringBuffer mediaStatusXml = new StringBuffer(200);
        mediaStatusXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n")
                      .append("<Response>\r\n")
                      .append("<CmdType>Broadcast</CmdType>\r\n")
                      .append("<SN>" + sn + "</SN>\r\n")
                      .append("<DeviceID>" + deviceChannel.getGbDeviceId() + "</DeviceID>\r\n")
                      .append("<Result>" + (result?"OK":"ERROR") + "</Result>\r\n")
                      .append("</Response>\r\n");

        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(platform.getDeviceIp(), platform.getTransport());

        SIPRequest messageRequest = (SIPRequest)headerProviderPlatformProvider.createMessageRequest(platform, mediaStatusXml.toString(),
               SipUtils.getNewFromTag(), SipUtils.getNewViaTag(), callIdHeader);

        sipSender.transmitRequest(platform.getDeviceIp(),messageRequest, errorEvent, okEvent);
    }

    @Override
    public void broadcastInviteCmd(Platform platform, CommonGBChannel channel,String sourceId, MediaServer mediaServerItem,
                                   SSRCInfo ssrcInfo, HookSubscribe.Event event, SipSubscribe.Event okEvent,
                                   SipSubscribe.Event errorEvent) throws ParseException, SipException, InvalidArgumentException {
        String stream = ssrcInfo.getStream();

        if (platform == null) {
            return;
        }

        log.info("{} 分配的ZLM为: {} [{}:{}]", stream, mediaServerItem.getId(), mediaServerItem.getIp(), ssrcInfo.getPort());
        Hook hook = Hook.getInstance(HookType.on_media_arrival, "rtp", stream, mediaServerItem.getId());
        subscribe.addSubscribe(hook, (hookData) -> {
            if (event != null) {
                event.response(hookData);
                subscribe.removeSubscribe(hook);
            }
        });
        String sdpIp = mediaServerItem.getSdpIp();

        StringBuffer content = new StringBuffer(200);
        content.append("v=0\r\n");
        content.append("o=" + platform.getDeviceGBId() + " 0 0 IN IP4 " + sdpIp + "\r\n");
        content.append("s=Play\r\n");
        content.append("u=" + channel.getGbDeviceId() + ":0\r\n");
        content.append("c=IN IP4 " + sdpIp + "\r\n");
        content.append("t=0 0\r\n");

        if ("TCP-PASSIVE".equalsIgnoreCase(userSetting.getBroadcastForPlatform())) {
            content.append("m=audio " + ssrcInfo.getPort() + " TCP/RTP/AVP 8 96\r\n");
        } else if ("TCP-ACTIVE".equalsIgnoreCase(userSetting.getBroadcastForPlatform())) {
            content.append("m=audio " + ssrcInfo.getPort() + " TCP/RTP/AVP 8 96\r\n");
        } else if ("UDP".equalsIgnoreCase(userSetting.getBroadcastForPlatform())) {
            content.append("m=audio " + ssrcInfo.getPort() + " RTP/AVP 8 96\r\n");
        }

        content.append("a=recvonly\r\n");
        content.append("a=rtpmap:8 PCMA/8000\r\n");
        content.append("a=rtpmap:96 PS/90000\r\n");
        if ("TCP-PASSIVE".equalsIgnoreCase(userSetting.getBroadcastForPlatform())) {
            content.append("a=setup:passive\r\n");
            content.append("a=connection:new\r\n");
        }else if ("TCP-ACTIVE".equalsIgnoreCase(userSetting.getBroadcastForPlatform())) {
            content.append("a=setup:active\r\n");
            content.append("a=connection:new\r\n");
        }

        content.append("y=" + ssrcInfo.getSsrc() + "\r\n");//ssrc
        // f字段:f= v/编码格式/分辨率/帧率/码率类型/码率大小a/编码格式/码率大小/采样率
        content.append("f=v/2/5/25/1/4096a/1/8/1\r\n");
        CallIdHeader callIdHeader = sipSender.getNewCallIdHeader(sipLayer.getLocalIp(platform.getDeviceIp()), platform.getTransport());

        Request request = headerProviderPlatformProvider.createInviteRequest(platform, sourceId, channel.getGbDeviceId(),
                content.toString(), SipUtils.getNewViaTag(), SipUtils.getNewFromTag(),  ssrcInfo.getSsrc(),
                callIdHeader);
        sipSender.transmitRequest(sipLayer.getLocalIp(platform.getDeviceIp()), request, (e -> {
            sessionManager.removeByStream(ssrcInfo.getApp(), ssrcInfo.getStream());
            mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
            subscribe.removeSubscribe(hook);
            errorEvent.response(e);
        }), e -> {
            ResponseEvent responseEvent = (ResponseEvent) e.event;
            SIPResponse response = (SIPResponse) responseEvent.getResponse();
            SsrcTransaction ssrcTransaction = SsrcTransaction.buildForPlatform(platform.getServerGBId(), channel.getGbId(),
                    callIdHeader.getCallId(), ssrcInfo.getApp(),  stream, ssrcInfo.getSsrc(), mediaServerItem.getId(), response, InviteSessionType.BROADCAST);
            sessionManager.put(ssrcTransaction);
            okEvent.response(e);
        });
    }
}
