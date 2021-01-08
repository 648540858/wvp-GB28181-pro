package com.genersoft.iot.vmp.gb28181.transmit.cmd;

import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;

import javax.sip.header.WWWAuthenticateHeader;

public interface ISIPCommanderForPlatform {

    /**
     * 向上级平台注册
     * @param parentPlatform
     * @return
     */
    boolean register(ParentPlatform parentPlatform);
    boolean register(ParentPlatform parentPlatform, String callId, WWWAuthenticateHeader www, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent);

    /**
     * 向上级平台注销
     * @param parentPlatform
     * @return
     */
    boolean unregister(ParentPlatform parentPlatform, SipSubscribe.Event errorEvent , SipSubscribe.Event okEvent);


    /**
     * 向上级平发送心跳信息
     * @param parentPlatform
     * @return callId(作为接受回复的判定)
     */
    String keepalive(ParentPlatform parentPlatform);
}
