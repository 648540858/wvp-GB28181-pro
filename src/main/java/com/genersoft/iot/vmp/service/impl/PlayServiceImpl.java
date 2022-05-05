package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.gb28181.utils.DateUtil;
import com.genersoft.iot.vmp.media.zlm.AssistRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.ZLMHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.InviteTimeOutCallback;
import com.genersoft.iot.vmp.service.bean.PlayBackCallback;
import com.genersoft.iot.vmp.service.bean.PlayBackResult;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
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

import javax.sip.ResponseEvent;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.util.*;

@SuppressWarnings(value = {"rawtypes", "unchecked"})
@Service
public class PlayServiceImpl implements IPlayService {

    private final static Logger logger = LoggerFactory.getLogger(PlayServiceImpl.class);

    @Autowired
    private IVideoManagerStorage storager;

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
    private AssistRESTfulUtils assistRESTfulUtils;

    @Autowired
    private IMediaService mediaService;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private VideoStreamSessionManager streamSession;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private DynamicTask dynamicTask;




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
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());
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
        if (streamInfo != null) {
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
            }else {
                redisCatchStorage.stopPlay(streamInfo);
                storager.stopPlay(streamInfo.getDeviceID(), streamInfo.getChannelId());
                streamInfo = null;
            }

        }
        if (streamInfo == null) {
            String streamId = null;
            if (mediaServerItem.isRtpEnable()) {
                streamId = String.format("%s_%s", device.getDeviceId(), channelId);
            }
            SSRCInfo ssrcInfo = mediaServerService.openRTPServer(mediaServerItem, streamId, device.isSsrcCheck());
            play(mediaServerItem, ssrcInfo, device, channelId, (mediaServerItemInUse, response)->{
                if (hookEvent != null) {
                    hookEvent.response(mediaServerItem, response);
                }
            }, event -> {
                // sip error错误
                WVPResult wvpResult = new WVPResult();
                wvpResult.setCode(-1);
                wvpResult.setMsg(String.format("点播失败， 错误码： %s, %s", event.statusCode, event.msg));
                msg.setData(wvpResult);
                resultHolder.invokeAllResult(msg);
                if (errorEvent != null) {
                    errorEvent.response(event);
                }
            }, (code, msgStr)->{
                // invite点播超时
                WVPResult wvpResult = new WVPResult();
                wvpResult.setCode(-1);
                if (code == 0) {
                    wvpResult.setMsg("点播超时，请稍候重试");
                }else if (code == 1) {
                    wvpResult.setMsg("收流超时，请稍候重试");
                }
                msg.setData(wvpResult);
                // 回复之前所有的点播请求
                resultHolder.invokeAllResult(msg);
            }, uuid);
        }
        return playResult;
    }



    @Override
    public void play(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo, Device device, String channelId,
                           ZLMHttpHookSubscribe.Event hookEvent, SipSubscribe.Event errorEvent,
                           InviteTimeOutCallback timeoutCallback, String uuid) {

        String streamId = null;
        if (mediaServerItem.isRtpEnable()) {
            streamId = String.format("%s_%s", device.getDeviceId(), channelId);
        }
        if (ssrcInfo == null) {
            ssrcInfo = mediaServerService.openRTPServer(mediaServerItem, streamId, device.isSsrcCheck());
        }

        // 超时处理
        String timeOutTaskKey = UUID.randomUUID().toString();
        SSRCInfo finalSsrcInfo = ssrcInfo;
        dynamicTask.startDelay( timeOutTaskKey,()->{
            logger.warn(String.format("设备点播超时，deviceId：%s ，channelId：%s", device.getDeviceId(), channelId));

            SIPDialog dialog = streamSession.getDialogByStream(device.getDeviceId(), channelId, finalSsrcInfo.getStream());
            if (dialog != null) {
                timeoutCallback.run(1, "收流超时");
                // 点播超时回复BYE 同时释放ssrc以及此次点播的资源
                cmder.streamByeCmd(device.getDeviceId(), channelId, finalSsrcInfo.getStream(), null);
            }else {
                timeoutCallback.run(0, "点播超时");
                mediaServerService.releaseSsrc(mediaServerItem.getId(), finalSsrcInfo.getSsrc());
                mediaServerService.closeRTPServer(device.getDeviceId(), channelId, finalSsrcInfo.getStream());
                streamSession.remove(device.getDeviceId(), channelId, finalSsrcInfo.getStream());
            }
        }, userSetting.getPlayTimeout()*1000);
        final String ssrc = ssrcInfo.getSsrc();
        cmder.playStreamCmd(mediaServerItem, ssrcInfo, device, channelId, (MediaServerItem mediaServerItemInuse, JSONObject response) -> {
            logger.info("收到订阅消息： " + response.toJSONString());
            dynamicTask.stop(timeOutTaskKey);
            // hook响应
            onPublishHandlerForPlay(mediaServerItemInuse, response, device.getDeviceId(), channelId, uuid);
            hookEvent.response(mediaServerItemInuse, response);
        }, (event) -> {
            ResponseEvent responseEvent = (ResponseEvent)event.event;
            String contentString = new String(responseEvent.getResponse().getRawContent());
            // 获取ssrc
            int ssrcIndex = contentString.indexOf("y=");
            // 检查是否有y字段
            if (ssrcIndex >= 0) {
                //ssrc规定长度为10字节，不取余下长度以避免后续还有“f=”字段 TODO 后续对不规范的非10位ssrc兼容
                String ssrcInResponse = contentString.substring(ssrcIndex + 2, ssrcIndex + 12);
                if (!ssrc.equals(ssrcInResponse) && device.isSsrcCheck()) { // 查询到ssrc不一致且开启了ssrc校验则需要针对处理
                    // 查询 ssrcInResponse 是否可用
                    if (mediaServerItem.isRtpEnable() && !mediaServerItem.getSsrcConfig().checkSsrc(ssrcInResponse)) {
                        // ssrc 不可用
                        // 释放ssrc
                        mediaServerService.releaseSsrc(mediaServerItem.getId(), finalSsrcInfo.getSsrc());
                        streamSession.remove(device.getDeviceId(), channelId, finalSsrcInfo.getStream());
                        event.msg = "下级自定义了ssrc,但是此ssrc不可用";
                        event.statusCode = 400;
                        errorEvent.response(event);
                        return;
                    }
                    // 关闭rtp server
                    mediaServerService.closeRTPServer(device.getDeviceId(), channelId, finalSsrcInfo.getStream());
                    // 重新开启ssrc server
                    mediaServerService.openRTPServer(mediaServerItem, finalSsrcInfo.getStream(), ssrcInResponse, device.isSsrcCheck(), false);
                }
            }
        }, (event) -> {
            dynamicTask.stop(timeOutTaskKey);
            mediaServerService.closeRTPServer(device.getDeviceId(), channelId, finalSsrcInfo.getStream());
            // 释放ssrc
            mediaServerService.releaseSsrc(mediaServerItem.getId(), finalSsrcInfo.getSsrc());

            streamSession.remove(device.getDeviceId(), channelId, finalSsrcInfo.getStream());
            errorEvent.response(event);
        });
    }

    @Override
    public void onPublishHandlerForPlay(MediaServerItem mediaServerItem, JSONObject response, String deviceId, String channelId, String uuid) {
        RequestMessage msg = new RequestMessage();
        if (uuid != null) {
            msg.setId(uuid);
        }
        msg.setKey(DeferredResultHolder.CALLBACK_CMD_PLAY + deviceId + channelId);
        StreamInfo streamInfo = onPublishHandler(mediaServerItem, response, deviceId, channelId);
        if (streamInfo != null) {
            DeviceChannel deviceChannel = storager.queryChannel(deviceId, channelId);
            if (deviceChannel != null) {
                deviceChannel.setStreamId(streamInfo.getStream());
                storager.startPlay(deviceId, channelId, streamInfo.getStream());
            }
            redisCatchStorage.startPlay(streamInfo);

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
        if (device == null) {
            return null;
        }
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
    public DeferredResult<ResponseEntity<String>> playBack(String deviceId, String channelId, String startTime,
                                                           String endTime,InviteStreamCallback inviteStreamCallback,
                                                           PlayBackCallback callback) {
        Device device = storager.queryVideoDevice(deviceId);
        if (device == null) {
            return null;
        }
        MediaServerItem newMediaServerItem = getNewMediaServerItem(device);
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(newMediaServerItem, null, true);

        return playBack(newMediaServerItem, ssrcInfo, deviceId, channelId, startTime, endTime, inviteStreamCallback, callback);
    }

    @Override
    public DeferredResult<ResponseEntity<String>> playBack(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo,
                                                           String deviceId, String channelId, String startTime,
                                                           String endTime, InviteStreamCallback infoCallBack,
                                                           PlayBackCallback playBackCallback) {
        if (mediaServerItem == null || ssrcInfo == null) {
            return null;
        }
        String uuid = UUID.randomUUID().toString();
        String key = DeferredResultHolder.CALLBACK_CMD_PLAYBACK + deviceId + channelId;
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<>(30000L);
        Device device = storager.queryVideoDevice(deviceId);
        if (device == null) {
            result.setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
            return result;
        }

        resultHolder.put(DeferredResultHolder.CALLBACK_CMD_PLAYBACK + deviceId + channelId, uuid, result);
        RequestMessage msg = new RequestMessage();
        msg.setId(uuid);
        msg.setKey(key);
        PlayBackResult<RequestMessage> playBackResult = new PlayBackResult<>();
        String  playBackTimeOutTaskKey = UUID.randomUUID().toString();
        dynamicTask.startDelay(playBackTimeOutTaskKey, ()->{
            logger.warn(String.format("设备回放超时，deviceId：%s ，channelId：%s", deviceId, channelId));
            playBackResult.setCode(-1);
            playBackResult.setData(msg);
            playBackCallback.call(playBackResult);
            SIPDialog dialog = streamSession.getDialogByStream(deviceId, channelId, ssrcInfo.getStream());
            // 点播超时回复BYE 同时释放ssrc以及此次点播的资源
            if (dialog != null) {
                // 点播超时回复BYE 同时释放ssrc以及此次点播的资源
                cmder.streamByeCmd(device.getDeviceId(), channelId, ssrcInfo.getStream(), null);
            }else {
                mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                mediaServerService.closeRTPServer(deviceId, channelId, ssrcInfo.getStream());
                streamSession.remove(deviceId, channelId, ssrcInfo.getStream());
            }
            cmder.streamByeCmd(device.getDeviceId(), channelId, ssrcInfo.getStream(), null);
            // 回复之前所有的点播请求
            playBackCallback.call(playBackResult);
        }, userSetting.getPlayTimeout()*1000);

        cmder.playbackStreamCmd(mediaServerItem, ssrcInfo, device, channelId, startTime, endTime, infoCallBack,
                (InviteStreamInfo inviteStreamInfo) -> {
                    logger.info("收到订阅消息： " + inviteStreamInfo.getResponse().toJSONString());
                    dynamicTask.stop(playBackTimeOutTaskKey);
                    StreamInfo streamInfo = onPublishHandler(inviteStreamInfo.getMediaServerItem(), inviteStreamInfo.getResponse(), deviceId, channelId);
                    if (streamInfo == null) {
                        logger.warn("设备回放API调用失败！");
                        msg.setData("设备回放API调用失败！");
                        playBackResult.setCode(-1);
                        playBackResult.setData(msg);
                        playBackCallback.call(playBackResult);
                        return;
                    }
                    redisCatchStorage.startPlayback(streamInfo, inviteStreamInfo.getCallId());
                    msg.setData(JSON.toJSONString(streamInfo));
                    playBackResult.setCode(0);
                    playBackResult.setData(msg);
                    playBackResult.setMediaServerItem(inviteStreamInfo.getMediaServerItem());
                    playBackResult.setResponse(inviteStreamInfo.getResponse());
                    playBackCallback.call(playBackResult);
                }, event -> {
                    dynamicTask.stop(playBackTimeOutTaskKey);
                    msg.setData(String.format("回放失败， 错误码： %s, %s", event.statusCode, event.msg));
                    playBackResult.setCode(-1);
                    playBackResult.setData(msg);
                    playBackResult.setEvent(event);
                    playBackCallback.call(playBackResult);
                    streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());
                });
        return result;
    }

    @Override
    public DeferredResult<ResponseEntity<String>> download(String deviceId, String channelId, String startTime, String endTime, int downloadSpeed, InviteStreamCallback infoCallBack, PlayBackCallback hookCallBack) {
        Device device = storager.queryVideoDevice(deviceId);
        if (device == null) {
            return null;
        }
        MediaServerItem newMediaServerItem = getNewMediaServerItem(device);
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(newMediaServerItem, null, true);

        return download(newMediaServerItem, ssrcInfo, deviceId, channelId, startTime, endTime, downloadSpeed,infoCallBack, hookCallBack);
    }

    @Override
    public DeferredResult<ResponseEntity<String>> download(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo, String deviceId, String channelId, String startTime, String endTime, int downloadSpeed, InviteStreamCallback infoCallBack, PlayBackCallback hookCallBack) {
        if (mediaServerItem == null || ssrcInfo == null) {
            return null;
        }
        String uuid = UUID.randomUUID().toString();
        String key = DeferredResultHolder.CALLBACK_CMD_DOWNLOAD + deviceId + channelId;
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<>(30000L);
        Device device = storager.queryVideoDevice(deviceId);
        if (device == null) {
            result.setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
            return result;
        }

        resultHolder.put(key, uuid, result);
        RequestMessage msg = new RequestMessage();
        msg.setId(uuid);
        msg.setKey(key);
        WVPResult<StreamInfo> wvpResult = new WVPResult<>();
        msg.setData(wvpResult);
        PlayBackResult<RequestMessage> downloadResult = new PlayBackResult<>();
        downloadResult.setData(msg);

        String downLoadTimeOutTaskKey = UUID.randomUUID().toString();
        dynamicTask.startDelay(downLoadTimeOutTaskKey, ()->{
            logger.warn(String.format("录像下载请求超时，deviceId：%s ，channelId：%s", deviceId, channelId));
            wvpResult.setCode(-1);
            wvpResult.setMsg("录像下载请求超时");
            downloadResult.setCode(-1);
            hookCallBack.call(downloadResult);
            SIPDialog dialog = streamSession.getDialogByStream(deviceId, channelId, ssrcInfo.getStream());
            // 点播超时回复BYE 同时释放ssrc以及此次点播的资源
            if (dialog != null) {
                // 点播超时回复BYE 同时释放ssrc以及此次点播的资源
                cmder.streamByeCmd(device.getDeviceId(), channelId, ssrcInfo.getStream(), null);
            }else {
                mediaServerService.releaseSsrc(mediaServerItem.getId(), ssrcInfo.getSsrc());
                mediaServerService.closeRTPServer(deviceId, channelId, ssrcInfo.getStream());
                streamSession.remove(deviceId, channelId, ssrcInfo.getStream());
            }
            cmder.streamByeCmd(device.getDeviceId(), channelId, ssrcInfo.getStream(), null);
            // 回复之前所有的点播请求
            hookCallBack.call(downloadResult);
        }, userSetting.getPlayTimeout()*1000);
        cmder.downloadStreamCmd(mediaServerItem, ssrcInfo, device, channelId, startTime, endTime, downloadSpeed, infoCallBack,
                inviteStreamInfo -> {
                    logger.info("收到订阅消息： " + inviteStreamInfo.getResponse().toJSONString());
                    dynamicTask.stop(downLoadTimeOutTaskKey);
                    StreamInfo streamInfo = onPublishHandler(inviteStreamInfo.getMediaServerItem(), inviteStreamInfo.getResponse(), deviceId, channelId);
                    streamInfo.setStartTime(startTime);
                    streamInfo.setEndTime(endTime);
                    if (streamInfo == null) {
                        logger.warn("录像下载API调用失败！");
                        wvpResult.setCode(-1);
                        wvpResult.setMsg("录像下载API调用失败");
                        downloadResult.setCode(-1);
                        hookCallBack.call(downloadResult);
                        return ;
                    }
                    redisCatchStorage.startDownload(streamInfo, inviteStreamInfo.getCallId());
                    wvpResult.setCode(0);
                    wvpResult.setMsg("success");
                    wvpResult.setData(streamInfo);
                    downloadResult.setCode(0);
                    downloadResult.setMediaServerItem(inviteStreamInfo.getMediaServerItem());
                    downloadResult.setResponse(inviteStreamInfo.getResponse());
                    hookCallBack.call(downloadResult);
                }, event -> {
                    dynamicTask.stop(downLoadTimeOutTaskKey);
                    downloadResult.setCode(-1);
                    wvpResult.setCode(-1);
                    wvpResult.setMsg(String.format("录像下载失败， 错误码： %s, %s", event.statusCode, event.msg));
                    downloadResult.setEvent(event);
                    hookCallBack.call(downloadResult);
                    streamSession.remove(device.getDeviceId(), channelId, ssrcInfo.getStream());
                });
        return result;
    }

    @Override
    public StreamInfo getDownLoadInfo(String deviceId, String channelId, String stream) {
        StreamInfo streamInfo = redisCatchStorage.queryDownload(deviceId, channelId, stream, null);
        if (streamInfo != null) {
            if (streamInfo.getProgress() == 1) {
                return streamInfo;
            }

            // 获取当前已下载时长
            String mediaServerId = streamInfo.getMediaServerId();
            MediaServerItem mediaServerItem = mediaServerService.getOne(mediaServerId);
            if (mediaServerItem == null) {
                logger.warn("查询录像信息时发现节点已离线");
                return null;
            }
            if (mediaServerItem.getRecordAssistPort() != 0) {
                JSONObject jsonObject = assistRESTfulUtils.fileDuration(mediaServerItem, streamInfo.getApp(), streamInfo.getStream(), null);
                if (jsonObject != null && jsonObject.getInteger("code") == 0) {
                    long duration = jsonObject.getLong("data");

                    if (duration == 0) {
                        streamInfo.setProgress(0);
                    }else {
                        String startTime = streamInfo.getStartTime();
                        String endTime = streamInfo.getEndTime();
                        long start = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime);
                        long end = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime);

                        BigDecimal currentCount = new BigDecimal(duration/1000);
                        BigDecimal totalCount = new BigDecimal(end-start);
                        BigDecimal divide = currentCount.divide(totalCount,2, BigDecimal.ROUND_HALF_UP);
                        double process = divide.doubleValue();
                        streamInfo.setProgress(process);
                    }
                }
            }
        }
        return streamInfo;
    }

    @Override
    public void onPublishHandlerForDownload(InviteStreamInfo inviteStreamInfo, String deviceId, String channelId, String uuid) {
        RequestMessage msg = new RequestMessage();
        msg.setKey(DeferredResultHolder.CALLBACK_CMD_DOWNLOAD + deviceId + channelId);
        msg.setId(uuid);
        StreamInfo streamInfo = onPublishHandler(inviteStreamInfo.getMediaServerItem(), inviteStreamInfo.getResponse(), deviceId, channelId);
        if (streamInfo != null) {
            redisCatchStorage.startDownload(streamInfo, inviteStreamInfo.getCallId());
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
                    cmder.streamByeCmd(ssrcTransaction.getDeviceId(), ssrcTransaction.getChannelId(),
                            ssrcTransaction.getStream(), null);
                }
            }
        }
    }
}
