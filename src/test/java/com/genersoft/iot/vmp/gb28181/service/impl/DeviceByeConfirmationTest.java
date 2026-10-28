package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.gb28181.bean.PendingDialogClose;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 验证设备主动BYE仅收口精确Call-ID，确认后本地清理失败也不重发停止请求。 */
class DeviceByeConfirmationTest {
    private final SipInviteSessionManager sessions = mock(SipInviteSessionManager.class);
    private final ISIPCommander commander = mock(ISIPCommander.class);
    private final DialogTerminationServiceImpl service = new DialogTerminationServiceImpl();
    private final AtomicReference<PendingDialogClose> stored = new AtomicReference<>();
    private SsrcTransaction original;

    /** 提供带原轮次令牌的关闭记录，保留实际CAS结果以验证删除顺序。 */
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "sessionManager", sessions);
        ReflectionTestUtils.setField(service, "commander", commander);
        original = new SsrcTransaction();
        original.setCallId("synthetic-old-call");
        PendingDialogClose close = new PendingDialogClose();
        close.setTransaction(original);
        close.setAttemptId("synthetic-attempt");
        close.setState(PendingDialogClose.PENDING);
        stored.set(close);
        when(sessions.getPendingDialogClose("synthetic-old-call")).thenAnswer(invocation -> stored.get());
        when(sessions.updatePendingDialogClose(eq("synthetic-old-call"), eq("synthetic-attempt"), any()))
                .thenAnswer(invocation -> {
                    stored.set(invocation.getArgument(2));
                    return true;
                });
        when(sessions.removeByCallId("synthetic-old-call", original)).thenReturn(true);
    }

    /** 交叉BYE是原Dialog的终止证据，确认旧记录不会向设备再发BYE。 */
    @Test
    void incomingByeConfirmsOriginalLedgerWithoutSending() {
        assertTrue(service.confirmDeviceBye("synthetic-old-call"));

        assertNull(stored.get());
        verify(sessions).removeByCallId("synthetic-old-call", original);
        verifyNoInteractions(commander);
    }

    /** 有界重试耗尽后仍能接收设备的明确结束通知，避免永久保留unknown影子。 */
    @Test
    void incomingByeCanResolveExhaustedUnknown() {
        stored.get().setState(PendingDialogClose.UNKNOWN);
        stored.get().setAttempts(3);

        assertTrue(service.confirmDeviceBye("synthetic-old-call"));

        assertNull(stored.get());
        verifyNoInteractions(commander);
    }

    /** 设备已确认但本地索引清理失败时保留终态，重复通知只补本地删除。 */
    @Test
    void cleanupFailureKeepsTerminalEvidenceForNextAttempt() {
        when(sessions.removeByCallId("synthetic-old-call", original)).thenReturn(false, true);

        assertTrue(service.confirmDeviceBye("synthetic-old-call"));

        assertEquals(PendingDialogClose.CONFIRMED, stored.get().getState());
        assertEquals(200, stored.get().getResponseCode());
        assertTrue(service.confirmDeviceBye("synthetic-old-call"));
        assertNull(stored.get());
        verifyNoInteractions(commander);
    }

    /** 无对应ledger时不声称已确认，活动会话仍交给其原媒体owner处理。 */
    @Test
    void unrelatedCallCannotConfirmOriginalLedger() {
        assertFalse(service.confirmDeviceBye("synthetic-other-call"));

        assertNotNull(stored.get());
        verify(sessions, never()).removeByCallId("synthetic-old-call", original);
        verifyNoInteractions(commander);
    }
}
