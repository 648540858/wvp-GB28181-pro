package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.common.StreamInfo;

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
     * @param commonGbChannel 通道
     * @param command 控制指令,允许值: left, right, up, down, upleft, upright, downleft, downright, zoomin, zoomout, stop
     * @param horizonSpeed 水平速度 0-255
     * @param verticalSpeed 垂直速度 0-255
     * @param zoomSpeed 缩放速度
     * @return 结果
     */
    boolean ptzControl(CommonGbChannel commonGbChannel, String command,
                       Integer horizonSpeed, Integer verticalSpeed, Integer zoomSpeed);

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
}
