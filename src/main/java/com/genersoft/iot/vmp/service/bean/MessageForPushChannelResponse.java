package com.genersoft.iot.vmp.service.bean;

/**
 * 当redis回复推流结果上级平台
 * @author lin
 */
public class MessageForPushChannelResponse {
    /**
     * 错误玛
     * 0 成功 1 失败
     */
    private int code;
    /**
     * 错误内容
     */
    private String msg;

    /**
     * 流应用名
     */
    private String app;

    /**
     * 流Id
     */
    private String stream;



    public static MessageForPushChannelResponse getInstance(int code, String msg, String app, String stream){
        MessageForPushChannelResponse messageForPushChannel = new MessageForPushChannelResponse();
        messageForPushChannel.setCode(code);
        messageForPushChannel.setMsg(msg);
        messageForPushChannel.setApp(app);
        messageForPushChannel.setStream(stream);
        return messageForPushChannel;
    }


    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
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

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
