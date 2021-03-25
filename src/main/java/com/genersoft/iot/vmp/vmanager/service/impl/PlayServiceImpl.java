package com.genersoft.iot.vmp.vmanager.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.vmanager.service.IPlayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PlayServiceImpl implements IPlayService {

    private final static Logger logger = LoggerFactory.getLogger(PlayServiceImpl.class);

    @Autowired
    private IVideoManagerStorager storager;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private DeferredResultHolder resultHolder;

    @Autowired
    private VideoStreamSessionManager streamSession;

    @Override
    public RequestMessage createCallbackPlayMsg() {
        String msgId = DeferredResultHolder.CALLBACK_CMD_PlAY + UUID.randomUUID();
        RequestMessage msg = new RequestMessage();
        msg.setId(msgId);
        return msg;
    }

    @Override
    public void onPublishHandlerForPlay(JSONObject response, String deviceId, String channelId, RequestMessage msg) {
        String streamId = response.getString("id");
        StreamInfo streamInfo = streamSession.getStreamInfo(channelId, streamId);
        if (streamInfo != null) {
            DeviceChannel deviceChannel = storager.queryChannel(deviceId, channelId);
            if (deviceChannel != null) {
                deviceChannel.setStreamId(streamInfo.getStreamId());
                storager.startPlay(deviceId, channelId, streamInfo.getStreamId());
            }

//            redisCatchStorage.startPlay(streamInfo);
            msg.setData(JSON.toJSONString(streamInfo));
            resultHolder.invokeResult(msg);
        } else {
            logger.warn("设备点播API调用失败！");
            msg.setData("设备点播API调用失败！");
            resultHolder.invokeResult(msg);
        }
    }

    @Override
    public void onPublishHandlerForPlayBack(JSONObject resonse, String deviceId, String channelId, RequestMessage msg) {
        String streamId = resonse.getString("id");
        StreamInfo streamInfo = streamSession.getStreamInfo(channelId, streamId);
        if (streamInfo != null) {
//            redisCatchStorage.startPlayback(streamInfo);
            msg.setData(JSON.toJSONString(streamInfo));
            resultHolder.invokeResult(msg);
        } else {
            logger.warn("设备回放API调用失败！");
            msg.setData("设备回放API调用失败！");
            resultHolder.invokeResult(msg);
        }
    }

}
