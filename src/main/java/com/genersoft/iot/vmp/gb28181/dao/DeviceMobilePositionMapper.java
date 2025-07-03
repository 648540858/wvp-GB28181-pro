package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DeviceMobilePositionMapper {

    @Insert("INSERT INTO wvp_device_mobile_position (device_id,channel_id, device_name,time,longitude,latitude,altitude,speed,direction,report_source,create_time)"+
            "VALUES (#{deviceId}, #{channelId}, #{deviceName}, #{time}, #{longitude}, #{latitude}, #{altitude}, #{speed}, #{direction}, #{reportSource}, #{createTime})")
    int insertNewPosition(MobilePosition mobilePosition);

    @Select(value = {" <script>" +
    "SELECT * FROM wvp_device_mobile_position" +
    " WHERE device_id = #{deviceId}" +
    "<if test=\"channelId != null\"> and channel_id = #{channelId}</if>" +
    "<if test=\"startTime != null\"> AND time&gt;=#{startTime}</if>" +
    "<if test=\"endTime != null\"> AND time&lt;=#{endTime}</if>" +
    " ORDER BY time ASC" +
    " </script>"})
    List<MobilePosition> queryPositionByDeviceIdAndTime(@Param("deviceId") String deviceId, @Param("channelId") String channelId, @Param("startTime") String startTime, @Param("endTime") String endTime);

    @Select("SELECT * FROM wvp_device_mobile_position WHERE device_id = #{deviceId}" +
            " ORDER BY time DESC LIMIT 1")
    MobilePosition queryLatestPositionByDevice(String deviceId);

    @Delete("DELETE FROM wvp_device_mobile_position WHERE device_id = #{deviceId}")
    int clearMobilePositionsByDeviceId(String deviceId);

    @Insert("<script> " +
            "<foreach collection='mobilePositions' index='index' item='item' separator=';'> " +
            "insert into wvp_device_mobile_position " +
            "(device_id,channel_id, device_name,time,longitude,latitude,altitude,speed,direction,report_source," +
            "create_time)"+
            "values " +
            "(#{item.deviceId}, #{item.channelId}, #{item.deviceName}, #{item.time}, #{item.longitude}, " +
            "#{item.latitude}, #{item.altitude}, #{item.speed},#{item.direction}," +
            "#{item.reportSource}, #{item.createTime}) " +
            "</foreach> " +
            "</script>")
    void batchadd(List<MobilePosition> mobilePositions);

}
