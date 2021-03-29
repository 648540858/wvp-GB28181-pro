package com.genersoft.iot.vmp.vmanager.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.media.zlm.ZLMHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.vmanager.service.IPlayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

import javax.sip.message.Response;
import java.util.UUID;

@Service
public class PlayServiceImpl implements IPlayService {

    private final static Logger logger = LoggerFactory.getLogger(PlayServiceImpl.class);

    @Autowired
    private IVideoManagerStorager storager;

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private DeferredResultHolder resultHolder;

    @Autowired
    private VideoStreamSessionManager streamSession;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Override
    public DeferredResult<ResponseEntity<String>> play(String deviceId, String channelId, ZLMHttpHookSubscribe.Event hookEvent, SipSubscribe.Event errorEvent) {
        Device device = storager.queryVideoDevice(deviceId);

        RequestMessage msg = this.createCallbackPlayMsg();
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<>();
        // 超时处理
        result.onTimeout(() -> {
            logger.warn(String.format("设备点播超时，deviceId：%s ，channelId：%s", deviceId, channelId));
            // 释放rtpserver
            cmder.closeRTPServer(device, channelId);
            StreamInfo streamInfo = streamSession.getPlayStreamInfo(channelId);
            streamSession.remove(streamInfo);
            msg.setData("Timeout");
            resultHolder.invokeResult(msg);
            if (errorEvent != null) {
                errorEvent.response(null);
            }
        });
        resultHolder.put(msg.getId(), result);

        // 判断是否已经存在点播
        StreamInfo oldStreamInfo = streamSession.getPlayStreamInfo(channelId);
        if (oldStreamInfo == null) {
            // 发送点播消息
            playStreamCmd(device, channelId, msg, hookEvent, errorEvent);
            return result;
        }

        // 若已有人点播，直接播放
        String streamId = oldStreamInfo.getStreamId();
        String mediaServerIp = oldStreamInfo.getMediaServerIp();
        JSONObject rtpInfo = zlmresTfulUtils.getRtpInfo(mediaServerIp, streamId);
        if (rtpInfo.getBoolean("exist")) {
            msg.setData(JSON.toJSONString(oldStreamInfo));
            resultHolder.invokeResult(msg);
            if (hookEvent != null) {
                hookEvent.response(null);
            }
            return result;
        }

        // 若已有人点播，但已超时自动断开，则重新发起点播
        storager.stopPlay(oldStreamInfo.getDeviceID(), oldStreamInfo.getChannelId());
        streamSession.remove(oldStreamInfo);

        playStreamCmd(device, channelId, msg, hookEvent, errorEvent);
        return result;
    }

    private void playStreamCmd(Device device, String channelId, RequestMessage msg, ZLMHttpHookSubscribe.Event hookEvent, SipSubscribe.Event errorEvent) {
        cmder.playStreamCmd(device, channelId, (JSONObject response) -> {
            logger.info("收到点播回调消息： " + response.toJSONString());
            this.onPublishHandlerForPlay(response, device.getDeviceId(), channelId, msg);
            if (hookEvent != null) {
                hookEvent.response(response);
            }
        }, event -> {
            StreamInfo streamInfo = streamSession.getPlayStreamInfo(channelId);
            streamSession.remove(streamInfo);
            Response response = event.getResponse();
            int statusCode = response.getStatusCode();
            String errMsg;
            if (503 == statusCode) {
                errMsg = "点播失败，请检查在NVR上是否可以正常打开监控，并检查NVR和SIP是否连通， 错误码： %s, %s";
            } else {
                errMsg = "点播失败，错误码： %s, %s";
            }
            msg.setData(String.format(errMsg, statusCode, response.getReasonPhrase()));
            resultHolder.invokeResult(msg);
            if (errorEvent != null) {
                errorEvent.response(event);
            }
        });
    }

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
