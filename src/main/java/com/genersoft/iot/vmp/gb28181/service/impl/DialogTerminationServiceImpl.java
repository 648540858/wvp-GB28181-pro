package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import com.genersoft.iot.vmp.gb28181.bean.PendingDialogClose;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.service.IDialogTerminationService;
import com.genersoft.iot.vmp.gb28181.session.AudioBroadcastManager;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IReceiveRtpServerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.util.UUID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
public class DialogTerminationServiceImpl implements IDialogTerminationService {

    private static final long RETRY_AFTER_MS = 5_000;
    private static final long PENDING_TIMEOUT_MS = 40_000;
    private static final int MAX_CLOSE_ATTEMPTS = 3;
    private final Object[] closeLocks = java.util.stream.IntStream.range(0, 64)
            .mapToObj(index -> new Object()).toArray();
    private Clock clock = Clock.systemUTC();

    @Autowired
    private SipInviteSessionManager sessionManager;

    @Autowired
    private ISIPCommander commander;

    @Autowired
    private ISIPCommanderForPlatform commanderForPlatform;

    @Autowired
    private AudioBroadcastManager audioBroadcastManager;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private IReceiveRtpServerService receiveRtpServerService;

    /** 只终止匹配设备、通道、类型及流名的对话，避免停止一个播放器时清理其他会话。 */
    @Override
    public int stopDevice(Device device, DeviceChannel channel, InviteSessionType type, String stream) {
        if (device == null || channel == null) {
            return 0;
        }
        Map<String, SsrcTransaction> result = new LinkedHashMap<>();
        for (SsrcTransaction transaction : sessionManager.getSsrcTransactionsByDeviceChannel(
                device.getDeviceId(), channel.getId(), type)) {
            if (matchesDevice(transaction, device, channel, type, stream)) {
                add(result, transaction);
            }
        }

        for (SsrcTransaction transaction : sessionManager.getAllStreamIndex()) {
            if (!matchesDevice(transaction, device, channel, type, stream)) {
                continue;
            }
            add(result, transaction);
        }

        int failed = 0;
        boolean audioSession = false;
        Set<String> sentDialogCallIds = new HashSet<>();
        Set<String> trackedRecordDialogs = new HashSet<>();
        for (SsrcTransaction transaction : result.values()) {
            if (transaction == null || transaction.getSipTransactionInfo() == null) {
                log.warn("[Dialog终止] 事务缺少 SipTransactionInfo, callId={}",
                        transaction == null ? null : transaction.getCallId());
                failed++;
                continue;
            }
            String dialogCallId = transaction.getSipTransactionInfo().getCallId();
            if (dialogCallId == null) {
                dialogCallId = transaction.getCallId();
            }
            if (dialogCallId == null) {
                failed++;
                continue;
            }

            if (isRecordSession(transaction)) {
                if (sentDialogCallIds.add(dialogCallId)) {
                    if (requestRecordClose(device, channel.getDeviceId(), transaction)) {
                        trackedRecordDialogs.add(dialogCallId);
                    } else {
                        failed++;
                    }
                }
                if (trackedRecordDialogs.contains(dialogCallId)) {
                    // 待确认身份已持久化到独立索引，活动索引和本地媒体可立即释放。
                    sessionManager.removeByCallId(transaction.getCallId() != null
                            ? transaction.getCallId() : dialogCallId);
                }
                continue;
            }

            if (InviteSessionType.BROADCAST.equals(transaction.getType())
                    || InviteSessionType.TALK.equals(transaction.getType())) {
                audioSession = true;
            }

            boolean alreadySent = !sentDialogCallIds.add(dialogCallId);
            if (!alreadySent) {
                try {
                    if (InviteSessionType.BROADCAST.equals(transaction.getType())) {
                        commander.streamByeCmdForDeviceInvite(device, channel.getDeviceId(),
                                transaction.getSipTransactionInfo(), null);
                    } else {
                        commander.streamByeCmd(device, channel.getDeviceId(),
                                transaction.getSipTransactionInfo(), null);
                    }
                } catch (Exception e) {
                    log.warn("[Dialog终止] 设备 BYE 发送失败, deviceId={}, channelId={}, callId={}",
                            device.getDeviceId(), channel.getDeviceId(), transaction.getCallId(), e);
                    failed++;
                    continue;
                }
            }

            String removeCallId = transaction.getCallId() != null
                    ? transaction.getCallId() : dialogCallId;
            sessionManager.removeByCallId(removeCallId);
        }

        if (audioSession) {
            audioBroadcastManager.del(channel.getId());
        }
        closeRtpStreams(result.values());
        return failed;
    }

