package com.genersoft.iot.vmp.vmanager.bean;

public class ResourceInfo {

    private ResourceBaseInfo device;
    private ResourceBaseInfo channel;
    private ResourceBaseInfo push;
    private ResourceBaseInfo proxy;

    public ResourceBaseInfo getDevice() {
        return device;
    }

    public void setDevice(ResourceBaseInfo device) {
        this.device = device;
    }

    public ResourceBaseInfo getChannel() {
        return channel;
    }

    public void setChannel(ResourceBaseInfo channel) {
        this.channel = channel;
    }

    public ResourceBaseInfo getPush() {
        return push;
    }

    public void setPush(ResourceBaseInfo push) {
        this.push = push;
    }

    public ResourceBaseInfo getProxy() {
        return proxy;
    }

    public void setProxy(ResourceBaseInfo proxy) {
        this.proxy = proxy;
    }
}
