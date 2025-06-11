package com.genersoft.iot.vmp.gat1400.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import javax.annotation.Resource;

import cz.data.viid.be.task.SocketClientTask;
import cz.data.viid.be.task.action.KeepaliveAction;
import cz.data.viid.framework.SpringContextHolder;
import cz.data.viid.framework.config.Constants;
import cz.data.viid.framework.domain.entity.APEDevice;
import cz.data.viid.framework.domain.entity.NodeDevice;
import cz.data.viid.framework.domain.entity.VIIDPublish;
import cz.data.viid.framework.domain.entity.VIIDServer;
import cz.data.viid.framework.service.APEDeviceService;
import cz.data.viid.framework.service.VIIDServerService;
import cz.data.viid.kafka.KafkaStartupService;
import cz.data.viid.listener.event.DeviceChangeEvent;
import cz.data.viid.listener.event.ServerChangeEvent;
import cz.data.viid.listener.event.ServerOfflineEvent;
import cz.data.viid.listener.event.ServerOnlineEvent;
import cz.data.viid.listener.event.VIIDPublishActiveEvent;
import cz.data.viid.listener.event.VIIDPublishInactiveEvent;
import cz.data.viid.listener.event.WebSocketCloseEvent;
import cz.data.viid.utils.DurationUtil;
import cz.data.viid.utils.StructCodec;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SpringEventListener {

    @Autowired
    KeepaliveAction keepaliveAction;
    @Resource
    KafkaStartupService kafkaStartupService;
    @Resource
    SocketClientTask socketClientTask;

    @EventListener(ApplicationReadyEvent.class)
    public void applicationReady(ApplicationReadyEvent event) {
        ConfigurableApplicationContext context = event.getApplicationContext();
        ConfigurableEnvironment environment = context.getEnvironment();
        StructCodec.VIID_AREA_CODE = environment.getProperty("VIID_AREA", "431002");
        VIIDServerService viidServerService = context.getBean(VIIDServerService.class);
        viidServerService.afterPropertiesSet();
        VIIDServer server = viidServerService.getCurrentServer();
        KeepaliveAction.CURRENT_SERVER_ID = server.getServerId();
        String banner = "\n-----------------------------------------\n" +
                "服务启动成功，时间：" + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()) + "\n" +
                "服务名称：" + environment.getProperty("spring.application.name") + "\n" +
                "注册地址：127.0.0.1:" + environment.getProperty("server.port") + "\n" +
                "-----------------------------------------";
        log.info(banner);
    }

    @EventListener(WebSocketCloseEvent.class)
    public void websocketClose(WebSocketCloseEvent event) {
        log.info("接收到websocketClose事件,标记视图库下线");
        VIIDServer server = event.getVIIDServer();
        String serverId = server.getServerId();
        kafkaStartupService.serverPublishStop(serverId);
        socketClientTask.serverSocketRemove(serverId);
    }

    @EventListener(ServerOnlineEvent.class)
    public void serverOnline(ServerOnlineEvent event) {
        NodeDevice device = event.getVIIDServer();
        String deviceId = device.getDeviceId();
        keepaliveAction.register(device);
        if (device.isServer()) {
            SpringContextHolder.getBean(VIIDServerService.class).instanceStatus(deviceId, Constants.DeviceStatus.Online);
            log.info("接收到视图库[{}]上线事件,加载订阅任务", deviceId);
            VIIDServer server = device.originVIIDServer();
            if (Constants.InstanceCategory.DOWN.getValue().equals(server.getCategory()) &&
                    Constants.VIID_SERVER.TRANSMISSION.WEBSOCKET.equals(server.getTransmission())) {
                DurationUtil.schedule(Duration.ofSeconds(3), () -> socketClientTask.serverSocketAdd(server));
            } else {
                DurationUtil.schedule(Duration.ofSeconds(3), () -> kafkaStartupService.serverPublishIdle(deviceId));
            }
        } else {
            log.info("接收到设备[{}]上线事件", deviceId);
            SpringContextHolder.getBean(APEDeviceService.class)
                    .deviceStatus(device.getDeviceId(), Constants.DeviceStatus.Online);
        }
    }

    @EventListener(ServerOfflineEvent.class)
    public void serverOffline(ServerOfflineEvent event) {
        String deviceId = event.getDeviceId();
        log.info("接收到设备[{}]下线事件,刷新缓存", deviceId);
        NodeDevice device = keepaliveAction.get(deviceId);
        if (Objects.nonNull(device)) {
            keepaliveAction.unregister(deviceId);
            if (NodeDevice.Category.Server.equals(device.getCategory())) {
                kafkaStartupService.serverPublishStop(deviceId);
            }
        }
    }

    @EventListener(ServerChangeEvent.class)
    public void serverChange(ServerChangeEvent event) {
        log.info("接收到视图库变更事件,刷新缓存");
        String serverId = event.getServerId();
        VIIDServer server = SpringContextHolder.getBean(VIIDServerService.class).getByIdAndEnabled(serverId);
        if (Objects.nonNull(server)) {
            keepaliveAction.refresh(NodeDevice.fromServer(server));
        } else {
            SpringContextHolder.publishEvent(new ServerOfflineEvent(serverId));
        }
    }

    @EventListener(DeviceChangeEvent.class)
    public void deviceChange(DeviceChangeEvent event) {
        log.info("接收到设备变更事件,刷新缓存");
        String deviceId = event.getDeviceId();
        APEDevice device = SpringContextHolder.getBean(APEDeviceService.class).getById(deviceId);
        if (Objects.nonNull(device)) {
            keepaliveAction.refresh(NodeDevice.fromDevice(device));
        } else {
            keepaliveAction.unregister(deviceId);
        }
    }

    @EventListener(VIIDPublishActiveEvent.class)
    public void publishActive(VIIDPublishActiveEvent event) {
        VIIDPublish publish = event.getPublish();
        String serverId = publish.getServerId();
        boolean online = keepaliveAction.online(serverId);
        log.info("节点{}状态{}接收到发布上线事件: {}-{}", serverId, online, publish.getSubscribeId(), publish.getTitle());
        if (online) {
            kafkaStartupService.startPublish(publish);
        }
    }

    @EventListener(VIIDPublishInactiveEvent.class)
    public void publishInactive(VIIDPublishInactiveEvent event) {
        VIIDPublish publish = event.getPublish();
        String serverId = publish.getServerId();
        log.info("节点{}接收到发布下线事件: {}-{}", serverId, publish.getSubscribeId(), publish.getTitle());
        kafkaStartupService.stopPublish(publish);
    }
}
