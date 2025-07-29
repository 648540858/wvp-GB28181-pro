package com.genersoft.iot.vmp.jt1078.service.impl;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.ftpServer.FtpSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.jt1078.bean.*;
import com.genersoft.iot.vmp.jt1078.cmd.JT1078Template;
import com.genersoft.iot.vmp.jt1078.event.FtpUploadEvent;
import com.genersoft.iot.vmp.jt1078.proc.request.J1205;
import com.genersoft.iot.vmp.jt1078.proc.response.*;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078PlayService;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.hook.Hook;
import com.genersoft.iot.vmp.media.event.hook.HookData;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.hook.HookType;
import com.genersoft.iot.vmp.media.event.media.MediaArrivalEvent;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.media.event.media.MediaNotFoundEvent;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaSendRtpStoppedEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.ISendRtpServerService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.utils.MediaServerUtils;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.sip.message.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class jt1078PlayServiceImpl implements Ijt1078PlayService {

    public static final String talkApp = "jt_talk";

    @Autowired
    private ISendRtpServerService sendRtpServerService;

    @Autowired
    private Ijt1078Service jt1078Service;

    @Autowired
    private JT1078Template jt1078Template;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private HookSubscribe subscribe;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private UserSetting userSetting;

    /**
     * 流到来的处理
     */
    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(MediaArrivalEvent event) {
        if (event.getApp().equals(talkApp) && event.getStream().endsWith("_talk")) {
            // 收到对JT讲的流
            if (event.getStream().indexOf("_") <= 0) {
                log.info("[JT-对讲流到来] 流格式有误，stream应该为jt_[phoneNumber]_[channelId]_talk");
                return;
            }
            String[] streamArray = event.getStream().split("_");
            if (streamArray.length != 4) {
                log.info("[JT-对讲流到来] 流格式有误，stream应该为jt_[phoneNumber]_[channelId]_talk");
                return;
            }
            String phoneNumber = streamArray[1];
            String channelId = streamArray[2];
            JTDevice device = jt1078Service.getDevice(phoneNumber);
            if (device == null) {
                log.info("[JT-对讲流到来] 未找到设备{}", phoneNumber);
                return;
            }
            sendTalk(device, Integer.valueOf(channelId), event.getMediaServer(), event.getApp(), event.getStream());

        }
    }

    /**
     * 流离开的处理
     */
    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(MediaDepartureEvent event) {

    }

    /**
     * 流未找到的处理
     */
    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(MediaNotFoundEvent event) {
        if (!userSetting.getAutoApplyPlay()) {
            return;
        }
        JTMediaStreamType jtMediaStreamType = checkStreamFromJt(event.getStream());
        if (jtMediaStreamType == null){
            return;
        }
        String[] streamParamArray = event.getStream().split("_");
        String phoneNumber = streamParamArray[1];
        int channelId = Integer.parseInt(streamParamArray[2]);
        String params = event.getParams();
        Map<String, String> paramMap = MediaServerUtils.urlParamToMap(params);
        int type = 0;
        try {
            type = Integer.parseInt(paramMap.get("type"));
        }catch (NumberFormatException ignored) {}
        if (jtMediaStreamType.equals(JTMediaStreamType.PLAY)) {
            play(phoneNumber, channelId, 0, null);
        }else if (jtMediaStreamType.equals(JTMediaStreamType.PLAYBACK)) {
            String startTimeParam = DateUtil.jt1078Toyyyy_MM_dd_HH_mm_ss(streamParamArray[3]);
            String endTimeParam = DateUtil.jt1078Toyyyy_MM_dd_HH_mm_ss(streamParamArray[4]);
            int rate = 0;
            int playbackType = 0;
            int playbackSpeed = 0;
            try {
                rate = Integer.parseInt(paramMap.get("rate"));
                playbackType = Integer.parseInt(paramMap.get("playbackType"));
                playbackSpeed = Integer.parseInt(paramMap.get("playbackSpeed"));
            }catch (NumberFormatException ignored) {}
            playback(phoneNumber, channelId, startTimeParam, endTimeParam, type, rate, playbackType, playbackSpeed, null);
        }
    }


    /**
     * 校验流是否是属于部标的
     */
    @Override
    public JTMediaStreamType checkStreamFromJt(String stream) {
        if (!stream.startsWith("jt_")) {
            return null;
        }
        String[] streamParamArray = stream.split("_");
        if (streamParamArray.length == 3) {
            return JTMediaStreamType.PLAY;
        }else if (streamParamArray.length == 5) {
            return JTMediaStreamType.PLAYBACK;
        }else if (streamParamArray.length == 4) {
            return JTMediaStreamType.TALK;
        }else {
            return null;
        }
    }

    private final Map<String, List<CommonCallback<WVPResult<StreamInfo>>>> inviteErrorCallbackMap = new ConcurrentHashMap<>();

    @Override
    public void play(String phoneNumber, Integer channelId, int type, CommonCallback<WVPResult<StreamInfo>> callback) {
        JTDevice device = jt1078Service.getDevice(phoneNumber);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "设备不存在");
        }
        jt1078Template.checkTerminalStatus(phoneNumber);
        JTChannel channel = jt1078Service.getChannel(device.getId(), channelId);
        if (channel == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "通道不存在");
        }
        play(device, channel, type, callback);
    }

    private void play(JTDevice device, JTChannel channel, int type, CommonCallback<WVPResult<StreamInfo>> callback) {
        String phoneNumber = device.getPhoneNumber();
        int channelId = channel.getChannelId();
        String app = "1078";
        String stream = phoneNumber + "_" + channelId;
        // 检查流是否已经存在，存在则返回
        String playKey = VideoManagerConstants.INVITE_INFO_1078_PLAY + phoneNumber + ":" + channelId;
        List<CommonCallback<WVPResult<StreamInfo>>> errorCallbacks = inviteErrorCallbackMap.computeIfAbsent(playKey, k -> new ArrayList<>());
        errorCallbacks.add(callback);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
        if (streamInfo != null) {
            MediaServer mediaServer = streamInfo.getMediaServer();
            if (mediaServer != null) {
                // 查询流是否存在，不存在则删除缓存数据
                MediaInfo mediaInfo = mediaServerService.getMediaInfo(mediaServer, app, streamInfo.getStream());
                if (mediaInfo != null) {
                    log.info("[JT-点播] 点播已经存在，直接返回， phoneNumber： {}， channelId： {}", phoneNumber, channelId);
                    for (CommonCallback<WVPResult<StreamInfo>> errorCallback : errorCallbacks) {
                        errorCallback.run(new WVPResult<>(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo));
                    }
                    return;
                }
            }
            // 清理数据
            redisTemplate.delete(playKey);
        }

        MediaServer mediaServer;
        if (org.springframework.util.ObjectUtils.isEmpty(device.getMediaServerId()) || "auto".equals(device.getMediaServerId())) {
            mediaServer = mediaServerService.getMediaServerForMinimumLoad(null);
        } else {
            mediaServer = mediaServerService.getOne(device.getMediaServerId());
        }
        if (mediaServer == null) {
            for (CommonCallback<WVPResult<StreamInfo>> errorCallback : errorCallbacks) {
                errorCallback.run(new WVPResult<>(InviteErrorCode.FAIL.getCode(), "未找到可用的媒体节点", streamInfo));
            }
            return;
        }
        // 设置hook监听
        Hook hook = Hook.getInstance(HookType.on_media_arrival, app, stream, mediaServer.getId());
        subscribe.addSubscribe(hook, (hookData) -> {
            dynamicTask.stop(playKey);
            log.info("[JT-点播] 点播成功， 手机号： {}， 通道： {}", phoneNumber, channelId);
            // TODO 发送9105 实时音视频传输状态通知， 通知丢包率
            StreamInfo info = onPublishHandler(mediaServer, hookData, phoneNumber, channelId);

            for (CommonCallback<WVPResult<StreamInfo>> errorCallback : errorCallbacks) {
                if (errorCallback == null) {
                    continue;
                }
                errorCallback.run(new WVPResult<>(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), info));
            }
            subscribe.removeSubscribe(hook);
            redisTemplate.opsForValue().set(playKey, info);
            // 截图
            String path = "snap";
            String fileName = phoneNumber + "_" + channelId + ".jpg";
            // 请求截图
            log.info("[请求截图]: " + fileName);
            mediaServerService.getSnap(mediaServer, app, stream, 15, 1, path, fileName);
        });
        // 开启收流端口
        SSRCInfo ssrcInfo = mediaServerService.openJTTServer(mediaServer, stream, null, false, !channel.isHasAudio(), 1);
        if (ssrcInfo == null) {
            stopPlay(phoneNumber, channelId);
            return;
        }

        // 设置超时监听
        dynamicTask.startDelay(playKey, () -> {
            log.info("[JT-点播] 超时， phoneNumber： {}， channelId： {}", phoneNumber, channelId);
            for (CommonCallback<WVPResult<StreamInfo>> errorCallback : errorCallbacks) {
                errorCallback.run(new WVPResult<>(InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getCode(),
                        InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getMsg(), null));
            }
            mediaServerService.closeJTTServer(mediaServer, stream, null);
            subscribe.removeSubscribe(hook);
            stopPlay(phoneNumber, channelId);
        }, userSetting.getPlayTimeout());

        log.info("[JT-点播] phoneNumber： {}， channelId： {}，IP: {}, 端口： {}", phoneNumber, channelId, mediaServer.getSdpIp(), ssrcInfo.getPort());
        J9101 j9101 = new J9101();
        j9101.setChannel(channelId);
        j9101.setIp(mediaServer.getSdpIp());
        j9101.setRate(1);
        j9101.setTcpPort(ssrcInfo.getPort());
        j9101.setUdpPort(ssrcInfo.getPort());
        j9101.setType(type);
        jt1078Template.startLive(phoneNumber, j9101, 6);
    }

    public StreamInfo onPublishHandler(MediaServer mediaServerItem, HookData hookData, String phoneNumber, Integer channelId) {
        StreamInfo streamInfo = mediaServerService.getStreamInfoByAppAndStream(mediaServerItem, "1078", hookData.getStream(), hookData.getMediaInfo(), null);
        streamInfo.setDeviceId(phoneNumber);
        streamInfo.setChannelId(channelId);
        return streamInfo;
    }

    @Override
    public void stopPlay(String phoneNumber, Integer channelId) {
        String playKey = VideoManagerConstants.INVITE_INFO_1078_PLAY + phoneNumber + ":" + channelId;
        dynamicTask.stop(playKey);
        // 清理回调
        List<CommonCallback<WVPResult<StreamInfo>>> generalCallbacks = inviteErrorCallbackMap.get(playKey);
        if (generalCallbacks != null && !generalCallbacks.isEmpty()) {
            for (CommonCallback<WVPResult<StreamInfo>> callback : generalCallbacks) {
                callback.run(new WVPResult<>(InviteErrorCode.ERROR_FOR_FINISH.getCode(), InviteErrorCode.ERROR_FOR_FINISH.getMsg(), null));
            }
        }
        jt1078Template.checkTerminalStatus(phoneNumber);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
        // 发送停止命令
        J9102 j9102 = new J9102();
        j9102.setChannel(channelId);
        j9102.setCommand(0);
        j9102.setCloseType(0);
        j9102.setStreamType(1);
        jt1078Template.stopLive(phoneNumber, j9102, 6);
        log.info("[JT-停止点播] phoneNumber： {}， channelId： {}", phoneNumber, channelId);
        // 删除缓存数据
        if (streamInfo != null) {
            // 关闭rtpServer
            mediaServerService.closeJTTServer(streamInfo.getMediaServer(), streamInfo.getStream(), null);
            redisTemplate.delete(playKey);
        }

    }

    @Override
    public void pausePlay(String phoneNumber, Integer channelId) {
        String playKey = VideoManagerConstants.INVITE_INFO_1078_PLAY + phoneNumber + ":" + channelId;
        dynamicTask.stop(playKey);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
        if (streamInfo == null) {
            log.info("[JT-暂停点播] 未找到点播信息 phoneNumber： {}， channelId： {}", phoneNumber, channelId);
        }
        log.info("[JT-暂停点播] phoneNumber： {}， channelId： {}", phoneNumber, channelId);
        // 发送暂停命令
        J9102 j9102 = new J9102();
        j9102.setChannel(channelId);
        j9102.setCommand(2);
        j9102.setCloseType(0);
        j9102.setStreamType(1);
        jt1078Template.stopLive(phoneNumber, j9102, 6);
    }

    @Override
    public void continueLivePlay(String phoneNumber, Integer channelId) {
        String playKey = VideoManagerConstants.INVITE_INFO_1078_PLAY + phoneNumber + ":" + channelId;
        dynamicTask.stop(playKey);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
        if (streamInfo == null) {
            log.info("[JT-继续点播] 未找到点播信息 phoneNumber： {}， channelId： {}", phoneNumber, channelId);
        }
        log.info("[JT-继续点播] phoneNumber： {}， channelId： {}", phoneNumber, channelId);
        // 发送暂停命令
        J9102 j9102 = new J9102();
        j9102.setChannel(channelId);
        j9102.setCommand(2);
        j9102.setCloseType(0);
        j9102.setStreamType(1);
        jt1078Template.stopLive(phoneNumber, j9102, 6);
    }

    @Override
    public List<J1205.JRecordItem> getRecordList(String phoneNumber, Integer channelId, String startTime, String endTime) {
        log.info("[JT-查询录像列表] phoneNumber： {}， channelId： {}， startTime： {}， endTime： {}"
                , phoneNumber, channelId, startTime, endTime);
        // 发送请求录像列表命令
        J9205 j9205 = new J9205();
        j9205.setChannelId(channelId);
        j9205.setStartTime(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(startTime));
        j9205.setEndTime(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(endTime));
        j9205.setMediaType(0);
        j9205.setStreamType(0);
        j9205.setStorageType(0);
        List<J1205.JRecordItem> JRecordItemList = (List<J1205.JRecordItem>) jt1078Template.queryBackTime(phoneNumber, j9205, 20);
        if (JRecordItemList == null || JRecordItemList.isEmpty()) {
            return null;
        }
        log.info("[JT-查询录像列表] phoneNumber： {}， channelId： {}， startTime： {}， endTime： {}, 结果: {}条"
                , phoneNumber, channelId, startTime, endTime, JRecordItemList.size());
        return JRecordItemList;
    }



    @Override
    public void playback(String phoneNumber, Integer channelId, String startTime, String endTime, Integer type,
                         Integer rate, Integer playbackType, Integer playbackSpeed, CommonCallback<WVPResult<StreamInfo>> callback) {
        JTDevice device = jt1078Service.getDevice(phoneNumber);
        if (device == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "设备不存在");
        }
        jt1078Template.checkTerminalStatus(phoneNumber);
        JTChannel channel = jt1078Service.getChannel(device.getId(), channelId);
        if (channel == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "通道不存在");
        }
        playback(device, channel, startTime, endTime, type, rate, playbackType, playbackSpeed, callback);

    }

    /**
     * 回放
     * @param device  设备
     * @param channel 通道
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param type 音视频资源类型：0.音视频 1.音频 2.视频 3.视频或音视频
     * @param rate 码流类型：0.所有码流 1.主码流 2.子码流(如果此通道只传输音频,此字段置0)
     * @param playbackType 回放方式：0.正常回放 1.快进回放 2.关键帧快退回放 3.关键帧播放 4.单帧上传
     * @param playbackSpeed 快进或快退倍数：0.无效 1.1倍 2.2倍 3.4倍 4.8倍 5.16倍 (回放控制为1和2时,此字段内容有效,否则置0)
     * @param callback 结束回调
     */
    private void playback(JTDevice device, JTChannel channel, String startTime, String endTime, Integer type,
                         Integer rate, Integer playbackType, Integer playbackSpeed, CommonCallback<WVPResult<StreamInfo>> callback) {

        String phoneNumber = device.getPhoneNumber();
        Integer channelId = channel.getChannelId();
        log.info("[JT-回放] 回放，设备:{}， 通道： {}， 开始时间： {}， 结束时间： {}， 音视频类型： {}， 码流类型： {}， " +
                "回放方式： {}， 快进或快退倍数： {}", phoneNumber, channelId, startTime, endTime, type, rate, playbackType, playbackSpeed);
        // 检查流是否已经存在，存在则返回
        String playbackKey = VideoManagerConstants.INVITE_INFO_1078_PLAYBACK + phoneNumber + ":" + channelId;
        List<CommonCallback<WVPResult<StreamInfo>>> errorCallbacks = inviteErrorCallbackMap.computeIfAbsent(playbackKey, k -> new ArrayList<>());
        errorCallbacks.add(callback);
        String logInfo = String.format("phoneNumber:%s, channelId:%s, startTime:%s, endTime:%s", phoneNumber, channelId, startTime, endTime);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playbackKey);
        if (streamInfo != null) {

            mediaServerService.closeJTTServer(streamInfo.getMediaServer(), streamInfo.getStream(), null);
            // 清理数据
            redisTemplate.delete(playbackKey);
        }

        String app = "1078";
        String stream = String.format("%s_%s_%s_%s", phoneNumber, channelId,
                DateUtil.yyyy_MM_dd_HH_mm_ssToUrl(startTime), DateUtil.yyyy_MM_dd_HH_mm_ssToUrl(endTime));
        MediaServer mediaServer;
        if (org.springframework.util.ObjectUtils.isEmpty(device.getMediaServerId()) || "auto".equals(device.getMediaServerId())) {
            mediaServer = mediaServerService.getMediaServerForMinimumLoad(null);
        } else {
            mediaServer = mediaServerService.getOne(device.getMediaServerId());
        }
        if (mediaServer == null) {
            for (CommonCallback<WVPResult<StreamInfo>> errorCallback : errorCallbacks) {
                errorCallback.run(new WVPResult<>(InviteErrorCode.FAIL.getCode(), "未找到可用的媒体节点", streamInfo));
            }
            return;
        }
        // 设置hook监听
        Hook hookSubscribe = Hook.getInstance(HookType.on_media_arrival, app, stream, mediaServer.getId());
        subscribe.addSubscribe(hookSubscribe, (hookData) -> {
            dynamicTask.stop(playbackKey);
            log.info("[JT-回放] 回放成功， logInfo： {}", logInfo);
            StreamInfo info = onPublishHandler(mediaServer, hookData, phoneNumber, channelId);

            for (CommonCallback<WVPResult<StreamInfo>> errorCallback : errorCallbacks) {
                if (errorCallback == null) {
                    continue;
                }
                errorCallback.run(new WVPResult<>(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), info));
            }
            subscribe.removeSubscribe(hookSubscribe);
            redisTemplate.opsForValue().set(playbackKey, info);
        });
        // 设置超时监听
        dynamicTask.startDelay(playbackKey, () -> {
            log.info("[JT-回放] 回放超时， logInfo： {}", logInfo);
            for (CommonCallback<WVPResult<StreamInfo>> errorCallback : errorCallbacks) {
                errorCallback.run(new WVPResult<>(InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getCode(),
                        InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getMsg(), null));
            }
            mediaServerService.closeJTTServer(mediaServer, stream, null);
            subscribe.removeSubscribe(hookSubscribe);
        }, userSetting.getPlayTimeout());

        // 开启收流端口
        SSRCInfo ssrcInfo = mediaServerService.openJTTServer(mediaServer, stream, null, false, !channel.isHasAudio(), 1);
        log.info("[JT-回放] logInfo： {}， 端口： {}", logInfo, ssrcInfo.getPort());
        J9201 j9201 = new J9201();
        j9201.setChannel(channelId);
        j9201.setIp(mediaServer.getSdpIp());
        if (rate != null) {
            j9201.setRate(rate);
        }
        if (playbackType != null) {
            j9201.setPlaybackType(playbackType);
        }
        if (playbackSpeed != null) {
            j9201.setPlaybackSpeed(playbackSpeed);
        }

        j9201.setTcpPort(ssrcInfo.getPort());
        j9201.setUdpPort(ssrcInfo.getPort());
        j9201.setType(type);
        j9201.setStartTime(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(startTime));
        j9201.setEndTime(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(endTime));
        jt1078Template.startBackLive(phoneNumber, j9201, 20);

    }

    @Override
    public void playbackControl(String phoneNumber, Integer channelId, Integer command, Integer playbackSpeed, String time) {
        String playKey = VideoManagerConstants.INVITE_INFO_1078_PLAYBACK + phoneNumber + ":" + channelId;
        dynamicTask.stop(playKey);
        if (command == 2) {
            log.info("[JT-停止回放] phoneNumber： {}， channelId： {}， command： {}， playbackSpeed： {}， time： {}",
                    phoneNumber, channelId, command, playbackSpeed, time);
            // 结束回放
            StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
            // 删除缓存数据
            if (streamInfo != null) {
                // 关闭rtpServer
                mediaServerService.closeJTTServer(streamInfo.getMediaServer(), streamInfo.getStream(), null);
            }
            // 清理回调
            List<CommonCallback<WVPResult<StreamInfo>>> generalCallbacks = inviteErrorCallbackMap.get(playKey);
            if (generalCallbacks != null && !generalCallbacks.isEmpty()) {
                for (CommonCallback<WVPResult<StreamInfo>> callback : generalCallbacks) {
                    if (callback == null) {
                        continue;
                    }
                    callback.run(new WVPResult<>(InviteErrorCode.ERROR_FOR_FINISH.getCode(), InviteErrorCode.ERROR_FOR_FINISH.getMsg(), null));
                }
            }
        }else {
            log.info("[JT-回放控制] phoneNumber： {}， channelId： {}， command： {}， playbackSpeed： {}， time： {}",
                    phoneNumber, channelId, command, playbackSpeed, time);
        }
        // 发送停止命令
        J9202 j9202 = new J9202();
        j9202.setChannel(channelId);
        j9202.setPlaybackType(command);

        if (playbackSpeed != null) {
            j9202.setPlaybackSpeed(playbackSpeed);

        }
        if (!ObjectUtils.isEmpty(time)) {
            j9202.setPlaybackTime(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(time));
        }
        jt1078Template.controlBackLive(phoneNumber, j9202, 4);
    }

    @Override
    public void stopPlayback(String phoneNumber, Integer channelId) {
        playbackControl(phoneNumber, channelId, 2, null, null);
    }

    /**
     * 监听发流停止
     */
    @EventListener
    public void onApplicationEvent(MediaSendRtpStoppedEvent event) {

        List<SendRtpInfo> sendRtpInfos = sendRtpServerService.queryByStream(event.getStream());
        if (sendRtpInfos.isEmpty()) {
            return;
        }
        for (SendRtpInfo sendRtpInfo : sendRtpInfos) {
            if (!sendRtpInfo.isOnlyAudio() || ObjectUtils.isEmpty(sendRtpInfo.getChannelId())) {
                continue;
            }
            if (!sendRtpInfo.getSsrc().contains("_")) {
                continue;
            }
            sendRtpServerService.delete(sendRtpInfo);
            String playKey = VideoManagerConstants.INVITE_INFO_1078_TALK + sendRtpInfo.getApp() + ":" + sendRtpInfo.getStream();
            redisTemplate.delete(playKey);
        }
    }


    @Override
    public StreamInfo startTalk(String phoneNumber, Integer channelId) {
        // 检查流是否已经存在，存在则返回
        String playKey = VideoManagerConstants.INVITE_INFO_1078_TALK + phoneNumber + ":" + channelId;
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);

        if (streamInfo != null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "对讲进行中");
        }

        JTDevice device = jt1078Service.getDevice(phoneNumber);
        Assert.notNull(device, "部标设备不存在");

        String stream = "jt_" + phoneNumber + "_" + channelId + "_talk";

        MediaServer mediaServer;
        if (org.springframework.util.ObjectUtils.isEmpty(device.getMediaServerId()) || "auto".equals(device.getMediaServerId())) {
            mediaServer = mediaServerService.getMediaServerForMinimumLoad(null);
        } else {
            mediaServer = mediaServerService.getOne(device.getMediaServerId());
        }

        // 检查待发送的流是否存在，
        MediaInfo mediaInfo = mediaServerService.getMediaInfo(mediaServer, talkApp, stream);
        Assert.isNull(mediaInfo, "对讲已经存在");
        return mediaServerService.getStreamInfoByAppAndStream(mediaServer, talkApp, stream, null, null, null, false);

    }
    private void sendTalk(JTDevice device, Integer channelId, MediaServer mediaServer, String app, String stream) {
        // 检查待发送的流是否存在，
        MediaInfo mediaInfo = mediaServerService.getMediaInfo(mediaServer, app, stream);
        if (mediaInfo == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), app + "/" + stream + "流不存在");
        }

        String phoneNumber = device.getPhoneNumber();

        // 开启收流端口, zlm发送1078的rtp流需要将ssrc字段设置为 imei_channel格式
        String ssrc = device.getPhoneNumber() + "_" + channelId;
        SendRtpInfo sendRtpInfo = sendRtpServerService.createSendRtpInfo(mediaServer, null, null, ssrc, phoneNumber, talkApp, stream, channelId, true, false);
        sendRtpInfo.setTcpActive(true);
        sendRtpInfo.setUsePs(false);
        sendRtpInfo.setOnlyAudio(true);
        sendRtpInfo.setReceiveStream(stream + "_talk");

        // 设置hook监听
        Hook hook = Hook.getInstance(HookType.on_media_arrival, "1078", sendRtpInfo.getReceiveStream(), mediaServer.getId());
        subscribe.addSubscribe(hook, (hookData) -> {
            log.info("[JT-对讲] 对讲连接建立， phoneNumber： {}， channelId： {}", phoneNumber, channelId);
            subscribe.removeSubscribe(hook);
            // 存储发流信息
            sendRtpServerService.update(sendRtpInfo);
        });
        Hook hookForDeparture = Hook.getInstance(HookType.on_media_departure, app, stream, mediaServer.getId());
        subscribe.addSubscribe(hookForDeparture, (hookData) -> {
            log.info("[JT-对讲] 对讲时源流注销， app: {}. stream: {}, phoneNumber： {}， channelId： {}", app, stream, phoneNumber, channelId);
            stopTalk(phoneNumber, channelId);
        });

        Integer localPort = mediaServerService.startSendRtpPassive(mediaServer, sendRtpInfo, userSetting.getPlayTimeout());

        log.info("[JT-对讲] phoneNumber： {}， channelId： {}， 收发端口： {}， app: {}, stream: {}",
                phoneNumber, channelId, localPort, app, stream);
        J9101 j9101 = new J9101();
        j9101.setChannel(channelId);
        j9101.setIp(mediaServer.getSdpIp());
        j9101.setRate(1);
        j9101.setTcpPort(sendRtpInfo.getLocalPort());
        j9101.setUdpPort(sendRtpInfo.getLocalPort());
        j9101.setType(4);
        jt1078Template.startLive(phoneNumber, j9101, 6);

        log.info("[JT-对讲] 对讲消息下发成功， phoneNumber： {}， channelId： {}", phoneNumber, channelId);
        // 存储发流信息
