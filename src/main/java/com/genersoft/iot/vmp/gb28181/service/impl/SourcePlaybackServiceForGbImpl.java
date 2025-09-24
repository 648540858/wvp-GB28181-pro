package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.CommonRecordInfo;
import com.genersoft.iot.vmp.gb28181.bean.PlayException;
import com.genersoft.iot.vmp.gb28181.bean.RecordItem;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IPlayService;
import com.genersoft.iot.vmp.gb28181.service.ISourcePlaybackService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sip.message.Response;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service(ChannelDataType.PLAYBACK_SERVICE + ChannelDataType.GB28181)
public class SourcePlaybackServiceForGbImpl implements ISourcePlaybackService {

    @Autowired
    private IPlayService playService;

    @Autowired
    private IDeviceChannelService channelService;

    @Override
    public void playback(CommonGBChannel channel, Long startTime, Long stopTime, ErrorCallback<StreamInfo> callback) {
        try {
            playService.playBack(channel, startTime, stopTime, callback);
        } catch (PlayException e) {
            callback.run(e.getCode(), e.getMsg(), null);
        } catch (Exception e) {
            callback.run(Response.BUSY_HERE, "busy here", null);
        }
    }

    @Override
    public void stopPlayback(CommonGBChannel channel, String stream) {
        // 国标通道
        try {
            playService.stop(InviteSessionType.PLAYBACK, channel, stream);
        }  catch (Exception e) {
            log.error("[停止点播失败] {}({})", channel.getGbName(), channel.getGbDeviceId(), e);
        }
    }

    @Override
    public void playbackPause(CommonGBChannel channel, String stream) {
        // 国标通道
        try {
            playService.playbackPause(stream);
        }  catch (Exception e) {
            log.error("[停止点播失败] {}({})", channel.getGbName(), channel.getGbDeviceId(), e);
        }
    }

    @Override
    public void playbackResume(CommonGBChannel channel, String stream) {
        // 国标通道
        try {
            playService.playbackPause(stream);
        }  catch (Exception e) {
            log.error("[停止点播失败] {}({})", channel.getGbName(), channel.getGbDeviceId(), e);
        }
    }

    @Override
    public void playbackSeek(CommonGBChannel channel, String stream, long seekTime) {
        // 国标通道
        try {
            playService.playbackPause(stream);
        }  catch (Exception e) {
            log.error("[停止点播失败] {}({})", channel.getGbName(), channel.getGbDeviceId(), e);
        }
    }

    @Override
    public void playbackSpeed(CommonGBChannel channel, String stream, Double speed) {
        // 国标通道
        try {
            playService.playbackSpeed(stream, speed);
        }  catch (Exception e) {
            log.error("[停止点播失败] {}({})", channel.getGbName(), channel.getGbDeviceId(), e);
        }
    }

    @Override
    public void queryRecord(CommonGBChannel channel, String startTime, String endTime, ErrorCallback<List<CommonRecordInfo>> callback) {
        channelService.queryRecordInfo(channel, startTime, endTime, (code, msg, data) -> {
            if (code == ErrorCode.SUCCESS.getCode()) {
                List<RecordItem> recordList = data.getRecordList();
                List<CommonRecordInfo> recordInfoList = new ArrayList<>();
                for (RecordItem recordItem : recordList) {
                    CommonRecordInfo recordInfo = new CommonRecordInfo();
                    recordInfo.setStartTime(recordItem.getStartTime());
                    recordInfo.setEndTime(recordItem.getEndTime());
                    recordInfo.setFileSize(recordItem.getFileSize());
                    recordInfoList.add(recordInfo);
                }
                callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), recordInfoList);
            }else {
                callback.run(code, msg, null);
            }
        });
    }
}
