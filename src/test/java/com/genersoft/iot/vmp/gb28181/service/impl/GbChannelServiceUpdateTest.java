package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GbChannelServiceUpdateTest {

    @Mock
    private CommonGBChannelMapper commonGBChannelMapper;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private GbChannelServiceImpl service;

    private CommonGBChannel channel(int id, String deviceId) {
        CommonGBChannel c = new CommonGBChannel();
        c.setGbId(id);
        c.setGbDeviceId(deviceId);
        c.setGbName("test-channel");
        return c;
    }

    @Test
    void updateWithoutChangingDeviceIdIgnoresExistingDuplicates() {
        CommonGBChannel old = channel(29, "34020000001310000001");
        CommonGBChannel patch = channel(29, "34020000001310000001");
        patch.setGbLongitude(116.3);
        patch.setGbLatitude(40.0);

        when(commonGBChannelMapper.queryById(29)).thenReturn(old, old);
        when(commonGBChannelMapper.update(patch)).thenReturn(1);

        int result = service.update(patch);

        assertEquals(1, result);
        verify(commonGBChannelMapper, never()).queryByDeviceId(anyString());
        verify(commonGBChannelMapper).update(patch);
    }

    @Test
    void updateWithChangedDeviceIdConflictThrows() {
        CommonGBChannel old = channel(29, "34020000001310000001");
        CommonGBChannel patch = channel(29, "41010500001320000031");

        when(commonGBChannelMapper.queryById(29)).thenReturn(old);
        when(commonGBChannelMapper.queryByDeviceId("41010500001320000031"))
                .thenReturn(Collections.singletonList(channel(32, "41010500001320000031")));

        ControllerException ex = assertThrows(ControllerException.class, () -> service.update(patch));
        assertTrue(ex.getMsg().contains("国标编号重复"));
        verify(commonGBChannelMapper, never()).update(any());
    }

    @Test
    void updateWithChangedDeviceIdNoConflictPasses() {
        CommonGBChannel old = channel(29, "34020000001310000001");
        CommonGBChannel patch = channel(29, "34020000001310009999");

        when(commonGBChannelMapper.queryById(29)).thenReturn(old, old);
        when(commonGBChannelMapper.queryByDeviceId("34020000001310009999")).thenReturn(Collections.emptyList());
        when(commonGBChannelMapper.update(patch)).thenReturn(1);

        assertEquals(1, service.update(patch));
    }

    @Test
    void updateWhenChannelNotExistsReturnsZero() {
        when(commonGBChannelMapper.queryById(29)).thenReturn(null);

        assertEquals(0, service.update(channel(29, "34020000001310000001")));
        verify(commonGBChannelMapper, never()).update(any());
    }
}
