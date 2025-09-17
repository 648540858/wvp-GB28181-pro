package com.genersoft.iot.vmp.media.bean;

import com.genersoft.iot.vmp.media.abl.bean.hook.OnRecordMp4ABLHookParam;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnRecordMp4HookParam;
import com.genersoft.iot.vmp.service.bean.CloudRecordItem;
import com.genersoft.iot.vmp.utils.DateUtil;
import lombok.Data;

@Data
public class RecordInfo {
    private String app;
    private String stream;
    private String fileName;
    private String filePath;
    private long fileSize;
    private String folder;
    private String url;
    /**
     * 单位毫秒
     */
    private long startTime;
    /**
     * 单位毫秒
     */
    private double timeLen;
    private String params;

    public static RecordInfo getInstance(OnRecordMp4HookParam hookParam) {
        RecordInfo recordInfo = new RecordInfo();
        recordInfo.setApp(hookParam.getApp());
        recordInfo.setStream(hookParam.getStream());
        recordInfo.setFileName(hookParam.getFile_name());
        recordInfo.setUrl(hookParam.getUrl());
        recordInfo.setFolder(hookParam.getFolder());
        recordInfo.setFilePath(hookParam.getFile_path());
        recordInfo.setFileSize(hookParam.getFile_size());
        recordInfo.setStartTime(hookParam.getStart_time() * 1000);
        recordInfo.setTimeLen(hookParam.getTime_len() * 1000);
        return recordInfo;
    }

    public static RecordInfo getInstance(OnRecordMp4ABLHookParam hookParam) {
        RecordInfo recordInfo = new RecordInfo();
        recordInfo.setApp(hookParam.getApp());
        recordInfo.setStream(hookParam.getStream());
        recordInfo.setFileName(hookParam.getFileName());
        recordInfo.setStartTime(DateUtil.urlToTimestampMs(hookParam.getStartTime()));
        recordInfo.setTimeLen(DateUtil.urlToTimestampMs(hookParam.getEndTime()) - recordInfo.getStartTime());
        recordInfo.setFileSize(hookParam.getFileSize());
        return recordInfo;
    }

    public static RecordInfo getInstance(CloudRecordItem cloudRecordItem) {
        RecordInfo recordInfo = new RecordInfo();
        recordInfo.setApp(cloudRecordItem.getApp());
        recordInfo.setStream(cloudRecordItem.getStream());
        recordInfo.setFileName(cloudRecordItem.getFileName());
        recordInfo.setStartTime(cloudRecordItem.getStartTime());
        recordInfo.setTimeLen(cloudRecordItem.getTimeLen());
        recordInfo.setFileSize(cloudRecordItem.getFileSize());
        recordInfo.setFilePath(cloudRecordItem.getFilePath());
        return recordInfo;
    }

    @Override
    public String toString() {
        return "RecordInfo{" +
                "文件名称='" + fileName + '\'' +
                ", 文件路径='" + filePath + '\'' +
                ", 文件大小=" + fileSize +
                ", 开始时间=" + startTime +
                ", 时长=" + timeLen +
                ", params=" + params +
                '}';
    }
}
