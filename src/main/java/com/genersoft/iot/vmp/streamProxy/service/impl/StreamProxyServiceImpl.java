package com.genersoft.iot.vmp.streamProxy.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.media.MediaArrivalEvent;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.media.event.media.MediaNotFoundEvent;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerOfflineEvent;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerOnlineEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OriginType;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.gb28181.dao.PlatformGbStreamMapper;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxy;
import com.genersoft.iot.vmp.streamProxy.dao.StreamProxyMapper;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyService;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @Transactional
    public void del(String app, String stream) {
        StreamProxy streamProxy = streamProxyMapper.selectOne(app, stream);
        if (streamProxy == null) {
            return;
        }
        if (streamProxy.getStreamKey() != null) {
            MediaServer mediaServer = mediaServerService.getOne(streamProxy.getMediaServerId());
            if (mediaServer != null) {
                mediaServerService.stopProxy(mediaServer, streamProxy.getStreamKey());
            }
        }
        if (streamProxy.getGbId() > 0) {
            gbChannelService.delete(streamProxy.getGbId());
        }
        streamProxyMapper.delete(streamProxy.getId());
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
    @Transactional
    public void zlmServerOnline(String mediaServerId) {
        MediaServer mediaServer = mediaServerService.getOne(mediaServerId);
        if (mediaServer == null) {
            return;
        }
        // 这里主要是控制数据库/redis缓存/以及zlm中存在的代理流 三者状态一致。以数据库中数据为根本
        redisCatchStorage.removeStream(mediaServerId, "pull");

        List<StreamProxy> streamProxies = streamProxyMapper.selectForEnableInMediaServer(mediaServerId, true);
        if (streamProxies.isEmpty()){
            return;
        }
        Map<String, StreamProxy> streamProxyMapForDb = new HashMap<>();
        for (StreamProxy streamProxy : streamProxies) {
            streamProxyMapForDb.put(streamProxy.getApp() + "_" + streamProxy.getStream(), streamProxy);
        }

        List<StreamInfo> streamInfoList = mediaServerService.getMediaList(mediaServer, null, null, null);

        List<CommonGBChannel> channelListForOnline = new ArrayList<>();
        for (StreamInfo streamInfo : streamInfoList) {
            String key = streamInfo.getApp() + streamInfo.getStream();
            StreamProxy streamProxy = streamProxyMapForDb.get(key);
            if (streamProxy == null) {
                // 流媒体存在，数据库中不存在
                continue;
            }
            if (streamInfo.getOriginType() == OriginType.PULL.ordinal()
                    || streamInfo.getOriginType() == OriginType.FFMPEG_PULL.ordinal() ) {
                if (streamProxyMapForDb.get(key) != null) {
                    redisCatchStorage.addStream(mediaServer, "pull", streamInfo.getApp(), streamInfo.getStream(), streamInfo.getMediaInfo());
                    if (streamProxy.getGbStatus() == 1 && streamProxy.getGbId() > 0) {
                        streamProxy.setGbStatus(1);
                        channelListForOnline.add(streamProxy.getCommonGBChannel());
                    }
                    streamProxyMapForDb.remove(key);
                }
            }
        }

        if (!channelListForOnline.isEmpty()) {
            gbChannelService.online(channelListForOnline);
        }
        List<CommonGBChannel> channelListForOffline = new ArrayList<>();
        List<StreamProxy> streamProxiesForRemove = new ArrayList<>();
        if (!streamProxyMapForDb.isEmpty()) {
            for (StreamProxy streamProxy : streamProxyMapForDb.values()) {
                if (streamProxy.getGbStatus() == 0 && streamProxy.getGbId() > 0) {
                    streamProxy.setGbStatus(0);
                    channelListForOffline.add(streamProxy.getCommonGBChannel());
                }
                // 移除开启了无人观看自动移除的流
                if (streamProxy.getGbDeviceId() == null && streamProxy.isEnableRemoveNoneReader()) {
                    streamProxiesForRemove.add(streamProxy);
                    streamProxyMapForDb.remove(streamProxy.getApp() + streamProxy.getStream());
                }
            }
        }
        if (!channelListForOffline.isEmpty()) {
            gbChannelService.offline(channelListForOffline);
        }
        if (!streamProxiesForRemove.isEmpty()) {
            streamProxyMapper.deleteByList(streamProxiesForRemove);
        }


        if (!streamProxyMapForDb.isEmpty()) {
            for (StreamProxy streamProxy : streamProxyMapForDb.values()) {
                log.info("恢复流代理，" + streamProxy.getApp() + "/" + streamProxy.getStream());
                mediaServerService.startProxy(mediaServer, streamProxy);
            }
        }
    }

    @Override
    public void zlmServerOffline(String mediaServerId) {
        List<StreamProxy> streamProxies = streamProxyMapper.selectForEnableInMediaServer(mediaServerId, true);

        // 清理redis相关的缓存
        redisCatchStorage.removeStream(mediaServerId, "pull");

        if (streamProxies.isEmpty()){
            return;
        }
        List<StreamProxy> streamProxiesForRemove = new ArrayList<>();
        List<StreamProxy> streamProxiesForSendMessage = new ArrayList<>();
        List<CommonGBChannel> channelListForOffline = new ArrayList<>();

        for (StreamProxy streamProxy : streamProxies) {
            if (streamProxy.getGbId() > 0 && streamProxy.getGbStatus() == 1) {
                channelListForOffline.add(streamProxy.getCommonGBChannel());
            }
            if (streamProxy.getGbId() == 0 && streamProxy.isEnableRemoveNoneReader()) {
                streamProxiesForRemove.add(streamProxy);
            }
            if (streamProxy.getGbStatus() == 1) {
                streamProxiesForSendMessage.add(streamProxy);
            }
        }
        // 移除开启了无人观看自动移除的流
        streamProxyMapper.deleteByList(streamProxiesForRemove);
        // 修改国标关联的国标通道的状态
        gbChannelService.offline(channelListForOffline);

        if (!streamProxiesForSendMessage.isEmpty()) {
            for (StreamProxy streamProxy : streamProxiesForSendMessage) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("serverId", userSetting.getServerId());
                jsonObject.put("app", streamProxy.getApp());
                jsonObject.put("stream", streamProxy.getStream());
                jsonObject.put("register", false);
                jsonObject.put("mediaServerId", mediaServerId);
                redisCatchStorage.sendStreamChangeMsg("pull", jsonObject);
            }
        }
    }

    @Override
    @Transactional
    public int updateStatusByAppAndStream(String app, String stream, boolean status) {
        // 状态变化时推送到国标上级
        StreamProxy streamProxyItem = streamProxyMapper.selectOne(app, stream);
        if (streamProxyItem == null) {
            return 0;
        }
        streamProxyItem.setGbStatus(status?1:0);
        if (streamProxyItem.getGbId() > 0) {
            if (status) {
                gbChannelService.online(streamProxyItem.getCommonGBChannel());
            }else {
                gbChannelService.offline(streamProxyItem.getCommonGBChannel());
            }

        }
        return 1;
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
