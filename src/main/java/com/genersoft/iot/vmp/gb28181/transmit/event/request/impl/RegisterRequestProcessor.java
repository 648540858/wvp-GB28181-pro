package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.common.RemoteAddressInfo;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.auth.DigestServerAuthenticationHelper;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.GbCode;
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
import javax.sip.header.ExpiresHeader;
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

    @Override
    public void process(RequestEvent evt) {
        try {
            SIPRequest request = (SIPRequest) evt.getRequest();

            FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
            AddressImpl address = (AddressImpl) fromHeader.getAddress();
            SipUri uri = (SipUri) address.getURI();
            String deviceId = uri.getUser();

            if (userSetting.isDeviceIdStrict()) {
                GbCode decode = GbCode.decode(deviceId);
                if (decode == null) {
                    Response response = getMessageFactory().createResponse(Response.FORBIDDEN, request);
                    sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
                    return;
                }
            }

            ExpiresHeader expiresHeader = request.getExpires();
            if (expiresHeader == null) {
                Response response = getMessageFactory().createResponse(Response.BAD_REQUEST, request);
                sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
                return;
            }

            boolean registerFlag = expiresHeader.getExpires() != 0;

            Device device = deviceService.getDeviceByDeviceId(deviceId);

            RemoteAddressInfo remoteAddressInfo = SipUtils.getRemoteAddressFromRequest(request,
                    userSetting.getSipUseSourceIpAsRemoteAddress());
            String requestAddress = remoteAddressInfo.getIp() + ":" + remoteAddressInfo.getPort();

            if (registerFlag) {
                registerHandler(device, request, remoteAddressInfo, deviceId, requestAddress);
            } else {
                cancellationHandler(device, request, remoteAddressInfo, deviceId, requestAddress);
            }
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

    private void registerHandler(Device device, SIPRequest request, RemoteAddressInfo remoteAddressInfo,
                                  String deviceId, String requestAddress) throws SipException, NoSuchAlgorithmException, ParseException {
        if (device != null && device.getSipTransactionInfo() != null &&
                request.getCallIdHeader().getCallId().equals(device.getSipTransactionInfo().getCallId())) {
            log.info("[注册续订] 设备：{}", device.getDeviceId());
            device.setExpires(request.getExpires().getExpires());
            device.setIp(remoteAddressInfo.getIp());
            device.setPort(remoteAddressInfo.getPort());
            device.setHostAddress(IpPortUtil.concatenateIpAndPort(remoteAddressInfo.getIp(), String.valueOf(remoteAddressInfo.getPort())));
            device.setLocalIp(request.getLocalAddress().getHostAddress());

            ViaHeader reqViaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
            String transport = reqViaHeader.getTransport();
            device.setTransport("TCP".equalsIgnoreCase(transport) ? "TCP" : "UDP");

            Response okResponse = getRegisterOkResponse(request);
            sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), okResponse);
            device.setRegisterTimeStamp(System.currentTimeMillis());
            deviceService.online(device);
            return;
        }

        if (device == null && ObjectUtils.isEmpty(sipConfig.getPassword())) {
            log.info("[注册请求] 设备：{}, 地址: {}, 公共密码已经禁用，请添加用户信息后注册", deviceId, requestAddress);
            Response response = getMessageFactory().createResponse(Response.FORBIDDEN, request);
            sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
            return;
        }
        String password = device != null && !ObjectUtils.isEmpty(device.getPassword()) ? device.getPassword() : sipConfig.getPassword();

        AuthorizationHeader authHead = (AuthorizationHeader) request.getHeader(AuthorizationHeader.NAME);
        if (!ObjectUtils.isEmpty(password) && authHead == null) {
            log.info("[注册请求] 设备：{}, 回复401: {}", deviceId, requestAddress);
            Response response = getMessageFactory().createResponse(Response.UNAUTHORIZED, request);
            new DigestServerAuthenticationHelper().generateChallenge(getHeaderFactory(), response, sipConfig.getDomain());
            sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
            return;
        }

        if (!ObjectUtils.isEmpty(password) && !new DigestServerAuthenticationHelper().doAuthenticatePlainTextPassword(request, password)) {
            log.info("[注册请求] 设备：{}, 密码/SIP服务器ID错误, 回复403: {}", deviceId, requestAddress);
            Response response = getMessageFactory().createResponse(Response.FORBIDDEN, request);
            response.setReasonPhrase("wrong password");
            sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
            return;
        }

        Response response = getMessageFactory().createResponse(Response.OK, request);
        if (!userSetting.isDisableDateHeader()) {
            SIPDateHeader dateHeader = new SIPDateHeader();
            GbSipDate gbSipDate = new GbSipDate(Calendar.getInstance(Locale.ENGLISH).getTimeInMillis());
            dateHeader.setDate(gbSipDate);
            response.addHeader(dateHeader);
        }
        response.addHeader(request.getHeader(ContactHeader.NAME));
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
        device.setExpires(request.getExpires().getExpires());

        ViaHeader reqViaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
        String transport = reqViaHeader.getTransport();
        device.setTransport("TCP".equalsIgnoreCase(transport) ? "TCP" : "UDP");

        sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);

        device.setRegisterTimeStamp(System.currentTimeMillis());
        SipTransactionInfo sipTransactionInfo = new SipTransactionInfo((SIPResponse) response);
        device.setSipTransactionInfo(sipTransactionInfo);
        deviceService.online(device);
        redisCatchStorage.updateDeviceRegisterTimeStamp(List.of(device));

        log.info("[注册成功] deviceId: {}->{}", deviceId, requestAddress);
    }

    private void cancellationHandler(Device device, SIPRequest request, RemoteAddressInfo remoteAddressInfo,
                                      String deviceId, String requestAddress) throws SipException, NoSuchAlgorithmException, ParseException {
        if (device != null && device.getSipTransactionInfo() != null &&
                request.getCallIdHeader().getCallId().equals(device.getSipTransactionInfo().getCallId())) {
            Response response = getRegisterOkResponse(request);
            sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
            deviceService.offline(device);
            log.info("[注销成功] deviceId: {}->{}", deviceId, requestAddress);
            return;
        }

        if (device == null && ObjectUtils.isEmpty(sipConfig.getPassword())) {
            log.info("[注销请求] 设备：{}, 地址: {}, 公共密码已经禁用，请添加用户信息后注销", deviceId, requestAddress);
            Response response = getMessageFactory().createResponse(Response.FORBIDDEN, request);
            sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
            return;
        }
        String password = device != null && !ObjectUtils.isEmpty(device.getPassword()) ? device.getPassword() : sipConfig.getPassword();

        AuthorizationHeader authHead = (AuthorizationHeader) request.getHeader(AuthorizationHeader.NAME);
        if (!ObjectUtils.isEmpty(password) && authHead == null) {
            log.info("[注销请求] 设备：{}, 回复401: {}", deviceId, requestAddress);
            Response response = getMessageFactory().createResponse(Response.UNAUTHORIZED, request);
            new DigestServerAuthenticationHelper().generateChallenge(getHeaderFactory(), response, sipConfig.getDomain());
            sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
            return;
        }

        if (!ObjectUtils.isEmpty(password) && !new DigestServerAuthenticationHelper().doAuthenticatePlainTextPassword(request, password)) {
            log.info("[注销请求] 设备：{}, 密码/SIP服务器ID错误, 回复403: {}", deviceId, requestAddress);
            Response response = getMessageFactory().createResponse(Response.FORBIDDEN, request);
            response.setReasonPhrase("wrong password");
            sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
            return;
        }

        Response response = getRegisterOkResponse(request);
        sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);

        if (device != null) {
            deviceService.offline(device);
            redisCatchStorage.updateDeviceRegisterTimeStamp(List.of(device));
        }

        log.info("[注销成功] deviceId: {}->{}", deviceId, requestAddress);
    }

}
