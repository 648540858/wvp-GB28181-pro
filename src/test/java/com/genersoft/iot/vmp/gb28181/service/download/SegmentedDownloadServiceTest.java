package com.genersoft.iot.vmp.gb28181.service.download;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IDialogTerminationService;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.DownloadFileInfo;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.genersoft.iot.vmp.gb28181.service.download.SegmentedDownloadTask.State;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 验证父任务与物理子段的可观察状态，不启动定时线程、网络、工具进程或真实设备。 */
@SuppressWarnings("unchecked")
class SegmentedDownloadServiceTest {
    private static final String DEVICE = "99999999999999999991";
    private static final String CHANNEL = "99999999999999999992";
    private static final String START = "2026-10-27 10:00:00";
    private static final String END = "2026-10-27 10:10:00";
    private static final String PARENT = DEVICE + "_" + CHANNEL + "_20261027100000_20261027101000";
    private final SegmentedDownloadStore store = mock(SegmentedDownloadStore.class);
    private final NativeDownloadGateway gateway = mock(NativeDownloadGateway.class);
    private final IDialogTerminationService termination = mock(IDialogTerminationService.class);
    private final IDeviceService devices = mock(IDeviceService.class);
    private final IDeviceChannelService channels = mock(IDeviceChannelService.class);
    private final IMediaServerService mediaServers = mock(IMediaServerService.class);
    private final SipInviteSessionManager sessions = mock(SipInviteSessionManager.class);
    private final SegmentedArtifactFiles files = mock(SegmentedArtifactFiles.class);
    private final Map<String, String> durable = new ConcurrentHashMap<>();
    private final List<NativeAttempt> attempts = new CopyOnWriteArrayList<>();
    private final SegmentedDownloadSettings settings = new SegmentedDownloadSettings();
    private final UserSetting userSetting = new UserSetting();
    private Device device;
    private DeviceChannel channel;
    private MediaServer media;
    private SegmentedDownloadService service;
    private Consumer<SegmentedDownloadTask> afterSave;
    private int forcedSaveConflicts;
    private Consumer<SegmentedDownloadTask> beforeConflict;
    private Boolean nativeAttempted;
    private Consumer<NativeAttempt> afterNativeStarted;

    /** 返回独立JSON快照并执行真实身份/版本条件，原生回调由测试显式投递。 */
    @BeforeEach
    void setUp() {
        userSetting.setServerId("synthetic-server");
        settings.getDevices().add(DEVICE);
        settings.setPublicBaseUrl("https://example.invalid/record-download");
        device = new Device();device.setDeviceId(DEVICE);device.setServerId(userSetting.getServerId());
        channel = new DeviceChannel();channel.setId(27);channel.setDeviceId(CHANNEL);channel.setDataDeviceId(1);
        media = new MediaServer();media.setId("synthetic-media");
        when(devices.getDeviceByDeviceId(DEVICE)).thenReturn(device);
        when(channels.getOneForSourceById(27)).thenReturn(channel);
        when(channels.getOneForSource(DEVICE, CHANNEL)).thenReturn(channel);
        when(mediaServers.getOne(media.getId())).thenReturn(media);
        when(sessions.getAll()).thenReturn(List.of());
        when(store.get(anyString())).thenAnswer(call -> read(call.getArgument(0)));
        when(store.list()).thenAnswer(call -> durable.values().stream()
                .map(json -> JSON.parseObject(json, SegmentedDownloadTask.class)).toList());
        when(store.reserve(any())).thenAnswer(call -> {
            SegmentedDownloadTask candidate = copy(call.getArgument(0));
            SegmentedDownloadTask current = read(candidate.getStream());
            if (current != null && (current.isBusy() || current.getState() == State.COMPLETED)) return current;
            if (durable.values().stream().map(json -> JSON.parseObject(json, SegmentedDownloadTask.class))
                    .anyMatch(other -> Objects.equals(other.getDeviceId(), candidate.getDeviceId()) && other.occupiesDevice())) {
                throw new IllegalStateException("synthetic device busy");
            }
            candidate.setVersion(1);
            durable.put(candidate.getStream(), JSON.toJSONString(candidate));
            return copy(candidate);
        });
        when(store.save(any(), anyLong())).thenAnswer(call -> {
            SegmentedDownloadTask candidate = call.getArgument(0);
            long expected = call.getArgument(1);
            SegmentedDownloadTask current = read(candidate.getStream());
            if (current == null || !Objects.equals(current.getId(), candidate.getId()) || current.getVersion() != expected) return false;
            if (forcedSaveConflicts > 0) {
                forcedSaveConflicts--;
                Consumer<SegmentedDownloadTask> competing = beforeConflict;
                beforeConflict = null;
                if (competing != null) competing.accept(copy(candidate));
                return false;
            }
            candidate.setVersion(expected + 1);candidate.setUpdatedAt(System.currentTimeMillis());
            durable.put(candidate.getStream(), JSON.toJSONString(candidate));
            Consumer<SegmentedDownloadTask> observer = afterSave;
            if (observer != null) observer.accept(copy(candidate));
            return true;
        });
        doAnswer(call -> {
            String requestedStart = call.getArgument(3);
            String requestedEnd = call.getArgument(4);
            SegmentedDownloadTask saved = durable.values().stream()
                    .map(json -> JSON.parseObject(json, SegmentedDownloadTask.class))
                    .filter(task -> task.getState() == State.STARTING && task.getChildStream() != null)
                    .filter(task -> task.getChildStream().endsWith(requestedStart.replaceAll("[-: ]", "")
                            + "_" + requestedEnd.replaceAll("[-: ]", "")))
                    .findFirst().orElseThrow();
            assertEquals(State.STARTING, saved.getState(), "必须先持久化STARTING再发送INVITE");
            assertFalse(saved.isSafeToStart());
            NativeAttempt attempt = new NativeAttempt();
            attempt.start = call.getArgument(3);attempt.end = call.getArgument(4);
            attempt.dialog = call.getArgument(6);attempt.result = call.getArgument(7);
            attempt.stream = saved.getChildStream();
            attempts.add(attempt);
            if (afterNativeStarted != null) afterNativeStarted.accept(attempt);
            return nativeAttempted;
        }).when(gateway).startNativeDownload(any(), any(), any(), anyString(), anyString(), anyInt(), any(), any());
        service = new SegmentedDownloadService(settings, store, gateway, termination, devices, channels,
                mediaServers, sessions, files, userSetting);
    }

