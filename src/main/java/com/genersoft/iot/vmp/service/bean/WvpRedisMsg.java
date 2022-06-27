package com.genersoft.iot.vmp.service.bean;

/**
 * @author lin
 */
public class WvpRedisMsg {

    public static WvpRedisMsg getInstance(String fromId, String toId, String type, String cmd, String serial, String content){
        WvpRedisMsg wvpRedisMsg = new WvpRedisMsg();
        wvpRedisMsg.setFromId(fromId);
        wvpRedisMsg.setToId(toId);
        wvpRedisMsg.setType(type);
        wvpRedisMsg.setCmd(cmd);
        wvpRedisMsg.setSerial(serial);
        wvpRedisMsg.setContent(content);
        return wvpRedisMsg;
    }

    private String fromId;

    private String toId;
    /**
     * req 请求, res 回复
     */
    private String type;
    private String cmd;

    /**
     * 消息的ID
     */
    private String serial;
    private Object content;

    private final static String requestTag = "req";
    private final static String responseTag = "res";

    public static WvpRedisMsg getRequestInstance(String fromId, String toId, String cmd, String serial, Object content) {
        WvpRedisMsg wvpRedisMsg = new WvpRedisMsg();
        wvpRedisMsg.setType(requestTag);
        wvpRedisMsg.setFromId(fromId);
        wvpRedisMsg.setToId(toId);
        wvpRedisMsg.setCmd(cmd);
        wvpRedisMsg.setSerial(serial);
        wvpRedisMsg.setContent(content);
        return wvpRedisMsg;
    }

    public static WvpRedisMsg getResponseInstance() {
        WvpRedisMsg wvpRedisMsg = new WvpRedisMsg();
        wvpRedisMsg.setType(responseTag);
        return wvpRedisMsg;
    }

    public static WvpRedisMsg getResponseInstance(String fromId, String toId, String cmd, String serial, Object content) {
        WvpRedisMsg wvpRedisMsg = new WvpRedisMsg();
        wvpRedisMsg.setType(responseTag);
        wvpRedisMsg.setFromId(fromId);
        wvpRedisMsg.setToId(toId);
        wvpRedisMsg.setCmd(cmd);
        wvpRedisMsg.setSerial(serial);
        wvpRedisMsg.setContent(content);
        return wvpRedisMsg;
    }

    public static boolean isRequest(WvpRedisMsg wvpRedisMsg) {
        return requestTag.equals(wvpRedisMsg.getType());
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getFromId() {
        return fromId;
    }

    public void setFromId(String fromId) {
        this.fromId = fromId;
    }

    public String getToId() {
        return toId;
    }

    public void setToId(String toId) {
        this.toId = toId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }
}
