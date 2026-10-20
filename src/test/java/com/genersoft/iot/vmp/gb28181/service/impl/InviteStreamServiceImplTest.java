package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.InviteInfo;
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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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
}
