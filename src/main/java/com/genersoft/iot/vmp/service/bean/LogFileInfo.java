package com.genersoft.iot.vmp.service.bean;

import lombok.Data;

@Data
public class LogFileInfo {

    private String fileName;
    private String startTime;
    private String endTime;

    public static LogFileInfo getInstance(String fileName, String startTime, String endTime) {
        LogFileInfo logFileInfo = new LogFileInfo();
        logFileInfo.setFileName(fileName);
        logFileInfo.setStartTime(startTime);
        logFileInfo.setEndTime(endTime);
        return logFileInfo;
    }

}
