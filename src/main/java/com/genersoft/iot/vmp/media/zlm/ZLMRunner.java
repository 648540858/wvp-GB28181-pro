package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.MediaConfig;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeFactory;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForServerStarted;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForStreamChange;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@Order(value=1)
public class ZLMRunner implements CommandLineRunner {

    private final static Logger logger = LoggerFactory.getLogger(ZLMRunner.class);

    private Map<String, Boolean> startGetMedia;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private ZLMHttpHookSubscribe hookSubscribe;

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
//        Instant expiresInstant = Instant.now().plusSeconds(TimeUnit.SECONDS.toSeconds(60));
//        hookSubscribeForStreamChange.setExpires(expiresInstant);
        // 订阅 zlm启动事件, 新的zlm也会从这里进入系统
        hookSubscribe.addSubscribe(hookSubscribeForServerStarted,
                (MediaServerItem mediaServerItem, JSONObject response)->{
            ZLMServerConfig zlmServerConfig = JSONObject.toJavaObject(response, ZLMServerConfig.class);
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
        mediaServerService.updateVmServer(all);
        if (all.size() == 0) {
            all.add(mediaConfig.getMediaSerItem());
        }
        for (MediaServerItem mediaServerItem : all) {
            if (startGetMedia == null) {
                startGetMedia = new HashMap<>();
            }
            startGetMedia.put(mediaServerItem.getId(), true);
            connectZlmServer(mediaServerItem);
        }
        String taskKey = "zlm-connect-timeout";
        dynamicTask.startDelay(taskKey, ()->{
            if (startGetMedia != null) {
                Set<String> allZlmId = startGetMedia.keySet();
                for (String id : allZlmId) {
                    logger.error("[ {} ]]主动连接失败，不再尝试连接", id);
                }
                startGetMedia = null;
            }
        //  TODO 清理数据库中与redis不匹配的zlm
        }, 60 * 1000 );
    }

    @Async
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
