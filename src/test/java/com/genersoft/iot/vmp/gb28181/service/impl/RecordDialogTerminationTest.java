package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 验证录像对话只有协议确认后才释放，重启与迟到回调不丢失原始关闭身份。 */
class RecordDialogTerminationTest {
    private final MemorySessions sessions = new MemorySessions();
    private final ISIPCommander commander = mock(ISIPCommander.class);
    private final MutableClock clock = new MutableClock();
    private final List<SipSubscribe.Event> replies = new ArrayList<>();
    private final List<Device> routes = new ArrayList<>();
    private final List<SipTransactionInfo> dialogs = new ArrayList<>();
    private DialogTerminationServiceImpl service;
    private Device device;
    private DeviceChannel channel;
    private SsrcTransaction transaction;

    /** 注入内存持久层和可推进时间，禁止用真实等待或生产设备完成回归。 */
    @BeforeEach
    void setUp() throws Exception {
        device = new Device();
        device.setDeviceId("device");
        device.setHostAddress("127.0.0.2:5060");
        device.setLocalIp("127.0.0.1");
        device.setTransport("UDP");
        channel = new DeviceChannel();
        channel.setId(17);
        channel.setDeviceId("channel-gb");
        transaction = new SsrcTransaction();
        transaction.setCallId("dialog");
        transaction.setDeviceId("device");
        transaction.setChannelId(17);
        transaction.setType(InviteSessionType.DOWNLOAD);
        transaction.setApp("rtp");
        transaction.setStream("original-stream");
        transaction.setMediaServerId("original-media");
        transaction.setSsrc("1200000001");
        SipTransactionInfo sip = new SipTransactionInfo();
        sip.setCallId("dialog");
        sip.setFromTag("original-from");
        sip.setToTag("original-to");
        sip.setViaBranch("original-branch");
        transaction.setSipTransactionInfo(sip);
        sessions.active.put("dialog", transaction);
        doAnswer(invocation -> {
            PendingDialogClose persisted = sessions.getPendingDialogClose("dialog");
            assertNotNull(persisted, "发送前必须持久化原对话");
            assertEquals(PendingDialogClose.PENDING, persisted.getState());
            routes.add(invocation.getArgument(0));
            dialogs.add(invocation.getArgument(2));
            replies.add(invocation.getArgument(3));
            assertSame(invocation.getArgument(3), invocation.getArgument(4));
            return null;
        }).when(commander).streamByeCmd(any(Device.class), eq("channel-gb"),
                any(SipTransactionInfo.class), any(SipSubscribe.Event.class), any(SipSubscribe.Event.class));
        service = restart();
    }

    /** 重建服务实例但保留持久层，模拟进程重启后恢复关闭结果。 */
    private DialogTerminationServiceImpl restart() {
        DialogTerminationServiceImpl result = new DialogTerminationServiceImpl();
        ReflectionTestUtils.setField(result, "sessionManager", sessions);
        ReflectionTestUtils.setField(result, "commander", commander);
        ReflectionTestUtils.setField(result, "clock", clock);
        ReflectionTestUtils.setField(result, "mediaServerService", mock(IMediaServerService.class));
        return result;
    }

    /** 以真实公共owner入口停止唯一录像对话。 */
    private int stop() {
        return service.stopDevice(device, channel, transaction.getType(), "original-stream");
    }

    /** 构造设备应答或协议栈超时，所有状态都经真实owner回调处理。 */
    private void respond(int index, int code) {
        SipSubscribe.EventResult<Object> result = new SipSubscribe.EventResult<>();
        result.statusCode = code;
        replies.get(index).response(result);
    }

    /** 媒体本地索引可先释放，但持久化关闭记录在设备确认前始终存在。 */
    @ParameterizedTest
    @EnumSource(value = InviteSessionType.class, names = {"DOWNLOAD", "PLAYBACK"})
    void persistsBeforeSendAndKeepsPendingAfterActiveRemoval(InviteSessionType type) {
        transaction.setType(type);
        assertEquals(0, stop());
        assertTrue(sessions.active.isEmpty());
        assertNotNull(sessions.getPendingDialogClose("dialog"));
        assertEquals(1, replies.size());
        assertEquals("original-media", sessions.getPendingDialogClose("dialog").getTransaction().getMediaServerId());
    }

