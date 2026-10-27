package com.genersoft.iot.vmp.gb28181.transmit.cmd.impl;

import com.genersoft.iot.vmp.gb28181.SipLayer;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.SIPSender;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.SIPRequestHeaderProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sip.SipException;
import javax.sip.message.Request;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 验证兼容的按流停止入口只有在设备确认协议终态后才释放原始Dialog身份。
@ExtendWith(MockitoExtension.class)
class SIPCommanderReliableByeTest {
    private static final String LOCAL_IP = "127.0.0.1";
    private static final String CALL_ID = "synthetic-call";
    @Mock
    private SipLayer sipLayer;
    @Mock
    private SIPSender sipSender;
    @Mock
    private SIPRequestHeaderProvider headerProvider;
    @Mock
    private SipInviteSessionManager sessionManager;
    @Mock
    private Request bye;
    @Mock
    private SipSubscribe.Event okEvent;
    @Mock
    private SipSubscribe.Event errorEvent;
    @InjectMocks
    private SIPCommander commander;
    private Device device;
    private SipTransactionInfo dialog;
    private SsrcTransaction storedTransaction;

    // 保留精确事务与BYE请求，模拟网络发送返回但设备尚未确认的阶段。
    @BeforeEach
    void setUp() throws Exception {
        device = new Device();
        device.setDeviceId("synthetic-device");
        device.setLocalIp(LOCAL_IP);
        dialog = new SipTransactionInfo();
        dialog.setCallId(CALL_ID);
        storedTransaction = new SsrcTransaction();
        storedTransaction.setCallId(CALL_ID);
        storedTransaction.setSipTransactionInfo(dialog);
        when(sipLayer.getLocalIp(LOCAL_IP)).thenReturn(LOCAL_IP);
    }

    // 本地send返回不是设备确认，原事务必须留给超时后的再次停止。
    @Test
    void legacyByeKeepsDialogUntilConfirmed() throws Exception {
        sendLegacyBye();

        verify(sessionManager, never()).removeByCallId(CALL_ID);
    }

    // 对端200确认后再删除事务并通知兼容的成功回调。
    @Test
    void legacyByeDeletesDialogAfterSuccessResponse() throws Exception {
        sendLegacyBye();
        ArgumentCaptor<SipSubscribe.Event> success = ArgumentCaptor.forClass(SipSubscribe.Event.class);
        ArgumentCaptor<SipSubscribe.Event> failure = ArgumentCaptor.forClass(SipSubscribe.Event.class);
        verify(sipSender).transmitRequest(eq(LOCAL_IP), eq(bye), failure.capture(), success.capture());
        SipSubscribe.EventResult<Object> response = response(200);

        success.getValue().response(response);

        verify(sessionManager).removeByCallId(CALL_ID);
        verify(okEvent).response(response);
    }

    // 481明确表示对端已无此Dialog，可终结本地身份；不能忽略失败响应入口。
    @Test
    void legacyByeTreats481AsConfirmedTermination() throws Exception {
        sendLegacyBye();
        ArgumentCaptor<SipSubscribe.Event> success = ArgumentCaptor.forClass(SipSubscribe.Event.class);
        ArgumentCaptor<SipSubscribe.Event> failure = ArgumentCaptor.forClass(SipSubscribe.Event.class);
        verify(sipSender).transmitRequest(eq(LOCAL_IP), eq(bye), failure.capture(), success.capture());
        assertNotNull(failure.getValue());
        SipSubscribe.EventResult<Object> response = response(481);

        failure.getValue().response(response);

        verify(sessionManager).removeByCallId(CALL_ID);
        verify(okEvent).response(response);
    }

    // 超时或服务器失败仍不能证明对端停流，必须保留原事务供上层恢复。
    @Test
    void legacyByeRetainsDialogAfterTimeout() throws Exception {
        sendLegacyBye();
        ArgumentCaptor<SipSubscribe.Event> success = ArgumentCaptor.forClass(SipSubscribe.Event.class);
        ArgumentCaptor<SipSubscribe.Event> failure = ArgumentCaptor.forClass(SipSubscribe.Event.class);
        verify(sipSender).transmitRequest(eq(LOCAL_IP), eq(bye), failure.capture(), success.capture());
        assertNotNull(failure.getValue());

        failure.getValue().response(response(-1024));

        verify(sessionManager, never()).removeByCallId(CALL_ID);
        verify(okEvent, never()).response(org.mockito.ArgumentMatchers.any());
    }

    // 标准Dialog的新重载必须保留独立确认和失败回调，不能在转交时互换或吞掉失败。
    @Test
    void exactDialogByeForwardsSuccessAndFailure() throws Exception {
        preparePlatformBye();

        commander.streamByeCmd(device, "synthetic-channel", dialog, okEvent, errorEvent);

        verify(sipSender).transmitRequest(LOCAL_IP, bye, errorEvent, okEvent);
        verify(sessionManager, never()).removeByCallId(CALL_ID);
    }

