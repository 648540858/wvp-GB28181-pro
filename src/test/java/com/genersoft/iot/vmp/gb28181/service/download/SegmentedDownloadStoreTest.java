package com.genersoft.iot.vmp.gb28181.service.download;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import com.genersoft.iot.vmp.service.bean.DownloadFileInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 执行真实事务回调，并用排队提交和竞争写入验证WATCH冲突下的设备认领与轮次CAS。 */
@SuppressWarnings({"unchecked", "rawtypes"})
class SegmentedDownloadStoreTest {
    private static final String KEY = "WVP_SEGMENTED_DOWNLOAD_store-test";
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final RedisOperations<String, String> operations = mock(RedisOperations.class);
    private final HashOperations<String, Object, Object> hashes = mock(HashOperations.class);
    private final Map<Object, Object> storage = new HashMap<>();
    private final List<Runnable> queued = new ArrayList<>();
    private SegmentedDownloadStore store;
    private boolean transactionOpen;
    private int abortExecs;
    private boolean abortWithEmptyResult;
    private Runnable competingWrite;

    /** MULTI阶段仅排队，冲突时丢弃整次提交，避免假Redis提前写入掩盖认领竞态。 */
    @BeforeEach
    void setUp() {
        UserSetting setting = new UserSetting();
        setting.setServerId("store-test");
        store = new SegmentedDownloadStore(redis, setting);
        when(redis.opsForHash()).thenReturn(hashes);
        when(operations.opsForHash()).thenReturn(hashes);
        when(hashes.get(eq(KEY), any())).thenAnswer(call -> storage.get(call.getArgument(1)));
        when(hashes.values(KEY)).thenAnswer(call -> new ArrayList<>(storage.values()));
        doAnswer(call -> {
            Object field = call.getArgument(1);
            Object value = call.getArgument(2);
            queued.add(() -> storage.put(field, value));
            return null;
        }).when(hashes).put(eq(KEY), any(), any());
        when(hashes.delete(eq(KEY), any())).thenAnswer(call -> {
            Object field = call.getArgument(1);
            queued.add(() -> storage.remove(field));
            return 1L;
        });
        doAnswer(call -> { transactionOpen = true; queued.clear(); return null; }).when(operations).multi();
        doAnswer(call -> { transactionOpen = false; queued.clear(); return null; }).when(operations).discard();
        when(operations.exec()).thenAnswer(call -> {
            assertTrue(transactionOpen, "写入必须处于MULTI内");
            transactionOpen = false;
            if (abortExecs > 0) {
                abortExecs--;
                queued.clear();
                if (competingWrite != null) { competingWrite.run(); competingWrite = null; }
                return abortWithEmptyResult ? List.of() : null;
            }
            queued.forEach(Runnable::run);
            queued.clear();
            return List.of(Boolean.TRUE);
        });
        when(redis.execute(any(SessionCallback.class))).thenAnswer(call ->
                ((SessionCallback<?>) call.getArgument(0)).execute(operations));
    }

    /** 使用固定服务和合成设备身份构造尚未建流的下载任务。 */
    private SegmentedDownloadTask task(String id, String stream, String device) {
        SegmentedDownloadTask task = new SegmentedDownloadTask();
        task.setId(id);task.setStream(stream);task.setDeviceId(device);task.setChannelId(27);
        task.setState(SegmentedDownloadTask.State.QUEUED);task.setSafeToStart(true);
        task.setStartTime("2026-10-27 10:00:00");task.setEndTime("2026-10-27 10:10:00");
        return task;
    }

    /** 初次认领使用版本一，返回和查询均为独立快照，不暴露可变的存储引用。 */
    @Test
    void reserveStartsAtOneAndReturnsIndependentSnapshots() {
        SegmentedDownloadTask input = task("first", "stream-a", "device-a");
        SegmentedDownloadTask reserved = store.reserve(input);
        assertEquals(1, reserved.getVersion());
        assertEquals("store-test", reserved.getServerId());
        assertTrue(reserved.getCreatedAt() > 0);
        assertTrue(reserved.getUpdatedAt() >= reserved.getCreatedAt());
        assertNotSame(input, reserved);
        reserved.setError("local-only");
        input.setDeviceId("changed-locally");
        assertNull(store.get("stream-a").getError());
        assertEquals("device-a", store.get("stream-a").getDeviceId());
        List<SegmentedDownloadTask> all = store.list();
        assertEquals(1, all.size());
        all.get(0).setError("another-local-change");
        assertNull(store.get("stream-a").getError());
        verify(operations, never()).keys(anyString());
        verify(redis, never()).keys(anyString());
    }

