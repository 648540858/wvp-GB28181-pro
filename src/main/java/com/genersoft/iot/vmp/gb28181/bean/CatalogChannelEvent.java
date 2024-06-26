package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;

import java.lang.reflect.InvocationTargetException;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class CatalogChannelEvent extends DeviceChannel{

    private String event;

    private DeviceChannel channel;

    public static CatalogChannelEvent decode(Element element) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Element eventElement = element.element("Event");
        CatalogChannelEvent catalogChannelEvent = new CatalogChannelEvent();
        if (eventElement != null) {
            catalogChannelEvent.setEvent(eventElement.getText());
        }else {
            catalogChannelEvent.setEvent(CatalogEvent.ADD);
        }
        DeviceChannel deviceChannel;
        if (CatalogEvent.ADD.equalsIgnoreCase(catalogChannelEvent.getEvent()) ||
        CatalogEvent.UPDATE.equalsIgnoreCase(catalogChannelEvent.getEvent()) ){
            deviceChannel = DeviceChannel.decode(element);
        }else {
            deviceChannel = DeviceChannel.decodeWithOnlyDeviceId(element);
        }
        catalogChannelEvent.setChannel(deviceChannel);
        return catalogChannelEvent;
    }
}
