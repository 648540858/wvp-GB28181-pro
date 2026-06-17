package com.genersoft.iot.vmp.gb28181.bean;

import org.dom4j.Element;

public interface DeviceConfigAware {

    String configType();

    void fromXml(Element element);
}
