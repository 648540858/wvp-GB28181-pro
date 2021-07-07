package com.genersoft.iot.vmp.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


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

    public void setSavePositionHistory(Boolean savePositionHistory) {
        this.savePositionHistory = savePositionHistory;
    }

    public void setAutoApplyPlay(Boolean autoApplyPlay) {
        this.autoApplyPlay = autoApplyPlay;
    }

    public void setSeniorSdp(Boolean seniorSdp) {
        this.seniorSdp = seniorSdp;
    }

    public void setPlayTimeout(Long playTimeout) {
        this.playTimeout = playTimeout;
    }

    public void setWaitTrack(Boolean waitTrack) {
        this.waitTrack = waitTrack;
    }

    public void setInterfaceAuthentication(boolean interfaceAuthentication) {
        this.interfaceAuthentication = interfaceAuthentication;
    }

    public void setRecordPushLive(Boolean recordPushLive) {
        this.recordPushLive = recordPushLive;
    }

    public void setInterfaceAuthenticationExcludes(List<String> interfaceAuthenticationExcludes) {
        this.interfaceAuthenticationExcludes = interfaceAuthenticationExcludes;
    }
}
