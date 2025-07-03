package com.genersoft.iot.vmp.media.event.media;

import com.genersoft.iot.vmp.media.abl.ABLHttpHookListener;
import com.genersoft.iot.vmp.media.abl.bean.hook.OnRecordProgressABLHookParam;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.utils.DateUtil;

import java.util.Date;

/**
 * 录像文件进度通知事件
 */
public class MediaRecordProcessEvent extends MediaEvent {

    private Integer currentFileDuration;
    private Integer TotalVideoDuration;
    private String fileName;
    private long startTime;
    private long endTime;

    public MediaRecordProcessEvent(Object source) {
        super(source);
    }

    public static MediaRecordProcessEvent getInstance(ABLHttpHookListener source, OnRecordProgressABLHookParam hookParam, MediaServer mediaServer) {
        MediaRecordProcessEvent mediaRecordMp4Event = new MediaRecordProcessEvent(source);
        mediaRecordMp4Event.setApp(hookParam.getApp());
        mediaRecordMp4Event.setStream(hookParam.getStream());
        mediaRecordMp4Event.setCurrentFileDuration(hookParam.getCurrentFileDuration());
        mediaRecordMp4Event.setTotalVideoDuration(hookParam.getTotalVideoDuration());
        mediaRecordMp4Event.setMediaServer(mediaServer);
        mediaRecordMp4Event.setFileName(hookParam.getFileName());
        mediaRecordMp4Event.setStartTime(DateUtil.urlToTimestampMs(hookParam.getStartTime()));
        mediaRecordMp4Event.setEndTime(DateUtil.urlToTimestampMs(hookParam.getEndTime()));
        return mediaRecordMp4Event;
    }

    public Integer getCurrentFileDuration() {
        return currentFileDuration;
    }

    public void setCurrentFileDuration(Integer currentFileDuration) {
        this.currentFileDuration = currentFileDuration;
    }

    public Integer getTotalVideoDuration() {
        return TotalVideoDuration;
    }

    public void setTotalVideoDuration(Integer totalVideoDuration) {
        TotalVideoDuration = totalVideoDuration;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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
}
