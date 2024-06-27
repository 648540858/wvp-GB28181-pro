package com.genersoft.iot.vmp.streamProxy.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.genersoft.iot.vmp.common.GeneralCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.hook.Hook;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.hook.HookType;
import com.genersoft.iot.vmp.media.event.media.MediaArrivalEvent;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.media.event.media.MediaNotFoundEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxy;
import com.genersoft.iot.vmp.service.IGbStreamService;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.storager.dao.GbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.PlatformGbStreamMapper;
import com.genersoft.iot.vmp.streamProxy.dao.StreamProxyMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 视频代理业务
 */
@Slf4j
@Service
@DS("master")
public class StreamProxyServiceImpl implements IStreamProxyService {

    @Autowired
    private StreamProxyMapper streamProxyMapper;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private GbStreamMapper gbStreamMapper;

    @Autowired
    private PlatformGbStreamMapper platformGbStreamMapper;

    @Autowired
    private IGbStreamService gbStreamService;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private HookSubscribe hookSubscribe;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    TransactionDefinition transactionDefinition;

    /**
     * 流到来的处理
     */
    @Async("taskExecutor")
    @org.springframework.context.event.EventListener
    public void onApplicationEvent(MediaArrivalEvent event) {
        if ("rtsp".equals(event.getSchema())) {
            updateStatusByAppAndStream(event.getApp(), event.getStream(), true);
        }
    }

    /**
     * 流离开的处理
     */
    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(MediaDepartureEvent event) {
        if ("rtsp".equals(event.getSchema())) {
            updateStatusByAppAndStream(event.getApp(), event.getStream(), false);
        }
    }

    /**
     * 流离开的处理
     */
    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(MediaNotFoundEvent event) {
        if ("rtp".equals(event.getApp())) {
            return;
        }
        // 拉流代理
        StreamProxy streamProxyByAppAndStream = getStreamProxyByAppAndStream(event.getApp(), event.getStream());
        if (streamProxyByAppAndStream != null && streamProxyByAppAndStream.isEnableDisableNoneReader()) {
            start(event.getApp(), event.getStream());
        }
    }


    @Override
    public void save(StreamProxy streamProxy, GeneralCallback<StreamInfo> callback) {
        MediaServer mediaServer;
        if (ObjectUtils.isEmpty(streamProxy.getMediaServerId()) || "auto".equals(streamProxy.getMediaServerId())){
            mediaServer = mediaServerService.getMediaServerForMinimumLoad(null);
        }else {
            mediaServer = mediaServerService.getOne(streamProxy.getMediaServerId());
        }
        if (mediaServer == null) {
            log.warn("保存代理未找到在线的ZLM...");
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "保存代理未找到在线的ZLM");
        }

        streamProxy.setMediaServerId(mediaServer.getId());
        boolean saveResult;
        // 更新
        if (streamProxyMapper.selectOne(streamProxy.getApp(), streamProxy.getStream()) != null) {
            saveResult = updateStreamProxy(streamProxy);
        }else { // 新增
            saveResult = addStreamProxy(streamProxy);
        }
        if (!saveResult) {
            callback.run(ErrorCode.ERROR100.getCode(), "保存失败", null);
            return;
        }

