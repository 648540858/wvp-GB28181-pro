package com.genersoft.iot.vmp.gb28181.session;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.redis.RedisTemplateConfig;
import com.genersoft.iot.vmp.gb28181.bean.PendingDialogClose;
import com.genersoft.iot.vmp.gb28181.bean.SsrcTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** 专用Redis实际执行序列化与WATCH冲突，不以模拟EXEC结果替代驱动行为。 */
class PendingDialogCloseRedisIntegrationTest {
    private final List<LettuceConnectionFactory> factories = new ArrayList<>();
    private String serverId;
    private RedisTemplate<String, Object> competingRedis;
    private SipInviteSessionManager competing;

    /** 独立连接与随机节点身份隔离测试，不读取部署配置或清空公共数据库。 */
    @BeforeEach
    void setUp() {
        String host = System.getenv("WVP_TEST_REDIS_HOST");
        assumeTrue(host != null && !host.isBlank(), "未配置专用Redis");
        int port = Integer.parseInt(System.getenv().getOrDefault("WVP_TEST_REDIS_PORT", "6379"));
        serverId = "dialog-close-test-" + UUID.randomUUID();
        for (int i = 0; i < 2; i++) {
            LettuceClientConfiguration client = LettuceClientConfiguration.builder()
                    .commandTimeout(Duration.ofSeconds(3)).shutdownTimeout(Duration.ofMillis(100)).build();
            LettuceConnectionFactory factory = new LettuceConnectionFactory(
                    new RedisStandaloneConfiguration(host.trim(), port), client);
            factory.afterPropertiesSet();factory.start();factories.add(factory);
        }
        competingRedis = template(factories.get(1), null);
        competing = manager(competingRedis);
    }

    /** 仅删除本测试随机节点的三个精确Hash，连接由创建者关闭。 */
    @AfterEach
    void tearDown() {
        try {
            if (competingRedis != null) {
                competingRedis.delete(List.of(VideoManagerConstants.SIP_INVITE_SESSION_STREAM + serverId,
                        VideoManagerConstants.SIP_INVITE_SESSION_CALL_ID + serverId,
                        VideoManagerConstants.SIP_INVITE_SESSION_CALL_ID + "CLOSING_" + serverId));
            }
        } finally {
            factories.forEach(LettuceConnectionFactory::destroy);
        }
    }

    /** 活动索引连续冲突耗尽必须报告失败，恢复后只能清理原Call-ID。 */
    @Test
    void activeDeleteWatchAbortRetainsOriginalAndNewStreamOwner() {
        AtomicInteger conflicts = new AtomicInteger();
        SipInviteSessionManager primary = manager(template(factories.getFirst(), () -> {
            if (conflicts.getAndUpdate(value -> Math.max(0, value - 1)) > 0) {
                SsrcTransaction noise = transaction("noise-" + conflicts.get());
                noise.setStream("noise-stream-" + conflicts.get());competing.put(noise);
            }
        }));
        SsrcTransaction original = transaction("original");
        primary.put(original);
        assertEquals(original, competing.getSsrcTransactionByCallId("original"));
        conflicts.set(3);
        assertFalse(primary.removeByCallId("original"));
        assertNotNull(competing.getSsrcTransactionByCallId("original"));
        assertEquals("original", competing.getSsrcTransactionByStream("rtp", "same-stream").getCallId());
        primary.put(transaction("replacement"));
        assertTrue(primary.removeByCallId("original"));
        assertEquals("replacement", competing.getSsrcTransactionByStream("rtp", "same-stream").getCallId());
    }

    /** 待关闭凭据也只能在真实提交后删除，空EXEC不能让设备被误报为空闲。 */
    @Test
    void pendingDeleteWatchAbortKeepsDurableCloseIdentity() {
        AtomicInteger conflicts = new AtomicInteger();
        SipInviteSessionManager primary = manager(template(factories.getFirst(), () -> {
            if (conflicts.getAndUpdate(value -> Math.max(0, value - 1)) > 0) {
                String callId = "noise-" + conflicts.get();
                assertTrue(competing.updatePendingDialogClose(callId, null, pending(callId, "noise-attempt")));
            }
        }));
        assertTrue(primary.updatePendingDialogClose("original", null, pending("original", "first")));
        conflicts.set(3);
        assertFalse(primary.updatePendingDialogClose("original", "first", null));
        assertEquals("first", competing.getPendingDialogClose("original").getAttemptId());
        assertTrue(primary.updatePendingDialogClose("original", "first", null));
        assertNull(competing.getPendingDialogClose("original"));
    }

    /** 另一实例能恢复序列化凭据，旧应答不能覆盖新结束轮次。 */
    @Test
    void serializedCloseRoundTripRejectsLateAttempt() {
        SipInviteSessionManager primary = manager(template(factories.getFirst(), null));
        assertTrue(primary.updatePendingDialogClose("original", null, pending("original", "first")));
        assertEquals("first", competing.getPendingDialogClose("original").getAttemptId());
        assertTrue(competing.updatePendingDialogClose("original", "first", pending("original", "second")));
        assertFalse(primary.updatePendingDialogClose("original", "first", null));
        assertEquals("second", primary.getPendingDialogClose("original").getAttemptId());
        assertTrue(primary.getAll().stream().anyMatch(value -> "original".equals(value.getCallId())));
        assertTrue(primary.updatePendingDialogClose("original", "second", null));
        assertTrue(primary.getAll().isEmpty());
    }

    /** 原样执行真实事务，仅在EXEC之前用另一连接制造被WATCH键的并发变化。 */
    private RedisTemplate<String, Object> template(LettuceConnectionFactory factory, Runnable beforeExec) {
        RedisTemplate<String, Object> configured = new RedisTemplateConfig().redisTemplate(factory);
        RedisTemplate<String, Object> observed = new RedisTemplate<>() {
            @Override
            public List<Object> exec() {
                if (beforeExec != null) beforeExec.run();
                return super.exec();
            }
        };
        observed.setConnectionFactory(factory);
        observed.setKeySerializer(configured.getKeySerializer());observed.setHashKeySerializer(configured.getHashKeySerializer());
        observed.setValueSerializer(configured.getValueSerializer());observed.setHashValueSerializer(configured.getHashValueSerializer());
        observed.afterPropertiesSet();return observed;
    }

    /** 只注入随机服务身份和真实模板，不连接任何设备。 */
    private SipInviteSessionManager manager(RedisTemplate<String, Object> redis) {
        UserSetting settings = new UserSetting();settings.setServerId(serverId);
        SipInviteSessionManager manager = new SipInviteSessionManager();
        ReflectionTestUtils.setField(manager, "userSetting", settings);
        ReflectionTestUtils.setField(manager, "redisTemplate", redis);return manager;
    }

    /** 合成事务仅包含归属字段，不携带SIP路由或凭据。 */
    private SsrcTransaction transaction(String callId) {
        SsrcTransaction transaction = new SsrcTransaction();transaction.setCallId(callId);
        transaction.setDeviceId("synthetic-device");transaction.setChannelId(17);transaction.setType(InviteSessionType.DOWNLOAD);
        transaction.setApp("rtp");transaction.setStream("same-stream");return transaction;
    }

    /** 用本轮身份构造最小待关闭记录。 */
    private PendingDialogClose pending(String callId, String attemptId) {
        PendingDialogClose close = new PendingDialogClose();close.setTransaction(transaction(callId));
        close.setAttemptId(attemptId);close.setState(PendingDialogClose.PENDING);return close;
    }
}
