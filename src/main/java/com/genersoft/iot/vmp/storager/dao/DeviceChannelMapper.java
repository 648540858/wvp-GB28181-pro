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

    @Update(value = {" <script>" +
            "UPDATE device_channel " +
            "SET deviceId='${deviceId}'" +
            "<if test=\"name != null\">, name='${name}'</if>" +
            "<if test=\"manufacture != null\">, manufacture='${manufacture}'</if>" +
            "<if test=\"model != null\">, model='${model}'</if>" +
            "<if test=\"owner != null\">, owner='${owner}'</if>" +
            "<if test=\"civilCode != null\">, civilCode='${civilCode}'</if>" +
            "<if test=\"block != null\">, block='${block}'</if>" +
            "<if test=\"address != null\">, address='${address}'</if>" +
            "<if test=\"parental != null\">, parental=${parental}</if>" +
            "<if test=\"parentId != null\">, parentId='${parentId}'</if>" +
            "<if test=\"safetyWay != null\">, safetyWay=${safetyWay}</if>" +
            "<if test=\"registerWay != null\">, registerWay=${registerWay}</if>" +
            "<if test=\"certNum != null\">, certNum='${certNum}'</if>" +
            "<if test=\"certifiable != null\">, certifiable=${certifiable}</if>" +
            "<if test=\"errCode != null\">, errCode=${errCode}</if>" +
            "<if test=\"secrecy != null\">, secrecy='${secrecy}'</if>" +
            "<if test=\"ipAddress != null\">, ipAddress='${ipAddress}'</if>" +
            "<if test=\"port != null\">, port=${port}</if>" +
            "<if test=\"password != null\">, password='${password}'</if>" +
            "<if test=\"PTZType != null\">, PTZType=${PTZType}</if>" +
            "<if test=\"status != null\">, status='${status}'</if>" +
            "<if test=\"streamId != null\">, streamId='${streamId}'</if>" +
            "<if test=\"hasAudio != null\">, hasAudio='${hasAudio}'</if>" +
            "WHERE deviceId='${deviceId}' AND channelId='${channelId}'"+
            " </script>"})
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
            " ORDER BY channelId ASC" +
            " </script>"})
    List<DeviceChannel> queryChannelsByDeviceId(String deviceId, String parentChannelId, String query, Boolean hasSubChannel, Boolean online);

    @Select("SELECT * FROM device_channel WHERE deviceId=#{deviceId} AND channelId=#{channelId}")
    DeviceChannel queryChannel(String deviceId, String channelId);

    @Delete("DELETE FROM device_channel WHERE deviceId=#{deviceId}")
    int cleanChannelsByDeviceId(String deviceId);

    @Update(value = {"UPDATE device_channel SET streamId=null WHERE deviceId=#{deviceId} AND channelId=#{channelId}"})
    void stopPlay(String deviceId, String channelId);

    @Update(value = {"UPDATE device_channel SET streamId=#{streamId} WHERE deviceId=#{deviceId} AND channelId=#{channelId}"})
    void startPlay(String deviceId, String channelId, String streamId);
}
