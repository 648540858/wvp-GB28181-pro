package com.genersoft.iot.vmp.gb28181.transmit.cmd;

import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;

public interface ISIPCommanderForPlatform {

    /**
     * 向上级平台注册
     * @param parentPlatform
     * @return
     */
    boolean register(ParentPlatform parentPlatform, String callId, String realm, String nonce, String scheme);
}
