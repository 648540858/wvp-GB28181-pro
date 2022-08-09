package com.genersoft.iot.vmp.gb28181.utils;

import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.Subject;

import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.message.Request;

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

}
