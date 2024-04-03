package com.genersoft.iot.vmp.jt1078.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.GeneralCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import com.genersoft.iot.vmp.jt1078.cmd.JT1078Template;
import com.genersoft.iot.vmp.jt1078.config.JT1078Controller;
import com.genersoft.iot.vmp.jt1078.dao.JTDeviceMapper;
import com.genersoft.iot.vmp.jt1078.proc.response.J9101;
import com.genersoft.iot.vmp.jt1078.proc.response.J9102;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeFactory;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForStreamChange;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.hook.HookParam;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnStreamChangedHookParam;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
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
    private ZlmHttpHookSubscribe subscribe;

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
        String playKey = VideoManagerConstants.INVITE_INFO_1078 + deviceId + ":" + channelId;
        List<GeneralCallback<StreamInfo>> errorCallbacks = inviteErrorCallbackMap.computeIfAbsent(playKey, k -> new ArrayList<>());
        errorCallbacks.add(callback);
        StreamInfo streamInfo = (StreamInfo)redisTemplate.opsForValue().get(playKey);
        if (streamInfo != null) {
            String mediaServerId = streamInfo.getMediaServerId();
            MediaServerItem mediaServerItem = mediaServerService.getOne(mediaServerId);
            if (mediaServerItem != null) {
                // 查询流是否存在，不存在则删除缓存数据
                JSONObject mediaInfo = zlmresTfulUtils.getMediaInfo(mediaServerItem, "rtp", "rtsp", streamInfo.getStream());
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
        MediaServerItem mediaServerItem = mediaServerService.getMediaServerForMinimumLoad(null);

        // 设置hook监听
        HookSubscribeForStreamChange hookSubscribe = HookSubscribeFactory.on_stream_changed("rtp", stream, true, "rtsp", mediaServerItem.getId());
        subscribe.addSubscribe(hookSubscribe, (MediaServerItem mediaServerItemInUse, HookParam hookParam) -> {
            OnStreamChangedHookParam streamChangedHookParam = (OnStreamChangedHookParam) hookParam;
            dynamicTask.stop(playKey);
            logger.info("[1078-点播] 点播成功， deviceId： {}， channelId： {}", deviceId, channelId);
            StreamInfo info = onPublishHandler(mediaServerItemInUse, streamChangedHookParam, deviceId, channelId);

            for (GeneralCallback<StreamInfo> errorCallback : errorCallbacks) {
                errorCallback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), info);
            }
            subscribe.removeSubscribe(hookSubscribe);
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
        SSRCInfo ssrcInfo = mediaServerService.openRTPServer(mediaServerItem, stream, null, false, false, 0, false, false, 1);
        logger.info("[1078-点播] deviceId： {}， channelId： {}， 端口： {}", deviceId, channelId, ssrcInfo.getPort());
        J9101 j9101 = new J9101();
        j9101.setChannel(Integer.valueOf(channelId));
        j9101.setIp(mediaServerItem.getSdpIp());
        j9101.setRate(1);
        j9101.setTcpPort(ssrcInfo.getPort());
        j9101.setUdpPort(ssrcInfo.getPort());
        j9101.setType(0);
        String s = jt1078Template.startLive(deviceId, j9101, 6);
        System.out.println("ssss=== " + s);

    }

    public StreamInfo onPublishHandler(MediaServerItem mediaServerItem, OnStreamChangedHookParam hookParam, String deviceId, String channelId) {
        StreamInfo streamInfo = mediaService.getStreamInfoByAppAndStream(mediaServerItem, "rtp", hookParam.getStream(), hookParam.getTracks(), null);
        streamInfo.setDeviceID(deviceId);
        streamInfo.setChannelId(channelId);
        return streamInfo;
    }

    @Override
    public void stopPlay(String deviceId, String channelId) {
        String playKey = VideoManagerConstants.INVITE_INFO_1078 + deviceId + ":" + channelId;
        dynamicTask.stop(playKey);
        StreamInfo streamInfo = (StreamInfo) redisTemplate.opsForValue().get(playKey);
        // 发送停止命令
        J9102 j9102 = new J9102();
        j9102.setChannel(Integer.valueOf(channelId));
        j9102.setCommand(0);
        j9102.setCloseType(0);
        j9102.setStreamType(1);
        jt1078Template.stopLive(deviceId, j9102, 6);
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
}