    /** 成功或设备确认对话不存在后，最后一步才撤销持久化关闭状态。 */
    @ParameterizedTest
    @ValueSource(ints = {200, 481})
    void protocolTerminalRemovesPendingOnlyAfterLocalCleanup(int code) {
        stop();
        sessions.operations.clear();
        respond(0, code);
        assertNull(sessions.getPendingDialogClose("dialog"));
        assertEquals(List.of("save", "remove-active", "save-receipt", "remove-pending"), sessions.operations);
    }

    /** 鉴权失败、设备失败与超时均不能伪装为设备资源已经释放。 */
    @ParameterizedTest
    @ValueSource(ints = {401, 500, -1024})
    void uncertainResultRemainsBusyAndRetryable(int code) {
        stop();
        respond(0, code);
        PendingDialogClose pending = sessions.getPendingDialogClose("dialog");
        assertEquals(PendingDialogClose.UNKNOWN, pending.getState());
        assertEquals(code, pending.getResponseCode());
        assertTrue(sessions.active.isEmpty());
        assertFalse(service.isDeviceRecordDialogTerminated(transaction));
    }

    /** 重复stop在同一关闭轮次中不创建第二个BYE事务。 */
    @Test
    void duplicateStopDoesNotResendPendingBye() {
        stop();
        stop();
        assertEquals(1, replies.size());
        assertEquals(1, sessions.getPendingDialogClose("dialog").getAttempts());
    }

    /** 流离开入口与主动停止共享关闭身份，不能重复发送或提前清除busy状态。 */
    @Test
    void streamDepartureThenStopReusesPendingClose() {
        assertEquals(0, service.stopDeviceByStream(device, channel, "rtp", "original-stream"));
        assertTrue(sessions.active.isEmpty());
        stop();
        assertEquals(1, replies.size());
        assertNotNull(sessions.getPendingDialogClose("dialog"));
    }

    /** 未确认失败在五秒恢复间隔后重试，不需要等待在途事务的40秒窗口。 */
    @Test
    void unknownResultUsesFiveSecondRetryDelay() {
        stop();
        respond(0, 500);
        clock.advance(4_999);
        service.retryPendingRecordCloses();
        assertEquals(1, replies.size());
        clock.advance(1);
        service.retryPendingRecordCloses();
        assertEquals(2, replies.size());
        assertEquals(PendingDialogClose.PENDING, sessions.getPendingDialogClose("dialog").getState());
    }

    /** 从旧进程恢复的待确认记录40秒后重试，最多三轮并保留未知结果。 */
    @Test
    void restartRetriesAfterFortySecondsAndStopsAtThreeAttempts() {
        stop();
        service = restart();
        clock.advance(39_999);
        service.retryPendingRecordCloses();
        assertEquals(1, replies.size());
        clock.advance(1);
        service.retryPendingRecordCloses();
        assertEquals(2, replies.size());
        clock.advance(40_000);
        service.retryPendingRecordCloses();
        assertEquals(3, replies.size());
        clock.advance(40_000);
        service.retryPendingRecordCloses();
        clock.advance(120_000);
        service.retryPendingRecordCloses();
        assertEquals(3, replies.size());
        assertEquals(PendingDialogClose.UNKNOWN, sessions.getPendingDialogClose("dialog").getState());
        assertEquals(3, sessions.getPendingDialogClose("dialog").getAttempts());
    }

    /** 旧轮次的迟到200与超时都不能结束新的关闭尝试。 */
    @Test
    void lateOldResponseCannotRemoveNewAttempt() {
        stop();
        clock.advance(40_000);
        service.retryPendingRecordCloses();
        String current = sessions.getPendingDialogClose("dialog").getAttemptId();
        respond(0, 200);
        respond(0, -1024);
        assertEquals(current, sessions.getPendingDialogClose("dialog").getAttemptId());
        assertEquals(PendingDialogClose.PENDING, sessions.getPendingDialogClose("dialog").getState());
        respond(1, 200);
        assertNull(sessions.getPendingDialogClose("dialog"));
    }