        if (streamProxy.isEnable()) {
            StreamInfo streamInfo = mediaServerService.startProxy(mediaServer, streamProxy);
            if (streamInfo != null) {
                callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
            }else {
                callback.run(ErrorCode.ERROR100.getCode(), "记录已保存，启用失败", null);
            }

//
//
//            Hook hook = Hook.getInstance(HookType.on_media_arrival, streamProxy.getApp(), streamProxy.getStream(), mediaServer.getId());
//            hookSubscribe.addSubscribe(hook, (hookData) -> {
//                StreamInfo streamInfo = mediaServerService.getStreamInfoByAppAndStream(
//                        mediaServer, streamProxy.getApp(), streamProxy.getStream(), null, null);
//                callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
//            });
//            String talkKey = UUID.randomUUID().toString();
//            String delayTalkKey = UUID.randomUUID().toString();
//            dynamicTask.startDelay(delayTalkKey, ()->{
//                StreamInfo streamInfo = mediaServerService.getStreamInfoByAppAndStreamWithCheck(streamProxy.getApp(), streamProxy.getStream(), mediaServer.getId(), false);
//                if (streamInfo != null) {
//                    callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
//                }else {
//                    dynamicTask.stop(talkKey);
//                    callback.run(ErrorCode.ERROR100.getCode(), "超时", null);
//                }
//            }, 7000);
//            WVPResult<String> result = addStreamProxyToZlm(streamProxy);
//            if (result != null && result.getCode() == 0) {
//                hookSubscribe.removeSubscribe(hook);
//                dynamicTask.stop(talkKey);
//                StreamInfo streamInfo = mediaServerService.getStreamInfoByAppAndStream(
//                        mediaServer, streamProxy.getApp(), streamProxy.getStream(), null, null);
//                callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
//            }else {
//                streamProxy.setEnable(false);
//                // 直接移除
//                if (streamProxy.isEnableRemoveNoneReader()) {
//                    del(streamProxy.getApp(), streamProxy.getStream());
//                }else {
//                    updateStreamProxy(streamProxy);
//                }
//                if (result == null){
//                    callback.run(ErrorCode.ERROR100.getCode(), "记录已保存，启用失败", null);
//                }else {
//                    callback.run(ErrorCode.ERROR100.getCode(), result.getMsg(), null);
//                }
//            }
        }else{
            StreamInfo streamInfo = mediaServerService.getStreamInfoByAppAndStream(
                    mediaServer, streamProxy.getApp(), streamProxy.getStream(), null, null);
            callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
        }
    }



    /**
     * 新增代理流
     * @param streamProxyItem
     * @return
     */
    private boolean addStreamProxy(StreamProxy streamProxyItem) {
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        boolean result = false;
        streamProxyItem.setStreamType("proxy");
        streamProxyItem.setStatus(true);
        String now = DateUtil.getNow();
        streamProxyItem.setCreateTime(now);
        try {
            if (streamProxyMapper.add(streamProxyItem) > 0) {
                if (!ObjectUtils.isEmpty(streamProxyItem.getGbId())) {
                    if (gbStreamMapper.add(streamProxyItem) < 0) {
                        //事务回滚
                        dataSourceTransactionManager.rollback(transactionStatus);
                        return false;
                    }
                }
            }else {
                //事务回滚
                dataSourceTransactionManager.rollback(transactionStatus);
                return false;
            }
            result = true;
            dataSourceTransactionManager.commit(transactionStatus);     //手动提交
        }catch (Exception e) {
            log.error("向数据库添加流代理失败：", e);
            dataSourceTransactionManager.rollback(transactionStatus);
        }


        return result;
    }

    /**
     * 更新代理流
     * @param streamProxyItem
     * @return
     */
    @Override
    public boolean updateStreamProxy(StreamProxy streamProxyItem) {
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        boolean result = false;
        streamProxyItem.setStreamType("proxy");
        try {
            if (streamProxyMapper.update(streamProxyItem) > 0) {
                if (!ObjectUtils.isEmpty(streamProxyItem.getGbId())) {
                    if (gbStreamMapper.updateByAppAndStream(streamProxyItem) == 0) {
                        //事务回滚
                        dataSourceTransactionManager.rollback(transactionStatus);
                        return false;
                    }
                }
            } else {
                //事务回滚
                dataSourceTransactionManager.rollback(transactionStatus);
                return false;
            }

            dataSourceTransactionManager.commit(transactionStatus);     //手动提交
            result = true;
        }catch (Exception e) {
            log.error("未处理的异常 ", e);
            dataSourceTransactionManager.rollback(transactionStatus);
        }
        return result;
    }

    @Override
    public WVPResult<String> addStreamProxyToZlm(StreamProxy param) {
        WVPResult<String> result = null;
        MediaServer mediaServer = null;
        if (param.getMediaServerId() == null) {
            log.warn("添加代理时MediaServerId 为null");
            return null;
        }else {
            mediaServer = mediaServerService.getOne(param.getMediaServerId());
        }
        if (mediaServer == null) {
            return null;
        }
        if (mediaServerService.isStreamReady(mediaServer, param.getApp(), param.getStream())) {
            mediaServerService.closeStreams(mediaServer, param.getApp(), param.getStream());
        }
        String msgResult;
        if ("ffmpeg".equalsIgnoreCase(param.getType())){
            if (param.getTimeoutMs() == 0) {
                param.setTimeoutMs(15);
            }
            result = mediaServerService.addFFmpegSource(mediaServer, param.getSrcUrl().trim(), param.getDstUrl(),
                    param.getTimeoutMs(), param.isEnableAudio(), param.isEnableMp4(),
                    param.getFfmpegCmdKey());
        }else {
            result = mediaServerService.addStreamProxy(mediaServer, param.getApp(), param.getStream(), param.getUrl().trim(),
                    param.isEnableAudio(), param.isEnableMp4(), param.getRtpType());
        }
        if (result != null && result.getCode() == 0) {
            String key = result.getData();
            if (key == null) {
                log.warn("[获取拉流代理的结果数据Data] 失败： {}", result );
                return result;
            }
            param.setStreamKey(key);
            streamProxyMapper.update(param);
        }
        return result;
    }

    @Override
    public Boolean removeStreamProxyFromZlm(StreamProxy param) {
        if (param ==null) {
            return null;
        }
        MediaServer mediaServer = mediaServerService.getOne(param.getMediaServerId());
        if (mediaServer == null) {
            return null;
        }
        List<StreamInfo> mediaList = mediaServerService.getMediaList(mediaServer, param.getApp(), param.getStream(), null);
        if (mediaList == null || mediaList.isEmpty()) {
            return true;
        }
        Boolean result = false;
        if ("ffmpeg".equalsIgnoreCase(param.getType())){
            result = mediaServerService.delFFmpegSource(mediaServer, param.getStreamKey());
        }else {
            result = mediaServerService.delStreamProxy(mediaServer, param.getStreamKey());
        }
        return result;
    }

    @Override
    public PageInfo<StreamProxy> getAll(Integer page, Integer count) {
        PageHelper.startPage(page, count);
        List<StreamProxy> all = streamProxyMapper.selectAll();
        return new PageInfo<>(all);
    }

    @Override
    public void del(String app, String stream) {
        StreamProxy streamProxyItem = streamProxyMapper.selectOne(app, stream);
        if (streamProxyItem != null) {
            gbStreamService.sendCatalogMsg(streamProxyItem, CatalogEvent.DEL);

            // 如果关联了国标那么移除关联
            platformGbStreamMapper.delByAppAndStream(app, stream);
            gbStreamMapper.del(app, stream);
            streamProxyMapper.del(app, stream);
            redisCatchStorage.removeStream(streamProxyItem.getMediaServerId(), "PULL", app, stream);
            redisCatchStorage.removeStream(streamProxyItem.getMediaServerId(), "PUSH", app, stream);
            Boolean result = removeStreamProxyFromZlm(streamProxyItem);
            if (result != null && result) {
                log.info("[移除代理]： 代理： {}/{}, 从zlm移除成功", app, stream);
            }else {
                log.info("[移除代理]： 代理： {}/{}, 从zlm移除失败", app, stream);
            }
        }
    }

    @Override
    public boolean start(String app, String stream) {
        boolean result = false;
        StreamProxy streamProxy = streamProxyMapper.selectOne(app, stream);
        if (streamProxy != null && !streamProxy.isEnable() ) {
            WVPResult<String> wvpResult = addStreamProxyToZlm(streamProxy);
            if (wvpResult == null) {
                return false;
            }
            if (wvpResult.getCode() == 0) {
                result = true;
                streamProxy.setEnable(true);
                updateStreamProxy(streamProxy);
            }else {
                log.info("启用代理失败： {}/{}->{}({})", app, stream, wvpResult.getMsg(),
                        streamProxy.getSrcUrl() == null? streamProxy.getUrl():streamProxy.getSrcUrl());
            }
        } else if (streamProxy != null && streamProxy.isEnable()) {
           return true ;
        }
        return result;
    }

    @Override
    public boolean stop(String app, String stream) {
        boolean result = false;
        StreamProxy streamProxyDto = streamProxyMapper.selectOne(app, stream);
        if (streamProxyDto != null && streamProxyDto.isEnable()) {
            Boolean removed = removeStreamProxyFromZlm(streamProxyDto);
            if (removed != null && removed) {
                streamProxyDto.setEnable(false);
                result = updateStreamProxy(streamProxyDto);
            }
        }
        return result;
    }

    @Override
    public Map<String, String> getFFmpegCMDs(MediaServer mediaServer) {
        return mediaServerService.getFFmpegCMDs(mediaServer);
    }


    @Override
    public StreamProxy getStreamProxyByAppAndStream(String app, String streamId) {
        return streamProxyMapper.selectOne(app, streamId);
    }

    @Override
    public void zlmServerOnline(String mediaServerId) {
        // 移除开启了无人观看自动移除的流
        List<StreamProxy> streamProxyItemList = streamProxyMapper.selectAutoRemoveItemByMediaServerId(mediaServerId);
        if (streamProxyItemList.size() > 0) {
            gbStreamMapper.batchDel(streamProxyItemList);
        }
        streamProxyMapper.deleteAutoRemoveItemByMediaServerId(mediaServerId);

        // 移除拉流代理生成的流信息
        syncPullStream(mediaServerId);

        // 恢复流代理, 只查找这个这个流媒体
        List<StreamProxy> streamProxyListForEnable = storager.getStreamProxyListForEnableInMediaServer(
                mediaServerId, true);
        for (StreamProxy streamProxyDto : streamProxyListForEnable) {
            log.info("恢复流代理，" + streamProxyDto.getApp() + "/" + streamProxyDto.getStream());
            WVPResult<String> wvpResult = addStreamProxyToZlm(streamProxyDto);
            if (wvpResult == null) {
                // 设置为离线
                log.info("恢复流代理失败" + streamProxyDto.getApp() + "/" + streamProxyDto.getStream());
                updateStatusByAppAndStream(streamProxyDto.getApp(), streamProxyDto.getStream(), false);
            }else {
                updateStatusByAppAndStream(streamProxyDto.getApp(), streamProxyDto.getStream(), true);
            }
        }
    }

    @Override
    public void zlmServerOffline(String mediaServerId) {
        // 移除开启了无人观看自动移除的流
        List<StreamProxy> streamProxyItemList = streamProxyMapper.selectAutoRemoveItemByMediaServerId(mediaServerId);
        if (streamProxyItemList.size() > 0) {
            gbStreamMapper.batchDel(streamProxyItemList);
        }
        streamProxyMapper.deleteAutoRemoveItemByMediaServerId(mediaServerId);
        // 其他的流设置离线
        streamProxyMapper.updateStatusByMediaServerId(mediaServerId, false);
        String type = "PULL";

        // 发送redis消息
        List<MediaInfo> mediaInfoList = redisCatchStorage.getStreams(mediaServerId, type);
        if (mediaInfoList.size() > 0) {
            for (MediaInfo mediaInfo : mediaInfoList) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("serverId", userSetting.getServerId());
                jsonObject.put("app", mediaInfo.getApp());
                jsonObject.put("stream", mediaInfo.getStream());
                jsonObject.put("register", false);
                jsonObject.put("mediaServerId", mediaServerId);
                redisCatchStorage.sendStreamChangeMsg(type, jsonObject);
                // 移除redis内流的信息
                redisCatchStorage.removeStream(mediaServerId, type, mediaInfo.getApp(), mediaInfo.getStream());
            }
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public int updateStatusByAppAndStream(String app, String stream, boolean status) {
        // 状态变化时推送到国标上级
        StreamProxy streamProxyItem = streamProxyMapper.selectOne(app, stream);
        if (streamProxyItem == null) {
            return 0;
        }
        int result = streamProxyMapper.updateStatus(app, stream, status);
        if (!ObjectUtils.isEmpty(streamProxyItem.getGbId())) {
            gbStreamService.sendCatalogMsg(streamProxyItem, status?CatalogEvent.ON:CatalogEvent.OFF);
        }
        return result;
    }

    private void syncPullStream(String mediaServerId){
        MediaServer mediaServer = mediaServerService.getOne(mediaServerId);
        if (mediaServer != null) {
            List<MediaInfo> mediaInfoList = redisCatchStorage.getStreams(mediaServerId, "PULL");
            if (!mediaInfoList.isEmpty()) {
                List<StreamInfo> mediaList = mediaServerService.getMediaList(mediaServer, null, null, null);
                Map<String, StreamInfo> stringStreamInfoMap = new HashMap<>();
                if (mediaList != null && !mediaList.isEmpty()) {
                    for (StreamInfo streamInfo : mediaList) {
                        stringStreamInfoMap.put(streamInfo.getApp() + streamInfo.getStream(), streamInfo);
                    }
                }
                if (stringStreamInfoMap.isEmpty()) {
                    redisCatchStorage.removeStream(mediaServerId, "PULL");
                }else {
                    for (String key : stringStreamInfoMap.keySet()) {
                        StreamInfo streamInfo = stringStreamInfoMap.get(key);
                        if (stringStreamInfoMap.get(streamInfo.getApp() + streamInfo.getStream()) == null) {
                            redisCatchStorage.removeStream(mediaServerId, "PULL", streamInfo.getApp(),
                                    streamInfo.getStream());
                        }
                    }
                }
            }
        }
    }

    @Override
    public ResourceBaseInfo getOverview() {

        int total = streamProxyMapper.getAllCount();
        int online = streamProxyMapper.getOnline();

        return new ResourceBaseInfo(total, online);
    }


    @Scheduled(cron = "* 0/10 * * * ?")
    public void asyncCheckStreamProxyStatus() {

        List<MediaServer> all = mediaServerService.getAllOnline();

        if (CollectionUtils.isEmpty(all)){
            return;
        }

        Map<String, MediaServer> serverItemMap = all.stream().collect(Collectors.toMap(MediaServer::getId, Function.identity(), (m1, m2) -> m1));

        List<StreamProxy> list = streamProxyMapper.selectForEnable(true);

        if (CollectionUtils.isEmpty(list)){
            return;
        }

        for (StreamProxy streamProxyItem : list) {

            MediaServer mediaServerItem = serverItemMap.get(streamProxyItem.getMediaServerId());

            MediaInfo mediaInfo = mediaServerService.getMediaInfo(mediaServerItem, streamProxyItem.getApp(), streamProxyItem.getStream());

            if (mediaInfo == null){
                streamProxyItem.setStatus(false);
            } else {
                if (mediaInfo.getOnline() != null && mediaInfo.getOnline()) {
                    streamProxyItem.setStatus(true);
                } else {
                    streamProxyItem.setStatus(false);
                }
            }

            updateStreamProxy(streamProxyItem);
        }
    }
}
