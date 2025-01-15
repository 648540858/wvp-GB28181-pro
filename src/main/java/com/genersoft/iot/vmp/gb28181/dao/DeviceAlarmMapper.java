package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用于存储设备的报警信息
 */
@Mapper
@Repository
public interface DeviceAlarmMapper {

    int add(DeviceAlarm alarm);

    List<DeviceAlarm> query(@Param("deviceId") String deviceId, @Param("alarmPriority") String alarmPriority, @Param("alarmMethod") String alarmMethod,
                            @Param("alarmType") String alarmType, @Param("startTime") String startTime, @Param("endTime") String endTime);

    int clearAlarmBeforeTime(@Param("id") Integer id, @Param("deviceIdList") List<String> deviceIdList, @Param("time") String time);
}
