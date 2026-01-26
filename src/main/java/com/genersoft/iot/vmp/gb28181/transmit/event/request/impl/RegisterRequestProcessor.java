package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.common.RemoteAddressInfo;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.auth.DigestServerAuthenticationHelper;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.GbSipDate;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.SIPSender;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.IpPortUtil;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.SIPDateHeader;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * SIP命令类型： REGISTER请求
 */
@Slf4j
@Component
public class RegisterRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

    public final String method = "REGISTER";

    @Autowired
    private SipConfig sipConfig;

    @Autowired
    private SIPProcessorObserver sipProcessorObserver;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private SIPSender sipSender;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;


    @Override
    public void afterPropertiesSet() throws Exception {
        // 添加消息处理的订阅
        sipProcessorObserver.addRequestProcessor(method, this);
    }

    /**
     * 收到注册请求 处理
     */
    @Override
    public void process(RequestEvent evt) {
        try {
            SIPRequest request = (SIPRequest) evt.getRequest();
            Response response = null;
            boolean passwordCorrect = false;
            // 注册标志
            boolean registerFlag = true;
            if (request.getExpires().getExpires() == 0) {
                // 注销成功
                registerFlag = false;
            }
            FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
            AddressImpl address = (AddressImpl) fromHeader.getAddress();
            SipUri uri = (SipUri) address.getURI();
            String deviceId = uri.getUser();
            // 调整逻辑，如果为设置公共密码，那么就必须要预设用户信息，否则无法注册。
            Device device = deviceService.getDeviceByDeviceId(deviceId);

            RemoteAddressInfo remoteAddressInfo = SipUtils.getRemoteAddressFromRequest(request,
                    userSetting.getSipUseSourceIpAsRemoteAddress());
            String requestAddress = remoteAddressInfo.getIp() + ":" + remoteAddressInfo.getPort();
            String title = registerFlag ? "[注册请求]" : "[注销请求]";
            log.info("{} 设备：{}, 开始处理: {}", title, deviceId, requestAddress);
            String password = null;
            if (device != null) {
                if (device.getSipTransactionInfo() != null &&
                        request.getCallIdHeader().getCallId().equals(device.getSipTransactionInfo().getCallId())) {
                    log.info("{} 设备：{}, 注册续订: {}", title, device.getDeviceId(), device.getDeviceId());
                    if (registerFlag) {
                        device.setExpires(request.getExpires().getExpires());
                        device.setIp(remoteAddressInfo.getIp());
                        device.setPort(remoteAddressInfo.getPort());
                        device.setHostAddress(IpPortUtil.concatenateIpAndPort(remoteAddressInfo.getIp(), String.valueOf(remoteAddressInfo.getPort())));

                        device.setLocalIp(request.getLocalAddress().getHostAddress());
                        Response registerOkResponse = getRegisterOkResponse(request);
                        // 判断TCP还是UDP
                        ViaHeader reqViaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
                        String transport = reqViaHeader.getTransport();
                        device.setTransport("TCP".equalsIgnoreCase(transport) ? "TCP" : "UDP");
                        sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), registerOkResponse);
                        device.setRegisterTimeStamp(System.currentTimeMillis());
                        deviceService.online(device);
                    } else {
                        deviceService.offline(device);
                    }
                    return;
                }else {
                    // 正常注册, 用户信息未设置密码，并且公共密码也未设置，则关闭鉴权
                    if (!ObjectUtils.isEmpty(device.getPassword()) || !ObjectUtils.isEmpty(sipConfig.getPassword())) {
                        password = (!ObjectUtils.isEmpty(device.getPassword())) ? device.getPassword() : sipConfig.getPassword();
                    }
                    // 如果设置了一个无密码的设备，那么这里就会自动跳动，后续会直接注册成功
                }
            }else {
                if (ObjectUtils.isEmpty(sipConfig.getPassword())) {
                    log.info("{} 设备：{}, 地址: {}, 公共密码已经禁用，请添加用户信息后注册", title, deviceId, requestAddress);
                    response = getMessageFactory().createResponse(Response.FORBIDDEN, request);
                    sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
                    return;
                }else {
                    password = sipConfig.getPassword();
                }
            }

            AuthorizationHeader authHead = (AuthorizationHeader) request.getHeader(AuthorizationHeader.NAME);
            if (authHead == null && !ObjectUtils.isEmpty(password)) {
                log.info(title + " 设备：{}, 回复401: {}", deviceId, requestAddress);
                response = getMessageFactory().createResponse(Response.UNAUTHORIZED, request);
                new DigestServerAuthenticationHelper().generateChallenge(getHeaderFactory(), response, sipConfig.getDomain());
                sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
                return;
            }

            // 校验密码是否正确
            passwordCorrect = ObjectUtils.isEmpty(password) ||
                    new DigestServerAuthenticationHelper().doAuthenticatePlainTextPassword(request, password);

            if (!passwordCorrect) {
                // 注册失败
                response = getMessageFactory().createResponse(Response.FORBIDDEN, request);
                response.setReasonPhrase("wrong password");
                log.info("{} 设备：{}, 密码/SIP服务器ID错误, 回复403: {}", title, deviceId, requestAddress);
                sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
                return;
            }

            // 携带授权头并且密码正确
            response = getMessageFactory().createResponse(Response.OK, request);
            // 如果主动禁用了Date头，则不添加
            if (!userSetting.isDisableDateHeader()) {
                // 添加 date头
                SIPDateHeader dateHeader = new SIPDateHeader();
                // 使用自己修改的
                GbSipDate gbSipDate = new GbSipDate(Calendar.getInstance(Locale.ENGLISH).getTimeInMillis());
                dateHeader.setDate(gbSipDate);
                response.addHeader(dateHeader);
            }

            if (request.getExpires() == null) {
                response = getMessageFactory().createResponse(Response.BAD_REQUEST, request);
                sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
                return;
            }
            // 添加 Contact头
            response.addHeader(request.getHeader(ContactHeader.NAME));
            // 添加 Expires头
            response.addHeader(request.getExpires());

            if (device == null) {
                device = new Device();
                device.setStreamMode("TCP-PASSIVE");
                device.setCharset("GB2312");
                device.setGeoCoordSys("WGS84");
                device.setMediaServerId("auto");
                device.setDeviceId(deviceId);
                device.setOnLine(false);
            } else {
                if (ObjectUtils.isEmpty(device.getStreamMode())) {
                    device.setStreamMode("TCP-PASSIVE");
                }
                if (ObjectUtils.isEmpty(device.getCharset())) {
                    device.setCharset("GB2312");
                }
                if (ObjectUtils.isEmpty(device.getGeoCoordSys())) {
                    device.setGeoCoordSys("WGS84");
                }
            }
            device.setServerId(userSetting.getServerId());
            device.setIp(remoteAddressInfo.getIp());
            device.setPort(remoteAddressInfo.getPort());
            device.setHostAddress(IpPortUtil.concatenateIpAndPort(remoteAddressInfo.getIp(), String.valueOf(remoteAddressInfo.getPort())));
            device.setLocalIp(request.getLocalAddress().getHostAddress());
            if (request.getExpires().getExpires() == 0) {
                // 注销成功
                registerFlag = false;
            } else {
                // 注册成功
                device.setExpires(request.getExpires().getExpires());
                registerFlag = true;
                // 判断 TCP/UDP
                ViaHeader reqViaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
                String transport = reqViaHeader.getTransport();
                device.setTransport("TCP".equalsIgnoreCase(transport) ? "TCP" : "UDP");
            }

            sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
            // 注册成功
            device.setRegisterTimeStamp(System.currentTimeMillis());
            // 保存到 redis
            if (registerFlag) {
                log.info("[注册成功] deviceId: {}->{}", deviceId, requestAddress);
                SipTransactionInfo sipTransactionInfo = new SipTransactionInfo((SIPResponse) response);
                device.setSipTransactionInfo(sipTransactionInfo);
                deviceService.online(device);
            } else {
                log.info("[注销成功] deviceId: {}->{}", deviceId, requestAddress);
                deviceService.offline(device);
            }
            redisCatchStorage.updateDeviceRegisterTimeStamp(List.of(device));
        } catch (SipException | NoSuchAlgorithmException | ParseException e) {
            log.error("未处理的异常 ", e);
        }
    }

    private Response getRegisterOkResponse(Request request) throws ParseException {
        // 携带授权头并且密码正确
        Response response = getMessageFactory().createResponse(Response.OK, request);
        // 如果主动禁用了Date头，则不添加
        if (!userSetting.isDisableDateHeader()) {
            // 添加date头
            SIPDateHeader dateHeader = new SIPDateHeader();
            // 使用自己修改的
            GbSipDate gbSipDate = new GbSipDate(Calendar.getInstance(Locale.ENGLISH).getTimeInMillis());
            dateHeader.setDate(gbSipDate);
            response.addHeader(dateHeader);
        }

        // 添加Contact头
        response.addHeader(request.getHeader(ContactHeader.NAME));
        // 添加Expires头
        response.addHeader(request.getExpires());

        return response;

    }

}
