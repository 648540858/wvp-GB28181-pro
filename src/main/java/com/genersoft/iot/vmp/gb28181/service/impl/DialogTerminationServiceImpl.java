package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.Platform;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
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
