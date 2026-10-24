package com.genersoft.iot.vmp.gb28181.session;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.local.InMemoryStore;
import com.genersoft.iot.vmp.conf.local.LocalMsgBus;
import com.genersoft.iot.vmp.conf.local.RedisTemplate;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 覆盖 #2216 的第 3 条根因：清理旧 Call-ID 时不得删掉已被新会话覆盖的 stream 索引。
 *
 * <p>测试直接使用单机模式的内存 Redis 门面（{@link InMemoryStore}），同时覆盖
 * {@code execute(SessionCallback)} 在内存模式下的事务语义。</p>
 */
class SipInviteSessionManagerTest {

    private static final String SERVER_ID = "test-server";
    private static final String APP = "rtp";
    private static final String STREAM = "34020000001320000001_34020000001320000001";
    private static final String OLD_CALL_ID = "old-call-id";
    private static final String NEW_CALL_ID = "new-call-id";

    private SipInviteSessionManager sessionManager;

    @BeforeEach
    void setUp() {
        UserSetting userSetting = new UserSetting();
        userSetting.setServerId(SERVER_ID);

        sessionManager = new SipInviteSessionManager();
        ReflectionTestUtils.setField(sessionManager, "userSetting", userSetting);
        // 未配置 Redis 时使用内存门面，覆盖 execute(SessionCallback) 的内存实现
        ReflectionTestUtils.setField(sessionManager, "redisTemplate",
                new RedisTemplate<String, Object>(new InMemoryStore(), new LocalMsgBus(), null));
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
    }

    @Test
    void removeByCallId_shouldDeleteStreamIndexWhenItStillPointsToSameTransaction() {
        sessionManager.put(transaction(OLD_CALL_ID));

        sessionManager.removeByCallId(OLD_CALL_ID);

        assertNull(sessionManager.getSsrcTransactionByCallId(OLD_CALL_ID));
        assertNull(sessionManager.getSsrcTransactionByStream(APP, STREAM));
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
