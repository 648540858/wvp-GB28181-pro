package com.genersoft.iot.vmp.jt1078.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.GeneralCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.jt1078.bean.*;
import com.genersoft.iot.vmp.jt1078.bean.common.ConfigAttribute;
import com.genersoft.iot.vmp.jt1078.cmd.JT1078Template;
import com.genersoft.iot.vmp.jt1078.dao.JTDeviceMapper;
import com.genersoft.iot.vmp.jt1078.event.CallbackManager;
import com.genersoft.iot.vmp.jt1078.proc.request.J1205;
import com.genersoft.iot.vmp.jt1078.proc.response.*;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.hook.Hook;
import com.genersoft.iot.vmp.media.event.hook.HookData;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.hook.HookType;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
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
    public void play(String deviceId, String channelId, GeneralCallback<StreamInfo> callback) {

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
        String stream = deviceId + "-" + channelId;
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
        j9101.setType(0);
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
        if (!generalCallbacks.isEmpty()) {
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
        if (!generalCallbacks.isEmpty()) {
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
}
