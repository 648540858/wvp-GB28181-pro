package com.genersoft.iot.vmp.gb28181.service.download;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.redis.RedisTemplateConfig;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import com.genersoft.iot.vmp.gb28181.bean.PendingDialogClose;
import com.genersoft.iot.vmp.gb28181.bean.RecordDialogCloseReceipt;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import com.genersoft.iot.vmp.service.bean.DownloadFileInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.*;

/** 仅显式配置专用Redis时验证Spring/Lettuce真实事务绑定，所有数据限制在本测试随机节点Hash。 */
class SegmentedDownloadRedisIntegrationTest {
    private static final String DEVICE = "99999999999999999991";
    private static final String CHANNEL = "99999999999999999992";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
    private static final DateTimeFormatter STREAM_TIME = DateTimeFormatter.ofPattern("uuuuMMddHHmmss");
    private final List<LettuceConnectionFactory> factories = new ArrayList<>();
    private StringRedisTemplate firstRedis;
    private SegmentedDownloadStore first;
    private SegmentedDownloadStore second;
    private String key;
    private String serverId;
    private final List<String> additionalKeys = new ArrayList<>();
    private final Queue<String> execResultShapes = new ConcurrentLinkedQueue<>();

    /** 两个独立工厂连接同一显式测试入口，随机服务身份保证不读写其他任务。 */
    @BeforeEach
    void setUp() {
        String host = System.getenv("WVP_TEST_REDIS_HOST");
        Assumptions.assumeTrue(host != null && !host.isBlank(), "未配置专用Redis，跳过真实事务集成验证");
        String portValue = System.getenv("WVP_TEST_REDIS_PORT");
        int port = portValue == null || portValue.isBlank() ? 6379 : Integer.parseInt(portValue);
        assertTrue(port > 0 && port <= 65535, "测试Redis端口必须有效");
        UserSetting setting = new UserSetting();
        serverId = "segmented-test-" + UUID.randomUUID();
        setting.setServerId(serverId);
        key = "WVP_SEGMENTED_DOWNLOAD_" + setting.getServerId();
        firstRedis = observingTemplate(factory(host.trim(), port));
        StringRedisTemplate secondRedis = observingTemplate(factory(host.trim(), port));
        first = new SegmentedDownloadStore(firstRedis, setting);
        second = new SegmentedDownloadStore(secondRedis, setting);
        assertFalse(Boolean.TRUE.equals(firstRedis.hasKey(key)), "随机命名空间必须从空状态开始");
    }

    /** 只删除当前随机节点的一个精确Hash，随后关闭本测试创建的两个连接工厂。 */
    @AfterEach
    void tearDown() {
        try {
            if (firstRedis != null && key != null) {
                firstRedis.delete(key);
                for (String ownedKey : additionalKeys) firstRedis.delete(ownedKey);
            }
            if (!execResultShapes.isEmpty()) System.out.println("Segmented Redis EXEC result shapes: " + execResultShapes);
        } finally {
            RuntimeException failure = null;
            for (LettuceConnectionFactory factory : factories) {
                try { factory.destroy(); }
                catch (RuntimeException error) {
                    if (failure == null) failure = error; else failure.addSuppressed(error);
                }
            }
            if (failure != null) throw failure;
        }
    }


    /** 保留真实连接和命令，只记录EXEC结果长度以诊断WATCH冲突的驱动返回语义。 */
    private StringRedisTemplate observingTemplate(LettuceConnectionFactory factory) {
        // 匿名模板只透明观测事务回复，不替换实际连接、返回值或提交行为。
        return new StringRedisTemplate(factory) {
            /** 空事务回复与null都必须由生产CAS正确识别，观测过程不改变任何Redis结果。 */
            @Override
            public List<Object> exec() {
                List<Object> result = super.exec();
                execResultShapes.add(result == null ? "null" : result.size() + ":" + result.stream()
                        .map(value -> value == null ? "null" : value.getClass().getSimpleName()).toList());
                return result;
            }
        };
    }

