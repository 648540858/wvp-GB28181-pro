package com.genersoft.iot.vmp.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置文件 user-settings 映射的配置信息
 */
@Component
@ConfigurationProperties(prefix = "user-settings", ignoreInvalidFields = true)
public class UserSetting {

    private Boolean savePositionHistory = Boolean.FALSE;

    private Boolean autoApplyPlay = Boolean.FALSE;

    private Boolean seniorSdp = Boolean.FALSE;

    private Integer playTimeout = 18000;

    private int platformPlayTimeout = 60000;

    private Boolean interfaceAuthentication = Boolean.TRUE;

    private Boolean recordPushLive = Boolean.TRUE;

    private Boolean recordSip = Boolean.TRUE;

    private Boolean logInDatebase = Boolean.TRUE;

    private Boolean redisConfig = Boolean.TRUE;

    private String serverId = "000000";

    private String thirdPartyGBIdReg = "[\\s\\S]*";

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

    public Integer getPlayTimeout() {
        return playTimeout;
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

    public void setPlayTimeout(Integer playTimeout) {
        this.playTimeout = playTimeout;
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

    public Boolean getLogInDatebase() {
        return logInDatebase;
    }

    public void setLogInDatebase(Boolean logInDatebase) {
        this.logInDatebase = logInDatebase;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getThirdPartyGBIdReg() {
        return thirdPartyGBIdReg;
    }

    public void setThirdPartyGBIdReg(String thirdPartyGBIdReg) {
        this.thirdPartyGBIdReg = thirdPartyGBIdReg;
    }

    public Boolean getRedisConfig() {
        return redisConfig;
    }

    public void setRedisConfig(Boolean redisConfig) {
        this.redisConfig = redisConfig;
    }

    public Boolean getRecordSip() {
        return recordSip;
    }

    public void setRecordSip(Boolean recordSip) {
        this.recordSip = recordSip;
    }

    public int getPlatformPlayTimeout() {
        return platformPlayTimeout;
    }

    public void setPlatformPlayTimeout(int platformPlayTimeout) {
        this.platformPlayTimeout = platformPlayTimeout;
    }
}
