package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MobilePositionMapper {

    @Insert("INSERT INTO wvp_mobile_position (channel_id, timestamp, longitude, latitude, altitude, speed, direction, create_time)"+
            "VALUES (#{channelId}, #{timestamp}, #{longitude}, #{latitude}, #{altitude}, #{speed}, #{direction}, #{createTime})")
    int insertNewPosition(MobilePosition mobilePosition);

    @Select(value = {" <script>" +
    "SELECT * FROM wvp_mobile_position" +
    " WHERE channel_id = #{channelId}" +
    "<if test=\"startTime != null\"> AND timestamp&gt;=#{startTime}</if>" +
    "<if test=\"endTime != null\"> AND timestamp&lt;=#{endTime}</if>" +
    " ORDER BY time ASC" +
    " </script>"})
    List<MobilePosition> queryPositionByDeviceIdAndTime(@Param("channelId") Integer channelId, @Param("startTime") Long startTime, @Param("endTime") Long endTime);

    @Select("SELECT * FROM wvp_mobile_position WHERE channel_id = #{channelId}" +
            " ORDER BY timestamp DESC LIMIT 1")
    MobilePosition queryLatestPosition(@Param("channelId") Integer channelId);

    @Insert("<script> " +
            "<foreach collection='mobilePositions' index='index' item='item' separator=';'> " +
            "insert into wvp_mobile_position " +
            "(channel_id, timestamp,longitude,latitude,altitude,speed,direction," +
            "create_time)"+
            "values " +
            "( #{item.channelId}, #{item.timestamp}, #{item.longitude}, " +
            " #{item.latitude}, #{item.altitude}, #{item.speed},#{item.direction}," +
            " #{item.createTime}) " +
            "</foreach> " +
            "</script>")
    void batchAdd(List<MobilePosition> mobilePositions);
}
