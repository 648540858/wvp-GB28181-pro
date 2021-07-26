package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.service.IMediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MediaServiceImpl implements IMediaService {

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IVideoManagerStorager storager;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;



    @Override
    public StreamInfo getStreamInfoByAppAndStream(MediaServerItem mediaInfo, String app, String stream, JSONArray tracks) {
        return getStreamInfoByAppAndStream(mediaInfo, app, stream, tracks, null);
    }

    @Override
    public StreamInfo getStreamInfoByAppAndStreamWithCheck(String app, String stream, String mediaServerId, String addr) {
        StreamInfo streamInfo = null;
        MediaServerItem mediaInfo = mediaServerService.getOne(mediaServerId);
        if (mediaInfo == null) {
            return streamInfo;
        }
        JSONObject mediaList = zlmresTfulUtils.getMediaList(mediaInfo, app, stream);
        if (mediaList != null) {
            if (mediaList.getInteger("code") == 0) {
                JSONArray data = mediaList.getJSONArray("data");
                if (data == null) return null;
                JSONObject mediaJSON = JSON.parseObject(JSON.toJSONString(data.get(0)), JSONObject.class);
                JSONArray tracks = mediaJSON.getJSONArray("tracks");
                streamInfo = getStreamInfoByAppAndStream(mediaInfo, app, stream, tracks);
            }
        }
        return streamInfo;
    }

    @Override
    public StreamInfo getStreamInfoByAppAndStreamWithCheck(String app, String stream, String mediaServerId) {
        return getStreamInfoByAppAndStreamWithCheck(app, stream, mediaServerId, null);
    }

    @Override
    public StreamInfo getStreamInfoByAppAndStream(MediaServerItem mediaInfo, String app, String stream, JSONArray tracks, String addr) {
        StreamInfo streamInfoResult = new StreamInfo();
        streamInfoResult.setStreamId(stream);
        streamInfoResult.setApp(app);
        if (addr == null) {
            addr = mediaInfo.getStreamIp();
        }
        streamInfoResult.setMediaServerId(mediaInfo.getId());
        streamInfoResult.setRtmp(String.format("rtmp://%s:%s/%s/%s", addr, mediaInfo.getRtmpPort(), app,  stream));
        streamInfoResult.setRtsp(String.format("rtsp://%s:%s/%s/%s", addr, mediaInfo.getRtspPort(), app,  stream));
        streamInfoResult.setFlv(String.format("http://%s:%s/%s/%s.flv", addr, mediaInfo.getHttpPort(), app,  stream));
        streamInfoResult.setWs_flv(String.format("ws://%s:%s/%s/%s.flv", addr, mediaInfo.getHttpPort(), app,  stream));
        streamInfoResult.setHls(String.format("http://%s:%s/%s/%s/hls.m3u8", addr, mediaInfo.getHttpPort(), app,  stream));
        streamInfoResult.setWs_hls(String.format("ws://%s:%s/%s/%s/hls.m3u8", addr, mediaInfo.getHttpPort(), app,  stream));
        streamInfoResult.setFmp4(String.format("http://%s:%s/%s/%s.live.mp4", addr, mediaInfo.getHttpPort(), app,  stream));
        streamInfoResult.setWs_fmp4(String.format("ws://%s:%s/%s/%s.live.mp4", addr, mediaInfo.getHttpPort(), app,  stream));
        streamInfoResult.setTs(String.format("http://%s:%s/%s/%s.live.ts", addr, mediaInfo.getHttpPort(), app,  stream));
        streamInfoResult.setWs_ts(String.format("ws://%s:%s/%s/%s.live.ts", addr, mediaInfo.getHttpPort(), app,  stream));
        streamInfoResult.setRtc(String.format("http://%s:%s/index/api/webrtc?app=%s&stream=%s&type=play", mediaInfo.getStreamIp(), mediaInfo.getHttpPort(), app,  stream));
        streamInfoResult.setTracks(tracks);
        return streamInfoResult;
    }

}
