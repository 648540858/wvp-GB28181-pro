package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.dto.FlagData;
import com.genersoft.iot.vmp.media.zlm.dto.ZLMResult;
import com.genersoft.iot.vmp.service.bean.CloudRecordItem;
import com.genersoft.iot.vmp.storager.dao.CloudRecordServiceMapper;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.cloudRecord.bean.CloudRecordSessionContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 从公共服务动作验证文件共享、控制隔离与失败恢复，不依赖真实时间或媒体节点。 */
@ExtendWith(MockitoExtension.class)
class CloudRecordSessionServiceImplTest {
    private static final int USER = 7;
    private static final int RECORD = 21;
    private static final String APP = "mp4_record";
    private static final String SERVER = "synthetic-wvp";
    private static final String FILE = "/synthetic/private/source.mp4";
    private static final long DURATION = 60_000L;

    @Mock
    private CloudRecordServiceMapper mapper;
    @Mock
    private IMediaServerService media;
    @Mock
    private ZLMRESTfulUtils zlm;
    @Mock
    private UserSetting settings;

    private MediaServer node;
    private CloudRecordItem record;
    private MutableClock clock;
    private CloudRecordSessionServiceImpl service;

    /** 设置可信录像索引与同步媒体响应；各拒绝用例可在任何外呼前结束。 */
    @BeforeEach
    void setUp() {
        clock = new MutableClock(1_000_000L);
        record = new CloudRecordItem();
        record.setId(RECORD);
        record.setServerId(SERVER);
        record.setMediaServerId("synthetic-zlm");
        record.setFilePath(FILE);
        record.setFileName("source.mp4");
        record.setTimeLen(DURATION);
        node = new MediaServer();
        node.setId("synthetic-zlm");
        node.setStatus(true);
        node.setType("zlm");
        node.setSecret("synthetic-node-secret");
        lenient().when(mapper.queryOne(RECORD)).thenReturn(record);
        lenient().when(settings.getServerId()).thenReturn(SERVER);
        lenient().when(media.getOne(node.getId())).thenReturn(node);
        lenient().doReturn(result(0)).when(zlm)
                .loadMP4File(eq(node), eq(APP), anyString(), eq(FILE), anyLong());
        lenient().when(zlm.closeStreams(eq(node), eq(APP), anyString())).thenReturn(closeResult(0));
        lenient().when(media.getStreamInfoByAppAndStream(eq(node), eq(APP), anyString(),
                isNull(), nullable(String.class), isNull(), eq(true))).thenAnswer(invocation -> {
            StreamInfo info = new StreamInfo();
            info.setApp(APP);
            info.setStream(invocation.getArgument(2));
            info.setMediaServer(node);
            return info;
        });
        service = new CloudRecordSessionServiceImpl(mapper, media, zlm, settings, clock);
    }

    /** 同一文件的两位观看者必须获得不同控制身份和不同媒体源。 */
    @Test
    void sameFileCreatesIndependentSessionsAndStreams() {
        CloudRecordSessionContent a = service.create(USER, RECORD, 0, null);
        CloudRecordSessionContent b = service.create(USER, RECORD, 0, null);

        assertNotEquals(a.getSessionId(), b.getSessionId());
        assertNotEquals(a.getPlayback().getStream(), b.getPlayback().getStream());
        assertEquals(APP, a.getPlayback().getApp());
        assertTrue(a.getPlayback().getStream().matches("session_[0-9a-f]{32}"));
        assertEquals(DURATION, a.getDurationMs());
        assertEquals(SERVER, a.getServerId());
        verify(zlm, times(2)).loadMP4File(eq(node), eq(APP), anyString(), eq(FILE), eq(0L));
        verify(mapper, times(2)).queryOne(RECORD);
        verifyNoMoreInteractions(mapper);
    }

