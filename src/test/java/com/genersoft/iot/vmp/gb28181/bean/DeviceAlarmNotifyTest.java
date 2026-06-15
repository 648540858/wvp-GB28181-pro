package com.genersoft.iot.vmp.gb28181.bean;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeviceAlarmNotifyTest {

    @Test
    void fromXml_withoutAlarmType_shouldNotThrowNpe() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Notify>
                    <DeviceID>55123456781381000010</DeviceID>
                    <AlarmPriority>1</AlarmPriority>
                    <AlarmMethod>7</AlarmMethod>
                    <AlarmTime>2026-06-05T09:46:05</AlarmTime>
                    <AlarmDescription>1001,1780623964994529058,55123456781381000010,25123456781381000050,55LCPCweb10</AlarmDescription>
                    <Longitude>0.0</Longitude>
                    <Latitude>0.0</Latitude>
                </Notify>
                """;

        Element root = DocumentHelper.parseText(xml).getRootElement();
        DeviceAlarmNotify notify = DeviceAlarmNotify.fromXml(root);

        assertNotNull(notify);
        assertEquals(Integer.valueOf(7), notify.getAlarmMethod());
        assertNull(notify.getAlarmType(), "AlarmType should be null when not present in XML");

        // Simulate the exact code path from AlarmNotifyMessageHandler.executeTaskQueue lines 131-141
        // which was causing the NPE
        AlarmChannelMessage alarmChannelMessage = new AlarmChannelMessage();
        assertDoesNotThrow(() -> {
            alarmChannelMessage.setAlarmType(notify.getAlarmType());
            alarmChannelMessage.setAlarmSn(notify.getAlarmMethod());
            alarmChannelMessage.setAlarmDescription(notify.getAlarmDescription());
            alarmChannelMessage.setGbId(notify.getChannelId());
        }, "setAlarmType(null) should not throw NPE when field type is Integer");
        assertNull(alarmChannelMessage.getAlarmType());
        assertEquals(Integer.valueOf(7), alarmChannelMessage.getAlarmSn());
    }

    @Test
    void fromXml_withAlarmType_shouldParseCorrectly() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Notify>
                    <DeviceID>34020000001320000001</DeviceID>
                    <AlarmPriority>1</AlarmPriority>
                    <AlarmMethod>2</AlarmMethod>
                    <AlarmTime>2026-06-05T10:30:00</AlarmTime>
                    <AlarmDescription>Video loss alarm</AlarmDescription>
                    <Longitude>116.397</Longitude>
                    <Latitude>39.908</Latitude>
                    <AlarmType>1</AlarmType>
                </Notify>
                """;

        Element root = DocumentHelper.parseText(xml).getRootElement();
        DeviceAlarmNotify notify = DeviceAlarmNotify.fromXml(root);

        assertNotNull(notify);
        assertEquals(Integer.valueOf(2), notify.getAlarmMethod());
        assertEquals(Integer.valueOf(1), notify.getAlarmType());

        AlarmChannelMessage msg = new AlarmChannelMessage();
        assertDoesNotThrow(() -> msg.setAlarmType(notify.getAlarmType()));
        assertEquals(Integer.valueOf(1), msg.getAlarmType());
    }

    @Test
    void fromXml_withAlarmTypeInInfo_shouldUseInfoValue() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Notify>
                    <DeviceID>34020000001320000001</DeviceID>
                    <AlarmPriority>1</AlarmPriority>
                    <AlarmMethod>5</AlarmMethod>
                    <AlarmTime>2026-06-05T10:30:00</AlarmTime>
                    <AlarmDescription>Motion detection</AlarmDescription>
                    <Longitude>116.397</Longitude>
                    <Latitude>39.908</Latitude>
                    <AlarmType>9</AlarmType>
                    <Info>
                        <AlarmType>2</AlarmType>
                    </Info>
                </Notify>
                """;

        Element root = DocumentHelper.parseText(xml).getRootElement();
        DeviceAlarmNotify notify = DeviceAlarmNotify.fromXml(root);

        assertNotNull(notify);
        assertEquals(Integer.valueOf(2), notify.getAlarmType(),
                "AlarmType should use Info/AlarmType value when present");
    }
}
