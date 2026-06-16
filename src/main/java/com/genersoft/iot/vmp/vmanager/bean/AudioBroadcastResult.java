package com.genersoft.iot.vmp.vmanager.bean;

import lombok.Getter;
import lombok.Setter;

/**
 * @author lin
 */
@Setter
@Getter
public class AudioBroadcastResult {
    /**
     * 推流的各个方式流地址
     */
    private StreamContent streamInfo;

    /**
     * 编码格式
     */
    private String codec;

    /**
     * 向zlm推流的应用名
     */
    private String app;

    /**
     * 向zlm推流的流ID
     */
    private String stream;

    /**
     * 播放流地址（设备音频通过ZLM播放给浏览器），对讲时设置
     */
    private StreamContent playStreamInfo;


}
