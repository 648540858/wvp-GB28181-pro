package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.media.event.MediaServerChangeEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.ZLMServerConfig;
import com.genersoft.iot.vmp.media.zlm.event.HookZlmServerKeepaliveEvent;
import com.genersoft.iot.vmp.media.zlm.event.HookZlmServerStartEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理zlm流媒体节点的状态
 */
@Component
public class ZLMMediaServerStatusManger {

    private final static Logger logger = LoggerFactory.getLogger(ZLMMediaServerStatusManger.class);

    private final Map<Object, MediaServerItem> offlineZlmPrimaryMap = new ConcurrentHashMap<>();
    private final Map<Object, MediaServerItem> offlineZlmsecondaryMap = new ConcurrentHashMap<>();
    private final Map<Object, Long> offlineZlmTimeMap = new ConcurrentHashMap<>();

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private DynamicTask dynamicTask;

    private final String type = "zlm";

    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(MediaServerChangeEvent event) {
        if (event.getMediaServerItemList() == null
                || event.getMediaServerItemList().isEmpty()) {
            return;
        }
        for (MediaServerItem mediaServerItem : event.getMediaServerItemList()) {
            if (!type.equals(mediaServerItem.getType())) {
                continue;
            }
            logger.info("[ZLM-添加待上线节点] ID：" + mediaServerItem.getId());
            offlineZlmPrimaryMap.put(mediaServerItem.getId(), mediaServerItem);
            offlineZlmTimeMap.put(mediaServerItem.getId(), System.currentTimeMillis());
        }
    }

    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(HookZlmServerStartEvent event) {
        if (event.getMediaServerItem() == null
                || !type.equals(event.getMediaServerItem().getType())
                || event.getMediaServerItem().isStatus()) {
            return;
        }
        MediaServerItem serverItem = mediaServerService.getOne(event.getMediaServerItem().getId());
        if (serverItem == null) {
            return;
        }
        logger.info("[ZLM-HOOK事件-服务启动] ID：" + event.getMediaServerItem().getId());
        online(serverItem);
    }

    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(HookZlmServerKeepaliveEvent event) {
        if (event.getMediaServerItem() == null
                || !type.equals(event.getMediaServerItem().getType())
                || event.getMediaServerItem().isStatus()) {
            return;
        }
        MediaServerItem serverItem = mediaServerService.getOne(event.getMediaServerItem().getId());
        if (serverItem == null) {
            return;
        }
        logger.info("[ZLM-HOOK事件-心跳] ID：" + event.getMediaServerItem().getId());
        online(serverItem);
    }

