package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.media.bean.Track;
import com.genersoft.iot.vmp.media.service.IMediaNodeServerService;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.ZLMServerConfig;
import com.genersoft.iot.vmp.service.impl.DeviceServiceImpl;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("zlm")
public class ZLMMediaNodeServerService implements IMediaNodeServerService {

    private final static Logger logger = LoggerFactory.getLogger(ZLMMediaNodeServerService.class);

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private ZLMServerFactory zlmServerFactory;

    @Value("${sip.ip}")
    private String sipIp;

    @Override
    public int createRTPServer(MediaServerItem mediaServerItem, String streamId, long ssrc, Integer port, Boolean onlyAuto, Boolean reUsePort, Integer tcpMode) {
        return zlmServerFactory.createRTPServer(mediaServerItem, streamId, ssrc, port, onlyAuto, reUsePort, tcpMode);
    }

    @Override
    public void closeRtpServer(MediaServerItem mediaServerItem, String streamId) {
        zlmresTfulUtils.closeStreams(mediaServerItem, "rtp", streamId);
    }

    @Override
    public void closeRtpServer(MediaServerItem mediaServerItem, String streamId, CommonCallback<Boolean> callback) {
        zlmServerFactory.closeRtpServer(mediaServerItem, streamId, callback);
    }

    @Override
    public void closeStreams(MediaServerItem mediaServerItem, String app, String stream) {
        zlmresTfulUtils.closeStreams(mediaServerItem, app, stream);
    }

    @Override
    public Boolean updateRtpServerSSRC(MediaServerItem mediaServerItem, String streamId, String ssrc) {
        return zlmServerFactory.updateRtpServerSSRC(mediaServerItem, streamId, ssrc);
    }

    @Override
    public boolean checkNodeId(MediaServerItem mediaServerItem) {
        if (mediaServerItem == null) {
            return false;
        }
        JSONObject responseJSON = zlmresTfulUtils.getMediaServerConfig(mediaServerItem);
        if (responseJSON != null) {
            JSONArray data = responseJSON.getJSONArray("data");
            if (data != null && !data.isEmpty()) {
                ZLMServerConfig zlmServerConfig= JSON.parseObject(JSON.toJSONString(data.get(0)), ZLMServerConfig.class);
                return zlmServerConfig.getGeneralMediaServerId().equals(mediaServerItem.getId());
            }else {
                return false;
            }

        }else {
            return false;
        }
    }

    @Override
    public void online(MediaServerItem mediaServerItem) {

    }

    @Override
    public MediaServerItem checkMediaServer(String ip, int port, String secret) {
        MediaServerItem mediaServerItem = new MediaServerItem();
        mediaServerItem.setIp(ip);
        mediaServerItem.setHttpPort(port);
        mediaServerItem.setSecret(secret);
        JSONObject responseJSON = zlmresTfulUtils.getMediaServerConfig(mediaServerItem);
        if (responseJSON == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "连接失败");
        }
        JSONArray data = responseJSON.getJSONArray("data");
        if (data == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "读取配置失败");
        }
        ZLMServerConfig zlmServerConfig = JSON.parseObject(JSON.toJSONString(data.get(0)), ZLMServerConfig.class);
        if (zlmServerConfig == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "读取配置失败");
        }
        mediaServerItem.setId(zlmServerConfig.getGeneralMediaServerId());
        mediaServerItem.setHttpSSlPort(zlmServerConfig.getHttpPort());
        mediaServerItem.setRtmpPort(zlmServerConfig.getRtmpPort());
        mediaServerItem.setRtmpSSlPort(zlmServerConfig.getRtmpSslPort());
        mediaServerItem.setRtspPort(zlmServerConfig.getRtspPort());
        mediaServerItem.setRtspSSLPort(zlmServerConfig.getRtspSSlport());
        mediaServerItem.setRtpProxyPort(zlmServerConfig.getRtpProxyPort());
        mediaServerItem.setStreamIp(ip);
        mediaServerItem.setHookIp(sipIp.split(",")[0]);
        mediaServerItem.setSdpIp(ip);
        mediaServerItem.setType("zlm");
        return mediaServerItem;
    }

    @Override
    public boolean stopSendRtp(MediaServerItem mediaInfo, String app, String stream, String ssrc) {
        Map<String, Object> param = new HashMap<>();
        param.put("vhost", "__defaultVhost__");
        param.put("app", app);
        param.put("stream", stream);
        if (!ObjectUtils.isEmpty(ssrc)) {
            param.put("ssrc", ssrc);
        }
        JSONObject jsonObject = zlmresTfulUtils.stopSendRtp(mediaInfo, param);
        return (jsonObject != null && jsonObject.getInteger("code") == 0);

    }

    @Override
    public boolean deleteRecordDirectory(MediaServerItem mediaServerItem, String app, String stream, String date, String fileName) {
        logger.info("[zlm-deleteRecordDirectory] 删除磁盘文件, server: {} {}:{}->{}/{}", mediaServerItem.getId(), app, stream, date, fileName);
        JSONObject jsonObject = zlmresTfulUtils.deleteRecordDirectory(mediaServerItem, app,
                stream, date, fileName);
        if (jsonObject.getInteger("code") == 0) {
            return true;
        }else {
            logger.info("[zlm-deleteRecordDirectory] 删除磁盘文件错误, server: {} {}:{}->{}/{}, 结果： {}", mediaServerItem.getId(), app, stream, date, fileName, jsonObject);
            return false;
        }
    }

    @Override
    public List<StreamInfo> getMediaList(MediaServerItem mediaServerItem, String app, String stream) {
        List<StreamInfo> streamInfoList = new ArrayList<>();
        JSONObject mediaList = zlmresTfulUtils.getMediaList(mediaServerItem, app, stream);
        if (mediaList != null) {
            if (mediaList.getInteger("code") == 0) {
                JSONArray data = mediaList.getJSONArray("data");
                if (data == null) {
                    return null;
                }
                JSONObject mediaJSON = data.getJSONObject(0);
                JSONArray tracks = mediaJSON.getJSONArray("tracks");

                if (authority) {
                    streamInfo = getStreamInfoByAppAndStream(mediaServerItem, app, stream, tracks, null, calld, true);
                }else {
                    streamInfo = getStreamInfoByAppAndStream(mediaServerItem, app, stream, tracks, null,null, true);
                }
            }
        }
        return streamInfoList;
    }

    public StreamInfo getStreamInfoByAppAndStream(MediaServerItem mediaInfo, String app, String stream, Track track, String addr, String callId, boolean isPlay) {
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
        streamInfoResult.setRtc(addr, mediaInfo.getHttpPort(),mediaInfo.getHttpSSlPort(), app,  stream, callIdParam, isPlay);

        streamInfoResult.setTrack(track);
        return streamInfoResult;
    }
}
