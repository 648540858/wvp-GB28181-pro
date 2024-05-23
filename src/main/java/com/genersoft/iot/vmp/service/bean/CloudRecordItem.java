package com.genersoft.iot.vmp.service.bean;

import com.genersoft.iot.vmp.media.event.media.MediaRecordMp4Event;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnRecordMp4HookParam;
import com.genersoft.iot.vmp.utils.MediaServerUtils;

import java.util.Map;

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
    private long timeLen;

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
        cloudRecordItem.setTimeLen((long) param.getRecordInfo().getTimeLen() * 1000);
        cloudRecordItem.setEndTime((param.getRecordInfo().getStartTime() + (long)param.getRecordInfo().getTimeLen()) * 1000);
        Map<String, String> paramsMap = MediaServerUtils.urlParamToMap(param.getParams());
        if (paramsMap.get("callId") != null) {
            cloudRecordItem.setCallId(paramsMap.get("callId"));
        }
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

    public Boolean getCollect() {
        return collect;
    }

    public void setCollect(Boolean collect) {
        this.collect = collect;
    }

    public Boolean getReserve() {
        return reserve;
    }

    public void setReserve(Boolean reserve) {
        this.reserve = reserve;
    }
}
