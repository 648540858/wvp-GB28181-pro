package com.genersoft.iot.vmp.service.bean;

import com.genersoft.iot.vmp.media.zlm.dto.hook.OnRecordMp4HookParam;

/**
 * 云端录像数据
 */
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
     * 收藏类型，收藏的文件不移除
     */
    private String collectType;
    
    /**
     * 文件大小
     */
    private long fileSize;
    
    /**
     * 文件时长
     */
    private long timeLen;

    public static CloudRecordItem getInstance(OnRecordMp4HookParam param) {
        CloudRecordItem cloudRecordItem = new CloudRecordItem();
        cloudRecordItem.setApp(param.getApp());
        cloudRecordItem.setStream(param.getStream());
        cloudRecordItem.setStartTime(param.getStart_time());
        cloudRecordItem.setFileName(param.getFile_name());
        cloudRecordItem.setFolder(param.getFolder());
        cloudRecordItem.setFileSize(param.getFile_size());
        cloudRecordItem.setFilePath(param.getFile_path());
        cloudRecordItem.setMediaServerId(param.getMediaServerId());
        cloudRecordItem.setTimeLen(param.getTime_len());
        cloudRecordItem.setEndTime(param.getStart_time() + param.getTime_len());
        return cloudRecordItem;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getMediaServerId() {
        return mediaServerId;
    }

    public void setMediaServerId(String mediaServerId) {
        this.mediaServerId = mediaServerId;
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

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getTimeLen() {
        return timeLen;
    }

    public void setTimeLen(long timeLen) {
        this.timeLen = timeLen;
    }

    public String getCollectType() {
        return collectType;
    }

    public void setCollectType(String collectType) {
        this.collectType = collectType;
    }
}
