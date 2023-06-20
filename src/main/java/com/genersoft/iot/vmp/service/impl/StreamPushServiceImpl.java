package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.genersoft.iot.vmp.conf.MediaConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.dto.*;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OriginType;
import com.genersoft.iot.vmp.service.IGbStreamService;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.service.bean.StreamPushItemFromRedis;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.*;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StreamPushServiceImpl implements IStreamPushService {

    private final static Logger logger = LoggerFactory.getLogger(StreamPushServiceImpl.class);

    @Autowired
    private GbStreamMapper gbStreamMapper;

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
    private IGbStreamService gbStreamService;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

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


    @Override
    public List<StreamPushItem> handleJSON(String jsonData, MediaServerItem mediaServerItem) {
        if (jsonData == null) {
            return null;
        }

        Map<String, StreamPushItem> result = new HashMap<>();

        List<OnStreamChangedHookParam> onStreamChangedHookParams = JSON.parseObject(jsonData, new TypeReference<List<OnStreamChangedHookParam>>() {});
        for (OnStreamChangedHookParam item : onStreamChangedHookParams) {

            // 不保存国标推理以及拉流代理的流
            if (item.getOriginType() == OriginType.RTSP_PUSH.ordinal()
                    || item.getOriginType() == OriginType.RTMP_PUSH.ordinal()
                    || item.getOriginType() == OriginType.RTC_PUSH.ordinal() ) {
                String key = item.getApp() + "_" + item.getStream();
                StreamPushItem streamPushItem = result.get(key);
                if (streamPushItem == null) {
                    streamPushItem = transform(item);
                    result.put(key, streamPushItem);
                }
            }
        }

        return new ArrayList<>(result.values());
    }
    @Override
    public StreamPushItem transform(OnStreamChangedHookParam item) {
        StreamPushItem streamPushItem = new StreamPushItem();
        streamPushItem.setApp(item.getApp());
        streamPushItem.setMediaServerId(item.getMediaServerId());
        streamPushItem.setStream(item.getStream());
        streamPushItem.setAliveSecond(item.getAliveSecond());
        streamPushItem.setOriginSock(item.getOriginSock());
        streamPushItem.setTotalReaderCount(item.getTotalReaderCount());
        streamPushItem.setOriginType(item.getOriginType());
        streamPushItem.setOriginTypeStr(item.getOriginTypeStr());
        streamPushItem.setOriginUrl(item.getOriginUrl());
        streamPushItem.setCreateTime(DateUtil.getNow());
        streamPushItem.setAliveSecond(item.getAliveSecond());
        streamPushItem.setStatus(true);
        streamPushItem.setStreamType("push");
        streamPushItem.setVhost(item.getVhost());
        streamPushItem.setServerId(item.getSeverId());
        return streamPushItem;
    }

    @Override
    public PageInfo<StreamPushItem> getPushList(Integer page, Integer count, String query, Boolean pushing, String mediaServerId) {
        PageHelper.startPage(page, count);
        List<StreamPushItem> all = streamPushMapper.selectAllForList(query, pushing, mediaServerId);
        return new PageInfo<>(all);
    }

    @Override
    public List<StreamPushItem> getPushList(String mediaServerId) {
        return streamPushMapper.selectAllByMediaServerIdWithOutGbID(mediaServerId);
    }

    @Override
    public boolean saveToGB(GbStream stream) {
        stream.setStreamType("push");
        stream.setStatus(true);
        stream.setCreateTime(DateUtil.getNow());
        stream.setStreamType("push");
        stream.setMediaServerId(mediaConfig.getId());
        int add = gbStreamMapper.add(stream);
        return add > 0;
    }

    @Override
    public boolean removeFromGB(GbStream stream) {
        // 判断是否需要发送事件
        gbStreamService.sendCatalogMsg(stream, CatalogEvent.DEL);
        platformGbStreamMapper.delByAppAndStream(stream.getApp(), stream.getStream());
        int del = gbStreamMapper.del(stream.getApp(), stream.getStream());
        MediaServerItem mediaInfo = mediaServerService.getOne(stream.getMediaServerId());
        JSONObject mediaList = zlmresTfulUtils.getMediaList(mediaInfo, stream.getApp(), stream.getStream());
        if (mediaList != null) {
            if (mediaList.getInteger("code") == 0) {
                JSONArray data = mediaList.getJSONArray("data");
                if (data == null) {
                    streamPushMapper.del(stream.getApp(), stream.getStream());
                }
            }
        }
        return del > 0;
    }


    @Override
    public StreamPushItem getPush(String app, String streamId) {
        return streamPushMapper.selectOne(app, streamId);
    }

    @Override
    public boolean stop(String app, String streamId) {
        logger.info("[推流 ] 停止流： {}/{}", app, streamId);
        StreamPushItem streamPushItem = streamPushMapper.selectOne(app, streamId);
        if (streamPushItem != null) {
            gbStreamService.sendCatalogMsg(streamPushItem, CatalogEvent.DEL);
        }

        platformGbStreamMapper.delByAppAndStream(app, streamId);
        gbStreamMapper.del(app, streamId);
        int delStream = streamPushMapper.del(app, streamId);
        if (delStream > 0) {
            MediaServerItem mediaServerItem = mediaServerService.getOne(streamPushItem.getMediaServerId());
            zlmresTfulUtils.closeStreams(mediaServerItem,app, streamId);
        }
        return true;
    }

    @Override
    public void zlmServerOnline(String mediaServerId) {
        // 同步zlm推流信息
        MediaServerItem mediaServerItem = mediaServerService.getOne(mediaServerId);
        if (mediaServerItem == null) {
            return;
        }
        // 数据库记录
        List<StreamPushItem> pushList = getPushList(mediaServerId);
        Map<String, StreamPushItem> pushItemMap = new HashMap<>();
        // redis记录
        List<OnStreamChangedHookParam> onStreamChangedHookParams = redisCatchStorage.getStreams(mediaServerId, "PUSH");
        Map<String, OnStreamChangedHookParam> streamInfoPushItemMap = new HashMap<>();
        if (pushList.size() > 0) {
            for (StreamPushItem streamPushItem : pushList) {
                if (ObjectUtils.isEmpty(streamPushItem.getGbId())) {
                    pushItemMap.put(streamPushItem.getApp() + streamPushItem.getStream(), streamPushItem);
                }
            }
        }
        if (onStreamChangedHookParams.size() > 0) {
            for (OnStreamChangedHookParam onStreamChangedHookParam : onStreamChangedHookParams) {
                streamInfoPushItemMap.put(onStreamChangedHookParam.getApp() + onStreamChangedHookParam.getStream(), onStreamChangedHookParam);
            }
        }
        // 获取所有推流鉴权信息，清理过期的
        List<StreamAuthorityInfo> allStreamAuthorityInfo = redisCatchStorage.getAllStreamAuthorityInfo();
        Map<String, StreamAuthorityInfo> streamAuthorityInfoInfoMap = new HashMap<>();
        for (StreamAuthorityInfo streamAuthorityInfo : allStreamAuthorityInfo) {
            streamAuthorityInfoInfoMap.put(streamAuthorityInfo.getApp() + streamAuthorityInfo.getStream(), streamAuthorityInfo);
        }
        zlmresTfulUtils.getMediaList(mediaServerItem, (mediaList ->{
            if (mediaList == null) {
                return;
            }
            String dataStr = mediaList.getString("data");

            Integer code = mediaList.getInteger("code");
            List<StreamPushItem> streamPushItems = null;
            if (code == 0 ) {
                if (dataStr != null) {
                    streamPushItems = handleJSON(dataStr, mediaServerItem);
                }
            }

            if (streamPushItems != null) {
                for (StreamPushItem streamPushItem : streamPushItems) {
                    pushItemMap.remove(streamPushItem.getApp() + streamPushItem.getStream());
                    streamInfoPushItemMap.remove(streamPushItem.getApp() + streamPushItem.getStream());
                    streamAuthorityInfoInfoMap.remove(streamPushItem.getApp() + streamPushItem.getStream());
                }
            }
            List<StreamPushItem> offlinePushItems = new ArrayList<>(pushItemMap.values());
            if (offlinePushItems.size() > 0) {
                String type = "PUSH";
                int runLimit = 300;
                if (offlinePushItems.size() > runLimit) {
                    for (int i = 0; i < offlinePushItems.size(); i += runLimit) {
                        int toIndex = i + runLimit;
                        if (i + runLimit > offlinePushItems.size()) {
                            toIndex = offlinePushItems.size();
                        }
                        List<StreamPushItem> streamPushItemsSub = offlinePushItems.subList(i, toIndex);
                        streamPushMapper.delAll(streamPushItemsSub);
                    }
                }else {
                    streamPushMapper.delAll(offlinePushItems);
                }

            }
            Collection<OnStreamChangedHookParam> offlineOnStreamChangedHookParamList = streamInfoPushItemMap.values();
            if (offlineOnStreamChangedHookParamList.size() > 0) {
                String type = "PUSH";
                for (OnStreamChangedHookParam offlineOnStreamChangedHookParam : offlineOnStreamChangedHookParamList) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("serverId", userSetting.getServerId());
                    jsonObject.put("app", offlineOnStreamChangedHookParam.getApp());
                    jsonObject.put("stream", offlineOnStreamChangedHookParam.getStream());
                    jsonObject.put("register", false);
                    jsonObject.put("mediaServerId", mediaServerId);
                    redisCatchStorage.sendStreamChangeMsg(type, jsonObject);
                    // 移除redis内流的信息
                    redisCatchStorage.removeStream(mediaServerItem.getId(), "PUSH", offlineOnStreamChangedHookParam.getApp(), offlineOnStreamChangedHookParam.getStream());
                }
            }

            Collection<StreamAuthorityInfo> streamAuthorityInfos = streamAuthorityInfoInfoMap.values();
            if (streamAuthorityInfos.size() > 0) {
                for (StreamAuthorityInfo streamAuthorityInfo : streamAuthorityInfos) {
                    // 移除redis内流的信息
                    redisCatchStorage.removeStreamAuthorityInfo(streamAuthorityInfo.getApp(), streamAuthorityInfo.getStream());
                }
            }
        }));
    }

    @Override
    public void zlmServerOffline(String mediaServerId) {
        List<StreamPushItem> streamPushItems = streamPushMapper.selectAllByMediaServerIdWithOutGbID(mediaServerId);
        // 移除没有GBId的推流
        streamPushMapper.deleteWithoutGBId(mediaServerId);
        gbStreamMapper.deleteWithoutGBId("push", mediaServerId);
        // 其他的流设置未启用
        streamPushMapper.updateStatusByMediaServerId(mediaServerId, false);
        streamProxyMapper.updateStatusByMediaServerId(mediaServerId, false);
        // 发送流停止消息
        String type = "PUSH";
        // 发送redis消息
        List<OnStreamChangedHookParam> streamInfoList = redisCatchStorage.getStreams(mediaServerId, type);
        if (streamInfoList.size() > 0) {
            for (OnStreamChangedHookParam onStreamChangedHookParam : streamInfoList) {
                // 移除redis内流的信息
                redisCatchStorage.removeStream(mediaServerId, type, onStreamChangedHookParam.getApp(), onStreamChangedHookParam.getStream());
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("serverId", userSetting.getServerId());
                jsonObject.put("app", onStreamChangedHookParam.getApp());
                jsonObject.put("stream", onStreamChangedHookParam.getStream());
                jsonObject.put("register", false);
                jsonObject.put("mediaServerId", mediaServerId);
                redisCatchStorage.sendStreamChangeMsg(type, jsonObject);
            }
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public boolean saveToRandomGB() {
        List<StreamPushItem> streamPushItems = streamPushMapper.selectAll();
        long gbId = 100001;
        for (StreamPushItem streamPushItem : streamPushItems) {
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
    public void batchAdd(List<StreamPushItem> streamPushItems) {
        streamPushMapper.addAll(streamPushItems);
        gbStreamMapper.batchAdd(streamPushItems);
    }


    @Override
    public void batchAddForUpload(List<StreamPushItem> streamPushItems, Map<String, List<String[]>> streamPushItemsForAll ) {
        // 存储数据到stream_push表
        streamPushMapper.addAll(streamPushItems);
        List<StreamPushItem> streamPushItemForGbStream = streamPushItems.stream()
                .filter(streamPushItem-> streamPushItem.getGbId() != null)
                .collect(Collectors.toList());
        // 存储数据到gb_stream表， id会返回到streamPushItemForGbStream里
        if (streamPushItemForGbStream.size() > 0) {
            gbStreamMapper.batchAdd(streamPushItemForGbStream);
        }
        // 去除没有ID也就是没有存储到数据库的数据
        List<StreamPushItem> streamPushItemsForPlatform = streamPushItemForGbStream.stream()
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
            List<StreamPushItem> streamPushItemListFroPlatform = new ArrayList<>();
            Map<String, List<GbStream>> platformForEvent = new HashMap<>();
            // 遍历存储结果，查找app+Stream->platformId+catalogId的对应关系，然后执行批量写入
            for (StreamPushItem streamPushItem : streamPushItemsForPlatform) {
                List<String[]> platFormInfoList = streamPushItemsForAll.get(streamPushItem.getApp() + streamPushItem.getStream());
                if (platFormInfoList != null && platFormInfoList.size() > 0) {
                    for (String[] platFormInfoArray : platFormInfoList) {
                        StreamPushItem streamPushItemForPlatform = new StreamPushItem();
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
            if (streamPushItemListFroPlatform.size() > 0) {
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
                MediaServerItem mediaServerItem = mediaServerService.getOne(gbStream.getMediaServerId());
                zlmresTfulUtils.closeStreams(mediaServerItem, gbStream.getApp(), gbStream.getStream());
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
    public boolean add(StreamPushItem stream) {
        stream.setUpdateTime(DateUtil.getNow());
        stream.setCreateTime(DateUtil.getNow());
        stream.setServerId(userSetting.getServerId());

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
    public List<String> getAllAppAndStream() {

        return streamPushMapper.getAllAppAndStream();
    }

    @Override
    public ResourceBaseInfo getOverview() {
        int total = streamPushMapper.getAllCount();
        int online = streamPushMapper.getAllOnline(userSetting.isUsePushingAsStatus());

        return new ResourceBaseInfo(total, online);
    }
}
