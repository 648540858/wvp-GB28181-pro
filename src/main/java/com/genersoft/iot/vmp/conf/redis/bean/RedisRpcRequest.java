package com.genersoft.iot.vmp.conf.redis.bean;

/**
 * 通过redis发送请求
 */
public class RedisRpcRequest {

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
     * 访问的路径
     */
    private String uri;

    /**
     * 参数
     */
    private Object param;

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

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Object getParam() {
        return param;
    }

    public void setParam(Object param) {
        this.param = param;
    }

    public long getSn() {
        return sn;
    }

    public void setSn(long sn) {
        this.sn = sn;
    }

    @Override
    public String toString() {
        return "RedisRpcRequest{" +
                "fromId='" + fromId + '\'' +
                ", toId='" + toId + '\'' +
                ", sn='" + sn + '\'' +
                ", uri='" + uri + '\'' +
                ", param=" + param +
                '}';
    }

    public RedisRpcResponse getResponse() {
        RedisRpcResponse response = new RedisRpcResponse();
        response.setFromId(fromId);
        response.setToId(toId);
        response.setSn(sn);
        response.setUri(uri);
        return response;
    }
}
