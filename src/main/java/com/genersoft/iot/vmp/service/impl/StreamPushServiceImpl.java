package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.MediaConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.PlatformCatalog;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.event.media.MediaArrivalEvent;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPush;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OriginType;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.service.bean.StreamPushItemFromRedis;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.*;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.genersoft.iot.vmp.vmanager.bean.StreamContent;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@DS("master")
public class StreamPushServiceImpl implements IStreamPushService {

    private final static Logger logger = LoggerFactory.getLogger(StreamPushServiceImpl.class);

    @Autowired
    private StreamPushMapper streamPushMapper;

    @Autowired
    private StreamProxyMapper streamProxyMapper;

    @Autowired
    private ParentPlatformMapper parentPlatformMapper;

    @Autowired
    private PlatformCatalogMapper platformCatalogMapper;

    @Autowired
    private PlatformGbStreamMapper platformGbStreamMapper;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    TransactionDefinition transactionDefinition;

    @Autowired
    private MediaConfig mediaConfig;

    /**
     * 流到来的处理
     */
    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(MediaArrivalEvent event) {
        MediaInfo mediaInfo = event.getMediaInfo();
        if (mediaInfo == null) {
            return;
        }
        if (mediaInfo.getOriginType() != OriginType.RTMP_PUSH.ordinal()
                && mediaInfo.getOriginType() != OriginType.RTSP_PUSH.ordinal()
                && mediaInfo.getOriginType() != OriginType.RTC_PUSH.ordinal()) {
            return;
        }

        StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(event.getApp(), event.getStream());
        if (streamAuthorityInfo == null) {
            streamAuthorityInfo = StreamAuthorityInfo.getInstanceByHook(event);
        } else {
            streamAuthorityInfo.setOriginType(mediaInfo.getOriginType());
        }
        redisCatchStorage.updateStreamAuthorityInfo(event.getApp(), event.getStream(), streamAuthorityInfo);
        StreamPush transform = StreamPush.getInstance(event, userSetting.getServerId());
        transform.setPushIng(true);
        transform.setUpdateTime(DateUtil.getNow());
        transform.setPushTime(DateUtil.getNow());
        transform.setSelf(true);
        StreamPush pushInDb = getPush(event.getApp(), event.getStream());
        if (pushInDb == null) {
            transform.setCreateTime(DateUtil.getNow());
            add(transform);
        }else {
            update(transform);
        }
        // 冗余数据，自己系统中自用
        if (!"broadcast".equals(event.getApp()) && !"talk".equals(event.getApp())) {
            StreamInfo streamInfo = mediaServerService.getStreamInfoByAppAndStream(
                    event.getMediaServer(), event.getApp(), event.getStream(), event.getMediaInfo(), event.getCallId());
            event.getHookParam().setStreamInfo(new StreamContent(streamInfo));
            redisCatchStorage.addPushListItem(event.getApp(), event.getStream(), event);
        }

        // 发送流变化redis消息
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("serverId", userSetting.getServerId());
        jsonObject.put("app", event.getApp());
        jsonObject.put("stream", event.getStream());
        jsonObject.put("register", true);
        jsonObject.put("mediaServerId", event.getMediaServer().getId());
        redisCatchStorage.sendStreamChangeMsg(OriginType.values()[event.getMediaInfo().getOriginType()].getType(), jsonObject);
    }

