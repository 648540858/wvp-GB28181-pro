package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.auth.DigestServerAuthenticationHelper;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.RemoteAddressInfo;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import com.genersoft.iot.vmp.gb28181.bean.GbSipDate;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.SIPSender;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.utils.SipUtils;
import com.genersoft.iot.vmp.service.IDeviceService;
import com.genersoft.iot.vmp.utils.DateUtil;
import gov.nist.javax.sip.RequestEventExt;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.SIPDateHeader;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Locale;

/**
 * SIP命令类型： REGISTER请求
 */
@Component
public class RegisterRequestProcessor extends SIPRequestProcessorParent implements InitializingBean, ISIPRequestProcessor {

    private final Logger logger = LoggerFactory.getLogger(RegisterRequestProcessor.class);

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

    @Override
    public void afterPropertiesSet() throws Exception {
        // 添加消息处理的订阅
        sipProcessorObserver.addRequestProcessor(method, this);
    }

    /**
     * 收到注册请求 处理
     *
     * @param evt
     */
    @Override
    public void process(RequestEvent evt) {
        try {
            RequestEventExt evtExt = (RequestEventExt) evt;

            SIPRequest request = (SIPRequest)evt.getRequest();
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

            Device device = deviceService.getDevice(deviceId);

            RemoteAddressInfo remoteAddressInfo = SipUtils.getRemoteAddressFromRequest(request,
                    userSetting.getSipUseSourceIpAsRemoteAddress());
            String requestAddress = remoteAddressInfo.getIp() + ":" + remoteAddressInfo.getPort();
            String title = registerFlag ? "[注册请求]": "[注销请求]";
                    logger.info(title + "设备：{}, 开始处理: {}", deviceId, requestAddress);
            if (device != null &&
                device.getSipTransactionInfo() != null &&
                request.getCallIdHeader().getCallId().equals(device.getSipTransactionInfo().getCallId())) {
                logger.info(title + "设备：{}, 注册续订: {}",device.getDeviceId(), device.getDeviceId());
                device.setExpires(request.getExpires().getExpires());
                device.setIp(remoteAddressInfo.getIp());
                device.setPort(remoteAddressInfo.getPort());
                device.setHostAddress(remoteAddressInfo.getIp().concat(":").concat(String.valueOf(remoteAddressInfo.getPort())));
                device.setLocalIp(request.getLocalAddress().getHostAddress());
                Response registerOkResponse = getRegisterOkResponse(request);
                // 判断TCP还是UDP
                ViaHeader reqViaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
                String transport = reqViaHeader.getTransport();
                device.setTransport("TCP".equalsIgnoreCase(transport) ? "TCP" : "UDP");
                sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), registerOkResponse);
                device.setRegisterTime(DateUtil.getNow());
                SipTransactionInfo sipTransactionInfo = new SipTransactionInfo((SIPResponse)registerOkResponse);
                deviceService.online(device, sipTransactionInfo);
                return;
            }
            String password = (device != null && !ObjectUtils.isEmpty(device.getPassword()))? device.getPassword() : sipConfig.getPassword();
            AuthorizationHeader authHead = (AuthorizationHeader) request.getHeader(AuthorizationHeader.NAME);
            if (authHead == null && !ObjectUtils.isEmpty(password)) {
                logger.info(title + " 设备：{}, 回复401: {}",deviceId, requestAddress);
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
                logger.info(title + " 设备：{}, 密码/SIP服务器ID错误, 回复403: {}", deviceId, requestAddress);
                sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
                return;
            }

            // 携带授权头并且密码正确
            response = getMessageFactory().createResponse(Response.OK, request);
            // 添加date头
            SIPDateHeader dateHeader = new SIPDateHeader();
            // 使用自己修改的
            GbSipDate gbSipDate = new GbSipDate(Calendar.getInstance(Locale.ENGLISH).getTimeInMillis());
            dateHeader.setDate(gbSipDate);
            response.addHeader(dateHeader);

            if (request.getExpires() == null) {
                response = getMessageFactory().createResponse(Response.BAD_REQUEST, request);
                sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
                return;
            }
            // 添加Contact头
            response.addHeader(request.getHeader(ContactHeader.NAME));
            // 添加Expires头
            response.addHeader(request.getExpires());

            if (device == null) {
                device = new Device();
                device.setStreamMode("UDP");
                device.setCharset("GB2312");
                device.setGeoCoordSys("WGS84");
                device.setDeviceId(deviceId);
                device.setOnLine(false);
            }else {
                if (ObjectUtils.isEmpty(device.getStreamMode())) {
                    device.setStreamMode("UDP");
                }
                if (ObjectUtils.isEmpty(device.getCharset())) {
                    device.setCharset("GB2312");
                }
                if (ObjectUtils.isEmpty(device.getGeoCoordSys())) {
                    device.setGeoCoordSys("WGS84");
                }
            }

            device.setIp(remoteAddressInfo.getIp());
            device.setPort(remoteAddressInfo.getPort());
            device.setHostAddress(remoteAddressInfo.getIp().concat(":").concat(String.valueOf(remoteAddressInfo.getPort())));
            device.setLocalIp(request.getLocalAddress().getHostAddress());
            if (request.getExpires().getExpires() == 0) {
                // 注销成功
                registerFlag = false;
            } else {
                // 注册成功
                device.setExpires(request.getExpires().getExpires());
                registerFlag = true;
                // 判断TCP还是UDP
                ViaHeader reqViaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
                String transport = reqViaHeader.getTransport();
                device.setTransport("TCP".equalsIgnoreCase(transport) ? "TCP" : "UDP");
            }

            sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
            // 注册成功
            // 保存到redis
            if (registerFlag) {
                logger.info("[注册成功] deviceId: {}->{}",  deviceId, requestAddress);
                device.setRegisterTime(DateUtil.getNow());
                SipTransactionInfo sipTransactionInfo = new SipTransactionInfo((SIPResponse)response);
                deviceService.online(device, sipTransactionInfo);
            } else {
                logger.info("[注销成功] deviceId: {}->{}" ,deviceId, requestAddress);
                deviceService.offline(deviceId, "主动注销");
            }
        } catch (SipException | NoSuchAlgorithmException | ParseException e) {
            logger.error("未处理的异常 ", e);
        }
    }

    private Response getRegisterOkResponse(Request request) throws ParseException {
        // 携带授权头并且密码正确
        Response  response = getMessageFactory().createResponse(Response.OK, request);
        // 添加date头
        SIPDateHeader dateHeader = new SIPDateHeader();
        // 使用自己修改的
        GbSipDate gbSipDate = new GbSipDate(Calendar.getInstance(Locale.ENGLISH).getTimeInMillis());
        dateHeader.setDate(gbSipDate);
        response.addHeader(dateHeader);

        // 添加Contact头
        response.addHeader(request.getHeader(ContactHeader.NAME));
        // 添加Expires头
        response.addHeader(request.getExpires());

        return response;

    }
}