    @Scheduled(fixedDelay = 10*1000)   //每隔10秒检查一次
    public void execute(){
        // 初次加入的离线节点会在30分钟内，每间隔十秒尝试一次，30分钟后如果仍然没有上线，则每隔30分钟尝试一次连接
        if (offlineZlmPrimaryMap.isEmpty() && offlineZlmsecondaryMap.isEmpty()) {
            return;
        }
        if (!offlineZlmPrimaryMap.isEmpty()) {
            for (MediaServerItem mediaServerItem : offlineZlmPrimaryMap.values()) {
                if (offlineZlmTimeMap.get(mediaServerItem.getId()) > 30*60*1000) {
                    offlineZlmsecondaryMap.put(mediaServerItem.getId(), mediaServerItem);
                    offlineZlmPrimaryMap.remove(mediaServerItem.getId());
                    continue;
                }
                logger.info("[ZLM-尝试连接] ID：{}, 地址： {}:{}", mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
                JSONObject responseJson = zlmresTfulUtils.getMediaServerConfig(mediaServerItem);
                ZLMServerConfig zlmServerConfig = null;
                if (responseJson == null) {
                    logger.info("[ZLM-尝试连接]失败, ID：{}, 地址： {}:{}", mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
                    continue;
                }
                JSONArray data = responseJson.getJSONArray("data");
                if (data == null || data.isEmpty()) {
                    logger.info("[ZLM-尝试连接]失败, ID：{}, 地址： {}:{}", mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
                }else {
                    zlmServerConfig = JSON.parseObject(JSON.toJSONString(data.get(0)), ZLMServerConfig.class);
                    initPort(mediaServerItem, zlmServerConfig);
                    online(mediaServerItem);
                }
            }
        }
        if (!offlineZlmsecondaryMap.isEmpty()) {
            for (MediaServerItem mediaServerItem : offlineZlmsecondaryMap.values()) {
                if (offlineZlmTimeMap.get(mediaServerItem.getId()) < 30*60*1000) {
                    continue;
                }
                logger.info("[ZLM-尝试连接] ID：{}, 地址： {}:{}", mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
                JSONObject responseJson = zlmresTfulUtils.getMediaServerConfig(mediaServerItem);
                ZLMServerConfig zlmServerConfig = null;
                if (responseJson == null) {
                    logger.info("[ZLM-尝试连接]失败, ID：{}, 地址： {}:{}", mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
                    offlineZlmTimeMap.put(mediaServerItem.getId(), System.currentTimeMillis());
                    continue;
                }
                JSONArray data = responseJson.getJSONArray("data");
                if (data == null || data.isEmpty()) {
                    logger.info("[ZLM-尝试连接]失败, ID：{}, 地址： {}:{}", mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
                    offlineZlmTimeMap.put(mediaServerItem.getId(), System.currentTimeMillis());
                }else {
                    zlmServerConfig = JSON.parseObject(JSON.toJSONString(data.get(0)), ZLMServerConfig.class);
                    initPort(mediaServerItem, zlmServerConfig);
                    online(mediaServerItem);
                }
            }
        }
    }

    private void online(MediaServerItem mediaServerItem) {
        offlineZlmPrimaryMap.remove(mediaServerItem.getId());
        offlineZlmsecondaryMap.remove(mediaServerItem.getId());
        offlineZlmTimeMap.remove(mediaServerItem.getId());
        if (!mediaServerItem.isStatus()) {
            logger.info("[ZLM-连接成功] ID：{}, 地址： {}:{}", mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
            mediaServerItem.setStatus(true);
            mediaServerService.update(mediaServerItem);
        }
        // 设置两次心跳未收到则认为zlm离线
        String key = "zlm-keepalive-" + mediaServerItem.getId();
        dynamicTask.startDelay(key, ()->{
            logger.warn("[ZLM-心跳超时] ID：{}", mediaServerItem.getId());
            mediaServerItem.setStatus(false);
            offlineZlmPrimaryMap.put(mediaServerItem.getId(), mediaServerItem);
            offlineZlmTimeMap.put(mediaServerItem.getId(), System.currentTimeMillis());
        }, (int)(mediaServerItem.getHookAliveInterval() * 2 * 1000));
    }
    private void initPort(MediaServerItem mediaServerItem, ZLMServerConfig zlmServerConfig) {
        if (mediaServerItem.getHttpSSlPort() == 0) {
            mediaServerItem.setHttpSSlPort(zlmServerConfig.getHttpSSLport());
        }
        if (mediaServerItem.getRtmpPort() == 0) {
            mediaServerItem.setRtmpPort(zlmServerConfig.getRtmpPort());
        }
        if (mediaServerItem.getRtmpSSlPort() == 0) {
            mediaServerItem.setRtmpSSlPort(zlmServerConfig.getRtmpSslPort());
        }
        if (mediaServerItem.getRtspPort() == 0) {
            mediaServerItem.setRtspPort(zlmServerConfig.getRtspPort());
        }
        if (mediaServerItem.getRtspSSLPort() == 0) {
            mediaServerItem.setRtspSSLPort(zlmServerConfig.getRtspSSlport());
        }
        if (mediaServerItem.getRtpProxyPort() == 0) {
            mediaServerItem.setRtpProxyPort(zlmServerConfig.getRtpProxyPort());
        }
        mediaServerItem.setHookAliveInterval(zlmServerConfig.getHookAliveInterval());
    }

}
