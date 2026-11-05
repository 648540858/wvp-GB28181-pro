package com.genersoft.iot.vmp.conf.trace;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 国标级联链路追踪事件发布器。
 * <p>
 * 默认关闭，通过 {@code trace.enabled=true} 开启。
 * 节点名固定使用 IP:serverPort，不再需要单独配置。
 */
@Slf4j
@Component
public class TracePublisher implements DisposableBean {

    @Value("${trace.enabled:false}")
    private boolean enabled;

    @Value("${server.port}")
    private int serverPort;

    @Autowired
    private SipConfig sipConfig;

    @Autowired
    private UserSetting userSetting;

    /**
     * 独立线程发布，避免阻塞 SIP / Redis 处理流程
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "wvp-trace-publisher");
        thread.setDaemon(true);
        return thread;
    });

    public boolean isEnabled() {
        return enabled;
    }

    public void publish(TraceEvent event) {
        if (!enabled || event == null) {
            return;
        }
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getType() == null) {
            event.setType("ALARM");
        }
        if (event.getServerId() == null) {
            event.setServerId(userSetting.getServerId());
        }
        if (event.getNodeName() == null) {
            event.setNodeName(buildNodeName());
        }
        if (event.getNode() == null) {
            event.setNode(buildNode());
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(System.currentTimeMillis());
        }
        try {
            executor.execute(() -> {
                try {
                    TraceChannel.push(event);
                } catch (Exception e) {
                    log.warn("[Trace] 推送事件失败", e);
                }
            });
        } catch (Exception e) {
            // 应用关闭时线程池可能已经停止，忽略即可
            log.debug("[Trace] 发布器已停止，忽略事件");
        }
    }

    private String buildNodeName() {
        return TraceIpUtil.resolveSipShowIp(sipConfig) + ":" + serverPort;
    }

    private String buildNode() {
        Integer sipPort = sipConfig.getPort();
        return TraceIpUtil.resolveSipShowIp(sipConfig) + ":" + (sipPort != null ? sipPort : "");
    }

    @Override
    public void destroy() {
        executor.shutdownNow();
    }
}
