package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetup;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.PlatformCatalog;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.media.zlm.ZLMHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.ZLMServerConfig;
import com.genersoft.iot.vmp.media.zlm.dto.*;
import com.genersoft.iot.vmp.service.IGbStreamService;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.*;
import com.genersoft.iot.vmp.vmanager.bean.StreamPushExcelDto;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StreamPushServiceImpl implements IStreamPushService {

    @Autowired
    private GbStreamMapper gbStreamMapper;

    @Autowired
    private StreamPushMapper streamPushMapper;

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
    private UserSetup userSetup;

    @Autowired
    private IMediaServerService mediaServerService;

    @Override
    public List<StreamPushItem> handleJSON(String jsonData, MediaServerItem mediaServerItem) {
        if (jsonData == null) return null;

        Map<String, StreamPushItem> result = new HashMap<>();

        List<MediaItem> mediaItems = JSON.parseObject(jsonData, new TypeReference<List<MediaItem>>() {});
        for (MediaItem item : mediaItems) {

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
    public StreamPushItem transform(MediaItem item) {
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
        streamPushItem.setCreateStamp(item.getCreateStamp() * 1000);
        streamPushItem.setAliveSecond(item.getAliveSecond());
        streamPushItem.setStatus(true);
        streamPushItem.setStreamType("push");
        streamPushItem.setVhost(item.getVhost());
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
        stream.setCreateStamp(System.currentTimeMillis());
        int add = gbStreamMapper.add(stream);

        // 查找开启了全部直播流共享的上级平台
        List<ParentPlatform> parentPlatforms = parentPlatformMapper.selectAllAhareAllLiveStream();
        if (parentPlatforms.size() > 0) {
            for (ParentPlatform parentPlatform : parentPlatforms) {
                stream.setCatalogId(parentPlatform.getCatalogId());
                stream.setPlatformId(parentPlatform.getServerGBId());
                String streamId = stream.getStream();
                StreamProxyItem streamProxyItem = platformGbStreamMapper.selectOne(stream.getApp(), streamId, parentPlatform.getServerGBId());
                if (streamProxyItem == null) {
                    platformGbStreamMapper.add(stream);
                    eventPublisher.catalogEventPublishForStream(parentPlatform.getServerGBId(), stream, CatalogEvent.ADD);
                }else {
                    if (!streamProxyItem.getGbId().equals(stream.getGbId())) {
                        // 此流使用另一个国标Id已经与该平台关联，移除此记录
                        platformGbStreamMapper.delByAppAndStreamAndPlatform(stream.getApp(), streamId, parentPlatform.getServerGBId());
                        platformGbStreamMapper.add(stream);
                        eventPublisher.catalogEventPublishForStream(parentPlatform.getServerGBId(), stream, CatalogEvent.ADD);
                    }
                }
            }
        }

        return add > 0;
    }

    @Override
    public boolean removeFromGB(GbStream stream) {
        // 判断是否需要发送事件
        gbStreamService.sendCatalogMsg(stream, CatalogEvent.DEL);
        int del = gbStreamMapper.del(stream.getApp(), stream.getStream());
        platformGbStreamMapper.delByAppAndStream(stream.getApp(), stream.getStream());
        MediaServerItem mediaInfo = mediaServerService.getOne(stream.getMediaServerId());
        JSONObject mediaList = zlmresTfulUtils.getMediaList(mediaInfo, stream.getApp(), stream.getStream());
        if (mediaList == null) {
            streamPushMapper.del(stream.getApp(), stream.getStream());
        }
        return del > 0;
    }


    @Override
    public StreamPushItem getPush(String app, String streamId) {

        return streamPushMapper.selectOne(app, streamId);
    }

    @Override
    public boolean stop(String app, String streamId) {
        StreamPushItem streamPushItem = streamPushMapper.selectOne(app, streamId);
        gbStreamService.sendCatalogMsg(streamPushItem, CatalogEvent.DEL);

        int delStream = streamPushMapper.del(app, streamId);
        gbStreamMapper.del(app, streamId);
        platformGbStreamMapper.delByAppAndStream(app, streamId);
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
        List<MediaItem> mediaItems = redisCatchStorage.getStreams(mediaServerId, "PUSH");
        Map<String, MediaItem> streamInfoPushItemMap = new HashMap<>();
        if (pushList.size() > 0) {
            for (StreamPushItem streamPushItem : pushList) {
                if (StringUtils.isEmpty(streamPushItem.getGbId())) {
                    pushItemMap.put(streamPushItem.getApp() + streamPushItem.getStream(), streamPushItem);
                }
            }
        }
        if (mediaItems.size() > 0) {
            for (MediaItem mediaItem : mediaItems) {
                streamInfoPushItemMap.put(mediaItem.getApp() + mediaItem.getStream(), mediaItem);
            }
        }
        zlmresTfulUtils.getMediaList(mediaServerItem, (mediaList ->{
            if (mediaList == null) return;
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
            Collection<MediaItem> offlineMediaItemList = streamInfoPushItemMap.values();
            if (offlineMediaItemList.size() > 0) {
                String type = "PUSH";
                for (MediaItem offlineMediaItem : offlineMediaItemList) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("serverId", userSetup.getServerId());
                    jsonObject.put("app", offlineMediaItem.getApp());
                    jsonObject.put("stream", offlineMediaItem.getStream());
                    jsonObject.put("register", false);
                    jsonObject.put("mediaServerId", mediaServerId);
                    redisCatchStorage.sendStreamChangeMsg(type, jsonObject);
                    // 移除redis内流的信息
                    redisCatchStorage.removeStream(mediaServerItem.getId(), "PUSH", offlineMediaItem.getApp(), offlineMediaItem.getStream());
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
        gbStreamMapper.updateStatusByMediaServerId(mediaServerId, false);
        // 发送流停止消息
        String type = "PUSH";
        // 发送redis消息
        List<MediaItem> streamInfoList = redisCatchStorage.getStreams(mediaServerId, type);
        if (streamInfoList.size() > 0) {
            for (MediaItem mediaItem : streamInfoList) {
                // 移除redis内流的信息
                redisCatchStorage.removeStream(mediaServerId, type, mediaItem.getApp(), mediaItem.getStream());
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("serverId", userSetup.getServerId());
                jsonObject.put("app", mediaItem.getApp());
                jsonObject.put("stream", mediaItem.getStream());
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
            streamPushItem.setCreateStamp(System.currentTimeMillis());
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
        // 查找开启了全部直播流共享的上级平台
        List<ParentPlatform> parentPlatforms = parentPlatformMapper.selectAllAhareAllLiveStream();
        if (parentPlatforms.size() > 0) {
            for (StreamPushItem stream : streamPushItems) {
                for (ParentPlatform parentPlatform : parentPlatforms) {
                    stream.setCatalogId(parentPlatform.getCatalogId());
                    stream.setPlatformId(parentPlatform.getServerGBId());
                    String streamId = stream.getStream();
                    StreamProxyItem streamProxyItem = platformGbStreamMapper.selectOne(stream.getApp(), streamId, parentPlatform.getServerGBId());
                    if (streamProxyItem == null) {
                        platformGbStreamMapper.add(stream);
                        eventPublisher.catalogEventPublishForStream(parentPlatform.getServerGBId(), stream, CatalogEvent.ADD);
                    }else {
                        if (!streamProxyItem.getGbId().equals(stream.getGbId())) {
                            // 此流使用另一个国标Id已经与该平台关联，移除此记录
                            platformGbStreamMapper.delByAppAndStreamAndPlatform(stream.getApp(), streamId, parentPlatform.getServerGBId());
                            platformGbStreamMapper.add(stream);
                            eventPublisher.catalogEventPublishForStream(parentPlatform.getServerGBId(), stream, CatalogEvent.ADD);
                            stream.setGbId(streamProxyItem.getGbId());
                            eventPublisher.catalogEventPublishForStream(parentPlatform.getServerGBId(), stream, CatalogEvent.DEL);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void batchAddForUpload(List<StreamPushItem> streamPushItems, Map<String, List<String[]>> streamPushItemsForAll ) {
        // 存储数据到stream_push表
        streamPushMapper.addAll(streamPushItems);
        List<StreamPushItem> streamPushItemForGbStream = streamPushItems.stream()
                .filter(streamPushItem-> streamPushItem.getId() != null)
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
            List<StreamPushItem> streamPushItemListFroPlatform = new ArrayList<>();
            Map<String, List<GbStream>> platformForEvent = new HashMap<>();
            // 遍历存储结果，查找app+Stream->platformId+catalogId的对应关系，然后执行批量写入
            for (StreamPushItem streamPushItem : streamPushItemsForPlatform) {
                List<String[]> platFormInfoList = streamPushItemsForAll.get(streamPushItem.getApp() + streamPushItem.getStream());
                if (platFormInfoList != null) {
                    if (platFormInfoList.size() > 0) {
                        for (String[] platFormInfoArray : platFormInfoList) {
                            StreamPushItem streamPushItemForPlatform = new StreamPushItem();
                            streamPushItemForPlatform.setGbStreamId(streamPushItem.getGbStreamId());
                            if (platFormInfoArray.length > 0) {
                                // 数组 platFormInfoArray 0 为平台ID。 1为目录ID
                                streamPushItemForPlatform.setPlatformId(platFormInfoArray[0]);

                                List<GbStream> gbStreamList = platformForEvent.get(streamPushItem.getPlatformId());
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

        int delStream = streamPushMapper.delAllForGbStream(gbStreams);
        gbStreamMapper.batchDelForGbStream(gbStreams);
        platformGbStreamMapper.delByGbStreams(gbStreams);
        if (delStream > 0) {
            for (GbStream gbStream : gbStreams) {
                MediaServerItem mediaServerItem = mediaServerService.getOne(gbStream.getMediaServerId());
                zlmresTfulUtils.closeStreams(mediaServerItem, gbStream.getApp(), gbStream.getStream());
            }

        }
        return true;
    }
}
