package com.genersoft.iot.vmp.gb28181.bean;

import org.dom4j.Element;

import javax.sip.RequestEvent;

public class SipMsgInfo {
    private RequestEvent evt;
    private  Device device;
    private ParentPlatform platform;
    private Element rootElement;

    public SipMsgInfo(RequestEvent evt, Device device, Element rootElement) {
        this.evt = evt;
        this.device = device;
        this.rootElement = rootElement;
    }

    public SipMsgInfo(RequestEvent evt, ParentPlatform platform, Element rootElement) {
        this.evt = evt;
        this.platform = platform;
        this.rootElement = rootElement;
    }

    public RequestEvent getEvt() {
        return evt;
    }

    public void setEvt(RequestEvent evt) {
        this.evt = evt;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public ParentPlatform getPlatform() {
        return platform;
    }

    public void setPlatform(ParentPlatform platform) {
        this.platform = platform;
    }

    public Element getRootElement() {
        return rootElement;
    }

    public void setRootElement(Element rootElement) {
        this.rootElement = rootElement;
    }
}
