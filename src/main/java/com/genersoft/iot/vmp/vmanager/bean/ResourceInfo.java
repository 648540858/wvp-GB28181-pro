package com.genersoft.iot.vmp.vmanager.bean;

public class ResourceInfo {

    private ResourceBaceInfo device;
    private ResourceBaceInfo channel;
    private ResourceBaceInfo push;
    private ResourceBaceInfo proxy;

    public ResourceBaceInfo getDevice() {
        return device;
    }

    public void setDevice(ResourceBaceInfo device) {
        this.device = device;
    }

    public ResourceBaceInfo getChannel() {
        return channel;
    }

    public void setChannel(ResourceBaceInfo channel) {
        this.channel = channel;
    }

    public ResourceBaceInfo getPush() {
        return push;
    }

    public void setPush(ResourceBaceInfo push) {
        this.push = push;
    }

    public ResourceBaceInfo getProxy() {
        return proxy;
    }

    public void setProxy(ResourceBaceInfo proxy) {
        this.proxy = proxy;
    }
}