    /** 设备路由变化或缓存消失后，重试继续使用原关闭路由、原节点和原SIP对话。 */
    @Test
    void retriesUsePersistedRouteAndOriginalDialogAfterRestart() {
        stop();
        device.setHostAddress("127.0.0.9:9000");
        device.setLocalIp("127.0.0.9");
        device.setTransport("TCP");
        service = restart();
        clock.advance(40_000);
        service.retryPendingRecordCloses();
        assertEquals(2, routes.size());
        assertEquals("127.0.0.2:5060", routes.get(1).getHostAddress());
        assertEquals("127.0.0.1", routes.get(1).getLocalIp());
        assertEquals("UDP", routes.get(1).getTransport());
        assertSame(dialogs.get(0), dialogs.get(1));
        assertEquals("original-media", sessions.getPendingDialogClose("dialog").getTransaction().getMediaServerId());
    }

    /** 收到200后Redis异常必须保留confirmed，恢复只补本地清理而不重发BYE。 */
    @Test
    void confirmedCleanupExceptionIsRetriedWithoutAnotherBye() {
        stop();
        sessions.failRemoval = true;
        assertThrows(IllegalStateException.class, () -> respond(0, 200));
        assertEquals(PendingDialogClose.CONFIRMED, sessions.getPendingDialogClose("dialog").getState());
        sessions.failRemoval = false;
        service = restart();
        service.retryPendingRecordCloses();
        assertNull(sessions.getPendingDialogClose("dialog"));
        assertEquals(1, replies.size());
    }

    /** 活动删除因CAS冲突返回false且仍存在时，不能丢失已确认状态。 */
    @Test
    void confirmedCleanupFalseWithActiveRecordMustKeepPendingUntilNextPass() {
        stop();
        sessions.active.put("dialog", transaction);
        sessions.refuseRemoval = true;
        respond(0, 200);
        assertNotNull(sessions.getPendingDialogClose("dialog"));
        assertEquals(PendingDialogClose.CONFIRMED, sessions.getPendingDialogClose("dialog").getState());
        sessions.refuseRemoval = false;
        service.retryPendingRecordCloses();
        assertTrue(sessions.active.isEmpty());
        assertNull(sessions.getPendingDialogClose("dialog"));
        assertEquals(1, replies.size());
    }

    /** 设备终态不可被同轮迟到超时或失败降级，清理恢复后仅补索引而不重发BYE。 */
    @ParameterizedTest
    @ValueSource(ints = {200, 481})
    void terminalResponseSurvivesLateFailureUntilLocalCleanupRecovers(int code) {
        stop();
        sessions.refuseRemoval = true;
        respond(0, code);
        String terminal = code == 200 ? PendingDialogClose.CONFIRMED : PendingDialogClose.ABSENT;
        PendingDialogClose confirmed = sessions.getPendingDialogClose("dialog");
        assertEquals(terminal, confirmed.getState());
        String attempt = confirmed.getAttemptId();
        respond(0, -1024);
        respond(0, 500);
        assertEquals(terminal, sessions.getPendingDialogClose("dialog").getState());
        assertEquals(code, sessions.getPendingDialogClose("dialog").getResponseCode());
        assertEquals(attempt, sessions.getPendingDialogClose("dialog").getAttemptId());
        clock.advance(40_000);
        service.retryPendingRecordCloses();
        assertEquals(terminal, sessions.getPendingDialogClose("dialog").getState());
        assertEquals(1, replies.size());
        sessions.refuseRemoval = false;
        sessions.operations.clear();
        service.retryPendingRecordCloses();
        assertNull(sessions.getPendingDialogClose("dialog"));
        assertTrue(sessions.active.isEmpty());
        assertEquals(List.of("remove-active", "save-receipt", "remove-pending"), sessions.operations);
        assertEquals(1, replies.size());
    }

    /** 关闭记录写入失败时不能先发BYE，也不能清除活动事务身份。 */
    @Test
    void persistenceConflictPreventsSendAndActiveRemoval() {
        sessions.refuseSave = true;
        assertEquals(1, stop());
        assertTrue(replies.isEmpty());
        assertTrue(sessions.active.containsKey("dialog"));
    }

