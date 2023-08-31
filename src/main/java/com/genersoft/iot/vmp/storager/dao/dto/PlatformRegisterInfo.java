package com.genersoft.iot.vmp.storager.dao.dto;

/**
 * 平台发送注册/注销消息时缓存此消息
 * @author lin
 */
public class PlatformRegisterInfo {

    /**
     * 平台Id
     */
    private String platformId;

    /**
     * 是否时注册，false为注销
     */
    private boolean register;

    public static PlatformRegisterInfo getInstance(String platformId, boolean register) {
        PlatformRegisterInfo platformRegisterInfo = new PlatformRegisterInfo();
        platformRegisterInfo.setPlatformId(platformId);
        platformRegisterInfo.setRegister(register);
        return platformRegisterInfo;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public boolean isRegister() {
        return register;
    }

    public void setRegister(boolean register) {
        this.register = register;
    }
}
