package com.genersoft.iot.vmp.jt1078.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.GeneralCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.jt1078.bean.*;
import com.genersoft.iot.vmp.jt1078.bean.common.ConfigAttribute;
import com.genersoft.iot.vmp.jt1078.cmd.JT1078Template;
import com.genersoft.iot.vmp.jt1078.dao.JTDeviceMapper;
import com.genersoft.iot.vmp.jt1078.event.CallbackManager;
import com.genersoft.iot.vmp.jt1078.proc.request.J1205;
import com.genersoft.iot.vmp.jt1078.proc.response.*;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.util.SSRCUtil;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class jt1078ServiceImpl implements Ijt1078Service {

    private final static Logger logger = LoggerFactory.getLogger(jt1078ServiceImpl.class);

    @Autowired
    private JTDeviceMapper jtDeviceMapper;

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


    @Override
    public JTDevice getDevice(String terminalId) {
        return jtDeviceMapper.getDevice(terminalId);
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
        device.setCreateTime(DateUtil.getNow());
        device.setUpdateTime(DateUtil.getNow());
        jtDeviceMapper.addDevice(device);
    }

    @Override
    public void deleteDeviceByDeviceId(String deviceId) {
        jtDeviceMapper.deleteDeviceByTerminalId(deviceId);
    }

    @Override
    public void updateDeviceStatus(boolean connected, String terminalId) {
        jtDeviceMapper.updateDeviceStatus(connected, terminalId);
    }

    private final Map<String, List<GeneralCallback<StreamInfo>>> inviteErrorCallbackMap = new ConcurrentHashMap<>();

    @Override
    public void play(String deviceId, String channelId, int type, GeneralCallback<StreamInfo> callback) {

        // 检查流是否已经存在，存在则返回
        String playKey = VideoManagerConstants.INVITE_INFO_1078_PLAY + deviceId + ":" + channelId;
        List<GeneralCallback<StreamInfo>> errorCallbacks = inviteErrorCallbackMap.computeIfAbsent(playKey, k -> new ArrayList<>());
        errorCallbacks.add(callback);
        StreamInfo streamInfo = (StreamInfo)redisTemplate.opsForValue().get(playKey);
        if (streamInfo != null) {
            String mediaServerId = streamInfo.getMediaServerId();
            MediaServer mediaServer = mediaServerService.getOne(mediaServerId);
            if (mediaServer != null) {
                // 查询流是否存在，不存在则删除缓存数据
                JSONObject mediaInfo = zlmresTfulUtils.getMediaInfo(mediaServer, "rtp", "rtsp", streamInfo.getStream());
                if (mediaInfo != null && mediaInfo.getInteger("code") == 0 ) {
                    Boolean online = mediaInfo.getBoolean("online");
                    if (online != null && online) {
                        logger.info("[1078-点播] 点播已经存在，直接返回， deviceId： {}， channelId： {}", deviceId, channelId);
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
        String stream = deviceId + "_" + channelId;
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
            logger.info("[1078-点播] 点播成功， deviceId： {}， channelId： {}", deviceId, channelId);
            StreamInfo info = onPublishHandler(mediaServer, hookData, deviceId, channelId);

            for (GeneralCallback<StreamInfo> errorCallback : errorCallbacks) {
                errorCallback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), info);
            }
            subscribe.removeSubscribe(hook);
            redisTemplate.opsForValue().set(playKey, info);
        });
        // 设置超时监听
        dynamicTask.startDelay(playKey, () -> {
            logger.info("[1078-点播] 超时， deviceId： {}， channelId： {}", deviceId, channelId);
            for (GeneralCallback<StreamInfo> errorCallback : errorCallbacks) {
                errorCallback.run(InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getCode(),
                        InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getMsg(), null);
            }

        }, userSetting.getPlayTimeout());

        // 开启收流端口
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(mediaServer, stream, null, false, false, 0, false, false, false,1);
        logger.info("[1078-点播] deviceId： {}， channelId： {}， 端口： {}", deviceId, channelId, ssrcInfo.getPort());
        J9101 j9101 = new J9101();
        j9101.setChannel(Integer.valueOf(channelId));
        j9101.setIp(mediaServer.getSdpIp());
        j9101.setRate(1);
        j9101.setTcpPort(ssrcInfo.getPort());
        j9101.setUdpPort(ssrcInfo.getPort());
        j9101.setType(type);
        Object s = jt1078Template.startLive(deviceId, j9101, 6);
        System.out.println("ssss=== " + s);

    }

    public StreamInfo onPublishHandler(MediaServer mediaServerItem, HookData hookData, String deviceId, String channelId) {
        StreamInfo streamInfo = mediaServerService.getStreamInfoByAppAndStream(mediaServerItem, "rtp", hookData.getStream(), hookData.getMediaInfo(), null);
        streamInfo.setDeviceID(deviceId);
        streamInfo.setChannelId(channelId);
        return streamInfo;
    }

    @Override
    public void stopPlay(String deviceId, String channelId) {
        String playKey = VideoManagerConstants.INVITE_INFO_1078_PLAY + deviceId + ":" + channelId;
        dynamicTask.stop(playKey);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
        // 发送停止命令
        J9102 j9102 = new J9102();
        j9102.setChannel(Integer.valueOf(channelId));
        j9102.setCommand(0);
        j9102.setCloseType(0);
        j9102.setStreamType(1);
        jt1078Template.stopLive(deviceId, j9102, 6);
        logger.info("[1078-停止点播] deviceId： {}， channelId： {}", deviceId, channelId);
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
    public void pausePlay(String deviceId, String channelId) {
        String playKey = VideoManagerConstants.INVITE_INFO_1078_PLAY + deviceId + ":" + channelId;
        dynamicTask.stop(playKey);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
        if (streamInfo == null) {
            logger.info("[1078-暂停点播] 未找到点播信息 deviceId： {}， channelId： {}", deviceId, channelId);
        }
        logger.info("[1078-暂停点播] deviceId： {}， channelId： {}", deviceId, channelId);
        // 发送暂停命令
        J9102 j9102 = new J9102();
        j9102.setChannel(Integer.valueOf(channelId));
        j9102.setCommand(2);
        j9102.setCloseType(0);
        j9102.setStreamType(1);
        jt1078Template.stopLive(deviceId, j9102, 6);
    }

    @Override
    public void continueLivePlay(String deviceId, String channelId) {
        String playKey = VideoManagerConstants.INVITE_INFO_1078_PLAY + deviceId + ":" + channelId;
        dynamicTask.stop(playKey);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
        if (streamInfo == null) {
            logger.info("[1078-继续点播] 未找到点播信息 deviceId： {}， channelId： {}", deviceId, channelId);
        }
        logger.info("[1078-继续点播] deviceId： {}， channelId： {}", deviceId, channelId);
        // 发送暂停命令
        J9102 j9102 = new J9102();
        j9102.setChannel(Integer.valueOf(channelId));
        j9102.setCommand(2);
        j9102.setCloseType(0);
        j9102.setStreamType(1);
        jt1078Template.stopLive(deviceId, j9102, 6);
    }

    @Override
    public List<J1205.JRecordItem> getRecordList(String deviceId, String channelId, String startTime, String endTime) {
        logger.info("[1078-查询录像列表] deviceId： {}， channelId： {}， startTime： {}， endTime： {}"
                , deviceId, channelId, startTime, endTime);
        // 发送请求录像列表命令
        J9205 j9205 = new J9205();
        j9205.setChannelId(Integer.parseInt(channelId));
        j9205.setStartTime(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(startTime));
        j9205.setEndTime(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(endTime));
        j9205.setMediaType(0);
        j9205.setStreamType(0);
        j9205.setStorageType(0);
        List<J1205.JRecordItem> JRecordItemList = (List<J1205.JRecordItem>) jt1078Template.queryBackTime(deviceId, j9205, 20);
        if (JRecordItemList == null || JRecordItemList.isEmpty()) {
            return null;
        }
        logger.info("[1078-查询录像列表] deviceId： {}， channelId： {}， startTime： {}， endTime： {}, 结果: {}条"
                , deviceId, channelId, startTime, endTime, JRecordItemList.size() );
        return JRecordItemList;
    }

    @Override
    public void playback(String deviceId, String channelId, String startTime, String endTime, GeneralCallback<StreamInfo> callback) {

        // 检查流是否已经存在，存在则返回
        String playbackKey = VideoManagerConstants.INVITE_INFO_1078_PLAYBACK + deviceId + ":" + channelId;
        List<GeneralCallback<StreamInfo>> errorCallbacks = inviteErrorCallbackMap.computeIfAbsent(playbackKey, k -> new ArrayList<>());
        errorCallbacks.add(callback);
        String logInfo = String.format("deviceId:%s, channelId:%s, startTime:%s, endTime:%s", deviceId, channelId, startTime, endTime);
        StreamInfo streamInfo = (StreamInfo)redisTemplate.opsForValue().get(playbackKey);
        if (streamInfo != null) {
            String mediaServerId = streamInfo.getMediaServerId();
            MediaServer mediaServer = mediaServerService.getOne(mediaServerId);
            if (mediaServer != null) {
                // 查询流是否存在，不存在则删除缓存数据
                JSONObject mediaInfo = zlmresTfulUtils.getMediaInfo(mediaServer, "rtp", "rtsp", streamInfo.getStream());
                if (mediaInfo != null && mediaInfo.getInteger("code") == 0 ) {
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
        String stream = deviceId + "-" + channelId + "-" + startTimeParam + "-" + endTimeParam;
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
            StreamInfo info = onPublishHandler(mediaServer, hookData, deviceId, channelId);

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
        j9201.setRate(0);
        j9201.setPlaybackType(0);
        j9201.setPlaybackSpeed(0);
        j9201.setTcpPort(ssrcInfo.getPort());
        j9201.setUdpPort(ssrcInfo.getPort());
        j9201.setType(0);
        j9201.setStartTime(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(startTime));
        j9201.setEndTime(DateUtil.yyyy_MM_dd_HH_mm_ssTo1078(endTime));
        jt1078Template.startBackLive(deviceId, j9201, 20);

    }

    @Override
    public void stopPlayback(String deviceId, String channelId) {
        String playKey = VideoManagerConstants.INVITE_INFO_1078_PLAYBACK + deviceId + ":" + channelId;
        dynamicTask.stop(playKey);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
        // 发送停止命令
        J9202 j9202 = new J9202();
        j9202.setChannel(Integer.parseInt(channelId));
        j9202.setPlaybackType(0);
        j9202.setPlaybackSpeed(0);
        j9202.setPlaybackTime("");
        jt1078Template.controlBackLive(deviceId, j9202, 6);
        logger.info("[1078-停止回放] deviceId： {}， channelId： {}", deviceId, channelId);
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
    public void ptzControl(String deviceId, String channelId, String command, int speed) {
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
                jt1078Template.ptzRotate(deviceId, j9301, 6);
                break;

            case "zoomin":
            case "zoomout":
                J9306 j9306 = new J9306();
                j9306.setChannel(Integer.parseInt(channelId));
                if (command.equals("zoomin")) {
                    j9306.setZoom(0);
                }else {
                    j9306.setZoom(1);
                }
                jt1078Template.ptzZoom(deviceId, j9306, 6);
                break;
            case "irisin":
            case "irisout":
                J9303 j9303 = new J9303();
                j9303.setChannel(Integer.parseInt(channelId));
                if (command.equals("irisin")) {
                    j9303.setIris(0);
                }else {
                    j9303.setIris(1);
                }
                jt1078Template.ptzIris(deviceId, j9303, 6);
                break;
            case "focusnear":
            case "focusfar":
                J9302 j9302 = new J9302();
                j9302.setChannel(Integer.parseInt(channelId));
                if (command.equals("focusfar")) {
                    j9302.setFocalDirection(0);
                }else {
                    j9302.setFocalDirection(1);
                }
                jt1078Template.ptzFocal(deviceId, j9302, 6);
                break;

        }
    }

    @Override
    public void supplementaryLight(String deviceId, String channelId, String command) {
        J9305 j9305 = new J9305();
        j9305.setChannel(Integer.parseInt(channelId));
        if (command.equalsIgnoreCase("on")) {
            j9305.setOn(1);
        }else {
            j9305.setOn(0);
        }
        jt1078Template.ptzSupplementaryLight(deviceId, j9305, 6);
    }

    @Override
    public void wiper(String deviceId, String channelId, String command) {
        J9304 j9304 = new J9304();
        j9304.setChannel(Integer.parseInt(channelId));
        if (command.equalsIgnoreCase("on")) {
            j9304.setOn(1);
        }else {
            j9304.setOn(0);
        }
        jt1078Template.ptzWiper(deviceId, j9304, 6);
    }

    @Override
    public JTDeviceConfig queryConfig(String deviceId, String[] params, GeneralCallback<StreamInfo> callback) {
        if (deviceId == null) {
            return null;
        }
        if (params == null || params.length == 0) {
            J8104 j8104 = new J8104();
            return (JTDeviceConfig)jt1078Template.getDeviceConfig(deviceId, j8104, 20);
        }else {
            long[] paramBytes = new long[params.length];
            for (int i = 0; i < params.length; i++) {
                try {
                    Field field = JTDeviceConfig.class.getDeclaredField(params[i]);
                    if (field.isAnnotationPresent(ConfigAttribute.class) ) {
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
            return (JTDeviceConfig)jt1078Template.getDeviceSpecifyConfig(deviceId, j8106, 20);
        }
    }

    @Override
    public void setConfig(String deviceId, JTDeviceConfig config) {
        J8103 j8103 = new J8103();
        j8103.setConfig(config);
        jt1078Template.setDeviceSpecifyConfig(deviceId, j8103, 6);
    }

    @Override
    public void connectionControl(String deviceId, JTDeviceConnectionControl control) {
        J8105 j8105 = new J8105();
        j8105.setConnectionControl(control);
        jt1078Template.deviceControl(deviceId, j8105, 6);
    }

    @Override
    public void resetControl(String deviceId) {
        J8105 j8105 = new J8105();
        j8105.setReset(true);
        jt1078Template.deviceControl(deviceId, j8105, 6);
    }

    @Override
    public void factoryResetControl(String deviceId) {
        J8105 j8105 = new J8105();
        j8105.setFactoryReset(true);
        jt1078Template.deviceControl(deviceId, j8105, 6);
    }

    @Override
    public JTDeviceAttribute attribute(String deviceId) {
        J8107 j8107 = new J8107();
        return (JTDeviceAttribute)jt1078Template.deviceAttribute(deviceId, j8107, 20);
    }

    @Override
    public JTPositionBaseInfo queryPositionInfo(String deviceId) {
        J8201 j8201 = new J8201();
        return (JTPositionBaseInfo)jt1078Template.queryPositionInfo(deviceId, j8201, 20);
    }

    @Override
    public void tempPositionTrackingControl(String deviceId, Integer timeInterval, Long validityPeriod) {
        J8202 j8202 = new J8202();
        j8202.setTimeInterval(timeInterval);
        j8202.setValidityPeriod(validityPeriod);
        jt1078Template.tempPositionTrackingControl(deviceId, j8202, 20);
    }

    @Override
    public void confirmationAlarmMessage(String deviceId, int alarmPackageNo, JTConfirmationAlarmMessageType alarmMessageType) {
        J8203 j8203 = new J8203();
        j8203.setAlarmMessageType(alarmMessageType);
        j8203.setAlarmPackageNo(alarmPackageNo);
        jt1078Template.confirmationAlarmMessage(deviceId, j8203, 6);
    }

    @Override
    public int linkDetection(String deviceId) {
        J8204 j8204 = new J8204();
        return (int)jt1078Template.linkDetection(deviceId, j8204, 6);
    }

    @Override
    public int textMessage(String deviceId, JTTextSign sign, int textType, String content) {
        J8300 j8300 = new J8300();
        j8300.setSign(sign);
        j8300.setTextType(textType);
        j8300.setContent(content);
        return (int)jt1078Template.textMessage(deviceId, j8300, 6);
    }

    @Override
    public int telephoneCallback(String deviceId, Integer sign, String phoneNumber) {
        J8400 j8400 = new J8400();
        j8400.setSign(sign);
        j8400.setPhoneNumber(phoneNumber);
        return (int)jt1078Template.telephoneCallback(deviceId, j8400, 6);
    }

    @Override
    public int setPhoneBook(String deviceId, int type, List<JTPhoneBookContact> phoneBookContactList) {
        J8401 j8401 = new J8401();
        j8401.setType(type);
        if (phoneBookContactList != null) {
            j8401.setPhoneBookContactList(phoneBookContactList);
        }
        return (int)jt1078Template.setPhoneBook(deviceId, j8401, 6);
    }

    @Override
    public JTPositionBaseInfo controlDoor(String deviceId, Boolean open) {
        J8500 j8500 = new J8500();
        JTVehicleControl jtVehicleControl = new JTVehicleControl();
        jtVehicleControl.setControlCarDoor(open?1:0);
        j8500.setVehicleControl(jtVehicleControl);
        return (JTPositionBaseInfo)jt1078Template.vehicleControl(deviceId, j8500, 20);
    }

    @Override
    public int setAreaForCircle(int attribute, String deviceId, List<JTCircleArea> circleAreaList) {
        J8600 j8600 = new J8600();
        j8600.setAttribute(attribute);
        j8600.setCircleAreaList(circleAreaList);
        return (int)jt1078Template.setAreaForCircle(deviceId, j8600, 20);
    }

    @Override
    public int deleteAreaForCircle(String deviceId, List<Long> ids) {
        J8601 j8601 = new J8601();
        j8601.setIdList(ids);
        return (int)jt1078Template.deleteAreaForCircle(deviceId, j8601, 20);
    }

    @Override
    public List<JTAreaOrRoute> queryAreaForCircle(String deviceId, List<Long> ids) {
        J8608 j8608 = new J8608();
        j8608.setType(1);
        j8608.setIdList(ids);
        return (List<JTAreaOrRoute>)jt1078Template.queryAreaOrRoute(deviceId, j8608, 20);
    }

    @Override
    public int setAreaForRectangle(int attribute, String deviceId, List<JTRectangleArea> rectangleAreas) {
        J8602 j8602 = new J8602();
        j8602.setAttribute(attribute);
        j8602.setRectangleAreas(rectangleAreas);
        return (int)jt1078Template.setAreaForRectangle(deviceId, j8602, 20);
    }

    @Override
    public int deleteAreaForRectangle(String deviceId, List<Long> ids) {
        J8603 j8603 = new J8603();
        j8603.setIdList(ids);
        return (int)jt1078Template.deleteAreaForRectangle(deviceId, j8603, 20);
    }

    @Override
    public List<JTAreaOrRoute> queryAreaForRectangle(String deviceId, List<Long> ids) {
        J8608 j8608 = new J8608();
        j8608.setType(2);
        j8608.setIdList(ids);
        return (List<JTAreaOrRoute>)jt1078Template.queryAreaOrRoute(deviceId, j8608, 20);
    }

    @Override
    public int setAreaForPolygon(String deviceId, JTPolygonArea polygonArea) {
        J8604 j8604 = new J8604();
        j8604.setPolygonArea(polygonArea);
        return (int)jt1078Template.setAreaForPolygon(deviceId, j8604, 20);
    }

    @Override
    public int deleteAreaForPolygon(String deviceId, List<Long> ids) {
        J8605 j8605 = new J8605();
        j8605.setIdList(ids);
        return (int)jt1078Template.deleteAreaForPolygon(deviceId, j8605, 20);
    }

    @Override
    public List<JTAreaOrRoute> queryAreaForPolygon(String deviceId, List<Long> ids) {
        J8608 j8608 = new J8608();
        j8608.setType(3);
        j8608.setIdList(ids);
        return (List<JTAreaOrRoute>)jt1078Template.queryAreaOrRoute(deviceId, j8608, 20);
    }

    @Override
    public int setRoute(String deviceId, JTRoute route) {
        J8606 j8606 = new J8606();
        j8606.setRoute(route);
        return (int)jt1078Template.setRoute(deviceId, j8606, 20);
    }

    @Override
    public int deleteRoute(String deviceId, List<Long> ids) {
        J8607 j8607 = new J8607();
        j8607.setIdList(ids);
        return (int)jt1078Template.deleteRoute(deviceId, j8607, 20);
    }

    @Override
    public List<JTAreaOrRoute> queryRoute(String deviceId, List<Long> ids) {
        J8608 j8608 = new J8608();
        j8608.setType(4);
        j8608.setIdList(ids);
        return (List<JTAreaOrRoute>)jt1078Template.queryAreaOrRoute(deviceId, j8608, 20);
    }

    @Override
    public JTDriverInformation queryDriverInformation(String deviceId) {
        J8702 j8702 = new J8702();
        return (JTDriverInformation)jt1078Template.queryDriverInformation(deviceId, j8702, 20);
    }

    @Override
    public List<Long> shooting(String deviceId, JTShootingCommand shootingCommand) {
        J8801 j8801 = new J8801();
        j8801.setCommand(shootingCommand);
        return (List<Long>)jt1078Template.shooting(deviceId, j8801, 300);
    }

    @Override
    public List<JTMediaDataInfo> queryMediaData(String deviceId, JTQueryMediaDataCommand queryMediaDataCommand) {
        J8802 j8802 = new J8802();
        j8802.setCommand(queryMediaDataCommand);
        return (List<JTMediaDataInfo>)jt1078Template.queryMediaData(deviceId, j8802, 300);
    }

    @Override
    public void uploadMediaData(String deviceId, JTQueryMediaDataCommand queryMediaDataCommand) {
        J8803 j8803 = new J8803();
        j8803.setCommand(queryMediaDataCommand);
        jt1078Template.uploadMediaData(deviceId, j8803, 10);
    }

    @Override
    public void record(String deviceId, int command, Integer time, Integer save, Integer samplingRate) {
        J8804 j8804 = new J8804();
        j8804.setCommond(command);
        j8804.setDuration(time);
        j8804.setSave(save);
        j8804.setSamplingRate(samplingRate);
        jt1078Template.record(deviceId, j8804, 10);
    }

    @Override
    public void uploadMediaDataForSingle(String deviceId, Long mediaId, Integer delete) {
        J8805 j8805 = new J8805();
        j8805.setMediaId(mediaId);
        j8805.setDelete(delete);
        jt1078Template.uploadMediaDataForSingle(deviceId, j8805, 10);
    }

    @Override
    public JTMediaAttribute queryMediaAttribute(String deviceId) {
        J9003 j9003 = new J9003();
        return (JTMediaAttribute)jt1078Template.queryMediaAttribute(deviceId, j9003, 300);
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
    public void startTalk(String deviceId, String channelId, String app, String stream, String mediaServerId, Boolean onlySend,
                          GeneralCallback<StreamInfo> callback) {
        // 检查流是否已经存在，存在则返回
        String playKey = VideoManagerConstants.INVITE_INFO_1078_TALK + deviceId + ":" + channelId;
        List<GeneralCallback<StreamInfo>> errorCallbacks = inviteErrorCallbackMap.computeIfAbsent(playKey, k -> new ArrayList<>());
        errorCallbacks.add(callback);
        StreamInfo streamInfo = (StreamInfo)redisTemplate.opsForValue().get(playKey);
        if (streamInfo != null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "对讲进行中");
        }

        String receiveStream = "1078" + "_" + deviceId + "_" + channelId;
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
        String ssrc = deviceId + "_" + channelId;
        SendRtpItem sendRtpItem = new SendRtpItem();
        sendRtpItem.setMediaServerId(mediaServerId);
        sendRtpItem.setPort(0);
        sendRtpItem.setSsrc(ssrc);
        sendRtpItem.setDeviceId(deviceId);
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
        sendRtpItem.setPlatformId(deviceId);
        if (onlySend == null || !onlySend) {
            // 设置hook监听
            Hook hook = Hook.getInstance(HookType.on_media_arrival, "rtp", receiveStream, mediaServer.getId());
            subscribe.addSubscribe(hook, (hookData) -> {
                dynamicTask.stop(playKey);
                logger.info("[1078-对讲] 对讲成功， deviceId： {}， channelId： {}", deviceId, channelId);
                StreamInfo info = onPublishHandler(mediaServer, hookData, deviceId, channelId);

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
                logger.info("[1078-对讲] 对讲时源流注销， app: {}. stream: {}, deviceId： {}， channelId： {}",app, stream, deviceId, channelId);
                stopTalk(deviceId, channelId);
            });
            // 设置超时监听
            dynamicTask.startDelay(playKey, () -> {
                logger.info("[1078-对讲] 超时， deviceId： {}， channelId： {}", deviceId, channelId);
                for (GeneralCallback<StreamInfo> errorCallback : errorCallbacks) {
                    errorCallback.run(InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getCode(),
                            InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getMsg(), null);
                }

            }, userSetting.getPlayTimeout());
        }

        Integer localPort = mediaServerService.startSendRtpPassive(mediaServer, sendRtpItem, 15000);

        logger.info("[1078-对讲] deviceId： {}， channelId： {}， 收发端口： {}， app: {}, stream: {}",
                deviceId, channelId, localPort, app, stream);
        J9101 j9101 = new J9101();
        j9101.setChannel(Integer.valueOf(channelId));
        j9101.setIp(mediaServer.getSdpIp());
        j9101.setRate(1);
        j9101.setTcpPort(localPort);
        j9101.setUdpPort(localPort);
        j9101.setType(2);
        jt1078Template.startLive(deviceId, j9101, 6);
        if (onlySend != null && onlySend) {
            logger.info("[1078-对讲] 对讲成功， deviceId： {}， channelId： {}", deviceId, channelId);
            for (GeneralCallback<StreamInfo> errorCallback : errorCallbacks) {
                errorCallback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), null);
            }
            // 存储发流信息
            redisCatchStorage.updateSendRTPSever(sendRtpItem);
        }
    }

    @Override
    public void stopTalk(String deviceId, String channelId) {
        String playKey = VideoManagerConstants.INVITE_INFO_1078_TALK + deviceId + ":" + channelId;
        dynamicTask.stop(playKey);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
        // 发送停止命令
        J9102 j9102 = new J9102();
        j9102.setChannel(Integer.valueOf(channelId));
        j9102.setCommand(4);
        j9102.setCloseType(0);
        j9102.setStreamType(1);
        jt1078Template.stopLive(deviceId, j9102, 6);
        logger.info("[1078-停止对讲] deviceId： {}， channelId： {}", deviceId, channelId);
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
    public void changeStreamType(String deviceId, String channelId, Integer streamType) {
        String playKey = VideoManagerConstants.INVITE_INFO_1078_PLAY + deviceId + ":" + channelId;
        dynamicTask.stop(playKey);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
        if (streamInfo == null) {
            logger.info("[1078-切换码流类型] 未找到点播信息 deviceId： {}， channelId： {}, streamType: {}", deviceId, channelId, streamType);
        }
        logger.info("[1078-切换码流类型] deviceId： {}， channelId： {}, streamType: {}", deviceId, channelId, streamType);
        // 发送暂停命令
        J9102 j9102 = new J9102();
        j9102.setChannel(Integer.valueOf(channelId));
        j9102.setCommand(1);
        j9102.setCloseType(0);
        j9102.setStreamType(streamType);
        jt1078Template.stopLive(deviceId, j9102, 6);
    }
}
