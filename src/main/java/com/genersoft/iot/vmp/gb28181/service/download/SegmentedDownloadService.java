package com.genersoft.iot.vmp.gb28181.service.download;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.enums.MediaStreamUtil;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.RecordDialogCloseReceipt;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IDialogTerminationService;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.genersoft.iot.vmp.gb28181.service.download.SegmentedDownloadTask.State;

/** 在既有下载接口内编排按设备启用的串行子段，只有验证成品才对外完成。 */
@Slf4j
@Service
public class SegmentedDownloadService {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter STREAM_TIME = DateTimeFormatter.ofPattern("uuuuMMddHHmmss");
    private final SegmentedDownloadSettings settings;
    private final SegmentedDownloadStore store;
    private final NativeDownloadGateway nativeDownload;
    private final IDialogTerminationService termination;
    private final IDeviceService devices;
    private final IDeviceChannelService channels;
    private final IMediaServerService mediaServers;
    private final SipInviteSessionManager sessions;
    private final SegmentedArtifactFiles files;
    private final UserSetting userSetting;
    private final String workerId = UUID.randomUUID().toString();
    private final Set<String> running = ConcurrentHashMap.newKeySet();
    private final Object[] locks = java.util.stream.IntStream.range(0, 64).mapToObj(i -> new Object()).toArray();
    private final ThreadPoolExecutor executor;
    private final ThreadPoolExecutor mergeExecutor;

