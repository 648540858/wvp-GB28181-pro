package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.media.zlm.ZLMServerConfig;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
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
    private ZLMRESTfulUtils zlmresTfulUtils;



    @Override
    public StreamInfo getStreamInfoByAppAndStream(String app, String stream, JSONArray tracks) {
        ZLMServerConfig mediaInfo = redisCatchStorage.getMediaInfo();
        StreamInfo streamInfoResult = new StreamInfo();
        streamInfoResult.setStreamId(stream);
        streamInfoResult.setApp(app);
        streamInfoResult.setRtmp(String.format("rtmp://%s:%s/%s/%s", mediaInfo.getStreamIp(), mediaInfo.getRtmpPort(), app,  stream));
        streamInfoResult.setRtsp(String.format("rtsp://%s:%s/%s/%s", mediaInfo.getStreamIp(), mediaInfo.getRtspPort(), app,  stream));
        streamInfoResult.setFlv(String.format("http://%s:%s/%s/%s.flv", mediaInfo.getStreamIp(), mediaInfo.getHttpPort(), app,  stream));
        streamInfoResult.setWs_flv(String.format("ws://%s:%s/%s/%s.flv", mediaInfo.getStreamIp(), mediaInfo.getHttpPort(), app,  stream));
        streamInfoResult.setHls(String.format("http://%s:%s/%s/%s/hls.m3u8", mediaInfo.getStreamIp(), mediaInfo.getHttpPort(), app,  stream));
        streamInfoResult.setWs_hls(String.format("ws://%s:%s/%s/%s/hls.m3u8", mediaInfo.getStreamIp(), mediaInfo.getHttpPort(), app,  stream));
        streamInfoResult.setFmp4(String.format("http://%s:%s/%s/%s.live.mp4", mediaInfo.getStreamIp(), mediaInfo.getHttpPort(), app,  stream));
        streamInfoResult.setWs_fmp4(String.format("ws://%s:%s/%s/%s.live.mp4", mediaInfo.getStreamIp(), mediaInfo.getHttpPort(), app,  stream));
        streamInfoResult.setTs(String.format("http://%s:%s/%s/%s.live.ts", mediaInfo.getStreamIp(), mediaInfo.getHttpPort(), app,  stream));
        streamInfoResult.setWs_ts(String.format("ws://%s:%s/%s/%s.live.ts", mediaInfo.getStreamIp(), mediaInfo.getHttpPort(), app,  stream));
        streamInfoResult.setRtc(String.format("http://%s:%s/index/api/webrtc?app=%s&stream=%s&type=play", mediaInfo.getStreamIp(), mediaInfo.getHttpPort(), app,  stream));
        streamInfoResult.setTracks(tracks);
        return streamInfoResult;
    }

    @Override
    public StreamInfo getStreamInfoByAppAndStreamWithCheck(String app, String stream) {
        StreamInfo streamInfo = null;
        JSONObject mediaList = zlmresTfulUtils.getMediaList(app, stream);
        if (mediaList != null) {
            if (mediaList.getInteger("code") == 0) {
                JSONArray data = mediaList.getJSONArray("data");
                if (data == null) return null;
                JSONObject mediaJSON = JSON.parseObject(JSON.toJSONString(data.get(0)), JSONObject.class);
                JSONArray tracks = mediaJSON.getJSONArray("tracks");
                streamInfo = getStreamInfoByAppAndStream(app, stream, tracks);
            }
        }
        return streamInfo;
    }
}