    /** 流离开时也复用原设备结束owner，待确认录像对话继续保留在独立索引。 */
    @Override
    public int stopDeviceByStream(Device device, DeviceChannel channel, String app, String stream) {
        if (device == null || channel == null || stream == null) {
            return 0;
        }
        Map<String, SsrcTransaction> result = new LinkedHashMap<>();
        for (SsrcTransaction transaction : sessionManager.getSsrcTransactionsByStream(app, stream)) {
            if (matchesDevice(transaction, device, channel, null, stream)) {
                add(result, transaction);
            }
        }
        for (SsrcTransaction transaction : sessionManager.getAllStreamIndex()) {
            if (matchesDevice(transaction, device, channel, null, stream)
                    && Objects.equals(app, transaction.getApp())) {
                add(result, transaction);
            }
        }

        int failed = 0;
        Set<String> sentDialogCallIds = new HashSet<>();
        Set<String> trackedRecordDialogs = new HashSet<>();
        for (SsrcTransaction transaction : result.values()) {
            if (transaction == null || transaction.getSipTransactionInfo() == null) {
                failed++;
                continue;
            }
            String dialogCallId = transaction.getSipTransactionInfo().getCallId();
            if (dialogCallId == null) {
                dialogCallId = transaction.getCallId();
            }
            if (dialogCallId == null) {
                failed++;
                continue;
            }
            if (isRecordSession(transaction)) {
                if (sentDialogCallIds.add(dialogCallId)) {
                    if (requestRecordClose(device, channel.getDeviceId(), transaction)) {
                        trackedRecordDialogs.add(dialogCallId);
                    } else {
                        failed++;
                    }
                }
                if (trackedRecordDialogs.contains(dialogCallId)) {
                    sessionManager.removeByCallId(transaction.getCallId() != null
                            ? transaction.getCallId() : dialogCallId);
                }
                continue;
            }
            if (sentDialogCallIds.add(dialogCallId)) {
                try {
                    if (InviteSessionType.BROADCAST.equals(transaction.getType())) {
                        commander.streamByeCmdForDeviceInvite(device, channel.getDeviceId(),
                                transaction.getSipTransactionInfo(), null);
                    } else {
                        commander.streamByeCmd(device, channel.getDeviceId(),
                                transaction.getSipTransactionInfo(), null);
                    }
                } catch (Exception e) {
                    log.warn("[Dialog终止] 流离开 BYE 发送失败, stream={}, callId={}",
                            stream, transaction.getCallId(), e);
                    failed++;
                    continue;
                }
            }
            String removeCallId = transaction.getCallId() != null
                    ? transaction.getCallId() : dialogCallId;
            sessionManager.removeByCallId(removeCallId);
        }
        closeRtpStreams(result.values());
        return failed;
    }

    /** 下载与录像回放的设备名额需要单独确认，不能把媒体关闭当作释放完成。 */
    private boolean isRecordSession(SsrcTransaction transaction) {
        return transaction.getType() == InviteSessionType.DOWNLOAD
                || transaction.getType() == InviteSessionType.PLAYBACK;
    }

    /** 持久化原对话后发起有界终止，重复停止复用在途请求，不生成重叠BYE事务。 */
    private boolean requestRecordClose(Device device, String channelDeviceId, SsrcTransaction transaction) {
        String callId = transaction.getSipTransactionInfo().getCallId();
        if (callId == null) {
            callId = transaction.getCallId();
        }
        final String dialogCallId = callId;
        synchronized (closeLock(dialogCallId)) {
            PendingDialogClose previous = sessionManager.getPendingDialogClose(dialogCallId);
            long now = clock.millis();
            if (previous != null) {
                if (isProtocolTerminal(previous)) {
                    finishLocalClose(dialogCallId, previous);
                    return true;
                }
                long wait = PendingDialogClose.PENDING.equals(previous.getState())
                        ? PENDING_TIMEOUT_MS : RETRY_AFTER_MS;
                if (now - previous.getUpdatedAt() < wait) {
                    return true;
                }
                if (previous.getAttempts() >= MAX_CLOSE_ATTEMPTS) {
                    // 耗尽自动重试仍保留unknown，禁止把消失或超时伪装成设备已释放。
                    if (PendingDialogClose.PENDING.equals(previous.getState())) {
                        previous.setState(PendingDialogClose.UNKNOWN);
                        previous.setResponseCode(-1024);
                        sessionManager.updatePendingDialogClose(dialogCallId, previous.getAttemptId(), previous);
                    }
                    return false;
                }
            }
            PendingDialogClose close = new PendingDialogClose();
            close.setTransaction(previous == null ? transaction : previous.getTransaction());
            close.setChannelDeviceId(previous == null ? channelDeviceId : previous.getChannelDeviceId());
            close.setRemoteHost(previous == null ? device.getHostAddress() : previous.getRemoteHost());
            close.setLocalIp(previous == null ? device.getLocalIp() : previous.getLocalIp());
            close.setTransport(previous == null ? device.getTransport() : previous.getTransport());
            close.setAttemptId(UUID.randomUUID().toString());
            close.setAttempts(previous == null ? 1 : previous.getAttempts() + 1);
            close.setUpdatedAt(now);
            close.setState(PendingDialogClose.PENDING);
            if (!sessionManager.updatePendingDialogClose(dialogCallId,
                    previous == null ? null : previous.getAttemptId(), close)) {
                return false;
            }
            SipSubscribe.Event result = event -> recordCloseResult(dialogCallId, close.getAttemptId(), event.statusCode);
            try {
                Device route = closeDevice(close);
                commander.streamByeCmd(route, close.getChannelDeviceId(),
                        close.getTransaction().getSipTransactionInfo(), result, result);
                log.info("[录像终止] BYE已请求，等待设备结果，callId={}, attempt={}", dialogCallId, close.getAttempts());
                return true;
            } catch (Exception error) {
                recordCloseResult(dialogCallId, close.getAttemptId(), -1);
                log.warn("[录像终止] BYE发送失败，保留原对话待重试，callId={}", dialogCallId);
                return false;
            }
        }
    }

