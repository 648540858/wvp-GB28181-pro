package com.genersoft.iot.vmp.service.bean;

import com.genersoft.iot.vmp.media.event.media.MediaRecordMp4Event;
import com.genersoft.iot.vmp.utils.MediaServerUtils;
import lombok.Data;

import java.util.Map;

/**
 * 云端录像数据
 */
@Data
public class CloudRecordItem {
    /**
     * 主键
     */
    private int id;

    /**
     * 应用名
     */
    private String app;

    /**
     * 流
     */
    private String stream;

    /**
     * 健全ID
     */
    private String callId;

    /**
     * 开始时间
     */
    private long startTime;

    /**
     * 结束时间
     */
    private long endTime;

    /**
     * ZLM Id
     */
    private String mediaServerId;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件夹
     */
    private String folder;

    /**
     * 收藏，收藏的文件不移除
     */
    private Boolean collect;

    /**
     * 保留，收藏的文件不移除
     */
    private Boolean reserve;

    /**
     * 文件大小
     */
    private long fileSize;

    /**
     * 文件时长
     */
    private double timeLen;

    /**
     * 所属服务ID
     */
    private String serverId;

    public static CloudRecordItem getInstance(MediaRecordMp4Event param) {
        CloudRecordItem cloudRecordItem = new CloudRecordItem();
        cloudRecordItem.setApp(param.getApp());
        cloudRecordItem.setStream(param.getStream());
        cloudRecordItem.setStartTime(param.getRecordInfo().getStartTime()*1000);
        cloudRecordItem.setFileName(param.getRecordInfo().getFileName());
        cloudRecordItem.setFolder(param.getRecordInfo().getFolder());
        cloudRecordItem.setFileSize(param.getRecordInfo().getFileSize());
        cloudRecordItem.setFilePath(param.getRecordInfo().getFilePath());
        cloudRecordItem.setMediaServerId(param.getMediaServer().getId());
        cloudRecordItem.setTimeLen(param.getRecordInfo().getTimeLen() * 1000);
        cloudRecordItem.setEndTime((param.getRecordInfo().getStartTime() + (long)param.getRecordInfo().getTimeLen()) * 1000);
        Map<String, String> paramsMap = MediaServerUtils.urlParamToMap(param.getRecordInfo().getParams());
        if (paramsMap.get("callId") != null) {
            cloudRecordItem.setCallId(paramsMap.get("callId"));
        }
        return cloudRecordItem;
    }

}
