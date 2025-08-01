package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import com.genersoft.iot.vmp.gb28181.service.*;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sip.message.Response;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GbChannelPlayServiceImpl implements IGbChannelPlayService {

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private CommonGBChannelMapper channelMapper;

    @Autowired
    private Map<String, ISourcePlayService> sourcePlayServiceMap;

    @Autowired
    private Map<String, ISourcePlaybackService> sourcePlaybackServiceMap;

    @Autowired
    private Map<String, ISourceDownloadService> sourceDownloadServiceMap;


    @Override
    public void startInvite(CommonGBChannel channel, InviteMessageInfo inviteInfo, Platform platform, ErrorCallback<StreamInfo> callback) {
        if (channel == null || inviteInfo == null || callback == null || channel.getDataType() == null) {
            log.warn("[通用通道点播] 参数异常, channel: {}, inviteInfo: {}, callback: {}", channel != null, inviteInfo != null, callback != null);
            throw new PlayException(Response.SERVER_INTERNAL_ERROR, "server internal error");
        }
        log.info("[点播通用通道] 类型：{}， 通道： {}({})", inviteInfo.getSessionName(), channel.getGbName(), channel.getGbDeviceId());

        if ("Play".equalsIgnoreCase(inviteInfo.getSessionName())) {
            play(channel, platform, userSetting.getRecordSip(), callback);
        }else if ("Playback".equals(inviteInfo.getSessionName())) {
            playback(channel, inviteInfo.getStartTime(), inviteInfo.getStopTime(), callback);
        }else if ("Download".equals(inviteInfo.getSessionName())) {
            Integer downloadSpeed = Integer.parseInt(inviteInfo.getDownloadSpeed());
            // 国标通道
            download(channel, inviteInfo.getStartTime(), inviteInfo.getStopTime(), downloadSpeed, callback);
        }else {
            // 不支持的点播方式
            log.error("[点播通用通道] 不支持的点播方式：{}， {}({})", inviteInfo.getSessionName(), channel.getGbName(), channel.getGbDeviceId());
            throw new PlayException(Response.BAD_REQUEST, "bad request");
        }
    }

    @Override
    public void stopInvite(InviteSessionType type, CommonGBChannel channel, String stream) {
        switch (type) {
            case PLAY:
                stopPlay(channel, stream);
                break;
            case PLAYBACK:
                stopPlayback(channel, stream);
                break;
            case DOWNLOAD:
                stopDownload(channel, stream);
                break;
            default:
                // 通道数据异常
                log.error("[点播通用通道] 类型编号： {} 不支持此类型请求", type);
                throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
    }

    @Override
    public void play(CommonGBChannel channel, Platform platform, Boolean record, ErrorCallback<StreamInfo> callback) {
        log.info("[通用通道] 播放， 类型： {}， 编号：{}", channel.getDataType(), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourcePlayService sourceChannelPlayService = sourcePlayServiceMap.get(ChannelDataType.PLAY_SERVICE + dataType);
        if (sourceChannelPlayService == null) {
            // 通道数据异常
            log.error("[点播通用通道] 类型编号： {} 不支持实时流预览", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        sourceChannelPlayService.play(channel, platform, record, (code, msg, data) -> {
            if (code == InviteErrorCode.SUCCESS.getCode()) {
                // 将流ID记录到数据库
                if (channel.getDataType() != ChannelDataType.GB28181) {
                    channelMapper.updateStream(channel.getGbId(), data.getStream());
                }
            }
            callback.run(code, msg, data);
        });
    }
    @Override
    public void playback(CommonGBChannel channel, Long startTime, Long stopTime, ErrorCallback<StreamInfo> callback) {
        log.info("[通用通道] 回放， 类型： {}， 编号：{}", channel.getDataType(), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourcePlaybackService playbackService = sourcePlaybackServiceMap.get(ChannelDataType.PLAYBACK_SERVICE + dataType);
        if (playbackService == null) {
            // 通道数据异常
            log.error("[点播通用通道] 类型编号： {} 不支持回放", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        playbackService.playback(channel, startTime, stopTime, callback);
    }

    @Override
    public void download(CommonGBChannel channel, Long startTime, Long stopTime, Integer downloadSpeed,
                         ErrorCallback<StreamInfo> callback){
        log.info("[通用通道] 录像下载， 类型： {}， 编号：{}", channel.getDataType(), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourceDownloadService downloadService = sourceDownloadServiceMap.get(ChannelDataType.DOWNLOAD_SERVICE + dataType);
        if (downloadService == null) {
            // 通道数据异常
            log.error("[点播通用通道] 类型编号： {} 不支持录像下载", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        downloadService.download(channel, startTime, stopTime, downloadSpeed, callback);
    }

    @Override
    public void stopPlay(CommonGBChannel channel, String stream) {
        Integer dataType = channel.getDataType();
        ISourcePlayService sourceChannelPlayService = sourcePlayServiceMap.get(ChannelDataType.PLAY_SERVICE + dataType);
        if (sourceChannelPlayService == null) {
            // 通道数据异常
            log.error("[点播通用通道] 类型编号： {} 不支持停止实时流", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        sourceChannelPlayService.stopPlay(channel, stream);
    }

    @Override
    public void stopPlayback(CommonGBChannel channel, String stream) {
        log.info("[通用通道] 停止回放， 类型： {}， 编号：{}", channel.getDataType(), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourcePlaybackService playbackService = sourcePlaybackServiceMap.get(ChannelDataType.PLAYBACK_SERVICE + dataType);
        if (playbackService == null) {
            // 通道数据异常
            log.error("[点播通用通道] 类型编号： {} 不支持回放", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        playbackService.stopPlayback(channel, stream);
    }

    @Override
    public void stopDownload(CommonGBChannel channel, String stream) {
        log.info("[通用通道] 停止录像下载， 类型： {}， 编号：{} stream: {}", channel.getDataType(), channel.getGbDeviceId(), stream);
        Integer dataType = channel.getDataType();
        ISourceDownloadService downloadService = sourceDownloadServiceMap.get(ChannelDataType.DOWNLOAD_SERVICE + dataType);
        if (downloadService == null) {
            // 通道数据异常
            log.error("[点播通用通道] 类型编号： {} 不支持录像下载", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        downloadService.stopDownload(channel, stream);
    }

    @Override
    public void playbackPause(CommonGBChannel channel, String stream) {
        log.info("[通用通道] 回放暂停， 类型： {}， 编号：{} stream：{}", channel.getDataType(), channel.getGbDeviceId(), stream);
        Integer dataType = channel.getDataType();
        ISourcePlaybackService playbackService = sourcePlaybackServiceMap.get(ChannelDataType.PLAYBACK_SERVICE + dataType);
        if (playbackService == null) {
            // 通道数据异常
            log.error("[点播通用通道] 类型编号： {} 不支持回放暂停", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        playbackService.playbackPause(channel, stream);
    }

    @Override
    public void playbackResume(CommonGBChannel channel, String stream) {
        log.info("[通用通道] 回放暂停恢复， 类型： {}， 编号：{} stream：{}", channel.getDataType(), channel.getGbDeviceId(), stream);
        Integer dataType = channel.getDataType();
        ISourcePlaybackService playbackService = sourcePlaybackServiceMap.get(ChannelDataType.PLAYBACK_SERVICE + dataType);
        if (playbackService == null) {
            // 通道数据异常
            log.error("[点播通用通道] 类型编号： {} 不支持回放暂停恢复", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        playbackService.playbackResume(channel, stream);
    }

    @Override
    public void playbackSeek(CommonGBChannel channel, String stream, long seekTime) {
        log.info("[通用通道] 回放拖动播放， 类型： {}， 编号：{} stream：{}", channel.getDataType(), channel.getGbDeviceId(), stream);
        Integer dataType = channel.getDataType();
        ISourcePlaybackService playbackService = sourcePlaybackServiceMap.get(ChannelDataType.PLAYBACK_SERVICE + dataType);
        if (playbackService == null) {
            // 通道数据异常
            log.error("[点播通用通道] 类型编号： {} 不支持回放暂停恢复", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        playbackService.playbackSeek(channel, stream, seekTime);
    }

    @Override
    public void playbackSpeed(CommonGBChannel channel, String stream, Double speed) {
        log.info("[通用通道] 回放倍速播放， 类型： {}， 编号：{} stream：{}", channel.getDataType(), channel.getGbDeviceId(), stream);
        Integer dataType = channel.getDataType();
        ISourcePlaybackService playbackService = sourcePlaybackServiceMap.get(ChannelDataType.PLAYBACK_SERVICE + dataType);
        if (playbackService == null) {
            // 通道数据异常
            log.error("[点播通用通道] 类型编号： {} 不支持回放暂停恢复", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        playbackService.playbackSpeed(channel, stream, speed);
    }

    @Override
    public void queryRecord(CommonGBChannel channel, String startTime, String endTime, ErrorCallback<List<CommonRecordInfo>> callback) {
        log.info("[通用通道] 录像查询， 类型： {}， 编号：{}", channel.getDataType(), channel.getGbDeviceId());
        Integer dataType = channel.getDataType();
        ISourcePlaybackService playbackService = sourcePlaybackServiceMap.get(ChannelDataType.PLAYBACK_SERVICE + dataType);
        if (playbackService == null) {
            // 通道数据异常
            log.error("[点播通用通道] 类型编号： {} 不支持回放暂停恢复", dataType);
            throw new PlayException(Response.BUSY_HERE, "channel not support");
        }
        playbackService.queryRecord(channel, startTime, endTime, callback);
    }
}
