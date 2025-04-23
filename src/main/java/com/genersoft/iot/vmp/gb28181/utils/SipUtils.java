package com.genersoft.iot.vmp.gb28181.utils;

import com.genersoft.iot.vmp.gb28181.bean.Gb28181Sdp;
import com.genersoft.iot.vmp.common.RemoteAddressInfo;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.utils.GitUtil;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.Subject;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.util.ObjectUtils;

import javax.sdp.SdpFactory;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.header.FromHeader;
import javax.sip.header.SubjectHeader;
import javax.sip.header.UserAgentHeader;
import javax.sip.message.Request;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author panlinlin
 * @version 1.0.0
 * @description JAIN SIP的工具类
 * @createTime 2021年09月27日 15:12:00
 */
@Slf4j
public class SipUtils {

    public static String getUserIdFromFromHeader(Request request) {
        FromHeader fromHeader = (FromHeader)request.getHeader(FromHeader.NAME);
        return getUserIdFromFromHeader(fromHeader);
    }
    /**
     * 从subject读取channelId
     * */
    public static String[] getChannelIdFromRequest(Request request) {
        SubjectHeader subject = (Subject)request.getHeader("subject");
        if (subject == null) {
            // 如果缺失subject
            return null;
        }
        String[] result = new String[2];
        String subjectStr = subject.getSubject();
        if (subjectStr.indexOf(",") > 0) {
            String[] subjectSplit = subjectStr.split(",");
            result[0] = subjectSplit[0].split(":")[0];
            result[1] = subjectSplit[1].split(":")[0];
        }else {
            result[0] = subjectStr.split(":")[0];
        }
        return result;
    }

    public static String getUserIdFromFromHeader(FromHeader fromHeader) {
        AddressImpl address = (AddressImpl)fromHeader.getAddress();
        SipUri uri = (SipUri) address.getURI();
        return uri.getUser();
    }

    public static  String getNewViaTag() {
        return "z9hG4bK" + RandomStringUtils.randomNumeric(10);
    }

    public static UserAgentHeader createUserAgentHeader(GitUtil gitUtil) throws PeerUnavailableException, ParseException {
        List<String> agentParam = new ArrayList<>();
        agentParam.add("WVP-Pro ");
        if (gitUtil != null ) {
            if (!ObjectUtils.isEmpty(gitUtil.getBuildVersion())) {
                agentParam.add("v");
                agentParam.add(gitUtil.getBuildVersion() + ".");
            }
            if (!ObjectUtils.isEmpty(gitUtil.getCommitTime())) {
                agentParam.add(gitUtil.getCommitTime());
            }
        }
        return SipFactory.getInstance().createHeaderFactory().createUserAgentHeader(agentParam);
    }

    public static String getNewFromTag(){
        return UUID.randomUUID().toString().replace("-", "");

//        return getNewTag();
    }

    public static String getNewTag(){
        return String.valueOf(System.currentTimeMillis());
    }


    /**
     * 云台指令码计算
     *
     * @param leftRight  镜头左移右移 0:停止 1:左移 2:右移
     * @param upDown     镜头上移下移 0:停止 1:上移 2:下移
     * @param inOut      镜头放大缩小 0:停止 1:缩小 2:放大
     * @param moveSpeed  镜头移动速度 默认 0XFF (0-255)
     * @param zoomSpeed  镜头缩放速度 默认 0X1 (0-255)
     */
    public static String cmdString(int leftRight, int upDown, int inOut, int moveSpeed, int zoomSpeed) {
        int cmdCode = 0;
        if (leftRight == 2) {
            cmdCode|=0x01;		// 右移
        } else if(leftRight == 1) {
            cmdCode|=0x02;		// 左移
        }
        if (upDown == 2) {
            cmdCode|=0x04;		// 下移
        } else if(upDown == 1) {
            cmdCode|=0x08;		// 上移
        }
        if (inOut == 2) {
            cmdCode |= 0x10;	// 放大
        } else if(inOut == 1) {
            cmdCode |= 0x20;	// 缩小
        }
        StringBuilder builder = new StringBuilder("A50F01");
        String strTmp;
        strTmp = String.format("%02X", cmdCode);
        builder.append(strTmp, 0, 2);
        strTmp = String.format("%02X", moveSpeed);
        builder.append(strTmp, 0, 2);
        builder.append(strTmp, 0, 2);

        //优化zoom低倍速下的变倍速率
        if ((zoomSpeed > 0) && (zoomSpeed <16))
        {
            zoomSpeed = 16;
        }
        strTmp = String.format("%X", zoomSpeed);
        builder.append(strTmp, 0, 1).append("0");
        //计算校验码
        int checkCode = (0XA5 + 0X0F + 0X01 + cmdCode + moveSpeed + moveSpeed + (zoomSpeed /*<< 4*/ & 0XF0)) % 0X100;
        strTmp = String.format("%02X", checkCode);
        builder.append(strTmp, 0, 2);
        return builder.toString();
    }

