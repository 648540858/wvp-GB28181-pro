package com.genersoft.iot.vmp.gb28181.utils;

import com.genersoft.iot.vmp.utils.GitUtil;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.Subject;

import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.UserAgentHeader;
import javax.sip.message.Request;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author panlinlin
 * @version 1.0.0
 * @description JAIN SIP的工具类
 * @createTime 2021年09月27日 15:12:00
 */
public class SipUtils {

    public static String getUserIdFromFromHeader(Request request) {
        FromHeader fromHeader = (FromHeader)request.getHeader(FromHeader.NAME);
        return getUserIdFromFromHeader(fromHeader);
    }
    /**
     * 从subject读取channelId
     * */
    public static String getChannelIdFromRequest(Request request) {
        Header subject = request.getHeader("subject");
        if (subject == null) {
            // 如果缺失subject
            return null;
        }
        return ((Subject) subject).getSubject().split(":")[0];
    }

    public static String getUserIdFromFromHeader(FromHeader fromHeader) {
        AddressImpl address = (AddressImpl)fromHeader.getAddress();
        SipUri uri = (SipUri) address.getURI();
        return uri.getUser();
    }

    public static  String getNewViaTag() {
        return "z9hG4bK" + System.currentTimeMillis();
    }

    public static UserAgentHeader createUserAgentHeader(SipFactory sipFactory, GitUtil gitUtil) throws PeerUnavailableException, ParseException {
        List<String> agentParam = new ArrayList<>();
        agentParam.add("WVP-Pro v");
        if (gitUtil != null && gitUtil.getCommitTime() != null) {
            agentParam.add(gitUtil.getBuildVersion() + ".");
            agentParam.add(gitUtil.getCommitTime());
        }
        return sipFactory.createHeaderFactory().createUserAgentHeader(agentParam);
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
        strTmp = String.format("%X", zoomSpeed);
        builder.append(strTmp, 0, 1).append("0");
        //计算校验码
        int checkCode = (0XA5 + 0X0F + 0X01 + cmdCode + moveSpeed + moveSpeed + (zoomSpeed /*<< 4*/ & 0XF0)) % 0X100;
        strTmp = String.format("%02X", checkCode);
        builder.append(strTmp, 0, 2);
        return builder.toString();
    }

}
