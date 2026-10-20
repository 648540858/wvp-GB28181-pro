package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DialogTerminationServiceImplTest {

    @Mock
    private SipInviteSessionManager sessionManager;

    @Mock
    private ISIPCommander commander;

    @Mock
    private ISIPCommanderForPlatform commanderForPlatform;

    @Mock
    private AudioBroadcastManager audioBroadcastManager;

    @InjectMocks
    private DialogTerminationServiceImpl service;

    private Device device;
    private DeviceChannel channel;

    @BeforeEach
    void setUp() {
        device = new Device();
        device.setDeviceId("device-1");

        channel = new DeviceChannel();
        channel.setId(1);
        channel.setDeviceId("channel-1");

        when(sessionManager.getAllStreamIndex()).thenReturn(Collections.emptyList());
        when(sessionManager.removeByCallId(anyString())).thenReturn(true);
    }

    @Test
    void stopDevice_shouldSendByeForAllDialogsAndRemoveThem() throws Exception {
        SsrcTransaction first = transaction("call-1", "ssrc-1", InviteSessionType.PLAY);
        SsrcTransaction second = transaction("call-2", "ssrc-2", InviteSessionType.PLAY);
        when(sessionManager.getSsrcTransactionsByDeviceChannel(
                "device-1", 1, InviteSessionType.PLAY))
                .thenReturn(List.of(first, second));

        int failed = service.stopDevice(device, channel, InviteSessionType.PLAY, null);

        assertEquals(0, failed);
        verify(commander, times(2)).streamByeCmd(any(Device.class), anyString(),
                any(SipTransactionInfo.class), isNull());
        verify(sessionManager, times(2)).removeByCallId(anyString());
    }

    @Test
    void stopDevice_shouldDeduplicateSameDialogStoredTwice() throws Exception {
        SsrcTransaction first = transaction("talk", "ssrc-1", InviteSessionType.TALK);
        SsrcTransaction second = transaction("actual-call", "ssrc-1", InviteSessionType.TALK);
        second.getSipTransactionInfo().setCallId("talk");
        when(sessionManager.getSsrcTransactionsByDeviceChannel(
                "device-1", 1, InviteSessionType.TALK))
                .thenReturn(List.of(first, second));

        service.stopDevice(device, channel, InviteSessionType.TALK, null);

        verify(commander, times(1)).streamByeCmd(any(Device.class), anyString(),
                any(SipTransactionInfo.class), isNull());
        verify(sessionManager, times(2)).removeByCallId(anyString());
    }

    @Test
    void stopDevice_shouldUseDeviceInviteByeForBroadcast() throws Exception {
        SsrcTransaction broadcast = transaction("broadcast-call", "ssrc-1",
                InviteSessionType.BROADCAST);
        when(sessionManager.getSsrcTransactionsByDeviceChannel(
                "device-1", 1, InviteSessionType.BROADCAST))
                .thenReturn(List.of(broadcast));

        service.stopDevice(device, channel, InviteSessionType.BROADCAST, null);

        verify(commander, never()).streamByeCmd(any(Device.class), anyString(),
                any(SipTransactionInfo.class), any());
        verify(commander, times(1)).streamByeCmdForDeviceInvite(
                any(Device.class), anyString(), any(SipTransactionInfo.class), isNull());
    }

    private SsrcTransaction transaction(String callId, String ssrc, InviteSessionType type) {
        SsrcTransaction transaction = new SsrcTransaction();
        transaction.setDeviceId("device-1");
        transaction.setChannelId(1);
        transaction.setCallId(callId);
        transaction.setApp("rtp");
        transaction.setStream("stream-1");
        transaction.setSsrc(ssrc);
        transaction.setType(type);
        SipTransactionInfo sipTransactionInfo = new SipTransactionInfo();
        sipTransactionInfo.setCallId(callId);
        transaction.setSipTransactionInfo(sipTransactionInfo);
        return transaction;
    }
}
