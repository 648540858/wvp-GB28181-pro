package com.genersoft.iot.vmp.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration("userSetup")
public class UserSetup {
    @Value("${userSettings.savePositionHistory}")
    boolean savePositionHistory;

    @Value("${userSettings.autoApplyPlay}")
    private boolean autoApplyPlay;

    @Value("${userSettings.seniorSdp}")
    private boolean seniorSdp;

    @Value("${userSettings.playTimeout}")
    private long playTimeout;

    @Value("${userSettings.waitTrack}")
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
