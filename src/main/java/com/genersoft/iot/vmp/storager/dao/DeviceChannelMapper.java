package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 用于存储设备通道信息
 */
@Mapper
public interface DeviceChannelMapper {

    @Insert("INSERT INTO device_channel (channelId, deviceId, name, manufacture, model, owner, civilCode, block, " +
            "address, parental, parentId, safetyWay, registerWay, certNum, certifiable, errCode, secrecy, " +
            "ipAddress, port, password, PTZType, status) " +
            "VALUES ('${channelId}', '${deviceId}', '${name}', '${manufacture}', '${model}', '${owner}', '${civilCode}', '${block}'," +
            "'${address}', ${parental}, '${parentId}', ${safetyWay}, ${registerWay}, '${certNum}', ${certifiable}, ${errCode}, '${secrecy}', " +
            "'${ipAddress}', ${port}, '${password}', ${PTZType}, ${status})")
    int add(DeviceChannel channel);

    @Update("UPDATE device_channel " +
            "SET name=#{name}, manufacture=#{manufacture}, model=#{model}, owner=#{owner}, civilCode=#{civilCode}, " +
            "block=#{block}, address=#{address}, parental=#{parental}, parentId=#{parentId}, safetyWay=#{safetyWay}, " +
            "registerWay=#{registerWay}, certNum=#{certNum}, certifiable=#{certifiable}, errCode=#{errCode}, secrecy=#{secrecy}, " +
            "ipAddress=#{ipAddress}, port=#{port}, password=#{password}, PTZType=#{PTZType}, status=#{status} " +
            "WHERE deviceId=#{deviceId} AND channelId=#{channelId}")
    int update(DeviceChannel channel);

    @Select(value = {" <script>" +
            "SELECT * FROM ( "+
            " SELECT * , (SELECT count(0) FROM device_channel WHERE parentId=dc.channelId) as subCount FROM device_channel dc " +
            " WHERE dc.deviceId=#{deviceId} " +
            " <if test=\"query != null\"> AND (dc.channelId LIKE '%${query}%' OR dc.name LIKE '%${query}%' OR dc.name LIKE '%${query}%')</if> " +
            " <if test=\"parentChannelId != null\"> AND dc.parentId=#{parentChannelId} </if> " +
            " <if test=\"online == true\" > AND dc.status=1</if>" +
            " <if test=\"online == false\" > AND dc.status=0</if>) dcr" +
            " WHERE 1=1 " +
            " <if test=\"hasSubChannel == true\" >  AND subCount >0</if>" +
            " <if test=\"hasSubChannel == false\" >  AND subCount=0</if>" +
            " </script>"})
    List<DeviceChannel> queryChannelsByDeviceId(String deviceId, String parentChannelId, String query, Boolean hasSubChannel, Boolean online);

    @Select("SELECT * FROM device_channel WHERE deviceId=#{deviceId} AND channelId=#{channelId}")
    DeviceChannel queryChannel(String deviceId, String channelId);

    @Delete("DELETE FROM device_channel WHERE deviceId=#{deviceId}")
    int cleanChannelsByDeviceId(String deviceId);

}
