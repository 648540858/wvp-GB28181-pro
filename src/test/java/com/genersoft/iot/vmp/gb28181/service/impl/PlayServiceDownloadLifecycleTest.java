package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.enums.MediaStreamUtil;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.OpenRTPServerResult;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IDialogTerminationService;
import com.genersoft.iot.vmp.gb28181.service.IInviteStreamService;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.bean.RecordInfo;
import com.genersoft.iot.vmp.media.event.hook.Hook;
import com.genersoft.iot.vmp.media.event.hook.HookData;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IReceiveRtpServerService;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.service.bean.DownloadFileInfo;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import gov.nist.javax.sip.message.SIPResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sip.ResponseEvent;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayServiceDownloadLifecycleTest {

    private static final String STREAM = "99999999999999999991_99999999999999999992_20261020100000_20261020100100";
    private static final String START = "2026-10-20 10:00:00";
    private static final String END = "2026-10-20 10:01:00";
    @Mock private IReceiveRtpServerService receiver;
    @Mock private IMediaServerService mediaServerService;
    @Mock private IDialogTerminationService dialogTerminationService;
    @Mock private IDeviceService deviceService;
    @Mock private IDeviceChannelService deviceChannelService;
    @Mock private ISendRtpServerService sendRtpServerService;
    @Mock private SipInviteSessionManager sessionManager;
    @Mock private ISIPCommander cmder;
    @Mock private HookSubscribe subscribe;
    @Mock private UserSetting userSetting;
    @Mock private IRedisCatchStorage redisCatchStorage;
    @Mock private ErrorCallback<StreamInfo> callback;
    @Spy @InjectMocks private PlayServiceImpl playService;

    private final Map<String, InviteInfo> cache = new ConcurrentHashMap<>();
    private InviteStreamServiceImpl invites;
    private Device device;
    private DeviceChannel channel;
    private MediaServer media;
    private DownloadFileInfo file;

    // 使用真实等待认领及内存持久化隔离网络，保留下载开始、自然结束与成品回调的顺序。
    @BeforeEach
    void setUp() {
        device = new Device();device.setDeviceId("99999999999999999991");device.setServerId("synthetic-server");device.setStreamMode("UDP");
        channel = new DeviceChannel();channel.setId(27);channel.setDeviceId("99999999999999999992");
        media = new MediaServer();media.setId("synthetic-media");
        file = new DownloadFileInfo();file.setHttpPath("http://synthetic.example/download.mp4");
        invites = spy(new InviteStreamServiceImpl());
        doAnswer(invocation -> cache.get(STREAM)).when(invites).getInviteInfo(InviteSessionType.DOWNLOAD, channel.getId(), STREAM);
        lenient().doAnswer(invocation -> cache.get(STREAM)).when(invites).getInviteInfoByStream(null, STREAM);
        doAnswer(invocation -> {cache.put(STREAM, invocation.getArgument(0));return null;}).when(invites).updateInviteInfo(any(InviteInfo.class));
        lenient().doAnswer(invocation -> {cache.put(STREAM, invocation.getArgument(0));return null;}).when(invites).updateInviteInfo(any(InviteInfo.class), anyLong());
        lenient().doAnswer(invocation -> {cache.remove(STREAM);return null;}).when(invites).removeInviteInfo(any(InviteInfo.class));
        lenient().doAnswer(invocation -> {cache.remove(STREAM);return null;}).when(invites).removeInviteInfo(any(InviteSessionType.class), any(), any());
        ReflectionTestUtils.setField(playService, "inviteStreamService", invites);
        ReflectionTestUtils.setField(playService, "receiveRtpServerService", receiver);
        when(receiver.getPlaybackStream(device, channel, START, END)).thenReturn(STREAM);
        when(receiver.openGbRTPServerForDownload(eq(media), eq(device), eq(channel), eq(START), eq(END), any()))
                .thenReturn(new SSRCInfo(30000, "1234567890", MediaStreamUtil.RTP_APP, STREAM));
        when(userSetting.getPlayTimeout()).thenReturn(1000);
        lenient().when(userSetting.getServerId()).thenReturn(device.getServerId());
        lenient().when(sendRtpServerService.queryByStream(STREAM)).thenReturn(List.of());
        lenient().when(deviceService.getDeviceByDeviceId(device.getDeviceId())).thenReturn(device);
        lenient().when(deviceChannelService.getOneForSourceById(channel.getId())).thenReturn(channel);
        lenient().when(mediaServerService.getDownloadFilePath(eq(media), any(RecordInfo.class))).thenReturn(file);
        doAnswer(invocation -> {
            StreamInfo info = new StreamInfo();info.setApp(MediaStreamUtil.RTP_APP);info.setStream(STREAM);info.setMediaServer(media);return info;
        }).when(playService).onPublishHandler(eq(media), any(MediaInfo.class), eq(device), eq(channel));
    }

    // 真实现场先注销再通知 MP4；自然结束必须保留本轮结果，成品可被进度接口重复读取。
    @Test
    void download_departureBeforeRecord_keepsFinishedFileAvailable() throws Exception {
        HookSubscribe.Event record = startAndPublish();
        when(mediaServerService.getMediaList(media, MediaStreamUtil.RTP_APP, STREAM, null)).thenReturn(List.of());
        MediaDepartureEvent departed = new MediaDepartureEvent(this);departed.setApp(MediaStreamUtil.RTP_APP);departed.setStream(STREAM);departed.setSchema("rtsp");departed.setMediaServer(media);

        playService.onApplicationEvent(departed);
        record.response(recordData(System.currentTimeMillis()));

        StreamInfo finished = playService.getDownLoadInfo(device, channel, STREAM);
        assertNotNull(finished, "自然注销不能删除仍等待 MP4 成品的下载结果");
        assertEquals(1.0, finished.getProgress());
        assertSame(file, finished.getDownLoadFilePath());
        assertSame(file, playService.getDownLoadInfo(device, channel, STREAM).getDownLoadFilePath());
    }

    // 主动停止后旧 MP4 回调不得重新创建已取消的结果缓存。
    @Test
    void download_stoppedBeforeRecord_doesNotResurrectResult() throws Exception {
        HookSubscribe.Event record = startAndPublish();
        playService.stop(InviteSessionType.DOWNLOAD, device, channel, STREAM);

        record.response(recordData(System.currentTimeMillis()));

        assertNull(cache.get(STREAM), "主动取消后旧成品不能复活下载会话");
    }

    // 相同区间停止重下后，旧 callback 不能把旧成品路径附到新会话。
    @Test
    void download_oldRecordCallbackAfterRestart_doesNotModifyReplacement() throws Exception {
        HookSubscribe.Event oldRecord = startAndPublish();
        playService.stop(InviteSessionType.DOWNLOAD, device, channel, STREAM);
        startAndPublish();
        InviteInfo replacement = cache.get(STREAM);

        oldRecord.response(recordData(System.currentTimeMillis()));

        assertSame(replacement, cache.get(STREAM));
        assertNull(replacement.getStreamInfo().getDownLoadFilePath(), "旧 callback 不得覆盖新下载结果");
    }

    // Hook 按流名分发，进入新订阅的旧文件也需按录制起始时间拒绝，不能只检查 callback 身份。
    @Test
    void download_oldRecordEventDeliveredToNewSubscription_isRejected() throws Exception {
        startAndPublish();
        playService.stop(InviteSessionType.DOWNLOAD, device, channel, STREAM);
        HookSubscribe.Event newRecord = startAndPublish();
        InviteInfo replacement = cache.get(STREAM);

        newRecord.response(recordData(replacement.getCreateTime() - 2000));

        assertNull(replacement.getStreamInfo().getDownLoadFilePath(), "早于本轮创建的旧 MP4 不得作为新成品");
    }

    // 同流重下后收到无精确实例身份的旧注销，当前媒体在线时不能关闭新下载。
    @Test
    void download_oldDepartureAfterRestart_keepsNewReceiver() throws Exception {
        startAndPublish();
        playService.stop(InviteSessionType.DOWNLOAD, device, channel, STREAM);
        startAndPublish();
        InviteInfo replacement = cache.get(STREAM);
        when(mediaServerService.getMediaList(media, MediaStreamUtil.RTP_APP, STREAM, null))
                .thenReturn(List.of(replacement.getStreamInfo()));
        clearInvocations(receiver, dialogTerminationService);
        MediaDepartureEvent departed = new MediaDepartureEvent(this);departed.setApp(MediaStreamUtil.RTP_APP);departed.setStream(STREAM);departed.setSchema("rtsp");

        playService.onApplicationEvent(departed);

        assertSame(replacement, cache.get(STREAM));
        verifyNoInteractions(receiver, dialogTerminationService);
    }

    // 先处理真实 200 包装，再投递 RTP 到来事件，返回本轮注册的 MP4 完成订阅。
    private HookSubscribe.Event startAndPublish() throws Exception {
        ReflectionTestUtils.invokeMethod(playService, "download", media, device, channel, START, END, 1, callback);
        ArgumentCaptor<SipSubscribe.Event> accepted = ArgumentCaptor.forClass(SipSubscribe.Event.class);
        verify(cmder, atLeastOnce()).downloadStreamCmd(eq(media), any(SSRCInfo.class), eq(device), eq(channel), eq(START), eq(END), eq(1), any(), accepted.capture(), eq(1000L), any());
        SIPResponse response = mock(SIPResponse.class);
        when(response.getRawContent()).thenReturn("v=0\r\ny=1234567890\r\n".getBytes(StandardCharsets.UTF_8));
        SipSubscribe.EventResult<ResponseEvent> ok = new SipSubscribe.EventResult<>();ok.statusCode=200;ok.event=new ResponseEvent(this,null,null,response);
        accepted.getAllValues().getLast().response(ok);
        ArgumentCaptor<ErrorCallback<OpenRTPServerResult>> arrival = ArgumentCaptor.forClass(ErrorCallback.class);
        verify(receiver, atLeastOnce()).openGbRTPServerForDownload(eq(media), eq(device), eq(channel), eq(START), eq(END), arrival.capture());
        MediaInfo info = new MediaInfo();info.setApp(MediaStreamUtil.RTP_APP);info.setStream(STREAM);info.setMediaServer(media);
        HookData data = new HookData();data.setMediaServer(media);data.setMediaInfo(info);
        OpenRTPServerResult opened = new OpenRTPServerResult();opened.setHookData(data);
        arrival.getAllValues().getLast().run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), opened);
        ArgumentCaptor<HookSubscribe.Event> record = ArgumentCaptor.forClass(HookSubscribe.Event.class);
        verify(subscribe, atLeastOnce()).addSubscribe(any(Hook.class), record.capture());
        return record.getAllValues().getLast();
    }

    // 使用媒体录制墙钟时间构造成品通知，避免把录像内容时间误作会话身份。
    private HookData recordData(long recordedAt) {
        RecordInfo record = new RecordInfo();record.setApp(MediaStreamUtil.RTP_APP);record.setStream(STREAM);record.setStartTime(recordedAt);record.setFileSize(100000);record.setTimeLen(60000);
        HookData data = new HookData();data.setApp(MediaStreamUtil.RTP_APP);data.setStream(STREAM);data.setMediaServer(media);data.setRecordInfo(record);return data;
    }
}
