package com.genersoft.iot.vmp.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration("userSetup")
public class UserSetup {
    @Value("${userSettings.savePositionHistory:false}")
    boolean savePositionHistory;

    @Value("${userSettings.autoApplyPlay}")
    private boolean autoApplyPlay;

    @Value("${userSettings.seniorSdp:false}")
    private boolean seniorSdp;

    @Value("${userSettings.playTimeout:18000}")
    private long playTimeout;

    @Value("${userSettings.waitTrack:false}")
    private boolean waitTrack;

    @Value("${userSettings.interfaceAuthentication}")
    private boolean interfaceAuthentication;

    @Value("${userSettings.recordPushLive}")
    private boolean recordPushLive;

    public boolean getSavePositionHistory() {
        return savePositionHistory;
    }

    public boolean isSavePositionHistory() {
        return savePositionHistory;
    }

    public boolean isAutoApplyPlay() {
        return autoApplyPlay;
    }

    public boolean isSeniorSdp() {
        return seniorSdp;
    }

    public long getPlayTimeout() {
        return playTimeout;
    }

    public boolean isWaitTrack() {
        return waitTrack;
    }

    public boolean isInterfaceAuthentication() {
        return interfaceAuthentication;
    }

    public boolean isRecordPushLive() {
        return recordPushLive;
    }
}