    /** 同一逻辑区间在途请求直接复用身份，不再次占用设备或重置检查点。 */
    @Test
    void sameBusyStreamIsReusedWithoutOverwritingOwner() {
        SegmentedDownloadTask first = store.reserve(task("first", "stream-a", "device-a"));
        SegmentedDownloadTask reused = store.reserve(task("second", "stream-a", "device-a"));
        assertEquals(first.getId(), reused.getId());
        assertEquals(first.getCreatedAt(), reused.getCreatedAt());
        assertEquals(1, reused.getVersion());
        verify(operations, times(1)).multi();
    }

    /** 完整成品即使设备正在处理其他区间，也可直接读取复用而不重新建流。 */
    @Test
    void completedFileIsReusedWhileAnotherStreamIsBusy() {
        SegmentedDownloadTask first = store.reserve(task("first", "stream-a", "device-a"));
        first.setState(SegmentedDownloadTask.State.COMPLETED);
        assertTrue(store.save(first, 1));
        store.reserve(task("active", "stream-b", "device-a"));
        assertEquals("first", store.reserve(task("retry", "stream-a", "device-a")).getId());
    }

    /** 不同区间共享设备名额，另一设备仍可独立认领。 */
    @Test
    void otherBusyStreamBlocksOnlyTheSameDevice() {
        store.reserve(task("first", "stream-a", "device-a"));
        assertThrows(IllegalStateException.class, () -> store.reserve(task("second", "stream-b", "device-a")));
        assertNull(store.get("stream-b"));
        assertEquals("third", store.reserve(task("third", "stream-c", "device-b")).getId());
    }

    /** 失败且未确认释放时继续占用设备，只有安全终态才允许新轮次替换。 */
    @Test
    void terminalFailureCannotReleaseUnconfirmedDevice() {
        SegmentedDownloadTask first = store.reserve(task("first", "stream-a", "device-a"));
        first.setState(SegmentedDownloadTask.State.FAILED);first.setSafeToStart(false);
        assertTrue(store.save(first, 1));
        assertEquals("first", store.reserve(task("blocked", "stream-a", "device-a")).getId());
        assertThrows(IllegalStateException.class, () -> store.reserve(task("other", "stream-b", "device-a")));
        assertFalse(store.deleteIfCurrent(first));
        first.setSafeToStart(true);
        assertTrue(store.save(first, 2));
        assertEquals("replacement", store.reserve(task("replacement", "stream-a", "device-a")).getId());
        assertFalse(store.save(first, 3), "旧任务即使版本巧合相同也不能覆盖替代身份");
    }


    /** 安全合并保留同区间任务身份，但不占设备名额，后续区间可以在原任务仍存在时开始。 */
    @Test
    void safeMergingReusesCanonicalButAllowsAnotherDeviceInterval() {
        SegmentedDownloadTask merging = store.reserve(task("merging", "stream-a", "device-a"));
        merging.setState(SegmentedDownloadTask.State.MERGING);
        assertTrue(store.save(merging, 1));
        assertTrue(merging.isBusy());assertFalse(merging.occupiesDevice());
        assertEquals("merging", store.reserve(task("same-request", "stream-a", "device-a")).getId());
        SegmentedDownloadTask later = store.reserve(task("later", "stream-b", "device-a"));
        assertEquals("later", later.getId());
        assertEquals(2, store.list().size());
        assertEquals(SegmentedDownloadTask.State.MERGING, store.get("stream-a").getState());
        assertEquals(2, store.get("stream-a").getVersion());
        later.setState(SegmentedDownloadTask.State.FAILED);later.setSafeToStart(false);
        assertTrue(store.save(later, 1));
        assertTrue(later.occupiesDevice());
        assertThrows(IllegalStateException.class, () -> store.reserve(task("blocked", "stream-c", "device-a")));
        assertFalse(store.deleteIfCurrent(later));
    }

