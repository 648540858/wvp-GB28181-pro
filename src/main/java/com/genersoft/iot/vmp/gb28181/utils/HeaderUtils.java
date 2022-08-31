package com.genersoft.iot.vmp.gb28181.utils;

import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.header.UserAgentHeader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 生成header的工具类
 * @author lin
 */
public class HeaderUtils {

    public static UserAgentHeader createUserAgentHeader(SipFactory sipFactory) throws PeerUnavailableException, ParseException {
        List<String> agentParam = new ArrayList<>();
        agentParam.add("WVP PRO");
        // TODO 添加版本信息以及日期
        return sipFactory.createHeaderFactory().createUserAgentHeader(agentParam);
    }
}
