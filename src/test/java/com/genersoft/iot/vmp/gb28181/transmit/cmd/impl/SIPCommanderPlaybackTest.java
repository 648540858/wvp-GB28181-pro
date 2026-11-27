package com.genersoft.iot.vmp.gb28181.transmit.cmd.impl;

import com.genersoft.iot.vmp.common.enums.MediaStreamUtil;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.SIPSender;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.SIPRequestHeaderProvider;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.message.SIPResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sip.ResponseEvent;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Request;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SIPCommanderPlaybackTest {

    private static final String LOCAL_IP = "127.0.0.1";
    private static final String STREAM = "synthetic-playback-stream";
    private static final String START = "2026-10-20 10:00:00";
    private static final String END = "2026-10-20 10:01:00";

    @Mock
    private SipLayer sipLayer;
    @Mock
    private SIPSender sipSender;
    @Mock
    private SIPRequestHeaderProvider headerProvider;
    @Mock
    private SipInviteSessionManager sessionManager;
    @Mock
    private UserSetting userSetting;
    @Mock
    private SipSubscribe.Event okEvent;
    @Mock
    private SipSubscribe.Event errorEvent;
    @Mock
    private CallIdHeader requestCallId;
    @Mock
    private CallIdHeader responseCallId;
    @Mock
    private Request inviteRequest;
    @Mock
    private Request byeRequest;
    @Mock
    private SIPResponse sipResponse;
    @InjectMocks
    private SIPCommander commander;

    private Device device;
    private DeviceChannel channel;
    private MediaServer mediaServer;
    private SSRCInfo ssrcInfo;

    // 隔离网络请求，只使用真实 Commander 的响应包装逻辑观察注册前的副作用。
    @BeforeEach
    void setUp() throws Exception {
        device = new Device();
        device.setDeviceId("synthetic-device");
        device.setLocalIp(LOCAL_IP);
        device.setTransport("UDP");
        device.setStreamMode("UDP");
        channel = new DeviceChannel();
        channel.setId(27);
        channel.setDeviceId("synthetic-channel");
        mediaServer = new MediaServer();
        mediaServer.setId("synthetic-media");
        mediaServer.setSdpIp(LOCAL_IP);
        ssrcInfo = new SSRCInfo(30000, "1234567890", MediaStreamUtil.RTP_APP, STREAM);
        when(sipLayer.getLocalIp(LOCAL_IP)).thenReturn(LOCAL_IP);
        when(sipSender.getNewCallIdHeader(LOCAL_IP, "UDP")).thenReturn(requestCallId);
        when(userSetting.getSeniorSdp()).thenReturn(false);
        when(headerProvider.createPlaybackInviteRequest(eq(device), eq(channel.getDeviceId()),
                anyString(), anyString(), anyString(), isNull(), eq(requestCallId), eq(ssrcInfo.getSsrc())))
                .thenReturn(inviteRequest);
    }

    // A 迟到的 200 被拒绝时，只终止 A 自身，不能读取 B 的流索引、向 B 发 BYE 或写入旧 A。
    @Test
    void playbackResponse_rejectedSessionGuard_doesNotReplaceNewDialog() throws Exception {
        when(headerProvider.createByteRequest(eq(device), eq(channel.getDeviceId()), any(SipTransactionInfo.class)))
                .thenReturn(byeRequest);
        SipSubscribe.Event response = sendPlayback(task -> false);
        SipSubscribe.EventResult<ResponseEvent> oldResponse = playbackResponse();

        response.response(oldResponse);

        verifyNoInteractions(sessionManager, okEvent);
        ArgumentCaptor<SipTransactionInfo> terminated = ArgumentCaptor.forClass(SipTransactionInfo.class);
        verify(headerProvider).createByteRequest(eq(device), eq(channel.getDeviceId()), terminated.capture());
        assertEquals("synthetic-old-call", terminated.getValue().getCallId(),
                "清理必须使用旧响应自带的 Call-ID，不能按同名流查找新事务");
        verify(sipSender).transmitRequest(eq(LOCAL_IP), eq(byeRequest), isNull(), isNull());
    }

    // 当前请求通过门禁后，在门禁临界区内注册事务再通知成功，保持正常回放建立能力。
    @Test
    void playbackResponse_acceptedSessionGuard_registersCurrentDialog() throws Exception {
        SipSubscribe.Event response = sendPlayback(task -> {
            task.run();
            return true;
        });
        SipSubscribe.EventResult<ResponseEvent> currentResponse = playbackResponse();

        response.response(currentResponse);

        ArgumentCaptor<SsrcTransaction> transaction = ArgumentCaptor.forClass(SsrcTransaction.class);
        verify(sessionManager).put(transaction.capture());
        assertEquals("synthetic-old-call", transaction.getValue().getCallId());
        assertEquals(STREAM, transaction.getValue().getStream());
        assertEquals(ssrcInfo.getSsrc(), transaction.getValue().getSsrc());
        verify(okEvent).response(currentResponse);
    }

    // 捕获真实 SIPSender 成功入口，避免直接调用 Play 层回调而遗漏 Commander 的注册副作用。
    private SipSubscribe.Event sendPlayback(Predicate<Runnable> sessionGuard) throws Exception {
        commander.playbackStreamCmd(mediaServer, ssrcInfo, device, channel, START, END,
                okEvent, errorEvent, 1000L, sessionGuard);
        ArgumentCaptor<SipSubscribe.Event> response = ArgumentCaptor.forClass(SipSubscribe.Event.class);
        verify(sipSender).transmitRequest(eq(LOCAL_IP), eq(inviteRequest), eq(errorEvent), response.capture(), eq(1000L));
        return response.getValue();
    }

    // 构造带完整旧 Dialog 身份的 200 响应，不连接真实 SIP 设备。
    private SipSubscribe.EventResult<ResponseEvent> playbackResponse() throws Exception {
        when(responseCallId.getCallId()).thenReturn("synthetic-old-call");
        when(sipResponse.getCallIdHeader()).thenReturn(responseCallId);
        when(sipResponse.getFromTag()).thenReturn("synthetic-from");
        when(sipResponse.getToTag()).thenReturn("synthetic-to");
        Via via = new Via();
        via.setBranch("z9hG4bK-synthetic-old");
        when(sipResponse.getTopmostViaHeader()).thenReturn(via);
        SipSubscribe.EventResult<ResponseEvent> result = new SipSubscribe.EventResult<>();
        result.statusCode = 200;
        result.event = new ResponseEvent(this, null, null, sipResponse);
        return result;
    }
}
