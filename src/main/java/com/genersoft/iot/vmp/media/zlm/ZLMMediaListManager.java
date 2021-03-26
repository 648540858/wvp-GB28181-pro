package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.genersoft.iot.vmp.common.RealVideo;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.gb28181.session.SsrcUtil;
import com.genersoft.iot.vmp.media.zlm.dto.MediaItem;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.impl.RedisCatchStorageImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ZLMMediaListManager {

    private Logger logger = LoggerFactory.getLogger("ZLMMediaListManager");

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;


    public void updateMediaList() {
        JSONObject mediaList = zlmresTfulUtils.getMediaList();
        String dataStr = mediaList.getString("data");

        Integer code = mediaList.getInteger("code");
        Map<String, RealVideo> result = new HashMap<>();
        if (code == 0 ) {
            if (dataStr != null) {
                List<MediaItem> mediaItems = JSON.parseObject(dataStr, new TypeReference<List<MediaItem>>() {});
                for (MediaItem item : mediaItems) {
                    if ("rtp".equals(item.getApp())) {
                        continue;
                    }
                    String key = item.getApp() + "_" + item.getStream();
                    RealVideo realVideo = result.get(key);
                    if (realVideo == null) {
                        realVideo = new RealVideo();
                        realVideo.setApp(item.getApp());
                        realVideo.setStream(item.getStream());
                        realVideo.setAliveSecond(item.getAliveSecond());
                        realVideo.setCreateStamp(item.getCreateStamp());
                        realVideo.setOriginSock(item.getOriginSock());
                        realVideo.setTotalReaderCount(item.getTotalReaderCount());
                        realVideo.setOriginType(item.getOriginType());
                        realVideo.setOriginTypeStr(item.getOriginTypeStr());
                        realVideo.setOriginUrl(item.getOriginUrl());
                        realVideo.setCreateStamp(item.getCreateStamp());
                        realVideo.setAliveSecond(item.getAliveSecond());

                        ArrayList<RealVideo.MediaSchema> mediaSchemas = new ArrayList<>();
                        realVideo.setSchemas(mediaSchemas);
                        realVideo.setTracks(item.getTracks());
                        realVideo.setVhost(item.getVhost());
                        result.put(key, realVideo);
                    }

                    RealVideo.MediaSchema mediaSchema = new RealVideo.MediaSchema();
                    mediaSchema.setSchema(item.getSchema());
                    mediaSchema.setBytesSpeed(item.getBytesSpeed());
                    realVideo.getSchemas().add(mediaSchema);
                }

            }
        }else {
            logger.warn("更新视频流失败，错误code： " + code);
        }

        List<RealVideo> realVideos = new ArrayList<>(result.values());
        Collections.sort(realVideos);
        redisCatchStorage.updateMediaList(realVideos);
    }



}
