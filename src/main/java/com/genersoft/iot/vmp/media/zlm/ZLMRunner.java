package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.conf.MediaConfig;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IStreamProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

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
    private IStreamProxyService streamProxyService;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private MediaConfig mediaConfig;

    @Override
    public void run(String... strings) throws Exception {
        // 清楚redis缓存的在线zlm信息
        mediaServerService.clearMediaServerForOnline();

        // 将配置文件的meida配置写入数据库
//        MediaServerItem presetMediaServer = mediaServerService.getOneByHostAndPort(
//                mediaConfig.getIp(), mediaConfig.getHttpPort());
//        if (presetMediaServer  != null) {
//            MediaServerItem mediaSerItem = mediaConfig.getMediaSerItem();
//            mediaSerItem.setId(presetMediaServer.getId());
//            mediaServerService.update(mediaSerItem);
//        }else {
//            if (mediaConfig.getId() != null) {
//                MediaServerItem mediaSerItem = mediaConfig.getMediaSerItem();
//                mediaServerService.add(mediaSerItem);
//            }
//        }

        // 订阅 zlm启动事件, 新的zlm也会从这里进入系统
        hookSubscribe.addSubscribe(ZLMHttpHookSubscribe.HookType.on_server_started,null,
                (MediaServerItem mediaServerItem, JSONObject response)->{
            ZLMServerConfig zlmServerConfig = JSONObject.toJavaObject(response, ZLMServerConfig.class);
            if (zlmServerConfig !=null ) {
                startGetMedia.remove(zlmServerConfig.getGeneralMediaServerId());
                mediaServerService.handLeZLMServerConfig(zlmServerConfig);
//                zLmRunning(zlmServerConfig);
            }
        });

        // 获取zlm信息
        logger.info("等待默认zlm接入...");

        // 获取所有的zlm， 并开启主动连接
        List<MediaServerItem> all = mediaServerService.getAllFromDatabase();
        if (all.size() == 0) {
            all.add(mediaConfig.getMediaSerItem());
        }
        for (MediaServerItem mediaServerItem : all) {
            if (startGetMedia == null) startGetMedia = new HashMap<>();
            startGetMedia.put(mediaServerItem.getId(), true);
            new Thread(() -> {

                ZLMServerConfig zlmServerConfig = getMediaServerConfig(mediaServerItem);
                if (zlmServerConfig != null) {
                    zlmServerConfig.setIp(mediaServerItem.getIp());
                    zlmServerConfig.setHttpPort(mediaServerItem.getHttpPort());
                    startGetMedia.remove(mediaServerItem.getId());
                    mediaServerService.handLeZLMServerConfig(zlmServerConfig);
                }

            }).start();
        }
        Timer timer = new Timer();
        // 2分钟后未连接到则不再去主动连接, TODO 并对重启前使用此在zlm的通道发送bye
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
            if (startGetMedia != null) {
                Set<String> allZlmId = startGetMedia.keySet();
                for (String id : allZlmId) {
                    logger.error("[ {} ]]主动连接失败，不再主动连接", id);
                }
                startGetMedia = null;
            }
            //  TODO 清理数据库中与redis不匹配的zlm
            }
        }, 60 * 1000 * 2);
    }

    public ZLMServerConfig getMediaServerConfig(MediaServerItem mediaServerItem) {
        if (startGetMedia == null) { return null;}
        if (!mediaServerItem.isDefaultServer() && mediaServerService.getOne(mediaServerItem.getId()) == null) {
            return null;
        }
        if ( startGetMedia.get(mediaServerItem.getId()) == null || !startGetMedia.get(mediaServerItem.getId())) {
            return null;
        }
        JSONObject responseJSON = zlmresTfulUtils.getMediaServerConfig(mediaServerItem);
        ZLMServerConfig ZLMServerConfig = null;
        if (responseJSON != null) {
            JSONArray data = responseJSON.getJSONArray("data");
            if (data != null && data.size() > 0) {
                ZLMServerConfig = JSON.parseObject(JSON.toJSONString(data.get(0)), ZLMServerConfig.class);
                ZLMServerConfig.setIp(mediaServerItem.getIp());
            }
        } else {
            logger.error("[ {} ]-[ {}:{} ]主动连接失败失败, 2s后重试",
                    mediaServerItem.getId(), mediaServerItem.getIp(), mediaServerItem.getHttpPort());
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ZLMServerConfig = getMediaServerConfig(mediaServerItem);
        }
        return ZLMServerConfig;

    }

    /**
     * zlm 连接成功或者zlm重启后
     */
//    private void zLmRunning(ZLMServerConfig zlmServerConfig){
//        logger.info( "[ id: " + zlmServerConfig.getGeneralMediaServerId() + "] zlm接入成功...");
//        // 关闭循环获取zlm配置
//        startGetMedia = false;
//        MediaServerItem mediaServerItem = new MediaServerItem(zlmServerConfig, sipIp);
//        storager.updateMediaServer(mediaServerItem);
//
//        if (mediaServerItem.isAutoConfig()) setZLMConfig(mediaServerItem);
//        zlmServerManger.updateServerCatchFromHook(zlmServerConfig);
//
//        // 清空所有session
////        zlmMediaListManager.clearAllSessions();
//
//        // 更新流列表
//        zlmMediaListManager.updateMediaList(mediaServerItem);
//        // 恢复流代理, 只查找这个这个流媒体
//        List<StreamProxyItem> streamProxyListForEnable = storager.getStreamProxyListForEnableInMediaServer(
//                mediaServerItem.getId(), true);
//        for (StreamProxyItem streamProxyDto : streamProxyListForEnable) {
//            logger.info("恢复流代理，" + streamProxyDto.getApp() + "/" + streamProxyDto.getStream());
//            JSONObject jsonObject = streamProxyService.addStreamProxyToZlm(streamProxyDto);
//            if (jsonObject == null) {
//                // 设置为未启用
//                logger.info("恢复流代理失败，请检查流地址后重新启用" + streamProxyDto.getApp() + "/" + streamProxyDto.getStream());
//                streamProxyService.stop(streamProxyDto.getApp(), streamProxyDto.getStream());
//            }
//        }
//    }
}
