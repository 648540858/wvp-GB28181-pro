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
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class LogServiceImpl implements ILogService {

    @Override
    public List<LogFileInfo> queryList(String query, String startTime, String endTime) {
        File logFile = getLogDir();
        if (logFile == null || !logFile.exists()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "获取日志文件目录失败");
        }
        File[] files = logFile.listFiles();
        List<LogFileInfo> result = new ArrayList<>();
        if (files == null || files.length == 0) {
            return result;
        }

        // 读取文件创建时间作为开始时间，修改时间为结束时间
        Long startTimestamp = null;
        if (startTime != null) {
            startTimestamp = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestampMs(startTime);
        }
        Long endTimestamp = null;
        if (endTime != null) {
            endTimestamp = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestampMs(endTime);
        }
        for (File file : files) {
            LogFileInfo logFileInfo = new LogFileInfo();
            logFileInfo.setFileName(file.getName());
            logFileInfo.setFileSize(file.length());
            if (query != null && !file.getName().contains(query)) {
                continue;
            }
            try {
                Long[] fileAttributes = getFileAttributes(file);
                if (fileAttributes == null) {
                    continue;
                }
                long startTimestampForFile  = fileAttributes[0];
                long endTimestampForFile  = fileAttributes[1];
                logFileInfo.setStartTime(startTimestampForFile);
                logFileInfo.setEndTime(endTimestampForFile);
                if (startTimestamp != null && startTimestamp > startTimestampForFile) {
                    continue;
                }
                if (endTimestamp != null && endTimestamp < endTimestampForFile) {
                    continue;
                }
            } catch (IOException e) {
                log.error("[读取日志文件列表] 获取创建时间和修改时间失败", e);
                continue;
            }
            result.add(logFileInfo);

        }
        result.sort((o1, o2) -> o2.getStartTime().compareTo(o1.getStartTime()));
        return result;
    }

    private File getLogDir() {
        Logger logger = (Logger) LoggerFactory.getLogger("root");
        RollingFileAppender rollingFileAppender = (RollingFileAppender) logger.getAppender("RollingFile");
        File rollingFile = new File(rollingFileAppender.getFile());
        return rollingFile.getParentFile();
    }

    Long[] getFileAttributes(File file) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        String startLine = bufferedReader.readLine();
        if (startLine== null) {
           return null;
        }
        String startTime = startLine.substring(0, 19);

        // 最后一行的开头不一定是时间
//        String lastLine = "";
//        try (ReversedLinesFileReader reversedLinesReader = new ReversedLinesFileReader(file, Charset.defaultCharset())) {
//            lastLine = reversedLinesReader.readLine();
//        } catch (Exception e) {
//            log.error("file read error, msg:{}", e.getMessage(), e);
//        }
//        String endTime = lastLine.substring(0, 19);
        return new Long[]{DateUtil.yyyy_MM_dd_HH_mm_ssToTimestampMs(startTime), file.lastModified()};
    }

    @Override
    public File getFileByName(String fileName) {
        File logDir = getLogDir();

        return new File(logDir, fileName);
    }
}
