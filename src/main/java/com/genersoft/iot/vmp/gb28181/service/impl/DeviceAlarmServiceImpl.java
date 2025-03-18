package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarm;
import com.genersoft.iot.vmp.gb28181.dao.DeviceAlarmMapper;
import com.genersoft.iot.vmp.gb28181.service.IDeviceAlarmService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceAlarmServiceImpl implements IDeviceAlarmService {

    @Autowired
    private DeviceAlarmMapper deviceAlarmMapper;

    @Override
    public PageInfo<DeviceAlarm> getAllAlarm(int page, int count, String deviceId, String alarmPriority, String alarmMethod, String alarmType, String startTime, String endTime) {
        PageHelper.startPage(page, count);
        List<DeviceAlarm> all = deviceAlarmMapper.query(deviceId, alarmPriority, alarmMethod, alarmType, startTime, endTime);
        return new PageInfo<>(all);
    }

    @Override
    public void add(DeviceAlarm deviceAlarm) {
        deviceAlarmMapper.add(deviceAlarm);
    }

    @Override
    public int clearAlarmBeforeTime(Integer id, List<String> deviceIdList, String time) {
        return deviceAlarmMapper.clearAlarmBeforeTime(id, deviceIdList, time);
    }
}
