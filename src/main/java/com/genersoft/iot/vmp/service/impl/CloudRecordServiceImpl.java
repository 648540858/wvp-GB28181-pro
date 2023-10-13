package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnRecordMp4HookParam;
import com.genersoft.iot.vmp.service.ICloudRecordService;
import com.genersoft.iot.vmp.service.bean.CloudRecordItem;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.dao.CloudRecordServiceMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataUnit;

import java.time.*;
import java.time.temporal.TemporalAccessor;
import java.util.*;

@Service
public class CloudRecordServiceImpl implements ICloudRecordService {

    private final static Logger logger = LoggerFactory.getLogger(CloudRecordServiceImpl.class);

    @Autowired
    private CloudRecordServiceMapper cloudRecordServiceMapper;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Override
    public PageInfo<CloudRecordItem> getList(int page, int count, String app, String stream, String startTime, String endTime, List<MediaServerItem> mediaServerItems) {
        // 开始时间和结束时间在数据库中都是以秒为单位的
        Long startTimeStamp = null;
        Long endTimeStamp = null;
        if (startTime != null ) {
            if (!DateUtil.verification(startTime, DateUtil.formatter)) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "开始时间格式错误，正确格式为： " + DateUtil.formatter);
            }
            startTimeStamp = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(startTime);

        }
        if (endTime != null ) {
            if (!DateUtil.verification(endTime, DateUtil.formatter)) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "结束时间格式错误，正确格式为： " + DateUtil.formatter);
            }
            endTimeStamp = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestamp(endTime);

        }
        PageHelper.startPage(page, count);
        List<CloudRecordItem> all = cloudRecordServiceMapper.getList(app, stream, startTimeStamp, endTimeStamp, mediaServerItems);
        return new PageInfo<>(all);
    }

    @Override
    public List<String> getDateList(String app, String stream, int year, int month, List<MediaServerItem> mediaServerItems) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate;
        if (month == 12) {
            endDate = LocalDate.of(year + 1, 1, 1);
        }else {
            endDate = LocalDate.of(year, month + 1, 1);
        }
        long startTimeStamp = startDate.atStartOfDay().toInstant(ZoneOffset.ofHours(8)).getEpochSecond();
        long endTimeStamp = endDate.atStartOfDay().toInstant(ZoneOffset.ofHours(8)).getEpochSecond();
        List<CloudRecordItem> cloudRecordItemList = cloudRecordServiceMapper.getList(app, stream, startTimeStamp, endTimeStamp, mediaServerItems);
        if (cloudRecordItemList.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> resultSet = new HashSet<>();
        cloudRecordItemList.stream().forEach(cloudRecordItem -> {
            String date = DateUtil.timestampTo_yyyy_MM_dd(cloudRecordItem.getStartTime());
            resultSet.add(date);
        });
        return new ArrayList<>(resultSet);
    }

    @Override
    public void addRecord(OnRecordMp4HookParam param) {
        CloudRecordItem  cloudRecordItem = CloudRecordItem.getInstance(param);
        StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(param.getApp(), param.getStream());
        if (streamAuthorityInfo != null) {
            cloudRecordItem.setCallId(streamAuthorityInfo.getCallId());
        }
        logger.info("[添加录像记录] {}/{} 文件大小：{}", param.getApp(), param.getStream(), param.getFile_size());
        cloudRecordServiceMapper.add(cloudRecordItem);
    }
}