    /**
     * 流离开的处理
     */
    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(MediaDepartureEvent event) {
        // 兼容流注销时类型从redis记录获取
        MediaInfo mediaInfo = redisCatchStorage.getStreamInfo(
                event.getApp(), event.getStream(), event.getMediaServer().getId());
        if (mediaInfo != null) {
            String type = OriginType.values()[mediaInfo.getOriginType()].getType();
            redisCatchStorage.removeStream(event.getMediaServer().getId(), type, event.getApp(), event.getStream());
            if ("PUSH".equalsIgnoreCase(type)) {
                // 冗余数据，自己系统中自用
                redisCatchStorage.removePushListItem(event.getApp(), event.getStream(), event.getMediaServer().getId());
            }
            if (type != null) {
                // 发送流变化redis消息
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("serverId", userSetting.getServerId());
                jsonObject.put("app", event.getApp());
                jsonObject.put("stream", event.getStream());
                jsonObject.put("register", false);
                jsonObject.put("mediaServerId", event.getMediaServer().getId());
                redisCatchStorage.sendStreamChangeMsg(type, jsonObject);
            }
        }
        StreamPush push = getPush(event.getApp(), event.getStream());
        push.setPushIng(false);
        if (push.getGbDeviceId() != null) {
            if (userSetting.isUsePushingAsStatus()) {
                push.setGbStatus(false);
                updateStatus(push);
//                streamPushMapper.updatePushStatus(event.getApp(), event.getStream(), false);
//                eventPublisher.catalogEventPublishForStream(null, gbStream, CatalogEvent.OFF);
            }
        }else {
            deleteByAppAndStream(event.getApp(), event.getStream());
        }
    }


    private List<StreamPush> handleJSON(List<StreamInfo> streamInfoList) {
        if (streamInfoList == null || streamInfoList.isEmpty()) {
            return null;
        }
        Map<String, StreamPush> result = new HashMap<>();
        for (StreamInfo streamInfo : streamInfoList) {
            // 不保存国标推理以及拉流代理的流
            if (streamInfo.getOriginType() == OriginType.RTSP_PUSH.ordinal()
                    || streamInfo.getOriginType() == OriginType.RTMP_PUSH.ordinal()
                    || streamInfo.getOriginType() == OriginType.RTC_PUSH.ordinal() ) {
                String key = streamInfo.getApp() + "_" + streamInfo.getStream();
                StreamPush streamPushItem = result.get(key);
                if (streamPushItem == null) {
                    streamPushItem = streamPushItem.getInstance(streamInfo);
                    result.put(key, streamPushItem);
                }
            }
        }
        return new ArrayList<>(result.values());
    }

    @Override
    public StreamPush transform(OnStreamChangedHookParam item) {
        StreamPush streamPushItem = new StreamPush();
        streamPushItem.setApp(item.getApp());
        streamPushItem.setMediaServerId(item.getMediaServerId());
        streamPushItem.setStream(item.getStream());
        streamPushItem.setCreateTime(DateUtil.getNow());
        streamPushItem.setServerId(item.getSeverId());
        return streamPushItem;
    }

    @Override
    public PageInfo<StreamPush> getPushList(Integer page, Integer count, String query, Boolean pushing, String mediaServerId) {
        PageHelper.startPage(page, count);
        List<StreamPush> all = streamPushMapper.selectAllForList(query, pushing, mediaServerId);
        return new PageInfo<>(all);
    }

    @Override
    public List<StreamPush> getPushList(String mediaServerId) {
        return streamPushMapper.selectAllByMediaServerIdWithOutGbID(mediaServerId);
    }


    @Override
    public StreamPush getPush(String app, String streamId) {
        return streamPushMapper.selectOne(app, streamId);
    }

    @Override
    public boolean stop(String app, String stream) {
        logger.info("[推流] 停止流： {}/{}", app, stream);
        StreamPush streamPushItem = streamPushMapper.selectOne(app, stream);
        if (streamPushItem != null) {
            gbStreamService.sendCatalogMsg(streamPushItem, CatalogEvent.DEL);
        }

        platformGbStreamMapper.delByAppAndStream(app, stream);
        gbStreamMapper.del(app, stream);
        int delStream = streamPushMapper.del(app, stream);
        if (delStream > 0) {
            MediaServer mediaServerItem = mediaServerService.getOne(streamPushItem.getMediaServerId());
            mediaServerService.closeStreams(mediaServerItem,app, stream);
        }
        return true;
    }

