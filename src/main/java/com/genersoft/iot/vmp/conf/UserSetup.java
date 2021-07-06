package com.genersoft.iot.vmp.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

//@Configuration("userSetup")
//public class UserSetup {
//    @Value("${userSettings.savePositionHistory:false}")
//    boolean savePositionHistory;
//
//    @Value("${userSettings.autoApplyPlay}")
//    private boolean autoApplyPlay;
//
//    @Value("${userSettings.seniorSdp:false}")
//    private boolean seniorSdp;
//
//    @Value("${userSettings.playTimeout:18000}")
//    private long playTimeout;
//
//    @Value("${userSettings.waitTrack:false}")
//    private boolean waitTrack;
//
//    @Value("${userSettings.interfaceAuthentication}")
//    private boolean interfaceAuthentication;
//
//    @Value("${userSettings.recordPushLive}")
//    private boolean recordPushLive;
//
//    @Value("${userSettings.interfaceAuthenticationExcludes:}")
//    private String interfaceAuthenticationExcludes;
//
//    public boolean getSavePositionHistory() {
//        return savePositionHistory;
//    }
//
//    public boolean isSavePositionHistory() {
//        return savePositionHistory;
//    }
//
//    public boolean isAutoApplyPlay() {
//        return autoApplyPlay;
//    }
//
//    public boolean isSeniorSdp() {
//        return seniorSdp;
//    }
//
//    public long getPlayTimeout() {
//        return playTimeout;
//    }
//
//    public boolean isWaitTrack() {
//        return waitTrack;
//    }
//
//    public boolean isInterfaceAuthentication() {
//        return interfaceAuthentication;
//    }
//
//    public boolean isRecordPushLive() {
//        return recordPushLive;
//    }
//
//    public String getInterfaceAuthenticationExcludes() {
//        return interfaceAuthenticationExcludes;
//    }
//}


@Component
@ConfigurationProperties(prefix = "user-settings", ignoreInvalidFields = true)
public class UserSetup {

    private Boolean savePositionHistory = Boolean.FALSE;

    private Boolean autoApplyPlay = Boolean.FALSE;

    private Boolean seniorSdp = Boolean.FALSE;

    private Long playTimeout = 18000L;

    private Boolean waitTrack = Boolean.FALSE;

    private Boolean interfaceAuthentication = Boolean.TRUE;

    private Boolean recordPushLive = Boolean.FALSE;

    private List<String> interfaceAuthenticationExcludes = new ArrayList<>();

    public Boolean getSavePositionHistory() {
        return savePositionHistory;
    }

    public Boolean isSavePositionHistory() {
        return savePositionHistory;
    }

    public Boolean isAutoApplyPlay() {
        return autoApplyPlay;
    }

    public Boolean isSeniorSdp() {
        return seniorSdp;
    }

    public Long getPlayTimeout() {
        return playTimeout;
    }

    public Boolean isWaitTrack() {
        return waitTrack;
    }

    public Boolean isInterfaceAuthentication() {
        return interfaceAuthentication;
    }

    public Boolean isRecordPushLive() {
        return recordPushLive;
    }

    public List<String> getInterfaceAuthenticationExcludes() {
        return interfaceAuthenticationExcludes;
    }
}
