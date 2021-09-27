package com.genersoft.iot.vmp.utils;

import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;

import javax.sip.header.FromHeader;
import javax.sip.message.Request;

/**
 * @author panlinlin
 * @version 1.0.0
 * @Description JAIN SIP的工具类
 * @createTime 2021年09月27日 15:12:00
 */
public class SipUtils {

    public static String getUserIdFromFromHeader(Request request) {
        FromHeader fromHeader = (FromHeader)request.getHeader(FromHeader.NAME);
        AddressImpl address = (AddressImpl)fromHeader.getAddress();
        SipUri uri = (SipUri) address.getURI();
        return uri.getUser();
    }
}
