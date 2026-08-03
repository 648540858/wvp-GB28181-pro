package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GbChannelServiceImplTest {

    @Mock
    private CommonGBChannelMapper commonGBChannelMapper;

    @InjectMocks
    private GbChannelServiceImpl service;

    @Test
    void clearChannelParentShouldSkipUpdateWhenAllQueryIsEmpty() {
        when(commonGBChannelMapper.queryAllForUnusualParent()).thenReturn(List.of());

        service.clearChannelParent(true, null);

        verify(commonGBChannelMapper).queryAllForUnusualParent();
        verify(commonGBChannelMapper, never()).removeParentIdByChannelIds(anyList());
    }

    @Test
    void clearChannelParentShouldSkipUpdateWhenIdsAreEmptyOrNull() {
        service.clearChannelParent(false, List.of());
        service.clearChannelParent(false, null);

        verifyNoInteractions(commonGBChannelMapper);
    }

    @Test
    void clearChannelParentShouldUpdateWhenIdsArePresent() {
        List<Integer> channelIds = List.of(1, 2);

        service.clearChannelParent(false, channelIds);

        verify(commonGBChannelMapper).removeParentIdByChannelIds(channelIds);
    }

    @Test
    void clearChannelCivilCodeShouldSkipUpdateWhenAllQueryIsEmpty() {
        when(commonGBChannelMapper.queryAllForUnusualCivilCode()).thenReturn(List.of());

        service.clearChannelCivilCode(true, null);

        verify(commonGBChannelMapper).queryAllForUnusualCivilCode();
        verify(commonGBChannelMapper, never()).removeCivilCodeByChannelIds(anyList());
    }

    @Test
    void clearChannelCivilCodeShouldSkipUpdateWhenIdsAreEmptyOrNull() {
        service.clearChannelCivilCode(false, List.of());
        service.clearChannelCivilCode(false, null);

        verifyNoInteractions(commonGBChannelMapper);
    }

    @Test
    void clearChannelCivilCodeShouldUpdateWhenIdsArePresent() {
        List<Integer> channelIds = List.of(1, 2);

        service.clearChannelCivilCode(false, channelIds);

        verify(commonGBChannelMapper).removeCivilCodeByChannelIds(channelIds);
    }
}
