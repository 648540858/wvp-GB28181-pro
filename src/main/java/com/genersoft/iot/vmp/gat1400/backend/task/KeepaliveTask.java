package com.genersoft.iot.vmp.gat1400.backend.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import cz.data.viid.be.task.action.KeepaliveAction;
import cz.data.viid.be.task.action.VIIDServerAction;
import cz.data.viid.framework.SpringContextHolder;
import cz.data.viid.framework.config.Constants;
import cz.data.viid.framework.domain.dto.ResponseStatusObject;
import cz.data.viid.framework.domain.entity.NodeDevice;
import cz.data.viid.framework.domain.entity.VIIDServer;
import cz.data.viid.framework.domain.vo.VIIDBaseResponse;
import cz.data.viid.framework.exception.VIIDRuntimeException;
import cz.data.viid.framework.service.VIIDServerService;
import cz.data.viid.listener.event.ServerOfflineEvent;
import cz.data.viid.listener.event.ServerOnlineEvent;

@Order(Integer.MIN_VALUE)
@Component
public class KeepaliveTask implements DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(KeepaliveTask.class);

    @Autowired
    KeepaliveAction keepaliveAction;
    @Autowired
    VIIDServerAction viidServerAction;
    @Autowired
    VIIDServerService viidServerService;

    /**
     * 主动保活 检查设置保活的视图库是否在线 不在线则注册重试
     */
    @Scheduled(initialDelay = 10, fixedDelay = 50, timeUnit = TimeUnit.SECONDS)
    public void keepalive() {
        List<VIIDServer> servers = viidServerService.activeKeepaliveServer();
        for (VIIDServer server : servers) {
            NodeDevice device = keepaliveAction.get(server.getServerId());
            if (device == null || Boolean.FALSE.equals(device.getOnline())) {
                try {
                    boolean success = success(viidServerAction.register(server));
                    if (!success) {
                        log.warn("视图库{}保活流程->[注册]动作失败", server.getServerId());
                        continue;
                    }
                    server.setOnline(Constants.DeviceStatus.Online.getValue());
                    device = NodeDevice.fromServer(server);
                    SpringContextHolder.publishEvent(new ServerOnlineEvent(device));
                } catch (Exception e) {
                    SpringContextHolder.publishEvent(new ServerOfflineEvent(server.getServerId()));
                    log.warn("保活失败: {}, 错误消息: {}", server.getServerId(), e.getMessage());
                    continue;
                }
            }
            log.info("主动保活设备: {}", device.getDeviceId());
            try {
                VIIDBaseResponse response = viidServerAction.keepalive(server);
                ResponseStatusObject statusObject = Optional.ofNullable(response)
                        .map(VIIDBaseResponse::getResponseStatusObject)
                        .orElseThrow(() -> new VIIDRuntimeException("主动保活错误"));
                if ("0".equals(statusObject.getStatusCode())) {
                    keepaliveAction.keepalive(device.getDeviceId());
                } else {
                    throw new VIIDRuntimeException(statusObject.toString());
                }
            } catch (Exception e) {
                log.warn("视图库[{}]保活流程->主动保活失败: {}", device.getDeviceId(), e.getMessage());
                SpringContextHolder.publishEvent(new ServerOfflineEvent(server.getServerId()));
            }
        }
    }

    @Override
    public void destroy() {
        log.info("{}销毁", getClass());
        for (Map.Entry<String, NodeDevice> entry : keepaliveAction.allNode().entrySet()) {
            String key = entry.getKey();
            NodeDevice device = entry.getValue();
            if (device.isServer()) {
                try {
                    VIIDServer value = device.originVIIDServer();
                    if (Boolean.TRUE.equals(value.getKeepalive())) {
                        log.info("下线主动注销视图库: {}", key);
                        viidServerAction.unRegister(value);
                    }
                } catch (Exception e) {
                    //调用注销可能会有403
                    log.warn("注销失败: {}, 错误消息: {}", key, e.getMessage());
                } finally {
                    keepaliveAction.unregister(device.getDeviceId());
                }
            } else {
                log.info("下线注销设备: {}", device.getDeviceId());
                keepaliveAction.unregister(device.getDeviceId());
            }
        }
    }

    private boolean success(VIIDBaseResponse response) {
        Optional<ResponseStatusObject> statusObject = Optional.ofNullable(response)
                .map(VIIDBaseResponse::getResponseStatusObject);
        statusObject.ifPresent(obj -> {
            if (!"0".equals(obj.getStatusCode())) {
                log.warn("请求错误响应: {}", obj);
            }
        });
        return statusObject.map(ResponseStatusObject::getStatusCode)
                .map("0"::equals)
                .orElse(false);
    }
}
