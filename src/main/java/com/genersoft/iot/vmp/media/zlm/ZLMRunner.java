package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.MediaConfig;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeFactory;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForServerStarted;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(value=12)
public class ZLMRunner implements CommandLineRunner {

    private final static Logger logger = LoggerFactory.getLogger(ZLMRunner.class);

    private Map<String, Boolean> startGetMedia;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private ZlmHttpHookSubscribe hookSubscribe;

    @Autowired
    private EventPublisher publisher;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private MediaConfig mediaConfig;

    @Autowired
    private DynamicTask dynamicTask;


    @Override
    public void run(String... strings) throws Exception {
        mediaServerService.clearMediaServerForOnline();
        MediaServerItem defaultMediaServer = mediaServerService.getDefaultMediaServer();
        if (defaultMediaServer == null) {
            mediaServerService.addToDatabase(mediaConfig.getMediaSerItem());
        }else {
            MediaServerItem mediaSerItem = mediaConfig.getMediaSerItem();
            mediaServerService.updateToDatabase(mediaSerItem);
        }
        mediaServerService.syncCatchFromDatabase();
        HookSubscribeForServerStarted hookSubscribeForServerStarted = HookSubscribeFactory.on_server_started();
        // 订阅 zlm启动事件, 新的zlm也会从这里进入系统
        hookSubscribe.addSubscribe(hookSubscribeForServerStarted,
                (mediaServerItem, hookParam)->{
            ZLMServerConfig zlmServerConfig = (ZLMServerConfig)hookParam;
            if (zlmServerConfig !=null ) {
                if (startGetMedia != null) {
                    startGetMedia.remove(zlmServerConfig.getGeneralMediaServerId());
                    if (startGetMedia.size() == 0) {
                        hookSubscribe.removeSubscribe(HookSubscribeFactory.on_server_started());
                    }
                }
            }
        });

        // 获取zlm信息
        logger.info("[zlm] 等待默认zlm中...");

        // 获取所有的zlm， 并开启主动连接
        List<MediaServerItem> all = mediaServerService.getAllFromDatabase();
        Map<String, MediaServerItem> allMap = new HashMap<>();
        mediaServerService.updateVmServer(all);
        if (all.size() == 0) {
            all.add(mediaConfig.getMediaSerItem());
        }
        for (MediaServerItem mediaServerItem : all) {
            if (startGetMedia == null) {
                startGetMedia = new ConcurrentHashMap<>();
            }
            startGetMedia.put(mediaServerItem.getId(), true);
            connectZlmServer(mediaServerItem);
            allMap.put(mediaServerItem.getId(), mediaServerItem);
        }
        String taskKey = "zlm-connect-timeout";
        dynamicTask.startDelay(taskKey, ()->{
            if (startGetMedia != null && startGetMedia.size() > 0) {
                Set<String> allZlmId = startGetMedia.keySet();
                for (String id : allZlmId) {
                    logger.error("[ {} ]]主动连接失败，不再尝试连接", id);
                }
                startGetMedia = null;
            }
            // 获取redis中所有的zlm
            List<MediaServerItem> allInRedis = mediaServerService.getAll();
            for (MediaServerItem mediaServerItem : allInRedis) {
                if (!allMap.containsKey(mediaServerItem.getId())) {
                    mediaServerService.delete(mediaServerItem.getId());
                }
            }
        }, 60 * 1000 );
    }

    @Async("taskExecutor")
    public void connectZlmServer(MediaServerItem mediaServerItem){
        String connectZlmServerTaskKey = "connect-zlm-" + mediaServerItem.getId();
        ZLMServerConfig zlmServerConfigFirst = getMediaServerConfig(mediaServerItem);
        if (zlmServerConfigFirst != null) {
            zlmServerConfigFirst.setIp(mediaServerItem.getIp());
            zlmServerConfigFirst.setHttpPort(mediaServerItem.getHttpPort());
            startGetMedia.remove(mediaServerItem.getId());
            if (startGetMedia.size() == 0) {
                hookSubscribe.removeSubscribe(HookSubscribeFactory.on_server_started());
            }
            mediaServerService.zlmServerOnline(zlmServerConfigFirst);
        }else {
            logger.info("[ {} ]-[ {}:{} ]主动连接失败, 清理相关资源， 开始尝试重试连接",
                    mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
            publisher.zlmOfflineEventPublish(mediaServerItem.getId());
        }

        dynamicTask.startCron(connectZlmServerTaskKey, ()->{
            ZLMServerConfig zlmServerConfig = getMediaServerConfig(mediaServerItem);
            if (zlmServerConfig != null) {
                dynamicTask.stop(connectZlmServerTaskKey);
                zlmServerConfig.setIp(mediaServerItem.getIp());
                zlmServerConfig.setHttpPort(mediaServerItem.getHttpPort());
                startGetMedia.remove(mediaServerItem.getId());
                if (startGetMedia.size() == 0) {
                    hookSubscribe.removeSubscribe(HookSubscribeFactory.on_server_started());
                }
                mediaServerService.zlmServerOnline(zlmServerConfig);
            }
        }, 2000);
    }

    public ZLMServerConfig getMediaServerConfig(MediaServerItem mediaServerItem) {
        if (startGetMedia == null) { return null;}
        if (!mediaServerItem.isDefaultServer() && mediaServerService.getOne(mediaServerItem.getId()) == null) {
            return null;
        }
        if ( startGetMedia.get(mediaServerItem.getId()) == null || !startGetMedia.get(mediaServerItem.getId())) {
            return null;
        }
        JSONObject responseJson = zlmresTfulUtils.getMediaServerConfig(mediaServerItem);
        ZLMServerConfig zlmServerConfig = null;
        if (responseJson != null) {
            JSONArray data = responseJson.getJSONArray("data");
            if (data != null && data.size() > 0) {
                zlmServerConfig = JSON.parseObject(JSON.toJSONString(data.get(0)), ZLMServerConfig.class);
            }
        } else {
            logger.error("[ {} ]-[ {}:{} ]主动连接失败, 2s后重试",
                    mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
        }
        return zlmServerConfig;

    }
}
