package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.enums.MediaApp;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.OpenRTPServerResult;
import com.genersoft.iot.vmp.gb28181.session.SSRCFactory;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.hook.Hook;
import com.genersoft.iot.vmp.media.event.hook.HookData;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.hook.HookType;
import com.genersoft.iot.vmp.media.event.media.MediaArrivalEvent;
import com.genersoft.iot.vmp.media.event.media.MediaDepartureEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IReceiveRtpServerService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.service.bean.RTPServerParam;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
public class RtpServerServiceImpl implements IReceiveRtpServerService {

    private final static String TIMEOUT_TASK_KEY_PREFIX = "RTP_SERVER_TIMEOUT_TASK";

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private SSRCFactory ssrcFactory;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private HookSubscribe subscribe;

    @Autowired
    private SipInviteSessionManager sessionManager;

    /**
     * 流到来的处理
     */
    @Async("taskExecutor")
    @org.springframework.context.event.EventListener
    public void onApplicationEvent(MediaArrivalEvent event) {

    }

    /**
     * 流离开的处理
     */
    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(MediaDepartureEvent event) {

    }

    @Override
    public SSRCInfo openGbRTPServer(MediaServer mediaServer, String streamId, String presetSSRC, int tcpMode,
                                    boolean playback, boolean ssrcCheck, boolean onlyAuto, boolean disableAuto,
                                    ErrorCallback<OpenRTPServerResult> callback) {
        if (callback == null) {
            log.warn("[开启国标RTP收流] 失败，回调为NULL");
            return null;
        }
        if (mediaServer == null) {
            log.warn("[开启国标RTP收流] 失败，媒体节点为NULL");
            return null;
        }

        // 获取 mediaServer 可用的 ssrc
        final String ssrc;
        if (presetSSRC != null) {
            ssrc = presetSSRC;
        }else {
            if (playback) {
                ssrc = ssrcFactory.getPlayBackSsrc(mediaServer.getId());
            }else {
                ssrc = ssrcFactory.getPlaySsrc(mediaServer.getId());
            }
        }
        if (streamId == null) {
            streamId = String.format("%08x", Long.parseLong(ssrc)).toUpperCase();
        }
        if (ssrcCheck && tcpMode > 0) {
            // 目前zlm不支持 tcp模式更新ssrc，暂时关闭ssrc校验
            log.warn("[openRTPServer] 平台对接时下级可能自定义ssrc，但是tcp模式zlm收流目前无法更新ssrc，可能收流超时，此时请使用udp收流或者关闭ssrc校验");
        }

        SSRCInfo ssrcInfo = new SSRCInfo(0, ssrc, MediaApp.GB28181, streamId);
        if (presetSSRC == null) {
            ssrcInfo.setAllocatedSsrc(ssrc);
        }
        RTPServerParam rtpServerParam = new RTPServerParam(mediaServer, MediaApp.GB28181, streamId, ssrcCheck ? Long.parseLong(ssrc): 0L, null, onlyAuto, disableAuto, false, tcpMode);
        int rtpServerPort = openRTPServer(rtpServerParam, ((code, msg, data) -> {
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                OpenRTPServerResult openRTPServerResult = new OpenRTPServerResult();
                openRTPServerResult.setHookData(data);
                openRTPServerResult.setSsrcInfo(ssrcInfo);
                callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), openRTPServerResult);
            } else {
                // 释放ssrc
                if (presetSSRC == null) {
                    ssrcFactory.releaseSsrc(mediaServer.getId(), ssrc);
                    ssrcInfo.setAllocatedSsrc(null);
                }
                OpenRTPServerResult openRTPServerResult = new OpenRTPServerResult();
                openRTPServerResult.setSsrcInfo(ssrcInfo);
                callback.run(code, msg, openRTPServerResult);
            }
        }));
        ssrcInfo.setPort(rtpServerPort);
        return new SSRCInfo(rtpServerPort, ssrc, MediaApp.GB28181, streamId);
    }

    @Override
    public int openRTPServer(RTPServerParam rtpServerParam, ErrorCallback<HookData> callback) {
        if (callback == null) {
            log.warn("[开启RTP收流] 失败，回调为NULL");
            return -1;
        }
        if (rtpServerParam.getMediaServer() == null) {
            log.warn("[开启RTP收流] 失败，媒体节点为NULL");
            return -1;
        }

        // 设置流超时的定时任务
        String timeOutTaskKey = String.format("%s_%s_%s_%s", TIMEOUT_TASK_KEY_PREFIX, rtpServerParam.getMediaServer().getId(), rtpServerParam.getApp(), rtpServerParam.getStreamId());

        Hook rtpHook = Hook.getInstance(HookType.on_media_arrival, rtpServerParam.getApp(), rtpServerParam.getStreamId(), rtpServerParam.getMediaServer().getId());
        dynamicTask.startDelay(timeOutTaskKey, () -> {
            // 收流超时
            // 关闭收流端口
            mediaServerService.closeRTPServer(rtpServerParam.getMediaServer(), rtpServerParam.getApp(), rtpServerParam.getStreamId());
            subscribe.removeSubscribe(rtpHook);
            callback.run(InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getCode(), InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getMsg(), null);
        }, userSetting.getPlayTimeout());
        // 开启流到来的监听
        subscribe.addSubscribe(rtpHook, (hookData) -> {
            dynamicTask.stop(timeOutTaskKey);
            // hook响应
            callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), hookData);
            subscribe.removeSubscribe(rtpHook);
        });

        int rtpServerPort;
        if (rtpServerParam.getMediaServer().isRtpEnable()) {
            rtpServerPort = mediaServerService.createRTPServer(rtpServerParam.getMediaServer(), rtpServerParam.getApp(), rtpServerParam.getStreamId(),
                    Objects.requireNonNullElse(rtpServerParam.getSsrc(), 0L), rtpServerParam.getPort(), rtpServerParam.isOnlyAuto(),
                    rtpServerParam.isDisableAudio(), rtpServerParam.isReUsePort(), rtpServerParam.getTcpMode());
        } else {
            rtpServerPort = rtpServerParam.getMediaServer().getRtpProxyPort();
        }
        if (rtpServerPort == 0) {
            callback.run(InviteErrorCode.ERROR_FOR_RESOURCE_EXHAUSTION.getCode(), "开启RTPServer失败", null);
            return -1;
        }
        return rtpServerPort;
    }

    @Override
    public void closeRTPServer(MediaServer mediaServer, String app, String stream) {
        if (mediaServer == null) {
            return;
        }
        String timeOutTaskKey = String.format("%s_%s_%s_%s", TIMEOUT_TASK_KEY_PREFIX, mediaServer.getId(), app, stream);
        if (dynamicTask.contains(timeOutTaskKey)) {
            dynamicTask.stop(timeOutTaskKey);
        }
        mediaServerService.closeRTPServer(mediaServer, app, stream);
    }

    @Override
    public void closeRTPServerByMediaServerId(String mediaServerId, String app, String stream) {
        MediaServer mediaServer = mediaServerService.getOne(mediaServerId);
        if (mediaServer == null) {
            return;
        }
        closeRTPServer(mediaServer, app, stream);
    }
}
