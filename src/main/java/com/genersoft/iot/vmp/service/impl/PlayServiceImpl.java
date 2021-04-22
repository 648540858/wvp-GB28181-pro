package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
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
import com.genersoft.iot.vmp.vmanager.gb28181.play.bean.PlayResult;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.service.IPlayService;
import gov.nist.javax.sip.stack.SIPDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.header.CallIdHeader;
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
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private IMediaService mediaService;

    @Autowired
    private VideoStreamSessionManager streamSession;


    @Override
    public PlayResult play(String deviceId, String channelId, ZLMHttpHookSubscribe.Event hookEvent, SipSubscribe.Event errorEvent) {
        PlayResult playResult = new PlayResult();
        Device device = storager.queryVideoDevice(deviceId);
        StreamInfo streamInfo = redisCatchStorage.queryPlayByDevice(deviceId, channelId);
        playResult.setDevice(device);
        UUID uuid = UUID.randomUUID();
        playResult.setUuid(uuid.toString());
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String>>();
        playResult.setResult(result);
        // 录像查询以channelId作为deviceId查询
        resultHolder.put(DeferredResultHolder.CALLBACK_CMD_PlAY + uuid, result);
        // 超时处理
        result.onTimeout(()->{
            logger.warn(String.format("设备点播超时，deviceId：%s ，channelId：%s", deviceId, channelId));
            // 释放rtpserver
            cmder.closeRTPServer(playResult.getDevice(), channelId);
            RequestMessage msg = new RequestMessage();
            msg.setId(DeferredResultHolder.CALLBACK_CMD_PlAY + playResult.getUuid());
            msg.setData("Timeout");
            resultHolder.invokeResult(msg);
        });
        if (streamInfo == null) {
            // 发送点播消息
            cmder.playStreamCmd(device, channelId, (JSONObject response) -> {
                logger.info("收到订阅消息： " + response.toJSONString());
                onPublishHandlerForPlay(response, deviceId, channelId, uuid.toString());
                if (hookEvent != null) {
                    hookEvent.response(response);
                }
            }, event -> {
                RequestMessage msg = new RequestMessage();
                msg.setId(DeferredResultHolder.CALLBACK_CMD_PlAY + uuid);
                Response response = event.getResponse();
                cmder.closeRTPServer(playResult.getDevice(), channelId);
                msg.setData(String.format("点播失败， 错误码： %s, %s", response.getStatusCode(), response.getReasonPhrase()));
                resultHolder.invokeResult(msg);
                if (errorEvent != null) {
                    errorEvent.response(event);
                }
            });
        } else {
            String streamId = streamInfo.getStreamId();
            if (streamId == null) {
                RequestMessage msg = new RequestMessage();
                msg.setId(DeferredResultHolder.CALLBACK_CMD_PlAY + uuid);
                msg.setData(String.format("点播失败， redis缓存streamId等于null"));
                resultHolder.invokeResult(msg);
                return playResult;
            }
            JSONObject rtpInfo = zlmresTfulUtils.getRtpInfo(streamId);
            if (rtpInfo != null && rtpInfo.getBoolean("exist")) {
                RequestMessage msg = new RequestMessage();
                msg.setId(DeferredResultHolder.CALLBACK_CMD_PlAY + uuid);
                msg.setData(JSON.toJSONString(streamInfo));
                resultHolder.invokeResult(msg);
                if (hookEvent != null) {
                    hookEvent.response(JSONObject.parseObject(JSON.toJSONString(streamInfo)));
                }
            } else {
                redisCatchStorage.stopPlay(streamInfo);
                storager.stopPlay(streamInfo.getDeviceID(), streamInfo.getChannelId());
                cmder.playStreamCmd(device, channelId, (JSONObject response) -> {
                    logger.info("收到订阅消息： " + response.toJSONString());
                    onPublishHandlerForPlay(response, deviceId, channelId, uuid.toString());
                }, event -> {
                    cmder.closeRTPServer(playResult.getDevice(), channelId);
                    RequestMessage msg = new RequestMessage();
                    msg.setId(DeferredResultHolder.CALLBACK_CMD_PlAY + uuid);
                    Response response = event.getResponse();
                    msg.setData(String.format("点播失败， 错误码： %s, %s", response.getStatusCode(), response.getReasonPhrase()));
                    resultHolder.invokeResult(msg);
                });
            }
        }

        return playResult;
    }

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
            ClientTransaction transaction = streamSession.getTransaction(deviceId, channelId);
            SIPDialog dialog = (SIPDialog)transaction.getDialog();
            StreamInfo.TransactionInfo transactionInfo = new StreamInfo.TransactionInfo();
            transactionInfo.callId = dialog.getCallId().getCallId();
            transactionInfo.localTag = dialog.getLocalTag();
            transactionInfo.remoteTag = dialog.getRemoteTag();
            transactionInfo.branch = dialog.getFirstTransactionInt().getBranchId();
            streamInfo.setTransactionInfo(transactionInfo);
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
        String streamId = resonse.getString("stream");
        JSONArray tracks = resonse.getJSONArray("tracks");
        StreamInfo streamInfo = mediaService.getStreamInfoByAppAndStream("rtp", streamId, tracks);
        streamInfo.setDeviceID(deviceId);
        streamInfo.setChannelId(channelId);
        return streamInfo;
    }

}
