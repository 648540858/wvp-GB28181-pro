package com.genersoft.iot.vmp.gb28181.session;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.PendingDialogClose;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 直接执行生产SessionCallback，验证关闭索引的轮次CAS和busy列表合并。 */
@SuppressWarnings({"unchecked", "rawtypes"})
class PendingDialogCloseStoreTest {
    private final RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
    private final RedisOperations<String, Object> operations = mock(RedisOperations.class);
    private final HashOperations<String, Object, Object> hashes = mock(HashOperations.class);
    private final Map<String, Map<Object, Object>> storage = new HashMap<>();
    private final List<Runnable> queued = new ArrayList<>();
    private final SipInviteSessionManager manager = new SipInviteSessionManager();
    private boolean transactionOpen;
    private int abortExecs;

    /** 用队列模拟MULTI提交与WATCH冲突，事务失败时绝不提前修改存储。 */
    @BeforeEach
    void setUp() {
        UserSetting settings = new UserSetting();
        settings.setServerId("isolated-test");
        ReflectionTestUtils.setField(manager, "userSetting", settings);
        ReflectionTestUtils.setField(manager, "redisTemplate", redis);
        when(redis.opsForHash()).thenReturn(hashes);
        when(operations.opsForHash()).thenReturn(hashes);
        when(hashes.get(anyString(), any())).thenAnswer(call -> bucket(call.getArgument(0)).get(call.getArgument(1)));
        when(hashes.values(anyString())).thenAnswer(call -> new ArrayList<>(bucket(call.getArgument(0)).values()));
        doAnswer(call -> {
            String key = call.getArgument(0); Object field = call.getArgument(1); Object value = call.getArgument(2);
            mutate(() -> bucket(key).put(field, value));
            return null;
        }).when(hashes).put(anyString(), any(), any());
        when(hashes.delete(anyString(), any())).thenAnswer(call -> {
            String key = call.getArgument(0); Object field = call.getArgument(1);
            mutate(() -> bucket(key).remove(field));
            return 1L;
        });
        doAnswer(call -> { transactionOpen = true; queued.clear(); return null; }).when(operations).multi();
        when(operations.exec()).thenAnswer(call -> {
            transactionOpen = false;
            if (abortExecs > 0) { abortExecs--; queued.clear(); return null; }
            queued.forEach(Runnable::run); queued.clear();
            return Collections.emptyList();
        });
        when(redis.execute(any(SessionCallback.class))).thenAnswer(call ->
                ((SessionCallback<?>) call.getArgument(0)).execute(operations));
    }

    /** 返回单个Redis Hash的内存内容，不跨实例或键空间写入。 */
    private Map<Object, Object> bucket(String key) { return storage.computeIfAbsent(key, ignored -> new HashMap<>()); }

    /** MULTI内只排队，确保CAS重试测试不会因假仓储提前写入而失真。 */
    private void mutate(Runnable mutation) {
        if (transactionOpen) { queued.add(mutation); } else { mutation.run(); }
    }

    /** 构造不依赖媒体或设备缓存的最小录像事务。 */
    private SsrcTransaction transaction(String callId) {
        SsrcTransaction result = new SsrcTransaction();
        result.setCallId(callId); result.setDeviceId("device"); result.setChannelId(17);
        result.setApp("rtp"); result.setStream("same-stream"); result.setType(InviteSessionType.DOWNLOAD);
        return result;
    }

    /** 以指定轮次构造持久化关闭记录。 */
    private PendingDialogClose close(String attempt, String callId) {
        PendingDialogClose result = new PendingDialogClose();
        result.setAttemptId(attempt); result.setTransaction(transaction(callId));
        result.setState(PendingDialogClose.PENDING); result.setAttempts(1);
        return result;
    }

    /** 首轮只允许空前置创建，重复创建不能覆盖正在等待设备的关闭身份。 */
    @Test
    void firstClaimCannotOverwriteExistingAttempt() {
        assertTrue(manager.updatePendingDialogClose("call", null, close("first", "call")));
        assertFalse(manager.updatePendingDialogClose("call", null, close("second", "call")));
        assertEquals("first", manager.getPendingDialogClose("call").getAttemptId());
    }

