package com.genersoft.iot.vmp.gb28181.controller;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelPlayService;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.gb28181.service.IPlayService;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaybackSessionControllerTest {
    @Mock private IPlayService playService;
    @Mock private IDeviceService deviceService;
    @Mock private IDeviceChannelService deviceChannels;
    @Mock private DeferredResultHolder resultHolder;
    @Mock private IGbChannelService channelService;
    @Mock private IGbChannelPlayService channelPlayService;
    @Mock private HttpServletRequest request;
    @Spy private UserSetting userSetting = new UserSetting();
    @InjectMocks private PlaybackController deviceController;
    @InjectMocks private ChannelController commonController;

    /** 使用真实配置超时，服务均由 mock 记录会话键，不依赖外部设备。 */
    @BeforeEach
    void configureTimeout() {
        userSetting.setPlayTimeout(180000);
    }

    /** 旧集成重复请求同一区间时保持共享回放，不能默认要求设备提供第二路媒体。 */
    @Test
    void legacyPublicStartsKeepSharedPlayback() {
        Device device = new Device();
        DeviceChannel channel = new DeviceChannel();
        when(deviceService.getDeviceByDeviceId("device")).thenReturn(device);
        when(deviceChannels.getOne("device", "channel")).thenReturn(channel);
        deviceController.start(request, "device", "channel", "2026-10-23 20:40:00", "2026-10-23 20:41:00", null);
        deviceController.start(request, "device", "channel", "2026-10-23 20:40:00", "2026-10-23 20:41:00", null);
        verify(playService, times(2)).playBack(eq(device), eq(channel), anyString(), anyString(), isNull(), any());
    }

    /** 设备入口只有收到显式身份才启用独立回放，并保持 UUID 规范化。 */
    @Test
    void deviceEntryForwardsExplicitIdentity() {
        Device device = new Device();
        DeviceChannel channel = new DeviceChannel();
        when(deviceService.getDeviceByDeviceId("device")).thenReturn(device);
        when(deviceChannels.getOne("device", "channel")).thenReturn(channel);
        deviceController.start(request, "device", "channel", "2026-10-23 20:40:00", "2026-10-23 20:41:00",
                "01234567-89AB-CDEF-0123-456789ABCDEF");
        verify(playService).playBack(eq(device), eq(channel), anyString(), anyString(),
                eq("0123456789abcdef0123456789abcdef"), any());
    }

    /** 通用国标入口未指定身份时也必须使用共享流，与旧设备入口保持相同默认行为。 */
    @Test
    void commonGbEntryDefaultsToSharedPlayback() {
        CommonGBChannel channel = new CommonGBChannel();
        channel.setDataType(ChannelDataType.GB28181);
        when(channelService.getOne(5)).thenReturn(channel);
        commonController.playback(request, 5, "2026-10-23 20:40:00", "2026-10-23 20:41:00", null);
        verify(channelPlayService).playback(eq(channel), anyLong(), anyLong(), isNull(), any());
    }

    /** 通用页面的国标回放入口也必须透传规范化的显式播放器身份。 */
    @Test
    void commonGbEntryForwardsExplicitIdentity() {
        CommonGBChannel channel = new CommonGBChannel();
        channel.setDataType(ChannelDataType.GB28181);
        when(channelService.getOne(5)).thenReturn(channel);
        commonController.playback(request, 5, "2026-10-23 20:40:00", "2026-10-23 20:41:00",
                "01234567-89AB-CDEF-0123-456789ABCDEF");
        verify(channelPlayService).playback(eq(channel), anyLong(), anyLong(), eq("0123456789abcdef0123456789abcdef"), any());
    }

    /** 没有国标会话能力的既有协议不因增加可选参数而改变原回放路径。 */
    @Test
    void nonGbEntryRetainsOriginalProtocolBehavior() {
        CommonGBChannel channel = new CommonGBChannel();
        channel.setDataType(ChannelDataType.JT_1078);
        when(channelService.getOne(6)).thenReturn(channel);
        commonController.playback(request, 6, "2026-10-23 20:40:00", "2026-10-23 20:41:00", null);
        verify(channelPlayService).playback(eq(channel), anyLong(), anyLong(), isNull(), any());
    }

    /** 非法会话参数必须在认领、设备查询和 INVITE 副作用之前被拒绝。 */
    @Test
    void malformedIdentityCannotReachPlayback() {
        assertThrows(ControllerException.class, () -> deviceController.start(request, "device", "channel", "start", "end", "../stream"));
        verifyNoInteractions(playService, deviceService, deviceChannels, resultHolder);
    }
}
