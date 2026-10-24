package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionStatus;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.DeviceMapper;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 点播等待回调的注册、通知和兜底清理测试。
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class InviteStreamServiceImplTest {

    private static final Integer CHANNEL_ID = 1;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private UserSetting userSetting;

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private DeviceChannelMapper deviceChannelMapper;

    @InjectMocks
    private InviteStreamServiceImpl inviteStreamService;

    @Test
    void call_shouldNotifyAllWaiters_andReleaseClaim() {
        ErrorCallback<StreamInfo> first = mock(ErrorCallback.class);
        ErrorCallback<StreamInfo> second = mock(ErrorCallback.class);
        assertTrue(inviteStreamService.onceAndFirst(InviteSessionType.PLAY, CHANNEL_ID, null, first));
        assertFalse(inviteStreamService.onceAndFirst(InviteSessionType.PLAY, CHANNEL_ID, null, second));

        StreamInfo streamInfo = new StreamInfo();
        inviteStreamService.call(InviteSessionType.PLAY, CHANNEL_ID, null,
                InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);

        verify(first).run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
        verify(second).run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);

        // 认领已经释放，后续请求可以重新发起
        assertTrue(inviteStreamService.onceAndFirst(InviteSessionType.PLAY, CHANNEL_ID, null,
                mock(ErrorCallback.class)));
    }

    @Test
    void call_shouldContinueNotifying_whenOneCallbackThrows() {
        ErrorCallback<StreamInfo> broken = mock(ErrorCallback.class);
        ErrorCallback<StreamInfo> normal = mock(ErrorCallback.class);
        doThrow(new RuntimeException("callback error"))
                .when(broken).run(anyInt(), anyString(), any());

        inviteStreamService.onceAndFirst(InviteSessionType.PLAY, CHANNEL_ID, null, broken);
        inviteStreamService.onceAndFirst(InviteSessionType.PLAY, CHANNEL_ID, null, normal);

        inviteStreamService.call(InviteSessionType.PLAY, CHANNEL_ID, null,
                InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), null);

        // 一个回调异常不能影响其他等待者
        verify(normal).run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), null);
    }

    @Test
    void cleanExpiredInviteCallbacks_shouldKeepFreshClaim() {
        when(userSetting.getPlayTimeout()).thenReturn(60_000);
        ErrorCallback<StreamInfo> callback = mock(ErrorCallback.class);
        assertTrue(inviteStreamService.onceAndFirst(InviteSessionType.PLAY, CHANNEL_ID, null, callback));

        inviteStreamService.cleanExpiredInviteCallbacks(System.currentTimeMillis());

        verifyNoInteractions(callback);
        // 认领仍然存在，新请求只能等待
        assertFalse(inviteStreamService.onceAndFirst(InviteSessionType.PLAY, CHANNEL_ID, null,
                mock(ErrorCallback.class)));
    }

    @Test
    void cleanExpiredInviteCallbacks_shouldNotifyTimeout_andReleaseClaim() {
        when(userSetting.getPlayTimeout()).thenReturn(60_000);
        mockEmptyInviteInfoScan();

        ErrorCallback<StreamInfo> callback = mock(ErrorCallback.class);
        assertTrue(inviteStreamService.onceAndFirst(InviteSessionType.PLAY, CHANNEL_ID, null, callback));

        // playTimeout * 3 之后仍未结束，触发兜底
        inviteStreamService.cleanExpiredInviteCallbacks(System.currentTimeMillis() + 180_001);

        verify(callback).run(eq(InviteErrorCode.ERROR_FOR_INVITE_TIMEOUT.getCode()),
                eq(InviteErrorCode.ERROR_FOR_INVITE_TIMEOUT.getMsg()), isNull());
        verifyNoInteractions(deviceChannelMapper);

        // 认领已经释放，后续点播可以重新发起
        assertTrue(inviteStreamService.onceAndFirst(InviteSessionType.PLAY, CHANNEL_ID, null,
                mock(ErrorCallback.class)));
    }

    @Test
    void cleanExpiredInviteCallbacks_shouldCleanStaleInviteInfoAndChannelState() {
        when(userSetting.getPlayTimeout()).thenReturn(60_000);

        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        Cursor<Map.Entry<Object, Object>> inviteInfoCursor = mock(Cursor.class);
        Cursor<Map.Entry<Object, Object>> emptyCursor = mock(Cursor.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        // 第一次 scan 是兜底清理查询旧 InviteInfo，第二次 scan 是 removeInviteInfo 内部查询 key
        when(hashOperations.scan(anyString(), any(ScanOptions.class)))
                .thenReturn(inviteInfoCursor, emptyCursor);

        InviteInfo staleInviteInfo = new InviteInfo();
        staleInviteInfo.setType(InviteSessionType.PLAY);
        staleInviteInfo.setChannelId(CHANNEL_ID);
        staleInviteInfo.setCreateTime(System.currentTimeMillis() - 10_000);
        Map.Entry<Object, Object> staleEntry = Map.entry("PLAY:1:stream", staleInviteInfo);
        when(inviteInfoCursor.hasNext()).thenReturn(true);
        when(inviteInfoCursor.next()).thenReturn(staleEntry);
        when(emptyCursor.hasNext()).thenReturn(false);

        ErrorCallback<StreamInfo> callback = mock(ErrorCallback.class);
        assertTrue(inviteStreamService.onceAndFirst(InviteSessionType.PLAY, CHANNEL_ID, null, callback));

        inviteStreamService.cleanExpiredInviteCallbacks(System.currentTimeMillis() + 180_001);

        verify(callback).run(eq(InviteErrorCode.ERROR_FOR_INVITE_TIMEOUT.getCode()),
                eq(InviteErrorCode.ERROR_FOR_INVITE_TIMEOUT.getMsg()), isNull());
        // 确认是旧会话后，回收通道播放状态
        verify(deviceChannelMapper).stopPlayById(CHANNEL_ID);
    }

    @Test
    void once_shouldAlsoBeRegisteredWithExpireTime() {
        when(userSetting.getPlayTimeout()).thenReturn(60_000);
        mockEmptyInviteInfoScan();

        ErrorCallback<StreamInfo> callback = mock(ErrorCallback.class);
        inviteStreamService.once(InviteSessionType.PLAY, CHANNEL_ID, null, callback);

        inviteStreamService.cleanExpiredInviteCallbacks(System.currentTimeMillis() + 180_001);

        verify(callback).run(eq(InviteErrorCode.ERROR_FOR_INVITE_TIMEOUT.getCode()),
                eq(InviteErrorCode.ERROR_FOR_INVITE_TIMEOUT.getMsg()), isNull());
    }

    @Test
    void updateInviteInfoForSSRC_shouldNotClearWaitingCallbacks() {
        // 单端口设备 200OK 中 SSRC 与请求不一致时，会走 updateInviteInfoForSSRC。
        // 该流程内部会先 removeInviteInfo 再重新写入，不能因此丢弃等待中的回调。
        InviteInfo inviteInfoInDb = new InviteInfo();
        inviteInfoInDb.setType(InviteSessionType.PLAY);
        inviteInfoInDb.setChannelId(CHANNEL_ID);
        inviteInfoInDb.setStream("stream-1");

        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        Cursor<Map.Entry<Object, Object>> inviteInfoCursor = mock(Cursor.class);
        Cursor<Map.Entry<Object, Object>> emptyCursor = mock(Cursor.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.scan(anyString(), any(ScanOptions.class)))
                .thenReturn(inviteInfoCursor, emptyCursor);
        when(inviteInfoCursor.hasNext()).thenReturn(true);
        when(inviteInfoCursor.next()).thenReturn(Map.entry("PLAY:1:stream-1", inviteInfoInDb));
        when(emptyCursor.hasNext()).thenReturn(false);

        ErrorCallback<StreamInfo> callback = mock(ErrorCallback.class);
        assertTrue(inviteStreamService.onceAndFirst(InviteSessionType.PLAY, CHANNEL_ID, null, callback));

        InviteInfo updated = inviteStreamService.updateInviteInfoForSSRC(inviteInfoInDb, "new-ssrc");
        assertNotNull(updated, "SSRC 更新流程应该成功");
        verify(hashOperations).put(anyString(), anyString(), any(InviteInfo.class));

        StreamInfo streamInfo = new StreamInfo();
        inviteStreamService.call(InviteSessionType.PLAY, CHANNEL_ID, null,
                InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
        verify(callback).run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
    }

    /**
     * 兜底清理会先查询 Redis 中的 InviteInfo，这里模拟“已经不存在”。
     */
    private void mockEmptyInviteInfoScan() {
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        Cursor<Map.Entry<Object, Object>> cursor = mock(Cursor.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.scan(anyString(), any(ScanOptions.class))).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(false);
    }

    @Test
    void execute_shouldKeepDownloadInviteInfoWithinCleanTime() {
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.size(anyString())).thenReturn(1L);
        InviteInfo inviteInfo = downloadInviteInfo(System.currentTimeMillis() + 60_000);
        List<Object> values = new ArrayList<>();
        values.add(inviteInfo);
        when(hashOperations.values(anyString())).thenReturn(values);

        inviteStreamService.execute();

        // 保留窗口未到期，下载会话不能被回收
        verify(hashOperations, never()).delete(anyString(), any());
    }

    @Test
    void execute_shouldRemoveDownloadInviteInfoAfterCleanTime() {
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.size(anyString())).thenReturn(1L);
        InviteInfo inviteInfo = downloadInviteInfo(System.currentTimeMillis() - 1);
        List<Object> values = new ArrayList<>();
        values.add(inviteInfo);
        when(hashOperations.values(anyString())).thenReturn(values);

        // 第 1 次 scan：按 type:channelId:stream 读取条目；第 2 次 scan：removeInviteInfo 内部读取待删除的 key
        Cursor<Map.Entry<Object, Object>> inviteInfoCursor = mock(Cursor.class);
        Cursor<Map.Entry<Object, Object>> keyCursor = mock(Cursor.class);
        when(hashOperations.scan(anyString(), any(ScanOptions.class)))
                .thenReturn(inviteInfoCursor, keyCursor);
        when(inviteInfoCursor.hasNext()).thenReturn(true, false);
        when(inviteInfoCursor.next()).thenReturn(Map.entry("DOWNLOAD:" + CHANNEL_ID + ":stream", inviteInfo));
        when(keyCursor.hasNext()).thenReturn(true, false);
        when(keyCursor.next()).thenReturn(Map.entry("DOWNLOAD:" + CHANNEL_ID + ":stream", inviteInfo));

        inviteStreamService.execute();

        verify(hashOperations).delete(anyString(), eq("DOWNLOAD:" + CHANNEL_ID + ":stream"));
    }

    @Test
    void execute_shouldNotRemoveDownloadInviteInfoOfNewSessionWithSameStream() {
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.size(anyString())).thenReturn(1L);
        InviteInfo expiredInviteInfo = downloadInviteInfo(System.currentTimeMillis() - 1);
        expiredInviteInfo.setCreateTime(System.currentTimeMillis() - 10_000);
        List<Object> values = new ArrayList<>();
        values.add(expiredInviteInfo);
        when(hashOperations.values(anyString())).thenReturn(values);

        // 同一 stream 上已经发起了新一轮下载（createTime 不同），不能按旧快照删除
        InviteInfo newInviteInfo = downloadInviteInfo(System.currentTimeMillis() - 1);
        newInviteInfo.setCreateTime(System.currentTimeMillis());
        Cursor<Map.Entry<Object, Object>> inviteInfoCursor = mock(Cursor.class);
        when(hashOperations.scan(anyString(), any(ScanOptions.class))).thenReturn(inviteInfoCursor);
        when(inviteInfoCursor.hasNext()).thenReturn(true, false);
        when(inviteInfoCursor.next())
                .thenReturn(Map.entry("DOWNLOAD:" + CHANNEL_ID + ":stream", newInviteInfo));

        inviteStreamService.execute();

        verify(hashOperations, never()).delete(anyString(), any());
    }

    @Test
    void setInviteInfoCleanTime_shouldWriteCleanTimeWithKeepWindow() {
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        InviteInfo inviteInfo = downloadInviteInfo(null);
        Cursor<Map.Entry<Object, Object>> inviteInfoCursor = mock(Cursor.class);
        when(hashOperations.scan(anyString(), any(ScanOptions.class))).thenReturn(inviteInfoCursor);
        when(inviteInfoCursor.hasNext()).thenReturn(true, false);
        when(inviteInfoCursor.next())
                .thenReturn(Map.entry("DOWNLOAD:" + CHANNEL_ID + ":stream", inviteInfo));

        long before = System.currentTimeMillis();
        inviteStreamService.setInviteInfoCleanTime(inviteInfo, 15 * 60 * 1000L);

        assertNotNull(inviteInfo.getCleanTime(), "保留窗口应该写入缓存对象");
        assertTrue(inviteInfo.getCleanTime() >= before + 15 * 60 * 1000L);
        verify(hashOperations).put(anyString(), eq("DOWNLOAD:" + CHANNEL_ID + ":stream"), eq(inviteInfo));
    }

    @Test
    void getInviteInfoByStreamPreferPlay_shouldReturnPlaybackInsteadOfRetainedDownload() {
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        Cursor<Map.Entry<Object, Object>> emptyCursor = mock(Cursor.class);
        Cursor<Map.Entry<Object, Object>> playbackCursor = mock(Cursor.class);
        InviteInfo playbackInviteInfo = downloadInviteInfo(null);
        playbackInviteInfo.setType(InviteSessionType.PLAYBACK);
        when(hashOperations.scan(anyString(), any(ScanOptions.class))).thenReturn(emptyCursor, playbackCursor);
        when(emptyCursor.hasNext()).thenReturn(false);
        when(playbackCursor.hasNext()).thenReturn(true, false);
        when(playbackCursor.next())
                .thenReturn(Map.entry("PLAYBACK:" + CHANNEL_ID + ":stream", playbackInviteInfo));

        InviteInfo result = inviteStreamService.getInviteInfoByStreamPreferPlay("stream");

        assertSame(playbackInviteInfo, result, "存在回放会话时不能命中保留中的下载会话");
    }

    private InviteInfo downloadInviteInfo(Long cleanTime) {
        InviteInfo inviteInfo = new InviteInfo();
        inviteInfo.setDeviceId("34020000001320000001");
        inviteInfo.setChannelId(CHANNEL_ID);
        inviteInfo.setStream("stream");
        inviteInfo.setType(InviteSessionType.DOWNLOAD);
        inviteInfo.setStatus(InviteSessionStatus.ok);
        inviteInfo.setCreateTime(System.currentTimeMillis());
        inviteInfo.setCleanTime(cleanTime);
        StreamInfo streamInfo = new StreamInfo();
        streamInfo.setStream("stream");
        inviteInfo.setStreamInfo(streamInfo);
        return inviteInfo;
    }
}
