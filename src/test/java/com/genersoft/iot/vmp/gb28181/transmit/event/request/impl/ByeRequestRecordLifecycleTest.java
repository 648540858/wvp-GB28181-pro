package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl;

import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.service.*;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.service.IReceiveRtpServerService;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import gov.nist.javax.sip.message.SIPRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sip.RequestEvent;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Response;

import static org.mockito.Mockito.*;

/** 验证设备BYE只结束原Dialog，不能因closing影子与新会话同流而删掉新媒体。 */
class ByeRequestRecordLifecycleTest {
    private static final String CALL_ID = "synthetic-old-call";
    private static final String STREAM = "synthetic-shared-record-stream";
    private final SipInviteSessionManager sessions = mock(SipInviteSessionManager.class);
    private final IInviteStreamService invites = mock(IInviteStreamService.class);
    private final IPlayService play = mock(IPlayService.class);
    private final IDialogTerminationService termination = mock(IDialogTerminationService.class);
    private final IReceiveRtpServerService receivers = mock(IReceiveRtpServerService.class);
    private final ByeRequestProcessor processor = spy(new ByeRequestProcessor());
    private SIPRequest request;
    private SsrcTransaction original;
    private InviteInfo current;

    /** 合成完整旧Dialog及同流新Invite；兼容基线旧入口以真实观察其误清理副作用。 */
    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(processor, "sessionManager", sessions);
        ReflectionTestUtils.setField(processor, "inviteStreamService", invites);
        ReflectionTestUtils.setField(processor, "playService", play);
        ReflectionTestUtils.setField(processor, "dialogTerminationService", termination);
        ReflectionTestUtils.setField(processor, "receiveRtpServerService", receivers);
        ReflectionTestUtils.setField(processor, "sendRtpServerService", mock(ISendRtpServerService.class));
        IDeviceService devices = mock(IDeviceService.class);
        IDeviceChannelService channels = mock(IDeviceChannelService.class);
        ReflectionTestUtils.setField(processor, "deviceService", devices);
        ReflectionTestUtils.setField(processor, "deviceChannelService", channels);
        Device device = new Device();
        device.setDeviceId("synthetic-device");
        DeviceChannel channel = new DeviceChannel();
        channel.setId(7);
        channel.setDeviceId("synthetic-channel");
        when(devices.getDeviceByDeviceId("synthetic-device")).thenReturn(device);
        when(channels.getOneForSourceById(7)).thenReturn(channel);
        original = transaction(CALL_ID, "1200000001");
        when(sessions.getSsrcTransactionByCallId(CALL_ID)).thenReturn(original);
        request = mock(SIPRequest.class);
        CallIdHeader callId = mock(CallIdHeader.class);
        when(callId.getCallId()).thenReturn(CALL_ID);
        when(request.getHeader(CallIdHeader.NAME)).thenReturn(callId);
        when(request.getFromTag()).thenReturn("synthetic-device-tag");
        when(request.getToTag()).thenReturn("synthetic-platform-tag");
        doReturn(null).when(processor).responseAck(request, Response.OK);
        current = new InviteInfo();
        current.setStream(STREAM);
        current.setType(InviteSessionType.DOWNLOAD);
        current.setSsrcInfo(new SSRCInfo(10002, "1200000002", "rtp", STREAM));
        StreamInfo streamInfo = new StreamInfo();
        MediaServer media = new MediaServer();
        media.setId("synthetic-node");
        streamInfo.setMediaServer(media);
        streamInfo.setApp("rtp");
        streamInfo.setStream(STREAM);
        current.setStreamInfo(streamInfo);
        when(invites.getInviteInfoByDeviceAndChannel(InviteSessionType.DOWNLOAD, 7)).thenReturn(current);
    }

    /** 待关闭的旧Call只确认ledger；即使新Call同stream，也不能再执行媒体清理。 */
    @Test
    void pendingOldByeCannotRemoveNewSameStreamInvite() {
        when(termination.confirmDeviceBye(CALL_ID)).thenReturn(true);

        process();

        verify(termination).confirmDeviceBye(CALL_ID);
        verifyNoInteractions(play, receivers);
        verify(invites, never()).removeInviteInfo(current);
    }

    /** 未在ledger中的迟到旧Call同样必须先检查活动stream指针，不能交给当前媒体owner。 */
    @Test
    void oldByeCannotReachNewStreamOwner() {
        when(sessions.getSsrcTransactionByStream("rtp", STREAM))
                .thenReturn(transaction("synthetic-new-call", "1200000002"));

        process();

        verifyNoInteractions(play, receivers);
        verify(invites, never()).removeInviteInfo(current);
        verify(sessions).removeByCallId(CALL_ID, original);
        verify(sessions, never()).removeByCallId("synthetic-new-call");
    }

    /** 当前下载的设备EOF交原播放owner保留MP4上下文，入口不再直接删除Invite或反发BYE。 */
    @Test
    void activeDownloadByeDelegatesPreservingFileCompletion() {
        when(sessions.getSsrcTransactionByStream("rtp", STREAM)).thenReturn(original);

        process();

        verify(play).handleDeviceRecordBye(original);
        verify(invites, never()).removeInviteInfo(current);
        verifyNoInteractions(receivers);
        verify(sessions).removeByCallId(CALL_ID, original);
    }

    /** 同Call-ID但From/To标签不匹配的请求不能确认旧ledger或影响活动媒体。 */
    @Test
    void mismatchedDialogTagsCannotConfirmOrStopRecord() {
        when(request.getFromTag()).thenReturn("synthetic-unrelated-tag");

        process();

        verifyNoInteractions(termination, play, receivers);
        verify(invites, never()).removeInviteInfo(current);
        verify(sessions, never()).removeByCallId(CALL_ID);
    }

    /** 构造具有相同逻辑流但独立Call-ID与SSRC的设备事务。 */
    private SsrcTransaction transaction(String callId, String ssrc) {
        SsrcTransaction transaction = new SsrcTransaction();
        transaction.setCallId(callId);
        transaction.setDeviceId("synthetic-device");
        transaction.setChannelId(7);
        transaction.setApp("rtp");
        transaction.setStream(STREAM);
        transaction.setSsrc(ssrc);
        transaction.setType(InviteSessionType.DOWNLOAD);
        transaction.setMediaServerId("synthetic-node");
        SipTransactionInfo dialog = new SipTransactionInfo();
        dialog.setCallId(callId);
        dialog.setFromTag("synthetic-platform-tag");
        dialog.setToTag("synthetic-device-tag");
        transaction.setSipTransactionInfo(dialog);
        return transaction;
    }

    /** 在无网络的真实BYE处理入口执行请求。 */
    private void process() {
        processor.process(new RequestEvent(this, null, null, request));
    }
}