    /** B的拖动、暂停、恢复、关闭只能操作B的媒体源，A的地址和期限均保持不变。 */
    @Test
    void seekPauseResumeAndCloseDoNotChangeOtherViewer() {
        CloudRecordSessionContent a = service.create(USER, RECORD, 0, null);
        CloudRecordSessionContent b = service.create(USER, RECORD, 0, null);
        String aStream = a.getPlayback().getStream();
        String bInitial = b.getPlayback().getStream();
        CloudRecordSessionContent sought = service.seek(USER, b.getSessionId(), 12_000L);
        String bSought = sought.getPlayback().getStream();
        assertNotEquals(bInitial, bSought);

        CloudRecordSessionContent paused = service.pause(USER, b.getSessionId(), 15_000L);
        assertEquals("paused", paused.getState());
        assertNull(paused.getPlayback());
        assertEquals(15_000L, paused.getPositionMs());
        CloudRecordSessionContent resumed = service.resume(USER, b.getSessionId());
        assertEquals("playing", resumed.getState());
        String bResumed = resumed.getPlayback().getStream();
        assertNotEquals(bSought, bResumed);
        assertEquals(15_000L, resumed.getPositionMs());
        assertEquals(bResumed, service.resume(USER, b.getSessionId()).getPlayback().getStream());
        service.close(USER, b.getSessionId());
        service.close(USER, b.getSessionId());

        CloudRecordSessionContent unchanged = service.get(USER, a.getSessionId());
        assertEquals(aStream, unchanged.getPlayback().getStream());
        assertEquals(a.getExpiresAt(), unchanged.getExpiresAt());
        verify(zlm).closeStreams(node, APP, bInitial);
        verify(zlm).closeStreams(node, APP, bSought);
        verify(zlm).closeStreams(node, APP, bResumed);
        verify(zlm, never()).closeStreams(node, APP, aStream);
        verify(zlm).loadMP4File(node, APP, bSought, FILE, 12_000L);
        verify(zlm).loadMP4File(node, APP, bResumed, FILE, 15_000L);
        assertCode(404, () -> service.get(USER, b.getSessionId()));
    }

    /** 暂停中的拖动只更新保存位置，恢复时才重新占用媒体资源。 */
    @Test
    void seekWhilePausedDoesNotLoadMediaUntilResume() {
        CloudRecordSessionContent created = service.create(USER, RECORD, 0, null);
        service.pause(USER, created.getSessionId(), 3_000L);
        clearInvocations(zlm);

        CloudRecordSessionContent sought = service.seek(USER, created.getSessionId(), 9_000L);
        assertEquals("paused", sought.getState());
        assertNull(sought.getPlayback());
        assertEquals(9_000L, sought.getPositionMs());
        verifyNoInteractions(zlm);
        CloudRecordSessionContent resumed = service.resume(USER, created.getSessionId());
        verify(zlm).loadMP4File(node, APP, resumed.getPlayback().getStream(), FILE, 9_000L);
    }

    /** 关闭失败不能虚报暂停，下一次暂停应使用同一真实流名重试。 */
    @Test
    void pauseCloseFailureKeepsOriginalIdentityForRetry() {
        CloudRecordSessionContent created = service.create(USER, RECORD, 0, null);
        String stream = created.getPlayback().getStream();
        when(zlm.closeStreams(node, APP, stream)).thenReturn(closeResult(-1), closeResult(0));

        assertCode(486, () -> service.pause(USER, created.getSessionId(), 7_000L));
        CloudRecordSessionContent stillPlaying = service.get(USER, created.getSessionId());
        assertEquals("playing", stillPlaying.getState());
        assertEquals(stream, stillPlaying.getPlayback().getStream());
        assertEquals(0L, stillPlaying.getPositionMs());
        assertEquals("paused", service.pause(USER, created.getSessionId(), 7_000L).getState());
        verify(zlm, times(2)).closeStreams(node, APP, stream);
    }