    /** 逻辑终态和设备释放分开判断，未知状态也始终保守占用名额。 */
    @Test
    void terminalAndBusyHaveIndependentMeanings() {
        SegmentedDownloadTask task = task("id", "stream", "device");
        for (SegmentedDownloadTask.State state : SegmentedDownloadTask.State.values()) {
            task.setState(state);task.setSafeToStart(true);
            boolean terminal = state == SegmentedDownloadTask.State.COMPLETED
                    || state == SegmentedDownloadTask.State.FAILED || state == SegmentedDownloadTask.State.CANCELLED;
            assertEquals(terminal, task.isTerminal());
            assertEquals(!terminal, task.isBusy());
            assertEquals(!terminal && state != SegmentedDownloadTask.State.MERGING, task.occupiesDevice());
            task.setSafeToStart(false);
            assertTrue(task.isBusy());
            assertTrue(task.occupiesDevice());
        }
        task.setState(null);task.setSafeToStart(true);
        assertTrue(task.isBusy());
        assertTrue(task.occupiesDevice());
    }

    /** 陈旧版本、陈旧身份或不存在的任务都不能被CAS更新。 */
    @Test
    void saveRequiresBothVersionAndIdentity() {
        SegmentedDownloadTask first = store.reserve(task("first", "stream-a", "device-a"));
        SegmentedDownloadTask old = store.get("stream-a");
        first.setState(SegmentedDownloadTask.State.DOWNLOADING);
        assertTrue(store.save(first, 1));
        assertEquals(2, first.getVersion());
        assertFalse(store.save(old, 1));
        old.setId("different-id");
        assertFalse(store.save(old, 2));
        assertFalse(store.save(task("absent", "other", "device-b"), 1));
        assertEquals(SegmentedDownloadTask.State.DOWNLOADING, store.get("stream-a").getState());
    }

    /** 事务冲突后重新核验设备，另一工作者的并发认领必须阻止本轮创建。 */
    @Test
    void watchConflictRechecksCompetingDeviceReservation() {
        abortExecs = 1;
        SegmentedDownloadTask competing = task("winner", "stream-b", "device-a");
        competing.setServerId("store-test");competing.setVersion(1);
        competingWrite = () -> storage.put("stream-b", JSON.toJSONString(competing));
        assertThrows(IllegalStateException.class, () -> store.reserve(task("loser", "stream-a", "device-a")));
        assertNull(store.get("stream-a"));
        assertEquals("winner", store.get("stream-b").getId());
        verify(operations, times(1)).exec();
        verify(operations, times(2)).watch(KEY);
    }

    /** 偶发WATCH冲突可以有限重试成功，未提交的候选不能提前出现在存储中。 */
    @Test
    void transientWatchConflictRetriesAndCommitsOnce() {
        abortExecs = 1;
        SegmentedDownloadTask first = store.reserve(task("first", "stream-a", "device-a"));
        assertEquals(1, first.getVersion());
        verify(operations, times(2)).exec();
        abortExecs = 1;
        first.setState(SegmentedDownloadTask.State.DOWNLOADING);
        assertTrue(store.save(first, 1));
        assertEquals(2, store.get("stream-a").getVersion());
    }

    /** 六次冲突耗尽必须失败关闭，调用方和存储的版本都不能被提前推进。 */
    @Test
    void exhaustedWatchConflictsDoNotPublishChanges() {
        abortExecs = 6;
        assertThrows(IllegalStateException.class, () -> store.reserve(task("lost", "stream-a", "device-a")));
        assertNull(store.get("stream-a"));
        verify(operations, times(6)).exec();
        SegmentedDownloadTask first = store.reserve(task("first", "stream-a", "device-a"));
        abortExecs = 6;
        first.setState(SegmentedDownloadTask.State.DOWNLOADING);
        assertFalse(store.save(first, 1));
        assertEquals(1, first.getVersion());
        assertEquals(SegmentedDownloadTask.State.QUEUED, store.get("stream-a").getState());
    }

