package com.genersoft.iot.vmp.gb28181.service.download;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.gb28181.service.IDialogTerminationService;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.DownloadFileInfo;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static com.genersoft.iot.vmp.gb28181.service.download.SegmentedDownloadTask.State;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 显式重做只复用完整原片，边界失败不能降级为NVR重新下载。 */
@SuppressWarnings("unchecked")
class SegmentedMergeResumeTest {
    private static final String DEVICE = "99999999999999999991";
    private static final String CHANNEL = "99999999999999999992";
    private static final String START = "2026-10-27 10:00:00";
    private static final String END = "2026-10-27 10:02:00";
    private static final String STREAM = DEVICE + "_" + CHANNEL + "_20261027100000_20261027100200";
    private static final String OLD = "11111111-2222-3333-4444-555555555555";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
    private final SegmentedDownloadStore store = mock(SegmentedDownloadStore.class);
    private final NativeDownloadGateway gateway = mock(NativeDownloadGateway.class);
    private final IDialogTerminationService termination = mock(IDialogTerminationService.class);
    private final IDeviceService devices = mock(IDeviceService.class);
    private final IDeviceChannelService channels = mock(IDeviceChannelService.class);
    private final IMediaServerService mediaServers = mock(IMediaServerService.class);
    private final SipInviteSessionManager sessions = mock(SipInviteSessionManager.class);
    private final SegmentedArtifactFiles files = mock(SegmentedArtifactFiles.class);
    private Device device;
    private DeviceChannel channel;
    private MediaServer media;
    private SegmentedDownloadTask previous;
    private SegmentedDownloadService service;

    /** 原任务采用真实fileInfo字段形状，不为每个分段伪造不存在的节点或设备字段。 */
    @BeforeEach
    void setUp() {
        UserSetting user = new UserSetting();user.setServerId("resume-test");
        SegmentedDownloadSettings settings = new SegmentedDownloadSettings();
        settings.getDevices().add(DEVICE);settings.setPublicBaseUrl("https://example.invalid/record-download");
        device = new Device();device.setDeviceId(DEVICE);device.setServerId(user.getServerId());
        channel = new DeviceChannel();channel.setId(27);channel.setDeviceId(CHANNEL);
        media = new MediaServer();media.setId("resume-media");media.setServerId(user.getServerId());media.setRecordPath("/record");
        previous = new SegmentedDownloadTask();previous.setId(OLD);previous.setServerId(user.getServerId());
        previous.setDeviceId(DEVICE);previous.setChannelId(27);previous.setMediaServerId(media.getId());
        previous.setStream(STREAM);previous.setStartTime(START);previous.setEndTime(END);
        previous.setState(State.FAILED);previous.setSafeToStart(true);previous.setVersion(9);
        previous.setArtifactToken("a".repeat(64));previous.setSegmentIndex(2);
        previous.setSegments(new ArrayList<>(List.of(segment(START,"2026-10-27 10:01:00"),
                segment("2026-10-27 10:00:50","2026-10-27 10:02:20"))));
        when(store.get(STREAM)).thenAnswer(call -> copy(previous));
        when(devices.getDeviceByDeviceId(DEVICE)).thenReturn(device);
        when(channels.getOneForSourceById(27)).thenReturn(channel);
        when(channels.getOneForSource(DEVICE,CHANNEL)).thenReturn(channel);
        when(mediaServers.getOne(media.getId())).thenReturn(media);
        service = new SegmentedDownloadService(settings,store,gateway,termination,devices,channels,mediaServers,sessions,files,user);
    }

    /** 测试只关闭本地工作线程，不对设备发结束命令。 */
    @AfterEach
    void tearDown() { service.shutdown(); }