    /** 主动关闭失败保留会话，重试成功后重复关闭不会重复外呼。 */
    @Test
    void closeFailureKeepsSessionUntilSuccessfulRetry() {
        CloudRecordSessionContent created = service.create(USER, RECORD, 0, null);
        String stream = created.getPlayback().getStream();
        when(zlm.closeStreams(node, APP, stream)).thenReturn(closeResult(-1), closeResult(0));

        assertCode(486, () -> service.close(USER, created.getSessionId()));
        assertEquals(stream, service.get(USER, created.getSessionId()).getPlayback().getStream());
        service.close(USER, created.getSessionId());
        service.close(USER, created.getSessionId());
        verify(zlm, times(2)).closeStreams(node, APP, stream);
        assertCode(404, () -> service.get(USER, created.getSessionId()));
    }

    /** 加载错误仅返回既有错误分类，按服务端唯一身份清理且不暴露ZLM原始路径。 */
    @Test
    void failedLoadClosesExactSourceAndSanitizesError() {
        ZLMResult<Object> failed = result(-1);
        failed.setMsg("open failed: /private/source.mp4 secret=synthetic-sensitive");
        doReturn(failed).when(zlm).loadMP4File(eq(node), eq(APP), anyString(), eq(FILE), eq(0L));

        ControllerException error = assertCode(486, () -> service.create(USER, RECORD, 0, null));
        assertEquals(ErrorCode.ERROR486.getMsg(), error.getMsg());
        assertFalse(error.getMsg().contains("private"));
        ArgumentCaptor<String> stream = ArgumentCaptor.forClass(String.class);
        verify(zlm).loadMP4File(eq(node), eq(APP), stream.capture(), eq(FILE), eq(0L));
        verify(zlm).closeStreams(node, APP, stream.getValue());
        verify(media, never()).getStreamInfoByAppAndStream(any(), anyString(), anyString(),
                any(), any(), any(), anyBoolean());
        service.expireSessions();
        verify(zlm, times(1)).closeStreams(node, APP, stream.getValue());
    }

    /** 创建失败且清理暂时失败时，清扫器必须保留原身份并继续重试。 */
    @Test
    void failedLoadCleanupIsRetriedWithoutForgettingSource() {
        doReturn(result(-1)).when(zlm).loadMP4File(eq(node), eq(APP), anyString(), eq(FILE), eq(0L));
        when(zlm.closeStreams(eq(node), eq(APP), anyString()))
                .thenReturn(closeResult(-1), closeResult(0));

        assertCode(486, () -> service.create(USER, RECORD, 0, null));
        ArgumentCaptor<String> stream = ArgumentCaptor.forClass(String.class);
        verify(zlm).loadMP4File(eq(node), eq(APP), stream.capture(), eq(FILE), eq(0L));
        service.expireSessions();
        service.expireSessions();
        verify(zlm, times(2)).closeStreams(node, APP, stream.getValue());
    }

    /** 未知录像不能触发节点查询、建流或关闭其他媒体。 */
    @Test
    void unknownRecordHasNoMediaSideEffects() {
        when(mapper.queryOne(RECORD)).thenReturn(null);
        assertCode(404, () -> service.create(USER, RECORD, 0, null));
        verifyNoInteractions(media, zlm);
    }

    /** 外实例的录像不能按同一路径在当前实例的节点盲目加载。 */
    @Test
    void foreignInstanceRecordHasNoMediaSideEffects() {
        record.setServerId("other-wvp");
        assertCode(400, () -> service.create(USER, RECORD, 0, null));
        verifyNoInteractions(media, zlm);
    }

    /** 原节点缺失时拒绝，不能选择另一个低负载节点替代文件归属。 */
    @Test
    void missingOriginalNodeDoesNotFallBackOrLoad() {
        when(media.getOne(node.getId())).thenReturn(null);
        assertCode(486, () -> service.create(USER, RECORD, 0, null));
        verifyNoInteractions(zlm);
        verify(media).getOne(node.getId());
        verifyNoMoreInteractions(media);
    }

