package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionStatus;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.enums.MediaStreamUtil;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.service.IInviteStreamService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.media.MediaNotFoundEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IReceiveRtpServerService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.service.redisMsg.IRedisRpcPlayService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayServicePlaybackIsolationTest {

    private static final String DEVICE_ID = "99999999999999999991";
    private static final String CHANNEL_ID = "99999999999999999992";
    private static final String START = "2026-10-20 10:00:00";
    private static final String END = "2026-10-20 10:01:00";
    private static final String SHARED_STREAM = DEVICE_ID + "_" + CHANNEL_ID + "_20261020100000_20261020100100";
    private static final String SESSION_A = "11111111222233334444555555555555";
    private static final String SESSION_B = "aaaaaaaa222233334444555555555555";

    @Mock private IInviteStreamService invites;
    @Mock private IReceiveRtpServerService receiver;
    @Mock private IMediaServerService mediaServerService;
    @Mock private IDeviceChannelService deviceChannelService;
    @Mock private IRedisCatchStorage redisCatchStorage;
    @Mock private IRedisRpcPlayService redisRpcPlayService;
    @Mock private UserSetting userSetting;
    @Mock private ISIPCommander cmder;
    @Mock private ErrorCallback<StreamInfo> callback;
    @InjectMocks private PlayServiceImpl playService;

    private Device device;
    private DeviceChannel channel;
    private MediaServer media;

    // 使用合成编码和两个不同播放器身份，测试不连接实际 SIP 或媒体服务。
    @BeforeEach
    void setUp() {
        device = new Device();device.setDeviceId(DEVICE_ID);device.setServerId("local");device.setMediaServerId("synthetic-media");device.setStreamMode("UDP");device.setOnLine(true);
        channel = new DeviceChannel();channel.setId(27);channel.setDeviceId(CHANNEL_ID);
        media = new MediaServer();media.setId("synthetic-media");
    }

    // 同通道同录像区间的不同播放器必须申请两股流并发送各自的 SIP INVITE。
    @Test
    void playback_differentSessionIds_createSeparateStreamsAndInvites() throws Exception {
        localNode();
        when(receiver.getPlaybackStream(eq(device), eq(channel), eq(START), eq(END), anyString()))
                .thenAnswer(invocation -> isolatedStream(invocation.getArgument(4)));
        when(invites.onceAndFirst(eq(InviteSessionType.PLAYBACK), eq(channel.getId()), anyString(), eq(callback))).thenReturn(true);
        when(mediaServerService.getMediaList(eq(media), eq(MediaStreamUtil.RTP_APP), anyString(), isNull())).thenReturn(List.of());
        when(userSetting.getPlayTimeout()).thenReturn(1000);
        when(receiver.openGbRTPServerForPlayback(eq(media), eq(device), eq(channel), eq(START), eq(END), anyString(), any()))
                .thenAnswer(invocation -> new SSRCInfo(30000, "1234567890", MediaStreamUtil.RTP_APP, isolatedStream(invocation.getArgument(5))));

        playService.playBack(device, channel, START, END, SESSION_A, callback);
        playService.playBack(device, channel, START, END, SESSION_B, callback);

        verify(invites).onceAndFirst(InviteSessionType.PLAYBACK, channel.getId(), isolatedStream(SESSION_A), callback);
        verify(invites).onceAndFirst(InviteSessionType.PLAYBACK, channel.getId(), isolatedStream(SESSION_B), callback);
        ArgumentCaptor<SSRCInfo> requests = ArgumentCaptor.forClass(SSRCInfo.class);
        verify(cmder, times(2)).playbackStreamCmd(eq(media), requests.capture(), eq(device), eq(channel), eq(START), eq(END), any(), any(), eq(1000L), any());
        assertEquals(Set.of(isolatedStream(SESSION_A), isolatedStream(SESSION_B)),
                Set.of(requests.getAllValues().get(0).getStream(), requests.getAllValues().get(1).getStream()));
    }

    // 同播放器重复请求只复用该身份的活流，不能退回四段共享会话。
    @Test
    void playback_sameSessionAndLiveCache_reusesItsOwnStream() {
        localNode();
        String stream = isolatedStream(SESSION_A);
        when(receiver.getPlaybackStream(device, channel, START, END, SESSION_A)).thenReturn(stream);
        when(invites.onceAndFirst(InviteSessionType.PLAYBACK, channel.getId(), stream, callback)).thenReturn(true);
        StreamInfo info = new StreamInfo();info.setMediaServer(media);info.setStream(stream);
        InviteInfo cached = new InviteInfo();cached.setStatus(InviteSessionStatus.ok);cached.setStreamInfo(info);
        when(invites.getInviteInfo(InviteSessionType.PLAYBACK, channel.getId(), stream)).thenReturn(cached);
        when(mediaServerService.getMediaList(media, MediaStreamUtil.RTP_APP, stream, null)).thenReturn(List.of(info));

        playService.playBack(device, channel, START, END, SESSION_A, callback);

        verify(invites).call(eq(InviteSessionType.PLAYBACK), eq(channel.getId()), eq(stream), eq(0), anyString(), eq(info));
        verify(receiver, never()).openGbRTPServerForPlayback(any(), any(), any(), any(), any(), anyString(), any());
        verifyNoInteractions(cmder);
    }

    // 跨节点调用必须透传规范化后的会话 ID，远端不能为同一播放器重新分配身份。
    @Test
    void playback_remoteDevice_forwardsNormalizedSessionId() {
        when(userSetting.getServerId()).thenReturn("other-local-server");

        playService.playBack(device, channel, START, END, "AAAAAAAA-2222-3333-4444-555555555555", callback);

        verify(redisRpcPlayService).playback(device.getServerId(), channel.getId(), START, END, SESSION_B, callback);
        verifyNoInteractions(receiver, mediaServerService);
    }

    // 五段 URL 自动拉流必须保留原播放器身份，避免恢复时意外落入其他播放器的流。
    @Test
    void mediaNotFound_fivePartStream_reusesRequestedSessionId() {
        when(redisCatchStorage.getDevice(DEVICE_ID)).thenReturn(device);
        when(deviceChannelService.getOne(DEVICE_ID, CHANNEL_ID)).thenReturn(channel);
        when(receiver.getPlaybackStream(device, channel, START, END, SESSION_A)).thenReturn(isolatedStream(SESSION_A));

        playService.onApplicationEvent(missing(isolatedStream(SESSION_A)));

        verify(invites).onceAndFirst(eq(InviteSessionType.PLAYBACK), eq(channel.getId()), eq(isolatedStream(SESSION_A)), any());
        verify(receiver, never()).getPlaybackStream(device, channel, START, END);
    }

    // 不认识的分段数量、空或非法身份都不能触发任何设备查询和点播。
    @Test
    void mediaNotFound_invalidShapeOrSession_isIgnored() {
        for (String stream : List.of(DEVICE_ID + "_" + CHANNEL_ID + "_extra", SHARED_STREAM + "_bad-session",
                SHARED_STREAM + "_", isolatedStream(SESSION_A) + "_extra")) {
            playService.onApplicationEvent(missing(stream));
        }

        verifyNoInteractions(receiver, invites, redisCatchStorage, deviceChannelService);
    }

    // 内部旧调用不显式传会话 ID 时，继续使用原四段确定性流名保持兼容。
    @Test
    void playback_legacyEntry_keepsSharedStreamContract() {
        localNode();
        when(receiver.getPlaybackStream(device, channel, START, END)).thenReturn(SHARED_STREAM);

        playService.playBack(device, channel, START, END, callback);

        verify(invites).onceAndFirst(eq(InviteSessionType.PLAYBACK), eq(channel.getId()), eq(SHARED_STREAM), eq(callback));
        verify(receiver, never()).getPlaybackStream(eq(device), eq(channel), eq(START), eq(END), anyString());
    }

    // 独立身份放在时间前，保持末尾两个十四位时间供原有播放端解析时长。
    private String isolatedStream(String sessionId) {
        return DEVICE_ID + "_" + CHANNEL_ID + "_" + sessionId + "_20261020100000_20261020100100";
    }

    // 将设备固定在同一本地媒体节点，避免负载选择掩盖会话隔离结果。
    private void localNode() {
        when(userSetting.getServerId()).thenReturn(device.getServerId());
        when(mediaServerService.getOne(media.getId())).thenReturn(media);
    }

    // 构造仅携带已有流地址的媒体缺失事件，不引入随机身份或外部输入。
    private MediaNotFoundEvent missing(String stream) {
        MediaNotFoundEvent event = new MediaNotFoundEvent(this);event.setApp(MediaStreamUtil.RTP_APP);event.setStream(stream);event.setMediaServer(media);return event;
    }
}
