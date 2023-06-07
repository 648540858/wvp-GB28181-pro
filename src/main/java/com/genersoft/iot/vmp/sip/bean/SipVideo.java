package com.genersoft.iot.vmp.sip.bean;

/**
 * 从一个sip服务中获取一个视频
 */
public class SipVideo {

    /**
     * ID
     */
    private int id;

    /**
     * sip 服务器帐号信息ID
     */
    private int sipAccountId;

    /**
     * 指定使用的流媒体，为空则自动获取
     */
    private String mediaServerId;

    /**
     * 从sip服务中获取的视频的设备编号
     */
    private String requestNo;

    /**
     * 服务重启时释放自动拉起视频
     */
    private boolean autoReconnectOnReboot;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 更新时间
     */
    private String updateTime;




    public int getSipAccountId() {
        return sipAccountId;
    }

    public void setSipAccountId(int sipAccountId) {
        this.sipAccountId = sipAccountId;
    }

    public String getMediaServerId() {
        return mediaServerId;
    }

    public void setMediaServerId(String mediaServerId) {
        this.mediaServerId = mediaServerId;
    }

    public String getRequestNo() {
        return requestNo;
    }

    public void setRequestNo(String requestNo) {
        this.requestNo = requestNo;
    }

    public boolean isAutoReconnectOnReboot() {
        return autoReconnectOnReboot;
    }

    public void setAutoReconnectOnReboot(boolean autoReconnectOnReboot) {
        this.autoReconnectOnReboot = autoReconnectOnReboot;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
}
