package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionStatus;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.enums.MediaStreamUtil;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IDialogTerminationService;
import com.genersoft.iot.vmp.gb28181.service.IInviteStreamService;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IReceiveRtpServerService;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 流离开事件的处理测试。
 *
 * <p>重点覆盖录像下载会话的保留逻辑：ZLM 先注销流、再回调 on_record_mp4，
 * 下载地址依赖 InviteInfo 存活，所以下载会话不能在流离开时被立即删除。</p>
 */
@ExtendWith(MockitoExtension.class)
class PlayServiceImplTest {

    private static final String DEVICE_ID = "34020000001320000001";
    private static final String CHANNEL_GB_ID = "34020000001320000002";
    /** 回放/下载流名格式：设备_通道_开始时间_结束时间 */
    private static final String STREAM = DEVICE_ID + "_" + CHANNEL_GB_ID + "_20260101120000_20260101130000";
    private static final Integer CHANNEL_ID = 1;
    /** 与 PlayServiceImpl.DOWNLOAD_INVITE_KEEP_MS 保持一致：录像下载会话的保留窗口 */
    private static final long DOWNLOAD_INVITE_KEEP_MS = 15 * 60 * 1000L;

    @Mock
    private IInviteStreamService inviteStreamService;

    @Mock
    private IDeviceService deviceService;

    @Mock
    private IDeviceChannelService deviceChannelService;

    @Mock
    private IDialogTerminationService dialogTerminationService;

    @Mock
    private ISendRtpServerService sendRtpServerService;

    @Mock
    private IReceiveRtpServerService receiveRtpServerService;

    @Mock
    private IMediaServerService mediaServerService;

    @InjectMocks
    private PlayServiceImpl playService;

    /** 已注册下载确认媒体消失后结束原对话，但保留成品回调需要的Invite与保留期限。 */
    @Test
    void downloadDepartureShouldKeepInviteInfoUntilDownloadAddressIsReady() {
        InviteInfo inviteInfo = downloadInviteInfo();
        when(mediaServerService.getMediaList(inviteInfo.getStreamInfo().getMediaServer(),
                MediaStreamUtil.RTP_APP, STREAM, null)).thenReturn(Collections.emptyList());
        when(sendRtpServerService.queryByStream(anyString())).thenReturn(Collections.emptyList());
        when(inviteStreamService.getInviteInfoByStreamPreferPlay(STREAM)).thenReturn(inviteInfo);
        when(inviteStreamService.isStaleStreamDeparture(eq(inviteInfo), any())).thenReturn(false);
        when(deviceService.getDeviceByDeviceId(DEVICE_ID)).thenReturn(new Device());
        when(deviceChannelService.getOneForSourceById(CHANNEL_ID)).thenReturn(new DeviceChannel());

        playService.onApplicationEvent(departureEvent());

        // 会话被保留，并设置了保留窗口；BYE 与收流端口释放仍然执行
        verify(inviteStreamService, never()).removeInviteInfo(any(InviteInfo.class));
        verify(inviteStreamService).setInviteInfoCleanTime(eq(inviteInfo), eq(DOWNLOAD_INVITE_KEEP_MS));
        verify(dialogTerminationService).stopDeviceByStream(any(Device.class), any(DeviceChannel.class),
                eq(MediaStreamUtil.RTP_APP), eq(STREAM));
        verify(receiveRtpServerService).closeRTPServer(any(MediaServer.class),
                eq(MediaStreamUtil.RTP_APP), eq(STREAM));
    }

    /** 实时点播流注销仍立即清除Invite，不改为录像下载的保留语义。 */
    @Test
    void playDepartureShouldRemoveInviteInfoImmediately() {
        InviteInfo inviteInfo = downloadInviteInfo();
        inviteInfo.setType(InviteSessionType.PLAY);
        when(sendRtpServerService.queryByStream(anyString())).thenReturn(Collections.emptyList());
        when(inviteStreamService.getInviteInfoByStreamPreferPlay(STREAM)).thenReturn(inviteInfo);
        when(inviteStreamService.isStaleStreamDeparture(eq(inviteInfo), any())).thenReturn(false);
        when(deviceService.getDeviceByDeviceId(DEVICE_ID)).thenReturn(new Device());
        when(deviceChannelService.getOneForSourceById(CHANNEL_ID)).thenReturn(new DeviceChannel());

        playService.onApplicationEvent(departureEvent());

        verify(inviteStreamService).removeInviteInfo(inviteInfo);
        verify(inviteStreamService, never()).setInviteInfoCleanTime(any(InviteInfo.class), anyLong());
    }