    /** 确保直接构造的服务不会在测试后留下工作线程。 */
    @AfterEach
    void tearDown() {
        service.shutdown();
    }

    /** 记录一次真实子段请求的时间与两个独立回调，便于控制200应答和媒体事件顺序。 */
    private static class NativeAttempt {
        private String stream;
        private String start;
        private String end;
        private Consumer<SsrcTransaction> dialog;
        private ErrorCallback<StreamInfo> result;
    }

    /** 按持久化协议返回深快照，避免共享Java引用掩盖CAS和迟到回调错误。 */
    private SegmentedDownloadTask copy(SegmentedDownloadTask task) {
        return JSON.parseObject(JSON.toJSONString(task), SegmentedDownloadTask.class);
    }

    /** 读取指定父任务的最新持久化快照。 */
    private SegmentedDownloadTask read(String stream) {
        return durable.containsKey(stream) ? JSON.parseObject(durable.get(stream), SegmentedDownloadTask.class) : null;
    }

    /** 读取本测试当前父任务，调用方修改不会自动写回。 */
    private SegmentedDownloadTask current() {
        return read(PARENT);
    }

    /** 模拟另一次已提交的持久化更新，便于推进时间条件或替换任务身份。 */
    private void rewrite(Consumer<SegmentedDownloadTask> update) {
        SegmentedDownloadTask task = current();
        update.accept(task);
        task.setVersion(task.getVersion() + 1);
        durable.put(PARENT, JSON.toJSONString(task));
    }

    /** 通过公开开始接口取得逻辑父任务，不等待任何物理建流。 */
    private StreamInfo start() {
        StreamInfo[] response = new StreamInfo[1];
        service.start(device, channel, media, START, END, 1, (code, message, info) -> {
            assertEquals(0, code);
            response[0] = info;
        });
        assertNotNull(response[0]);
        return response[0];
    }

    /** 推进一次已排队任务，返回新产生的原生请求回调。 */
    private NativeAttempt startFirstChild() {
        start();
        service.advance(PARENT);
        assertEquals(1, attempts.size());
        return attempts.get(0);
    }

    /** 构造完整的原始Dialog身份，不从流名或空会话表猜测设备已经释放。 */
    private SsrcTransaction dialog(NativeAttempt attempt) {
        SsrcTransaction dialog = new SsrcTransaction();
        dialog.setDeviceId(DEVICE);dialog.setChannelId(channel.getId());dialog.setCallId("call-" + attempts.size());
        dialog.setStream(attempt.stream);dialog.setApp("rtp");dialog.setSsrc("1234567890");
        dialog.setMediaServerId(media.getId());dialog.setType(InviteSessionType.DOWNLOAD);
        SipTransactionInfo sip = new SipTransactionInfo();
        sip.setCallId(dialog.getCallId());sip.setFromTag("synthetic-from");sip.setToTag("synthetic-to");sip.setViaBranch("synthetic-branch");
        dialog.setSipTransactionInfo(sip);
        return dialog;
    }

    /** 构造已接收子段，完整结果拥有文件地址，中断结果不借完整字段伪造成功。 */
    private StreamInfo child(NativeAttempt attempt, String status) {
        StreamInfo info = new StreamInfo();
        info.setStream(attempt.stream);info.setApp("rtp");info.setStartTime(attempt.start);info.setEndTime(attempt.end);
        info.setDownloadStatus(status);info.setMediaServer(media);
        double seconds = Duration.between(LocalDateTime.parse(attempt.start.replace(' ', 'T')),
                LocalDateTime.parse(attempt.end.replace(' ', 'T'))).getSeconds();
        info.setDownloadedDuration("completed".equals(status) ? seconds : seconds / 2);
        info.setProgress("completed".equals(status) ? 1 : 0.5);
        if ("completed".equals(status)) {
            DownloadFileInfo file = new DownloadFileInfo();file.setHttpsPath("https://example.invalid/child.mp4");
            info.setDownLoadFilePath(file);
        }
        return info;
    }

    /** 固定本段结果供观察和关闭阶段重复读取，不隐式启动新的下载。 */
    private void nativeResult(NativeAttempt attempt, String status) {
        when(gateway.getNativeDownloadInfo(device, channel, attempt.stream)).thenReturn(child(attempt, status));
    }

    /** 普通设备默认完全不进入分段策略。 */
    @Test
    void unconfiguredDeviceRemainsNative() {
        assertFalse(service.isEnabled("normal-hikvision-device"));
        assertTrue(service.isEnabled(DEVICE));
        verifyNoInteractions(gateway, store);
    }