    /** 只接受本轮应答；200系列与481是协议终态，超时不代表设备内部名额已释放。 */
    private void recordCloseResult(String callId, String attemptId, int statusCode) {
        synchronized (closeLock(callId)) {
            PendingDialogClose close = sessionManager.getPendingDialogClose(callId);
            if (close == null || !attemptId.equals(close.getAttemptId())) {
                return;
            }
            if (isProtocolTerminal(close)) {
                // 已收到的终态不能被同轮迟到超时降级；仅补之前未成功的本地清理。
                finishLocalClose(callId, close);
                return;
            }
            close.setResponseCode(statusCode);
            close.setUpdatedAt(clock.millis());
            close.setState(statusCode >= 200 && statusCode < 300 ? PendingDialogClose.CONFIRMED
                    : statusCode == 481 ? PendingDialogClose.ABSENT : PendingDialogClose.UNKNOWN);
            if (!sessionManager.updatePendingDialogClose(callId, attemptId, close)) {
                return;
            }
            log.info("[录像终止] BYE结果，callId={}, status={}, state={}", callId, statusCode, close.getState());
            if (isProtocolTerminal(close)) {
                finishLocalClose(callId, close);
            }
        }
    }

    /** 设备主动结束可确认原记录，交叉BYE或耗尽重试后的通知也不再发起新的BYE。 */
    @Override
    public boolean confirmDeviceBye(String callId) {
        if (callId == null) {
            return false;
        }
        synchronized (closeLock(callId)) {
            PendingDialogClose close = sessionManager.getPendingDialogClose(callId);
            if (close == null) {
                return false;
            }
            if (!isProtocolTerminal(close)) {
                close.setState(PendingDialogClose.CONFIRMED);
                close.setResponseCode(200);
                close.setUpdatedAt(clock.millis());
                if (!sessionManager.updatePendingDialogClose(callId, close.getAttemptId(), close)) {
                    // 原ledger已接住此Dialog；持久化冲突也不能退回按stream清理当前播放器。
                    return true;
                }
            }
            finishLocalClose(callId, close);
            return true;
        }
    }

    /** 先清理活动索引，再撤销待确认记录，保证列表不存在只能发生在协议终态之后。 */
    private void finishLocalClose(String callId, PendingDialogClose close) {
        SsrcTransaction transaction = close.getTransaction();
        if (!sessionManager.removeByCallId(transaction.getCallId() == null ? callId : transaction.getCallId(), transaction)) {
            // Redis冲突或清理未成功时继续保留已确认结果，下轮只补本地清理，不重发BYE。
            return;
        }
        sessionManager.updatePendingDialogClose(callId, close.getAttemptId(), null);
    }

    /** 已收到的协议终态不再重发BYE，只继续完成可能失败的本地索引清理。 */
    private boolean isProtocolTerminal(PendingDialogClose close) {
        return PendingDialogClose.CONFIRMED.equals(close.getState())
                || PendingDialogClose.ABSENT.equals(close.getState());
    }

    /** 从原始结束路由构造最小设备对象，不依赖已被更新或删除的设备缓存。 */
    private Device closeDevice(PendingDialogClose close) {
        Device device = new Device();
        device.setDeviceId(close.getTransaction().getDeviceId());
        device.setHostAddress(close.getRemoteHost());
        device.setLocalIp(close.getLocalIp());
        device.setTransport(close.getTransport());
        return device;
    }

    /** 条带锁仅保护同一Call-ID的结束轮次；旧回调不能与下一次重试交错修改状态。 */
    private Object closeLock(String callId) {
        return closeLocks[Math.floorMod(callId.hashCode(), closeLocks.length)];
    }

