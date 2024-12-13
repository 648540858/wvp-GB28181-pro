package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelPlayService;
import com.genersoft.iot.vmp.gb28181.service.IPlayService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyPlayService;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushPlayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sip.message.Response;

@Slf4j
@Service
public class GbChannelPlayServiceImpl implements IGbChannelPlayService {

    @Autowired
    private IPlayService deviceChannelPlayService;

    @Autowired
    private IStreamProxyPlayService streamProxyPlayService;

    @Autowired
    private IStreamPushPlayService streamPushPlayService;


    @Override
    public void start(CommonGBChannel channel, InviteMessageInfo inviteInfo, Platform platform, ErrorCallback<StreamInfo> callback) {
        if (channel == null || inviteInfo == null || callback == null) {
            log.warn("[通用通道点播] 参数异常, channel: {}, inviteInfo: {}, callback: {}", channel != null, inviteInfo != null, callback != null);
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
        log.info("[点播通用通道] 类型：{}， 通道： {}({})", inviteInfo.getSessionName(), channel.getGbName(), channel.getGbDeviceId());
        if ("Play".equalsIgnoreCase(inviteInfo.getSessionName())) {
            play(channel, platform, callback);
        }else if ("Playback".equals(inviteInfo.getSessionName())) {
            if (channel.getGbDeviceDbId() != null) {
                // 国标通道
                playbackGbDeviceChannel(channel, inviteInfo.getStartTime(), inviteInfo.getStopTime(), callback);
            } else if (channel.getStreamProxyId() != null) {
                // 拉流代理
                log.warn("[回放通用通道] 不支持回放拉流代理的录像： {}({})", channel.getGbName(), channel.getGbDeviceId());
                throw new PlayException(Response.FORBIDDEN, "forbidden");
            } else if (channel.getStreamPushId() != null) {
                // 推流
                log.warn("[回放通用通道] 不支持回放推流的录像： {}({})", channel.getGbName(), channel.getGbDeviceId());
                throw new PlayException(Response.FORBIDDEN, "forbidden");
            } else {
                // 通道数据异常
                log.error("[回放通用通道] 通道数据异常，无法识别通道来源： {}({})", channel.getGbName(), channel.getGbDeviceId());
                throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
            }
        }else if ("Download".equals(inviteInfo.getSessionName())) {
            if (channel.getGbDeviceDbId() != null) {
                int downloadSpeed = 4;
                try {
                    if (inviteInfo.getDownloadSpeed() != null){
                        downloadSpeed = Integer.parseInt(inviteInfo.getDownloadSpeed());
                    }
                }catch (Exception ignored) {}

                // 国标通道
                downloadGbDeviceChannel(channel, inviteInfo.getStartTime(), inviteInfo.getStopTime(), downloadSpeed, callback);
            } else if (channel.getStreamProxyId() != null) {
                // 拉流代理
                log.warn("[下载通用通道录像] 不支持下载拉流代理的录像： {}({})", channel.getGbName(), channel.getGbDeviceId());
                throw new PlayException(Response.FORBIDDEN, "forbidden");
            } else if (channel.getStreamPushId() != null) {
                // 推流
                log.warn("[下载通用通道录像] 不支持下载推流的录像： {}({})", channel.getGbName(), channel.getGbDeviceId());
                throw new PlayException(Response.FORBIDDEN, "forbidden");
            } else {
                // 通道数据异常
                log.error("[回放通用通道] 通道数据异常，无法识别通道来源： {}({})", channel.getGbName(), channel.getGbDeviceId());
                throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
            }
        }else {
            // 不支持的点播方式
            log.error("[点播通用通道] 不支持的点播方式：{}， {}({})", inviteInfo.getSessionName(), channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.BAD_REQUEST, "bad request");
        }
    }

    @Override
    public void stopPlay(InviteSessionType type, CommonGBChannel channel, String stream) {
        if (channel.getGbDeviceDbId() != null) {
            // 国标通道
            stopPlayDeviceChannel(channel, stream);
        } else if (channel.getStreamProxyId() != null) {
            // 拉流代理
            stopPlayProxy(channel);
        } else if (channel.getStreamPushId() != null) {
            // 推流
            stopPlayPush(channel);
        } else {
            // 通道数据异常
            log.error("[点播通用通道] 通道数据异常，无法识别通道来源： {}({})", channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
    }

    @Override
    public void play(CommonGBChannel channel, Platform platform, ErrorCallback<StreamInfo> callback) {
        if (channel.getGbDeviceDbId() != null) {
            // 国标通道
            playGbDeviceChannel(channel, callback);
        } else if (channel.getStreamProxyId() != null) {
            // 拉流代理
            playProxy(channel, callback);
        } else if (channel.getStreamPushId() != null) {
            if (platform != null) {
                // 推流
                playPush(channel, platform.getServerGBId(), platform.getName(), callback);
            }else {
                // 推流
                playPush(channel, null, null, callback);
            }
        } else {
            // 通道数据异常
            log.error("[点播通用通道] 通道数据异常，无法识别通道来源： {}({})", channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
    }

    @Override
    public void playGbDeviceChannel(CommonGBChannel channel, ErrorCallback<StreamInfo> callback){
        // 国标通道
        try {
            deviceChannelPlayService.play(channel, callback);
        } catch (PlayException e) {
            callback.run(e.getCode(), e.getMsg(), null);
        } catch (Exception e) {
            log.error("[点播失败] {}({})", channel.getGbName(), channel.getGbDeviceId(), e);
            callback.run(Response.BUSY_HERE, "busy here", null);
        }
    }

    @Override
    public void stopPlayDeviceChannel(CommonGBChannel channel, String stream) {
        // 国标通道
        try {
            deviceChannelPlayService.stop(InviteSessionType.PLAY, channel, stream);
        }  catch (Exception e) {
            log.error("[停止点播失败] {}({})", channel.getGbName(), channel.getGbDeviceId(), e);
        }
    }

    @Override
    public void playProxy(CommonGBChannel channel, ErrorCallback<StreamInfo> callback){
        // 拉流代理通道
        try {
            StreamInfo streamInfo = streamProxyPlayService.start(channel.getStreamProxyId());
            if (streamInfo == null) {
                callback.run(Response.BUSY_HERE, "busy here", null);
            }else {
                callback.run(InviteErrorCode.SUCCESS.getCode(), InviteErrorCode.SUCCESS.getMsg(), streamInfo);
            }
        }catch (Exception e) {
            callback.run(Response.BUSY_HERE, "busy here", null);
        }
    }

    @Override
    public void stopPlayProxy(CommonGBChannel channel) {
        // 拉流代理通道
        try {
            streamProxyPlayService.stop(channel.getStreamProxyId());
        }catch (Exception e) {
            log.error("[停止点播失败] {}({})", channel.getGbName(), channel.getGbDeviceId(), e);
        }
    }

    @Override
    public void playPush(CommonGBChannel channel, String platformDeviceId, String platformName, ErrorCallback<StreamInfo> callback){
        // 推流
        try {
            streamPushPlayService.start(channel.getStreamPushId(), callback, platformDeviceId, platformName);
        }catch (PlayException e) {
            callback.run(e.getCode(), e.getMsg(), null);
        }catch (Exception e) {
            log.error("[点播推流通道失败] 通道： {}({})", channel.getGbName(), channel.getGbDeviceId(), e);
            callback.run(Response.BUSY_HERE, "busy here", null);
        }
    }

    @Override
    public void stopPlayPush(CommonGBChannel channel) {
        // 推流
        try {
            streamPushPlayService.stop(channel.getStreamPushId());
        }catch (Exception e) {
            log.error("[停止点播失败] {}({})", channel.getGbName(), channel.getGbDeviceId(), e);
        }
    }

    private void playbackGbDeviceChannel(CommonGBChannel channel, Long startTime, Long stopTime, ErrorCallback<StreamInfo> callback){
        try {
            deviceChannelPlayService.playBack(channel, startTime, stopTime, callback);
        } catch (PlayException e) {
            callback.run(e.getCode(), e.getMsg(), null);
        } catch (Exception e) {
            callback.run(Response.BUSY_HERE, "busy here", null);
        }
    }

    private void downloadGbDeviceChannel(CommonGBChannel channel, Long startTime, Long stopTime, Integer downloadSpeed,
                                         ErrorCallback<StreamInfo> callback){
        try {
            deviceChannelPlayService.download(channel, startTime, stopTime, downloadSpeed, callback);
        } catch (PlayException e) {
            callback.run(e.getCode(), e.getMsg(), null);
        } catch (Exception e) {
            callback.run(Response.BUSY_HERE, "busy here", null);
        }
    }


}
