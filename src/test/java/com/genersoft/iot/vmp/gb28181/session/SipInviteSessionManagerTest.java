package com.genersoft.iot.vmp.gb28181.session;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 覆盖 #2216 的第 3 条根因：清理旧 Call-ID 时不得删掉已被新会话覆盖的 stream 索引。
 */
@ExtendWith(MockitoExtension.class)
class SipInviteSessionManagerTest {

    private static final String SERVER_ID = "test-server";
    private static final String APP = "rtp";
    private static final String STREAM = "34020000001320000001_34020000001320000001";
    private static final String OLD_CALL_ID = "old-call-id";
    private static final String NEW_CALL_ID = "new-call-id";

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private RedisOperations<String, Object> redisOperations;

    /** 用内存 Map 模拟 Redis Hash，便于断言键是否真的被删除 */
    private final Map<String, Map<Object, Object>> store = new HashMap<>();

    private SipInviteSessionManager sessionManager;

    @BeforeEach
    void setUp() {
        UserSetting userSetting = new UserSetting();
        userSetting.setServerId(SERVER_ID);

        sessionManager = new SipInviteSessionManager();
        ReflectionTestUtils.setField(sessionManager, "userSetting", userSetting);
        ReflectionTestUtils.setField(sessionManager, "redisTemplate", redisTemplate);

        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(hashOperations.get(anyString(), any())).thenAnswer(
                invocation -> bucket(invocation.getArgument(0)).get(invocation.getArgument(1)));
        lenient().doAnswer(invocation -> {
            bucket(invocation.getArgument(0)).put(invocation.getArgument(1), invocation.getArgument(2));
            return null;
        }).when(hashOperations).put(anyString(), any(), any());
        lenient().when(hashOperations.delete(anyString(), any())).thenAnswer(invocation -> {
            bucket(invocation.getArgument(0)).remove(invocation.getArgument(1));
            return 1L;
        });
        lenient().when(hashOperations.values(anyString())).thenAnswer(invocation ->
                new ArrayList<>(bucket(invocation.getArgument(0)).values()));

        lenient().when(redisOperations.opsForHash()).thenReturn(hashOperations);
        lenient().when(redisOperations.exec()).thenReturn(Collections.emptyList());
        lenient().doNothing().when(redisOperations).multi();
        lenient().doNothing().when(redisOperations).watch(anyString());
        lenient().doNothing().when(redisOperations).unwatch();
        lenient().when(redisTemplate.execute(any(SessionCallback.class))).thenAnswer(invocation -> {
            SessionCallback<?> callback = invocation.getArgument(0);
            return callback.execute(redisOperations);
        });
    }

    private Map<Object, Object> bucket(String key) {
        return store.computeIfAbsent(key, k -> new HashMap<>());
    }

    private String streamKey() {
        return VideoManagerConstants.SIP_INVITE_SESSION_STREAM + SERVER_ID;
    }

    private String callIdKey() {
        return VideoManagerConstants.SIP_INVITE_SESSION_CALL_ID + SERVER_ID;
    }

    private static SsrcTransaction transaction(String callId) {
        SsrcTransaction transaction = new SsrcTransaction();
        transaction.setApp(APP);
        transaction.setStream(STREAM);
        transaction.setCallId(callId);
        return transaction;
    }

    @Test
    void removeByCallId_shouldKeepStreamIndexOwnedByNewerTransaction() {
        // 第一次点播，写入旧事务
        sessionManager.put(transaction(OLD_CALL_ID));
        // 同一通道第二次点播，stream 索引被新事务覆盖（Call-ID 索引仍然保留两个事务）
        sessionManager.put(transaction(NEW_CALL_ID));
        assertEquals(NEW_CALL_ID, sessionManager.getSsrcTransactionByStream(APP, STREAM).getCallId());

        // 清理旧的 Call-ID
        sessionManager.removeByCallId(OLD_CALL_ID);

        // 旧事务被删除，但新事务的 stream 索引必须保留，否则后续 stop 无法发送 BYE
        assertNull(sessionManager.getSsrcTransactionByCallId(OLD_CALL_ID));
        assertEquals(NEW_CALL_ID, sessionManager.getSsrcTransactionByCallId(NEW_CALL_ID).getCallId());
        assertEquals(NEW_CALL_ID, sessionManager.getSsrcTransactionByStream(APP, STREAM).getCallId());
        assertTrue(bucket(streamKey()).containsKey(APP + STREAM));
    }