    /** 开始立即返回原区间父身份，重复申请只复用一个任务，尚未执行子段INVITE。 */
    @Test
    void startReturnsStableParentBeforeAnyNativeInvite() {
        StreamInfo response = start();
        String id = current().getId();
        StreamInfo reused = start();
        assertEquals(PARENT, response.getStream());
        assertEquals(PARENT, reused.getStream());
        assertEquals(id, current().getId());
        assertEquals(START, response.getStartTime());assertEquals(END, response.getEndTime());
        assertEquals(0, response.getProgress());
        assertNotNull(response.getMediaServer());
        assertEquals(media.getId(), new StreamContent(response).getMediaServerId());
        assertNull(response.getMediaServer().getSecret());
        assertNull(response.getMediaServer().getIp());
        assertNull(response.getDownLoadFilePath());
        assertEquals(1, durable.size());
        assertEquals(State.QUEUED, current().getState());
        verifyNoInteractions(gateway);
    }



    /** 失败或取消的安全终态被同区间新任务替换之前，只回收原UUID的文件并保留新任务目录。 */
    @Test
    void safeFailedAndCancelledReplacementCleansOnlyPreviousIdentity() {
        start();
        List<String> cleaned = new ArrayList<>();
        doAnswer(call -> {
            SegmentedDownloadTask old = call.getArgument(0);
            assertEquals(old.getId(), current().getId(), "旧目录必须在Hash换轮前清理");
            assertTrue(old.isTerminal());assertTrue(old.isSafeToStart());
            cleaned.add(old.getId());
            return null;
        }).when(files).cleanup(any());
        for (State terminal : List.of(State.FAILED, State.CANCELLED)) {
            String previousId = current().getId();
            rewrite(task -> { task.setState(terminal);task.setSafeToStart(true); });
            start();
            String replacementId = current().getId();
            assertNotEquals(previousId, replacementId);
            assertEquals(State.QUEUED, current().getState());
            assertTrue(cleaned.contains(previousId));
            assertFalse(cleaned.contains(replacementId));
            verify(files, times(1)).cleanup(argThat(task -> Objects.equals(previousId, task.getId())));
        }
        assertEquals(2, cleaned.size());
        verifyNoInteractions(gateway);
    }

    /** 完成成品失效后先把旧任务降为安全失败并回收其目录，之后才能以新UUID重新申请。 */
    @Test
    void expiredCompletedReplacementReclaimsOldArtifactBeforeReserve() {
        start();
        String previousId = current().getId();
        DownloadFileInfo finalFile = new DownloadFileInfo();finalFile.setHttpsPath("https://example.invalid/expired.mp4");
        rewrite(task -> { task.setState(State.COMPLETED);task.setSafeToStart(true);task.setFinalFile(finalFile); });
        doAnswer(call -> {
            SegmentedDownloadTask old = call.getArgument(0);
            assertEquals(previousId, old.getId());
            assertEquals(previousId, current().getId());
            assertEquals(State.FAILED, current().getState());
            return null;
        }).when(files).cleanup(any());
        start();
        String replacementId = current().getId();
        assertNotEquals(previousId, replacementId);
        assertEquals(State.QUEUED, current().getState());
        assertNull(current().getFinalFile());
        verify(files).cleanup(argThat(task -> Objects.equals(previousId, task.getId())));
        verify(files, never()).cleanup(argThat(task -> Objects.equals(replacementId, task.getId())));
    }

    /** 正在执行的任务与仍有效的完整成品都直接复用，不能因重复开始而删除其文件。 */
    @Test
    void busyAndValidCompletedReuseNeverCleansFiles() {
        start();
        String previousId = current().getId();
        start();
        assertEquals(previousId, current().getId());
        verify(files, never()).cleanup(any());
        DownloadFileInfo finalFile = new DownloadFileInfo();finalFile.setHttpsPath("https://example.invalid/complete.mp4");
        rewrite(task -> { task.setState(State.COMPLETED);task.setSafeToStart(true);task.setFinalFile(finalFile); });
        when(files.resolve(anyString())).thenReturn(Path.of("/synthetic/verified.mp4"));
        StreamInfo reused = start();
        assertEquals(previousId, current().getId());
        assertEquals("completed", reused.getDownloadStatus());
        assertEquals(finalFile, reused.getDownLoadFilePath());
        verify(files, never()).cleanup(any());
        verifyNoInteractions(gateway);
    }

    /** 持久化不可用时开始必须明确失败，不能用只存在内存的父身份启动设备。 */
    @Test
    void unavailableReservationNeverInvitesDevice() {
        doThrow(new IllegalStateException("synthetic unavailable")).when(store).reserve(any());
        int[] responseCode = new int[1];
        service.start(device, channel, media, START, END, 1, (code, message, info) -> {
            responseCode[0] = code;
            assertNull(info);
        });
        assertNotEquals(0, responseCode[0]);
        assertTrue(durable.isEmpty());
        verifyNoInteractions(gateway);
    }

