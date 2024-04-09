package com.genersoft.iot.vmp.media.bean;

import com.genersoft.iot.vmp.media.abl.bean.hook.OnRecordMp4ABLHookParam;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnRecordMp4HookParam;

public class RecordInfo {
    private String fileName;
    private String filePath;
    private long fileSize;
    private String folder;
    private String url;
    private long startTime;
    private double timeLen;

    public static RecordInfo getInstance(OnRecordMp4HookParam hookParam) {
        RecordInfo recordInfo = new RecordInfo();
        recordInfo.setFileName(hookParam.getFile_name());
        recordInfo.setUrl(hookParam.getUrl());
        recordInfo.setFolder(hookParam.getFolder());
        recordInfo.setFilePath(hookParam.getFile_path());
        recordInfo.setFileSize(hookParam.getFile_size());
        recordInfo.setStartTime(hookParam.getStart_time());
        recordInfo.setTimeLen(hookParam.getTime_len());
        return recordInfo;
    }

    public static RecordInfo getInstance(OnRecordMp4ABLHookParam hookParam) {
        RecordInfo recordInfo = new RecordInfo();
        recordInfo.setFileName(hookParam.getFileName());
        return recordInfo;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public double getTimeLen() {
        return timeLen;
    }

    public void setTimeLen(double timeLen) {
        this.timeLen = timeLen;
    }

    @Override
    public String toString() {
        return "RecordInfo{" +
                "文件名称='" + fileName + '\'' +
                ", 文件路径='" + filePath + '\'' +
                ", 文件大小=" + fileSize +
                ", 开始时间=" + startTime +
                ", 时长=" + timeLen +
                '}';
    }
}
