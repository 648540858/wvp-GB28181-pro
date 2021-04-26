package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.service.IStreamPushService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.storager.dao.GbStreamMapper;
import com.genersoft.iot.vmp.storager.dao.PlatformGbStreamMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ZLMMediaListManager {

    private Logger logger = LoggerFactory.getLogger("ZLMMediaListManager");

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IVideoManagerStorager storager;

    @Autowired
    private GbStreamMapper gbStreamMapper;

    @Autowired
    private PlatformGbStreamMapper platformGbStreamMapper;

    @Autowired
    private IStreamPushService streamPushService;

    @Autowired
    private ZLMHttpHookSubscribe subscribe;


    public void updateMediaList() {
        storager.clearMediaList();

        // 使用异步的当时更新媒体流列表
        zlmresTfulUtils.getMediaList((mediaList ->{
            if (mediaList == null) return;
            String dataStr = mediaList.getString("data");

            Integer code = mediaList.getInteger("code");
            Map<String, StreamPushItem> result = new HashMap<>();
            List<StreamPushItem> streamPushItems = null;
            // 获取所有的国标关联
            List<GbStream> gbStreams = gbStreamMapper.selectAll();
            if (code == 0 ) {
                if (dataStr != null) {
                    streamPushItems = streamPushService.handleJSON(dataStr);
                }
            }else {
                logger.warn("更新视频流失败，错误code： " + code);
            }

            if (streamPushItems != null) {
                storager.updateMediaList(streamPushItems);
                for (StreamPushItem streamPushItem : streamPushItems) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("app", streamPushItem.getApp());
                    jsonObject.put("stream", streamPushItem.getStream());
                    subscribe.addSubscribe(ZLMHttpHookSubscribe.HookType.on_play,jsonObject,(response)->{
                        updateMedia(response.getString("app"), response.getString("stream"));
                    });
                }
            }
        }));

    }

    public void addMedia(String app, String streamId) {
        //使用异步更新推流
        updateMedia(app, streamId);
    }


    public void updateMedia(String app, String streamId) {
        //使用异步更新推流
        zlmresTfulUtils.getMediaList(app, streamId, "rtmp", json->{

            if (json == null) return;
            String dataStr = json.getString("data");

            Integer code = json.getInteger("code");
            Map<String, StreamPushItem> result = new HashMap<>();
            List<StreamPushItem> streamPushItems = null;
            if (code == 0 ) {
                if (dataStr != null) {
                    streamPushItems = streamPushService.handleJSON(dataStr);
                }
            }else {
                logger.warn("更新视频流失败，错误code： " + code);
            }

            if (streamPushItems != null && streamPushItems.size() == 1) {
                storager.updateMedia(streamPushItems.get(0));
            }
        });
    }


    public void removeMedia(String app, String streamId) {
        // 查找是否关联了国标， 关联了不删除， 置为离线
        StreamProxyItem streamProxyItem = gbStreamMapper.selectOne(app, streamId);
        if (streamProxyItem == null) {
            storager.removeMedia(app, streamId);
        }else {
            storager.mediaOutline(app, streamId);
        }
    }
}
