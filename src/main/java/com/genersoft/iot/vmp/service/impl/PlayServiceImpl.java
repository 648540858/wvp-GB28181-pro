package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetup;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.media.zlm.ZLMHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.PlayBackCallback;
import com.genersoft.iot.vmp.service.bean.PlayBackResult;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.genersoft.iot.vmp.vmanager.gb28181.play.bean.PlayResult;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.service.IPlayService;
import gov.nist.javax.sip.stack.SIPDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.FileNotFoundException;
import java.util.*;

@SuppressWarnings(value = {"rawtypes", "unchecked"})
@Service
public class PlayServiceImpl implements IPlayService {

    private final static Logger logger = LoggerFactory.getLogger(PlayServiceImpl.class);

    @Autowired
    private IVideoManagerStorager storager;

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private SIPCommanderFroPlatform sipCommanderFroPlatform;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private RedisUtil redis;

    @Autowired
    private DeferredResultHolder resultHolder;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private IMediaService mediaService;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private VideoStreamSessionManager streamSession;

    @Autowired
    private UserSetup userSetup;


    @Override
    public PlayResult play(MediaServerItem mediaServerItem, String deviceId, String channelId,
                           ZLMHttpHookSubscribe.Event hookEvent, SipSubscribe.Event errorEvent,
                           Runnable timeoutCallback) {
        PlayResult playResult = new PlayResult();
        RequestMessage msg = new RequestMessage();
        String key = DeferredResultHolder.CALLBACK_CMD_PLAY + deviceId + channelId;
        msg.setKey(key);
        String uuid = UUID.randomUUID().toString();
        msg.setId(uuid);
        playResult.setUuid(uuid);
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<>(userSetup.getPlayTimeout());
        playResult.setResult(result);
        // 录像查询以channelId作为deviceId查询
        resultHolder.put(key, uuid, result);
        if (mediaServerItem == null) {
            WVPResult wvpResult = new WVPResult();
            wvpResult.setCode(-1);
            wvpResult.setMsg("未找到可用的zlm");
            msg.setData(wvpResult);
            resultHolder.invokeResult(msg);
            return playResult;
        }
        Device device = redisCatchStorage.getDevice(deviceId);
        StreamInfo streamInfo = redisCatchStorage.queryPlayByDevice(deviceId, channelId);
        playResult.setDevice(device);

        result.onCompletion(()->{
            // 点播结束时调用截图接口
            // TODO 应该在上流时调用更好，结束也可能是错误结束
            try {
                String classPath = ResourceUtils.getURL("classpath:").getPath();
                // 兼容打包为jar的class路径
                if(classPath.contains("jar")) {
                    classPath = classPath.substring(0, classPath.lastIndexOf("."));
                    classPath = classPath.substring(0, classPath.lastIndexOf("/") + 1);
                }
                if (classPath.startsWith("file:")) {
                    classPath = classPath.substring(classPath.indexOf(":") + 1);
                }
                String path = classPath + "static/static/snap/";
                // 兼容Windows系统路径（去除前面的“/”）
                if(System.getProperty("os.name").contains("indows")) {
                    path = path.substring(1);
                }
                String fileName =  deviceId + "_" + channelId + ".jpg";
                ResponseEntity responseEntity =  (ResponseEntity)result.getResult();
                if (responseEntity != null && responseEntity.getStatusCode() == HttpStatus.OK) {
                    WVPResult wvpResult = (WVPResult)responseEntity.getBody();
                    if (Objects.requireNonNull(wvpResult).getCode() == 0) {
                        StreamInfo streamInfoForSuccess = (StreamInfo)wvpResult.getData();
                        MediaServerItem mediaInfo = mediaServerService.getOne(streamInfoForSuccess.getMediaServerId());
                        String streamUrl = streamInfoForSuccess.getFmp4();
                        // 请求截图
                        logger.info("[请求截图]: " + fileName);
                        zlmresTfulUtils.getSnap(mediaInfo, streamUrl, 15, 1, path, fileName);
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });
        if (streamInfo == null) {
            String streamId = null;
            if (mediaServerItem.isRtpEnable()) {
                streamId = String.format("%s_%s", device.getDeviceId(), channelId);
            }
            SSRCInfo ssrcInfo = mediaServerService.openRTPServer(mediaServerItem, streamId);
            // 超时处理
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    logger.warn(String.format("设备点播超时，deviceId：%s ，channelId：%s", deviceId, channelId));
                    if (timeoutCallback != null) {
                        timeoutCallback.run();
                    }
                    WVPResult wvpResult = new WVPResult();
                    wvpResult.setCode(-1);
                    SIPDialog dialog = streamSession.getDialogByStream(deviceId, channelId, ssrcInfo.getStream());
                    if (dialog != null) {
                        wvpResult.setMsg("收流超时，请稍候重试");
                        // 点播超时回复BYE 同时释放ssrc以及此次点播的资源
                        cmder.streamByeCmd(device.getDeviceId(), channelId, ssrcInfo.getStream());
                    }else {
                        wvpResult.setMsg("点播超时，请稍候重试");
                        mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                        mediaServerService.closeRTPServer(deviceId, channelId, ssrcInfo.getStream());
                        streamSession.remove(deviceId, channelId, ssrcInfo.getStream());
                    }

                    msg.setData(wvpResult);

                    // 回复之前所有的点播请求
                    resultHolder.invokeAllResult(msg);
                }
            }, userSetup.getPlayTimeout());
            // 发送点播消息
            cmder.playStreamCmd(mediaServerItem, ssrcInfo, device, channelId, (MediaServerItem mediaServerItemInUse, JSONObject response) -> {
                logger.info("收到订阅消息： " + response.toJSONString());
                timer.cancel();
                onPublishHandlerForPlay(mediaServerItemInUse, response, deviceId, channelId, uuid);
                if (hookEvent != null) {
                    hookEvent.response(mediaServerItem, response);
                }
            }, (event) -> {
                timer.cancel();
                WVPResult wvpResult = new WVPResult();
                wvpResult.setCode(-1);
                // 点播返回sip错误
                mediaServerService.closeRTPServer(playResult.getDevice().getDeviceId(), channelId, ssrcInfo.getStream());
                // 释放ssrc
                mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                streamSession.remove(deviceId, channelId, ssrcInfo.getStream());

                wvpResult.setMsg(String.format("点播失败， 错误码： %s, %s", event.statusCode, event.msg));
                msg.setData(wvpResult);
                resultHolder.invokeAllResult(msg);
                if (errorEvent != null) {
                    errorEvent.response(event);
                }
            });
        } else {
            String streamId = streamInfo.getStream();
            if (streamId == null) {
                WVPResult wvpResult = new WVPResult();
                wvpResult.setCode(-1);
                wvpResult.setMsg("点播失败， redis缓存streamId等于null");
                msg.setData(wvpResult);
                resultHolder.invokeAllResult(msg);
                return playResult;
            }
            String mediaServerId = streamInfo.getMediaServerId();
            MediaServerItem mediaInfo = mediaServerService.getOne(mediaServerId);

            JSONObject rtpInfo = zlmresTfulUtils.getRtpInfo(mediaInfo, streamId);
            if (rtpInfo != null && rtpInfo.getBoolean("exist")) {

                WVPResult wvpResult = new WVPResult();
                wvpResult.setCode(0);
                wvpResult.setMsg("success");
                wvpResult.setData(streamInfo);
                msg.setData(wvpResult);

                resultHolder.invokeAllResult(msg);
                if (hookEvent != null) {
                    hookEvent.response(mediaServerItem, JSONObject.parseObject(JSON.toJSONString(streamInfo)));
                }
            } else {
                // TODO 点播前是否重置状态
                redisCatchStorage.stopPlay(streamInfo);
                storager.stopPlay(streamInfo.getDeviceID(), streamInfo.getChannelId());
                String streamId2 = null;
                if (mediaServerItem.isRtpEnable()) {
                    streamId2 = String.format("%s_%s", device.getDeviceId(), channelId);
                }
                SSRCInfo ssrcInfo = mediaServerService.openRTPServer(mediaServerItem, streamId2);
                // 超时处理
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        logger.warn(String.format("设备点播超时，deviceId：%s ，channelId：%s", deviceId, channelId));
                        if (timeoutCallback != null) {
                            timeoutCallback.run();
                        }
                        WVPResult wvpResult = new WVPResult();
                        wvpResult.setCode(-1);
                        SIPDialog dialog = streamSession.getDialogByStream(deviceId, channelId, ssrcInfo.getStream());
                        if (dialog != null) {
                            wvpResult.setMsg("收流超时，请稍候重试");
                            // 点播超时回复BYE 同时释放ssrc以及此次点播的资源
                            cmder.streamByeCmd(device.getDeviceId(), channelId, ssrcInfo.getStream());
                        }else {
                            wvpResult.setMsg("点播超时，请稍候重试");
                            mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                            mediaServerService.closeRTPServer(deviceId, channelId, ssrcInfo.getStream());
                            streamSession.remove(deviceId, channelId, ssrcInfo.getStream());
                        }

                        msg.setData(wvpResult);
                        // 回复之前所有的点播请求
                        resultHolder.invokeAllResult(msg);
                    }
                }, userSetup.getPlayTimeout());
                cmder.playStreamCmd(mediaServerItem, ssrcInfo, device, channelId, (MediaServerItem mediaServerItemInuse, JSONObject response) -> {
                    logger.info("收到订阅消息： " + response.toJSONString());
                    onPublishHandlerForPlay(mediaServerItemInuse, response, deviceId, channelId, uuid);
                }, (event) -> {
                    mediaServerService.closeRTPServer(playResult.getDevice().getDeviceId(), channelId, ssrcInfo.getStream());
                    // 释放ssrc
                    mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                    streamSession.remove(deviceId, channelId, ssrcInfo.getStream());
                    WVPResult wvpResult = new WVPResult();
                    wvpResult.setCode(-1);
                    wvpResult.setMsg(String.format("点播失败， 错误码： %s, %s", event.statusCode, event.msg));
                    msg.setData(wvpResult);
                    resultHolder.invokeAllResult(msg);
                });
            }
        }

        return playResult;
    }

    @Override
    public void onPublishHandlerForPlay(MediaServerItem mediaServerItem, JSONObject response, String deviceId, String channelId, String uuid) {
        RequestMessage msg = new RequestMessage();
        msg.setId(uuid);
        msg.setKey(DeferredResultHolder.CALLBACK_CMD_PLAY + deviceId + channelId);
        StreamInfo streamInfo = onPublishHandler(mediaServerItem, response, deviceId, channelId);
        if (streamInfo != null) {
            DeviceChannel deviceChannel = storager.queryChannel(deviceId, channelId);
            if (deviceChannel != null) {
                deviceChannel.setStreamId(streamInfo.getStream());
                storager.startPlay(deviceId, channelId, streamInfo.getStream());
            }
            redisCatchStorage.startPlay(streamInfo);
            msg.setData(JSON.toJSONString(streamInfo));

            WVPResult wvpResult = new WVPResult();
            wvpResult.setCode(0);
            wvpResult.setMsg("success");
            wvpResult.setData(streamInfo);
            msg.setData(wvpResult);

            resultHolder.invokeAllResult(msg);
        } else {
            logger.warn("设备预览API调用失败！");
            msg.setData("设备预览API调用失败！");
            resultHolder.invokeAllResult(msg);
        }
    }

    @Override
    public MediaServerItem getNewMediaServerItem(Device device) {
        if (device == null) return null;
        String mediaServerId = device.getMediaServerId();
        MediaServerItem mediaServerItem;
        if (mediaServerId == null) {
            mediaServerItem = mediaServerService.getMediaServerForMinimumLoad();
        }else {
            mediaServerItem = mediaServerService.getOne(mediaServerId);
        }
        if (mediaServerItem == null) {
            logger.warn("点播时未找到可使用的ZLM...");
        }
        return mediaServerItem;
    }


    @Override
    public DeferredResult<ResponseEntity<String>> playBack(String deviceId, String channelId, String startTime, String endTime, PlayBackCallback callback) {
        String uuid = UUID.randomUUID().toString();
        String key = DeferredResultHolder.CALLBACK_CMD_PLAYBACK + deviceId + channelId;
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<>(30000L);
        Device device = storager.queryVideoDevice(deviceId);
        if (device == null) {
            result.setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
            return result;
        }

        MediaServerItem newMediaServerItem = getNewMediaServerItem(device);
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(newMediaServerItem, null, true);
        resultHolder.put(DeferredResultHolder.CALLBACK_CMD_PLAY + deviceId + channelId, uuid, result);
        RequestMessage msg = new RequestMessage();
        msg.setId(uuid);
        msg.setKey(key);
        PlayBackResult<RequestMessage> playBackResult = new PlayBackResult<>();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                logger.warn(String.format("设备回放超时，deviceId：%s ，channelId：%s", deviceId, channelId));
                playBackResult.setCode(-1);
                playBackResult.setData(msg);
                callback.call(playBackResult);
                SIPDialog dialog = streamSession.getDialogByStream(deviceId, channelId, ssrcInfo.getStream());
                // 点播超时回复BYE 同时释放ssrc以及此次点播的资源
                if (dialog != null) {
                    // 点播超时回复BYE 同时释放ssrc以及此次点播的资源
                    cmder.streamByeCmd(device.getDeviceId(), channelId, ssrcInfo.getStream());
                }else {
                    mediaServerService.releaseSsrc(newMediaServerItem.getId(), ssrcInfo.getSsrc());
                    mediaServerService.closeRTPServer(deviceId, channelId, ssrcInfo.getStream());
                    streamSession.remove(deviceId, channelId, ssrcInfo.getStream());
                }
                cmder.streamByeCmd(device.getDeviceId(), channelId, ssrcInfo.getStream());
                // 回复之前所有的点播请求
                callback.call(playBackResult);
            }
        }, userSetup.getPlayTimeout());
        cmder.playbackStreamCmd(newMediaServerItem, ssrcInfo, device, channelId, startTime, endTime, (MediaServerItem mediaServerItem, JSONObject response) -> {
            logger.info("收到订阅消息： " + response.toJSONString());
            timer.cancel();
            StreamInfo streamInfo = onPublishHandler(mediaServerItem, response, deviceId, channelId);
            if (streamInfo == null) {
                logger.warn("设备回放API调用失败！");
                msg.setData("设备回放API调用失败！");
                playBackResult.setCode(-1);
                playBackResult.setData(msg);
                callback.call(playBackResult);
                return;
            }
            redisCatchStorage.startPlayback(streamInfo);
            msg.setData(JSON.toJSONString(streamInfo));
            playBackResult.setCode(0);
            playBackResult.setData(msg);
            playBackResult.setMediaServerItem(mediaServerItem);
            playBackResult.setResponse(response);
            callback.call(playBackResult);
        }, event -> {
            timer.cancel();
            msg.setData(String.format("回放失败， 错误码： %s, %s", event.statusCode, event.msg));
            playBackResult.setCode(-1);
            playBackResult.setData(msg);
            playBackResult.setEvent(event);
            callback.call(playBackResult);
            streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());
        });
        return result;
    }



    @Override
    public void onPublishHandlerForDownload(MediaServerItem mediaServerItem, JSONObject response, String deviceId, String channelId, String uuid) {
        RequestMessage msg = new RequestMessage();
        msg.setKey(DeferredResultHolder.CALLBACK_CMD_DOWNLOAD + deviceId + channelId);
        msg.setId(uuid);
        StreamInfo streamInfo = onPublishHandler(mediaServerItem, response, deviceId, channelId);
        if (streamInfo != null) {
            redisCatchStorage.startDownload(streamInfo);
            msg.setData(JSON.toJSONString(streamInfo));
            resultHolder.invokeResult(msg);
        } else {
            logger.warn("设备预览API调用失败！");
            msg.setData("设备预览API调用失败！");
            resultHolder.invokeResult(msg);
        }
    }


    public StreamInfo onPublishHandler(MediaServerItem mediaServerItem, JSONObject resonse, String deviceId, String channelId) {
        String streamId = resonse.getString("stream");
        JSONArray tracks = resonse.getJSONArray("tracks");
        StreamInfo streamInfo = mediaService.getStreamInfoByAppAndStream(mediaServerItem,"rtp", streamId, tracks);
        streamInfo.setDeviceID(deviceId);
        streamInfo.setChannelId(channelId);
        return streamInfo;
    }

    @Override
    public void zlmServerOffline(String mediaServerId) {
        // 处理正在向上推流的上级平台
        List<SendRtpItem> sendRtpItems = redisCatchStorage.querySendRTPServer(null);
        if (sendRtpItems.size() > 0) {
            for (SendRtpItem sendRtpItem : sendRtpItems) {
                if (sendRtpItem.getMediaServerId().equals(mediaServerId)) {
                    ParentPlatform platform = storager.queryParentPlatByServerGBId(sendRtpItem.getPlatformId());
                    sipCommanderFroPlatform.streamByeCmd(platform, sendRtpItem.getCallId());
                }
            }
        }
        // 处理正在观看的国标设备
        List<SsrcTransaction> allSsrc = streamSession.getAllSsrc();
        if (allSsrc.size() > 0) {
            for (SsrcTransaction ssrcTransaction : allSsrc) {
                if(ssrcTransaction.getMediaServerId().equals(mediaServerId)) {
                    cmder.streamByeCmd(ssrcTransaction.getDeviceId(), ssrcTransaction.getChannelId(), ssrcTransaction.getStream());
                }
            }
        }
    }
}