    /** 第一段只在advance后启动，且200身份在媒体回调之前已写入持久化快照。 */
    @Test
    void advancePersistsStartingAndDialogBeforeMediaCallback() {
        NativeAttempt attempt = startFirstChild();
        assertEquals(START, attempt.start);
        assertEquals("2026-10-27 10:01:00", attempt.end);
        SsrcTransaction original = dialog(attempt);
        attempt.dialog.accept(original);
        assertEquals(original.getSipTransactionInfo().getCallId(), current().getDialog().getSipTransactionInfo().getCallId());
        assertEquals(original.getSsrc(), current().getLastDialog().getSsrc());
        assertEquals(State.STARTING, current().getState());
        attempt.result.run(0, "success", child(attempt, "downloading"));
        service.advance(PARENT);
        assertEquals(State.DOWNLOADING, current().getState());
        assertEquals(1, attempts.size());
    }

    /** 短段已经完成也必须同时取得原Dialog关闭凭据与媒体消失证明，才允许下一段。 */
    @Test
    void completedChildNeedsReceiptAndMediaGoneBeforeNextSegment() {
        NativeAttempt attempt = startFirstChild();
        SsrcTransaction original = dialog(attempt);attempt.dialog.accept(original);
        attempt.result.run(0, "success", child(attempt, "downloading"));
        nativeResult(attempt, "completed");
        service.advance(PARENT);
        assertEquals(State.CLOSING, current().getState());
        service.advance(PARENT);
        assertFalse(current().isSafeToStart());
        assertEquals(0, current().getSegmentIndex());
        verify(gateway).stopNativeDownload(eq(device), eq(channel), argThat(value -> Objects.equals(original.getCallId(), value.getCallId())));
        when(termination.isDeviceRecordDialogTerminated(any())).thenReturn(true);
        service.advance(PARENT);
        assertEquals(State.CLOSING, current().getState());
        assertEquals(0, current().getSegmentIndex());
        when(gateway.isNativeDownloadMediaGone(media, attempt.stream)).thenReturn(true);
        service.advance(PARENT);
        assertEquals(State.QUEUED, current().getState());
        assertTrue(current().isSafeToStart());
        assertEquals(1, current().getSegments().size());
        assertEquals(1, current().getSegmentIndex());
        assertEquals(1, attempts.size());
        rewrite(task -> task.setNextActionAt(0));
        service.advance(PARENT);
        assertEquals(2, attempts.size());
        assertEquals("2026-10-27 10:00:50", attempts.get(1).start);
    }

    /** 未确认的结束即使超时也保持占用；后续确认只能解除占用，不自动重试失败任务。 */
    @Test
    void unknownCloseTimeoutNeverRetriesOrFreesDevicePrematurely() {
        NativeAttempt attempt = startFirstChild();attempt.dialog.accept(dialog(attempt));
        attempt.result.run(0, "success", child(attempt, "downloading"));
        nativeResult(attempt, "interrupted");
        service.advance(PARENT);
        rewrite(task -> task.setNextActionAt(0));
        service.advance(PARENT);
        assertEquals(State.FAILED, current().getState());
        assertFalse(current().isSafeToStart());
        assertThrows(ControllerException.class, () -> service.progress(device, channel, PARENT));
        service.advance(PARENT);
        assertEquals(1, attempts.size());
        when(termination.isDeviceRecordDialogTerminated(any())).thenReturn(true);
        when(gateway.isNativeDownloadMediaGone(media, attempt.stream)).thenReturn(true);
        service.advance(PARENT);
        assertEquals(State.FAILED, current().getState());
        assertTrue(current().isSafeToStart());
        service.advance(PARENT);
        assertEquals(1, attempts.size());
    }

    /** 原200身份缺失时不能把空媒体列表当成安全释放，更不能重新发送子段INVITE。 */
    @Test
    void nullAttemptedStartWithoutIdentityStaysUnsafeWithoutRetry() {
        NativeAttempt attempt = startFirstChild();
        attempt.result.run(-1, "synthetic unknown", null);
        rewrite(task -> task.setNextActionAt(0));
        service.advance(PARENT);
        assertEquals(State.FAILED, current().getState());
        assertFalse(current().isSafeToStart());
        assertEquals(1, attempts.size());
        verify(gateway, never()).isNativeDownloadMediaGone(any(), anyString());
    }


    /** 只有明确FALSE证明未执行SIP发送时，分配失败才能安全退避，并在三次后释放逻辑名额。 */
    @Test
    void provenUnsentAllocationFailureRetriesThreeTimesThenFailsSafely() {
        doAnswer(call -> {
            ErrorCallback<StreamInfo> callback = call.getArgument(7);
            callback.run(-1, "synthetic RTP allocation failure", null);
            return Boolean.FALSE;
        }).when(gateway).startNativeDownload(any(), any(), any(), anyString(), anyString(), anyInt(), any(), any());
        start();
        for (int index = 1; index <= 3; index++) {
            service.advance(PARENT);
            assertEquals(index, current().getAttempt());
            assertTrue(current().isSafeToStart());
            assertNull(current().getDialog());
            verify(gateway, times(index)).startNativeDownload(any(), any(), any(), anyString(), anyString(), anyInt(), any(), any());
            if (index < 3) {
                assertEquals(State.RETRY_WAIT, current().getState());
                long remaining = current().getNextActionAt() - System.currentTimeMillis();
                assertTrue(remaining > 0 && remaining <= 15000);
                service.advance(PARENT);
                verify(gateway, times(index)).startNativeDownload(any(), any(), any(), anyString(), anyString(), anyInt(), any(), any());
                rewrite(task -> task.setNextActionAt(0));
            }
        }
        assertEquals(State.FAILED, current().getState());
        service.advance(PARENT);
        verify(gateway, times(3)).startNativeDownload(any(), any(), any(), anyString(), anyString(), anyInt(), any(), any());
        verify(gateway, never()).stopNativeDownload(any(), any(), any());
        verify(gateway, never()).isNativeDownloadMediaGone(any(), anyString());
        verifyNoInteractions(termination);
        verify(files, never()).publish(any(), any());
    }

