package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson.annotation.JSONField;
import com.genersoft.iot.vmp.conf.MediaConfig;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ZLMServerManger {

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private MediaConfig mediaConfig;

    public void updateServerCatch(ZLMServerConfig zlmServerConfig) {

        zlmServerConfig.setLocalIP(mediaConfig.getIp());
        zlmServerConfig.setWanIp(StringUtils.isEmpty(mediaConfig.getWanIp())? mediaConfig.getIp(): mediaConfig.getWanIp());
        zlmServerConfig.setHttpPort(mediaConfig.getHttpPort());

        if(!StringUtils.isEmpty(mediaConfig.getHttpSSlPort()))
            zlmServerConfig.setHttpSSLport(mediaConfig.getHttpSSlPort());

        if(!StringUtils.isEmpty(mediaConfig.getRtspPort()))
            zlmServerConfig.setRtspPort(mediaConfig.getRtspPort());

        if(!StringUtils.isEmpty(mediaConfig.getRtspSSLPort()))
            zlmServerConfig.setRtspSSlport(mediaConfig.getRtspSSLPort());

        if(!StringUtils.isEmpty(mediaConfig.getRtmpPort()))
            zlmServerConfig.setRtmpPort(mediaConfig.getRtmpPort());

        if(!StringUtils.isEmpty(mediaConfig.getRtmpSSlPort()))
            zlmServerConfig.setRtmpSslPort(mediaConfig.getRtmpSSlPort());

        if(!StringUtils.isEmpty(mediaConfig.getRtpProxyPort()))
            zlmServerConfig.setRtpProxyPort(mediaConfig.getRtpProxyPort());

        redisCatchStorage.updateMediaInfo(zlmServerConfig);
    }
}
