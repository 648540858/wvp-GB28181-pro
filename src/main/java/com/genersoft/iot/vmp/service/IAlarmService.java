package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.service.bean.Alarm;
import com.genersoft.iot.vmp.service.bean.AlarmType;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface IAlarmService {
    // 保存报警信息
    void saveAlarmInfo(Alarm alarm);

    // 分页获取报警信息
    PageInfo<Alarm> getAlarms(int page, int size, List<AlarmType> alarmType, String beginTime, String endTime);

    // 删除报警信息
    void deleteAlarmInfo(List<Long> ids);

    // 按筛选条件清空报警信息
    int clearAlarmsByCondition(List<AlarmType> alarmType, String beginTime, String endTime);

    // 根据ID获取报警快照
    String getAlarmSnapById(Long id);

    // 根据ID获取报警录像
    StreamInfo getAlarmRecordById(Long id);
}
