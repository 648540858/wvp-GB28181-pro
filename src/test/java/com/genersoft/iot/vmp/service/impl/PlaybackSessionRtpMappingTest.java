package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.bean.ResultForOnPublish;
import com.genersoft.iot.vmp.service.bean.RTPServerParam;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 验证独立回放的流名、接收器和单端口发布鉴权始终指向同一会话。 */
class PlaybackSessionRtpMappingTest {
    private static final String START = "2026-10-23 20:40:00";
    private static final String END = "2026-10-23 20:40:20";
    private static final String SESSION = "123e4567e89b12d3a456426614174000";
    private static final String LEGACY = "device_channel_20261023204000_20261023204020";
    private static final String ISOLATED = "device_channel_" + SESSION + "_20261023204000_20261023204020";
    private RtpServerServiceImpl service;
    private Device device;
    private DeviceChannel channel;

    /** 准备无需外部服务的设备和通道，仅在收流映射用例中注入资源mock。 */
    @BeforeEach
    void setUp() {
        service = new RtpServerServiceImpl();
        device = new Device();
        device.setDeviceId("device");
        device.setStreamMode("TCP-PASSIVE");
        channel = new DeviceChannel();
        channel.setDeviceId("channel");
        channel.setHasAudio(true);
    }

    /** UUID表示幂等且播放器互相隔离，末尾时间继续兼容现用fMP4时长提取契约。 */
    @Test
    void canonicalSessionNamesPreserveIdempotencyAndLegacyFormat() {
        String first = service.getPlaybackStream(device, channel, START, END, SESSION);
        assertEquals(ISOLATED, first);
        assertEquals(first, service.getPlaybackStream(device, channel, START, END, "123E4567-E89B-12D3-A456-426614174000"));
        assertNotEquals(first, service.getPlaybackStream(device, channel, START, END, "123e4567e89b12d3a456426614174001"));
        assertEquals(LEGACY, service.getPlaybackStream(device, channel, START, END));
        java.util.regex.Matcher duration = java.util.regex.Pattern.compile("_(\\d{14})_(\\d{14})\\.live\\.mp4")
                .matcher(first + ".live.mp4");
        assertTrue(duration.find(), "现用fMP4适配器必须仍能从URL尾部提取录像时间");
        assertEquals("20261023204000", duration.group(1));
        assertEquals("20261023204020", duration.group(2));
    }

    /** 非法标识在分配SSRC之前失败，不能留下无法寻址的接收器或鉴权记录。 */
    @Test
    void malformedSessionIsRejectedBeforeAllocatingResources() {
        SSRCFactory factory = mock(SSRCFactory.class);
        ReflectionTestUtils.setField(service, "ssrcFactory", factory);
        ControllerException error = assertThrows(ControllerException.class, () -> service.openGbRTPServerForPlayback(
                new MediaServer(), device, channel, START, END, "bad_session", (code, message, data) -> {}));
        assertEquals(ErrorCode.ERROR400.getCode(), error.getCode());
        verifyNoInteractions(factory);
    }

    /** 单端口下原始SSRC流的重命名目标与Hook监听流及返回流必须完全相同。 */
    @Test
    @SuppressWarnings("unchecked")
    void receiverAndPublishAuthenticationUseTheSameIsolatedStream() {
        RtpServerServiceImpl spyService = spy(service);
        SSRCFactory factory = mock(SSRCFactory.class);
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        ReflectionTestUtils.setField(spyService, "ssrcFactory", factory);
        ReflectionTestUtils.setField(spyService, "redisTemplate", redis);
        MediaServer media = new MediaServer();
        when(factory.getPlayBackSsrc(media)).thenReturn("1200000001");
        when(redis.opsForValue()).thenReturn(values);
        doReturn(10000).when(spyService).openCommonRTPServer(any(), any());

        SSRCInfo result = spyService.openGbRTPServerForPlayback(media, device, channel, START, END, SESSION,
                (code, message, data) -> {});

        ArgumentCaptor<RTPServerParam> receiver = ArgumentCaptor.forClass(RTPServerParam.class);
        verify(spyService).openCommonRTPServer(receiver.capture(), any());
        assertEquals(ISOLATED, receiver.getValue().getStreamId());
        assertEquals(receiver.getValue().getStreamId(), result.getStream());
        ArgumentCaptor<Object> authentication = ArgumentCaptor.forClass(Object.class);
        verify(values).set(eq("VMP_RTP_AUTHENTICATE:" + Long.toHexString(1200000001L).toUpperCase()), authentication.capture());
        assertEquals(result.getStream(), ((ResultForOnPublish) authentication.getValue()).getStream_replace());
    }
}