    @Override
    public void zlmServerOnline(String mediaServerId) {
        // 同步zlm推流信息
        MediaServer mediaServerItem = mediaServerService.getOne(mediaServerId);
        if (mediaServerItem == null) {
            return;
        }
        // 数据库记录
        List<StreamPush> pushList = getPushList(mediaServerId);
        Map<String, StreamPush> pushItemMap = new HashMap<>();
        // redis记录
        List<MediaInfo> mediaInfoList = redisCatchStorage.getStreams(mediaServerId, "PUSH");
        Map<String, MediaInfo> streamInfoPushItemMap = new HashMap<>();
        if (pushList.size() > 0) {
            for (StreamPush streamPushItem : pushList) {
                if (ObjectUtils.isEmpty(streamPushItem.getGbId())) {
                    pushItemMap.put(streamPushItem.getApp() + streamPushItem.getStream(), streamPushItem);
                }
            }
        }
        if (mediaInfoList.size() > 0) {
            for (MediaInfo mediaInfo : mediaInfoList) {
                streamInfoPushItemMap.put(mediaInfo.getApp() + mediaInfo.getStream(), mediaInfo);
            }
        }
        // 获取所有推流鉴权信息，清理过期的
        List<StreamAuthorityInfo> allStreamAuthorityInfo = redisCatchStorage.getAllStreamAuthorityInfo();
        Map<String, StreamAuthorityInfo> streamAuthorityInfoInfoMap = new HashMap<>();
        for (StreamAuthorityInfo streamAuthorityInfo : allStreamAuthorityInfo) {
            streamAuthorityInfoInfoMap.put(streamAuthorityInfo.getApp() + streamAuthorityInfo.getStream(), streamAuthorityInfo);
        }
        List<StreamInfo> mediaList = mediaServerService.getMediaList(mediaServerItem, null, null, null);
        if (mediaList == null) {
            return;
        }
        List<StreamPush> streamPushItems = handleJSON(mediaList);
        if (streamPushItems != null) {
            for (StreamPush streamPushItem : streamPushItems) {
                pushItemMap.remove(streamPushItem.getApp() + streamPushItem.getStream());
                streamInfoPushItemMap.remove(streamPushItem.getApp() + streamPushItem.getStream());
                streamAuthorityInfoInfoMap.remove(streamPushItem.getApp() + streamPushItem.getStream());
            }
        }
        List<StreamPush> offlinePushItems = new ArrayList<>(pushItemMap.values());
        if (offlinePushItems.size() > 0) {
            String type = "PUSH";
            int runLimit = 300;
            if (offlinePushItems.size() > runLimit) {
                for (int i = 0; i < offlinePushItems.size(); i += runLimit) {
                    int toIndex = i + runLimit;
                    if (i + runLimit > offlinePushItems.size()) {
                        toIndex = offlinePushItems.size();
                    }
                    List<StreamPush> streamPushItemsSub = offlinePushItems.subList(i, toIndex);
                    streamPushMapper.delAll(streamPushItemsSub);
                }
            }else {
                streamPushMapper.delAll(offlinePushItems);
            }

        }
        Collection<MediaInfo> mediaInfos = streamInfoPushItemMap.values();
        if (mediaInfos.size() > 0) {
            String type = "PUSH";
            for (MediaInfo mediaInfo : mediaInfos) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("serverId", userSetting.getServerId());
                jsonObject.put("app", mediaInfo.getApp());
                jsonObject.put("stream", mediaInfo.getStream());
                jsonObject.put("register", false);
                jsonObject.put("mediaServerId", mediaServerId);
                redisCatchStorage.sendStreamChangeMsg(type, jsonObject);
                // 移除redis内流的信息
                redisCatchStorage.removeStream(mediaServerItem.getId(), "PUSH", mediaInfo.getApp(), mediaInfo.getStream());
                // 冗余数据，自己系统中自用
                redisCatchStorage.removePushListItem(mediaInfo.getApp(), mediaInfo.getStream(), mediaServerItem.getId());
            }
        }

