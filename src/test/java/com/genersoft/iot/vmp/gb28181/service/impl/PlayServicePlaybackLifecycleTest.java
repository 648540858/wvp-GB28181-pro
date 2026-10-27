package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionStatus;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.enums.MediaStreamUtil;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.OpenRTPServerResult;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.service.IDialogTerminationService;
import com.genersoft.iot.vmp.gb28181.service.IInviteStreamService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IReceiveRtpServerService;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayServicePlaybackLifecycleTest {

    private static final String STREAM = "99999999999999999991_99999999999999999992_20261020100000_20261020100100";
    private static final String START = "2026-10-20 10:00:00";
    private static final String END = "2026-10-20 10:01:00";

    @Mock
    private IInviteStreamService inviteStreamService;
    @Mock
    private IReceiveRtpServerService receiveRtpServerService;
    @Mock
    private IMediaServerService mediaServerService;
    @Mock
    private IDialogTerminationService dialogTerminationService;
    @Mock
    private ISIPCommander cmder;
    @Mock
    private UserSetting userSetting;
    @Mock
    private ISendRtpServerService sendRtpServerService;
    @Mock
    private IDeviceService deviceService;
    @Mock
    private IDeviceChannelService deviceChannelService;
    @Mock
    private SipInviteSessionManager sessionManager;
    @Mock
    private ErrorCallback<StreamInfo> nextCallback;
    @Mock
    private ErrorCallback<StreamInfo> callback;
    @InjectMocks
    private PlayServiceImpl playService;

    private Device device;
    private DeviceChannel channel;
    private MediaServer selectedMedia;
    private MediaServer cachedMedia;

    // 使用两个不同媒体节点模拟负载选择变化，避免误把新节点查询当成旧流存活依据。
    @BeforeEach
    void setUp() {
        device = new Device();
        device.setDeviceId("synthetic-device");
        device.setServerId("synthetic-server");
        device.setStreamMode("UDP");
        channel = new DeviceChannel();
        channel.setId(27);
        channel.setDeviceId("synthetic-channel");
        selectedMedia = new MediaServer();
        selectedMedia.setId("selected-media");
        cachedMedia = new MediaServer();
        cachedMedia.setId("cached-media");
    }

    // 只在缓存所属节点确认流仍在线后复用，负载选择到其他节点也不能重复 INVITE。
    @Test
    void playback_cachedStreamAlive_reusesOriginalNodeWithoutInvite() {
        claimPlayback();
        InviteInfo cached = cachedInvite();
        when(inviteStreamService.getInviteInfo(InviteSessionType.PLAYBACK, channel.getId(), STREAM))
                .thenReturn(cached);
        when(mediaServerService.getMediaList(cachedMedia, MediaStreamUtil.RTP_APP, STREAM, null))
                .thenReturn(List.of(cached.getStreamInfo()));

        beginPlayback();

        verify(mediaServerService).getMediaList(cachedMedia, MediaStreamUtil.RTP_APP, STREAM, null);
        verify(mediaServerService, never()).getMediaList(eq(selectedMedia), anyString(), anyString(), any());
        verify(inviteStreamService).call(InviteSessionType.PLAYBACK, channel.getId(), STREAM,
                InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), cached.getStreamInfo());
        verifyNoNewPlayback();
        verifyNoInteractions(dialogTerminationService);
    }

    // 缓存已有成功结果但媒体流已结束时，释放旧会话并为同一录像重新发起一次回放。
    @Test
    void playback_cachedStreamMissing_releasesStaleSessionAndStartsAgain() throws Exception {
        claimPlayback();
        InviteInfo cached = cachedInvite();
        when(inviteStreamService.getInviteInfo(InviteSessionType.PLAYBACK, channel.getId(), STREAM))
                .thenReturn(cached);
        when(mediaServerService.getMediaList(any(MediaServer.class), eq(MediaStreamUtil.RTP_APP), eq(STREAM), isNull()))
                .thenReturn(List.of());
        allowNewPlayback();

        beginPlayback();

        verify(mediaServerService).getMediaList(cachedMedia, MediaStreamUtil.RTP_APP, STREAM, null);
        verify(dialogTerminationService).stopDeviceByStream(device, channel, MediaStreamUtil.RTP_APP, STREAM);
        verify(inviteStreamService).removeInviteInfo(cached);
        verify(receiveRtpServerService).openGbRTPServerForPlayback(eq(selectedMedia), eq(device), eq(channel),
                eq(START), eq(END), any());
        verify(cmder).playbackStreamCmd(eq(selectedMedia), any(SSRCInfo.class), eq(device), eq(channel),
                eq(START), eq(END), any(), any(), eq(1000L), any());
        verify(inviteStreamService, never()).call(InviteSessionType.PLAYBACK, channel.getId(), STREAM,
                InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), cached.getStreamInfo());
    }

    // 查询媒体节点失败不能等同于流不存在，否则会中断仍在播放的旧会话。
    @Test
    void playback_cachedMediaQueryUnavailable_reportsFailureWithoutDestroyingSession() {
        claimPlayback();
        InviteInfo cached = cachedInvite();
        when(inviteStreamService.getInviteInfo(InviteSessionType.PLAYBACK, channel.getId(), STREAM))
                .thenReturn(cached);
        when(mediaServerService.getMediaList(cachedMedia, MediaStreamUtil.RTP_APP, STREAM, null)).thenReturn(null);

        beginPlayback();

        verifyPlaybackFailure();
        verifyCacheNotRemoved();
        verifyNoInteractions(dialogTerminationService);
        verifyNoNewPlayback();
    }

    // 媒体查询抛异常时也必须保留旧会话，避免异常恢复路径破坏正常播放。
    @Test
    void playback_cachedMediaQueryThrows_reportsFailureWithoutDestroyingSession() {
        claimPlayback();
        InviteInfo cached = cachedInvite();
        when(inviteStreamService.getInviteInfo(InviteSessionType.PLAYBACK, channel.getId(), STREAM))
                .thenReturn(cached);
        when(mediaServerService.getMediaList(cachedMedia, MediaStreamUtil.RTP_APP, STREAM, null))
                .thenThrow(new IllegalStateException("synthetic media query failure"));

        beginPlayback();

        verifyPlaybackFailure();
        verifyCacheNotRemoved();
        verifyNoInteractions(dialogTerminationService);
        verifyNoNewPlayback();
    }

    // 没有缓存时仍需确认目标节点的媒体状态，查询失败不能盲目创建同名流。
    @Test
    void playback_noCacheAndMediaUnavailable_reportsFailureWithoutInvite() {
        claimPlayback();
        when(mediaServerService.getMediaList(selectedMedia, MediaStreamUtil.RTP_APP, STREAM, null)).thenReturn(null);

        beginPlayback();

        verifyPlaybackFailure();
        verifyCacheNotRemoved();
        verifyNoInteractions(dialogTerminationService);
        verifyNoNewPlayback();
    }

    // 无缓存且确认媒体不存在时正常发起一次回放，保持首次播放能力。
    @Test
    void playback_noCacheAndNoMedia_startsPlaybackOnce() throws Exception {
        claimPlayback();
        when(mediaServerService.getMediaList(selectedMedia, MediaStreamUtil.RTP_APP, STREAM, null))
                .thenReturn(List.of());
        allowNewPlayback();

        beginPlayback();

        verify(mediaServerService).getMediaList(selectedMedia, MediaStreamUtil.RTP_APP, STREAM, null);
        verify(receiveRtpServerService).openGbRTPServerForPlayback(eq(selectedMedia), eq(device), eq(channel),
                eq(START), eq(END), any());
        verify(cmder).playbackStreamCmd(eq(selectedMedia), any(SSRCInfo.class), eq(device), eq(channel),
                eq(START), eq(END), any(), any(), eq(1000L), any());
    }

    // 同一区间已有发起者时只保留等待回调，避免并发重复建流和抢占旧会话。
    @Test
    void playback_anotherRequestOwnsClaim_doesNotStartAnotherSession() {
        when(receiveRtpServerService.getPlaybackStream(device, channel, START, END)).thenReturn(STREAM);
        when(inviteStreamService.onceAndFirst(InviteSessionType.PLAYBACK, channel.getId(), STREAM, callback))
                .thenReturn(false);

        beginPlayback();

        verifyNoInteractions(mediaServerService, dialogTerminationService);
        verifyNoNewPlayback();
    }

    // 停止尚未收到注册事件的回放也需释放收流端口，不能依赖尚未生成的 StreamInfo。
    @Test
    void stop_pendingPlayback_closesReceiverByStoredMediaNode() {
        InviteInfo pending = cachedInvite();
        pending.setStatus(InviteSessionStatus.ready);
        pending.setStreamInfo(null);
        when(userSetting.getServerId()).thenReturn(device.getServerId());
        InviteStreamServiceImpl realInvites = spy(new InviteStreamServiceImpl());
        doReturn(pending).when(realInvites).getInviteInfo(InviteSessionType.PLAYBACK, channel.getId(), STREAM);
        doNothing().when(realInvites).removeInviteInfo(pending);
        ReflectionTestUtils.setField(playService, "inviteStreamService", realInvites);
        when(mediaServerService.getOne(cachedMedia.getId())).thenReturn(cachedMedia);
        assertTrue(realInvites.onceAndFirst(InviteSessionType.PLAYBACK, channel.getId(), STREAM, callback));

        playService.stop(InviteSessionType.PLAYBACK, device, channel, STREAM);

        verify(dialogTerminationService).stopDevice(device, channel, InviteSessionType.PLAYBACK, STREAM);
        verify(realInvites).removeInviteInfo(pending);
        verify(receiveRtpServerService).closeRTPServer(cachedMedia, MediaStreamUtil.RTP_APP, STREAM);
        verify(callback).run(eq(InviteErrorCode.ERROR_FOR_FINISH.getCode()), anyString(), isNull());
        assertTrue(realInvites.onceAndFirst(InviteSessionType.PLAYBACK, channel.getId(), STREAM, callback),
                "停止待注册会话后必须允许同区间立即重新认领，不能等待已被取消的旧超时任务");
    }

    // 停止并重播后，旧 RTP 超时不能通知新等待者、删除新缓存或再次关闭接收器。
    @Test
    void playback_oldRtpTimeoutAfterRestart_keepsNewSessionUntouched() {
        Map<String, InviteInfo> cache = prepareRestartablePlayback();
        beginPlayback();
        playService.stop(InviteSessionType.PLAYBACK, device, channel, STREAM);
        ReflectionTestUtils.invokeMethod(playService, "playBack", selectedMedia, device, channel, START, END, nextCallback);
        InviteInfo replacement = cache.get(STREAM);
        ArgumentCaptor<ErrorCallback<OpenRTPServerResult>> arrivals = ArgumentCaptor.forClass(ErrorCallback.class);
        verify(receiveRtpServerService, times(2)).openGbRTPServerForPlayback(eq(selectedMedia), eq(device), eq(channel),
                eq(START), eq(END), arrivals.capture());
        clearInvocations(receiveRtpServerService, nextCallback, cmder);

        arrivals.getAllValues().getFirst().run(InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getCode(),
                "synthetic old timeout", null);

        assertNewSessionRetained(cache, replacement);
    }

    // 旧 SIP 失败响应到达时同区间已换成新会话，清理必须留在旧请求的归属范围内。
    @Test
    void playback_oldSipFailureAfterRestart_keepsNewSessionUntouched() throws Exception {
        Map<String, InviteInfo> cache = prepareRestartablePlayback();
        beginPlayback();
        playService.stop(InviteSessionType.PLAYBACK, device, channel, STREAM);
        ReflectionTestUtils.invokeMethod(playService, "playBack", selectedMedia, device, channel, START, END, nextCallback);
        InviteInfo replacement = cache.get(STREAM);
        ArgumentCaptor<SipSubscribe.Event> failures = ArgumentCaptor.forClass(SipSubscribe.Event.class);
        verify(cmder, times(2)).playbackStreamCmd(eq(selectedMedia), any(SSRCInfo.class), eq(device), eq(channel),
                eq(START), eq(END), any(), failures.capture(), eq(1000L), any());
        clearInvocations(receiveRtpServerService, nextCallback, cmder);
        SipSubscribe.EventResult<Object> oldFailure = new SipSubscribe.EventResult<>();
        oldFailure.statusCode = 486;
        oldFailure.msg = "synthetic old rejection";

        failures.getAllValues().getFirst().response(oldFailure);

        assertNewSessionRetained(cache, replacement);
    }

    // 新回放尚未完成注册时，旧注销事件不能清理当前缓存或关闭新接收器。
    @Test
    void departure_pendingPlayback_ignoresOldDeparture() {
        InviteInfo pending = cachedInvite();
        pending.setStatus(InviteSessionStatus.ready);
        pending.setStreamInfo(null);
        prepareDeparture(pending);

        playService.onApplicationEvent(departureEvent());

        verifyCacheNotRemoved();
        verifyNoInteractions(mediaServerService, deviceService, deviceChannelService,
                dialogTerminationService, receiveRtpServerService);
    }

    // 即使注销事件时间信息缺失，只要当前同名媒体仍在线就保留新回放。
    @Test
    void departure_registeredPlaybackStillAlive_ignoresOldDeparture() {
        InviteInfo current = cachedInvite();
        prepareDeparture(current);
        when(mediaServerService.getMediaList(cachedMedia, MediaStreamUtil.RTP_APP, STREAM, null))
                .thenReturn(List.of(current.getStreamInfo()));

        playService.onApplicationEvent(departureEvent());

        verify(mediaServerService).getMediaList(cachedMedia, MediaStreamUtil.RTP_APP, STREAM, null);
        verifyCacheNotRemoved();
        verifyNoInteractions(deviceService, deviceChannelService, dialogTerminationService, receiveRtpServerService);
    }

    // 媒体确实消失后释放回放资源，但不能误更新实时点播的通道状态。
    @Test
    void departure_registeredPlaybackMediaGone_cleansOnlyPlaybackSession() {
        InviteInfo current = cachedInvite();
        prepareDeparture(current);
        when(mediaServerService.getMediaList(cachedMedia, MediaStreamUtil.RTP_APP, STREAM, null))
                .thenReturn(List.of());
        when(deviceService.getDeviceByDeviceId(device.getDeviceId())).thenReturn(device);
        when(deviceChannelService.getOneForSourceById(channel.getId())).thenReturn(channel);

        playService.onApplicationEvent(departureEvent());

        verify(dialogTerminationService).stopDeviceByStream(device, channel, MediaStreamUtil.RTP_APP, STREAM);
        verify(inviteStreamService).removeInviteInfo(current);
        verify(receiveRtpServerService).closeRTPServer(cachedMedia, MediaStreamUtil.RTP_APP, STREAM);
        verify(deviceChannelService, never()).stopPlay(any());
    }

    // 只提供事件入口所需的流与回放缓存，不创建级联发送或其他点播状态。
    private void prepareDeparture(InviteInfo invite) {
        when(sendRtpServerService.queryByStream(STREAM)).thenReturn(List.of());
        when(inviteStreamService.getInviteInfoByStreamPreferPlay(STREAM)).thenReturn(invite);
    }

    // 构造缺少旧实例精确时间的注销事件，验证真实媒体查询提供的防误清理边界。
    private MediaDepartureEvent departureEvent() {
        MediaDepartureEvent event = new MediaDepartureEvent(this);
        event.setApp(MediaStreamUtil.RTP_APP);
        event.setStream(STREAM);
        event.setSchema("rtsp");
        event.setMediaServer(cachedMedia);
        return event;
    }

    // 用真实等待认领和可观察的内存缓存替代 Redis，只隔离持久化与外部网络。
    private Map<String, InviteInfo> prepareRestartablePlayback() {
        Map<String, InviteInfo> cache = new ConcurrentHashMap<>();
        InviteStreamServiceImpl realInvites = spy(new InviteStreamServiceImpl());
        doAnswer(invocation -> cache.get(STREAM)).when(realInvites)
                .getInviteInfo(InviteSessionType.PLAYBACK, channel.getId(), STREAM);
        doAnswer(invocation -> {
            cache.put(STREAM, invocation.getArgument(0));
            return null;
        }).when(realInvites).updateInviteInfo(any(InviteInfo.class));
        doAnswer(invocation -> {
            cache.remove(STREAM);
            return null;
        }).when(realInvites).removeInviteInfo(any(InviteInfo.class));
        // 旧错误分支可能按键删除；修复后不应再进入该分支，因此保留宽松桩作为失败观察面。
        lenient().doAnswer(invocation -> {
            cache.remove(STREAM);
            return null;
        }).when(realInvites).removeInviteInfo(any(InviteSessionType.class), any(), any());
        ReflectionTestUtils.setField(playService, "inviteStreamService", realInvites);
        when(receiveRtpServerService.getPlaybackStream(device, channel, START, END)).thenReturn(STREAM);
        when(mediaServerService.getMediaList(selectedMedia, MediaStreamUtil.RTP_APP, STREAM, null))
                .thenReturn(List.of());
        when(mediaServerService.getOne(selectedMedia.getId())).thenReturn(selectedMedia);
        when(userSetting.getServerId()).thenReturn(device.getServerId());
        when(userSetting.getPlayTimeout()).thenReturn(1000);
        when(receiveRtpServerService.openGbRTPServerForPlayback(eq(selectedMedia), eq(device), eq(channel),
                eq(START), eq(END), any()))
                .thenReturn(new SSRCInfo(30000, "1234567890", MediaStreamUtil.RTP_APP, STREAM),
                        new SSRCInfo(30001, "1234567891", MediaStreamUtil.RTP_APP, STREAM));
        return cache;
    }

    // 通过新缓存、等待者和接收器副作用证明旧回调没有伤害新会话，不读取生产 token。
    private void assertNewSessionRetained(Map<String, InviteInfo> cache, InviteInfo replacement) {
        assertSame(replacement, cache.get(STREAM), "旧回调不得删除或替换新会话缓存");
        verifyNoInteractions(nextCallback, receiveRtpServerService, cmder);
        IInviteStreamService realInvites = (IInviteStreamService) ReflectionTestUtils.getField(playService, "inviteStreamService");
        assertFalse(realInvites.onceAndFirst(InviteSessionType.PLAYBACK, channel.getId(), STREAM, callback),
                "旧回调不得释放仍在等待注册的新会话认领");
    }

    // 注册本轮发起权，使私有核心流程绕开与本次生命周期无关的控制器和跨节点 RPC。
    private void claimPlayback() {
        when(receiveRtpServerService.getPlaybackStream(device, channel, START, END)).thenReturn(STREAM);
        when(inviteStreamService.onceAndFirst(InviteSessionType.PLAYBACK, channel.getId(), STREAM, callback))
                .thenReturn(true);
    }

    // 构造携带旧媒体节点及已成功播放结果的会话缓存。
    private InviteInfo cachedInvite() {
        StreamInfo streamInfo = new StreamInfo();
        streamInfo.setMediaServer(cachedMedia);
        streamInfo.setApp(MediaStreamUtil.RTP_APP);
        streamInfo.setStream(STREAM);
        InviteInfo invite = new InviteInfo();
        invite.setDeviceId(device.getDeviceId());
        invite.setChannelId(channel.getId());
        invite.setStream(STREAM);
        invite.setMediaServerId(cachedMedia.getId());
        invite.setStreamInfo(streamInfo);
        invite.setStatus(InviteSessionStatus.ok);
        invite.setType(InviteSessionType.PLAYBACK);
        invite.setCreateTime(1L);
        return invite;
    }

    // 给正常建流提供有效资源，测试只观察资源申请和清理，不需要真实 SIP 或媒体服务。
    private void allowNewPlayback() {
        when(receiveRtpServerService.openGbRTPServerForPlayback(eq(selectedMedia), eq(device), eq(channel),
                eq(START), eq(END), any()))
                .thenReturn(new SSRCInfo(30000, "1234567890", MediaStreamUtil.RTP_APP, STREAM));
        when(userSetting.getPlayTimeout()).thenReturn(1000);
    }

    // 调用与公开 API 共用的回放核心流程，保留完整缓存与媒体生命周期副作用。
    private void beginPlayback() {
        ReflectionTestUtils.invokeMethod(playService, "playBack", selectedMedia, device, channel, START, END, callback);
    }

    // 失败必须通知本轮等待者，并且不能把旧成功缓存误报为播放成功。
    private void verifyPlaybackFailure() {
        ArgumentCaptor<Integer> code = ArgumentCaptor.forClass(Integer.class);
        verify(inviteStreamService).call(eq(InviteSessionType.PLAYBACK), eq(channel.getId()), eq(STREAM),
                code.capture(), anyString(), isNull());
        assertNotEquals(InviteErrorCode.SUCCESS.getCode(), code.getValue());
    }

    // 失败路径保留旧缓存及会话归属，不能做无条件删除。
    private void verifyCacheNotRemoved() {
        verify(inviteStreamService, never()).removeInviteInfo(any(InviteInfo.class));
        verify(inviteStreamService, never()).removeInviteInfo(any(InviteSessionType.class), any(), any());
    }

    // 验证没有重复申请收流资源，也没有提前发送新的录像 INVITE。
    private void verifyNoNewPlayback() {
        verify(receiveRtpServerService, never()).openGbRTPServerForPlayback(any(), any(), any(), any(), any(), any());
        verifyNoInteractions(cmder);
    }
}
