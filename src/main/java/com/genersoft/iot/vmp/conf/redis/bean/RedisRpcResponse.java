package com.genersoft.iot.vmp.conf.redis.bean;

/**
 * 通过redis发送回复
 */
public class RedisRpcResponse {

    /**
     * 来自的WVP ID
     */
    private String fromId;


    /**
     * 目标的WVP ID
     */
    private String toId;


    /**
     * 序列号
     */
    private long sn;

    /**
     * 状态码
     */
    private int statusCode;

    /**
     * 访问的路径
     */
    private String uri;

    /**
     * 参数
     */
    private Object body;

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

    public long getSn() {
        return sn;
    }

    public void setSn(long sn) {
        this.sn = sn;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }
}