        Collection<StreamAuthorityInfo> streamAuthorityInfos = streamAuthorityInfoInfoMap.values();
        if (streamAuthorityInfos.size() > 0) {
            for (StreamAuthorityInfo streamAuthorityInfo : streamAuthorityInfos) {
                // 移除redis内流的信息
                redisCatchStorage.removeStreamAuthorityInfo(streamAuthorityInfo.getApp(), streamAuthorityInfo.getStream());
            }
        }
    }

    @Override
    public void zlmServerOffline(String mediaServerId) {
        List<StreamPush> streamPushItems = streamPushMapper.selectAllByMediaServerIdWithOutGbID(mediaServerId);
        // 移除没有GBId的推流
        streamPushMapper.deleteWithoutGBId(mediaServerId);
        gbStreamMapper.deleteWithoutGBId("push", mediaServerId);
        // 其他的流设置未启用
        streamPushMapper.updateStatusByMediaServerId(mediaServerId, false);
        streamProxyMapper.updateStatusByMediaServerId(mediaServerId, false);
        // 发送流停止消息
        String type = "PUSH";
        // 发送redis消息
        List<MediaInfo> mediaInfoList = redisCatchStorage.getStreams(mediaServerId, type);
        if (mediaInfoList.size() > 0) {
            for (MediaInfo mediaInfo : mediaInfoList) {
                // 移除redis内流的信息
                redisCatchStorage.removeStream(mediaServerId, type, mediaInfo.getApp(), mediaInfo.getStream());
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("serverId", userSetting.getServerId());
                jsonObject.put("app", mediaInfo.getApp());
                jsonObject.put("stream", mediaInfo.getStream());
                jsonObject.put("register", false);
                jsonObject.put("mediaServerId", mediaServerId);
                redisCatchStorage.sendStreamChangeMsg(type, jsonObject);

                // 冗余数据，自己系统中自用
                redisCatchStorage.removePushListItem(mediaInfo.getApp(), mediaInfo.getStream(), mediaServerId);
            }
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public boolean saveToRandomGB() {
        List<StreamPush> streamPushItems = streamPushMapper.selectAll();
        long gbId = 100001;
        for (StreamPush streamPushItem : streamPushItems) {
            streamPushItem.setStreamType("push");
            streamPushItem.setStatus(true);
            streamPushItem.setGbId("34020000004111" + gbId);
            streamPushItem.setCreateTime(DateUtil.getNow());
            gbId ++;
        }
        int  limitCount = 30;

        if (streamPushItems.size() > limitCount) {
            for (int i = 0; i < streamPushItems.size(); i += limitCount) {
                int toIndex = i + limitCount;
                if (i + limitCount > streamPushItems.size()) {
                    toIndex = streamPushItems.size();
                }
                gbStreamMapper.batchAdd(streamPushItems.subList(i, toIndex));
            }
        }else {
            gbStreamMapper.batchAdd(streamPushItems);
        }
        return true;
    }

    @Override
    public void batchAdd(List<StreamPush> streamPushItems) {
        streamPushMapper.addAll(streamPushItems);
        gbStreamMapper.batchAdd(streamPushItems);
    }


    @Override
    public void batchAddForUpload(List<StreamPush> streamPushItems, Map<String, List<String[]>> streamPushItemsForAll ) {
        // 存储数据到stream_push表
        streamPushMapper.addAll(streamPushItems);
        List<StreamPush> streamPushItemForGbStream = streamPushItems.stream()
                .filter(streamPushItem-> streamPushItem.getGbId() != null)
                .collect(Collectors.toList());
        // 存储数据到gb_stream表， id会返回到streamPushItemForGbStream里
        if (streamPushItemForGbStream.size() > 0) {
            gbStreamMapper.batchAdd(streamPushItemForGbStream);
        }
        // 去除没有ID也就是没有存储到数据库的数据
        List<StreamPush> streamPushItemsForPlatform = streamPushItemForGbStream.stream()
                .filter(streamPushItem-> streamPushItem.getGbStreamId() != null)
                .collect(Collectors.toList());

        if (streamPushItemsForPlatform.size() > 0) {
            // 获取所有平台，平台和目录信息一般不会特别大量。
            List<ParentPlatform> parentPlatformList = parentPlatformMapper.getParentPlatformList();
            Map<String, Map<String, PlatformCatalog>> platformInfoMap = new HashMap<>();
            if (parentPlatformList.size() == 0) {
                return;
            }
            for (ParentPlatform platform : parentPlatformList) {
                Map<String, PlatformCatalog> catalogMap = new HashMap<>();

                // 创建根节点
                PlatformCatalog platformCatalog = new PlatformCatalog();
                platformCatalog.setId(platform.getServerGBId());
                catalogMap.put(platform.getServerGBId(), platformCatalog);

                // 查询所有节点信息
                List<PlatformCatalog> platformCatalogs = platformCatalogMapper.selectByPlatForm(platform.getServerGBId());
                if (platformCatalogs.size() > 0) {
                    for (PlatformCatalog catalog : platformCatalogs) {
                        catalogMap.put(catalog.getId(), catalog);
                    }
                }
                platformInfoMap.put(platform.getServerGBId(), catalogMap);
            }
            List<StreamPush> streamPushItemListFroPlatform = new ArrayList<>();
            Map<String, List<GbStream>> platformForEvent = new HashMap<>();
            // 遍历存储结果，查找app+Stream->platformId+catalogId的对应关系，然后执行批量写入
            for (StreamPush streamPushItem : streamPushItemsForPlatform) {
                List<String[]> platFormInfoList = streamPushItemsForAll.get(streamPushItem.getApp() + streamPushItem.getStream());
                if (platFormInfoList != null && platFormInfoList.size() > 0) {
                    for (String[] platFormInfoArray : platFormInfoList) {
                        StreamPush streamPushItemForPlatform = new StreamPush();
                        streamPushItemForPlatform.setGbStreamId(streamPushItem.getGbStreamId());
                        if (platFormInfoArray.length > 0) {
                            // 数组 platFormInfoArray 0 为平台ID。 1为目录ID
                            // 不存在这个平台，则忽略导入此关联关系
                            if (platformInfoMap.get(platFormInfoArray[0]) == null
                                    || platformInfoMap.get(platFormInfoArray[0]).get(platFormInfoArray[1]) == null) {
                                logger.info("导入数据时不存在平台或目录{}/{},已导入未分配", platFormInfoArray[0], platFormInfoArray[1] );
                                continue;
                            }
                            streamPushItemForPlatform.setPlatformId(platFormInfoArray[0]);
                            List<GbStream> gbStreamList = platformForEvent.get(platFormInfoArray[0]);
                            if (gbStreamList == null) {
                                gbStreamList = new ArrayList<>();
                                platformForEvent.put(platFormInfoArray[0], gbStreamList);
                            }
                            // 为发送通知整理数据
                            streamPushItemForPlatform.setName(streamPushItem.getName());
                            streamPushItemForPlatform.setApp(streamPushItem.getApp());
                            streamPushItemForPlatform.setStream(streamPushItem.getStream());
                            streamPushItemForPlatform.setGbId(streamPushItem.getGbId());
                            gbStreamList.add(streamPushItemForPlatform);
                        }
                        if (platFormInfoArray.length > 1) {
                            streamPushItemForPlatform.setCatalogId(platFormInfoArray[1]);
                        }
                        streamPushItemListFroPlatform.add(streamPushItemForPlatform);
                    }

                }
            }
            if (!streamPushItemListFroPlatform.isEmpty()) {
                platformGbStreamMapper.batchAdd(streamPushItemListFroPlatform);
                // 发送通知
                for (String platformId : platformForEvent.keySet()) {
                    eventPublisher.catalogEventPublishForStream(
                            platformId, platformForEvent.get(platformId), CatalogEvent.ADD);
                }
            }
        }
    }

    @Override
    public boolean batchStop(List<GbStream> gbStreams) {
        if (gbStreams == null || gbStreams.size() == 0) {
            return false;
        }
        gbStreamService.sendCatalogMsgs(gbStreams, CatalogEvent.DEL);

        platformGbStreamMapper.delByGbStreams(gbStreams);
        gbStreamMapper.batchDelForGbStream(gbStreams);
        int delStream = streamPushMapper.delAllForGbStream(gbStreams);
        if (delStream > 0) {
            for (GbStream gbStream : gbStreams) {
                MediaServer mediaServerItem = mediaServerService.getOne(gbStream.getMediaServerId());
                mediaServerService.closeStreams(mediaServerItem, gbStream.getApp(), gbStream.getStream());
            }

        }
        return true;
    }

    @Override
    public void allStreamOffline() {
        List<GbStream> onlinePushers = streamPushMapper.getOnlinePusherForGb();
        if (onlinePushers.size() == 0) {
            return;
        }
        streamPushMapper.setAllStreamOffline();

        // 发送通知
        eventPublisher.catalogEventPublishForStream(null, onlinePushers, CatalogEvent.OFF);
    }

    @Override
    public void offline(List<StreamPushItemFromRedis> offlineStreams) {
        // 更新部分设备离线
        List<GbStream> onlinePushers = streamPushMapper.getOnlinePusherForGbInList(offlineStreams);
        streamPushMapper.offline(offlineStreams);
        // 发送通知
        eventPublisher.catalogEventPublishForStream(null, onlinePushers, CatalogEvent.OFF);
    }

    @Override
    public void online(List<StreamPushItemFromRedis> onlineStreams) {
        // 更新部分设备上线streamPushService
        List<GbStream> onlinePushers = streamPushMapper.getOfflinePusherForGbInList(onlineStreams);
        streamPushMapper.online(onlineStreams);
        // 发送通知
        eventPublisher.catalogEventPublishForStream(null, onlinePushers, CatalogEvent.ON);
    }

    @Override
    public boolean add(StreamPush stream) {
        stream.setUpdateTime(DateUtil.getNow());
        stream.setCreateTime(DateUtil.getNow());
        stream.setServerId(userSetting.getServerId());
        stream.setMediaServerId(mediaConfig.getId());
        stream.setSelf(true);
        stream.setPushIng(true);

        // 放在事务内执行
        boolean result = false;
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        try {
            int addStreamResult = streamPushMapper.add(stream);
            if (!ObjectUtils.isEmpty(stream.getGbId())) {
                stream.setStreamType("push");
                gbStreamMapper.add(stream);
            }
            dataSourceTransactionManager.commit(transactionStatus);
            result = true;
        }catch (Exception e) {
            logger.error("批量移除流与平台的关系时错误", e);
            dataSourceTransactionManager.rollback(transactionStatus);
        }
        return result;
    }

    @Override
    public boolean update(StreamPush stream) {

    }

    @Override
    public List<String> getAllAppAndStream() {

        return streamPushMapper.getAllAppAndStream();
    }

    @Override
    public ResourceBaseInfo getOverview() {
        int total = streamPushMapper.getAllCount();
        int online = streamPushMapper.getAllOnline(userSetting.isUsePushingAsStatus());

        return new ResourceBaseInfo(total, online);
    }

    @Override
    public Map<String, StreamPush> getAllAppAndStreamMap() {
        return streamPushMapper.getAllAppAndStreamMap();
    }

    @Override
    public Map<String, StreamPush> getAllGBId() {
        return streamPushMapper.getAllGBId();
    }

    @Override
    public void updatePush(OnStreamChangedHookParam param) {
        StreamPush transform = transform(param);
        StreamPush pushInDb = getPush(param.getApp(), param.getStream());
        transform.setPushIng(param.isRegist());
        transform.setUpdateTime(DateUtil.getNow());
        transform.setPushTime(DateUtil.getNow());
        transform.setSelf(userSetting.getServerId().equals(param.getSeverId()));
        if (pushInDb == null) {
            transform.setCreateTime(DateUtil.getNow());
            streamPushMapper.add(transform);
        }else {
            streamPushMapper.update(transform);
            gbStreamMapper.updateMediaServer(param.getApp(), param.getStream(), param.getMediaServerId());
        }
    }

    @Override
    public void updateStatus(StreamPush push) {

    }

    @Override
    public void deleteByAppAndStream(String app, String stream) {

    }


}