    /** 清理只允许安全终态且版本一致，旧扫描不能删掉后来的状态或替代任务。 */
    @Test
    void deletionRequiresCurrentSafeTerminalSnapshot() {
        SegmentedDownloadTask first = store.reserve(task("first", "stream-a", "device-a"));
        assertFalse(store.deleteIfCurrent(first));
        SegmentedDownloadTask old = store.get("stream-a");
        first.setState(SegmentedDownloadTask.State.CANCELLED);
        assertTrue(store.save(first, 1));
        assertFalse(store.deleteIfCurrent(old));
        assertTrue(store.deleteIfCurrent(first));
        assertNull(store.get("stream-a"));
        assertFalse(store.deleteIfCurrent(first));
    }


    /** Spring/Lettuce把WATCH中止转换为空列表时，认领仍须重读，不能返回尚未持久化的UUID。 */
    @Test
    void emptyExecReservationRetriesAndNeverReturnsGhostIdentity() {
        abortWithEmptyResult = true;abortExecs = 1;
        SegmentedDownloadTask reserved = store.reserve(task("first", "stream-a", "device-a"));
        assertNotNull(store.get("stream-a"));
        assertEquals(reserved.getId(), store.get("stream-a").getId());
        verify(operations, times(2)).exec();
        abortExecs = 6;
        assertThrows(IllegalStateException.class, () -> store.reserve(task("ghost", "stream-b", "device-b")));
        assertNull(store.get("stream-b"));
        assertEquals(1, store.list().size());
    }

    /** 空EXEC结果不能提前推进调用方版本，只有后续真实提交才能改变持久化状态。 */
    @Test
    void emptyExecSaveRetriesWithoutAdvancingUncommittedVersion() {
        SegmentedDownloadTask current = store.reserve(task("first", "stream-a", "device-a"));
        abortWithEmptyResult = true;abortExecs = 1;
        current.setState(SegmentedDownloadTask.State.DOWNLOADING);
        assertTrue(store.save(current, 1));
        assertEquals(2, current.getVersion());
        assertEquals(2, store.get("stream-a").getVersion());
        assertEquals(SegmentedDownloadTask.State.DOWNLOADING, store.get("stream-a").getState());
        abortExecs = 6;
        long updatedAt = current.getUpdatedAt();
        current.setState(SegmentedDownloadTask.State.MERGING);
        assertFalse(store.save(current, 2));
        assertEquals(2, current.getVersion());assertEquals(updatedAt, current.getUpdatedAt());
        assertEquals(SegmentedDownloadTask.State.DOWNLOADING, store.get("stream-a").getState());
    }

    /** 清理遇到空EXEC同样必须重试，耗尽仍保留原安全终态而非假报已经删除。 */
    @Test
    void emptyExecDeleteRetriesAndKeepsTaskWhenExhausted() {
        SegmentedDownloadTask current = store.reserve(task("first", "stream-a", "device-a"));
        current.setState(SegmentedDownloadTask.State.CANCELLED);
        assertTrue(store.save(current, 1));
        abortWithEmptyResult = true;abortExecs = 1;
        assertTrue(store.deleteIfCurrent(current));
        assertNull(store.get("stream-a"));
        SegmentedDownloadTask later = store.reserve(task("later", "stream-b", "device-a"));
        later.setState(SegmentedDownloadTask.State.CANCELLED);
        assertTrue(store.save(later, 1));
        abortExecs = 6;
        assertFalse(store.deleteIfCurrent(later));
        assertEquals(later.getId(), store.get("stream-b").getId());
        assertEquals(later.getVersion(), store.get("stream-b").getVersion());
    }

    /** 不可用连接和损坏快照都必须向上游明确失败，不得当作没有任务而继续建流。 */
    @Test
    void unavailableOrCorruptStoreCannotBecomeFreeCapacity() {
        when(redis.execute(any(SessionCallback.class))).thenThrow(new RedisConnectionFailureException("synthetic outage"));
        assertThrows(RedisConnectionFailureException.class, () -> store.reserve(task("first", "stream-a", "device-a")));
        storage.put("corrupt", "invalid-json");
        assertThrows(IllegalStateException.class, store::list);
    }