    /** 重启后继续原结束请求，自动重试最多三轮，未知状态保留给后续诊断与人工恢复。 */
    @Scheduled(fixedDelay = 5_000)
    public void retryPendingRecordCloses() {
        for (PendingDialogClose close : sessionManager.getPendingDialogCloses()) {
            try {
                if (close.getTransaction() != null && close.getTransaction().getSipTransactionInfo() != null) {
                    requestRecordClose(closeDevice(close), close.getChannelDeviceId(), close.getTransaction());
                }
            } catch (Exception error) {
                log.warn("[录像终止] 结束结果收口未完成，保留持久化记录");
            }
        }
    }

    @Override
    public int stopPlatform(Platform platform, CommonGBChannel channel,
                            InviteSessionType type, String stream) {
        if (platform == null || channel == null) {
            return 0;
        }
        Map<String, SsrcTransaction> result = new LinkedHashMap<>();
        addAll(result, sessionManager.getSsrcTransactionsByPlatformChannel(
                platform.getServerGBId(), channel.getGbId(), type));

        for (SsrcTransaction transaction : sessionManager.getAllStreamIndex()) {
            if (transaction == null || transaction.getPlatformId() == null) {
                continue;
            }
            if (!Objects.equals(platform.getServerGBId(), transaction.getPlatformId())) {
                continue;
            }
            if (!Objects.equals(channel.getGbId(), transaction.getChannelId())) {
                continue;
            }
            if (type != null && !Objects.equals(type, transaction.getType())) {
                continue;
            }
            if (stream != null && !Objects.equals(stream, transaction.getStream())) {
                continue;
            }
            add(result, transaction);
        }

        int failed = 0;
        Set<String> sentDialogCallIds = new HashSet<>();
        for (SsrcTransaction transaction : result.values()) {
            if (transaction == null || transaction.getSipTransactionInfo() == null) {
                failed++;
                continue;
            }
            String dialogCallId = transaction.getSipTransactionInfo().getCallId();
            if (dialogCallId == null) {
                dialogCallId = transaction.getCallId();
            }
            if (dialogCallId == null) {
                failed++;
                continue;
            }
            if (sentDialogCallIds.add(dialogCallId)) {
                try {
                    commanderForPlatform.streamByeCmd(platform, channel,
                            transaction.getSipTransactionInfo(), null);
                } catch (Exception e) {
                    log.warn("[Dialog终止] 平台 BYE 发送失败, platformId={}, channelId={}, callId={}",
                            platform.getServerGBId(), channel.getGbId(), transaction.getCallId(), e);
                    failed++;
                    continue;
                }
            }
            String removeCallId = transaction.getCallId() != null
                    ? transaction.getCallId() : dialogCallId;
            sessionManager.removeByCallId(removeCallId);
        }
        closeRtpStreams(result.values());
        return failed;
    }

    private void closeRtpStreams(Collection<SsrcTransaction> transactions) {
        Set<String> closed = new HashSet<>();
        for (SsrcTransaction transaction : transactions) {
            if (transaction == null || transaction.getApp() == null
                    || transaction.getStream() == null || transaction.getMediaServerId() == null) {
                continue;
            }
            String key = transaction.getMediaServerId() + "|"
                    + transaction.getApp() + "|" + transaction.getStream();
            if (!closed.add(key)) {
                continue;
            }
            MediaServer mediaServer = mediaServerService.getOne(transaction.getMediaServerId());
            if (mediaServer != null) {
                receiveRtpServerService.closeRTPServer(mediaServer,
                        transaction.getApp(), transaction.getStream());
            }
        }
    }

    private boolean matchesDevice(SsrcTransaction transaction, Device device,
                                  DeviceChannel channel, InviteSessionType type, String stream) {
        if (transaction == null || transaction.getPlatformId() != null) {
            return false;
        }
        if (!Objects.equals(device.getDeviceId(), transaction.getDeviceId())) {
            return false;
        }
        if (!Objects.equals(channel.getId(), transaction.getChannelId())) {
            return false;
        }
        if (type != null && !Objects.equals(type, transaction.getType())) {
            return false;
        }
        return stream == null || Objects.equals(stream, transaction.getStream());
    }

    private void addAll(Map<String, SsrcTransaction> result, List<SsrcTransaction> transactions) {
        if (transactions == null) {
            return;
        }
        for (SsrcTransaction transaction : transactions) {
            add(result, transaction);
        }
    }

    private void add(Map<String, SsrcTransaction> result, SsrcTransaction transaction) {
        if (transaction == null) {
            return;
        }
        String key = transaction.getCallId();
        if (key == null && transaction.getSipTransactionInfo() != null) {
            key = transaction.getSipTransactionInfo().getCallId();
        }
        if (key == null) {
            key = System.identityHashCode(transaction) + "";
        }
        result.putIfAbsent(key, transaction);
    }
}
