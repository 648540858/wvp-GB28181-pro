package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.command.PTZCommand;

/**
 * 同用资源接入接口，待接入的资源实现此接口即可自动接入，
 * 包括GIS，分屏播放，国标级联等功能
 */
public interface IResourceService {


    /**
     * 通知资源类通道删除
     */
    boolean deleteChannel(CommonGbChannel commonGbChannel);

    /**
     * 开始播放通道
     */
    void startPlay(CommonGbChannel commonGbChannel, IResourcePlayCallback callback);

    /**
     * 停止播放通道
     */
    void stopPlay(CommonGbChannel commonGbChannel, IResourcePlayCallback callback);


    /**
     * 云台控制
     */
    boolean ptzControl(CommonGbChannel commonGbChannel, PTZCommand ptzCommand);

    /**
     * 流离线
     */
    void streamOffline(String app, String streamId);

    /**
     * 录像回放
     */
    void startPlayback(CommonGbChannel channel, Long startTime, Long stopTime, IResourcePlayCallback callback);

    /**
     * 录像下载
     */
    void startDownload(CommonGbChannel channel, Long startTime, Long stopTime, Integer downloadSpeed, IResourcePlayCallback playCallback);

    /**
     * 报警复位
     */
    void resetAlarm(CommonGbChannel commonGbChannel, Integer alarmMethod, Integer alarmType);
}
