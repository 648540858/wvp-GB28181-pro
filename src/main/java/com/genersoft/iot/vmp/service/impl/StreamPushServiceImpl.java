package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.conf.MediaConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.dto.*;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OriginType;
import com.genersoft.iot.vmp.service.ICommonGbChannelService;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.service.bean.CommonGbChannelType;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StreamPushServiceImpl implements IStreamPushService {

    private final static Logger logger = LoggerFactory.getLogger(StreamPushServiceImpl.class);

    @Autowired
    private StreamPushMapper streamPushMapper;

    @Autowired
    private StreamProxyMapper streamProxyMapper;

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
    private ICommonGbChannelService commonGbChannelService;

    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    TransactionDefinition transactionDefinition;

    @Autowired
    private MediaConfig mediaConfig;


    @Override
    public List<StreamPush> handleJSON(String jsonData, MediaServerItem mediaServerItem) {
        if (jsonData == null) {
            return null;
        }

        Map<String, StreamPush> result = new HashMap<>();

        List<OnStreamChangedHookParam> onStreamChangedHookParams = JSON.parseObject(jsonData, new TypeReference<List<OnStreamChangedHookParam>>() {});
        for (OnStreamChangedHookParam item : onStreamChangedHookParams) {

            // 不保存国标推理以及拉流代理的流
            if (item.getOriginType() == OriginType.RTSP_PUSH.ordinal()
                    || item.getOriginType() == OriginType.RTMP_PUSH.ordinal()
                    || item.getOriginType() == OriginType.RTC_PUSH.ordinal() ) {
                String key = item.getApp() + "_" + item.getStream();
                StreamPush streamPushItem = result.get(key);
                if (streamPushItem == null) {
                    streamPushItem = transform(item);
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
        streamPushItem.setAliveSecond(item.getAliveSecond());
        streamPushItem.setTotalReaderCount(item.getTotalReaderCount());
        streamPushItem.setCreateTime(DateUtil.getNow());
        streamPushItem.setAliveSecond(item.getAliveSecond());
        streamPushItem.setVhost(item.getVhost());
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
    public boolean stop(String app, String streamId) {
        logger.info("[停止推流 ] {}/{}", app, streamId);

        StreamPush streamPushItem = streamPushMapper.selectOne(app, streamId);
        if (streamPushItem == null) {
            logger.info("[停止推流] 不存在 {}/{} ", app, streamId);
            return false;
        }
        if (streamPushItem.getCommonGbChannelId() == 0) {
            streamPushMapper.del(app, streamId);
        }
        MediaServerItem mediaServerItem = mediaServerService.getOne(streamPushItem.getMediaServerId());
        zlmresTfulUtils.closeStreams(mediaServerItem,app, streamId);
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
        List<StreamPush> pushList = getPushList(mediaServerId);
        Map<String, StreamPush> pushItemMap = new HashMap<>();
        // redis记录
        List<OnStreamChangedHookParam> onStreamChangedHookParams = redisCatchStorage.getStreams(mediaServerId, "PUSH");
        Map<String, OnStreamChangedHookParam> streamInfoPushItemMap = new HashMap<>();
        if (pushList.size() > 0) {
            for (StreamPush streamPushItem : pushList) {
                if (streamPushItem.getCommonGbChannelId() > 0) {
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
            List<StreamPush> streamPushItems = null;
            if (code == 0 ) {
                if (dataStr != null) {
                    streamPushItems = handleJSON(dataStr, mediaServerItem);
                }
            }

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
        List<StreamPush> streamPushItems = streamPushMapper.selectAllByMediaServerIdWithOutGbID(mediaServerId);
        // 移除没有GBId的推流
        streamPushMapper.deleteWithoutGBId(mediaServerId);
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
    @Transactional
    public void batchAdd(List<StreamPush> streamPushItems) {
        // 把存在国标Id的写入同步资源库

        streamPushMapper.addAll(streamPushItems);


    }

    @Override
    public void batchUpdate(List<StreamPush> streamPushItemForUpdate) {

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
    @Transactional
    public boolean add(StreamPush stream, CommonGbChannel commonGbChannel) {
        assert !ObjectUtils.isEmpty(commonGbChannel.getCommonGbDeviceID());
        assert !ObjectUtils.isEmpty(commonGbChannel.getCommonGbName());
        String now = DateUtil.getNow();
        commonGbChannel.setCreateTime(now);
        commonGbChannel.setUpdateTime(now);
        commonGbChannel.setType(CommonGbChannelType.PUSH);

        commonGbChannelService.add(commonGbChannel);
        if (commonGbChannel.getCommonGbId() > 0) {
            stream.setCommonGbChannelId(commonGbChannel.getCommonGbId());
        }else {
            return false;
        }
        stream.setUpdateTime(now);
        stream.setCreateTime(now);
        stream.setServerId(userSetting.getServerId());
        return streamPushMapper.add(stream) > 1;
    }

    @Override
    public Map<String, StreamPush> getAllAppAndStream() {
        return streamPushMapper.getAllAppAndStream();
    }

    @Override
    public ResourceBaseInfo getOverview() {
        int total = streamPushMapper.getAllCount();
        int online = streamPushMapper.getAllOnline(userSetting.isUsePushingAsStatus());

        return new ResourceBaseInfo(total, online);
    }
}