    /** 构造带完整状态与原区间的源片快照，保留父节点归属作为唯一节点证据。 */
    private SegmentedDownloadTask.Segment segment(String start, String end) {
        String stream = DEVICE + "_" + CHANNEL + "_" + start.replaceAll("[-: ]", "") + "_" + end.replaceAll("[-: ]", "");
        StreamInfo info = new StreamInfo();info.setApp("rtp");info.setStream(stream);
        info.setStartTime(start);info.setEndTime(end);info.setDownloadStatus("completed");info.setProgress(1);
        info.setDownloadedDuration((double)Duration.between(LocalDateTime.parse(start,TIME),LocalDateTime.parse(end,TIME)).getSeconds());
        DownloadFileInfo file = new DownloadFileInfo();file.setHttpsPath("https://example.invalid/index/api/downloadFile?file_path=%2Frecord%2Fsynthetic.mp4");info.setDownLoadFilePath(file);
        SegmentedDownloadTask.Segment result = new SegmentedDownloadTask.Segment();result.setStream(stream);result.setStartTime(start);result.setEndTime(end);result.setInfo(info);return result;
    }

    /** 使用持久化协议深复制，避免测试共享引用掩盖来源快照变化。 */
    private SegmentedDownloadTask copy(SegmentedDownloadTask task) { return JSON.parseObject(JSON.toJSONString(task),SegmentedDownloadTask.class); }

    /** 每次回调同时验证无设备侧调用，错误不得附带任何可用文件。 */
    private StreamInfo resume(int expectedCode, String id) {
        List<StreamInfo> results = new ArrayList<>();
        service.resumeMerge(device,channel,START,END,id,(code,message,info)->{
            assertEquals(expectedCode,code);results.add(info);
        });
        assertEquals(1,results.size());verifyNoInteractions(gateway,termination,sessions);
        if(expectedCode!=0)assertNull(results.getFirst());
        return results.getFirst();
    }

    /** 完整安全失败任务进入新轮合并，原来源不重新获取、清理或触碰NVR。 */
    @Test
    void resume_safeCompleteSources_returnsOnlyNewMergeRound() {
        when(store.resumeMerge(any(),eq(OLD),anyString(),eq(21600))).thenAnswer(call->{
            SegmentedDownloadTask original=call.getArgument(0);assertEquals(OLD,original.getId());
            assertEquals(ReflectionTestUtils.getField(service,"workerId"),call.getArgument(2));
            SegmentedDownloadTask next=copy(original);next.setId(UUID.randomUUID().toString());next.setResumedFromTaskId(OLD);next.setState(State.MERGING);return next;
        });
        StreamInfo result=resume(0,OLD);
        assertEquals(STREAM,result.getStream());assertEquals("finalizing",result.getDownloadStatus());
        assertEquals(0.99,result.getProgress());assertNull(result.getDownLoadFilePath());
        verify(files).validateReusableSources(any(),eq(media));verify(files,never()).cleanup(any());
        verify(store,never()).reserve(any());assertEquals(State.FAILED,previous.getState());
    }

    /** 同一旧UUID的已提交合并可找回，已完成结果仍必须有真实可交付文件。 */
    @Test
    void resume_duplicateMergingOrCompleted_findsSameRound() {
        previous.setId(UUID.randomUUID().toString());previous.setResumedFromTaskId(OLD);previous.setState(State.MERGING);
        when(store.resumeMerge(any(),eq(OLD),anyString(),anyInt())).thenAnswer(call->copy(previous));
        assertEquals("finalizing",resume(0,OLD).getDownloadStatus());
        previous.setState(State.COMPLETED);previous.setFinalFile(previous.getSegments().getFirst().getInfo().getDownLoadFilePath());
        when(files.resolve(previous.getArtifactToken())).thenReturn(Path.of("synthetic-artifact.mp4"));
        assertEquals("completed",resume(0,OLD).getDownloadStatus());
        assertEquals(1,resume(0,OLD).getProgress());
    }

