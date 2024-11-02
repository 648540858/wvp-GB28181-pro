package com.genersoft.iot.vmp.vmanager.log;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.rolling.RollingFileAppender;
import com.alibaba.fastjson2.JSONArray;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.service.ICloudRecordService;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.ILogService;
import com.genersoft.iot.vmp.service.bean.CloudRecordItem;
import com.genersoft.iot.vmp.service.bean.DownloadFileInfo;
import com.genersoft.iot.vmp.service.bean.LogFileInfo;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.cloudRecord.bean.CloudRecordUrl;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@SuppressWarnings("rawtypes")
@Tag(name = "日志文件查询接口")
@Slf4j
@RestController
@RequestMapping("/api/log")
public class LogController {

    @Autowired
    private ILogService logService;


    @ResponseBody
    @GetMapping("/list")
    @Operation(summary = "分页查询日志文件", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "query", description = "检索内容", required = false)
    @Parameter(name = "startTime", description = "开始时间(yyyy-MM-dd HH:mm:ss)", required = false)
    @Parameter(name = "endTime", description = "结束时间(yyyy-MM-dd HH:mm:ss)", required = false)
    public List<LogFileInfo> queryList(@RequestParam(required = false) String query, @RequestParam(required = false) String startTime, @RequestParam(required = false) String endTime

    ) {
        return logService.queryList(query, startTime, endTime);
    }

    /**
     * 下载指定日志文件
     */
    @ResponseBody
    @GetMapping("/file")
    public void downloadFile(HttpServletResponse response, @RequestParam(required = true) String fileName) {
        try {
            File file = logService.getFileByName(fileName);
            if (file == null || !file.exists() || !file.isFile()) {
                throw new ControllerException(ErrorCode.ERROR400);
            }
            final InputStream in = Files.newInputStream(file.toPath());
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            ServletOutputStream outputStream = response.getOutputStream();
            IOUtils.copy(in, response.getOutputStream());
            in.close();
            outputStream.close();
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

}