    /** 精确结束旧节点的对话不能扫描并终止另一节点同stream的新Call。 */
    @Test
    void exactDialogClosePreservesReplacementOnAnotherNode() {
        SsrcTransaction replacement = new SsrcTransaction();
        replacement.setCallId("replacement");
        replacement.setDeviceId(transaction.getDeviceId());
        replacement.setChannelId(transaction.getChannelId());
        replacement.setApp(transaction.getApp());
        replacement.setStream(transaction.getStream());
        replacement.setType(transaction.getType());
        replacement.setMediaServerId("replacement-media");
        SipTransactionInfo sip = new SipTransactionInfo();
        sip.setCallId("replacement");
        replacement.setSipTransactionInfo(sip);
        sessions.active.put("replacement", replacement);
        assertTrue(service.stopDeviceDialog(device, channel, transaction));
        assertEquals(1, dialogs.size());
        assertEquals("dialog", dialogs.getFirst().getCallId());
        assertSame(replacement, sessions.active.get("replacement"));
        assertFalse(sessions.active.containsKey("dialog"));
        respond(0, 200);
        assertSame(replacement, sessions.active.get("replacement"));
        assertTrue(sessions.pending.isEmpty());
    }

    /** 两类Hash都空且没有凭据时仍未知，不能把本地不存在当成设备确认。 */
    @Test
    void emptyIndexesWithoutReceiptAreNotTerminationConfirmation() {
        sessions.active.clear();
        assertFalse(service.isDeviceRecordDialogTerminated(transaction));
        assertFalse(service.isDeviceRecordDialogTerminated(null));
    }

    /** 200系列与481均保存原身份凭据，pending撤销后才对分段驱动返回确认。 */
    @ParameterizedTest
    @ValueSource(ints = {200, 204, 481})
    void protocolTerminalReceiptConfirmsOnlyAfterIndexesAreClean(int code) {
        stop();
        assertFalse(service.isDeviceRecordDialogTerminated(transaction));
        respond(0, code);
        assertTrue(service.isDeviceRecordDialogTerminated(transaction));
        assertEquals(code, sessions.receipts.get("dialog").getResponseCode());
        assertTrue(sessions.getPendingDialogCloses().isEmpty());
        sessions.active.put("dialog", transaction);
        assertFalse(service.isDeviceRecordDialogTerminated(transaction));
    }

    /** 匹配设备BYE通过原owner落下确认凭据，不另发第二个BYE。 */
    @Test
    void matchingDeviceByeCreatesReceiptWithoutAnotherBye() {
        stop();
        assertTrue(service.confirmDeviceBye("dialog"));
        assertTrue(service.isDeviceRecordDialogTerminated(transaction));
        assertEquals(1, replies.size());
        assertFalse(service.confirmDeviceBye("unknown-dialog"));
    }

    /** 无pending的自然BYE必须先保存confirmed，再清理活动索引并留下原身份凭据。 */
    @Test
    void naturalDeviceByeConfirmsOriginalSnapshotWithoutSendingBye() {
        assertTrue(service.confirmDeviceRecordBye(transaction));
        assertEquals(List.of(PendingDialogClose.CONFIRMED), sessions.savedStates);
        assertEquals(List.of("save", "remove-active", "save-receipt", "remove-pending"), sessions.operations);
        assertTrue(service.isDeviceRecordDialogTerminated(transaction));
        assertTrue(sessions.getPendingDialogCloses().isEmpty());
        verifyNoInteractions(commander);
    }

    /** 自然BYE确认写入失败时保留活动身份，不能清掉最后可恢复的原始快照。 */
    @Test
    void naturalDeviceByePersistenceConflictKeepsOriginalIdentity() {
        sessions.refuseSave = true;
        assertFalse(service.confirmDeviceRecordBye(transaction));
        assertSame(transaction, sessions.active.get("dialog"));
        assertFalse(service.isDeviceRecordDialogTerminated(transaction));
        verifyNoInteractions(commander);
    }

