package com.genersoft.iot.vmp.conf.redis.bean;

public class RedisRpcMessage {

    private RedisRpcRequest request;

    private RedisRpcResponse response;

    public RedisRpcRequest getRequest() {
        return request;
    }

    public void setRequest(RedisRpcRequest request) {
        this.request = request;
    }

    public RedisRpcResponse getResponse() {
        return response;
    }

    public void setResponse(RedisRpcResponse response) {
        this.response = response;
    }
}