    public static String getNewCallId() {
        return (int) Math.floor(Math.random() * 1000000000) + "";
    }

    public static int getTypeCodeFromGbCode(String deviceId) {
        if (ObjectUtils.isEmpty(deviceId)) {
            return 0;
        }
        return Integer.parseInt(deviceId.substring(10, 13));
    }

    /**
     * 判断是否是前端外围设备
     * @param deviceId
     * @return
     */
    public static boolean isFrontEnd(String deviceId) {
        int typeCodeFromGbCode = getTypeCodeFromGbCode(deviceId);
        return typeCodeFromGbCode > 130 && typeCodeFromGbCode < 199;
    }
    /**
     * 从请求中获取设备ip地址和端口号
     * @param request 请求
     * @param sipUseSourceIpAsRemoteAddress  false 从via中获取地址， true 直接获取远程地址
     * @return 地址信息
     */
    public static RemoteAddressInfo getRemoteAddressFromRequest(SIPRequest request, boolean sipUseSourceIpAsRemoteAddress) {

        String remoteAddress;
        int remotePort;
        if (sipUseSourceIpAsRemoteAddress) {
            remoteAddress = request.getPeerPacketSourceAddress().getHostAddress();
            remotePort = request.getPeerPacketSourcePort();

        }else {
            // 判断RPort是否改变，改变则说明路由nat信息变化，修改设备信息
            // 获取到通信地址等信息
            remoteAddress = request.getTopmostViaHeader().getReceived();
            remotePort = request.getTopmostViaHeader().getRPort();
            // 解析本地地址替代
            if (ObjectUtils.isEmpty(remoteAddress) || remotePort == -1) {
                remoteAddress = request.getPeerPacketSourceAddress().getHostAddress();
                remotePort = request.getPeerPacketSourcePort();
            }
        }

        return new RemoteAddressInfo(remoteAddress, remotePort);
    }

    public static Gb28181Sdp parseSDP(String sdpStr) throws SdpParseException {

        // jainSip不支持y= f=字段， 移除以解析。
        int ssrcIndex = sdpStr.indexOf("y=");
        int mediaDescriptionIndex = sdpStr.indexOf("f=");
        // 检查是否有y字段
        SessionDescription sdp;
        String ssrc = null;
        String mediaDescription = null;
        if (mediaDescriptionIndex == 0 && ssrcIndex == 0) {
            sdp = SdpFactory.getInstance().createSessionDescription(sdpStr);
        }else {
            String lines[] = sdpStr.split("\\r?\\n");
            StringBuilder sdpBuffer = new StringBuilder();
            for (String line : lines) {
                if (line.trim().startsWith("y=")) {
                    ssrc = line.substring(2);
                }else if (line.trim().startsWith("f=")) {
                    mediaDescription = line.substring(2);
                }else {
                    sdpBuffer.append(line.trim()).append("\r\n");
                }
            }
            sdp = SdpFactory.getInstance().createSessionDescription(sdpBuffer.toString());
        }
        return Gb28181Sdp.getInstance(sdp, ssrc, mediaDescription);
    }

    public static String getSsrcFromSdp(String sdpStr) {

        // jainSip不支持y= f=字段， 移除以解析。
        int ssrcIndex = sdpStr.indexOf("y=");
        if (ssrcIndex == 0) {
            return null;
        }
        String lines[] = sdpStr.split("\\r?\\n");
        for (String line : lines) {
            if (line.trim().startsWith("y=")) {
                return line.substring(2);
            }
        }
        return null;
    }

    public static String parseTime(String timeStr) {
        if (ObjectUtils.isEmpty(timeStr)){
            return null;
        }
        LocalDateTime localDateTime;
        try {
            localDateTime = LocalDateTime.parse(timeStr);
        }catch (DateTimeParseException e) {
            try {
                localDateTime = LocalDateTime.parse(timeStr, DateUtil.formatterISO8601);
            }catch (DateTimeParseException e2) {
                log.error("[格式化时间] 无法格式化时间： {}", timeStr);
                return null;
            }
        }
        return localDateTime.format(DateUtil.formatter);
    }
}