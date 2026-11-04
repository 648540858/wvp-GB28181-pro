package com.genersoft.iot.vmp.gb28181.controller;

import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionStatus;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.GlobalResponseAdvice;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IInviteStreamService;
import com.genersoft.iot.vmp.gb28181.service.IPlayService;
import com.genersoft.iot.vmp.gb28181.service.download.SegmentedDownloadService;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.DownloadFileInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RecordSessionControllerTest {
    @Mock private IInviteStreamService invites;
    @Mock private SipInviteSessionManager sessions;
    @Mock private IDeviceService devices;
    @Mock private IDeviceChannelService channels;
    @Mock private IPlayService play;
    @Mock private IMediaServerService media;
    @Mock private ObjectProvider<SegmentedDownloadService> segmentedDownloads;
    @Mock private SegmentedDownloadService segmented;
    @InjectMocks private PlayController controller;

    /** 每个用例使用真实存在的设备和数据库通道身份，避免把国标编号误当会话内键。 */
    @BeforeEach
    void setUp() {
        lenient().when(segmentedDownloads.getObject()).thenReturn(segmented);
        Device device = new Device();
        device.setDeviceId("device");
        DeviceChannel channel = new DeviceChannel();
        channel.setId(7);
        channel.setDeviceId("channel");
        when(devices.getDeviceByDeviceId("device")).thenReturn(device);
        lenient().when(channels.getOne("device", "channel")).thenReturn(channel);
    }

    /** 所有成功、拒绝和读取失败路径均只允许状态读取，任何停止、清理或媒体请求都会失败。 */
    @AfterEach
    void verifyReadOnlyBoundary() {
        verify(invites, atMostOnce()).getAllInviteInfo();
        verify(invites, atMostOnce()).hasPendingInvite(InviteSessionType.PLAYBACK, 7);
        verify(invites, atMostOnce()).hasPendingInvite(InviteSessionType.DOWNLOAD, 7);
        verify(sessions, atMostOnce()).getSsrcTransactionByDeviceId("device");
        verifyNoMoreInteractions(invites, sessions);
        verifyNoInteractions(play, media);
    }

    /** pending 尚无 SIP 事务和媒体时也属于占用，不能被下载队列误判为空闲。 */
    @Test
    void pendingInviteBeforeSip200IsBusy() {
        when(invites.getAllInviteInfo()).thenReturn(List.of(invite(InviteSessionType.PLAYBACK, false)));
        assertTrue(controller.hasRecordSession("device", "channel").getData());
        verifyNoInteractions(play, media);
    }

    /** 认领后尚未写入 InviteInfo 的短窗口由既有等待回调覆盖。 */
    @Test
    void pendingClaimBeforeInviteInfoIsBusy() {
        when(invites.hasPendingInvite(InviteSessionType.PLAYBACK, 7)).thenReturn(false);
        when(invites.hasPendingInvite(InviteSessionType.DOWNLOAD, 7)).thenReturn(true);
        assertTrue(controller.hasRecordSession("device", "channel").getData());
        verifyNoInteractions(play, media);
    }

    /** 经真实 MVC 路由确认 SDK 使用的路径和布尔包裹契约，避免新增接口仍返回 404。 */
    @Test
    void endpointReturnsBooleanDataThroughExistingResponseAdvice() throws Exception {
        when(invites.hasPendingInvite(InviteSessionType.PLAYBACK, 7)).thenReturn(true);
        MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalResponseAdvice()).build()
                .perform(get("/api/play/record/session/device/channel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));
    }

    /** 任意设备侧录像事务仍存在时，即使完成结果已缓存也不能宣布空闲。 */
    @Test
    void completedDownloadWithSipTransportIsStillBusy() {
        when(invites.getAllInviteInfo()).thenReturn(List.of(invite(InviteSessionType.DOWNLOAD, true)));
        when(sessions.getSsrcTransactionByDeviceId("device")).thenReturn(List.of(transaction(InviteSessionType.DOWNLOAD)));
        assertTrue(controller.hasRecordSession("device", "channel").getData());
        verifyNoInteractions(play, media);
    }

    /** 成品缓存仅用于查询下载地址，不占用已经释放的设备媒体传输。 */
    @Test
    void completedDownloadResultWithoutTransportIsNotBusy() {
        when(invites.getAllInviteInfo()).thenReturn(List.of(invite(InviteSessionType.DOWNLOAD, true)));
        assertFalse(controller.hasRecordSession("device", "channel").getData());
        verify(invites, never()).removeInviteInfo(any(InviteInfo.class));
        verifyNoInteractions(play, media);
    }

    /** 进度可能先四舍五入到 1，缺少真实成品路径时仍须等待录制完成。 */
    @Test
    void progressOneWithoutFileIsNotCompleted() {
        InviteInfo downloading = invite(InviteSessionType.DOWNLOAD, true);
        downloading.getStreamInfo().setDownLoadFilePath(new DownloadFileInfo());
        when(invites.getAllInviteInfo()).thenReturn(List.of(downloading));
        assertTrue(controller.hasRecordSession("device", "channel").getData());
    }

    /** 完成结果排在前面不能遮蔽同通道另一个仍在等待或活动的回放。 */
    @Test
    void scansEveryInviteRatherThanOnlyFirstChannelEntry() {
        InviteInfo playback = invite(InviteSessionType.PLAYBACK, false);
        playback.setStatus(InviteSessionStatus.ok);
        when(invites.getAllInviteInfo()).thenReturn(List.of(invite(InviteSessionType.DOWNLOAD, true), playback));
        assertTrue(controller.hasRecordSession("device", "channel").getData());
    }

    /** 直播、其他设备和其他通道的会话不阻塞当前录像通道。 */
    @Test
    void unrelatedTypesAndOwnersAreIgnored() {
        InviteInfo otherDevice = invite(InviteSessionType.PLAYBACK, false);
        otherDevice.setDeviceId("other");
        InviteInfo otherChannel = invite(InviteSessionType.DOWNLOAD, false);
        otherChannel.setChannelId(8);
        SsrcTransaction platform = transaction(InviteSessionType.PLAYBACK);
        platform.setPlatformId("platform");
        SsrcTransaction wrongChannel = transaction(InviteSessionType.DOWNLOAD);
        wrongChannel.setChannelId(8);
        SsrcTransaction wrongDevice = transaction(InviteSessionType.PLAYBACK);
        wrongDevice.setDeviceId("other");
        when(invites.getAllInviteInfo()).thenReturn(Arrays.asList(null, otherDevice, otherChannel, invite(InviteSessionType.PLAY, false)));
        when(sessions.getSsrcTransactionByDeviceId("device")).thenReturn(Arrays.asList(null, platform, wrongChannel, wrongDevice, transaction(InviteSessionType.PLAY)));
        assertFalse(controller.hasRecordSession("device", "channel").getData());
        verifyNoInteractions(play, media);
    }

    /** 活动回放即使 InviteInfo 丢失，只要 SIP 事务仍在就不能启动另一条录像下载。 */
    @Test
    void sipOnlyPlaybackIsBusyAndReadOnly() {
        when(sessions.getSsrcTransactionByDeviceId("device")).thenReturn(List.of(transaction(InviteSessionType.PLAYBACK)));
        assertTrue(controller.hasRecordSession("device", "channel").getData());
        verify(sessions).getSsrcTransactionByDeviceId("device");
        verifyNoMoreInteractions(sessions);
        verifyNoInteractions(play, media);
    }

    /** 查询失败必须向上传递，不能把 Redis 故障静默解释为设备空闲。 */
    @Test
    void stateReadFailureCannotBecomeIdle() {
        when(invites.getAllInviteInfo()).thenThrow(new IllegalStateException("unavailable"));
        assertThrows(IllegalStateException.class, () -> controller.hasRecordSession("device", "channel"));
        verifyNoInteractions(play, media);
    }

    /** 不存在的设备和通道均拒绝查询，也不能触发任何会话扫描或清理。 */
    @Test
    void missingDeviceAndChannelAreRejectedBeforeSessionReads() {
        when(devices.getDeviceByDeviceId("device")).thenReturn(null);
        assertThrows(ControllerException.class, () -> controller.hasRecordSession("device", "channel"));
        verifyNoInteractions(invites, sessions, play, media);
    }

    /** 已有设备也必须确认通道确实属于该设备，不能用另一设备的通道编号探测状态。 */
    @Test
    void missingChannelIsRejectedBeforeSessionReads() {
        when(channels.getOne("device", "channel")).thenReturn(null);
        assertThrows(ControllerException.class, () -> controller.hasRecordSession("device", "channel"));
        verifyNoInteractions(invites, sessions, play, media);
    }

    /** 父任务排队或重试尚无子段时仍占用设备，不能仅查询原生Invite和SIP。 */
    @Test
    void logicalParentWaitingBeforeChildInviteIsBusy() {
        when(segmented.hasBusyTask("device", 7)).thenReturn(true);
        assertTrue(controller.hasRecordSession("device", "channel").getData());
        verifyNoInteractions(invites, sessions);
    }

    /** 设备关闭已确认的云端合并不占回放名额，状态由分段任务服务统一判断。 */
    @Test
    void safeMergingWithoutOtherSipIsFree() {
        when(segmented.hasBusyTask("device", 7)).thenReturn(false);
        assertFalse(controller.hasRecordSession("device", "channel").getData());
        verify(segmented).hasBusyTask("device", 7);
    }

    /** 失败但尚未确认关闭的父任务仍占名额，不能被终态名称误认为设备空闲。 */
    @Test
    void unsafeFailedParentStillConsumesDeviceCapacity() {
        when(segmented.hasBusyTask("device", 7)).thenReturn(true);
        assertTrue(controller.hasRecordSession("device", "channel").getData());
        verifyNoInteractions(invites, sessions);
    }

    /** 未识别的新状态即使夹带旧文件与满进度，也不能猜测本次传输已经结束。 */
    @Test
    void unknownDownloadStateWithOldFileRemainsBusy() {
        InviteInfo unknown = invite(InviteSessionType.DOWNLOAD, true);unknown.getStreamInfo().setDownloadStatus("unknown");
        when(invites.getAllInviteInfo()).thenReturn(List.of(unknown));
        assertTrue(controller.hasRecordSession("device", "channel").getData());
    }

    /** 中断结果可保留用于诊断和部分文件下载，设备事务已消失时不继续阻塞队列。 */
    @Test
    void interruptedResultWithoutRealSipIsFree() {
        InviteInfo stopped = invite(InviteSessionType.DOWNLOAD, false);
        StreamInfo info = new StreamInfo();info.setDownloadStatus("interrupted");info.setProgress(0.4);
        stopped.setStreamInfo(info);
        when(invites.getAllInviteInfo()).thenReturn(List.of(stopped));
        assertFalse(controller.hasRecordSession("device", "channel").getData());
    }

    /** 中断缓存不能证明设备已经释放，真实待关闭Dialog仍需占用名额。 */
    @Test
    void interruptedResultWithPendingRealSipRemainsBusy() {
        InviteInfo stopped = invite(InviteSessionType.DOWNLOAD, false);
        StreamInfo info = new StreamInfo();info.setDownloadStatus("interrupted");info.setProgress(0.4);
        stopped.setStreamInfo(info);
        SsrcTransaction pending = transaction(InviteSessionType.DOWNLOAD);pending.setTerminationState("pending");
        when(invites.getAllInviteInfo()).thenReturn(List.of(stopped));
        when(sessions.getSsrcTransactionByDeviceId("device")).thenReturn(List.of(pending));
        assertTrue(controller.hasRecordSession("device", "channel").getData());
        verify(sessions).getSsrcTransactionByDeviceId("device");
    }

    /** 构造与目标设备通道精确匹配的缓存，不以流名形状代替身份判断。 */
    private InviteInfo invite(InviteSessionType type, boolean completed) {
        InviteInfo info = new InviteInfo();
        info.setDeviceId("device"); info.setChannelId(7); info.setType(type);
        info.setStatus(InviteSessionStatus.ready); info.setStream("stream");
        if (completed) {
            StreamInfo stream = new StreamInfo();
            stream.setProgress(1);
            DownloadFileInfo file = new DownloadFileInfo();
            file.setHttpPath("https://files.test/record.mp4");
            stream.setDownLoadFilePath(file);
            info.setStreamInfo(stream);
        }
        return info;
    }

    /** 构造设备侧事务，便于测试其类型、通道和平台归属过滤。 */
    private SsrcTransaction transaction(InviteSessionType type) {
        SsrcTransaction transaction = new SsrcTransaction();
        transaction.setDeviceId("device"); transaction.setChannelId(7); transaction.setType(type);
        return transaction;
    }
}
