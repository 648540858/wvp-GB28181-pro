package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.media.service.IMediaNodeServerService;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.ZLMServerConfig;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service("zlm")
public class ZLMMediaNodeServerService implements IMediaNodeServerService {

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
}
