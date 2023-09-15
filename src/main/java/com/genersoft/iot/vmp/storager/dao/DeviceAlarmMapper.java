package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.gb28181.bean.DeviceAlarm;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用于存储设备的报警信息
 */
@Mapper
@Repository
public interface DeviceAlarmMapper {

    @Insert("INSERT INTO wvp_device_alarm (device_id, channel_id, alarm_priority, alarm_method, alarm_time, alarm_description, longitude, latitude, alarm_type , create_time ) " +
            "VALUES (#{deviceId}, #{channelId}, #{alarmPriority}, #{alarmMethod}, #{alarmTime}, #{alarmDescription}, #{longitude}, #{latitude}, #{alarmType}, #{createTime})")
    int add(DeviceAlarm alarm);


    @Select( value = {" <script>" +
            " SELECT * FROM wvp_device_alarm " +
            " WHERE 1=1 " +
            " <if test=\"deviceId != null\" >  AND device_id = #{deviceId}</if>" +
            " <if test=\"alarmPriority != null\" >  AND alarm_priority = #{alarmPriority} </if>" +
            " <if test=\"alarmMethod != null\" >  AND alarm_method = #{alarmMethod} </if>" +
            " <if test=\"alarmType != null\" >  AND alarm_type = #{alarmType} </if>" +
            " <if test=\"startTime != null\" >  AND alarm_time &gt;= #{startTime} </if>" +
            " <if test=\"endTime != null\" >  AND alarm_time &lt;= #{endTime} </if>" +
            " ORDER BY alarm_time ASC " +
            " </script>"})
    List<DeviceAlarm> query(@Param("deviceId") String deviceId, @Param("alarmPriority") String alarmPriority, @Param("alarmMethod") String alarmMethod,
                            @Param("alarmType") String alarmType, @Param("startTime") String startTime, @Param("endTime") String endTime);


    @Delete(" <script>" +
            "DELETE FROM wvp_device_alarm WHERE 1=1 " +
            " <if test=\"deviceIdList != null and id == null \" > AND device_id in " +
            "<foreach collection='deviceIdList'  item='item'  open='(' separator=',' close=')' > #{item}</foreach>" +
            "</if>" +
            " <if test=\"time != null and id == null \" > AND alarm_time &lt;= #{time}</if>" +
            " <if test=\"id != null\" > AND id = #{id}</if>" +
            " </script>"
            )
    int clearAlarmBeforeTime(@Param("id") Integer id, @Param("deviceIdList") List<String> deviceIdList, @Param("time") String time);
}