    @Test
    void removeByCallId_shouldDeleteStreamIndexWhenItStillPointsToSameTransaction() {
        sessionManager.put(transaction(OLD_CALL_ID));

        sessionManager.removeByCallId(OLD_CALL_ID);

        assertNull(sessionManager.getSsrcTransactionByCallId(OLD_CALL_ID));
        assertNull(sessionManager.getSsrcTransactionByStream(APP, STREAM));
        assertFalse(bucket(streamKey()).containsKey(APP + STREAM));
    }

    @Test
    void isStreamIndexOwnedBy_shouldHandleNulls() {
        assertFalse(SipInviteSessionManager.isStreamIndexOwnedBy(null, OLD_CALL_ID));
        assertFalse(SipInviteSessionManager.isStreamIndexOwnedBy(transaction(OLD_CALL_ID), null));
        assertFalse(SipInviteSessionManager.isStreamIndexOwnedBy(transaction(NEW_CALL_ID), OLD_CALL_ID));
        assertTrue(SipInviteSessionManager.isStreamIndexOwnedBy(transaction(OLD_CALL_ID), OLD_CALL_ID));
    }

    @Test
    void getSsrcTransactionByCallId_shouldFallbackToSipTransactionInfoCallId() {
        SsrcTransaction transaction = transaction(OLD_CALL_ID);
        SipTransactionInfo sipTransactionInfo = new SipTransactionInfo();
        sipTransactionInfo.setCallId(NEW_CALL_ID);
        transaction.setSipTransactionInfo(sipTransactionInfo);
        sessionManager.put(transaction);

        assertNotNull(sessionManager.getSsrcTransactionByCallId(NEW_CALL_ID));

        assertTrue(sessionManager.removeByCallId(NEW_CALL_ID));
        assertNull(sessionManager.getSsrcTransactionByCallId(NEW_CALL_ID));
        assertNull(sessionManager.getSsrcTransactionByStream(APP, STREAM));
    }

    @Test
    void getSsrcTransactionsByDeviceChannel_shouldFilterByDeviceChannelAndType() {
        SsrcTransaction first = transaction(OLD_CALL_ID);
        first.setDeviceId("device-1");
        first.setChannelId(1);
        first.setType(InviteSessionType.PLAY);
        sessionManager.put(first);

        SsrcTransaction second = transaction(NEW_CALL_ID);
        second.setDeviceId("device-1");
        second.setChannelId(2);
        second.setType(InviteSessionType.PLAY);
        sessionManager.put(second);

        assertEquals(1, sessionManager.getSsrcTransactionsByDeviceChannel(
                "device-1", 1, InviteSessionType.PLAY).size());
        assertEquals(0, sessionManager.getSsrcTransactionsByDeviceChannel(
                "device-1", 1, InviteSessionType.PLAYBACK).size());
    }

    @Test
    void removeByStreamIfSsrc_shouldNotDeleteNewerTransaction() {
        SsrcTransaction oldTransaction = transaction(OLD_CALL_ID);
        oldTransaction.setSsrc("old-ssrc");
        sessionManager.put(oldTransaction);

        SsrcTransaction newTransaction = transaction(NEW_CALL_ID);
        newTransaction.setSsrc("new-ssrc");
        sessionManager.put(newTransaction);

        assertFalse(sessionManager.removeByStreamIfSsrc(APP, STREAM, "old-ssrc"));
        assertEquals(NEW_CALL_ID, sessionManager.getSsrcTransactionByStream(APP, STREAM).getCallId());

        assertTrue(sessionManager.removeByStreamIfSsrc(APP, STREAM, "new-ssrc"));
        assertNull(sessionManager.getSsrcTransactionByStream(APP, STREAM));
    }
}
