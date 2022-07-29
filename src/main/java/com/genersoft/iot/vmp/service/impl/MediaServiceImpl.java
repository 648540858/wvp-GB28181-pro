package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.MediaConfig;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.OnPublishHookParam;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.service.IMediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MediaServiceImpl implements IMediaService {

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private IMediaServerService mediaServerService;


    @Autowired
    private MediaConfig mediaConfig;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;



    @Override
    public StreamInfo getStreamInfoByAppAndStream(MediaServerItem mediaInfo, String app, String stream, Object tracks, String callId) {
        return getStreamInfoByAppAndStream(mediaInfo, app, stream, tracks, null, callId);
    }

    @Override
    public StreamInfo getStreamInfoByAppAndStreamWithCheck(String app, String stream, String mediaServerId, String addr, boolean authority) {
        StreamInfo streamInfo = null;
        if (mediaServerId == null) {
            mediaServerId = mediaConfig.getId();
        }
        MediaServerItem mediaInfo = mediaServerService.getOne(mediaServerId);
        if (mediaInfo == null) {
            return null;
        }
        StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(app, stream);
        if (streamAuthorityInfo == null) {
            return null;
        }
        JSONObject mediaList = zlmresTfulUtils.getMediaList(mediaInfo, app, stream);
        if (mediaList != null) {
            if (mediaList.getInteger("code") == 0) {
                JSONArray data = mediaList.getJSONArray("data");
                if (data == null) {
                    return null;
                }
                JSONObject mediaJSON = JSON.parseObject(JSON.toJSONString(data.get(0)), JSONObject.class);
                JSONArray tracks = mediaJSON.getJSONArray("tracks");
                if (authority) {
                    streamInfo = getStreamInfoByAppAndStream(mediaInfo, app, stream, tracks, addr,streamAuthorityInfo.getCallId());
                }else {
                    streamInfo = getStreamInfoByAppAndStream(mediaInfo, app, stream, tracks, addr,null);
                }

            }
        }
        return streamInfo;
    }



    @Override
    public StreamInfo getStreamInfoByAppAndStreamWithCheck(String app, String stream, String mediaServerId, boolean authority) {
        return getStreamInfoByAppAndStreamWithCheck(app, stream, mediaServerId, null, authority);
    }

    @Override
    public StreamInfo getStreamInfoByAppAndStream(MediaServerItem mediaInfo, String app, String stream, Object tracks, String addr, String callId) {
        StreamInfo streamInfoResult = new StreamInfo();
        streamInfoResult.setStream(stream);
        streamInfoResult.setApp(app);
        if (addr == null) {
            addr = mediaInfo.getStreamIp();
        }
        streamInfoResult.setIp(addr);
        streamInfoResult.setMediaServerId(mediaInfo.getId());
        String callIdParam = StringUtils.isEmpty(callId)?"":"?callId=" + callId;
        streamInfoResult.setRtmp(String.format("rtmp://%s:%s/%s/%s%s", addr, mediaInfo.getRtmpPort(), app,  stream, callIdParam));
        if (mediaInfo.getRtmpSSlPort() != 0) {
            streamInfoResult.setRtmps(String.format("rtmps://%s:%s/%s/%s%s", addr, mediaInfo.getRtmpSSlPort(), app,  stream, callIdParam));
        }
        streamInfoResult.setRtsp(String.format("rtsp://%s:%s/%s/%s%s", addr, mediaInfo.getRtspPort(), app,  stream, callIdParam));
        if (mediaInfo.getRtspSSLPort() != 0) {
            streamInfoResult.setRtsps(String.format("rtsps://%s:%s/%s/%s%s", addr, mediaInfo.getRtspSSLPort(), app,  stream, callIdParam));
        }
        streamInfoResult.setFlv(String.format("http://%s:%s/%s/%s.live.flv%s", addr, mediaInfo.getHttpPort(), app,  stream, callIdParam));
        streamInfoResult.setWs_flv(String.format("ws://%s:%s/%s/%s.live.flv%s", addr, mediaInfo.getHttpPort(), app,  stream, callIdParam));
        streamInfoResult.setHls(String.format("http://%s:%s/%s/%s/hls.m3u8%s", addr, mediaInfo.getHttpPort(), app,  stream, callIdParam));
        streamInfoResult.setWs_hls(String.format("ws://%s:%s/%s/%s/hls.m3u8%s", addr, mediaInfo.getHttpPort(), app,  stream, callIdParam));
        streamInfoResult.setFmp4(String.format("http://%s:%s/%s/%s.live.mp4%s", addr, mediaInfo.getHttpPort(), app,  stream, callIdParam));
        streamInfoResult.setWs_fmp4(String.format("ws://%s:%s/%s/%s.live.mp4%s", addr, mediaInfo.getHttpPort(), app,  stream, callIdParam));
        streamInfoResult.setTs(String.format("http://%s:%s/%s/%s.live.ts%s", addr, mediaInfo.getHttpPort(), app,  stream, callIdParam));
        streamInfoResult.setWs_ts(String.format("ws://%s:%s/%s/%s.live.ts%s", addr, mediaInfo.getHttpPort(), app,  stream, callIdParam));
        if (mediaInfo.getHttpSSlPort() != 0) {
            streamInfoResult.setHttps_flv(String.format("https://%s:%s/%s/%s.live.flv%s", addr, mediaInfo.getHttpSSlPort(), app,  stream, callIdParam));
            streamInfoResult.setWss_flv(String.format("wss://%s:%s/%s/%s.live.flv%s", addr, mediaInfo.getHttpSSlPort(), app,  stream, callIdParam));
            streamInfoResult.setHttps_hls(String.format("https://%s:%s/%s/%s/hls.m3u8%s", addr, mediaInfo.getHttpSSlPort(), app,  stream, callIdParam));
            streamInfoResult.setWss_hls(String.format("wss://%s:%s/%s/%s/hls.m3u8%s", addr, mediaInfo.getHttpSSlPort(), app,  stream, callIdParam));
            streamInfoResult.setHttps_fmp4(String.format("https://%s:%s/%s/%s.live.mp4%s", addr, mediaInfo.getHttpSSlPort(), app,  stream, callIdParam));
            streamInfoResult.setWss_fmp4(String.format("wss://%s:%s/%s/%s.live.mp4%s", addr, mediaInfo.getHttpSSlPort(), app,  stream, callIdParam));
            streamInfoResult.setHttps_ts(String.format("https://%s:%s/%s/%s.live.ts%s", addr, mediaInfo.getHttpSSlPort(), app,  stream, callIdParam));
            streamInfoResult.setWss_ts(String.format("wss://%s:%s/%s/%s.live.ts%s", addr, mediaInfo.getHttpSSlPort(), app,  stream, callIdParam));
            streamInfoResult.setWss_ts(String.format("wss://%s:%s/%s/%s.live.ts%s", addr, mediaInfo.getHttpSSlPort(), app,  stream, callIdParam));
            streamInfoResult.setRtc(String.format("https://%s:%s/index/api/webrtc?app=%s&stream=%s&type=play%s", mediaInfo.getStreamIp(), mediaInfo.getHttpSSlPort(), app,  stream, StringUtils.isEmpty(callId)?"":"&callId=" + callId));
        }

        streamInfoResult.setTracks(tracks);
        return streamInfoResult;
    }

}