    /** 排队写入异常必须DISCARD，不能把残留MULTI连接交给后续任务。 */
    @Test
    void failedQueuedWriteDiscardsTransactionAndDoesNotPersist() {
        doThrow(new RedisConnectionFailureException("synthetic queued write failure"))
                .when(hashes).put(eq(KEY), any(), any());
        assertThrows(RedisConnectionFailureException.class, () -> store.reserve(task("first", "stream-a", "device-a")));
        assertFalse(transactionOpen);
        assertTrue(storage.isEmpty());
        verify(operations).discard();
    }


    /** 实际JSON协议完整恢复对话与分段信息，恢复投影不会共享调用方的可变列表或对象。 */
    @Test
    void durableSnapshotRetainsDialogAndSegmentMetadata() {
        SegmentedDownloadTask input = task("first", "stream-a", "device-a");
        SsrcTransaction dialog = new SsrcTransaction();
        dialog.setCallId("synthetic-call");dialog.setStream("child-stream");dialog.setSsrc("1234567890");
        SipTransactionInfo sip = new SipTransactionInfo();
        sip.setCallId("synthetic-call");sip.setFromTag("synthetic-from");sip.setToTag("synthetic-to");
        dialog.setSipTransactionInfo(sip);
        input.setDialog(dialog);input.setLastDialog(dialog);
        StreamInfo child = new StreamInfo();
        child.setStream("child-stream");child.setDownloadStatus("completed");child.setDownloadedDuration(60.0);
        input.setChildInfo(child);
        SegmentedDownloadTask.Segment segment = new SegmentedDownloadTask.Segment();
        segment.setStream("child-stream");segment.setStartTime(input.getStartTime());segment.setEndTime(input.getEndTime());segment.setInfo(child);
        input.getSegments().add(segment);
        DownloadFileInfo file = new DownloadFileInfo();file.setHttpPath("http://example.invalid/complete.mp4");
        input.setFinalFile(file);input.setCancelRequested(true);
        store.reserve(input);
        input.getSegments().clear();dialog.setCallId("local-change");

        SegmentedDownloadTask recovered = store.get("stream-a");
        assertEquals("synthetic-call", recovered.getDialog().getCallId());
        assertEquals("synthetic-call", recovered.getLastDialog().getCallId());
        assertEquals("synthetic-to", recovered.getLastDialog().getSipTransactionInfo().getToTag());
        assertEquals(1, recovered.getSegments().size());
        assertEquals(60.0, recovered.getSegments().get(0).getInfo().getDownloadedDuration());
        assertEquals("http://example.invalid/complete.mp4", recovered.getFinalFile().getHttpPath());
        assertTrue(recovered.isCancelRequested());
    }

    /** 删除持续冲突时保留原安全终态，后续清理轮次仍可恢复而不丢任务。 */
    @Test
    void exhaustedDeleteConflictsLeaveTerminalTaskAvailable() {
        SegmentedDownloadTask current = store.reserve(task("first", "stream-a", "device-a"));
        current.setState(SegmentedDownloadTask.State.CANCELLED);
        assertTrue(store.save(current, 1));
        abortExecs = 6;
        assertFalse(store.deleteIfCurrent(current));
        assertEquals("first", store.get("stream-a").getId());
        assertTrue(store.deleteIfCurrent(current));
        assertNull(store.get("stream-a"));
    }

    /** 空身份或跨节点任务不能访问当前服务的持久化命名空间。 */
    @Test
    void invalidOrCrossServerIdentityIsRejectedBeforeRedis() {
        SegmentedDownloadTask invalid = task("first", "stream-a", "device-a");
        invalid.setServerId("another-server");
        assertThrows(IllegalArgumentException.class, () -> store.reserve(invalid));
        assertThrows(IllegalArgumentException.class, () -> store.get(""));
        assertThrows(IllegalArgumentException.class, () -> store.reserve(null));
        verify(redis, never()).execute(any(SessionCallback.class));
    }
}
