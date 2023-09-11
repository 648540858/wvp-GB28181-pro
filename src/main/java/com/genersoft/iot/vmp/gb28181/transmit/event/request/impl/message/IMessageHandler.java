package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import org.dom4j.Element;

import javax.sip.RequestEvent;

public interface IMessageHandler {
    /**
     * 处理来自设备的信息
     * @param evt
     * @param device
     */
    void handForDevice(RequestEvent evt, Device device, Element element);

    /**
     * 处理来自平台的信息
     * @param evt
     * @param parentPlatform
     */
    void handForPlatform(RequestEvent evt, ParentPlatform parentPlatform, Element element);
}
