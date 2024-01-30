package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.gb28181.bean.DragZoomRequest;
import com.genersoft.iot.vmp.gb28181.bean.RecordInfo;
import com.genersoft.iot.vmp.gb28181.bean.command.PTZCommand;

import java.time.Instant;

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
    void startPlayback(CommonGbChannel channel, Instant startTime, Instant stopTime, IResourcePlayCallback callback);

    /**
     * 录像下载
     */
    void startDownload(CommonGbChannel channel, Instant startTime, Instant stopTime, Integer downloadSpeed, IResourcePlayCallback playCallback);

    /**
     * 报警复位
     */
    void resetAlarm(CommonGbChannel commonGbChannel, Integer alarmMethod, Integer alarmType);

    /**
     * 布防/撤防
     */
    void setGuard(CommonGbChannel commonGbChannel, boolean setGuard);

    /**
     * 录像控制
     */
    void setRecord(CommonGbChannel commonGbChannel, Boolean isRecord);

    /**
     * 强制关键帧
     */
    void setIFame(CommonGbChannel commonGbChannel);

    /**
     * 重启
     */
    void setTeleBoot(CommonGbChannel commonGbChannel);

    /**
     * 拉框放大/缩小
     */
    void dragZoom(CommonGbChannel commonGbChannel, DragZoomRequest.DragZoom dragZoom, boolean isIn);

    /**
     * 看守位控制
     */
    void setHomePosition(CommonGbChannel commonGbChannel, boolean enabled, Integer resetTime, Integer presetIndex);


    void queryrecord(CommonGbChannel commonGbChannel, int sn, int secrecy, String type,
                     String startTime, String endTime, CommonCallback<RecordInfo> callback);
}
