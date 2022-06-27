package com.genersoft.iot.vmp.service.bean;

import java.util.stream.Stream;

/**
 * 当上级平台
 * @author lin
 */
public class MessageForPushChannel {
    /**
     * 消息类型
     * 0 流注销 1 流注册
     */
    private int type;

    /**
     * 流应用名
     */
    private String app;

    /**
     * 流Id
     */
    private String stream;

    /**
     * 国标ID
     */
    private String gbId;

    /**
     * 请求的平台ID
     */
    private String platFormId;

    /**
     * 请求平台名称
     */
    private String platFormName;

    /**
     * WVP服务ID
     */
    private String serverId;

    /**
     * 目标流媒体节点ID
     */
    private String mediaServerId;

    public static MessageForPushChannel getInstance(int type, String app, String stream, String gbId,
                                                    String platFormId, String platFormName, String serverId,
                                                    String mediaServerId){
        MessageForPushChannel messageForPushChannel = new MessageForPushChannel();
        messageForPushChannel.setType(type);
        messageForPushChannel.setGbId(gbId);
        messageForPushChannel.setApp(app);
        messageForPushChannel.setStream(stream);
        messageForPushChannel.setMediaServerId(mediaServerId);
        messageForPushChannel.setPlatFormId(platFormId);
        messageForPushChannel.setPlatFormName(platFormName);
        return messageForPushChannel;
    }


    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getGbId() {
        return gbId;
    }

    public void setGbId(String gbId) {
        this.gbId = gbId;
    }

    public String getPlatFormId() {
        return platFormId;
    }

    public void setPlatFormId(String platFormId) {
        this.platFormId = platFormId;
    }

    public String getPlatFormName() {
        return platFormName;
    }

    public void setPlatFormName(String platFormName) {
        this.platFormName = platFormName;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getMediaServerId() {
        return mediaServerId;
    }

    public void setMediaServerId(String mediaServerId) {
        this.mediaServerId = mediaServerId;
    }
}
