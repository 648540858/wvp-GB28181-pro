package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommonGBChannelTest {

    @Test
    void encodeCatalogAddForCameraChannelShouldNotDuplicateRegisterWayOrSecrecy() {
        CommonGBChannel channel = new CommonGBChannel();
        channel.setGbDeviceId("42010100001320000006");
        channel.setGbName("camera");
        channel.setGbManufacturer("Dahua");
        channel.setGbModel("DH-SD2904-GN");
        channel.setGbOwner("Owner");
        channel.setGbCivilCode("420101");
        channel.setGbAddress("address");
        channel.setGbParentId("42010100001320001006");
        channel.setGbParental(0);
        channel.setGbSafetyWay(0);
        channel.setGbRegisterWay(1);
        channel.setGbSecrecy(0);
        channel.setGbIpAddress("172.16.0.145");
        channel.setGbPort(80);
        channel.setGbStatus("ON");

        String content = channel.encode(CatalogEvent.ADD, "42010100002000000001");

        assertEquals(1, count(content, "<RegisterWay>"));
        assertEquals(1, count(content, "<Secrecy>"));
    }

    private static int count(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
