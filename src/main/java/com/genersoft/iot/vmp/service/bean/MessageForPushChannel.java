package com.genersoft.iot.vmp.service.bean;

import lombok.Data;

/**
 * 当上级平台
 * @author lin
 */

@Data
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
     * 请求的平台国标编号
     */
    private String platFormId;

    /**
     * 请求的平台自增ID
     */
    private int platFormIndex;

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
        messageForPushChannel.setServerId(serverId);
        messageForPushChannel.setMediaServerId(mediaServerId);
        messageForPushChannel.setPlatFormId(platFormId);
        messageForPushChannel.setPlatFormName(platFormName);
        return messageForPushChannel;
    }
}
