package com.genersoft.iot.vmp.streamProxy.bean;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamProxyExcelDtoTest {

    @Test
    void sampleAndPromptRowsShouldBeIgnored() {
        assertTrue(StreamProxyExcelDto.sample().isIgnoredRow());
        assertTrue(StreamProxyExcelDto.prompt().isIgnoredRow());
    }

    @Test
    void toStreamProxyShouldConvertChineseValues() {
        StreamProxyExcelDto dto = new StreamProxyExcelDto();
        dto.setName("园区东门");
        dto.setType("FFmpeg");
        dto.setApp("live");
        dto.setStream("east-gate");
        dto.setSrcUrl("rtsp://192.168.1.10/live");
        dto.setGbDeviceId("41010500001327000001");
        dto.setTimeout("15");
        dto.setRtspType("UDP");
        dto.setEnable("是");
        dto.setEnableAudio("否");
        dto.setEnableMp4("是");
        dto.setEnableDisableNoneReader("否");
        dto.setLongitude("120.12");
        dto.setLatitude("30.28");

        StreamProxy result = dto.toStreamProxy();

        assertEquals("ffmpeg", result.getType());
        assertEquals("1", result.getRtspType());
        assertEquals(15, result.getTimeout());
        assertTrue(result.isEnable());
        assertFalse(result.isEnableAudio());
        assertTrue(result.isEnableMp4());
        assertFalse(result.isEnableDisableNoneReader());
        assertEquals(120.12, result.getGbLongitude());
        assertEquals(30.28, result.getGbLatitude());
    }

    @Test
    void toStreamProxyShouldUseTemplateDefaultsForBlankOptions() {
        StreamProxyExcelDto dto = new StreamProxyExcelDto();
        dto.setApp("live");
        dto.setStream("camera-1");
        dto.setSrcUrl("rtsp://192.168.1.11/live");
        dto.setName("园区西门");
        dto.setGbDeviceId("41010500001327000002");
        dto.setRelatesMediaServerId("自动选择");

        StreamProxy result = dto.toStreamProxy();

        assertEquals("default", result.getType());
        assertEquals("0", result.getRtspType());
        assertEquals(10, result.getTimeout());
        assertTrue(result.isEnable());
        assertTrue(result.isEnableAudio());
        assertFalse(result.isEnableMp4());
        assertTrue(result.isEnableDisableNoneReader());
        assertNull(result.getRelatesMediaServerId());
        assertNull(result.getFfmpegCmdKey());
    }

    @Test
    void toStreamProxyShouldRequireNameAndTwentyDigitGbDeviceId() {
        StreamProxyExcelDto dto = new StreamProxyExcelDto();
        dto.setApp("live");
        dto.setStream("1");
        dto.setSrcUrl("rtsp://192.168.1.11/live");

        assertEquals("名称不能为空", assertThrows(IllegalArgumentException.class, dto::toStreamProxy).getMessage());

        dto.setName("园区西门");
        dto.setGbDeviceId("123");
        assertEquals("国标编码必须为20位数字",
                assertThrows(IllegalArgumentException.class, dto::toStreamProxy).getMessage());
    }
}
