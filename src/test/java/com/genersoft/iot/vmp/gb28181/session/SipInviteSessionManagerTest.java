package com.genersoft.iot.vmp.gb28181.session;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import com.genersoft.iot.vmp.gb28181.bean.RecordDialogCloseReceipt;
import com.genersoft.iot.vmp.gb28181.bean.PendingDialogClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;

/**
 * 覆盖 #2216 的第 3 条根因：清理旧 Call-ID 时不得删掉已被新会话覆盖的 stream 索引。
 */
@ExtendWith(MockitoExtension.class)
class SipInviteSessionManagerTest {

    private static final String SERVER_ID = "test-server";
    private static final String APP = "rtp";
    private static final String STREAM = "34020000001320000001_34020000001320000001";
    private static final String OLD_CALL_ID = "old-call-id";
    private static final String NEW_CALL_ID = "new-call-id";

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private RedisOperations<String, Object> redisOperations;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    /** 用内存 Map 模拟 Redis Hash，便于断言键是否真的被删除 */
    private final Map<String, Map<Object, Object>> store = new HashMap<>();

    private SipInviteSessionManager sessionManager;
    private final List<Runnable> queued = new ArrayList<>();
    private final List<Object> commandResults = new ArrayList<>();
    private boolean transactionOpen;
    private int emptyAborts;

