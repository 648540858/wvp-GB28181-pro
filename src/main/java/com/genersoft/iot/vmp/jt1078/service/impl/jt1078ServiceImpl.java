package com.genersoft.iot.vmp.jt1078.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.GeneralCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.ftpServer.FtpSetting;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.jt1078.bean.*;
import com.genersoft.iot.vmp.jt1078.bean.common.ConfigAttribute;
import com.genersoft.iot.vmp.jt1078.cmd.JT1078Template;
import com.genersoft.iot.vmp.jt1078.dao.JTChannelMapper;
import com.genersoft.iot.vmp.jt1078.dao.JTTerminalMapper;
import com.genersoft.iot.vmp.jt1078.event.CallbackManager;
import com.genersoft.iot.vmp.jt1078.event.FtpUploadEvent;
import com.genersoft.iot.vmp.jt1078.proc.request.J1205;
import com.genersoft.iot.vmp.jt1078.proc.response.*;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.hook.Hook;
import com.genersoft.iot.vmp.media.event.hook.HookData;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.hook.HookType;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaSendRtpStoppedEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class jt1078ServiceImpl implements Ijt1078Service {

    private final static Logger logger = LoggerFactory.getLogger(jt1078ServiceImpl.class);

    @Autowired
    private JTTerminalMapper jtDeviceMapper;

    @Autowired
    private JTChannelMapper jtChannelMapper;

    @Autowired
    private JT1078Template jt1078Template;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private HookSubscribe subscribe;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private IMediaService mediaService;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private CallbackManager callbackManager;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private FtpSetting ftpSetting;


    @Override
    public JTDevice getDevice(String phoneNumber) {
        return jtDeviceMapper.getDevice(phoneNumber);
    }

    @Override
    public void updateDevice(JTDevice device) {
        device.setUpdateTime(DateUtil.getNow());
        jtDeviceMapper.updateDevice(device);
    }

    @Override
    public PageInfo<JTDevice> getDeviceList(int page, int count, String query, Boolean online) {
        PageHelper.startPage(page, count);
        List<JTDevice> all = jtDeviceMapper.getDeviceList(query, online);
        return new PageInfo<>(all);
    }

    @Override
    public void addDevice(JTDevice device) {
        JTDevice deviceInDb = jtDeviceMapper.getDevice(device.getPhoneNumber());
        if (deviceInDb != null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "设备" + device.getPhoneNumber() + "已存在");
        }
        device.setCreateTime(DateUtil.getNow());
        device.setUpdateTime(DateUtil.getNow());
        jtDeviceMapper.addDevice(device);
    }

    @Override
    public void deleteDeviceByPhoneNumber(String phoneNumber) {
        jtDeviceMapper.deleteDeviceByPhoneNumber(phoneNumber);
    }

    @Override
    public void updateDeviceStatus(boolean connected, String phoneNumber) {
        jtDeviceMapper.updateDeviceStatus(connected, phoneNumber);
    }

    private final Map<String, List<GeneralCallback<StreamInfo>>> inviteErrorCallbackMap = new ConcurrentHashMap<>();

    @Override
    public void play(String phoneNumber, String channelId, int type, GeneralCallback<StreamInfo> callback) {

        // 检查流是否已经存在，存在则返回
        String playKey = VideoManagerConstants.INVITE_INFO_1078_PLAY + phoneNumber + ":" + channelId;
        List<GeneralCallback<StreamInfo>> errorCallbacks = inviteErrorCallbackMap.computeIfAbsent(playKey, k -> new ArrayList<>());
        errorCallbacks.add(callback);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
        if (streamInfo != null) {
            String mediaServerId = streamInfo.getMediaServerId();
            MediaServer mediaServer = mediaServerService.getOne(mediaServerId);
            if (mediaServer != null) {
                // 查询流是否存在，不存在则删除缓存数据
                JSONObject mediaInfo = zlmresTfulUtils.getMediaInfo(mediaServer, "rtp", "rtsp", streamInfo.getStream());
                if (mediaInfo != null && mediaInfo.getInteger("code") == 0) {
                    Boolean online = mediaInfo.getBoolean("online");
                    if (online != null && online) {
                        logger.info("[1078-点播] 点播已经存在，直接返回， phoneNumber： {}， channelId： {}", phoneNumber, channelId);
                        for (GeneralCallback<StreamInfo> errorCallback : errorCallbacks) {
                            errorCallback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
                        }
                        return;
                    }
                }
            }
            // 清理数据
            redisTemplate.delete(playKey);
        }
        String stream = phoneNumber + "_" + channelId;
        MediaServer mediaServer = mediaServerService.getMediaServerForMinimumLoad(null);
        if (mediaServer == null) {
            for (GeneralCallback<StreamInfo> errorCallback : errorCallbacks) {
                errorCallback.run(InviteErrorCode.FAIL.getCode(), "未找到可用的媒体节点", streamInfo);
            }
            return;
        }
        // 设置hook监听
        Hook hook = Hook.getInstance(HookType.on_media_arrival, "rtp", stream, mediaServer.getId());
        subscribe.addSubscribe(hook, (hookData) -> {
            dynamicTask.stop(playKey);
            logger.info("[1078-点播] 点播成功， phoneNumber： {}， channelId： {}", phoneNumber, channelId);
            // TODO 发送9105 实时音视频传输状态通知， 通知丢包率
            StreamInfo info = onPublishHandler(mediaServer, hookData, phoneNumber, channelId);

            for (GeneralCallback<StreamInfo> errorCallback : errorCallbacks) {
                errorCallback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), info);
            }
            subscribe.removeSubscribe(hook);
            redisTemplate.opsForValue().set(playKey, info);
        });
        // 设置超时监听
        dynamicTask.startDelay(playKey, () -> {
            logger.info("[1078-点播] 超时， phoneNumber： {}， channelId： {}", phoneNumber, channelId);
            for (GeneralCallback<StreamInfo> errorCallback : errorCallbacks) {
                errorCallback.run(InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getCode(),
                        InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getMsg(), null);
            }

        }, userSetting.getPlayTimeout());

        // 开启收流端口
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(mediaServer, stream, null, false, false, 0, false, false, false, 1);
        logger.info("[1078-点播] phoneNumber： {}， channelId： {}， 端口： {}", phoneNumber, channelId, ssrcInfo.getPort());
        J9101 j9101 = new J9101();
        j9101.setChannel(Integer.valueOf(channelId));
        j9101.setIp(mediaServer.getSdpIp());
        j9101.setRate(1);
        j9101.setTcpPort(ssrcInfo.getPort());
        j9101.setUdpPort(ssrcInfo.getPort());
        j9101.setType(type);
        Object s = jt1078Template.startLive(phoneNumber, j9101, 6);
        System.out.println("ssss=== " + s);

    }

    public StreamInfo onPublishHandler(MediaServer mediaServerItem, HookData hookData, String phoneNumber, String channelId) {
        StreamInfo streamInfo = mediaServerService.getStreamInfoByAppAndStream(mediaServerItem, "rtp", hookData.getStream(), hookData.getMediaInfo(), null);
        streamInfo.setDeviceID(phoneNumber);
        streamInfo.setChannelId(channelId);
        return streamInfo;
    }

    @Override
    public void stopPlay(String phoneNumber, String channelId) {
        String playKey = VideoManagerConstants.INVITE_INFO_1078_PLAY + phoneNumber + ":" + channelId;
        dynamicTask.stop(playKey);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
        // 发送停止命令
        J9102 j9102 = new J9102();
        j9102.setChannel(Integer.valueOf(channelId));
        j9102.setCommand(0);
        j9102.setCloseType(0);
        j9102.setStreamType(1);
        jt1078Template.stopLive(phoneNumber, j9102, 6);
        logger.info("[1078-停止点播] phoneNumber： {}， channelId： {}", phoneNumber, channelId);
        // 删除缓存数据
        if (streamInfo != null) {
            // 关闭rtpServer
            mediaServerService.closeRTPServer(streamInfo.getMediaServerId(), streamInfo.getStream());
        }
        // 清理回调
        List<GeneralCallback<StreamInfo>> generalCallbacks = inviteErrorCallbackMap.get(playKey);
        if (generalCallbacks != null && !generalCallbacks.isEmpty()) {
            for (GeneralCallback<StreamInfo> callback : generalCallbacks) {
                callback.run(InviteErrorCode.ERROR_FOR_FINISH.getCode(), InviteErrorCode.ERROR_FOR_FINISH.getMsg(), null);
            }
        }
    }

    @Override
    public void pausePlay(String phoneNumber, String channelId) {
        String playKey = VideoManagerConstants.INVITE_INFO_1078_PLAY + phoneNumber + ":" + channelId;
        dynamicTask.stop(playKey);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
        if (streamInfo == null) {
            logger.info("[1078-暂停点播] 未找到点播信息 phoneNumber： {}， channelId： {}", phoneNumber, channelId);
        }
        logger.info("[1078-暂停点播] phoneNumber： {}， channelId： {}", phoneNumber, channelId);
        // 发送暂停命令
        J9102 j9102 = new J9102();
        j9102.setChannel(Integer.valueOf(channelId));
        j9102.setCommand(2);
        j9102.setCloseType(0);
        j9102.setStreamType(1);
        jt1078Template.stopLive(phoneNumber, j9102, 6);
    }

    @Override
    public void continueLivePlay(String phoneNumber, String channelId) {
        String playKey = VideoManagerConstants.INVITE_INFO_1078_PLAY + phoneNumber + ":" + channelId;
        dynamicTask.stop(playKey);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
        if (streamInfo == null) {
            logger.info("[1078-继续点播] 未找到点播信息 phoneNumber： {}， channelId： {}", phoneNumber, channelId);
        }
        logger.info("[1078-继续点播] phoneNumber： {}， channelId： {}", phoneNumber, channelId);
        // 发送暂停命令
        J9102 j9102 = new J9102();
        j9102.setChannel(Integer.valueOf(channelId));
        j9102.setCommand(2);
        j9102.setCloseType(0);
        j9102.setStreamType(1);
        jt1078Template.stopLive(phoneNumber, j9102, 6);
    }

    @Override
    public List<J1205.JRecordItem> getRecordList(String phoneNumber, String channelId, String startTime, String endTime) {
        logger.info("[1078-查询录像列表] phoneNumber： {}， channelId： {}， startTime： {}， endTime： {}"
                , phoneNumber, channelId, startTime, endTime);
        // 发送请求录像列表命令
        J9205 j9205 = new J9205();
        j9205.setChannelId(Integer.parseInt(channelId));
        j9205.setStartTime(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(startTime));
        j9205.setEndTime(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(endTime));
        j9205.setMediaType(0);
        j9205.setStreamType(0);
        j9205.setStorageType(0);
        List<J1205.JRecordItem> JRecordItemList = (List<J1205.JRecordItem>) jt1078Template.queryBackTime(phoneNumber, j9205, 20);
        if (JRecordItemList == null || JRecordItemList.isEmpty()) {
            return null;
        }
        logger.info("[1078-查询录像列表] phoneNumber： {}， channelId： {}， startTime： {}， endTime： {}, 结果: {}条"
                , phoneNumber, channelId, startTime, endTime, JRecordItemList.size());
        return JRecordItemList;
    }

    @Override
    public void playback(String phoneNumber, String channelId, String startTime, String endTime, Integer type,
                         Integer rate, Integer playbackType, Integer playbackSpeed, GeneralCallback<StreamInfo> callback) {
        logger.info("[1078-回放] 回放，设备:{}， 通道： {}， 开始时间： {}， 结束时间： {}， 音视频类型： {}， 码流类型： {}， " +
                "回放方式： {}， 快进或快退倍数： {}", phoneNumber, channelId, startTime, endTime, type, rate, playbackType, playbackSpeed);
        // 检查流是否已经存在，存在则返回
        String playbackKey = VideoManagerConstants.INVITE_INFO_1078_PLAYBACK + phoneNumber + ":" + channelId;
        List<GeneralCallback<StreamInfo>> errorCallbacks = inviteErrorCallbackMap.computeIfAbsent(playbackKey, k -> new ArrayList<>());
        errorCallbacks.add(callback);
        String logInfo = String.format("phoneNumber:%s, channelId:%s, startTime:%s, endTime:%s", phoneNumber, channelId, startTime, endTime);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playbackKey);
        if (streamInfo != null) {
            String mediaServerId = streamInfo.getMediaServerId();
            MediaServer mediaServer = mediaServerService.getOne(mediaServerId);
            if (mediaServer != null) {
                // 查询流是否存在，不存在则删除缓存数据
                JSONObject mediaInfo = zlmresTfulUtils.getMediaInfo(mediaServer, "rtp", "rtsp", streamInfo.getStream());
                if (mediaInfo != null && mediaInfo.getInteger("code") == 0) {
                    Boolean online = mediaInfo.getBoolean("online");
                    if (online != null && online) {
                        logger.info("[1078-回放] 回放已经存在，直接返回， logInfo： {}", logInfo);
                        for (GeneralCallback<StreamInfo> errorCallback : errorCallbacks) {
                            errorCallback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
                        }
                        return;
                    }
                }
            }
            // 清理数据
            redisTemplate.delete(playbackKey);
        }
        String startTimeParam = DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(startTime);
        String endTimeParam = DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(endTime);
        String stream = phoneNumber + "_" + channelId + "_" + startTimeParam + "_" + endTimeParam;
        MediaServer mediaServer = mediaServerService.getMediaServerForMinimumLoad(null);
        if (mediaServer == null) {
            for (GeneralCallback<StreamInfo> errorCallback : errorCallbacks) {
                errorCallback.run(InviteErrorCode.FAIL.getCode(), "未找到可用的媒体节点", streamInfo);
            }
            return;
        }
        // 设置hook监听
        Hook hookSubscribe = Hook.getInstance(HookType.on_media_arrival, "rtp", stream, mediaServer.getId());
        subscribe.addSubscribe(hookSubscribe, (hookData) -> {
            dynamicTask.stop(playbackKey);
            logger.info("[1078-回放] 回放成功， logInfo： {}", logInfo);
            StreamInfo info = onPublishHandler(mediaServer, hookData, phoneNumber, channelId);

            for (GeneralCallback<StreamInfo> errorCallback : errorCallbacks) {
                errorCallback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), info);
            }
            subscribe.removeSubscribe(hookSubscribe);
            redisTemplate.opsForValue().set(playbackKey, info);
        });
        // 设置超时监听
        dynamicTask.startDelay(playbackKey, () -> {
            logger.info("[1078-回放] 回放超时， logInfo： {}", logInfo);
            for (GeneralCallback<StreamInfo> errorCallback : errorCallbacks) {
                errorCallback.run(InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getCode(),
                        InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getMsg(), null);
            }

        }, userSetting.getPlayTimeout());

        // 开启收流端口
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(mediaServer, stream, null, false, false, 0, false, false, false, 1);
        logger.info("[1078-回放] logInfo： {}， 端口： {}", logInfo, ssrcInfo.getPort());
        J9201 j9201 = new J9201();
        j9201.setChannel(Integer.parseInt(channelId));
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
    public void playbackControl(String phoneNumber, String channelId, Integer command, Integer playbackSpeed, String time) {
        logger.info("[1078-回放控制] phoneNumber： {}， channelId： {}， command： {}， playbackSpeed： {}， time： {}",
                phoneNumber, channelId, command, playbackSpeed, time);
        String playKey = VideoManagerConstants.INVITE_INFO_1078_PLAYBACK + phoneNumber + ":" + channelId;
        dynamicTask.stop(playKey);
        if (command == 2) {
            // 结束回放
            StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
            // 删除缓存数据
            if (streamInfo != null) {
                // 关闭rtpServer
                mediaServerService.closeRTPServer(streamInfo.getMediaServerId(), streamInfo.getStream());
            }
            // 清理回调
            List<GeneralCallback<StreamInfo>> generalCallbacks = inviteErrorCallbackMap.get(playKey);
            if (generalCallbacks != null && !generalCallbacks.isEmpty()) {
                for (GeneralCallback<StreamInfo> callback : generalCallbacks) {
                    callback.run(InviteErrorCode.ERROR_FOR_FINISH.getCode(), InviteErrorCode.ERROR_FOR_FINISH.getMsg(), null);
                }
            }
        }
        // 发送停止命令
        J9202 j9202 = new J9202();
        j9202.setChannel(Integer.parseInt(channelId));
        j9202.setPlaybackType(command);
        if (playbackSpeed != null) {
            j9202.setPlaybackSpeed(playbackSpeed);
        }
        if (!ObjectUtils.isEmpty(time)) {
            j9202.setPlaybackTime(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(time));
        }
        jt1078Template.controlBackLive(phoneNumber, j9202, 6);
    }

    @Override
    public void stopPlayback(String phoneNumber, String channelId) {
        playbackControl(phoneNumber, channelId, 2, null, String.valueOf(0));
    }

    private Map<String, GeneralCallback<String>> fileUploadMap = new ConcurrentHashMap<>();

    @EventListener
    public void onApplicationEvent(FtpUploadEvent event) {
        if (fileUploadMap.isEmpty()) {
            return;
        }
        fileUploadMap.keySet().forEach(key -> {
            if (!event.getFileName().contains(key)) {
                return;
            }
            GeneralCallback<String> callback = fileUploadMap.get(key);
            if (callback != null) {
                callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), event.getFileName());
                fileUploadMap.remove(key);
            }
        });
    }

    @Override
    public void recordDownload(String phoneNumber, String channelId, String startTime, String endTime, Integer type, Integer rate, GeneralCallback<String> fileCallback) {
        String filePath = UUID.randomUUID().toString();
        fileUploadMap.put(filePath, fileCallback);
        dynamicTask.startDelay(filePath, ()->{
            fileUploadMap.remove(filePath);
        }, 2*60*60*1000);
        logger.info("[1078-录像] 下载，设备:{}， 通道： {}， 开始时间： {}， 结束时间： {}，等待上传文件路径： {} ",
                phoneNumber, channelId, startTime, endTime, filePath);
        // 发送停止命令
        J9206 j92026 = new J9206();
        j92026.setChannelId(Integer.parseInt(channelId));
        j92026.setStartTime(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(startTime));
        j92026.setEndTime(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(endTime));
        j92026.setServerIp(ftpSetting.getIp());
        j92026.setPort(ftpSetting.getPort());
        j92026.setUsername(ftpSetting.getUsername());
        j92026.setPassword(ftpSetting.getPassword());
        j92026.setPath(filePath);

        if (type != null) {
            j92026.setMediaType(type);
        }
        if (rate != null) {
            j92026.setStreamType(rate);
        }
        jt1078Template.fileUpload(phoneNumber, j92026, 7200);
    }

    @Override
    public void ptzControl(String phoneNumber, String channelId, String command, int speed) {
        // 发送停止命令

        switch (command) {
            case "left":
            case "right":
            case "up":
            case "down":
            case "stop":
                J9301 j9301 = new J9301();
                j9301.setChannel(Integer.parseInt(channelId));
                switch (command) {
                    case "left":
                        j9301.setDirection(3);
                        j9301.setSpeed(speed);
                        break;
                    case "right":
                        j9301.setDirection(4);
                        j9301.setSpeed(speed);
                        break;
                    case "up":
                        j9301.setDirection(1);
                        j9301.setSpeed(speed);
                        break;
                    case "down":
                        j9301.setDirection(2);
                        j9301.setSpeed(speed);
                        break;
                    case "stop":
                        j9301.setDirection(0);
                        j9301.setSpeed(0);
                        break;
                }
                jt1078Template.ptzRotate(phoneNumber, j9301, 6);
                break;

            case "zoomin":
            case "zoomout":
                J9306 j9306 = new J9306();
                j9306.setChannel(Integer.parseInt(channelId));
                if (command.equals("zoomin")) {
                    j9306.setZoom(0);
                } else {
                    j9306.setZoom(1);
                }
                jt1078Template.ptzZoom(phoneNumber, j9306, 6);
                break;
            case "irisin":
            case "irisout":
                J9303 j9303 = new J9303();
                j9303.setChannel(Integer.parseInt(channelId));
                if (command.equals("irisin")) {
                    j9303.setIris(0);
                } else {
                    j9303.setIris(1);
                }
                jt1078Template.ptzIris(phoneNumber, j9303, 6);
                break;
            case "focusnear":
            case "focusfar":
                J9302 j9302 = new J9302();
                j9302.setChannel(Integer.parseInt(channelId));
                if (command.equals("focusfar")) {
                    j9302.setFocalDirection(0);
                } else {
                    j9302.setFocalDirection(1);
                }
                jt1078Template.ptzFocal(phoneNumber, j9302, 6);
                break;

        }
    }

    @Override
    public void supplementaryLight(String phoneNumber, String channelId, String command) {
        J9305 j9305 = new J9305();
        j9305.setChannel(Integer.parseInt(channelId));
        if (command.equalsIgnoreCase("on")) {
            j9305.setOn(1);
        } else {
            j9305.setOn(0);
        }
        jt1078Template.ptzSupplementaryLight(phoneNumber, j9305, 6);
    }

    @Override
    public void wiper(String phoneNumber, String channelId, String command) {
        J9304 j9304 = new J9304();
        j9304.setChannel(Integer.parseInt(channelId));
        if (command.equalsIgnoreCase("on")) {
            j9304.setOn(1);
        } else {
            j9304.setOn(0);
        }
        jt1078Template.ptzWiper(phoneNumber, j9304, 6);
    }

    @Override
    public JTDeviceConfig queryConfig(String phoneNumber, String[] params, GeneralCallback<StreamInfo> callback) {
        if (phoneNumber == null) {
            return null;
        }
        if (params == null || params.length == 0) {
            J8104 j8104 = new J8104();
            return (JTDeviceConfig) jt1078Template.getDeviceConfig(phoneNumber, j8104, 20);
        } else {
            long[] paramBytes = new long[params.length];
            for (int i = 0; i < params.length; i++) {
                try {
                    Field field = JTDeviceConfig.class.getDeclaredField(params[i]);
                    if (field.isAnnotationPresent(ConfigAttribute.class)) {
                        ConfigAttribute configAttribute = field.getAnnotation(ConfigAttribute.class);
                        long id = configAttribute.id();
                        String description = configAttribute.description();
                        System.out.println(description + ":  " + id);
                        paramBytes[i] = configAttribute.id();
                    }
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }
            }
            J8106 j8106 = new J8106();
            j8106.setParams(paramBytes);
            return (JTDeviceConfig) jt1078Template.getDeviceSpecifyConfig(phoneNumber, j8106, 20);
        }
    }

    @Override
    public void setConfig(String phoneNumber, JTDeviceConfig config) {
        J8103 j8103 = new J8103();
        j8103.setConfig(config);
        jt1078Template.setDeviceSpecifyConfig(phoneNumber, j8103, 6);
    }

    @Override
    public void connectionControl(String phoneNumber, JTDeviceConnectionControl control) {
        J8105 j8105 = new J8105();
        j8105.setConnectionControl(control);
        jt1078Template.deviceControl(phoneNumber, j8105, 6);
    }

    @Override
    public void resetControl(String phoneNumber) {
        J8105 j8105 = new J8105();
        j8105.setReset(true);
        jt1078Template.deviceControl(phoneNumber, j8105, 6);
    }

    @Override
    public void factoryResetControl(String phoneNumber) {
        J8105 j8105 = new J8105();
        j8105.setFactoryReset(true);
        jt1078Template.deviceControl(phoneNumber, j8105, 6);
    }

    @Override
    public JTDeviceAttribute attribute(String phoneNumber) {
        J8107 j8107 = new J8107();
        return (JTDeviceAttribute) jt1078Template.deviceAttribute(phoneNumber, j8107, 20);
    }

    @Override
    public JTPositionBaseInfo queryPositionInfo(String phoneNumber) {
        J8201 j8201 = new J8201();
        return (JTPositionBaseInfo) jt1078Template.queryPositionInfo(phoneNumber, j8201, 20);
    }

    @Override
    public void tempPositionTrackingControl(String phoneNumber, Integer timeInterval, Long validityPeriod) {
        J8202 j8202 = new J8202();
        j8202.setTimeInterval(timeInterval);
        j8202.setValidityPeriod(validityPeriod);
        jt1078Template.tempPositionTrackingControl(phoneNumber, j8202, 20);
    }

    @Override
    public void confirmationAlarmMessage(String phoneNumber, int alarmPackageNo, JTConfirmationAlarmMessageType alarmMessageType) {
        J8203 j8203 = new J8203();
        j8203.setAlarmMessageType(alarmMessageType);
        j8203.setAlarmPackageNo(alarmPackageNo);
        jt1078Template.confirmationAlarmMessage(phoneNumber, j8203, 6);
    }

    @Override
    public int linkDetection(String phoneNumber) {
        J8204 j8204 = new J8204();
        return (int) jt1078Template.linkDetection(phoneNumber, j8204, 6);
    }

    @Override
    public int textMessage(String phoneNumber, JTTextSign sign, int textType, String content) {
        J8300 j8300 = new J8300();
        j8300.setSign(sign);
        j8300.setTextType(textType);
        j8300.setContent(content);
        return (int) jt1078Template.textMessage(phoneNumber, j8300, 6);
    }

    @Override
    public int telephoneCallback(String phoneNumber, Integer sign, String destPhoneNumber) {
        J8400 j8400 = new J8400();
        j8400.setSign(sign);
        j8400.setPhoneNumber(destPhoneNumber);
        return (int) jt1078Template.telephoneCallback(phoneNumber, j8400, 6);
    }

    @Override
    public int setPhoneBook(String phoneNumber, int type, List<JTPhoneBookContact> phoneBookContactList) {
        J8401 j8401 = new J8401();
        j8401.setType(type);
        if (phoneBookContactList != null) {
            j8401.setPhoneBookContactList(phoneBookContactList);
        }
        return (int) jt1078Template.setPhoneBook(phoneNumber, j8401, 6);
    }

    @Override
    public JTPositionBaseInfo controlDoor(String phoneNumber, Boolean open) {
        J8500 j8500 = new J8500();
        JTVehicleControl jtVehicleControl = new JTVehicleControl();
        jtVehicleControl.setControlCarDoor(open ? 1 : 0);
        j8500.setVehicleControl(jtVehicleControl);
        return (JTPositionBaseInfo) jt1078Template.vehicleControl(phoneNumber, j8500, 20);
    }

    @Override
    public int setAreaForCircle(int attribute, String phoneNumber, List<JTCircleArea> circleAreaList) {
        J8600 j8600 = new J8600();
        j8600.setAttribute(attribute);
        j8600.setCircleAreaList(circleAreaList);
        return (int) jt1078Template.setAreaForCircle(phoneNumber, j8600, 20);
    }

    @Override
    public int deleteAreaForCircle(String phoneNumber, List<Long> ids) {
        J8601 j8601 = new J8601();
        j8601.setIdList(ids);
        return (int) jt1078Template.deleteAreaForCircle(phoneNumber, j8601, 20);
    }

    @Override
    public List<JTAreaOrRoute> queryAreaForCircle(String phoneNumber, List<Long> ids) {
        J8608 j8608 = new J8608();
        j8608.setType(1);
        j8608.setIdList(ids);
        return (List<JTAreaOrRoute>) jt1078Template.queryAreaOrRoute(phoneNumber, j8608, 20);
    }

    @Override
    public int setAreaForRectangle(int attribute, String phoneNumber, List<JTRectangleArea> rectangleAreas) {
        J8602 j8602 = new J8602();
        j8602.setAttribute(attribute);
        j8602.setRectangleAreas(rectangleAreas);
        return (int) jt1078Template.setAreaForRectangle(phoneNumber, j8602, 20);
    }

    @Override
    public int deleteAreaForRectangle(String phoneNumber, List<Long> ids) {
        J8603 j8603 = new J8603();
        j8603.setIdList(ids);
        return (int) jt1078Template.deleteAreaForRectangle(phoneNumber, j8603, 20);
    }

    @Override
    public List<JTAreaOrRoute> queryAreaForRectangle(String phoneNumber, List<Long> ids) {
        J8608 j8608 = new J8608();
        j8608.setType(2);
        j8608.setIdList(ids);
        return (List<JTAreaOrRoute>) jt1078Template.queryAreaOrRoute(phoneNumber, j8608, 20);
    }

    @Override
    public int setAreaForPolygon(String phoneNumber, JTPolygonArea polygonArea) {
        J8604 j8604 = new J8604();
        j8604.setPolygonArea(polygonArea);
        return (int) jt1078Template.setAreaForPolygon(phoneNumber, j8604, 20);
    }

    @Override
    public int deleteAreaForPolygon(String phoneNumber, List<Long> ids) {
        J8605 j8605 = new J8605();
        j8605.setIdList(ids);
        return (int) jt1078Template.deleteAreaForPolygon(phoneNumber, j8605, 20);
    }

    @Override
    public List<JTAreaOrRoute> queryAreaForPolygon(String phoneNumber, List<Long> ids) {
        J8608 j8608 = new J8608();
        j8608.setType(3);
        j8608.setIdList(ids);
        return (List<JTAreaOrRoute>) jt1078Template.queryAreaOrRoute(phoneNumber, j8608, 20);
    }

    @Override
    public int setRoute(String phoneNumber, JTRoute route) {
        J8606 j8606 = new J8606();
        j8606.setRoute(route);
        return (int) jt1078Template.setRoute(phoneNumber, j8606, 20);
    }

    @Override
    public int deleteRoute(String phoneNumber, List<Long> ids) {
        J8607 j8607 = new J8607();
        j8607.setIdList(ids);
        return (int) jt1078Template.deleteRoute(phoneNumber, j8607, 20);
    }

    @Override
    public List<JTAreaOrRoute> queryRoute(String phoneNumber, List<Long> ids) {
        J8608 j8608 = new J8608();
        j8608.setType(4);
        j8608.setIdList(ids);
        return (List<JTAreaOrRoute>) jt1078Template.queryAreaOrRoute(phoneNumber, j8608, 20);
    }

    @Override
    public JTDriverInformation queryDriverInformation(String phoneNumber) {
        J8702 j8702 = new J8702();
        return (JTDriverInformation) jt1078Template.queryDriverInformation(phoneNumber, j8702, 20);
    }

    @Override
    public List<Long> shooting(String phoneNumber, JTShootingCommand shootingCommand) {
        J8801 j8801 = new J8801();
        j8801.setCommand(shootingCommand);
        return (List<Long>) jt1078Template.shooting(phoneNumber, j8801, 300);
    }

    @Override
    public List<JTMediaDataInfo> queryMediaData(String phoneNumber, JTQueryMediaDataCommand queryMediaDataCommand) {
        J8802 j8802 = new J8802();
        j8802.setCommand(queryMediaDataCommand);
        return (List<JTMediaDataInfo>) jt1078Template.queryMediaData(phoneNumber, j8802, 300);
    }

    @Override
    public void uploadMediaData(String phoneNumber, JTQueryMediaDataCommand queryMediaDataCommand) {
        J8803 j8803 = new J8803();
        j8803.setCommand(queryMediaDataCommand);
        jt1078Template.uploadMediaData(phoneNumber, j8803, 10);
    }

    @Override
    public void record(String phoneNumber, int command, Integer time, Integer save, Integer samplingRate) {
        J8804 j8804 = new J8804();
        j8804.setCommond(command);
        j8804.setDuration(time);
        j8804.setSave(save);
        j8804.setSamplingRate(samplingRate);
        jt1078Template.record(phoneNumber, j8804, 10);
    }

    @Override
    public void uploadMediaDataForSingle(String phoneNumber, Long mediaId, Integer delete) {
        J8805 j8805 = new J8805();
        j8805.setMediaId(mediaId);
        j8805.setDelete(delete);
        jt1078Template.uploadMediaDataForSingle(phoneNumber, j8805, 10);
    }

    @Override
    public JTMediaAttribute queryMediaAttribute(String phoneNumber) {
        J9003 j9003 = new J9003();
        return (JTMediaAttribute) jt1078Template.queryMediaAttribute(phoneNumber, j9003, 300);
    }

    /**
     * 监听发流停止
     */
    @EventListener
    public void onApplicationEvent(MediaSendRtpStoppedEvent event) {
        List<SendRtpItem> sendRtpItems = redisCatchStorage.querySendRTPServerByStream(event.getStream());
        for (SendRtpItem sendRtpItem : sendRtpItems) {
            if (!sendRtpItem.isOnlyAudio()
                    || ObjectUtils.isEmpty(sendRtpItem.getDeviceId())
                    || ObjectUtils.isEmpty(sendRtpItem.getChannelId())) {
                continue;
            }
            if (!sendRtpItem.getSsrc().contains("_")) {
                continue;
            }
            redisCatchStorage.deleteSendRTPServer(sendRtpItem);
            String playKey = VideoManagerConstants.INVITE_INFO_1078_TALK + sendRtpItem.getDeviceId() + ":" + sendRtpItem.getChannelId();
            redisTemplate.delete(playKey);
        }
    }

    @Override
    public void startTalk(String phoneNumber, String channelId, String app, String stream, String mediaServerId, Boolean onlySend,
                          GeneralCallback<StreamInfo> callback) {
        // 检查流是否已经存在，存在则返回
        String playKey = VideoManagerConstants.INVITE_INFO_1078_TALK + phoneNumber + ":" + channelId;
        List<GeneralCallback<StreamInfo>> errorCallbacks = inviteErrorCallbackMap.computeIfAbsent(playKey, k -> new ArrayList<>());
        errorCallbacks.add(callback);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
        if (streamInfo != null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "对讲进行中");
        }

        String receiveStream = "1078" + "_" + phoneNumber + "_" + channelId;
        MediaServer mediaServer = mediaServerService.getOne(mediaServerId);
        if (mediaServer == null) {
            for (GeneralCallback<StreamInfo> errorCallback : errorCallbacks) {
                errorCallback.run(InviteErrorCode.FAIL.getCode(), "未找到可用的媒体节点", streamInfo);
            }
            return;
        }
        // 检查待发送的流是否存在，
        MediaInfo mediaInfo = mediaServerService.getMediaInfo(mediaServer, app, stream);
        if (mediaInfo == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), app + "/" + stream + "流不存在");
        }
        // 开启收流端口, zlm发送1078的rtp流需要将ssrc字段设置为 imei_channel格式
        String ssrc = phoneNumber + "_" + channelId;
        SendRtpItem sendRtpItem = new SendRtpItem();
        sendRtpItem.setMediaServerId(mediaServerId);
        sendRtpItem.setPort(0);
        sendRtpItem.setSsrc(ssrc);
        sendRtpItem.setDeviceId(phoneNumber);
        sendRtpItem.setChannelId(channelId);
        sendRtpItem.setRtcp(false);
        sendRtpItem.setApp(app);
        sendRtpItem.setStream(stream);
        sendRtpItem.setTcp(true);
        sendRtpItem.setTcpActive(true);
        sendRtpItem.setUsePs(false);
        sendRtpItem.setOnlyAudio(true);
        if (onlySend == null || !onlySend) {
            sendRtpItem.setReceiveStream(receiveStream);
        }
        sendRtpItem.setPlatformId(phoneNumber);
        if (onlySend == null || !onlySend) {
            // 设置hook监听
            Hook hook = Hook.getInstance(HookType.on_media_arrival, "rtp", receiveStream, mediaServer.getId());
            subscribe.addSubscribe(hook, (hookData) -> {
                dynamicTask.stop(playKey);
                logger.info("[1078-对讲] 对讲成功， phoneNumber： {}， channelId： {}", phoneNumber, channelId);
                StreamInfo info = onPublishHandler(mediaServer, hookData, phoneNumber, channelId);

                for (GeneralCallback<StreamInfo> errorCallback : errorCallbacks) {
                    errorCallback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), info);
                }
                subscribe.removeSubscribe(hook);
                redisTemplate.opsForValue().set(playKey, info);
                // 存储发流信息
                redisCatchStorage.updateSendRTPSever(sendRtpItem);
            });
            Hook hookForDeparture = Hook.getInstance(HookType.on_media_departure, "rtp", receiveStream, mediaServer.getId());
            subscribe.addSubscribe(hookForDeparture, (hookData) -> {
                logger.info("[1078-对讲] 对讲时源流注销， app: {}. stream: {}, phoneNumber： {}， channelId： {}", app, stream, phoneNumber, channelId);
                stopTalk(phoneNumber, channelId);
            });
            // 设置超时监听
            dynamicTask.startDelay(playKey, () -> {
                logger.info("[1078-对讲] 超时， phoneNumber： {}， channelId： {}", phoneNumber, channelId);
                for (GeneralCallback<StreamInfo> errorCallback : errorCallbacks) {
                    errorCallback.run(InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getCode(),
                            InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getMsg(), null);
                }

            }, userSetting.getPlayTimeout());
        }

        Integer localPort = mediaServerService.startSendRtpPassive(mediaServer, sendRtpItem, 15000);

        logger.info("[1078-对讲] phoneNumber： {}， channelId： {}， 收发端口： {}， app: {}, stream: {}",
                phoneNumber, channelId, localPort, app, stream);
        J9101 j9101 = new J9101();
        j9101.setChannel(Integer.valueOf(channelId));
        j9101.setIp(mediaServer.getSdpIp());
        j9101.setRate(1);
        j9101.setTcpPort(localPort);
        j9101.setUdpPort(localPort);
        j9101.setType(2);
        jt1078Template.startLive(phoneNumber, j9101, 6);
        if (onlySend != null && onlySend) {
            logger.info("[1078-对讲] 对讲成功， phoneNumber： {}， channelId： {}", phoneNumber, channelId);
            for (GeneralCallback<StreamInfo> errorCallback : errorCallbacks) {
                errorCallback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), null);
            }
            // 存储发流信息
            redisCatchStorage.updateSendRTPSever(sendRtpItem);
        }
    }

    @Override
    public void stopTalk(String phoneNumber, String channelId) {
        String playKey = VideoManagerConstants.INVITE_INFO_1078_TALK + phoneNumber + ":" + channelId;
        dynamicTask.stop(playKey);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
        // 发送停止命令
        J9102 j9102 = new J9102();
        j9102.setChannel(Integer.valueOf(channelId));
        j9102.setCommand(4);
        j9102.setCloseType(0);
        j9102.setStreamType(1);
        jt1078Template.stopLive(phoneNumber, j9102, 6);
        logger.info("[1078-停止对讲] phoneNumber： {}， channelId： {}", phoneNumber, channelId);
        // 删除缓存数据
        if (streamInfo != null) {
            redisTemplate.delete(playKey);
            // 关闭rtpServer
            mediaServerService.closeRTPServer(streamInfo.getMediaServerId(), streamInfo.getStream());
        }
        // 清理回调
        List<GeneralCallback<StreamInfo>> generalCallbacks = inviteErrorCallbackMap.get(playKey);
        if (generalCallbacks != null && !generalCallbacks.isEmpty()) {
            for (GeneralCallback<StreamInfo> callback : generalCallbacks) {
                callback.run(InviteErrorCode.ERROR_FOR_FINISH.getCode(), InviteErrorCode.ERROR_FOR_FINISH.getMsg(), null);
            }
        }
    }

    @Override
    public void changeStreamType(String phoneNumber, String channelId, Integer streamType) {
        String playKey = VideoManagerConstants.INVITE_INFO_1078_PLAY + phoneNumber + ":" + channelId;
        dynamicTask.stop(playKey);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
        if (streamInfo == null) {
            logger.info("[1078-切换码流类型] 未找到点播信息 phoneNumber： {}， channelId： {}, streamType: {}", phoneNumber, channelId, streamType);
        }
        logger.info("[1078-切换码流类型] phoneNumber： {}， channelId： {}, streamType: {}", phoneNumber, channelId, streamType);
        // 发送暂停命令
        J9102 j9102 = new J9102();
        j9102.setChannel(Integer.valueOf(channelId));
        j9102.setCommand(1);
        j9102.setCloseType(0);
        j9102.setStreamType(streamType);
        jt1078Template.stopLive(phoneNumber, j9102, 6);
    }

    @Override
    public PageInfo<JTChannel> getChannelList(int page, int count, int deviceId, String query) {
        PageHelper.startPage(page, count);
        List<JTChannel> all = jtChannelMapper.getAll(deviceId, query);
        return new PageInfo<>(all);
    }

    @Override
    public void updateChannel(JTChannel channel) {
        channel.setUpdateTime(DateUtil.getNow());
        jtChannelMapper.update(channel);
    }

    @Override
    public void addChannel(JTChannel channel) {
        channel.setCreateTime(DateUtil.getNow());
        channel.setUpdateTime(DateUtil.getNow());
        jtChannelMapper.add(channel);
    }

    @Override
    public void deleteChannelById(Integer id) {
        jtChannelMapper.delete(id);
    }
}