    /** 自然BYE的receipt失败可在重启后恢复，整个补偿过程都不发送平台BYE。 */
    @Test
    void naturalDeviceByeReceiptFailureRecoversWithoutAnyBye() {
        sessions.failReceiptSave = true;
        assertThrows(IllegalStateException.class, () -> service.confirmDeviceRecordBye(transaction));
        assertEquals(PendingDialogClose.CONFIRMED, sessions.getPendingDialogClose("dialog").getState());
        assertEquals(0, sessions.getPendingDialogClose("dialog").getAttempts());
        assertFalse(service.isDeviceRecordDialogTerminated(transaction));
        sessions.failReceiptSave = false;
        service = restart();
        service.retryPendingRecordCloses();
        assertTrue(service.isDeviceRecordDialogTerminated(transaction));
        verifyNoInteractions(commander);
    }

    /** 收口期间新建的pending若与传入快照不同，不能凭相同Call-ID确认另一个Dialog。 */
    @Test
    void naturalDeviceByeRejectsMismatchedPendingSnapshot() {
        stop();
        SsrcTransaction other = new SsrcTransaction();
        org.springframework.beans.BeanUtils.copyProperties(transaction, other);
        other.setSsrc("1200000002");
        assertFalse(service.confirmDeviceRecordBye(other));
        assertEquals(PendingDialogClose.PENDING, sessions.getPendingDialogClose("dialog").getState());
        assertFalse(service.isDeviceRecordDialogTerminated(transaction));
        assertEquals(1, replies.size());
    }

    /** 历史自然BYE缺少SIP快照仍沿原语义清理，但不能取得强确认凭据。 */
    @Test
    void incompleteNaturalDeviceByeCleansLocallyWithoutStrongReceipt() {
        transaction.setSipTransactionInfo(null);
        assertTrue(service.confirmDeviceRecordBye(transaction));
        assertTrue(sessions.active.isEmpty());
        assertNull(sessions.getPendingDialogClose("dialog"));
        assertNull(sessions.getRecordDialogCloseReceipt("dialog"));
        assertFalse(service.isDeviceRecordDialogTerminated(transaction));
        verifyNoInteractions(commander);
    }

    /** 历史confirmed缺少SIP身份时重启只补清理，不能NPE、重发BYE或补造receipt。 */
    @Test
    void incompleteConfirmedPendingRecoversWithoutReceiptOrBye() {
        transaction.setSipTransactionInfo(null);
        sessions.refuseRemoval = true;
        assertTrue(service.confirmDeviceRecordBye(transaction));
        assertEquals(PendingDialogClose.CONFIRMED, sessions.getPendingDialogClose("dialog").getState());
        sessions.refuseRemoval = false;
        service = restart();
        service.retryPendingRecordCloses();
        assertTrue(sessions.active.isEmpty());
        assertNull(sessions.getPendingDialogClose("dialog"));
        assertNull(sessions.getRecordDialogCloseReceipt("dialog"));
        assertFalse(service.isDeviceRecordDialogTerminated(transaction));
        verifyNoInteractions(commander);
    }

