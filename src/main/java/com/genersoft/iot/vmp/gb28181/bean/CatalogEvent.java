package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.utils.MessageElement;

public class CatalogEvent extends DeviceChannel{

    @MessageElement("Event")
    private String event;
}