    /** 离线节点不能接受文件会话。 */
    @Test
    void offlineOriginalNodeDoesNotLoad() {
        node.setStatus(false);
        assertCode(486, () -> service.create(USER, RECORD, 0, null));
        verifyNoInteractions(zlm);
    }

    /** 不支持的媒体节点不能误走ZLM专有文件接口。 */
    @Test
    void unsupportedNodeDoesNotLoad() {
        node.setType("abl");
        assertCode(486, () -> service.create(USER, RECORD, 0, null));
        verifyNoInteractions(zlm);
    }

    /** 负值、文件尾后及uint32溢出的位置必须在建流前拒绝。 */
    @ParameterizedTest
    @ValueSource(longs = {-1L, DURATION, 0x1_0000_0000L, Long.MAX_VALUE})
    void invalidCreatePositionHasNoMediaSideEffects(long position) {
        assertCode(400, () -> service.create(USER, RECORD, position, null));
        verifyNoInteractions(zlm);
    }

    /** 不可信时长不能进入整数转换或导致无界会话期限。 */
    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, 0, -1, 4_294_967_296D})
    void invalidDurationHasNoMediaSideEffects(double duration) {
        record.setTimeLen(duration);
        assertCode(400, () -> service.create(USER, RECORD, 0, null));
        verifyNoInteractions(zlm);
    }

    /** 无文件路径的索引不触发设备导出或媒体加载。 */
    @Test
    void missingStoredFilePathIsRejectedWithoutLoading() {
        record.setFilePath(" ");
        assertCode(400, () -> service.create(USER, RECORD, 0, null));
        verifyNoInteractions(zlm);
    }

    /** 已存在会话的非法定位和暂停不能先关闭正常媒体。 */
    @Test
    void invalidControlPositionDoesNotCloseCurrentStream() {
        CloudRecordSessionContent created = service.create(USER, RECORD, 0, null);
        clearInvocations(zlm);

        assertCode(400, () -> service.seek(USER, created.getSessionId(), -1));
        assertCode(400, () -> service.pause(USER, created.getSessionId(), DURATION));
        assertEquals(created.getPlayback().getStream(),
                service.get(USER, created.getSessionId()).getPlayback().getStream());
        verifyNoInteractions(zlm);
    }

    /** 所有控制都先验证持有人，外用户不能借过期检查关闭别人的媒体。 */
    @Test
    void nonOwnerCannotReadControlOrExpireOtherSession() {
        CloudRecordSessionContent created = service.create(USER, RECORD, 0, null);
        clearInvocations(zlm);
        clock.advance(DURATION + 120_001L);

        assertCode(403, () -> service.get(USER + 1, created.getSessionId()));
        assertCode(403, () -> service.seek(USER + 1, created.getSessionId(), 1));
        assertCode(403, () -> service.pause(USER + 1, created.getSessionId(), 1));
        assertCode(403, () -> service.resume(USER + 1, created.getSessionId()));
        assertCode(403, () -> service.close(USER + 1, created.getSessionId()));
        verifyNoInteractions(zlm);
    }

    /** 任意未知ID只能得到不存在或幂等关闭，不能把输入当作媒体流名使用。 */
    @Test
    void unknownSessionNeverBecomesAMediaSelector() {
        String unknown = "*";
        assertCode(404, () -> service.get(USER, unknown));
        assertCode(404, () -> service.seek(USER, unknown, 1));
        assertCode(404, () -> service.pause(USER, unknown, 1));
        assertCode(404, () -> service.resume(USER, unknown));
        service.close(USER, unknown);
        verifyNoInteractions(zlm, media);
    }

    /** 查询暂停状态不续期，超期恢复必须拒绝且不重新建立媒体。 */
    @Test
    void pausedSessionExpiresWithoutPollingRenewal() {
        CloudRecordSessionContent created = service.create(USER, RECORD, 0, null);
        CloudRecordSessionContent paused = service.pause(USER, created.getSessionId(), 6_000L);
        clock.advance(119_999L);
        assertEquals(paused.getExpiresAt(), service.get(USER, created.getSessionId()).getExpiresAt());
        clearInvocations(zlm);
        clock.advance(1L);

        service.expireSessions();
        assertCode(404, () -> service.resume(USER, created.getSessionId()));
        verifyNoInteractions(zlm);
    }

    /** 期限清理失败应留到下一轮重试，不能释放一个仍在播放的身份。 */
    @Test
    void expiryCloseFailureRetriesSameStream() {
        CloudRecordSessionContent created = service.create(USER, RECORD, 0, null);
        String stream = created.getPlayback().getStream();
        when(zlm.closeStreams(node, APP, stream)).thenReturn(closeResult(-1), closeResult(0));
        clock.advance(DURATION + 120_001L);

        service.expireSessions();
        service.expireSessions();
        verify(zlm, times(2)).closeStreams(node, APP, stream);
        assertCode(404, () -> service.get(USER, created.getSessionId()));
    }

    /** 同会话重建后，旧源注销、其他节点及非主协议事件均不能终止新源。 */
    @Test
    void staleDepartureCannotEndReplacementStream() {
        CloudRecordSessionContent created = service.create(USER, RECORD, 0, null);
        String oldStream = created.getPlayback().getStream();
        CloudRecordSessionContent sought = service.seek(USER, created.getSessionId(), 8_000L);
        String newStream = sought.getPlayback().getStream();
        service.onMediaDeparture(departure(node, APP, oldStream, "rtsp"));
        service.onMediaDeparture(departure(node, "rtp", newStream, "rtsp"));
        service.onMediaDeparture(departure(node, APP, newStream, "rtmp"));
        MediaServer otherNode = new MediaServer();
        otherNode.setId("another-node");
        service.onMediaDeparture(departure(otherNode, APP, newStream, "rtsp"));
        assertEquals(newStream, service.get(USER, created.getSessionId()).getPlayback().getStream());

        service.onMediaDeparture(departure(node, APP, newStream, "rtsp"));
        assertCode(404, () -> service.get(USER, created.getSessionId()));
        verify(zlm, never()).closeStreams(node, APP, newStream);
    }

    /** 文件结束事件只移除对应观看者，另一位观看者仍可继续控制自己的媒体。 */
    @Test
    void eofOnlyEndsMatchingViewer() {
        CloudRecordSessionContent a = service.create(USER, RECORD, 0, null);
        CloudRecordSessionContent b = service.create(USER, RECORD, 0, null);
        service.onMediaDeparture(departure(node, APP, b.getPlayback().getStream(), "rtsp"));

        assertCode(404, () -> service.get(USER, b.getSessionId()));
        assertEquals(a.getPlayback().getStream(), service.get(USER, a.getSessionId()).getPlayback().getStream());
    }

    /** 已取得会话引用但等待关闭锁的定位请求，不能在关闭后复活媒体。 */
    @Test
    void queuedSeekCannotReviveSessionAfterConcurrentClose() throws Exception {
        CloudRecordSessionContent created = service.create(USER, RECORD, 0, null);
        String stream = created.getPlayback().getStream();
        CountDownLatch closing = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(zlm.closeStreams(node, APP, stream)).thenAnswer(invocation -> {
            closing.countDown();
            assertTrue(release.await(3, TimeUnit.SECONDS));
            return closeResult(0);
        });
        FutureTask<Void> close = new FutureTask<>(() -> {
            service.close(USER, created.getSessionId());
            return null;
        });
        FutureTask<CloudRecordSessionContent> seek = new FutureTask<>(
                () -> service.seek(USER, created.getSessionId(), 10_000L));
        Thread closeThread = new Thread(close, "file-session-close-test");
        Thread seekThread = new Thread(seek, "file-session-seek-test");
        try {
            closeThread.start();
            assertTrue(closing.await(2, TimeUnit.SECONDS));
            seekThread.start();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (seekThread.getState() != Thread.State.BLOCKED && System.nanoTime() < deadline) {
                Thread.sleep(1);
            }
            assertEquals(Thread.State.BLOCKED, seekThread.getState(), "定位请求应已取到引用并等待会话锁");
            release.countDown();
            close.get(2, TimeUnit.SECONDS);
            ExecutionException error = assertThrows(ExecutionException.class,
                    () -> seek.get(2, TimeUnit.SECONDS));
            assertInstanceOf(ControllerException.class, error.getCause());
            assertEquals(404, ((ControllerException) error.getCause()).getCode());
            verify(zlm, times(1)).loadMP4File(eq(node), eq(APP), anyString(), eq(FILE), anyLong());
        } finally {
            release.countDown();
            closeThread.join(3_000);
            seekThread.join(3_000);
        }
    }

    /** 停机只释放当前实例仍持有的媒体源，不重复关闭已暂停的源。 */
    @Test
    void shutdownReleasesPlayingSessionsWithoutDeletingSharedFile() {
        CloudRecordSessionContent a = service.create(USER, RECORD, 0, null);
        CloudRecordSessionContent b = service.create(USER, RECORD, 0, null);
        service.pause(USER, b.getSessionId(), 4_000L);
        clearInvocations(zlm);

        service.shutdown();
        verify(zlm).closeStreams(node, APP, a.getPlayback().getStream());
        verifyNoMoreInteractions(zlm);
        verify(mapper, times(2)).queryOne(RECORD);
        verifyNoMoreInteractions(mapper);
        assertCode(404, () -> service.get(USER, a.getSessionId()));
        assertCode(404, () -> service.get(USER, b.getSessionId()));
    }

    /** 创建可配置返回码的媒体响应，避免在断言中依赖原始错误字符串。 */
    private static ZLMResult<Object> result(int code) {
        ZLMResult<Object> result = new ZLMResult<>();
        result.setCode(code);
        return result;
    }

    /** 创建关闭响应，只以协议返回码决定清理是否确认成功。 */
    private static ZLMResult<FlagData> closeResult(int code) {
        ZLMResult<FlagData> result = new ZLMResult<>();
        result.setCode(code);
        return result;
    }

    /** 构造指定节点和媒体身份的离开事件，模拟EOF和迟到回调。 */
    private static MediaDepartureEvent departure(MediaServer node, String app, String stream, String schema) {
        MediaDepartureEvent event = new MediaDepartureEvent("synthetic-test");
        event.setMediaServer(node);
        event.setApp(app);
        event.setStream(stream);
        event.setSchema(schema);
        return event;
    }

    /** 同时断言拒绝动作的异常类型及既有协议错误分类。 */
    private static ControllerException assertCode(int code, org.junit.jupiter.api.function.Executable action) {
        ControllerException error = assertThrows(ControllerException.class, action);
        assertEquals(code, error.getCode());
        return error;
    }

    /** 只推进测试时钟，使暂停和失联边界不依赖真实等待。 */
    private static final class MutableClock extends Clock {
        private final AtomicLong time;

        /** 从固定Unix毫秒起点建立确定性时钟。 */
        private MutableClock(long initial) {
            time = new AtomicLong(initial);
        }

        /** 原子推进时间，供清扫器和并发请求读取同一时间线。 */
        private void advance(long millis) {
            time.addAndGet(millis);
        }

        /** 使用UTC防止测试依赖运行主机时区。 */
        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        /** 本服务只读取毫秒值，切换展示时区无需改变底层时间线。 */
        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        /** 返回可推进时钟的当前时刻。 */
        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(time.get());
        }

        /** 返回与Instant一致的Unix毫秒。 */
        @Override
        public long millis() {
            return time.get();
        }
    }
}
