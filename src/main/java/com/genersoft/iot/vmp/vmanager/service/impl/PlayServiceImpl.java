package com.genersoft.iot.vmp.vmanager.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.MediaServerConfig;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.vmanager.play.PlayController;
import com.genersoft.iot.vmp.vmanager.service.IPlayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;

@Service
public class PlayServiceImpl implements IPlayService {

    private final static Logger logger = LoggerFactory.getLogger(PlayServiceImpl.class);

    @Autowired
    private IVideoManagerStorager storager;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private DeferredResultHolder resultHolder;

    @Override
    public void onPublishHandlerForPlay(JSONObject resonse, String deviceId, String channelId, String uuid) {
        RequestMessage msg = new RequestMessage();
        msg.setId(DeferredResultHolder.CALLBACK_CMD_PlAY + uuid);
        StreamInfo streamInfo = onPublishHandler(resonse, deviceId, channelId, uuid);
        if (streamInfo != null) {
            DeviceChannel deviceChannel = storager.queryChannel(deviceId, channelId);
            if (deviceChannel != null) {
                deviceChannel.setStreamId(streamInfo.getStreamId());
                storager.startPlay(deviceId, channelId, streamInfo.getStreamId());
            }

            redisCatchStorage.startPlay(streamInfo);
            msg.setData(JSON.toJSONString(streamInfo));
            resultHolder.invokeResult(msg);
        } else {
            logger.warn("设备预览API调用失败！");
            msg.setData("设备预览API调用失败！");
            resultHolder.invokeResult(msg);
        }
    }

    @Override
    public void onPublishHandlerForPlayBack(JSONObject resonse, String deviceId, String channelId, String uuid) {
        RequestMessage msg = new RequestMessage();
        msg.setId(DeferredResultHolder.CALLBACK_CMD_PlAY + uuid);
        StreamInfo streamInfo = onPublishHandler(resonse, deviceId, channelId, uuid);
        if (streamInfo != null) {
            redisCatchStorage.startPlayback(streamInfo);
            msg.setData(JSON.toJSONString(streamInfo));
            resultHolder.invokeResult(msg);
        } else {
            logger.warn("设备预览API调用失败！");
            msg.setData("设备预览API调用失败！");
            resultHolder.invokeResult(msg);
        }
    }

    public StreamInfo onPublishHandler(JSONObject resonse, String deviceId, String channelId, String uuid) {
        String streamId = resonse.getString("id");
        StreamInfo streamInfo = new StreamInfo();
        streamInfo.setStreamId(streamId);
        streamInfo.setDeviceID(deviceId);
        streamInfo.setChannelId(channelId);
        MediaServerConfig mediaServerConfig = redisCatchStorage.getMediaInfo();

        streamInfo.setFlv(String.format("http://%s:%s/rtp/%s.flv", mediaServerConfig.getWanIp(), mediaServerConfig.getHttpPort(), streamId));
        streamInfo.setWs_flv(String.format("ws://%s:%s/rtp/%s.flv", mediaServerConfig.getWanIp(), mediaServerConfig.getHttpPort(), streamId));

        streamInfo.setFmp4(String.format("http://%s:%s/rtp/%s.live.mp4", mediaServerConfig.getWanIp(), mediaServerConfig.getHttpPort(), streamId));
        streamInfo.setWs_fmp4(String.format("ws://%s:%s/rtp/%s.live.mp4", mediaServerConfig.getWanIp(), mediaServerConfig.getHttpPort(), streamId));

        streamInfo.setHls(String.format("http://%s:%s/rtp/%s/hls.m3u8", mediaServerConfig.getWanIp(), mediaServerConfig.getHttpPort(), streamId));
        streamInfo.setWs_hls(String.format("ws://%s:%s/rtp/%s/hls.m3u8", mediaServerConfig.getWanIp(), mediaServerConfig.getHttpPort(), streamId));

        streamInfo.setTs(String.format("http://%s:%s/rtp/%s.live.ts", mediaServerConfig.getWanIp(), mediaServerConfig.getHttpPort(), streamId));
        streamInfo.setWs_ts(String.format("ws://%s:%s/rtp/%s.live.ts", mediaServerConfig.getWanIp(), mediaServerConfig.getHttpPort(), streamId));

        streamInfo.setRtmp(String.format("rtmp://%s:%s/rtp/%s", mediaServerConfig.getWanIp(), mediaServerConfig.getRtmpPort(), streamId));
        streamInfo.setRtsp(String.format("rtsp://%s:%s/rtp/%s", mediaServerConfig.getWanIp(), mediaServerConfig.getRtspPort(), streamId));

        return streamInfo;
    }

}
