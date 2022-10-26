package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.WvpSipDate;
import com.genersoft.iot.vmp.gb28181.transmit.SIPProcessorObserver;
import com.genersoft.iot.vmp.gb28181.transmit.SIPSender;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.ISIPRequestProcessor;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.auth.DigestServerAuthenticationHelper;
import com.genersoft.iot.vmp.service.IDeviceService;
import com.genersoft.iot.vmp.utils.DateUtil;
import gov.nist.javax.sip.RequestEventExt;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.Expires;
import gov.nist.javax.sip.header.SIPDateHeader;
import gov.nist.javax.sip.message.SIPRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.*;
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
            String requestAddress = evtExt.getRemoteIpAddress() + ":" + evtExt.getRemotePort();
            logger.info("[注册请求] 开始处理: {}", requestAddress);
            SIPRequest request = (SIPRequest)evt.getRequest();
            Response response = null;
            boolean passwordCorrect = false;
            // 注册标志
            boolean registerFlag = false;
            FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
            AddressImpl address = (AddressImpl) fromHeader.getAddress();
            SipUri uri = (SipUri) address.getURI();
            String deviceId = uri.getUser();
            Device device = deviceService.getDevice(deviceId);
            String password = (device != null && !ObjectUtils.isEmpty(device.getPassword()))? device.getPassword() : sipConfig.getPassword();
            AuthorizationHeader authHead = (AuthorizationHeader) request.getHeader(AuthorizationHeader.NAME);
            if (authHead == null && !ObjectUtils.isEmpty(password)) {
                logger.info("[注册请求] 未携带授权头 回复401: {}", requestAddress);
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
                logger.info("[注册请求] 密码/SIP服务器ID错误, 回复403: {}", requestAddress);
                sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
                return;
            }

            // 携带授权头并且密码正确
            response = getMessageFactory().createResponse(Response.OK, request);
            // 添加date头
            SIPDateHeader dateHeader = new SIPDateHeader();
            // 使用自己修改的
            WvpSipDate wvpSipDate = new WvpSipDate(Calendar.getInstance(Locale.ENGLISH).getTimeInMillis());
            dateHeader.setDate(wvpSipDate);
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

            // 获取到通信地址等信息
            ViaHeader viaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
            String received = viaHeader.getReceived();
            int rPort = viaHeader.getRPort();
            // 解析本地地址替代
            if (ObjectUtils.isEmpty(received) || rPort == -1) {
                received = viaHeader.getHost();
                rPort = viaHeader.getPort();
            }
            if (device == null) {
                device = new Device();
                device.setStreamMode("UDP");
                device.setCharset("GB2312");
                device.setGeoCoordSys("WGS84");
                device.setTreeType("CivilCode");
                device.setDeviceId(deviceId);
                device.setOnline(0);
            }
            device.setIp(received);
            device.setPort(rPort);
            device.setHostAddress(received.concat(":").concat(String.valueOf(rPort)));
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
                deviceService.online(device);
            } else {
                logger.info("[注销成功] deviceId: {}->{}" ,deviceId, requestAddress);
                deviceService.offline(deviceId);
            }
        } catch (SipException | NoSuchAlgorithmException | ParseException e) {
            e.printStackTrace();
        }
    }
}
