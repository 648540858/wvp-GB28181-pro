package com.genersoft.iot.vmp.media.bean;

import com.genersoft.iot.vmp.media.zlm.dto.hook.OnRecordMp4HookParam;
import lombok.Data;

@Data
public class RecordInfo {
    private String fileName;
    private String filePath;
    private long fileSize;
    private String folder;
    private String url;
    private long startTime;
    private double timeLen;
    private String params;

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
