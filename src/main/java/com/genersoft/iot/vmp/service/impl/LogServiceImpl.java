package com.genersoft.iot.vmp.service.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.rolling.RollingFileAppender;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.service.ILogService;
import com.genersoft.iot.vmp.service.bean.LogFileInfo;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class LogServiceImpl implements ILogService {

    @Override
    public List<LogFileInfo> queryList(String query, String startTime, String endTime) {
        File logFile = getLogDir();
        if (logFile == null && !logFile.exists()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "获取日志文件目录失败");
        }
        File[] files = logFile.listFiles();
        List<LogFileInfo> result = new ArrayList<>();
        if (files == null || files.length == 0) {
            return result;
        }
        for (File file : files) {
            LogFileInfo logFileInfo = new LogFileInfo();
            logFileInfo.setFileName(file.getName());
            if (query != null && !file.getName().contains(query)) {
                continue;
            }
            // 读取文件创建时间作为开始时间，修改时间为结束时间

            Long startTimestamp = null;
            if (startTime != null) {
                startTimestamp = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime);
            }
            Long endTimestamp = null;
            if (startTime != null) {
                endTimestamp = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime);
            }
            try {
                String[] fileAttributes = getFileAttributes(file);
                if (fileAttributes == null) {
                    continue;
                }
                logFileInfo.setStartTime(fileAttributes[0]);
                logFileInfo.setEndTime(fileAttributes[1]);
                if (startTimestamp != null && startTimestamp > DateUtil.yyyy_MM_dd_HH_mm_ssToTimestampMs(fileAttributes[0])) {
                    continue;
                }
                if (endTimestamp != null && endTimestamp < DateUtil.yyyy_MM_dd_HH_mm_ssToTimestampMs(fileAttributes[1])) {
                    continue;
                }
            } catch (IOException e) {
                log.error("[读取日志文件列表] 获取创建时间和修改时间失败", e);
                continue;
            }
            result.add(logFileInfo);

        }
        return result;
    }

    private File getLogDir() {
        Logger logger = (Logger) LoggerFactory.getLogger("com.genersoft.iot.vmp");
        RollingFileAppender rollingFileAppender = (RollingFileAppender) logger.getAppender("RollingFile");
        File rollingFile = new File(rollingFileAppender.getFile());
        return rollingFile.getParentFile();
    }

    String[] getFileAttributes(File file) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        String startLine = bufferedReader.readLine();
        if (startLine== null) {
           return null;
        }
        String startTime = startLine.substring(0, 19);


        String lastLine = "";
        try (ReversedLinesFileReader reversedLinesReader = new ReversedLinesFileReader(file, Charset.defaultCharset())) {
            lastLine = reversedLinesReader.readLine();
        } catch (Exception e) {
            log.error("file read error, msg:{}", e.getMessage(), e);
        }
        String endTime = lastLine.substring(0, 19);
        return new String[]{startTime, endTime};
    }

    @Override
    public File getFileByName(String fileName) {
        File logDir = getLogDir();

        return new File(logDir, fileName);
    }
}
