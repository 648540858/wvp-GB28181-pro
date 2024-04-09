package com.genersoft.iot.vmp.media.abl.bean.hook;

public class OnPublishABLHookParam extends ABLHookParam{
    private String ip;
    private Integer port;
    private String params;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }
}