    /** 有界命令超时和关闭期限确保专用Redis不可用时验证不会永久占用线程。 */
    private LettuceConnectionFactory factory(String host, int port) {
        LettuceClientConfiguration client = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(3)).shutdownTimeout(Duration.ofMillis(200)).build();
        LettuceConnectionFactory factory = new LettuceConnectionFactory(new RedisStandaloneConfiguration(host, port), client);
        factories.add(factory);
        factory.afterPropertiesSet();
        factory.start();
        return factory;
    }

    /** 为同一设备的不同一分钟区间生成真实四段父流名，任务身份彼此独立。 */
    private SegmentedDownloadTask task(int interval) {
        LocalDateTime start = LocalDateTime.of(2026, 10, 27, 10, 0).plusMinutes(interval);
        LocalDateTime end = start.plusMinutes(1);
        SegmentedDownloadTask task = new SegmentedDownloadTask();
        task.setId(UUID.randomUUID().toString());
        task.setDeviceId(DEVICE);task.setChannelId(27);
        task.setStream(DEVICE + "_" + CHANNEL + "_" + start.format(STREAM_TIME) + "_" + end.format(STREAM_TIME));
        task.setStartTime(start.format(TIME));task.setEndTime(end.format(TIME));
        task.setState(SegmentedDownloadTask.State.QUEUED);task.setSafeToStart(true);
        return task;
    }

    /** 八个线程同时越过起跑门，所有等待和回收都使用总期限而非无限阻塞。 */
    private <T> List<T> concurrent(IntFunction<T> operation) throws Exception {
        AtomicInteger sequence = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(8, runnable -> {
            Thread thread = new Thread(runnable, "segmented-redis-test-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
        CountDownLatch ready = new CountDownLatch(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<T>> pending = new ArrayList<>();
        try {
            for (int index = 0; index < 8; index++) {
                int slot = index;
                pending.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("测试起跑门超时");
                    return operation.apply(slot);
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS), "所有测试线程必须就绪");
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : pending) {
                long remaining = deadline - System.nanoTime();
                assertTrue(remaining > 0, "真实Redis并发验证超时");
                results.add(future.get(remaining, TimeUnit.NANOSECONDS));
            }
            return results;
        } finally {
            start.countDown();
            pending.forEach(future -> future.cancel(true));
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "测试线程必须在连接工厂关闭前退出");
        }
    }

    /** 真实WATCH监视整个Hash时，同设备八个不同区间只能有一个取得忙碌任务名额。 */
    @Test
    void eightDifferentStreamsReserveOnlyOneBusyDeviceSlot() throws Exception {
        List<Boolean> reserved = concurrent(index -> {
            try {
                SegmentedDownloadTask candidate = task(index);
                SegmentedDownloadTask result = (index % 2 == 0 ? first : second).reserve(candidate);
                assertEquals(candidate.getId(), result.getId());
                return true;
            } catch (IllegalStateException rejected) {
                return false;
            }
        });
        List<SegmentedDownloadTask> durable = first.list();
        assertEquals(1, reserved.stream().filter(Boolean.TRUE::equals).count(), "持久化任务数=" + durable.size() + "; EXEC结果形状=" + execResultShapes);
        assertEquals(7, reserved.stream().filter(Boolean.FALSE::equals).count());
        assertEquals(1, durable.size());
        assertTrue(durable.get(0).isBusy());
        assertEquals(durable.get(0).getId(), second.get(durable.get(0).getStream()).getId());
    }

    /** 同一四段流名的八次并发申请都返回同一个持久化任务，不能各自创建新轮次。 */
    @Test
    void eightSameCanonicalRequestsReuseOneDurableTask() throws Exception {
        List<SegmentedDownloadTask> reserved = concurrent(index ->
                (index % 2 == 0 ? first : second).reserve(task(0)));
        assertEquals(1, reserved.stream().map(SegmentedDownloadTask::getId).distinct().count(), "持久化任务数=" + first.list().size() + "; EXEC结果形状=" + execResultShapes);
        assertTrue(reserved.stream().allMatch(value -> value.getVersion() == 1));
        assertEquals(1, first.list().size());
        assertEquals(reserved.get(0).getId(), second.get(reserved.get(0).getStream()).getId());
    }

    /** 两个真实连接的旧版本不能覆盖另一实例的取消，同时验证对话及分段的实际JSON往返。 */
    @Test
    void independentStoreCasRetainsCancellationAndDialogSnapshots() {
        SegmentedDownloadTask reserved = first.reserve(task(0));
        SsrcTransaction currentDialog = dialog("current-call");
        SsrcTransaction lastDialog = dialog("previous-call");
        reserved.setDialog(currentDialog);reserved.setLastDialog(lastDialog);reserved.setSafeToStart(false);
        reserved.setState(SegmentedDownloadTask.State.DOWNLOADING);
        StreamInfo info = new StreamInfo();info.setStream("native-child");info.setStartTime(reserved.getStartTime());
        info.setEndTime(reserved.getEndTime());info.setDownloadStatus("completed");info.setDownloadedDuration(60.0);
        DownloadFileInfo file = new DownloadFileInfo();file.setHttpsPath("https://example.invalid/segment.mp4");
        info.setDownLoadFilePath(file);
        SegmentedDownloadTask.Segment segment = new SegmentedDownloadTask.Segment();
        segment.setStream("native-child");segment.setStartTime(reserved.getStartTime());segment.setEndTime(reserved.getEndTime());segment.setInfo(info);
        reserved.getSegments().add(segment);
        reserved.setChildInfo(info);
        assertTrue(first.save(reserved, 1));
        SegmentedDownloadTask stale = first.get(reserved.getStream());
        SegmentedDownloadTask cancelling = second.get(reserved.getStream());
        cancelling.setCancelRequested(true);cancelling.setState(SegmentedDownloadTask.State.CLOSING);
        assertTrue(second.save(cancelling, 2));
        long staleUpdatedAt = stale.getUpdatedAt();
        stale.setSafeToStart(true);stale.setState(SegmentedDownloadTask.State.QUEUED);
        assertFalse(first.save(stale, 2));
        assertEquals(2, stale.getVersion());assertEquals(staleUpdatedAt, stale.getUpdatedAt());
        currentDialog.setCallId("local-mutation");lastDialog.getSipTransactionInfo().setToTag("local-mutation");
        SegmentedDownloadTask durable = first.get(reserved.getStream());
        assertEquals(3, durable.getVersion());
        assertTrue(durable.isCancelRequested());assertFalse(durable.isSafeToStart());
        assertEquals(SegmentedDownloadTask.State.CLOSING, durable.getState());
        assertEquals("current-call", durable.getDialog().getCallId());
        assertEquals("previous-call", durable.getLastDialog().getCallId());
        assertEquals("to-previous-call", durable.getLastDialog().getSipTransactionInfo().getToTag());
        assertEquals("https://example.invalid/segment.mp4", durable.getSegments().get(0).getInfo().getDownLoadFilePath().getHttpsPath());
        assertEquals(60.0, durable.getChildInfo().getDownloadedDuration());
    }



    /** 真实Redis区分父任务复用与物理设备容量，合并中的原任务不挡后续区间或失去自身身份。 */
    @Test
    void safeMergingReusesParentAndAdmitsNextIntervalWithCasCancellation() {
        SegmentedDownloadTask merging = first.reserve(task(0));
        merging.setState(SegmentedDownloadTask.State.MERGING);
        assertTrue(first.save(merging, 1));
        assertTrue(merging.isBusy());assertFalse(merging.occupiesDevice());
        assertEquals(merging.getId(), second.reserve(task(0)).getId());
        SegmentedDownloadTask later = second.reserve(task(1));
        assertNotEquals(merging.getId(), later.getId());
        assertEquals(2, first.list().size());
        assertEquals(SegmentedDownloadTask.State.MERGING, first.get(merging.getStream()).getState());
        SegmentedDownloadTask stale = first.get(later.getStream());
        SegmentedDownloadTask cancelled = second.get(later.getStream());
        cancelled.setCancelRequested(true);cancelled.setState(SegmentedDownloadTask.State.CANCELLED);
        assertTrue(second.save(cancelled, 1));
        stale.setState(SegmentedDownloadTask.State.STARTING);stale.setSafeToStart(false);
        assertFalse(first.save(stale, 1));
        assertEquals(SegmentedDownloadTask.State.CANCELLED, first.get(later.getStream()).getState());
        assertTrue(first.get(later.getStream()).isCancelRequested());
        assertEquals(SegmentedDownloadTask.State.MERGING, first.get(merging.getStream()).getState());
        assertEquals(merging.getId(), first.get(merging.getStream()).getId());
    }

    /** 实际SIP序列化和WATCH冲突复用专用Redis，空EXEC不能撤销尚未删除的原Dialog或pending依据。 */
    @Test
    void sipManagerRealTransactionsKeepIdentityAfterWatchAbort() {
        String streamKey = VideoManagerConstants.SIP_INVITE_SESSION_STREAM + serverId;
        String callKey = VideoManagerConstants.SIP_INVITE_SESSION_CALL_ID + serverId;
        String closingKey = VideoManagerConstants.SIP_INVITE_SESSION_CALL_ID + "CLOSING_" + serverId;
        additionalKeys.addAll(List.of(streamKey, callKey, closingKey));
        RedisTemplate<String, Object> competingRedis = objectTemplate(factories.get(1), null);
        SipInviteSessionManager competing = manager(competingRedis);
        AtomicInteger conflicts = new AtomicInteger();
        AtomicInteger noise = new AtomicInteger();
        AtomicBoolean closing = new AtomicBoolean();
        RedisTemplate<String, Object> primaryRedis = objectTemplate(factories.get(0), () -> {
            if (conflicts.getAndUpdate(value -> Math.max(0, value - 1)) == 0) return;
            int number = noise.incrementAndGet();
            SsrcTransaction concurrent = dialog("noise-" + number);
            concurrent.setStream("noise-stream-" + number);
            if (closing.get()) {
                PendingDialogClose pending = new PendingDialogClose();
                pending.setAttemptId("noise-attempt-" + number);pending.setTransaction(concurrent);pending.setState(PendingDialogClose.PENDING);
                assertTrue(competing.updatePendingDialogClose(concurrent.getCallId(), null, pending));
            } else {
                competing.put(concurrent);
            }
        });
        SipInviteSessionManager primary = manager(primaryRedis);
        SsrcTransaction original = dialog("original-call");
        primary.put(original);
        assertEquals(original, competing.getSsrcTransactionByCallId(original.getCallId()));
        conflicts.set(3);
        assertFalse(primary.removeByCallId(original.getCallId()));
        assertNotNull(competing.getSsrcTransactionByCallId(original.getCallId()));
        assertNotNull(competing.getSsrcTransactionByStream(original.getApp(), original.getStream()));
        assertTrue(primary.removeByCallId(original.getCallId()));
        assertNull(competing.getSsrcTransactionByCallId(original.getCallId()));
        assertNull(competing.getSsrcTransactionByStream(original.getApp(), original.getStream()));

        SsrcTransaction replacement = dialog("replacement-call");
        replacement.setSsrc("1234567891");
        primary.put(original);primary.put(replacement);
        assertTrue(primary.removeByCallId(original.getCallId()));
        assertEquals(replacement.getCallId(), competing.getSsrcTransactionByStream("rtp", "native-child").getCallId());
        assertTrue(primary.removeByStreamIfCallId("rtp", "native-child", replacement.getCallId()));
        primary.put(replacement);
        assertTrue(primary.removeByStreamIfSsrc("rtp", "native-child", replacement.getSsrc()));

        PendingDialogClose pending = new PendingDialogClose();
        pending.setAttemptId("original-attempt");pending.setTransaction(original);pending.setState(PendingDialogClose.PENDING);
        assertTrue(primary.updatePendingDialogClose(original.getCallId(), null, pending));
        assertEquals("original-attempt", competing.getPendingDialogClose(original.getCallId()).getAttemptId());
        closing.set(true);conflicts.set(3);
        assertFalse(primary.updatePendingDialogClose(original.getCallId(), "original-attempt", null));
        assertEquals("original-attempt", competing.getPendingDialogClose(original.getCallId()).getAttemptId());
        assertTrue(primary.updatePendingDialogClose(original.getCallId(), "original-attempt", null));
        assertNull(competing.getPendingDialogClose(original.getCallId()));

        String receiptKey = callKey + ":RECORD_CLOSED:" + original.getCallId();
        additionalKeys.add(receiptKey);
        RecordDialogCloseReceipt receipt = RecordDialogCloseReceipt.fromSnapshot(original, System.currentTimeMillis(), 200);
        primary.saveRecordDialogCloseReceipt(original.getCallId(), receipt);
        assertTrue(competing.getRecordDialogCloseReceipt(original.getCallId()).matches(original));
        Long remaining = primaryRedis.getExpire(receiptKey, TimeUnit.SECONDS);
        assertNotNull(remaining);assertTrue(remaining > 0 && remaining <= 1800);
    }

    /** 使用生产RedisTemplateConfig的四种实际序列化器，拦截点只注入真实的另一连接写入。 */
    private RedisTemplate<String, Object> objectTemplate(LettuceConnectionFactory factory, Runnable beforeExec) {
        RedisTemplate<String, Object> configured = new RedisTemplateConfig().redisTemplate(factory);
        RedisTemplate<String, Object> observed = new RedisTemplate<>() {
            /** 返回真实EXEC结果，仅记录长度与类型，并允许另一真实连接改动WATCH键。 */
            @Override
            public List<Object> exec() {
                if (beforeExec != null) beforeExec.run();
                List<Object> result = super.exec();
                execResultShapes.add(result == null ? "SIP-null" : "SIP-" + result.size() + ":" + result.stream()
                        .map(value -> value == null ? "null" : value.getClass().getSimpleName()).toList());
                return result;
            }
        };
        observed.setConnectionFactory(factory);
        observed.setKeySerializer(configured.getKeySerializer());observed.setHashKeySerializer(configured.getHashKeySerializer());
        observed.setValueSerializer(configured.getValueSerializer());observed.setHashValueSerializer(configured.getHashValueSerializer());
        observed.afterPropertiesSet();
        return observed;
    }

    /** 只注入随机测试节点与真实模板，管理器不会连接设备或发出任何SIP消息。 */
    private SipInviteSessionManager manager(RedisTemplate<String, Object> redis) {
        UserSetting setting = new UserSetting();setting.setServerId(serverId);
        SipInviteSessionManager manager = new SipInviteSessionManager();
        ReflectionTestUtils.setField(manager, "userSetting", setting);
        ReflectionTestUtils.setField(manager, "redisTemplate", redis);
        return manager;
    }

    /** 使用无路由凭据的完整原对话快照验证父任务持久化，不发送任何SIP命令。 */
    private SsrcTransaction dialog(String callId) {
        SsrcTransaction dialog = new SsrcTransaction();
        dialog.setDeviceId(DEVICE);dialog.setChannelId(27);dialog.setCallId(callId);dialog.setSsrc("1234567890");
        dialog.setStream("native-child");dialog.setApp("rtp");dialog.setMediaServerId("synthetic-media");
        dialog.setType(InviteSessionType.DOWNLOAD);
        SipTransactionInfo sip = new SipTransactionInfo();
        sip.setCallId(callId);sip.setFromTag("from-" + callId);sip.setToTag("to-" + callId);sip.setViaBranch("branch-" + callId);
        dialog.setSipTransactionInfo(sip);
        return dialog;
    }
}
