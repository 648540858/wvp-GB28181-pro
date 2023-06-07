package com.genersoft.iot.vmp.sip.bean;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * sip用户帐号，用于接收sip视频以及推送视频到sip服务中
 */
public class SipServerAccount {

    /**
     * ID
     */
    private Integer id;

    /**
     * 关联的SIP服务器ID
     */
    private Integer sipServerId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 关联一个国标设备作为帐号视频，用于将自己的视频加入到sip会议中
     * deviceChannelId/pushStreamId/proxyStreamId任选其一
     */
    private Integer deviceChannelId;

    /**
     * 关联一个推流设备作为帐号视频，用于将自己的视频加入到sip会议中
     * deviceChannelId/pushStreamId/proxyStreamId任选其一
     */
    private Integer pushStreamId;

    /**
     * 关联一个拉流代理作为帐号视频，用于将自己的视频加入到sip会议中
     * deviceChannelId/pushStreamId/proxyStreamId任选其一
     */
    private Integer proxyStreamId;

    /**
     * 状态
     */
    private boolean status;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 更新时间
     */
    private String updateTime;

    private final List<SipVideo> sipVideoList = new CopyOnWriteArrayList<>();


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSipServerId() {
        return sipServerId;
    }

    public void setSipServerId(Integer sipServerId) {
        this.sipServerId = sipServerId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getDeviceChannelId() {
        return deviceChannelId;
    }

    public void setDeviceChannelId(Integer deviceChannelId) {
        this.deviceChannelId = deviceChannelId;
    }

    public Integer getPushStreamId() {
        return pushStreamId;
    }

    public void setPushStreamId(Integer pushStreamId) {
        this.pushStreamId = pushStreamId;
    }

    public Integer getProxyStreamId() {
        return proxyStreamId;
    }

    public void setProxyStreamId(Integer proxyStreamId) {
        this.proxyStreamId = proxyStreamId;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public void addVideo(SipVideo sipVideo) {
        sipVideoList.add(sipVideo);
    }

    public void removeVideoById(int id) {
        if (sipVideoList.size() == 0) {
            return;
        }
        for (int i = 0; i < sipVideoList.size(); i++) {
            if (sipVideoList.get(i).getId() == id) {
                sipVideoList.remove(i);
                return;
            }
        }
    }

}
