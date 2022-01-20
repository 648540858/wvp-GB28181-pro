package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetup;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
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
import com.genersoft.iot.vmp.storager.dao.GbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.ParentPlatformMapper;
import com.genersoft.iot.vmp.storager.dao.PlatformGbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.StreamPushMapper;
import com.genersoft.iot.vmp.vmanager.bean.StreamPushExcelDto;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class StreamPushServiceImpl implements IStreamPushService {

    @Autowired
    private GbStreamMapper gbStreamMapper;

    @Autowired
    private StreamPushMapper streamPushMapper;

    @Autowired
    private ParentPlatformMapper parentPlatformMapper;

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
        streamPushItem.setCreateStamp(item.getCreateStamp());
        streamPushItem.setOriginSock(item.getOriginSock());
        streamPushItem.setTotalReaderCount(item.getTotalReaderCount());
        streamPushItem.setOriginType(item.getOriginType());
        streamPushItem.setOriginTypeStr(item.getOriginTypeStr());
        streamPushItem.setOriginUrl(item.getOriginUrl());
        streamPushItem.setCreateStamp(item.getCreateStamp());
        streamPushItem.setAliveSecond(item.getAliveSecond());
        streamPushItem.setStatus(true);
        streamPushItem.setStreamType("push");
        streamPushItem.setVhost(item.getVhost());
        return streamPushItem;
    }

    @Override
    public PageInfo<StreamPushItem> getPushList(Integer page, Integer count) {
        PageHelper.startPage(page, count);
        List<StreamPushItem> all = streamPushMapper.selectAll();
        return new PageInfo<>(all);
    }

    @Override
    public List<StreamPushItem> getPushList(String mediaServerId) {
        return streamPushMapper.selectAllByMediaServerId(mediaServerId);
    }

    @Override
    public boolean saveToGB(GbStream stream) {
        stream.setStreamType("push");
        stream.setStatus(true);
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
                pushItemMap.put(streamPushItem.getApp() + streamPushItem.getStream(), streamPushItem);
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
        List<StreamPushItem> streamPushItems = streamPushMapper.selectAllByMediaServerId(mediaServerId);
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
}
