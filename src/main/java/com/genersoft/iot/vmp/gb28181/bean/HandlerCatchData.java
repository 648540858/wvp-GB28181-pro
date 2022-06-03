package com.genersoft.iot.vmp.gb28181.bean;

import org.dom4j.Element;

import javax.sip.RequestEvent;

/**
 * @author lin
 */
public class HandlerCatchData {
    private RequestEvent evt;
    private Device device;
    private Element rootElement;

    public HandlerCatchData(RequestEvent evt, Device device, Element rootElement) {
        this.evt = evt;
        this.device = device;
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

    public Element getRootElement() {
        return rootElement;
    }

    public void setRootElement(Element rootElement) {
        this.rootElement = rootElement;
    }
}