    /** 通过延迟原生适配器避免播放服务与编排服务的初始化循环，执行线程和排队均有上限。 */
    public SegmentedDownloadService(SegmentedDownloadSettings settings, SegmentedDownloadStore store,
                                    @Lazy NativeDownloadGateway nativeDownload, IDialogTerminationService termination,
                                    IDeviceService devices, IDeviceChannelService channels,
                                    IMediaServerService mediaServers, SipInviteSessionManager sessions,
                                    SegmentedArtifactFiles files, UserSetting userSetting) {
        this.settings = settings;
        this.store = store;
        this.nativeDownload = nativeDownload;
        this.termination = termination;
        this.devices = devices;
        this.channels = channels;
        this.mediaServers = mediaServers;
        this.sessions = sessions;
        this.files = files;
        this.userSetting = userSetting;
        int workers = Math.max(1, Math.min(4, settings.getMaxWorkers()));
        this.executor = new ThreadPoolExecutor(workers, workers, 30, TimeUnit.SECONDS,
                new SynchronousQueue<>(), runnable -> {
                    Thread thread = new Thread(runnable, "record-download-segment");
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
        executor.allowCoreThreadTimeOut(true);
        // 本地编码不占用设备会话，也不能堵住后续子段的关闭轮询或其他设备的控制。
        this.mergeExecutor = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS,
                new SynchronousQueue<>(), runnable -> {
                    Thread thread = new Thread(runnable, "record-download-merge");
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
        mergeExecutor.allowCoreThreadTimeOut(true);
    }

    /** 未显式启用的设备完全沿用原生下载，不从一次失败推断设备能力。 */
    public boolean isEnabled(String deviceId) {
        return settings.appliesTo(deviceId);
    }

    /** 校验并持久化父任务后立即返回稳定流名，建流由有限工作线程推进。 */
    public void start(Device device, DeviceChannel channel, MediaServer media, String start, String end,
                      int speed, ErrorCallback<StreamInfo> callback) {
        try {
            settings.validateEnabled();
            LocalDateTime first = LocalDateTime.parse(start, TIME);
            LocalDateTime last = LocalDateTime.parse(end, TIME);
            long seconds = Duration.between(first, last).getSeconds();
            if (seconds <= 0 || seconds > settings.getMaxRequestSeconds() || speed < 1 || speed > 4) {
                throw new IllegalArgumentException("录像区间或下载速度超出分段下载范围");
            }
            String stream = canonicalStream(device.getDeviceId(), channel.getDeviceId(), first, last);
            synchronized (lock(stream)) {
                SegmentedDownloadTask existing = store.get(stream);
                if (existing != null && existing.isTerminal() && existing.isSafeToStart()
                        && existing.getState() == State.COMPLETED && files.resolve(existing.getArtifactToken()) == null) {
                    existing.setState(State.FAILED);
                    existing.setError("成品文件已失效，请重新下载");
                    save(existing);
                }
                if (existing != null && !existing.isBusy() && existing.getState() != State.COMPLETED) {
                    // 替换同区间终态前回收旧UUID的私有文件，否则Hash换轮后失去清理索引。
                    files.cleanup(existing);
                }
                if (existing == null || (!existing.isBusy() && existing.getState() != State.COMPLETED)) {
                    // 不能抢占已有摄像机回放；设备名额既可能被其他用户占用，也可能尚未确认释放。
                    boolean busy = sessions.getAll().stream().anyMatch(session ->
                            Objects.equals(device.getDeviceId(), session.getDeviceId())
                                    && (session.getType() == InviteSessionType.DOWNLOAD
                                    || session.getType() == InviteSessionType.PLAYBACK));
                    if (busy) {
                        throw new IllegalStateException("设备已有录像会话或尚未确认释放");
                    }
                }
                SegmentedDownloadTask candidate = new SegmentedDownloadTask();
                candidate.setId(UUID.randomUUID().toString());
                candidate.setStream(stream);
                candidate.setDeviceId(device.getDeviceId());
                candidate.setChannelId(channel.getId());
                candidate.setServerId(userSetting.getServerId());
                candidate.setWorkerId(workerId);
                candidate.setStartTime(start);
                candidate.setEndTime(end);
                candidate.setMediaServerId(media.getId());
                candidate.setDownloadSpeed(speed);
                candidate.setState(State.QUEUED);
                candidate.setSafeToStart(true);
                candidate.setCreatedAt(System.currentTimeMillis());
                candidate.setUpdatedAt(candidate.getCreatedAt());
                candidate.setDeadlineAt(candidate.getCreatedAt() + settings.getJobTimeoutSeconds() * 1000L);
                byte[] token = new byte[32];
                new SecureRandom().nextBytes(token);
                candidate.setArtifactToken(HexFormat.of().formatHex(token));
                SegmentedDownloadTask task = store.reserve(candidate);
                if (task.isTerminal() && task.getState() != State.COMPLETED) {
                    throw new IllegalStateException("原下载任务尚未确认释放");
                }
                callback.run(0, "success", view(task));
            }
        } catch (RuntimeException ex) {
            // 不把文件路径、内部地址或 Redis 异常详情暴露到旧下载接口。
            callback.run(-1, "分段下载未启动：请检查区间、设备占用与成品出口配置", null);
        }
    }

    /** 父任务只按设备与数据库通道匹配，不能仅靠可猜测流名跨设备查询或停止。 */
    public boolean owns(Device device, DeviceChannel channel, String stream) {
        if (stream == null) {
            return false;
        }
        SegmentedDownloadTask task = store.get(stream);
        return task != null && Objects.equals(device.getDeviceId(), task.getDeviceId())
                && Objects.equals(channel.getId(), task.getChannelId());
    }

    /** 旧 SDK 忽略新增状态，因此失败返回既有业务错误，部分文件从不放入完整字段。 */
    public StreamInfo progress(Device device, DeviceChannel channel, String stream) {
        SegmentedDownloadTask task = store.get(stream);
        requireOwner(task, device, channel);
        if (task.getState() == State.FAILED || task.getState() == State.CANCELLED) {
            throw new ControllerException(-1, task.getState() == State.CANCELLED ? "录像下载已取消" :
                    "录像未通过完整性校验或原会话尚未确认关闭，请检查设备与下载日志");
        }
        if (task.getState() == State.COMPLETED && files.resolve(task.getArtifactToken()) == null) {
            throw new ControllerException(-1, "录像成品已失效，请重新下载");
        }
        return view(task);
    }

    /** 取消持久化后禁止任何下一段，完成文件保留；协议关闭由原身份继续确认。 */
    public void cancel(Device device, DeviceChannel channel, String stream) {
        synchronized (lock(stream)) {
            for (int retry = 0; retry < 6; retry++) {
                SegmentedDownloadTask task = store.get(stream);
                requireOwner(task, device, channel);
                if (task.getState() == State.COMPLETED || task.getState() == State.CANCELLED) {
                    return;
                }
                task.setCancelRequested(true);
                if (task.isSafeToStart()) {
                    task.setState(State.CANCELLED);
                } else {
                    beginClosing(task, "用户取消录像下载");
                }
                if (store.save(task, task.getVersion())) {
                    return;
                }
            }
            throw new ControllerException(-1, "录像任务正在变更，请重试停止");
        }
    }

    /** 找回接口投影原区间和最近真实 SSRC，不把父任务插进 SIP 活动事务表。 */
    public List<Map<String, Object>> sessionViews() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (SegmentedDownloadTask task : store.list()) {
            if (!task.isBusy()) {
                continue;
            }
            SsrcTransaction last = task.getDialog() == null ? task.getLastDialog() : task.getDialog();
            if (last == null || last.getSsrc() == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("deviceId", task.getDeviceId());
            item.put("channelId", task.getChannelId());
            item.put("ssrc", last.getSsrc());
            item.put("streamId", task.getStream());
            item.put("segmentedDownload", true);
            item.put("terminationState", task.getState().name().toLowerCase());
            result.add(item);
        }
        return result;
    }

    /** 设备下载在分段间隙仍占用一个逻辑名额，供既有设备忙碌查询复用。 */
    public boolean hasBusyTask(String deviceId, Integer channelId) {
        return store.list().stream().anyMatch(task -> task.occupiesDevice()
                && Objects.equals(deviceId, task.getDeviceId())
                && (channelId == null || Objects.equals(channelId, task.getChannelId())));
    }

    /** 过滤内部子段，防止旧 SDK 把短区间当作用户原始请求找回。 */
    public boolean isChildStream(String stream) {
        return store.list().stream().anyMatch(task -> task.occupiesDevice() && Objects.equals(stream, task.getChildStream()));
    }

    /** 固定频率只投递空闲任务，不堆积排队；未知持久化状态不发新的 INVITE。 */
    @Scheduled(fixedDelay = 1000)
    public void schedule() {
        try {
            for (SegmentedDownloadTask task : store.list()) {
                if (task.isTerminal() && task.isSafeToStart()) {
                    if (System.currentTimeMillis() - task.getUpdatedAt() > settings.getRetentionSeconds() * 1000L) {
                        files.cleanup(task);
                        store.deleteIfCurrent(task);
                    }
                    continue;
                }
                if (running.contains(task.getStream()) && Objects.equals(workerId, task.getWorkerId())
                        && System.currentTimeMillis() - task.getUpdatedAt() > 10000) {
                    synchronized (lock(task.getStream())) {
                        SegmentedDownloadTask current = store.get(task.getStream());
                        if (current != null && Objects.equals(task.getId(), current.getId())
                                && Objects.equals(workerId, current.getWorkerId()) && !current.isTerminal()) {
                            save(current);
                        }
                    }
                }
                if (running.add(task.getStream())) {
                    try {
                        ThreadPoolExecutor target = task.getState() == State.MERGING ? mergeExecutor : executor;
                        target.execute(() -> {
                            try {
                                advance(task.getStream());
                            } catch (RuntimeException ex) {
                                log.warn("[分段录像下载] 推进失败，保留原会话和任务凭据，stream={}", task.getStream());
                            } finally {
                                running.remove(task.getStream());
                            }
                        });
                    } catch (java.util.concurrent.RejectedExecutionException ex) {
                        running.remove(task.getStream());
                    }
                }
            }
        } catch (RuntimeException ex) {
            log.warn("[分段录像下载] 暂不能读取持久化状态，不创建子段");
        }
    }

    /** 每次推进重新读取版本；重启仅清理原会话，绝不盲目重放 STARTING 或继续建段。 */
    void advance(String stream) {
        SegmentedDownloadTask task;
        synchronized (lock(stream)) {
            task = store.get(stream);
            if (task == null) {
                return;
            }
            long now = System.currentTimeMillis();
            if (!Objects.equals(workerId, task.getWorkerId())) {
                if (now - task.getUpdatedAt() < 30000) {
                    return;
                }
                task.setWorkerId(workerId);
                task.setCancelRequested(true);
                task.setError("原工作进程已结束，停止原会话后重新申请下载");
                if (task.isSafeToStart()) {
                    task.setState(State.FAILED);
                } else {
                    beginClosing(task, task.getError());
                }
                save(task);
            }
            if (task.isTerminal() && task.isSafeToStart()) {
                return;
            }
            if (now > task.getDeadlineAt() && !task.isCancelRequested()) {
                task.setCancelRequested(true);
                beginClosing(task, "录像下载超过总时限");
                save(task);
            }
            if (task.getState() == State.QUEUED || task.getState() == State.RETRY_WAIT) {
                if (now < task.getNextActionAt()) {
                    return;
                }
                prepareChild(task);
                save(task); // 必须先记录 STARTING 与不确定占用，才允许执行原生建流。
            } else if (task.getState() == State.STARTING || task.getState() == State.DOWNLOADING) {
                observeChild(task);
                return;
            } else if (task.getState() == State.CLOSING || (task.isTerminal() && !task.isSafeToStart())) {
                closeChild(task);
                return;
            } else if (task.getState() != State.MERGING) {
                return;
            }
        }
        if (task.getState() == State.STARTING) {
            startChild(task);
        } else if (task.getState() == State.MERGING) {
            merge(task);
        }
    }

    /** 按请求时间推进窗口，重叠只用于真实内容对齐，末段留出有限尾部保护。 */
    private void prepareChild(SegmentedDownloadTask task) {
        if (!task.isSafeToStart() || task.isCancelRequested()) {
            throw new IllegalStateException("原录像会话尚未确认关闭");
        }
        LocalDateTime start = LocalDateTime.parse(task.getStartTime(), TIME);
        LocalDateTime end = LocalDateTime.parse(task.getEndTime(), TIME);
        LocalDateTime cursor = start.plusSeconds((long) task.getSegmentIndex() * settings.getSegmentSeconds());
        LocalDateTime first = task.getSegmentIndex() == 0 ? start : cursor.minusSeconds(settings.getOverlapSeconds());
        LocalDateTime last = cursor.plusSeconds(settings.getSegmentSeconds());
        if (!last.isBefore(end)) {
            last = end.plusSeconds(settings.getTailGuardSeconds());
        }
        task.setChildStream(canonicalStream(task.getDeviceId(), channel(task).getDeviceId(), first, last));
        StreamInfo child = new StreamInfo();
        child.setStream(task.getChildStream());
        child.setStartTime(first.format(TIME));
        child.setEndTime(last.format(TIME));
        task.setChildInfo(child);
        task.setDialog(null);
        task.setState(State.STARTING);
        task.setAttempt(task.getAttempt() + 1);
        task.setSafeToStart(false);
        task.setChildStartedAt(System.currentTimeMillis());
        task.setError(null);
    }

    /** 原生回调只可更新同任务、同子段、同尝试；旧 200 和旧完成结果不得复活取消任务。 */
    private void startChild(SegmentedDownloadTask task) {
        synchronized (lock(task.getStream())) {
            SegmentedDownloadTask latest = store.get(task.getStream());
            if (latest == null || !Objects.equals(latest.getId(), task.getId())
                    || latest.getAttempt() != task.getAttempt()) {
                return;
            }
            if (latest.isCancelRequested()) {
                // STARTING 只是持久化意图；此临界区还未执行原生建流，可证明没有新的设备副作用。
                latest.setSafeToStart(true);
                latest.setState(State.CANCELLED);
                save(latest);
                return;
            }
            if (latest.getState() != State.STARTING || !Objects.equals(latest.getWorkerId(), workerId)) {
                return;
            }
            Device device = device(task);
            DeviceChannel channel = channel(task);
            MediaServer media = media(task);
            try {
                Boolean attempted = nativeDownload.startNativeDownload(media, device, channel, task.getChildInfo().getStartTime(),
                        task.getChildInfo().getEndTime(), task.getDownloadSpeed(), dialog -> {
                            updateAttempt(task, current -> {
                                if (!RecordDialogCloseReceipt.hasCompleteIdentity(dialog)
                                        || !Objects.equals(current.getChildStream(), dialog.getStream())
                                        || !Objects.equals(current.getMediaServerId(), dialog.getMediaServerId())) {
                                    throw new IllegalStateException("原下载对话身份不完整");
                                }
                                SsrcTransaction snapshot = JSON.parseObject(JSON.toJSONString(dialog), SsrcTransaction.class);
                                current.setDialog(snapshot);
                                current.setLastDialog(snapshot);
                            });
                        }, (code, message, info) -> updateAttempt(task, current -> {
                            if (code == 0 && info != null && !current.isCancelRequested()
                                    && (current.getState() == State.STARTING || current.getState() == State.DOWNLOADING)) {
                                current.setChildInfo(fileInfo(info));
                                current.setState(State.DOWNLOADING);
                            } else if (!current.isTerminal()) {
                                beginClosing(current, "子段未成功开始");
                            }
                        }));
                if (Boolean.FALSE.equals(attempted)) {
                    updateAttempt(task, current -> {
                        current.setSafeToStart(true);
                        current.setError("子段尚未发送设备请求");
                        if (current.isCancelRequested()) {
                            current.setState(State.CANCELLED);
                        } else if (current.getAttempt() >= settings.getMaxAttempts()) {
                            current.setState(State.FAILED);
                        } else {
                            current.setState(State.RETRY_WAIT);
                            current.setNextActionAt(System.currentTimeMillis() + (current.getAttempt() == 1 ? 5000 : 15000));
                        }
                    });
                }
            } catch (RuntimeException ex) {
                updateAttempt(task, current -> beginClosing(current, "子段启动结果不确定"));
            }
        }
    }

    /** 只有原生文件完成才进入关闭确认；超时、短文件或媒体未知均不能直接推进游标。 */
    private void observeChild(SegmentedDownloadTask task) {
        StreamInfo info = nativeDownload.getNativeDownloadInfo(device(task), channel(task), task.getChildStream());
        if (info != null) {
            task.setChildInfo(fileInfo(info));
            if ("completed".equals(info.getDownloadStatus()) || "interrupted".equals(info.getDownloadStatus())) {
                beginClosing(task, "completed".equals(info.getDownloadStatus()) ? null : "子段录像不完整");
                save(task);
                return;
            }
        }
        if (task.isCancelRequested() || System.currentTimeMillis() - task.getChildStartedAt()
                > settings.getChildTimeoutSeconds() * 1000L) {
            beginClosing(task, "子段录像接收超时");
        }
        save(task);
    }

    /** 进入关闭等待时只设置一次期限，不能因重复轮询无限延长结束确认。 */
    private void beginClosing(SegmentedDownloadTask task, String error) {
        if (task.getState() != State.CLOSING) {
            task.setNextActionAt(System.currentTimeMillis() + settings.getCloseTimeoutSeconds() * 1000L);
        }
        task.setState(State.CLOSING);
        task.setError(error);
    }

    /** 原 Dialog 协议确认且原媒体消失后，才接受成段或允许最多三次尝试。 */
    private void closeChild(SegmentedDownloadTask task) {
        if (task.getDialog() == null) {
            // 缺少应答身份时，可能是尚未收到 200；不能把空会话列表当作设备已经释放。
            if (System.currentTimeMillis() > task.getNextActionAt()) {
                task.setState(State.FAILED);
                task.setError("原下载对话缺少关闭确认，禁止自动重试");
                save(task);
            }
            return;
        }
        boolean confirmed = termination.isDeviceRecordDialogTerminated(task.getDialog());
        if (!confirmed) {
            nativeDownload.stopNativeDownload(device(task), channel(task), task.getDialog());
            confirmed = termination.isDeviceRecordDialogTerminated(task.getDialog());
        }
        if (!confirmed || !nativeDownload.isNativeDownloadMediaGone(media(task), task.getChildStream())) {
            if (System.currentTimeMillis() > task.getNextActionAt() && task.getState() != State.FAILED) {
                task.setState(State.FAILED);
                task.setError("原录像会话未确认关闭，已停止自动重试");
                save(task);
            }
            return;
        }
        task.setSafeToStart(true);
        if (task.isCancelRequested() || task.getState() == State.FAILED) {
            task.setState(task.isCancelRequested() ? State.CANCELLED : State.FAILED);
            save(task);
            return;
        }
        // MP4 钩子可能稍晚于 BYE 和注销；只在有限关闭等待内接纳同一尝试的文件。
        StreamInfo result = nativeDownload.getNativeDownloadInfo(device(task), channel(task), task.getChildStream());
        if (result != null && "completed".equals(result.getDownloadStatus()) && result.getDownLoadFilePath() != null) {
            task.setChildInfo(fileInfo(result));
            SegmentedDownloadTask.Segment segment = new SegmentedDownloadTask.Segment();
            segment.setStream(task.getChildStream());
            segment.setStartTime(result.getStartTime());
            segment.setEndTime(result.getEndTime());
            segment.setInfo(fileInfo(result));
            task.getSegments().add(segment);
            task.setSegmentIndex(task.getSegmentIndex() + 1);
            task.setAttempt(0);
            long covered = (long) task.getSegmentIndex() * settings.getSegmentSeconds();
            long requested = Duration.between(LocalDateTime.parse(task.getStartTime(), TIME),
                    LocalDateTime.parse(task.getEndTime(), TIME)).getSeconds();
            task.setState(covered >= requested ? State.MERGING : State.QUEUED);
            task.setNextActionAt(System.currentTimeMillis() + 1000);
            save(task);
        } else if (System.currentTimeMillis() > task.getNextActionAt()
                || (result != null && "interrupted".equals(result.getDownloadStatus()))) {
            if (task.getAttempt() >= settings.getMaxAttempts()) {
                task.setState(State.FAILED);
                task.setError("子段在有限尝试内未取得完整录像");
            } else {
                task.setState(State.RETRY_WAIT);
                task.setNextActionAt(System.currentTimeMillis() + (task.getAttempt() == 1 ? 5000 : 15000));
            }
            save(task);
        }
    }

    /** 本地合并验证与成品公开分离，取消或版本变化后绝不把旧文件写回当前任务。 */
    private void merge(SegmentedDownloadTask task) {
        try {
            Path verified = files.assemble(task, media(task), () -> noLongerCurrent(task));
            synchronized (lock(task.getStream())) {
                SegmentedDownloadTask current = store.get(task.getStream());
                if (current == null || !Objects.equals(task.getId(), current.getId())
                        || current.isCancelRequested() || current.getState() != State.MERGING) {
                    return;
                }
                current.setFinalFile(files.publish(current, verified));
                current.setState(State.COMPLETED);
                current.setError(null);
                save(current);
            }
        } catch (RuntimeException ex) {
            synchronized (lock(task.getStream())) {
                SegmentedDownloadTask current = store.get(task.getStream());
                if (current != null && Objects.equals(task.getId(), current.getId()) && current.getState() == State.MERGING) {
                    current.setState(current.isCancelRequested() ? State.CANCELLED : State.FAILED);
                    current.setError("合并录像未通过真实内容与完整性校验");
                    save(current);
                }
            }
        } finally {
            try {
                SegmentedDownloadTask current = store.get(task.getStream());
                if (current != null && (!Objects.equals(task.getId(), current.getId())
                        || (current.isTerminal() && current.getState() != State.COMPLETED))) {
                    // 成品失败或取消后立即释放私有临时空间，保留逻辑终态供调用方读取。
                    files.cleanup(task);
                }
            } catch (RuntimeException ex) {
                // 存储结果未知时不能删除可能已成功发布的成品，恢复扫描再按身份清理。
                log.warn("[分段录像下载] 临时文件清理待恢复，stream={}", task.getStream());
            }
        }
    }

    /** 异步处理器只有持有原任务及原尝试身份才能推进持久化状态。 */
    private void updateAttempt(SegmentedDownloadTask expected, Consumer<SegmentedDownloadTask> update) {
        // 原生 200/媒体回调持有子流锁；这里不能再取父锁，否则进度读取会形成父→子、子→父死锁。
        // 只做有界 CAS 状态更新，每次冲突都重新核对任务/尝试/取消状态，不在此发外部命令。
        for (int retry = 0; retry < 6; retry++) {
            SegmentedDownloadTask task = store.get(expected.getStream());
            if (task == null || !Objects.equals(task.getId(), expected.getId())
                    || task.getAttempt() != expected.getAttempt()
                    || !Objects.equals(task.getChildStream(), expected.getChildStream())
                    || (task.isTerminal() && task.isSafeToStart())) {
                return;
            }
            update.accept(task);
            if (store.save(task, task.getVersion())) {
                return;
            }
        }
        throw new IllegalStateException("下载回调持久化持续冲突");
    }

    /** 工具进程和文件传输定期检查原任务，重启、取消或存储故障都终止本次工作。 */
    private boolean noLongerCurrent(SegmentedDownloadTask expected) {
        try {
            SegmentedDownloadTask current = store.get(expected.getStream());
            return current == null || !Objects.equals(current.getId(), expected.getId())
                    || !Objects.equals(current.getWorkerId(), workerId)
                    || current.isCancelRequested() || current.getState() != State.MERGING
                    || System.currentTimeMillis() > current.getDeadlineAt();
        } catch (RuntimeException ex) {
            return true;
        }
    }

    /** 下载任务只持久化区间、进度与文件地址，媒体节点的密钥及播放链接仍归原生缓存管理。 */
    private static StreamInfo fileInfo(StreamInfo source) {
        StreamInfo result = new StreamInfo();
        result.setApp(source.getApp());
        result.setStream(source.getStream());
        result.setStartTime(source.getStartTime());
        result.setEndTime(source.getEndTime());
        result.setProgress(source.getProgress());
        result.setDownloadStatus(source.getDownloadStatus());
        result.setDownloadedDuration(source.getDownloadedDuration());
        result.setDownLoadFilePath(source.getDownLoadFilePath());
        result.setPartialDownloadFilePath(source.getPartialDownloadFilePath());
        return result;
    }

    /** 对外只投影父任务的原申请区间和最终成品，不泄漏子段或设备凭据。 */
    private StreamInfo view(SegmentedDownloadTask task) {
        StreamInfo info = new StreamInfo();
        info.setApp(MediaStreamUtil.RTP_APP);
        info.setStream(task.getStream());
        info.setDeviceId(task.getDeviceId());
        info.setChannelId(task.getChannelId());
        info.setServerId(task.getServerId());
        MediaServer media = new MediaServer();
        media.setId(task.getMediaServerId());
        info.setMediaServer(media);
        info.setStartTime(task.getStartTime());
        info.setEndTime(task.getEndTime());
        boolean complete = task.getState() == State.COMPLETED;
        double total = Duration.between(LocalDateTime.parse(task.getStartTime(), TIME),
                LocalDateTime.parse(task.getEndTime(), TIME)).getSeconds();
        double downloaded = Math.min(total, (double) task.getSegmentIndex() * settings.getSegmentSeconds());
        info.setDownloadedDuration(complete ? total : downloaded);
        info.setDownloadStatus(complete ? "completed" : task.getState() == State.MERGING ? "finalizing" : "downloading");
        info.setProgress(complete ? 1 : Math.min(0.99, downloaded / total));
        if (complete) {
            info.setDownLoadFilePath(task.getFinalFile());
        }
        return info;
    }

    /** 保存冲突必须停止当前外部动作，不能用旧内存状态覆盖取消或新轮次。 */
    private void save(SegmentedDownloadTask task) {
        if (!store.save(task, task.getVersion())) {
            throw new IllegalStateException("分段下载状态已变化");
        }
    }

    /** 设备或通道删除后保留不确定占用，不凭失去配置自动创建新会话。 */
    private Device device(SegmentedDownloadTask task) {
        Device device = devices.getDeviceByDeviceId(task.getDeviceId());
        if (device == null || !Objects.equals(device.getServerId(), task.getServerId())) {
            throw new IllegalStateException("原录像设备已不存在或归属变化");
        }
        return device;
    }

    /** 内部通道查找仍核对设备归属，防止数据库 ID 复用导致停止其他设备。 */
    private DeviceChannel channel(SegmentedDownloadTask task) {
        DeviceChannel channel = channels.getOneForSourceById(task.getChannelId());
        DeviceChannel owned = channel == null ? null : channels.getOneForSource(task.getDeviceId(), channel.getDeviceId());
        if (owned == null || !Objects.equals(owned.getId(), task.getChannelId())) {
            throw new IllegalStateException("原录像通道归属已变化");
        }
        return channel;
    }

    /** 整个任务固定原媒体节点，不能在重试时把文件和结束状态分散到其他 ZLM。 */
    private MediaServer media(SegmentedDownloadTask task) {
        MediaServer media = mediaServers.getOne(task.getMediaServerId());
        if (media == null) {
            throw new IllegalStateException("原媒体节点已不存在");
        }
        return media;
    }

    /** 保持旧 SDK 可找回的四段流名，任务内部另外用 UUID 隔离异步轮次。 */
    private static String canonicalStream(String device, String channel, LocalDateTime start, LocalDateTime end) {
        return device + "_" + channel + "_" + start.format(STREAM_TIME) + "_" + end.format(STREAM_TIME);
    }

    /** 使用固定锁条带串行父任务变更，Redis CAS 负责跨进程的最终归属。 */
    private Object lock(String stream) {
        return locks[Math.floorMod(stream.hashCode(), locks.length)];
    }

    /** 父任务查询与停止必须属于请求中的同一设备和通道。 */
    private static void requireOwner(SegmentedDownloadTask task, Device device, DeviceChannel channel) {
        if (task == null || !Objects.equals(device.getDeviceId(), task.getDeviceId())
                || !Objects.equals(channel.getId(), task.getChannelId())) {
            throw new ControllerException(-1, "录像下载任务不存在");
        }
    }

    /** 应用退出停止有界工作线程，持久化状态供下次启动只做原会话收口。 */
    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
        mergeExecutor.shutdownNow();
    }
}
