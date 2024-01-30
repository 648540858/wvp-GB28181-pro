package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.gb28181.bean.DragZoomRequest;
import com.genersoft.iot.vmp.gb28181.bean.RecordInfo;
import com.genersoft.iot.vmp.gb28181.bean.command.PTZCommand;
import com.genersoft.iot.vmp.service.IResourcePlayCallback;
import com.genersoft.iot.vmp.service.IResourceService;
import com.genersoft.iot.vmp.service.bean.CommonGbChannelType;
import org.springframework.stereotype.Service;

@Service(CommonGbChannelType.PUSH)
public class StreamPushResourceServiceImpl implements IResourceService {
    @Override
    public boolean deleteChannel(CommonGbChannel commonGbChannel) {
        return false;
    }

    @Override
    public void startPlay(CommonGbChannel commonGbChannel, IResourcePlayCallback callback) {

    }

    @Override
    public void stopPlay(CommonGbChannel commonGbChannel, IResourcePlayCallback callback) {

    }

    @Override
    public boolean ptzControl(CommonGbChannel commonGbChannel, PTZCommand ptzCommand) {
        return false;
    }

    @Override
    public void streamOffline(String app, String streamId) {

    }

    @Override
    public void startPlayback(CommonGbChannel channel, Long startTime, Long stopTime, IResourcePlayCallback callback) {

    }

    @Override
    public void startDownload(CommonGbChannel channel, Long startTime, Long stopTime, Integer downloadSpeed, IResourcePlayCallback playCallback) {

    }

    @Override
    public void resetAlarm(CommonGbChannel commonGbChannel, Integer alarmMethod, Integer alarmType) {

    }

    @Override
    public void setGuard(CommonGbChannel commonGbChannel, boolean setGuard) {

    }

    @Override
    public void setRecord(CommonGbChannel commonGbChannel, Boolean isRecord) {

    }

    @Override
    public void setIFame(CommonGbChannel commonGbChannel) {

    }

    @Override
    public void setTeleBoot(CommonGbChannel commonGbChannel) {

    }

    @Override
    public void dragZoom(CommonGbChannel commonGbChannel, DragZoomRequest.DragZoom dragZoom, boolean isIn) {

    }

    @Override
    public void setHomePosition(CommonGbChannel commonGbChannel, boolean enabled, Integer resetTime, Integer presetIndex) {

    }

    @Override
    public void queryrecord(CommonGbChannel commonGbChannel, int sn, int secrecy, String type, String startTime, String endTime, CommonCallback<RecordInfo> callback) {

    }
}