    /** 初始COMPLETED、取消、未关闭和不完整来源均拒绝，不能因已存在URL放宽资格。 */
    @Test
    void resume_unsafeOrIncompleteSnapshot_neverReplacesOrDownloads() {
        SegmentedDownloadTask valid=copy(previous);
        List<Consumer<SegmentedDownloadTask>> changes=List.of(
                task->task.setState(State.COMPLETED),task->task.setState(State.CANCELLED),
                task->task.setState(State.MERGING),task->task.setSafeToStart(false),task->task.setCancelRequested(true),
                task->task.setServerId("other-server"),task->task.setStream("wrong-canonical"),task->task.setChannelId(28),task->task.setDeviceId("different-device"),task->task.setStartTime("2026-10-27 09:59:00"),
                task->task.setSegmentIndex(1),task->task.setSegments(List.of()),
                task->task.getSegments().get(0).getInfo().setDownloadStatus("interrupted"),
                task->task.getSegments().get(0).getInfo().setProgress(0.5),
                task->task.getSegments().get(0).getInfo().setDownloadedDuration(1.0),
                task->task.getSegments().get(0).getInfo().setDownloadedDuration(null),
                task->task.getSegments().get(0).getInfo().setDownLoadFilePath(null),
                task->task.getSegments().get(0).getInfo().setStream("another-stream"),
                task->task.setSegments(List.of(task.getSegments().get(1),task.getSegments().get(0))),
                task->task.getSegments().set(1,segment("2026-10-27 10:01:01","2026-10-27 10:02:20")),
                task->task.getSegments().set(1,segment("2026-10-27 10:00:50","2026-10-27 10:01:59")));
        for(Consumer<SegmentedDownloadTask> change:changes){previous=copy(valid);change.accept(previous);resume(-1,OLD);}
        verify(store,never()).resumeMerge(any(),anyString(),anyString(),anyInt());verify(store,never()).reserve(any());
    }

    /** 非法、空或不匹配UUID都不允许隐式退回普通下载。 */
    @Test
    void resume_missingOrDifferentIdentity_isRejected() {
        for(String id:List.of("","not-a-uuid","aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"))resume(-1,id);
        resume(-1,null);verify(store,never()).resumeMerge(any(),anyString(),anyString(),anyInt());
    }

    /** 已经重做失败或取消的同轮请求不能通过重复旧UUID重新启动。 */
    @Test
    void resume_duplicateFailedOrCancelled_isRejected() {
        previous.setId(UUID.randomUUID().toString());previous.setResumedFromTaskId(OLD);
        resume(-1,OLD);previous.setState(State.CANCELLED);resume(-1,OLD);
        verify(store,never()).resumeMerge(any(),anyString(),anyString(),anyInt());
    }

    /** 原URI校验和事务失败都保留错误结果，不能转为reserve或原生下载。 */
    @Test
    void resume_sourceOrCasFailure_isNotConvertedToNormalStart() {
        doThrow(new IllegalStateException("synthetic invalid source")).when(files).validateReusableSources(any(),any());
        resume(-1,OLD);verify(store,never()).resumeMerge(any(),anyString(),anyString(),anyInt());
        doNothing().when(files).validateReusableSources(any(),any());
        when(store.resumeMerge(any(),anyString(),anyString(),anyInt())).thenThrow(new IllegalStateException("synthetic cas conflict"));
        resume(-1,OLD);verify(store,never()).reserve(any());
    }

    /** 业务回调抛错不能再次向同一调用者投递失败或创建另一轮。 */
    @Test
    void resume_callbackFailure_isDeliveredOnlyOnce() {
        SegmentedDownloadTask next=copy(previous);next.setState(State.MERGING);
        when(store.resumeMerge(any(),anyString(),anyString(),anyInt())).thenReturn(next);
        ErrorCallback<StreamInfo> callback=mock(ErrorCallback.class);
        doThrow(new IllegalStateException("synthetic callback")).when(callback).run(eq(0),anyString(),any());
        assertThrows(IllegalStateException.class,()->service.resumeMerge(device,channel,START,END,OLD,callback));
        verify(callback,times(1)).run(anyInt(),anyString(),any());verifyNoInteractions(gateway,termination,sessions);
    }
}
