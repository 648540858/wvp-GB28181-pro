package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.common.ServerInfo;
import com.genersoft.iot.vmp.conf.trace.TraceIpUtil;
import com.genersoft.iot.vmp.gb28181.service.IPlatformService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 集群心跳任务。
 * <p>
 * 使用独立的单线程调度器，避免被其他 {@link org.springframework.scheduling.annotation.Scheduled}
 * 任务阻塞。心跳每秒写入一次，Redis key TTL 3 秒：
 * 连续 3 秒没有写入成功，其他节点即可认为本节点宕机。
 */
@Slf4j
@Component
public class WVPTimerTask implements InitializingBean, DisposableBean {

    /**
     * 心跳 key TTL，单位：秒
     */
    private static final long HEARTBEAT_TTL_SECONDS = 3;

    /**
     * 连续 3 秒没有写入成功，认为本节点已被其他节点判定为宕机
     */
    private static final long HEARTBEAT_TIMEOUT_MS = 3000;

    /**
     * 平台归属巡检周期：5 秒
     */
    private static final long OWNER_CHECK_INTERVAL_MS = 5000;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IPlatformService platformService;

    @Autowired
    private UserSetting userSetting;

    @Value("${server.port}")
    private int serverPort;

    @Autowired
    private SipConfig sipConfig;

    private ScheduledExecutorService heartbeatExecutor;

    /**
     * 最后一次成功写入心跳的时间（单调时钟）
     */
    private volatile long lastWriteOk = System.nanoTime();

    /**
     * 最后一次平台归属巡检时间（单调时钟）
     */
    private volatile long lastOwnerCheck = 0;

    /**
     * 数据库归属巡检连续失败次数
     */
    private int dbFailCount = 0;

    /**
     * 重复 serverId 预警的限频时间
     */
    private volatile long lastDuplicateWarnTime = 0;

    @Override
    public void afterPropertiesSet() {
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "wvp-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                heartbeat();
            } catch (Exception e) {
                log.warn("[集群] 心跳任务异常", e);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    @Override
    public void destroy() {
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
        }
    }

    private void heartbeat() {
        // 1. 自检：连续 3 秒没有写成功心跳，认为自己可能已经被判宕机
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastWriteOk);
        if (elapsed > HEARTBEAT_TIMEOUT_MS) {
            platformService.setFenced(true);
        }

        // 2. 写心跳前检查 serverId 是否被其他实例使用（默认认为不会重复，仅日志预警）
        checkDuplicateServerId();

        // 3. 写心跳（节点存活标识，和平台归属无关，只要节点活着就要写）
        try {
            redisCatchStorage.updateWVPInfo(ServerInfo.create(TraceIpUtil.resolveSipShowIp(sipConfig), serverPort),
                    (int) HEARTBEAT_TTL_SECONDS);
            lastWriteOk = System.nanoTime();
        } catch (Exception e) {
            log.warn("[集群] 心跳写入失败", e);
        }

        // 4. 每 5 秒做一次平台归属巡检
        long ownerCheckElapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastOwnerCheck);
        if (ownerCheckElapsed < OWNER_CHECK_INTERVAL_MS) {
            return;
        }
        lastOwnerCheck = System.nanoTime();

        long lastWriteElapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastWriteOk);
        boolean heartbeatHealthy = lastWriteElapsed <= HEARTBEAT_TIMEOUT_MS;
        if (platformService.isFenced() && !heartbeatHealthy) {
            // 心跳还没恢复，先不巡检
            return;
        }

        try {
            platformService.checkPlatformOwnership(heartbeatHealthy);
            dbFailCount = 0;
        } catch (Exception e) {
            dbFailCount++;
            log.warn("[集群] 平台归属巡检失败，连续失败次数：{}", dbFailCount, e);
            if (dbFailCount >= 3) {
                // 无法确认自己是否还是 owner，保守停止所有级联动作
                platformService.setFenced(true);
            }
        }
    }

    /**
     * 默认认为 serverId 唯一，这里只做日志预警：
     * 心跳 key 中的 IP/端口与本节点不一致，说明可能有另一个实例在用同一个 serverId。
     */
    private void checkDuplicateServerId() {
        try {
            ServerInfo existing = redisCatchStorage.queryServerInfo(userSetting.getServerId());
            if (existing == null) {
                return;
            }
            String myIp = sipConfig.getShowIp();
            boolean sameIp = myIp == null || myIp.equals(existing.getIp());
            boolean samePort = existing.getPort() == serverPort;
            if (sameIp && samePort) {
                return;
            }
            long now = System.currentTimeMillis();
            if (now - lastDuplicateWarnTime < 60000) {
                return;
            }
            lastDuplicateWarnTime = now;
            log.warn("[集群] 检测到 serverId={} 被其他实例使用，existing={}:{}，current={}:{}，请检查 serverId 是否重复",
                    userSetting.getServerId(), existing.getIp(), existing.getPort(), myIp, serverPort);
        } catch (Exception e) {
            log.warn("[集群] 检测重复 serverId 失败", e);
        }
    }
}