    /** 未发送证明返回前收到取消时直接安全结束，不为不存在的Dialog发BYE或继续建段。 */
    @Test
    void cancelDuringProvenUnsentStartEndsSafelyWithoutBye() {
        doAnswer(call -> {
            ErrorCallback<StreamInfo> callback = call.getArgument(7);
            callback.run(-1, "synthetic RTP allocation failure", null);
            service.cancel(device, channel, PARENT);
            return Boolean.FALSE;
        }).when(gateway).startNativeDownload(any(), any(), any(), anyString(), anyString(), anyInt(), any(), any());
        start();
        service.advance(PARENT);
        assertTrue(current().isCancelRequested());
        assertTrue(current().isSafeToStart());
        assertEquals(State.CANCELLED, current().getState());
        assertNull(current().getDialog());
        service.advance(PARENT);
        verify(gateway, times(1)).startNativeDownload(any(), any(), any(), anyString(), anyString(), anyInt(), any(), any());
        verify(gateway, never()).stopNativeDownload(any(), any(), any());
        verifyNoInteractions(termination);
    }

    /** TRUE但未取得200身份时，即使收到同样分配失败文案也不能猜测请求尚未发送。 */
    @Test
    void attemptedStartWithoutIdentityRemainsUnsafeAfterFailure() {
        nativeAttempted = Boolean.TRUE;
        NativeAttempt attempt = startFirstChild();
        attempt.result.run(-1, "synthetic RTP allocation failure", null);
        rewrite(task -> task.setNextActionAt(0));
        service.advance(PARENT);
        assertEquals(State.FAILED, current().getState());
        assertFalse(current().isSafeToStart());
        assertNull(current().getDialog());
        service.advance(PARENT);
        assertEquals(1, attempts.size());
        verify(gateway, never()).stopNativeDownload(any(), any(), any());
        verifyNoInteractions(termination);
    }

    /** 三次重试分别保留有限退避，只有已确认原会话结束才创建下一尝试。 */
    @Test
    void confirmedInterruptedChildRetriesAtMostThreeTimesWithBackoff() {
        NativeAttempt attempt = startFirstChild();
        when(termination.isDeviceRecordDialogTerminated(any())).thenReturn(true);
        when(gateway.isNativeDownloadMediaGone(eq(media), anyString())).thenReturn(true);
        for (int index = 1; index <= 3; index++) {
            attempt = attempts.get(index - 1);
            attempt.dialog.accept(dialog(attempt));
            attempt.result.run(0, "success", child(attempt, "downloading"));
            nativeResult(attempt, "interrupted");
            service.advance(PARENT);
            service.advance(PARENT);
            assertTrue(current().isSafeToStart());
            if (index < 3) {
                assertEquals(State.RETRY_WAIT, current().getState());
                assertTrue(current().getNextActionAt() > System.currentTimeMillis());
                assertTrue(current().getNextActionAt() <= System.currentTimeMillis() + 15000);
                service.advance(PARENT);
                assertEquals(index, attempts.size());
                rewrite(task -> task.setNextActionAt(0));
                service.advance(PARENT);
            }
        }
        assertEquals(State.FAILED, current().getState());
        assertEquals(3, current().getAttempt());
        service.advance(PARENT);
        assertEquals(3, attempts.size());
        verify(files, never()).publish(any(), any());
    }

    /** 取消后迟到200仍须留下原身份用于关闭，但迟到媒体成功不能复活任务或继续下一段。 */
    @Test
    void cancellationKeepsLateDialogOnlyForClosingAndNeverResurrects() {
        NativeAttempt attempt = startFirstChild();
        service.cancel(device, channel, PARENT);
        assertTrue(current().isCancelRequested());
        assertEquals(State.CLOSING, current().getState());
        attempt.dialog.accept(dialog(attempt));
        attempt.result.run(0, "success", child(attempt, "completed"));
        assertEquals(State.CLOSING, current().getState());
        when(termination.isDeviceRecordDialogTerminated(any())).thenReturn(true);
        when(gateway.isNativeDownloadMediaGone(media, attempt.stream)).thenReturn(true);
        service.advance(PARENT);
        assertEquals(State.CANCELLED, current().getState());
        assertTrue(current().isSafeToStart());
        attempt.result.run(0, "late-success", child(attempt, "completed"));
        service.advance(PARENT);
        assertEquals(State.CANCELLED, current().getState());
        assertEquals(1, attempts.size());
        verify(files, never()).publish(any(), any());
    }

    /** STARTING已落盘而原生建流尚未开始时取消，不得再依据旧快照发送INVITE。 */
    @Test
    void cancelBetweenStartingCheckpointAndNativeStartPreventsInvite() {
        start();
        doReturn(null).when(gateway).startNativeDownload(any(), any(), any(), anyString(), anyString(), anyInt(), any(), any());
        afterSave = task -> {
            if (task.getState() == State.STARTING) {
                afterSave = null;
                service.cancel(device, channel, PARENT);
            }
        };
        service.advance(PARENT);
        assertTrue(current().isCancelRequested());
        assertTrue(attempts.isEmpty(), "已持久化的取消必须阻止尚未执行的INVITE");
        verify(gateway, never()).startNativeDownload(any(), any(), any(), anyString(), anyString(), anyInt(), any(), any());
    }