    /** 缺失SSRC或标签的历史pending虽有协议终态，也不能被coordinator接受为强身份确认。 */
    @Test
    void incompleteIdentityTerminalDoesNotPublishReceipt() {
        stop();
        transaction.setSsrc(null);
        respond(0, 200);
        assertNull(sessions.getPendingDialogClose("dialog"));
        assertNull(sessions.getRecordDialogCloseReceipt("dialog"));
        assertFalse(service.isDeviceRecordDialogTerminated(transaction));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "deviceId,other-device", "channelId,18", "type,PLAY", "ssrc,1200000002",
            "app,other-app", "stream,other-stream", "mediaServerId,other-media",
            "callId,other-stored-call", "fromTag,other-from", "toTag,other-to",
            "viaBranch,other-branch", "sipCallId,other-call", "fromTag,''"})
    /** 同Call-ID旧凭据不能确认不同设备、流或SIP标签的新轮次，缺少身份也必须拒绝。 */
    void receiptIdentityMismatchCannotConfirmNewRound(String field, String value) {
        stop();
        respond(0, 200);
        assertTrue(service.isDeviceRecordDialogTerminated(transaction));
        if (field.equals("channelId")) {
            transaction.setChannelId(Integer.parseInt(value));
        } else if (field.equals("type")) {
            transaction.setType(InviteSessionType.valueOf(value));
        } else if (Set.of("fromTag", "toTag", "viaBranch", "sipCallId").contains(field)) {
            ReflectionTestUtils.setField(transaction.getSipTransactionInfo(),
                    field.equals("sipCallId") ? "callId" : field, value);
        } else {
            ReflectionTestUtils.setField(transaction, field, value);
        }
        assertFalse(service.isDeviceRecordDialogTerminated(transaction));
    }

    /** 凭据写入异常必须保留已确认pending，恢复只补保存和清理，不重发BYE。 */
    @ParameterizedTest
    @ValueSource(ints = {200, 481})
    void receiptPersistenceFailureRecoversWithoutAnotherBye(int code) {
        stop();
        sessions.failReceiptSave = true;
        assertThrows(IllegalStateException.class, () -> respond(0, code));
        assertNotNull(sessions.getPendingDialogClose("dialog"));
        assertNull(sessions.getRecordDialogCloseReceipt("dialog"));
        assertFalse(service.isDeviceRecordDialogTerminated(transaction));
        service = restart();
        service.retryPendingRecordCloses();
        assertEquals(1, replies.size());
        sessions.failReceiptSave = false;
        service.retryPendingRecordCloses();
        assertTrue(service.isDeviceRecordDialogTerminated(transaction));
        assertEquals(1, replies.size());
    }

    /** 写入凭据后pending清理失败仍须返回false，补偿结束后才能启动下一段。 */
    @Test
    void receiptCannotHidePendingCleanupFailure() {
        stop();
        sessions.refusePendingRemoval = true;
        respond(0, 200);
        assertNotNull(sessions.getRecordDialogCloseReceipt("dialog"));
        assertFalse(service.isDeviceRecordDialogTerminated(transaction));
        sessions.refusePendingRemoval = false;
        service.retryPendingRecordCloses();
        assertTrue(service.isDeviceRecordDialogTerminated(transaction));
        assertEquals(1, replies.size());
    }

    /** Redis读取异常必须保持未知，不能把错误降级为结束成功。 */
    @Test
    void receiptReadFailureRemainsUnknown() {
        stop();
        respond(0, 200);
        sessions.failReceiptRead = true;
        assertFalse(service.isDeviceRecordDialogTerminated(transaction));
    }

    /** 仅模拟存储协议，不替代生产关闭状态机；真实Redis合并和CAS由独立仓储用例验证。 */
    private static final class MemorySessions extends SipInviteSessionManager {
        final Map<String, SsrcTransaction> active = new LinkedHashMap<>();
        final Map<String, PendingDialogClose> pending = new LinkedHashMap<>();
        final Map<String, RecordDialogCloseReceipt> receipts = new LinkedHashMap<>();
        final List<String> operations = new ArrayList<>();
        final List<String> savedStates = new ArrayList<>();
        boolean failRemoval;
        boolean refuseRemoval;
        boolean refuseSave;
        boolean failReceiptSave;
        boolean failReceiptRead;
        boolean refusePendingRemoval;

        /** 模拟独立确认凭据的持久化，故障不能影响已确认pending的保留。 */
        @Override
        public void saveRecordDialogCloseReceipt(String callId, RecordDialogCloseReceipt receipt) {
            if (failReceiptSave) { throw new IllegalStateException("确认凭据暂不可写"); }
            operations.add("save-receipt");
            receipts.put(callId, receipt);
        }

        /** 查询仅返回已存凭据，读取失败交由服务层按未知处理。 */
        @Override
        public RecordDialogCloseReceipt getRecordDialogCloseReceipt(String callId) {
            if (failReceiptRead) { throw new IllegalStateException("确认凭据暂不可读"); }
            return receipts.get(callId);
        }

        /** 从活动和待确认事务中保留同设备的可停止对话身份。 */
        @Override
        public List<SsrcTransaction> getSsrcTransactionsByDeviceChannel(String deviceId, Integer channelId, InviteSessionType type) {
            Map<String, SsrcTransaction> merged = new LinkedHashMap<>(active);
            pending.values().forEach(close -> merged.put(close.getTransaction().getCallId(), close.getTransaction()));
            return merged.values().stream().filter(tx -> Objects.equals(deviceId, tx.getDeviceId())
                    && Objects.equals(channelId, tx.getChannelId()) && (type == null || tx.getType() == type)).toList();
        }

        /** 按流离开入口读取原对话，后续stop仍能经待确认索引找到相同身份。 */
        @Override
        public List<SsrcTransaction> getSsrcTransactionsByStream(String app, String stream) {
            return active.values().stream().filter(tx -> Objects.equals(app, tx.getApp())
                    && Objects.equals(stream, tx.getStream())).toList();
        }

        /** 活动流索引在本地释放后不再返回，但不改变独立关闭记录。 */
        @Override
        public List<SsrcTransaction> getAllStreamIndex() { return new ArrayList<>(active.values()); }

        /** 读取快照以模拟Redis反序列化，避免测试对象共享掩盖CAS错误。 */
        @Override
        public PendingDialogClose getPendingDialogClose(String callId) { return copy(pending.get(callId)); }

        /** 重启后的扫描仅读取已持久化关闭记录。 */
        @Override
        public List<PendingDialogClose> getPendingDialogCloses() { return pending.values().stream().map(MemorySessions::copy).toList(); }

        /** 比较轮次后持久化快照，模拟原子存储但不模拟业务状态转换。 */
        @Override
        public boolean updatePendingDialogClose(String callId, String expected, PendingDialogClose replacement) {
            PendingDialogClose previous = pending.get(callId);
            if (refuseSave || (replacement == null && refusePendingRemoval)
                    || !Objects.equals(expected, previous == null ? null : previous.getAttemptId())) { return false; }
            operations.add(replacement == null ? "remove-pending" : "save");
            if (replacement == null) {
                pending.remove(callId);
            } else {
                savedStates.add(replacement.getState());
                pending.put(callId, copy(replacement));
            }
            return true;
        }

        /** 模拟Redis删除异常及CAS冲突，便于验证恢复而不重发协议动作。 */
        @Override
        public boolean removeByCallId(String callId) {
            if (failRemoval) { throw new IllegalStateException("本地Redis暂不可用"); }
            operations.add("remove-active");
            return !refuseRemoval && active.remove(callId) != null;
        }

        /** 已确认快照即使活动索引已删也能幂等清理，失败则保留状态等待补偿。 */
        @Override
        public boolean removeByCallId(String callId, SsrcTransaction snapshot) {
            if (failRemoval) { throw new IllegalStateException("本地Redis暂不可用"); }
            operations.add("remove-active");
            if (refuseRemoval) { return false; }
            active.remove(callId);
            return true;
        }

        /** 为关闭owner区分真正不存在与删除失败提供权威活动查询。 */
        @Override
        public SsrcTransaction getSsrcTransactionByCallId(String callId) { return active.get(callId); }

        /** 复制持久化字段，原对话快照只读复用。 */
        private static PendingDialogClose copy(PendingDialogClose source) {
            if (source == null) { return null; }
            PendingDialogClose result = new PendingDialogClose();
            result.setTransaction(source.getTransaction()); result.setChannelDeviceId(source.getChannelDeviceId());
            result.setRemoteHost(source.getRemoteHost()); result.setLocalIp(source.getLocalIp());
            result.setTransport(source.getTransport()); result.setAttemptId(source.getAttemptId());
            result.setAttempts(source.getAttempts()); result.setUpdatedAt(source.getUpdatedAt());
            result.setState(source.getState()); result.setResponseCode(source.getResponseCode());
            return result;
        }
    }

    /** 以可控绝对时间验证重启与重试边界，测试无需真实等待40秒。 */
    private static final class MutableClock extends Clock {
        private long millis = 1_000_000;
        /** 推进状态机观察到的时间。 */
        void advance(long amount) { millis += amount; }
        /** 统一使用UTC，不引入时区换算。 */
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        /** 本测试时钟的时间值不依赖时区。 */
        @Override public Clock withZone(ZoneId zone) { return this; }
        /** 提供由测试推进的当前时刻。 */
        @Override public Instant instant() { return Instant.ofEpochMilli(millis); }
        /** 直接返回测试控制的绝对毫秒。 */
        @Override public long millis() { return millis; }
    }
}
