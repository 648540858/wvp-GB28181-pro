package com.genersoft.iot.vmp.gat1400.backend.task.action;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.github.benmanes.caffeine.cache.Scheduler;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;

import cz.data.viid.framework.config.Constants;
import cz.data.viid.framework.domain.entity.NodeDevice;
import cz.data.viid.framework.service.APEDeviceService;
import cz.data.viid.framework.service.VIIDServerService;
import cz.data.viid.utils.DurationUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KeepaliveAction implements InitializingBean, DisposableBean {
    public static volatile String CURRENT_SERVER_ID = null;
    private Cache<String, NodeDevice> context;
    private ThreadPoolTaskExecutor executor;

    @Resource
    VIIDServerService serverService;
    @Resource
    APEDeviceService deviceService;

    @Override
    public void afterPropertiesSet() throws Exception {
        executor = createThreadPoolTaskExecutor("pool-caffeine-");
        InstanceRemovalListener listener = new InstanceRemovalListener(serverService, deviceService);
        context = Caffeine.newBuilder()
                //最大缓存个数3000
                .maximumSize(3000)
                //写入70秒后过期
                .expireAfterWrite(Duration.ofSeconds(180))
                //指定删除通知线程执行器
                .executor(executor)
                //调度器主动驱逐过期元素
                .scheduler(Scheduler.forScheduledExecutorService(DurationUtil.getScheduledThreadPool()))
                //缓存过期监听
                .removalListener(listener)
                .build();
    }

    @Override
    public void destroy() throws Exception {
        log.info("{}销毁", getClass());
        if (executor != null) {
            executor.shutdown();
        }
    }

    public NodeDevice get(String deviceId) {
        return context.getIfPresent(deviceId);
    }

    public void refresh(NodeDevice device) {
        NodeDevice node = context.getIfPresent(device.getDeviceId());
        if (Objects.nonNull(node)) {
            device.setOnline(node.getOnline());
            context.put(device.getDeviceId(), device);
        }
    }

    public NodeDevice keepalive(String deviceId) {
        NodeDevice device = context.getIfPresent(deviceId);
        if (Objects.nonNull(device)) {
            context.put(deviceId, device);
        }
        return device;
    }

    public void register(NodeDevice device) {
        context.put(device.getDeviceId(), device);
    }

    public void unregister(String deviceId) {
        context.invalidate(deviceId);
    }

    public Map<String, NodeDevice> allNode() {
        return context.asMap();
    }

    public boolean online(String deviceId) {
        NodeDevice domain = this.get(deviceId);
        if (Objects.nonNull(domain)) {
            return domain.getOnline();
        }
        return false;
    }

    public static class InstanceRemovalListener implements RemovalListener<String, NodeDevice> {

        private final VIIDServerService serverService;
        private final APEDeviceService deviceService;

        public InstanceRemovalListener(VIIDServerService serverService,
                                       APEDeviceService deviceService) {
            this.serverService = serverService;
            this.deviceService = deviceService;
        }

        @Override
        public void onRemoval(@Nullable String instanceId,
                              @Nullable NodeDevice instance,
                              @NonNull RemovalCause removalCause) {
            if (removalCause == RemovalCause.REPLACED) {
                //重新put缓存事件不处理
                return;
            }
            if (Objects.nonNull(instance)) {
                if (instance.isServer()) {
                    log.info("视图库下线更改在线状态: {}-{}", removalCause, instanceId);
                    serverService.instanceStatus(instanceId, Constants.DeviceStatus.Offline);
                } else {
                    log.info("采集设备下线更改在线状态: {}-{}", removalCause, instanceId);
                    deviceService.deviceStatus(instanceId, Constants.DeviceStatus.Offline);
                }
            }
        }
    }

    public static class CaffeineThreadFactory implements ThreadFactory {
        private static final Map<String, AtomicInteger> poolNumberMap = new ConcurrentHashMap<String, AtomicInteger>();

        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        public CaffeineThreadFactory(String name) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null)? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = "pool-" + name +"-t-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);

            if (t.isDaemon()) {
                t.setDaemon(false);
            }

            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }

            return t;
        }
    }

    private static ThreadPoolTaskExecutor createThreadPoolTaskExecutor(String prefixName) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(5);
        executor.setCorePoolSize(1);
        executor.setQueueCapacity(10);
        executor.setKeepAliveSeconds(300);
        // 线程池对拒绝任务(无线程可用)的处理策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setThreadNamePrefix(prefixName);
        executor.initialize();
        return executor;
    }
}
