package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.DeviceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeviceChannelServiceImplTest {

    @Test
    void addChannelShouldIgnoreOrphanGbChannel() {
        DeviceChannelMapper channelMapper = mock(DeviceChannelMapper.class);

        DeviceChannelServiceImpl service = new DeviceChannelServiceImpl();
        ReflectionTestUtils.setField(service, "channelMapper", channelMapper);
        ReflectionTestUtils.setField(service, "deviceMapper", mock(DeviceMapper.class));

        DeviceChannel channel = new DeviceChannel();
        channel.setDeviceId("34020000001320000001");
        channel.setDataDeviceId(0);

        service.addChannel(channel);

        verify(channelMapper, never()).add(any(DeviceChannel.class));
    }

    @Test
    void resetChannelsShouldCleanOrphanChannelsBeforeSave() {
        DeviceChannelMapper channelMapper = mock(DeviceChannelMapper.class);
        when(channelMapper.cleanOrphanChannelsByDeviceIds(any())).thenReturn(1);
        when(channelMapper.queryAllChannelsForRefresh(12)).thenReturn(Collections.emptyList());
        when(channelMapper.batchAdd(any())).thenReturn(1);

        DeviceChannelServiceImpl service = new DeviceChannelServiceImpl();
        ReflectionTestUtils.setField(service, "channelMapper", channelMapper);

        DeviceChannel channel = new DeviceChannel();
        channel.setDeviceId("34020000001320000001");
        channel.setDataDeviceId(12);
        channel.setStatus("ON");

        service.resetChannels(12, List.of(channel));

        verify(channelMapper).cleanOrphanChannelsByDeviceIds(eq(List.of("34020000001320000001")));
        verify(channelMapper).batchAdd(any());
    }

    @Test
    void addChannelShouldPersistWhenDeviceExists() {
        DeviceChannelMapper channelMapper = mock(DeviceChannelMapper.class);
        DeviceMapper deviceMapper = mock(DeviceMapper.class);
        when(deviceMapper.query(12)).thenReturn(new Device());

        DeviceChannelServiceImpl service = new DeviceChannelServiceImpl();
        ReflectionTestUtils.setField(service, "channelMapper", channelMapper);
        ReflectionTestUtils.setField(service, "deviceMapper", deviceMapper);

        DeviceChannel channel = new DeviceChannel();
        channel.setDeviceId("34020000001320000001");
        channel.setDataDeviceId(12);

        service.addChannel(channel);

        verify(channelMapper).add(channel);
    }
}
