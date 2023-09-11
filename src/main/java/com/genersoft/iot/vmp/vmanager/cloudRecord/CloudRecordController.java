package com.genersoft.iot.vmp.vmanager.cloudRecord;

import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.media.zlm.SendRtpPortManager;
import com.genersoft.iot.vmp.media.zlm.ZLMServerFactory;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.PageInfo;
import com.genersoft.iot.vmp.vmanager.bean.RecordFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@SuppressWarnings("rawtypes")
@Tag(name = "云端录像接口")

@RestController
@RequestMapping("/api/cloud/record")
public class CloudRecordController {

    @Autowired
    private ZLMServerFactory zlmServerFactory;

    @Autowired
    private SendRtpPortManager sendRtpPortManager;

    private final static Logger logger = LoggerFactory.getLogger(CloudRecordController.class);

    @Autowired
    private ZlmHttpHookSubscribe hookSubscribe;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @ResponseBody
    @GetMapping("/date/list")
    @Operation(summary = "查询存在云端录像的日期")
    @Parameter(name = "app", description = "应用名", required = true)
    @Parameter(name = "stream", description = "流ID", required = true)
    @Parameter(name = "year", description = "年，置空则查询当年", required = false)
    @Parameter(name = "month", description = "月，置空则查询当月", required = false)
    @Parameter(name = "mediaServerId", description = "流媒体ID，置空则查询全部", required = false)
    public List<String> openRtpServer(
            @RequestParam String app,
            @RequestParam String stream,
            @RequestParam(required = false) int year,
            @RequestParam(required = false) int month,
            @RequestParam(required = false) String mediaServerId

    ) {
        logger.info("[云端录像] 查询存在云端录像的日期 app->{}, stream->{}, mediaServerId->{}, year->{}, month->{}",
                app, stream, mediaServerId, year, month);
        Calendar calendar = Calendar.getInstance();
        if (ObjectUtils.isEmpty(year)) {
            year = calendar.get(Calendar.YEAR);
        }
        if (ObjectUtils.isEmpty(month)) {
            month = calendar.get(Calendar.MONTH) + 1;
        }
        List<MediaServerItem> mediaServerItems;
        if (!ObjectUtils.isEmpty(mediaServerId)) {
            mediaServerItems = new ArrayList<>();
            MediaServerItem mediaServerItem = mediaServerService.getOne(mediaServerId);
            if (mediaServerItem == null) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到流媒体: " + mediaServerId);
            }
            mediaServerItems.add(mediaServerItem);
        } else {
            mediaServerItems = mediaServerService.getAll();
        }
        if (mediaServerItems.isEmpty()) {
            return new ArrayList<>();
        }

        return mediaServerService.getRecordDates(app, stream, year, month, mediaServerItems);
    }

    @ResponseBody
    @GetMapping("/list")
    @Operation(summary = "分页查询云端录像")
    @Parameter(name = "app", description = "应用名", required = true)
    @Parameter(name = "stream", description = "流ID", required = true)
    @Parameter(name = "page", description = "当前页", required = false)
    @Parameter(name = "count", description = "每页查询数量", required = false)
    @Parameter(name = "startTime", description = "开始时间(yyyy-MM-dd HH:mm:ss)", required = true)
    @Parameter(name = "endTime", description = "结束时间(yyyy-MM-dd HH:mm:ss)", required = true)
    @Parameter(name = "mediaServerId", description = "流媒体ID，置空则查询全部流媒体", required = false)
    public PageInfo<RecordFile> openRtpServer(
            @RequestParam String app,
            @RequestParam String stream,
            @RequestParam int page,
            @RequestParam int count,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(required = false) String mediaServerId

    ) {
        logger.info("[云端录像] 查询 app->{}, stream->{}, mediaServerId->{}, page->{}, count->{}, startTime->{}, endTime->{}",
                app, stream, mediaServerId, page, count, startTime, endTime);

        List<MediaServerItem> mediaServerItems;
        if (!ObjectUtils.isEmpty(mediaServerId)) {
            mediaServerItems = new ArrayList<>();
            MediaServerItem mediaServerItem = mediaServerService.getOne(mediaServerId);
            if (mediaServerItem == null) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到流媒体: " + mediaServerId);
            }
            mediaServerItems.add(mediaServerItem);
        } else {
            mediaServerItems = mediaServerService.getAll();
        }
        if (mediaServerItems.isEmpty()) {
            return new PageInfo<>();
        }
        List<RecordFile> records = mediaServerService.getRecords(app, stream, startTime, endTime, mediaServerItems);
        PageInfo<RecordFile> pageInfo = new PageInfo<>(records);
        pageInfo.startPage(page, count);
        return pageInfo;
    }


}
