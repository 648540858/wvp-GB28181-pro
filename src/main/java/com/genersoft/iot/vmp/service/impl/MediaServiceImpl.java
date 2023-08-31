package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.StreamURL;
import com.genersoft.iot.vmp.conf.MediaConfig;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.service.IMediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.net.URL;

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
        String calld = null;
        StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(app, stream);
        if (streamAuthorityInfo != null) {
            calld = streamAuthorityInfo.getCallId();
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
                    streamInfo = getStreamInfoByAppAndStream(mediaInfo, app, stream, tracks, addr, calld);
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
        String callIdParam = ObjectUtils.isEmpty(callId)?"":"?callId=" + callId;
        streamInfoResult.setRtmp(addr, mediaInfo.getRtmpPort(),mediaInfo.getRtmpSSlPort(), app,  stream, callIdParam);
        streamInfoResult.setRtsp(addr, mediaInfo.getRtspPort(),mediaInfo.getRtspSSLPort(), app,  stream, callIdParam);
        streamInfoResult.setFlv(addr, mediaInfo.getHttpPort(),mediaInfo.getHttpSSlPort(), app,  stream, callIdParam);
        streamInfoResult.setFmp4(addr, mediaInfo.getHttpPort(),mediaInfo.getHttpSSlPort(), app,  stream, callIdParam);
        streamInfoResult.setHls(addr, mediaInfo.getHttpPort(),mediaInfo.getHttpSSlPort(), app,  stream, callIdParam);
        streamInfoResult.setTs(addr, mediaInfo.getHttpPort(),mediaInfo.getHttpSSlPort(), app,  stream, callIdParam);
        streamInfoResult.setRtc(addr, mediaInfo.getHttpPort(),mediaInfo.getHttpSSlPort(), app,  stream, callIdParam);

        streamInfoResult.setTracks(tracks);
        return streamInfoResult;
    }

}