    // 设备发起的Dialog仍使用反向请求构造器，同时提供失败确认。
    @Test
    void deviceInvitedByePreservesDirectionAndBothCallbacks() throws Exception {
        when(headerProvider.createByteRequestForDeviceInvite(device, "synthetic-channel", dialog)).thenReturn(bye);

        commander.streamByeCmdForDeviceInvite(device, "synthetic-channel", dialog, okEvent, errorEvent);

        verify(sipSender).transmitRequest(LOCAL_IP, bye, errorEvent, okEvent);
        verify(headerProvider, never()).createByteRequest(device, "synthetic-channel", dialog);
    }

    // 绑定新Dialog时旧Call-ID仍保留到确认，删除旧身份不应误删新事务。
    @Test
    void registeringNewDialogRetainsOldUntilConfirmed() throws Exception {
        preparePlatformBye();
        SsrcTransaction replacement = prepareReplacement(InviteSessionType.PLAYBACK);

        ReflectionTestUtils.invokeMethod(commander, "registerTransaction", device, channel(), replacement);

        verify(sessionManager).put(replacement);
        verify(sessionManager, never()).removeByCallId(CALL_ID);
        ArgumentCaptor<SipSubscribe.Event> success = ArgumentCaptor.forClass(SipSubscribe.Event.class);
        ArgumentCaptor<SipSubscribe.Event> failure = ArgumentCaptor.forClass(SipSubscribe.Event.class);
        verify(sipSender).transmitRequest(eq(LOCAL_IP), eq(bye), failure.capture(), success.capture());
        failure.getValue().response(response(500));
        verify(sessionManager, never()).removeByCallId(CALL_ID);

        success.getValue().response(response(200));

        verify(sessionManager).removeByCallId(CALL_ID);
        verify(sessionManager, never()).removeByCallId("synthetic-new-call");
    }

    // 广播旧Dialog的481也按原方向确认终止，只删除原Call-ID。
    @Test
    void registeringBroadcastDialogConfirmsOldOn481() throws Exception {
        when(headerProvider.createByteRequestForDeviceInvite(device, "synthetic-channel", dialog)).thenReturn(bye);
        SsrcTransaction replacement = prepareReplacement(InviteSessionType.BROADCAST);

        ReflectionTestUtils.invokeMethod(commander, "registerTransaction", device, channel(), replacement);

        ArgumentCaptor<SipSubscribe.Event> success = ArgumentCaptor.forClass(SipSubscribe.Event.class);
        ArgumentCaptor<SipSubscribe.Event> failure = ArgumentCaptor.forClass(SipSubscribe.Event.class);
        verify(sipSender).transmitRequest(eq(LOCAL_IP), eq(bye), failure.capture(), success.capture());
        verify(sessionManager, never()).removeByCallId(CALL_ID);

        failure.getValue().response(response(481));

        verify(sessionManager).removeByCallId(CALL_ID);
        verify(sessionManager).put(replacement);
        verify(sessionManager, never()).removeByCallId("synthetic-new-call");
    }

    // 旧Dialog的BYE发送抛错时仍保存旧Call-ID，不能把同步失败当作停止确认。
    @Test
    void registeringNewDialogRetainsOldAfterSendFailure() throws Exception {
        preparePlatformBye();
        SsrcTransaction replacement = prepareReplacement(InviteSessionType.PLAYBACK);
        doThrow(new SipException("synthetic failure")).when(sipSender)
                .transmitRequest(eq(LOCAL_IP), eq(bye), any(), any());

        ReflectionTestUtils.invokeMethod(commander, "registerTransaction", device, channel(), replacement);

        verify(sessionManager, never()).removeByCallId(CALL_ID);
        verify(sessionManager).put(replacement);
    }

    // 为旧按流入口提供真实事务查找，避免其他新入口测试携带无关桩。
    private void sendLegacyBye() throws Exception {
        preparePlatformBye();
        when(sessionManager.getSsrcTransactionByCallId(CALL_ID)).thenReturn(storedTransaction);
        commander.streamByeCmd(device, "synthetic-channel", "rtp", "synthetic-stream", CALL_ID, okEvent);
    }

    // 标准方向的BYE请求由原请求构造器生成。
    private void preparePlatformBye() throws Exception {
        when(headerProvider.createByteRequest(device, "synthetic-channel", dialog)).thenReturn(bye);
    }

    // 构造不同Call-ID但同一逻辑流的替换请求，旧事务仍保留在独立Call-ID索引中。
    private SsrcTransaction prepareReplacement(InviteSessionType type) {
        storedTransaction.setType(type);
        storedTransaction.setApp("rtp");
        storedTransaction.setStream("synthetic-stream");
        when(sessionManager.getSsrcTransactionByStream("rtp", "synthetic-stream")).thenReturn(storedTransaction);
        SsrcTransaction replacement = new SsrcTransaction();
        replacement.setApp("rtp");
        replacement.setStream("synthetic-stream");
        replacement.setCallId("synthetic-new-call");
        replacement.setType(type);
        return replacement;
    }

    // 提供仅用于构造BYE目标的合成通道。
    private DeviceChannel channel() {
        DeviceChannel channel = new DeviceChannel();
        channel.setDeviceId("synthetic-channel");
        return channel;
    }

    // 构造协议结果而不伪造媒体成功，用于确认事务清理的边界。
    private SipSubscribe.EventResult<Object> response(int status) {
        SipSubscribe.EventResult<Object> result = new SipSubscribe.EventResult<>();
        result.statusCode = status;
        return result;
    }
}
