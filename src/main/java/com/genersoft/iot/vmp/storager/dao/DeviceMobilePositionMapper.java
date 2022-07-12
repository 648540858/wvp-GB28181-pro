package com.genersoft.iot.vmp.storager.dao;

import java.util.List;

import com.genersoft.iot.vmp.gb28181.bean.MobilePosition;
import org.apache.ibatis.annotations.*;

@Mapper
public interface DeviceMobilePositionMapper {

    @Insert("INSERT INTO device_mobile_position (deviceId,channelId, deviceName, time, longitude, latitude, altitude, speed, direction, reportSource, longitudeGcj02, latitudeGcj02, longitudeWgs84, latitudeWgs84, createTime) " +
            "VALUES ('${deviceId}','${channelId}', '${deviceName}', '${time}', ${longitude}, ${latitude}, ${altitude}, ${speed}, ${direction}, '${reportSource}', ${longitudeGcj02}, ${latitudeGcj02}, ${longitudeWgs84}, ${latitudeWgs84}, '${createTime}')")
    int insertNewPosition(MobilePosition mobilePosition);

    @Select(value = {" <script>" +
    "SELECT * FROM device_mobile_position" +
    " WHERE deviceId = #{deviceId}" +
    "<if test=\"channelId != null\"> and channelId = #{channelId}</if>" +
    "<if test=\"startTime != null\"> AND time&gt;=#{startTime}</if>" +
    "<if test=\"endTime != null\"> AND time&lt;=#{endTime}</if>" +
    " ORDER BY time ASC" +
    " </script>"})
    List<MobilePosition> queryPositionByDeviceIdAndTime(String deviceId, String channelId, String startTime, String endTime);

    @Select("SELECT * FROM device_mobile_position WHERE deviceId = #{deviceId}" +
            " ORDER BY time DESC LIMIT 1")
    MobilePosition queryLatestPositionByDevice(String deviceId);

    @Delete("DELETE FROM device_mobile_position WHERE deviceId = #{deviceId}")
    int clearMobilePositionsByDeviceId(String deviceId);

}
