package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.service.IAlarmService;
import com.genersoft.iot.vmp.service.bean.Alarm;
import com.genersoft.iot.vmp.service.bean.AlarmType;
import com.genersoft.iot.vmp.storager.dao.AlarmMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlarmServiceImpl implements IAlarmService {

    private final AlarmMapper alarmMapper;

    @Override
    public void saveAlarmInfo(Alarm alarm) {

    }

    @Override
    public PageInfo<Alarm> getAlarms(int page, int count, List<AlarmType> alarmType, String beginTime, String endTime) {
        PageHelper.startPage(page, count);
        Long beginTimeLong = null;
        Long endTimeLong = null;
        if (beginTime != null) {
            beginTimeLong = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestampMs(beginTime);
        }
        if (endTime != null) {
            endTimeLong = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestampMs(endTime);
        }
        List<Alarm> alarmList = alarmMapper.getAlarms(alarmType, beginTimeLong, endTimeLong);
        return new PageInfo<>(alarmList);
    }

    @Override
    public void deleteAlarmInfo(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            alarmMapper.deleteAlarms(ids);
        }
    }

    @Override
    public String getAlarmSnapById(Long id) {
        return "";
    }

    @Override
    public StreamInfo getAlarmRecordById(Long id) {
        return null;
    }
}
