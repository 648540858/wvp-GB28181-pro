package com.genersoft.iot.vmp.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置文件 user-settings 映射的配置信息
 */
@Component
@ConfigurationProperties(prefix = "user-settings", ignoreInvalidFields = true)
@Order(0)
public class UserSetting {

    private Boolean savePositionHistory = Boolean.FALSE;

    private Boolean autoApplyPlay = Boolean.FALSE;

    private Boolean seniorSdp = Boolean.FALSE;

    private Integer playTimeout = 18000;

    private int platformPlayTimeout = 60000;

    private Boolean interfaceAuthentication = Boolean.TRUE;

    private Boolean recordPushLive = Boolean.TRUE;

    private Boolean recordSip = Boolean.TRUE;

    private Boolean logInDatabase = Boolean.TRUE;

    private Boolean usePushingAsStatus = Boolean.TRUE;

    private Boolean useSourceIpAsStreamIp = Boolean.FALSE;

    private Boolean sipUseSourceIpAsRemoteAddress = Boolean.FALSE;

    private Boolean streamOnDemand = Boolean.TRUE;

    private Boolean pushAuthority = Boolean.TRUE;

    private Boolean syncChannelOnDeviceOnline = Boolean.FALSE;

    private Boolean sipLog = Boolean.FALSE;
    private Boolean sqlLog = Boolean.FALSE;
    private Boolean sendToPlatformsWhenIdLost = Boolean.FALSE;

    private Boolean refuseChannelStatusChannelFormNotify = Boolean.FALSE;

    private Boolean deviceStatusNotify = Boolean.FALSE;
    private Boolean useCustomSsrcForParentInvite = Boolean.TRUE;

    private String serverId = "000000";

    private String recordPath = null;

    private String thirdPartyGBIdReg = "[\\s\\S]*";

    private String civilCodeFile = "classpath:civilCode.csv";

    private List<String> interfaceAuthenticationExcludes = new ArrayList<>();

    private List<String> allowedOrigins = new ArrayList<>();

    private int maxNotifyCountQueue = 10000;

    private int registerAgainAfterTime = 60;

    private boolean registerKeepIntDialog = false;

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

    public Boolean getLogInDatabase() {
        return logInDatabase;
    }

    public void setLogInDatabase(Boolean logInDatabase) {
        this.logInDatabase = logInDatabase;
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

    public Boolean isUsePushingAsStatus() {
        return usePushingAsStatus;
    }

    public void setUsePushingAsStatus(Boolean usePushingAsStatus) {
        this.usePushingAsStatus = usePushingAsStatus;
    }

    public Boolean getStreamOnDemand() {
        return streamOnDemand;
    }

    public void setStreamOnDemand(Boolean streamOnDemand) {
        this.streamOnDemand = streamOnDemand;
    }

    public Boolean getUseSourceIpAsStreamIp() {
        return useSourceIpAsStreamIp;
    }

    public void setUseSourceIpAsStreamIp(Boolean useSourceIpAsStreamIp) {
        this.useSourceIpAsStreamIp = useSourceIpAsStreamIp;
    }

    public Boolean getPushAuthority() {
        return pushAuthority;
    }

    public void setPushAuthority(Boolean pushAuthority) {
        this.pushAuthority = pushAuthority;
    }

    public Boolean getSyncChannelOnDeviceOnline() {
        return syncChannelOnDeviceOnline;
    }

    public void setSyncChannelOnDeviceOnline(Boolean syncChannelOnDeviceOnline) {
        this.syncChannelOnDeviceOnline = syncChannelOnDeviceOnline;
    }

    public Boolean getSipUseSourceIpAsRemoteAddress() {
        return sipUseSourceIpAsRemoteAddress;
    }

    public void setSipUseSourceIpAsRemoteAddress(Boolean sipUseSourceIpAsRemoteAddress) {
        this.sipUseSourceIpAsRemoteAddress = sipUseSourceIpAsRemoteAddress;
    }

    public Boolean getSipLog() {
        return sipLog;
    }

    public void setSipLog(Boolean sipLog) {
        this.sipLog = sipLog;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public Boolean getSendToPlatformsWhenIdLost() {
        return sendToPlatformsWhenIdLost;
    }

    public void setSendToPlatformsWhenIdLost(Boolean sendToPlatformsWhenIdLost) {
        this.sendToPlatformsWhenIdLost = sendToPlatformsWhenIdLost;
    }

    public Boolean getRefuseChannelStatusChannelFormNotify() {
        return refuseChannelStatusChannelFormNotify;
    }

    public void setRefuseChannelStatusChannelFormNotify(Boolean refuseChannelStatusChannelFormNotify) {
        this.refuseChannelStatusChannelFormNotify = refuseChannelStatusChannelFormNotify;
    }

    public String getRecordPath() {
        return recordPath;
    }

    public void setRecordPath(String recordPath) {
        this.recordPath = recordPath;
    }

    public int getMaxNotifyCountQueue() {
        return maxNotifyCountQueue;
    }

    public void setMaxNotifyCountQueue(int maxNotifyCountQueue) {
        this.maxNotifyCountQueue = maxNotifyCountQueue;
    }

    public Boolean getDeviceStatusNotify() {
        return deviceStatusNotify;
    }

    public void setDeviceStatusNotify(Boolean deviceStatusNotify) {
        this.deviceStatusNotify = deviceStatusNotify;
    }

    public Boolean getUseCustomSsrcForParentInvite() {
        return useCustomSsrcForParentInvite;
    }

    public void setUseCustomSsrcForParentInvite(Boolean useCustomSsrcForParentInvite) {
        this.useCustomSsrcForParentInvite = useCustomSsrcForParentInvite;
    }

    public Boolean getSqlLog() {
        return sqlLog;
    }

    public void setSqlLog(Boolean sqlLog) {
        this.sqlLog = sqlLog;
    }

    public String getCivilCodeFile() {
        return civilCodeFile;
    }

    public void setCivilCodeFile(String civilCodeFile) {
        this.civilCodeFile = civilCodeFile;
    }

    public int getRegisterAgainAfterTime() {
        return registerAgainAfterTime;
    }

    public void setRegisterAgainAfterTime(int registerAgainAfterTime) {
        this.registerAgainAfterTime = registerAgainAfterTime;
    }

    public boolean isRegisterKeepIntDialog() {
        return registerKeepIntDialog;
    }

    public void setRegisterKeepIntDialog(boolean registerKeepIntDialog) {
        this.registerKeepIntDialog = registerKeepIntDialog;
    }
}