    /** 新下载尚未注册时，旧注销事件不能终止当前pending下载或删除其缓存。 */
    @Test
    void downloadDepartureWithoutStreamShouldPreservePendingInviteInfo() {
        InviteInfo inviteInfo = downloadInviteInfo();
        inviteInfo.setStreamInfo(null);
        inviteInfo.setStatus(InviteSessionStatus.ready);
        when(sendRtpServerService.queryByStream(anyString())).thenReturn(Collections.emptyList());
        when(inviteStreamService.getInviteInfoByStreamPreferPlay(STREAM)).thenReturn(inviteInfo);
        when(inviteStreamService.isStaleStreamDeparture(eq(inviteInfo), any())).thenReturn(false);

        playService.onApplicationEvent(departureEvent());

        verify(inviteStreamService, never()).removeInviteInfo(any(InviteInfo.class));
        verify(inviteStreamService, never()).setInviteInfoCleanTime(any(InviteInfo.class), anyLong());
        org.mockito.Mockito.verifyNoInteractions(mediaServerService, deviceService, deviceChannelService,
                dialogTerminationService, receiveRtpServerService);
    }

    /** 同名下载被保留时，已注册回放确认无媒体仍必须结束回放，不误删下载。 */
    @Test
    void departureShouldCleanPlaybackSessionWhenDownloadIsRetainedWithSameStream() {
        InviteInfo playbackInviteInfo = downloadInviteInfo();
        playbackInviteInfo.setType(InviteSessionType.PLAYBACK);
        when(mediaServerService.getMediaList(playbackInviteInfo.getStreamInfo().getMediaServer(),
                MediaStreamUtil.RTP_APP, STREAM, null)).thenReturn(Collections.emptyList());
        when(sendRtpServerService.queryByStream(anyString())).thenReturn(Collections.emptyList());
        when(inviteStreamService.getInviteInfoByStreamPreferPlay(STREAM)).thenReturn(playbackInviteInfo);
        when(inviteStreamService.isStaleStreamDeparture(eq(playbackInviteInfo), any())).thenReturn(false);
        when(deviceService.getDeviceByDeviceId(DEVICE_ID)).thenReturn(new Device());
        when(deviceChannelService.getOneForSourceById(CHANNEL_ID)).thenReturn(new DeviceChannel());

        playService.onApplicationEvent(departureEvent());

        // 命中的是回放会话，必须按回放清理，不能把保留中的下载会话清掉
        verify(inviteStreamService).removeInviteInfo(playbackInviteInfo);
        verify(inviteStreamService, never()).setInviteInfoCleanTime(any(InviteInfo.class), anyLong());
    }

    /** 创建时间已证明属于旧实例的注销不触发任何结束或保留窗口更新。 */
    @Test
    void staleDepartureShouldBeSkipped() {
        InviteInfo inviteInfo = downloadInviteInfo();
        when(sendRtpServerService.queryByStream(anyString())).thenReturn(Collections.emptyList());
        when(inviteStreamService.getInviteInfoByStreamPreferPlay(STREAM)).thenReturn(inviteInfo);
        when(inviteStreamService.isStaleStreamDeparture(eq(inviteInfo), any())).thenReturn(true);

        playService.onApplicationEvent(departureEvent());

        verify(inviteStreamService, never()).removeInviteInfo(any(InviteInfo.class));
        verify(inviteStreamService, never()).setInviteInfoCleanTime(any(InviteInfo.class), anyLong());
        verify(dialogTerminationService, never()).stopDeviceByStream(any(), any(), anyString(), anyString());
    }

    /** 构造带当前创建时间的主协议注销事件。 */
    private MediaDepartureEvent departureEvent() {
        MediaDepartureEvent event = new MediaDepartureEvent(this);
        event.setApp(MediaStreamUtil.RTP_APP);
        event.setStream(STREAM);
        event.setSchema("rtsp");
        event.setCreateStamp(System.currentTimeMillis() / 1000);
        return event;
    }

    /** 构造已建立媒体的下载会话，避免用pending掩盖已注册清理路径。 */
    private InviteInfo downloadInviteInfo() {
        InviteInfo inviteInfo = InviteInfo.getInviteInfo(DEVICE_ID, CHANNEL_ID, STREAM, null, "mediaServer",
                null, null, "UDP", InviteSessionType.DOWNLOAD, InviteSessionStatus.ok);
        StreamInfo streamInfo = new StreamInfo();
        streamInfo.setApp(MediaStreamUtil.RTP_APP);
        streamInfo.setStream(STREAM);
        streamInfo.setMediaServer(new MediaServer());
        inviteInfo.setStreamInfo(streamInfo);
        inviteInfo.setCreateTime(System.currentTimeMillis());
        return inviteInfo;
    }
}
