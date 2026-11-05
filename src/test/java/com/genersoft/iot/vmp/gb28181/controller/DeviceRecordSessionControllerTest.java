package com.genersoft.iot.vmp.gb28181.controller;

import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.GlobalResponseAdvice;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DeviceRecordSessionControllerTest {
    @Mock private IInviteStreamService invites;
    @Mock private SipInviteSessionManager sessions;
    @Mock private IDeviceService devices;
    @Mock private IDeviceChannelService channels;
    @Mock private IPlayService play;
    @Mock private IMediaServerService media;
    @Mock private ObjectProvider<SegmentedDownloadService> segmentedDownloads;
    @Mock private SegmentedDownloadService segmented;
    @InjectMocks private PlayController controller;

    /** 明确物理设备的数据库身份和全部通道，不把设备级查询缩成指定单通道。 */
    @BeforeEach
    void setUp() {
        lenient().when(segmentedDownloads.getObject()).thenReturn(segmented);
        Device device = new Device();
        device.setId(90);
        device.setDeviceId("device");
        when(devices.getDeviceByDeviceId("device")).thenReturn(device);
        lenient().when(channels.queryChaneIdListByDeviceDbIds(List.of(90))).thenReturn(List.of(7, 8));
    }

    /** 所有路径只允许缓存、事务和认领读取，不得清理、停止或请求媒体节点。 */
    @AfterEach
    void onlyAllowsStateReads() {
        verify(invites, atMostOnce()).getAllInviteInfo();
        verify(invites, atMost(2)).hasPendingInvite(eq(InviteSessionType.PLAYBACK), anyInt());
        verify(invites, atMost(2)).hasPendingInvite(eq(InviteSessionType.DOWNLOAD), anyInt());
        verify(sessions, atMostOnce()).getSsrcTransactionByDeviceId("device");
        verifyNoMoreInteractions(invites, sessions);
        verifyNoInteractions(play, media);
    }

    /** 首通道的成品缓存不能遮蔽同设备另一通道尚未收到 SIP 响应的下载。 */
    @Test
    void completedFirstChannelCannotHideAnotherPendingChannel() {
        when(invites.getAllInviteInfo()).thenReturn(List.of(completedDownload(), invite(8, InviteSessionType.DOWNLOAD)));
        assertTrue(controller.hasDeviceRecordSession("device").getData());
    }

    /** 活动回放即使丢失 InviteInfo，其他通道的设备侧 SIP 事务仍应阻塞新导出。 */
    @Test
    void sipOnAnotherChannelIsBusyEvenWithoutInviteInfo() {
        when(sessions.getSsrcTransactionByDeviceId("device")).thenReturn(List.of(transaction(8, InviteSessionType.PLAYBACK)));
        assertTrue(controller.hasDeviceRecordSession("device").getData());
    }

    /** 成品地址只代表缓存结果，真实 SIP 事务未释放时仍占用设备的唯一导出通道。 */
    @Test
    void completedCacheWithLiveSipTransportRemainsBusy() {
        when(invites.getAllInviteInfo()).thenReturn(List.of(completedDownload()));
        when(sessions.getSsrcTransactionByDeviceId("device")).thenReturn(List.of(transaction(7, InviteSessionType.DOWNLOAD)));
        assertTrue(controller.hasDeviceRecordSession("device").getData());
    }

    /** 已从数据库删除的通道仍可能保留真实事务，设备级占用不能依赖当前通道列表过滤。 */
    @Test
    void transactionForDeletedChannelStillConsumesDeviceCapacity() {
        when(sessions.getSsrcTransactionByDeviceId("device")).thenReturn(List.of(transaction(99, InviteSessionType.DOWNLOAD)));
        assertTrue(controller.hasDeviceRecordSession("device").getData());
        verifyNoInteractions(channels);
    }

    /** InviteInfo 尚未持久化时，必须检查设备全部数据库通道上的认领。 */
    @Test
    void pendingClaimOnSecondDatabaseChannelIsBusy() {
        when(invites.hasPendingInvite(any(InviteSessionType.class), anyInt()))
                .thenAnswer(call -> call.getArgument(0) == InviteSessionType.DOWNLOAD && (Integer) call.getArgument(1) == 8);
        assertTrue(controller.hasDeviceRecordSession("device").getData());
        verify(channels).queryChaneIdListByDeviceDbIds(List.of(90));
    }

    /** 完整成品且所有媒体事务和认领均已释放，设备才可开始下一通道。 */
    @Test
    void completedCacheWithoutAnyTransportIsIdle() {
        when(invites.getAllInviteInfo()).thenReturn(List.of(completedDownload()));
        assertFalse(controller.hasDeviceRecordSession("device").getData());
        verify(invites).hasPendingInvite(InviteSessionType.PLAYBACK, 8);
        verify(invites).hasPendingInvite(InviteSessionType.DOWNLOAD, 8);
    }

    /** 进度达到 1 但没有成品地址，仍可能正在封装，不能释放设备容量。 */
    @Test
    void progressOneWithoutFinalFileRemainsBusy() {
        InviteInfo pending = completedDownload();
        pending.getStreamInfo().setDownLoadFilePath(null);
        when(invites.getAllInviteInfo()).thenReturn(List.of(pending));
        assertTrue(controller.hasDeviceRecordSession("device").getData());
    }

    /** 设备之外的会话、直播和上级平台会话均不消耗本设备的录像排队容量。 */
    @Test
    void ignoresUnrelatedDeviceLiveAndPlatformSessions() {
        InviteInfo other = invite(8, InviteSessionType.PLAYBACK);
        other.setDeviceId("other");
        SsrcTransaction platform = transaction(8, InviteSessionType.DOWNLOAD);
        platform.setPlatformId("platform");
        SsrcTransaction otherDevice = transaction(8, InviteSessionType.PLAYBACK);
        otherDevice.setDeviceId("other");
        when(invites.getAllInviteInfo()).thenReturn(List.of(other, invite(7, InviteSessionType.PLAY)));
        when(sessions.getSsrcTransactionByDeviceId("device")).thenReturn(List.of(platform, otherDevice, transaction(8, InviteSessionType.PLAY)));
        assertFalse(controller.hasDeviceRecordSession("device").getData());
        verify(invites, never()).hasPendingInvite(any(), eq(9));
    }

    /** 旧通道路由只检查请求通道，设备级新路由不能改变现有调用方语义。 */
    @Test
    void legacyChannelRouteKeepsNarrowScope() {
        DeviceChannel channel = new DeviceChannel();
        channel.setId(7);
        when(channels.getOne("device", "channel7")).thenReturn(channel);
        when(invites.getAllInviteInfo()).thenReturn(List.of(invite(8, InviteSessionType.PLAYBACK)));
        assertFalse(controller.hasRecordSession("device", "channel7").getData());
        verify(channels, never()).queryChaneIdListByDeviceDbIds(anyList());
    }

    /** 缺失设备必须拒绝，不能扫描全局通道或将不存在解释为空闲。 */
    @Test
    void missingDeviceIsRejectedBeforeStateReads() {
        when(devices.getDeviceByDeviceId("device")).thenReturn(null);
        assertThrows(ControllerException.class, () -> controller.hasDeviceRecordSession("device"));
        verifyNoInteractions(invites, sessions, channels);
    }

    /** 查询设备通道失败时保留未知状态，不能让下载方获得错误的空闲结论。 */
    @Test
    void databaseReadFailureCannotBecomeIdle() {
        when(channels.queryChaneIdListByDeviceDbIds(List.of(90))).thenThrow(new IllegalStateException("unavailable"));
        assertThrows(IllegalStateException.class, () -> controller.hasDeviceRecordSession("device"));
    }

    /** 真实 MVC 路由输出 data:boolean，供 SDK 严格区别空闲、缺失接口与未知结果。 */
    @Test
    void deviceOnlyRouteReturnsBooleanData() throws Exception {
        when(invites.getAllInviteInfo()).thenReturn(List.of(invite(8, InviteSessionType.PLAYBACK)));
        MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalResponseAdvice()).build()
                .perform(get("/api/play/record/session/device"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));
    }

    /** 父任务排队或重试尚无子段时仍占用设备，不能仅查询原生Invite和SIP。 */
    @Test
    void logicalParentWaitingBeforeChildInviteIsBusy() {
        when(segmented.hasBusyTask("device", null)).thenReturn(true);
        assertTrue(controller.hasDeviceRecordSession("device").getData());
        verifyNoInteractions(invites, sessions);
    }

    /** 设备关闭已确认的云端合并不占回放名额，状态由分段任务服务统一判断。 */
    @Test
    void safeMergingWithoutOtherSipIsFree() {
        when(segmented.hasBusyTask("device", null)).thenReturn(false);
        assertFalse(controller.hasDeviceRecordSession("device").getData());
        verify(segmented).hasBusyTask("device", null);
    }

    /** 失败但尚未确认关闭的父任务仍占名额，不能被终态名称误认为设备空闲。 */
    @Test
    void unsafeFailedParentStillConsumesDeviceCapacity() {
        when(segmented.hasBusyTask("device", null)).thenReturn(true);
        assertTrue(controller.hasDeviceRecordSession("device").getData());
        verifyNoInteractions(invites, sessions);
    }

    /** 未识别的新状态即使夹带旧文件与满进度，也不能猜测本次传输已经结束。 */
    @Test
    void unknownDownloadStateWithOldFileRemainsBusy() {
        InviteInfo unknown = completedDownload();unknown.getStreamInfo().setDownloadStatus("unknown");
        when(invites.getAllInviteInfo()).thenReturn(List.of(unknown));
        assertTrue(controller.hasDeviceRecordSession("device").getData());
    }

    /** 中断结果可保留用于诊断和部分文件下载，设备事务已消失时不继续阻塞队列。 */
    @Test
    void interruptedResultWithoutRealSipIsFree() {
        InviteInfo stopped = invite(7, InviteSessionType.DOWNLOAD);
        StreamInfo info = new StreamInfo();info.setDownloadStatus("interrupted");info.setProgress(0.4);
        stopped.setStreamInfo(info);
        when(invites.getAllInviteInfo()).thenReturn(List.of(stopped));
        assertFalse(controller.hasDeviceRecordSession("device").getData());
    }

    /** 中断缓存不能证明设备已经释放，真实待关闭Dialog仍需占用名额。 */
    @Test
    void interruptedResultWithPendingRealSipRemainsBusy() {
        InviteInfo stopped = invite(7, InviteSessionType.DOWNLOAD);
        StreamInfo info = new StreamInfo();info.setDownloadStatus("interrupted");info.setProgress(0.4);
        stopped.setStreamInfo(info);
        SsrcTransaction pending = transaction(7, InviteSessionType.DOWNLOAD);pending.setTerminationState("pending");
        when(invites.getAllInviteInfo()).thenReturn(List.of(stopped));
        when(sessions.getSsrcTransactionByDeviceId("device")).thenReturn(List.of(pending));
        assertTrue(controller.hasDeviceRecordSession("device").getData());
        verify(sessions).getSsrcTransactionByDeviceId("device");
    }

    /** 构造同设备不同通道的录像缓存，用于验证聚合身份过滤。 */
    private InviteInfo invite(int channelId, InviteSessionType type) {
        InviteInfo invite = new InviteInfo();
        invite.setDeviceId("device"); invite.setChannelId(channelId); invite.setType(type);
        return invite;
    }

    /** 构造真实成品缓存，单独模拟已释放媒体后的下载结果保留。 */
    private InviteInfo completedDownload() {
        InviteInfo invite = invite(7, InviteSessionType.DOWNLOAD);
        StreamInfo stream = new StreamInfo(); stream.setProgress(1);
        DownloadFileInfo file = new DownloadFileInfo(); file.setHttpPath("https://files.test/complete.mp4");
        stream.setDownLoadFilePath(file); invite.setStreamInfo(stream);
        return invite;
    }

    /** 构造设备侧媒体事务，不借助流名推断物理设备或通道。 */
    private SsrcTransaction transaction(int channelId, InviteSessionType type) {
        SsrcTransaction transaction = new SsrcTransaction();
        transaction.setDeviceId("device"); transaction.setChannelId(channelId); transaction.setType(type);
        return transaction;
    }
}