    /** 初始化隔离的Hash与独立Value存储，避免确认凭据混入原活动列表。 */
    @BeforeEach
    void setUp() {
        UserSetting userSetting = new UserSetting();
        userSetting.setServerId(SERVER_ID);

        sessionManager = new SipInviteSessionManager();
        ReflectionTestUtils.setField(sessionManager, "userSetting", userSetting);
        ReflectionTestUtils.setField(sessionManager, "redisTemplate", redisTemplate);

        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(hashOperations.get(anyString(), any())).thenAnswer(
                invocation -> bucket(invocation.getArgument(0)).get(invocation.getArgument(1)));
        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);Object field = invocation.getArgument(1);Object value = invocation.getArgument(2);
            mutate(() -> bucket(key).put(field, value), Boolean.TRUE);
            return null;
        }).when(hashOperations).put(anyString(), any(), any());
        lenient().when(hashOperations.delete(anyString(), any())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);Object field = invocation.getArgument(1);
            mutate(() -> bucket(key).remove(field), 1L);
            return 1L;
        });
        lenient().when(hashOperations.values(anyString())).thenAnswer(invocation ->
                new ArrayList<>(bucket(invocation.getArgument(0)).values()));

        lenient().when(redisOperations.opsForHash()).thenReturn(hashOperations);
        lenient().when(redisOperations.exec()).thenAnswer(invocation -> {
            assertTrue(transactionOpen, "EXEC必须在MULTI之后执行");
            transactionOpen = false;
            if (emptyAborts > 0) {
                emptyAborts--;queued.clear();commandResults.clear();
                return Collections.emptyList();
            }
            queued.forEach(Runnable::run);
            List<Object> result = new ArrayList<>(commandResults);
            queued.clear();commandResults.clear();
            return result;
        });
        lenient().doAnswer(invocation -> {
            transactionOpen = true;queued.clear();commandResults.clear();return null;
        }).when(redisOperations).multi();
        lenient().doNothing().when(redisOperations).watch(anyString());
        lenient().doNothing().when(redisOperations).unwatch();
        lenient().when(redisTemplate.execute(any(SessionCallback.class))).thenAnswer(invocation -> {
            SessionCallback<?> callback = invocation.getArgument(0);
            return callback.execute(redisOperations);
        });
    }

    /** MULTI内只排队，真实提交才修改Hash，并为每条命令保留一个驱动回复。 */
    private void mutate(Runnable mutation, Object reply) {
        if (transactionOpen) { queued.add(mutation);commandResults.add(reply); }
        else mutation.run();
    }

    /** 返回当前测试节点的精确Hash，不访问外部Redis命名空间。 */
    private Map<Object, Object> bucket(String key) {
        return store.computeIfAbsent(key, k -> new HashMap<>());
    }

    /** 构造当前测试节点的流索引键。 */
    private String streamKey() {
        return VideoManagerConstants.SIP_INVITE_SESSION_STREAM + SERVER_ID;
    }

    /** 构造当前测试节点的原Dialog索引键。 */
    private String callIdKey() {
        return VideoManagerConstants.SIP_INVITE_SESSION_CALL_ID + SERVER_ID;
    }

    /** 构造固定逻辑流的事务身份，便于验证不同Call-ID之间的条件清理。 */
    private static SsrcTransaction transaction(String callId) {
        SsrcTransaction transaction = new SsrcTransaction();
        transaction.setApp(APP);
        transaction.setStream(STREAM);
        transaction.setCallId(callId);
        return transaction;
    }


    /** 空EXEC意味着WATCH冲突，不能假报原Call-ID和stream双索引已经删除。 */
    @Test
    void emptyExecCannotReportCallIdRemovalAsCommitted() {
        sessionManager.put(transaction(OLD_CALL_ID));
        emptyAborts = 3;
        assertFalse(sessionManager.removeByCallId(OLD_CALL_ID));
        assertNotNull(sessionManager.getSsrcTransactionByCallId(OLD_CALL_ID));
        assertNotNull(sessionManager.getSsrcTransactionByStream(APP, STREAM));
        assertTrue(sessionManager.removeByCallId(OLD_CALL_ID));
        assertNull(sessionManager.getSsrcTransactionByCallId(OLD_CALL_ID));
        assertNull(sessionManager.getSsrcTransactionByStream(APP, STREAM));
    }

    /** 两种条件删除都必须核实两条命令真实提交，暂时冲突可以重试但耗尽不能清掉身份。 */
    @Test
    void emptyExecCannotReportConditionalCallOrSsrcRemovalAsCommitted() {
        SsrcTransaction original = transaction(OLD_CALL_ID);original.setSsrc("1234567890");
        sessionManager.put(original);
        emptyAborts = 3;
        assertFalse(sessionManager.removeByStreamIfCallId(APP, STREAM, OLD_CALL_ID));
        assertNotNull(sessionManager.getSsrcTransactionByCallId(OLD_CALL_ID));
        assertNotNull(sessionManager.getSsrcTransactionByStream(APP, STREAM));
        emptyAborts = 3;
        assertFalse(sessionManager.removeByStreamIfSsrc(APP, STREAM, original.getSsrc()));
        assertNotNull(sessionManager.getSsrcTransactionByCallId(OLD_CALL_ID));
        assertNotNull(sessionManager.getSsrcTransactionByStream(APP, STREAM));
        assertTrue(sessionManager.removeByStreamIfSsrc(APP, STREAM, original.getSsrc()));
        assertNull(sessionManager.getSsrcTransactionByCallId(OLD_CALL_ID));
        sessionManager.put(original);
        emptyAborts = 1;
        assertTrue(sessionManager.removeByStreamIfCallId(APP, STREAM, OLD_CALL_ID));
        assertNull(sessionManager.getSsrcTransactionByStream(APP, STREAM));
    }

    /** pending关闭记录的创建、替换和删除都不能把空事务回复当成已提交。 */
    @Test
    void emptyExecCannotReportPendingCloseUpdateAsCommitted() {
        PendingDialogClose initial = new PendingDialogClose();
        initial.setAttemptId("initial");initial.setState(PendingDialogClose.PENDING);initial.setTransaction(transaction(OLD_CALL_ID));
        emptyAborts = 3;
        assertFalse(sessionManager.updatePendingDialogClose(OLD_CALL_ID, null, initial));
        assertNull(sessionManager.getPendingDialogClose(OLD_CALL_ID));
        assertTrue(sessionManager.updatePendingDialogClose(OLD_CALL_ID, null, initial));
        PendingDialogClose replacement = new PendingDialogClose();
        replacement.setAttemptId("replacement");replacement.setState(PendingDialogClose.CONFIRMED);
        emptyAborts = 3;
        assertFalse(sessionManager.updatePendingDialogClose(OLD_CALL_ID, "initial", replacement));
        assertEquals("initial", sessionManager.getPendingDialogClose(OLD_CALL_ID).getAttemptId());
        emptyAborts = 3;
        assertFalse(sessionManager.updatePendingDialogClose(OLD_CALL_ID, "initial", null));
        assertEquals("initial", sessionManager.getPendingDialogClose(OLD_CALL_ID).getAttemptId());
        assertTrue(sessionManager.updatePendingDialogClose(OLD_CALL_ID, "initial", null));
        assertNull(sessionManager.getPendingDialogClose(OLD_CALL_ID));
    }

    /** 确认凭据原子写入带30分钟TTL的独立实例键，不能留在getAll或pending列表中。 */
    @Test
    void recordCloseReceiptHasBoundedTtlAndDoesNotEnterSessionIndexes() {
        RecordDialogCloseReceipt receipt = new RecordDialogCloseReceipt();
        receipt.setCallId(OLD_CALL_ID);
        sessionManager.saveRecordDialogCloseReceipt(OLD_CALL_ID, receipt);
        verify(valueOperations).set(eq(callIdKey() + ":RECORD_CLOSED:" + OLD_CALL_ID),
                same(receipt), eq(Duration.ofMinutes(30)));
        assertTrue(sessionManager.getAll().isEmpty());
        assertTrue(sessionManager.getPendingDialogCloses().isEmpty());
    }

    /** 已过期、未知或不兼容缓存值均视为没有凭据，不根据空Hash补造确认。 */
    @Test
    void recordCloseReceiptReadTreatsMissingAndWrongTypesAsUnknown() {
        RecordDialogCloseReceipt receipt = new RecordDialogCloseReceipt();
        String key = callIdKey() + ":RECORD_CLOSED:" + OLD_CALL_ID;
        when(valueOperations.get(key)).thenReturn(receipt, null, "unknown");
        assertSame(receipt, sessionManager.getRecordDialogCloseReceipt(OLD_CALL_ID));
        assertNull(sessionManager.getRecordDialogCloseReceipt(OLD_CALL_ID));
        assertNull(sessionManager.getRecordDialogCloseReceipt(OLD_CALL_ID));
        assertNull(sessionManager.getRecordDialogCloseReceipt(null));
    }

    /** 写失败必须上抛，使终止owner保留confirmed pending，而非先删除最后确认依据。 */
    @Test
    void recordCloseReceiptWriteFailureIsNotSilentlyIgnored() {
        RecordDialogCloseReceipt receipt = new RecordDialogCloseReceipt();
        doThrow(new IllegalStateException("Redis不可用")).when(valueOperations)
                .set(anyString(), same(receipt), any(Duration.class));
        assertThrows(IllegalStateException.class,
                () -> sessionManager.saveRecordDialogCloseReceipt(OLD_CALL_ID, receipt));
    }

    @Test
    void removeByCallId_shouldKeepStreamIndexOwnedByNewerTransaction() {
        // 第一次点播，写入旧事务
        sessionManager.put(transaction(OLD_CALL_ID));
        // 同一通道第二次点播，stream 索引被新事务覆盖（Call-ID 索引仍然保留两个事务）
        sessionManager.put(transaction(NEW_CALL_ID));
        assertEquals(NEW_CALL_ID, sessionManager.getSsrcTransactionByStream(APP, STREAM).getCallId());

        // 清理旧的 Call-ID
        sessionManager.removeByCallId(OLD_CALL_ID);

        // 旧事务被删除，但新事务的 stream 索引必须保留，否则后续 stop 无法发送 BYE
        assertNull(sessionManager.getSsrcTransactionByCallId(OLD_CALL_ID));
        assertEquals(NEW_CALL_ID, sessionManager.getSsrcTransactionByCallId(NEW_CALL_ID).getCallId());
        assertEquals(NEW_CALL_ID, sessionManager.getSsrcTransactionByStream(APP, STREAM).getCallId());
        assertTrue(bucket(streamKey()).containsKey(APP + STREAM));
    }

    @Test
    void removeByCallId_shouldDeleteStreamIndexWhenItStillPointsToSameTransaction() {
        sessionManager.put(transaction(OLD_CALL_ID));

        sessionManager.removeByCallId(OLD_CALL_ID);

        assertNull(sessionManager.getSsrcTransactionByCallId(OLD_CALL_ID));
        assertNull(sessionManager.getSsrcTransactionByStream(APP, STREAM));
        assertFalse(bucket(streamKey()).containsKey(APP + STREAM));
    }

    @Test
    void isStreamIndexOwnedBy_shouldHandleNulls() {
        assertFalse(SipInviteSessionManager.isStreamIndexOwnedBy(null, OLD_CALL_ID));
        assertFalse(SipInviteSessionManager.isStreamIndexOwnedBy(transaction(OLD_CALL_ID), null));
        assertFalse(SipInviteSessionManager.isStreamIndexOwnedBy(transaction(NEW_CALL_ID), OLD_CALL_ID));
        assertTrue(SipInviteSessionManager.isStreamIndexOwnedBy(transaction(OLD_CALL_ID), OLD_CALL_ID));
    }

    @Test
    void getSsrcTransactionByCallId_shouldFallbackToSipTransactionInfoCallId() {
        SsrcTransaction transaction = transaction(OLD_CALL_ID);
        SipTransactionInfo sipTransactionInfo = new SipTransactionInfo();
        sipTransactionInfo.setCallId(NEW_CALL_ID);
        transaction.setSipTransactionInfo(sipTransactionInfo);
        sessionManager.put(transaction);

        assertNotNull(sessionManager.getSsrcTransactionByCallId(NEW_CALL_ID));

        assertTrue(sessionManager.removeByCallId(NEW_CALL_ID));
        assertNull(sessionManager.getSsrcTransactionByCallId(NEW_CALL_ID));
        assertNull(sessionManager.getSsrcTransactionByStream(APP, STREAM));
    }

    @Test
    void getSsrcTransactionsByDeviceChannel_shouldFilterByDeviceChannelAndType() {
        SsrcTransaction first = transaction(OLD_CALL_ID);
        first.setDeviceId("device-1");
        first.setChannelId(1);
        first.setType(InviteSessionType.PLAY);
        sessionManager.put(first);

        SsrcTransaction second = transaction(NEW_CALL_ID);
        second.setDeviceId("device-1");
        second.setChannelId(2);
        second.setType(InviteSessionType.PLAY);
        sessionManager.put(second);

        assertEquals(1, sessionManager.getSsrcTransactionsByDeviceChannel(
                "device-1", 1, InviteSessionType.PLAY).size());
        assertEquals(0, sessionManager.getSsrcTransactionsByDeviceChannel(
                "device-1", 1, InviteSessionType.PLAYBACK).size());
    }

    @Test
    void removeByStreamIfSsrc_shouldNotDeleteNewerTransaction() {
        SsrcTransaction oldTransaction = transaction(OLD_CALL_ID);
        oldTransaction.setSsrc("old-ssrc");
        sessionManager.put(oldTransaction);

        SsrcTransaction newTransaction = transaction(NEW_CALL_ID);
        newTransaction.setSsrc("new-ssrc");
        sessionManager.put(newTransaction);

        assertFalse(sessionManager.removeByStreamIfSsrc(APP, STREAM, "old-ssrc"));
        assertEquals(NEW_CALL_ID, sessionManager.getSsrcTransactionByStream(APP, STREAM).getCallId());

        assertTrue(sessionManager.removeByStreamIfSsrc(APP, STREAM, "new-ssrc"));
        assertNull(sessionManager.getSsrcTransactionByStream(APP, STREAM));
    }
}