    /** 旧轮次更新或删除都必须失败，当前轮次仍可准确确认并删除。 */
    @Test
    void oldTokenCannotUpdateOrDeleteNewAttempt() {
        manager.updatePendingDialogClose("call", null, close("first", "call"));
        assertTrue(manager.updatePendingDialogClose("call", "first", close("second", "call")));
        assertFalse(manager.updatePendingDialogClose("call", "first", close("late", "call")));
        assertFalse(manager.updatePendingDialogClose("call", "first", null));
        assertEquals("second", manager.getPendingDialogClose("call").getAttemptId());
        assertTrue(manager.updatePendingDialogClose("call", "second", null));
        assertNull(manager.getPendingDialogClose("call"));
    }

    /** WATCH冲突后重试成功，不能把尚未提交的轮次当作已持久化。 */
    @Test
    void watchConflictRetriesWithoutPublishingUncommittedAttempt() {
        abortExecs = 1;
        assertTrue(manager.updatePendingDialogClose("call", null, close("first", "call")));
        assertEquals("first", manager.getPendingDialogClose("call").getAttemptId());
        verify(operations, times(2)).exec();
    }

    /** 三次冲突耗尽明确返回失败，不能留下未提交的ghost记录。 */
    @Test
    void exhaustedWatchConflictDoesNotCreatePendingRecord() {
        abortExecs = 3;
        assertFalse(manager.updatePendingDialogClose("call", null, close("first", "call")));
        assertNull(manager.getPendingDialogClose("call"));
        verify(operations, times(3)).exec();
    }

    /** 活动与关闭索引同Call-ID合并为一条，并暴露真实未知关闭状态。 */
    @Test
    void getAllDeduplicatesActiveAndPendingWithPendingState() {
        manager.put(transaction("call"));
        PendingDialogClose closing = close("first", "call");
        closing.setState(PendingDialogClose.UNKNOWN); closing.setResponseCode(500);
        manager.updatePendingDialogClose("call", null, closing);
        List<SsrcTransaction> all = manager.getAll();
        assertEquals(1, all.size());
        assertEquals(PendingDialogClose.UNKNOWN, all.getFirst().getTerminationState());
        assertEquals(500, all.getFirst().getTerminationResponseCode());
    }

    /** 活动索引先删除后busy列表仍有关闭记录，只有对应确认删除后才空闲。 */
    @Test
    void pendingKeepsGetAllBusyAfterActiveIndexRemoved() {
        manager.put(transaction("call"));
        manager.updatePendingDialogClose("call", null, close("first", "call"));
        assertTrue(manager.removeByCallId("call"));
        assertEquals(1, manager.getAll().size());
        assertEquals("call", manager.getAll().getFirst().getCallId());
        manager.updatePendingDialogClose("call", "first", null);
        assertTrue(manager.getAll().isEmpty());
    }

    /** 独立关闭Hash只扫描合法实体，不会把旧字段或损坏条目当作会话。 */
    @Test
    void storeIgnoresForeignValuesAndRetainsOtherCallIds() {
        manager.updatePendingDialogClose("first", null, close("a", "first"));
        manager.updatePendingDialogClose("second", null, close("b", "second"));
        bucket(VideoManagerConstants.SIP_INVITE_SESSION_CALL_ID + "CLOSING_isolated-test").put("foreign", "old-data");
        assertEquals(2, manager.getPendingDialogCloses().size());
        manager.updatePendingDialogClose("first", "a", null);
        assertNotNull(manager.getPendingDialogClose("second"));
        assertEquals(1, manager.getAll().size());
    }

    /** 确认快照可清理已经没有active索引的原对话，同时保护同流的新事务。 */
    @Test
    void confirmedSnapshotCleanupIsIdempotentAndPreservesNewStreamOwner() {
        SsrcTransaction original = transaction("old");
        manager.put(original);
        assertTrue(manager.removeByCallId("old"));
        manager.put(transaction("new"));
        assertTrue(manager.removeByCallId("old", original));
        assertEquals("new", manager.getSsrcTransactionByStream("rtp", "same-stream").getCallId());
        assertNotNull(manager.getSsrcTransactionByCallId("new"));
    }

    /** 已确认快照的删除CAS冲突必须报告失败，供owner保留终态并补清理。 */
    @Test
    void confirmedSnapshotCleanupReportsExhaustedConflict() {
        SsrcTransaction original = transaction("old");
        manager.put(original);
        abortExecs = 3;
        assertFalse(manager.removeByCallId("old", original));
        assertNotNull(manager.getSsrcTransactionByCallId("old"));
    }
}