//        sendRtpServerService.update(sendRtpInfo);
    }

    @Override
    public void stopTalk(String phoneNumber, Integer channelId) {
        String playKey = VideoManagerConstants.INVITE_INFO_1078_TALK + phoneNumber + ":" + channelId;
        dynamicTask.stop(playKey);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
        // 发送停止命令
        J9102 j9102 = new J9102();
        j9102.setChannel(channelId);
        j9102.setCommand(4);
        j9102.setCloseType(0);
        j9102.setStreamType(1);
        jt1078Template.stopLive(phoneNumber, j9102, 6);
        log.info("[JT-停止对讲] phoneNumber： {}， channelId： {}", phoneNumber, channelId);
        // 删除缓存数据
        if (streamInfo != null) {
            redisTemplate.delete(playKey);
            // 关闭rtpServer
            mediaServerService.closeJTTServer(streamInfo.getMediaServer(), streamInfo.getStream(), null);
        }
        // 清理回调
        List<CommonCallback<WVPResult<StreamInfo>>> generalCallbacks = inviteErrorCallbackMap.get(playKey);
        if (generalCallbacks != null && !generalCallbacks.isEmpty()) {
            for (CommonCallback<WVPResult<StreamInfo>> callback : generalCallbacks) {
                callback.run(new WVPResult<>(InviteErrorCode.ERROR_FOR_FINISH.getCode(), InviteErrorCode.ERROR_FOR_FINISH.getMsg(), null));
            }
        }
    }

    @Override
    public void start(Integer channelId, Boolean record, ErrorCallback<StreamInfo> callback) {
        JTChannel channel = jt1078Service.getChannelByDbId(channelId);
        Assert.notNull(channel, "通道不存在");
        JTDevice device = jt1078Service.getDeviceById(channel.getDataDeviceId());
        Assert.notNull(device, "设备不存在");
        jt1078Template.checkTerminalStatus(device.getPhoneNumber());
        play(device, channel, 0,
                result -> callback.run(result.getCode(), result.getMsg(), result.getData()));
    }

    @Override
    public void stop(Integer channelId) {
        JTChannel channel = jt1078Service.getChannelByDbId(channelId);
        Assert.notNull(channel, "通道不存在");
        JTDevice device = jt1078Service.getDeviceById(channel.getDataDeviceId());
        Assert.notNull(device, "设备不存在");
        stopPlay(device.getPhoneNumber(), channel.getChannelId());
    }

    @Override
    public void playBack(Integer channelId, Long startTime, Long stopTime, ErrorCallback<StreamInfo> callback) {
        if (startTime == null || stopTime == null) {
            throw new PlayException(Response.BAD_REQUEST, "bad request");
        }
        JTChannel channel = jt1078Service.getChannelByDbId(channelId);
        Assert.notNull(channel, "通道不存在");
        JTDevice device = jt1078Service.getDeviceById(channel.getDataDeviceId());
        Assert.notNull(device, "设备不存在");
        jt1078Template.checkTerminalStatus(device.getPhoneNumber());
        String startTimeStr = DateUtil.timestampTo_yyyy_MM_dd_HH_mm_ss(startTime);
        String stopTimeStr = DateUtil.timestampTo_yyyy_MM_dd_HH_mm_ss(stopTime);
        playback(device, channel, startTimeStr, stopTimeStr, 0, 1, 0, 0,
                result -> callback.run(result.getCode(), result.getMsg(), result.getData()));
    }
}