    /** 模拟原生SIP回调持子锁、轮询持父锁的真实反向顺序，两个线程都必须有限时间退出。 */
    @Test
    void nativeDialogCallbackAndParentObservationCannotDeadlock() throws Exception {
        NativeAttempt attempt = startFirstChild();
        attempt.result.run(0, "success", child(attempt, "downloading"));
        Object nativeLock = new Object();
        CountDownLatch nativeLocked = new CountDownLatch(1);
        CountDownLatch nativeReadEntered = new CountDownLatch(1);
        when(gateway.getNativeDownloadInfo(device, channel, attempt.stream)).thenAnswer(call -> {
            nativeReadEntered.countDown();
            synchronized (nativeLock) { return null; }
        });
        ExecutorService threads = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "segmented-download-lock-regression");
            thread.setDaemon(true);
            return thread;
        });
        try {
            Future<?> callback = threads.submit(() -> {
                synchronized (nativeLock) {
                    nativeLocked.countDown();
                    try { assertTrue(nativeReadEntered.await(2, TimeUnit.SECONDS)); }
                    catch (InterruptedException ex) { Thread.currentThread().interrupt();throw new IllegalStateException(ex); }
                    attempt.dialog.accept(dialog(attempt));
                }
            });
            assertTrue(nativeLocked.await(2, TimeUnit.SECONDS));
            Future<?> observation = threads.submit(() -> service.advance(PARENT));
            callback.get(2, TimeUnit.SECONDS);
            try {
                observation.get(2, TimeUnit.SECONDS);
            } catch (ExecutionException ex) {
                // 回调先写入新版本时，本轮轮询可以因CAS失败退出，但不得阻塞下一轮读取。
                assertInstanceOf(IllegalStateException.class, ex.getCause());
            }
            assertNotNull(current().getDialog());
            assertEquals(dialog(attempt).getSipTransactionInfo().getCallId(), current().getDialog().getSipTransactionInfo().getCallId());
            assertEquals(1, attempts.size());
        } finally {
            threads.shutdownNow();
        }
    }

    /** 200快照与取消竞争写入时，CAS重读仍保留原身份，但不清除取消或复活下载。 */
    @Test
    void dialogCasConflictRechecksConcurrentCancellation() {
        NativeAttempt attempt = startFirstChild();
        forcedSaveConflicts = 1;
        beforeConflict = ignored -> service.cancel(device, channel, PARENT);
        attempt.dialog.accept(dialog(attempt));
        assertTrue(current().isCancelRequested());
        assertEquals(State.CLOSING, current().getState());
        assertNotNull(current().getDialog());
        assertNotNull(current().getLastDialog());
        assertFalse(current().isSafeToStart());
        attempt.result.run(0, "late-success", child(attempt, "completed"));
        assertEquals(State.CLOSING, current().getState());
        assertEquals(1, attempts.size());
    }

    /** 回调CAS冲突后发现新的任务身份就停止，不能把旧Dialog写给替代任务。 */
    @Test
    void dialogCasConflictRejectsReplacementIdentityOnReread() {
        NativeAttempt attempt = startFirstChild();
        forcedSaveConflicts = 1;
        beforeConflict = ignored -> rewrite(task -> {
            task.setId("replacement-id");task.setState(State.QUEUED);task.setSafeToStart(true);
        });
        attempt.dialog.accept(dialog(attempt));
        assertEquals("replacement-id", current().getId());
        assertEquals(State.QUEUED, current().getState());
        assertNull(current().getDialog());
        assertEquals(1, attempts.size());
    }

    /** 取消与媒体回调抢占同一版本时须重新读当前状态重试，不能悄悄丢掉停止请求。 */
    @Test
    void cancellationCasConflictRetriesAfterMediaCallback() {
        NativeAttempt attempt = startFirstChild();attempt.dialog.accept(dialog(attempt));
        forcedSaveConflicts = 1;
        beforeConflict = ignored -> attempt.result.run(0, "success", child(attempt, "downloading"));
        service.cancel(device, channel, PARENT);
        assertTrue(current().isCancelRequested());
        assertEquals(State.CLOSING, current().getState());
        assertNotNull(current().getDialog());
        assertFalse(current().isSafeToStart());
        assertEquals(1, attempts.size());
    }

    /** 回调竞争持续六次必须失败关闭，不能假称原Dialog身份已经持久化。 */
    @Test
    void exhaustedDialogCasRetriesRemainUnconfirmed() {
        NativeAttempt attempt = startFirstChild();
        forcedSaveConflicts = 6;
        assertThrows(IllegalStateException.class, () -> attempt.dialog.accept(dialog(attempt)));
        assertEquals(0, forcedSaveConflicts);
        assertNull(current().getDialog());
        assertEquals(State.STARTING, current().getState());
        assertFalse(current().isSafeToStart());
    }

    /** 新进程接管不确定STARTING只清理原Dialog，不能把未知执行结果重新发起为下载。 */
    @Test
    void restartDoesNotReplayAnUncertainStartingAttempt() {
        NativeAttempt attempt = startFirstChild();attempt.dialog.accept(dialog(attempt));
        rewrite(task -> { task.setWorkerId("previous-worker");task.setUpdatedAt(0); });
        service.advance(PARENT);
        assertTrue(current().isCancelRequested());
        assertEquals(State.CLOSING, current().getState());
        assertEquals(1, attempts.size());
        verify(gateway).stopNativeDownload(any(), any(), any());
    }

    /** 未开始建流的旧工作进程任务恢复为失败，不偷偷继续整个任务。 */
    @Test
    void restartOfQueuedJobFailsSafelyWithoutInvite() {
        start();
        rewrite(task -> { task.setWorkerId("previous-worker");task.setUpdatedAt(0); });
        service.advance(PARENT);
        assertEquals(State.FAILED, current().getState());
        assertTrue(current().isSafeToStart());
        verifyNoInteractions(gateway);
    }

    /** 旧请求的Dialog和媒体回调均不能覆盖已经替代的任务身份。 */
    @Test
    void staleNativeCallbacksCannotOverwriteReplacementJob() {
        NativeAttempt attempt = startFirstChild();
        rewrite(task -> { task.setId("replacement-id");task.setState(State.QUEUED);task.setSafeToStart(true); });
        long version = current().getVersion();
        attempt.dialog.accept(dialog(attempt));
        attempt.result.run(0, "late-success", child(attempt, "completed"));
        assertEquals("replacement-id", current().getId());
        assertEquals(version, current().getVersion());
        assertEquals(State.QUEUED, current().getState());
        assertNull(current().getDialog());
    }

    /** 原关闭检查期间任务被替换时，CAS失败必须保留新任务，不能推进其游标。 */
    @Test
    void staleCloseCannotOverwriteReplacementJob() {
        NativeAttempt attempt = startFirstChild();attempt.dialog.accept(dialog(attempt));
        attempt.result.run(0, "success", child(attempt, "downloading"));
        nativeResult(attempt, "completed");
        service.advance(PARENT);
        when(termination.isDeviceRecordDialogTerminated(any())).thenAnswer(call -> {
            rewrite(task -> { task.setId("replacement-id");task.setState(State.QUEUED);task.setSafeToStart(true); });
            return true;
        });
        when(gateway.isNativeDownloadMediaGone(media, attempt.stream)).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.advance(PARENT));
        assertEquals("replacement-id", current().getId());
        assertEquals(State.QUEUED, current().getState());
        assertEquals(0, current().getSegmentIndex());
    }

    /** 合并期间取消时不能公开文件，也不能把已取消任务写回完成。 */
    @Test
    void cancelDuringAssemblyPreventsPublication() {
        start();
        rewrite(task -> { task.setState(State.MERGING);task.setSegmentIndex(10); });
        when(files.assemble(any(), eq(media), any())).thenAnswer(call -> {
            service.cancel(device, channel, PARENT);
            return Path.of("/synthetic/verified.mp4");
        });
        service.advance(PARENT);
        assertEquals(State.CANCELLED, current().getState());
        verify(files, never()).publish(any(), any());
        assertNull(current().getFinalFile());
    }

    /** 旧合并返回时已出现新任务，不能发布旧成品或覆盖新任务检查点。 */
    @Test
    void staleAssemblyCannotPublishOverReplacementJob() {
        start();
        rewrite(task -> { task.setState(State.MERGING);task.setSegmentIndex(10); });
        when(files.assemble(any(), eq(media), any())).thenAnswer(call -> {
            rewrite(task -> { task.setId("replacement-id");task.setState(State.QUEUED); });
            return Path.of("/synthetic/verified.mp4");
        });
        service.advance(PARENT);
        assertEquals("replacement-id", current().getId());
        assertEquals(State.QUEUED, current().getState());
        verify(files, never()).publish(any(), any());
    }

    /** 已确认关闭的合并任务保留原父身份，但设备名额应立即供下一区间使用。 */
    @Test
    void safeMergingReusesParentButDoesNotOccupyNextDeviceDownload() {
        NativeAttempt oldAttempt = startFirstChild();oldAttempt.dialog.accept(dialog(oldAttempt));
        rewrite(task -> { task.setState(State.MERGING);task.setSafeToStart(true);task.setSegmentIndex(10); });
        String oldId = current().getId();
        assertFalse(service.hasBusyTask(DEVICE, null));
        assertFalse(service.hasBusyTask(DEVICE, channel.getId()));
        assertFalse(service.isChildStream(oldAttempt.stream));
        assertEquals(PARENT, start().getStream());
        assertEquals(oldId, current().getId());
        verify(files, never()).cleanup(any());

        String next = startNextInterval();
        assertEquals(2, durable.size());
        assertEquals(State.MERGING, current().getState());
        assertEquals(oldId, current().getId());
        assertTrue(service.hasBusyTask(DEVICE, null));
        service.cancel(device, channel, PARENT);
        service.advance(next);
        assertEquals(2, attempts.size());
        NativeAttempt nextAttempt = attempts.get(1);
        oldAttempt.dialog.accept(dialog(oldAttempt));
        oldAttempt.result.run(0, "late", child(oldAttempt, "completed"));
        assertEquals(State.CANCELLED, current().getState());
        assertEquals(State.STARTING, read(next).getState());
        assertFalse(read(next).isCancelRequested());
        verify(gateway, never()).stopNativeDownload(any(), any(), argThat(value -> value != null && nextAttempt.stream.equals(value.getStream())));

        SegmentedDownloadTask unsafe = read(next);unsafe.setState(State.FAILED);unsafe.setSafeToStart(false);
        durable.put(next, JSON.toJSONString(unsafe));
        assertTrue(service.hasBusyTask(DEVICE, channel.getId()));
        assertTrue(service.isChildStream(nextAttempt.stream));
    }

    /** 独立合并线程阻塞时，单控制工作线程仍能建下一路；取消旧任务不能停止新会话。 */
    @Test
    void blockedMergeDoesNotStarveSingleControlWorkerOrCancelNextJob() throws Exception {
        service.shutdown();settings.setMaxWorkers(1);
        service = new SegmentedDownloadService(settings, store, gateway, termination, devices, channels,
                mediaServers, sessions, files, userSetting);
        start();
        rewrite(task -> { task.setState(State.MERGING);task.setSafeToStart(true);task.setSegmentIndex(10); });
        CountDownLatch mergeEntered = new CountDownLatch(1);
        CountDownLatch releaseMerge = new CountDownLatch(1);
        CountDownLatch nextStarted = new CountDownLatch(1);
        when(files.assemble(any(), eq(media), any())).thenAnswer(call -> {
            mergeEntered.countDown();
            assertTrue(releaseMerge.await(5, TimeUnit.SECONDS), "测试必须及时释放合并线程");
            return Path.of("/synthetic/verified.mp4");
        });
        afterNativeStarted = attempt -> nextStarted.countDown();
        try {
            service.schedule();
            assertTrue(mergeEntered.await(2, TimeUnit.SECONDS), "合并应已经进入独立工作线程");
            String next = startNextInterval();
            String nextId = read(next).getId();
            service.schedule();
            assertTrue(nextStarted.await(2, TimeUnit.SECONDS), "合并不能占满唯一控制工作线程");
            assertEquals(State.STARTING, read(next).getState());
            service.cancel(device, channel, PARENT);
            assertEquals(State.CANCELLED, current().getState());
            assertEquals(nextId, read(next).getId());
            assertFalse(read(next).isCancelRequested());
            releaseMerge.countDown();
            ThreadPoolExecutor mergePool = (ThreadPoolExecutor) ReflectionTestUtils.getField(service, "mergeExecutor");
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (mergePool.getActiveCount() != 0 && System.nanoTime() < deadline) Thread.sleep(5);
            assertEquals(0, mergePool.getActiveCount(), "旧合并必须已经完成取消后的收口");
            assertEquals(State.CANCELLED, current().getState());
            assertEquals(State.STARTING, read(next).getState());
            assertEquals(nextId, read(next).getId());
            verify(files, never()).publish(any(), any());
            verify(gateway, never()).stopNativeDownload(any(), any(), any());
        } finally {
            releaseMerge.countDown();
        }
    }

    /** 申请相邻原始区间，保持同设备同通道，仅时间范围不同。 */
    private String startNextInterval() {
        StreamInfo[] response = new StreamInfo[1];
        service.start(device, channel, media, END, "2026-10-27 10:20:00", 1, (code, message, info) -> {
            assertEquals(0, code);response[0] = info;
        });
        assertNotNull(response[0]);
        return response[0].getStream();
    }

    /** 成品字段只在COMPLETED对外可见，合并中仍无旧SDK会误用的文件地址。 */
    @Test
    void publicFinalFileAppearsOnlyAfterVerifiedPublication() {
        start();
        DownloadFileInfo finalFile = new DownloadFileInfo();finalFile.setHttpsPath("https://example.invalid/complete.mp4");
        rewrite(task -> { task.setState(State.MERGING);task.setSegmentIndex(10);task.setFinalFile(finalFile); });
        StreamInfo waiting = service.progress(device, channel, PARENT);
        assertNull(waiting.getDownLoadFilePath());
        assertEquals("finalizing", waiting.getDownloadStatus());
        assertTrue(waiting.getProgress() < 1);
        when(files.assemble(any(), eq(media), any())).thenReturn(Path.of("/synthetic/verified.mp4"));
        when(files.publish(any(), any())).thenReturn(finalFile);
        when(files.resolve(anyString())).thenReturn(Path.of("/synthetic/verified.mp4"));
        service.advance(PARENT);
        StreamInfo complete = service.progress(device, channel, PARENT);
        assertEquals("completed", complete.getDownloadStatus());
        assertEquals(1, complete.getProgress());
        assertEquals(finalFile, complete.getDownLoadFilePath());
        service.cancel(device, channel, PARENT);
        assertEquals(State.COMPLETED, current().getState());
        assertNotNull(service.progress(device, channel, PARENT).getDownLoadFilePath());
    }

    /** 找回投影保留原区间父流与真实SSRC；没有真实应答身份前不虚构活动会话。 */
    @Test
    void recoveryViewUsesParentIntervalAndLastRealSsrc() {
        start();
        assertTrue(service.sessionViews().isEmpty());
        service.advance(PARENT);
        NativeAttempt attempt = attempts.get(0);attempt.dialog.accept(dialog(attempt));
        rewrite(task -> { task.setState(State.RETRY_WAIT);task.setSafeToStart(true);task.setDialog(null); });
        List<Map<String, Object>> views = service.sessionViews();
        assertEquals(1, views.size());
        assertEquals(PARENT, views.get(0).get("streamId"));
        assertEquals("1234567890", views.get(0).get("ssrc"));
        assertEquals(DEVICE, views.get(0).get("deviceId"));
        assertEquals(channel.getId(), views.get(0).get("channelId"));
        StreamInfo info = service.progress(device, channel, PARENT);
        assertEquals(START, info.getStartTime());assertEquals(END, info.getEndTime());
        assertEquals(PARENT, info.getStream());
        assertTrue(service.isChildStream(attempt.stream));
        assertFalse(service.isChildStream("unrelated-stream"));
    }
}
