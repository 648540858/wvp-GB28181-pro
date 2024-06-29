package com.genersoft.iot.vmp.streamProxy.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.genersoft.iot.vmp.common.GeneralCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.hook.Hook;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.hook.HookType;
import com.genersoft.iot.vmp.media.event.media.MediaArrivalEvent;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.media.event.media.MediaNotFoundEvent;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerOfflineEvent;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerOnlineEvent;
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
import org.springframework.transaction.annotation.Transactional;
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
    private IGbChannelService gbChannelService;

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
     * 流未找到的处理
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

    /**
     * 流媒体节点上线
     */
    @Async("taskExecutor")
    @EventListener
    @Transactional
    public void onApplicationEvent(MediaServerOnlineEvent event) {
        zlmServerOnline(event.getMediaServerId());
    }

    /**
     * 流媒体节点离线
     */
    @Async("taskExecutor")
    @EventListener
    @Transactional
    public void onApplicationEvent(MediaServerOfflineEvent event) {
        zlmServerOffline(event.getMediaServerId());
    }


    @Override
    @Transactional
    public StreamInfo save(StreamProxy streamProxy) {
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
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "保存失败");
        }

        if (streamProxy.isEnable()) {
            return mediaServerService.startProxy(mediaServer, streamProxy);
        }
        return null;
    }



    /**
     * 新增代理流
     */
    @Transactional
    public boolean addStreamProxy(StreamProxy streamProxy) {
        String now = DateUtil.getNow();
        streamProxy.setCreateTime(now);
        streamProxy.setUpdateTime(now);

        if (streamProxyMapper.add(streamProxy) > 0 && !ObjectUtils.isEmpty(streamProxy.getGbDeviceId())) {
            gbChannelService.add(streamProxy.getCommonGBChannel());
        }
        return true;
    }

    /**
     * 更新代理流
     */
    @Override
    public boolean updateStreamProxy(StreamProxy streamProxy) {
        streamProxy.setUpdateTime(DateUtil.getNow());

        if (streamProxyMapper.update(streamProxy) > 0 && !ObjectUtils.isEmpty(streamProxy.getGbDeviceId())) {
            if (streamProxy.getGbId() > 0) {
                gbChannelService.update(streamProxy.getCommonGBChannel());
            }else {
                gbChannelService.add(streamProxy.getCommonGBChannel());
            }
        }
        return true;
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
        StreamProxy streamProxy = streamProxyMapper.selectOne(app, stream);
        if (streamProxy == null) {
            throw new ControllerException(ErrorCode.ERROR404.getCode(), "代理信息未找到");
        }
        MediaServer mediaServer;
        if (ObjectUtils.isEmpty(streamProxy.getMediaServerId()) || "auto".equals(streamProxy.getMediaServerId())){
            mediaServer = mediaServerService.getMediaServerForMinimumLoad(null);
        }else {
            mediaServer = mediaServerService.getOne(streamProxy.getMediaServerId());
        }
        if (mediaServer == null) {
            log.warn("[启用代理] 未找到可用的媒体节点");
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到可用的媒体节点");
        }
        StreamInfo streamInfo = mediaServerService.startProxy(mediaServer, streamProxy);
        if (streamInfo == null) {
            log.warn("[启用代理] 失败");
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "失败");
        }
        if (!streamProxy.isEnable()) {
            updateStreamProxy(streamProxy);
        }
        return true;
    }

    @Override
    public void stop(String app, String stream) {
        StreamProxy streamProxy = streamProxyMapper.selectOne(app, stream);
        if (streamProxy == null) {
            throw new ControllerException(ErrorCode.ERROR404.getCode(), "代理信息未找到");
        }
        MediaServer mediaServer = mediaServerService.getOne(streamProxy.getMediaServerId());
        if (mediaServer == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到启用时使用的媒体节点");
        }
        mediaServerService.stopProxy(mediaServer, streamProxy.getStreamKey());
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
        List<StreamProxy> streamProxyItemList = streamProxyMapper.selectWithAutoRemoveAndWithoutGbDeviceIdByMediaServerId(mediaServerId);
        streamProxyMapper.deleteAutoRemoveItemByMediaServerId(mediaServerId);

        // 移除拉流代理生成的流信息
        syncPullStream(mediaServerId);

        // 恢复流代理, 只查找这个这个流媒体
        List<StreamProxy> streamProxyListForEnable = storager.getStreamProxyListForEnableInMediaServer(
                mediaServerId, true);
        for (StreamProxy streamProxyDto : streamProxyListForEnable) {
            log.info("恢复流代理，" + streamProxyDto.getApp() + "/" + streamProxyDto.getStream());
            mediaServerService.startProxy(me)
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


//    @Scheduled(cron = "* 0/10 * * * ?")
//    public void asyncCheckStreamProxyStatus() {
//
//        List<MediaServer> all = mediaServerService.getAllOnline();
//
//        if (CollectionUtils.isEmpty(all)){
//            return;
//        }
//
//        Map<String, MediaServer> serverItemMap = all.stream().collect(Collectors.toMap(MediaServer::getId, Function.identity(), (m1, m2) -> m1));
//
//        List<StreamProxy> list = streamProxyMapper.selectForEnable(true);
//
//        if (CollectionUtils.isEmpty(list)){
//            return;
//        }
//
//        for (StreamProxy streamProxyItem : list) {
//
//            MediaServer mediaServerItem = serverItemMap.get(streamProxyItem.getMediaServerId());
//
//            MediaInfo mediaInfo = mediaServerService.getMediaInfo(mediaServerItem, streamProxyItem.getApp(), streamProxyItem.getStream());
//
//            if (mediaInfo == null){
//                streamProxyItem.setStatus(false);
//            } else {
//                if (mediaInfo.getOnline() != null && mediaInfo.getOnline()) {
//                    streamProxyItem.setStatus(true);
//                } else {
//                    streamProxyItem.setStatus(false);
//                }
//            }
//
//            updateStreamProxy(streamProxyItem);
//        }
//    }
}
