package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.dto.MediaItem;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.GbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.PlatformGbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.StreamPushMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StreamPushServiceImpl implements IStreamPushService {

    @Autowired
    private GbStreamMapper gbStreamMapper;

    @Autowired
    private StreamPushMapper streamPushMapper;

    @Autowired
    private PlatformGbStreamMapper platformGbStreamMapper;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IMediaServerService mediaServerService;

    @Override
    public List<StreamPushItem> handleJSON(String jsonData, MediaServerItem mediaServerItem) {
        if (jsonData == null) return null;

        Map<String, StreamPushItem> result = new HashMap<>();

        List<MediaItem> mediaItems = JSON.parseObject(jsonData, new TypeReference<List<MediaItem>>() {});
        for (MediaItem item : mediaItems) {

            // 不保存国标推理以及拉流代理的流
            if (item.getOriginType() == 1 || item.getOriginType() == 2 || item.getOriginType() == 8) {
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
    public boolean saveToGB(GbStream stream) {
        stream.setStreamType("push");
        stream.setStatus(true);
        int add = gbStreamMapper.add(stream);
        return add > 0;
    }

    @Override
    public boolean removeFromGB(GbStream stream) {
        int del = gbStreamMapper.del(stream.getApp(), stream.getStream());
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
        int delStream = streamPushMapper.del(app, streamId);
        gbStreamMapper.del(app, streamId);
        platformGbStreamMapper.delByAppAndStream(app, streamId);
        if (delStream > 0) {
            MediaServerItem mediaServerItem = mediaServerService.getOne(streamPushItem.getMediaServerId());
            zlmresTfulUtils.closeStreams(mediaServerItem,app, streamId);
        }
        return true;
    }

}
