package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.ChannelReduce;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用于存储设备通道信息
 */
@Mapper
@Repository
public interface DeviceChannelMapper {

    @Insert("INSERT INTO device_channel (channelId, deviceId, name, manufacture, model, owner, civilCode, block, " +
            "address, parental, parentId, safetyWay, registerWay, certNum, certifiable, errCode, secrecy, " +
            "ipAddress, port, password, PTZType, status, streamId, longitude, latitude, createTime, updateTime) " +
            "VALUES ('${channelId}', '${deviceId}', '${name}', '${manufacture}', '${model}', '${owner}', '${civilCode}', '${block}'," +
            "'${address}', ${parental}, '${parentId}', ${safetyWay}, ${registerWay}, '${certNum}', ${certifiable}, ${errCode}, '${secrecy}', " +
            "'${ipAddress}', ${port}, '${password}', ${PTZType}, ${status}, '${streamId}', ${longitude}, ${latitude},'${createTime}', '${updateTime}')")
    int add(DeviceChannel channel);

    @Update(value = {" <script>" +
            "UPDATE device_channel " +
            "SET updateTime='${updateTime}'" +
            "<if test='name != null'>, name='${name}'</if>" +
            "<if test='manufacture != null'>, manufacture='${manufacture}'</if>" +
            "<if test='model != null'>, model='${model}'</if>" +
            "<if test='owner != null'>, owner='${owner}'</if>" +
            "<if test='civilCode != null'>, civilCode='${civilCode}'</if>" +
            "<if test='block != null'>, block='${block}'</if>" +
            "<if test='address != null'>, address='${address}'</if>" +
            "<if test='parental != null'>, parental=${parental}</if>" +
            "<if test='parentId != null'>, parentId='${parentId}'</if>" +
            "<if test='safetyWay != null'>, safetyWay=${safetyWay}</if>" +
            "<if test='registerWay != null'>, registerWay=${registerWay}</if>" +
            "<if test='certNum != null'>, certNum='${certNum}'</if>" +
            "<if test='certifiable != null'>, certifiable=${certifiable}</if>" +
            "<if test='errCode != null'>, errCode=${errCode}</if>" +
            "<if test='secrecy != null'>, secrecy='${secrecy}'</if>" +
            "<if test='ipAddress != null'>, ipAddress='${ipAddress}'</if>" +
            "<if test='port != null'>, port=${port}</if>" +
            "<if test='password != null'>, password='${password}'</if>" +
            "<if test='PTZType != null'>, PTZType=${PTZType}</if>" +
            "<if test='status != null'>, status='${status}'</if>" +
            "<if test='streamId != null'>, streamId='${streamId}'</if>" +
            "<if test='hasAudio != null'>, hasAudio=${hasAudio}</if>" +
            "<if test='longitude != null'>, longitude=${longitude}</if>" +
            "<if test='latitude != null'>, latitude=${latitude}</if>" +
            "WHERE deviceId='${deviceId}' AND channelId='${channelId}'"+
            " </script>"})
    int update(DeviceChannel channel);

    @Select(value = {" <script>" +
            "SELECT " +
            "dc1.*, " +
            "COUNT(dc2.channelId) as subCount " +
            "from " +
            "device_channel dc1 " +
            "left join device_channel dc2 on " +
            "dc1.channelId = dc2.parentId " +
            "WHERE " +
            "dc1.deviceId = #{deviceId} " +
            " <if test='query != null'> AND (dc1.channelId LIKE '%${query}%' OR dc1.name LIKE '%${query}%' OR dc1.name LIKE '%${query}%')</if> " +
            " <if test='parentChannelId != null'> AND dc1.parentId=#{parentChannelId} </if> " +
            " <if test='online == true' > AND dc1.status=1</if>" +
            " <if test='online == false' > AND dc1.status=0</if>" +
            " <if test='hasSubChannel == true' >  AND subCount >0</if>" +
            " <if test='hasSubChannel == false' >  AND subCount=0</if>" +
            "GROUP BY dc1.channelId " +
            " </script>"})
    List<DeviceChannel> queryChannels(String deviceId, String parentChannelId, String query, Boolean hasSubChannel, Boolean online);

    @Select("SELECT * FROM device_channel WHERE deviceId=#{deviceId}")
    List<DeviceChannel> queryChannelsByDeviceId(String deviceId);

    @Select("SELECT * FROM device_channel WHERE deviceId=#{deviceId} AND channelId=#{channelId}")
    DeviceChannel queryChannel(String deviceId, String channelId);

    @Delete("DELETE FROM device_channel WHERE deviceId=#{deviceId}")
    int cleanChannelsByDeviceId(String deviceId);

    @Delete("DELETE FROM device_channel WHERE deviceId=#{deviceId} AND channelId=#{channelId}")
    int del(String deviceId, String channelId);

    @Update(value = {"UPDATE device_channel SET streamId=null WHERE deviceId=#{deviceId} AND channelId=#{channelId}"})
    void stopPlay(String deviceId, String channelId);

    @Update(value = {"UPDATE device_channel SET streamId=#{streamId} WHERE deviceId=#{deviceId} AND channelId=#{channelId}"})
    void startPlay(String deviceId, String channelId, String streamId);


    @Select(value = {" <script>" +
            "SELECT * FROM ( "+
                " SELECT dc.channelId, dc.deviceId, dc.name, de.manufacturer, de.hostAddress, " +
                "(SELECT count(0) FROM device_channel WHERE parentId=dc.channelId) as subCount, " +
                "(SELECT pc.platformId FROM platform_gb_channel pc WHERE pc.deviceId=dc.deviceId AND pc.channelId = dc.channelId AND pc.platformId = #{platformId}) as platformId, " +
                "(SELECT pc.catalogId FROM platform_gb_channel pc WHERE pc.deviceId=dc.deviceId AND pc.channelId = dc.channelId AND pc.platformId = #{platformId} ) as catalogId " +
                "FROM device_channel dc " +
                "LEFT JOIN device de ON dc.deviceId = de.deviceId " +
                " WHERE 1=1 " +
                " <if test='query != null'> AND (dc.channelId LIKE '%${query}%' OR dc.name LIKE '%${query}%' OR dc.name LIKE '%${query}%')</if> " +
                " <if test='online == true' > AND dc.status=1</if> " +
                " <if test='online == false' > AND dc.status=0</if> " +
            ") dcr" +
            " WHERE 1=1 " +
            " <if test='hasSubChannel!= null and hasSubChannel == true' >  AND subCount >0</if> " +
            " <if test='hasSubChannel!= null and hasSubChannel == false' >  AND subCount=0</if> " +
            " <if test='platformId != null and inPlatform == true ' >  AND platformId='${platformId}'</if> " +
            " <if test='platformId != null and inPlatform == false ' >  AND (platformId != '${platformId}' OR platformId is NULL )  </if> " +
            " ORDER BY deviceId, channelId ASC" +
            " </script>"})

    List<ChannelReduce> queryChannelListInAll(String query, Boolean online, Boolean hasSubChannel, String platformId, Boolean inPlatform);

    @Select("SELECT * FROM device_channel WHERE channelId=#{channelId}")
    List<DeviceChannel> queryChannelByChannelId( String channelId);

    @Update(value = {"UPDATE device_channel SET status=0 WHERE deviceId=#{deviceId} AND channelId=#{channelId}"})
    void offline(String deviceId,  String channelId);

    @Update(value = {"UPDATE device_channel SET status=1 WHERE deviceId=#{deviceId} AND channelId=#{channelId}"})
    void online(String deviceId,  String channelId);

    @Insert("<script> " +
            "insert into device_channel " +
            "(channelId, deviceId, name, manufacture, model, owner, civilCode, block, " +
            "  address, parental, parentId, safetyWay, registerWay, certNum, certifiable, errCode, secrecy, " +
            "  ipAddress, port, password, PTZType, status, streamId, longitude, latitude, createTime, updateTime) " +
            "values " +
            "<foreach collection='addChannels' index='index' item='item' separator=','> " +
            "('${item.channelId}', '${item.deviceId}', '${item.name}', '${item.manufacture}', '${item.model}', " +
            "'${item.owner}', '${item.civilCode}', '${item.block}'," +
            "'${item.address}', ${item.parental}, '${item.parentId}', ${item.safetyWay}, ${item.registerWay}, " +
            "'${item.certNum}', ${item.certifiable}, ${item.errCode}, '${item.secrecy}', " +
            "'${item.ipAddress}', ${item.port}, '${item.password}', ${item.PTZType}, ${item.status}, " +
            "'${item.streamId}', ${item.longitude}, ${item.latitude},'${item.createTime}', '${item.updateTime}')" +
            "</foreach> " +
            "</script>")
    int batchAdd(List<DeviceChannel> addChannels);

    @Update({"<script>" +
            "<foreach collection='updateChannels' item='item' separator=';'>" +
            " UPDATE" +
            " device_channel" +
            " SET updateTime='${item.updateTime}'" +
            "<if test='item.name != null'>, name='${item.name}'</if>" +
            "<if test='item.manufacture != null'>, manufacture='${item.manufacture}'</if>" +
            "<if test='item.model != null'>, model='${item.model}'</if>" +
            "<if test='item.owner != null'>, owner='${item.owner}'</if>" +
            "<if test='item.civilCode != null'>, civilCode='${item.civilCode}'</if>" +
            "<if test='item.block != null'>, block='${item.block}'</if>" +
            "<if test='item.address != null'>, address='${item.address}'</if>" +
            "<if test='item.parental != null'>, parental=${item.parental}</if>" +
            "<if test='item.parentId != null'>, parentId='${item.parentId}'</if>" +
            "<if test='item.safetyWay != null'>, safetyWay=${item.safetyWay}</if>" +
            "<if test='item.registerWay != null'>, registerWay=${item.registerWay}</if>" +
            "<if test='item.certNum != null'>, certNum='${item.certNum}'</if>" +
            "<if test='item.certifiable != null'>, certifiable=${item.certifiable}</if>" +
            "<if test='item.errCode != null'>, errCode=${item.errCode}</if>" +
            "<if test='item.secrecy != null'>, secrecy='${item.secrecy}'</if>" +
            "<if test='item.ipAddress != null'>, ipAddress='${item.ipAddress}'</if>" +
            "<if test='item.port != null'>, port=${item.port}</if>" +
            "<if test='item.password != null'>, password='${item.password}'</if>" +
            "<if test='item.PTZType != null'>, PTZType=${item.PTZType}</if>" +
            "<if test='item.status != null'>, status='${item.status}'</if>" +
            "<if test='item.streamId != null'>, streamId='${item.streamId}'</if>" +
            "<if test='item.hasAudio != null'>, hasAudio=${item.hasAudio}</if>" +
            "<if test='item.longitude != null'>, longitude=${item.longitude}</if>" +
            "<if test='item.latitude != null'>, latitude=${item.latitude}</if>" +
            "WHERE deviceId=#{item.deviceId} AND channelId=#{item.channelId}"+
            "</foreach>" +
            "</script>"})
    int batchUpdate(List<DeviceChannel> updateChannels);


    @Select(value = {" <script>" +
            "SELECT " +
            "dc1.*, " +
            "COUNT(dc2.channelId) as subCount " +
            "from " +
            "device_channel dc1 " +
            "left join device_channel dc2 on " +
            "dc1.channelId = dc2.parentId " +
            "WHERE " +
            "dc1.deviceId = #{deviceId} " +
            " <if test='query != null'> AND (dc1.channelId LIKE '%${query}%' OR dc1.name LIKE '%${query}%' OR dc1.name LIKE '%${query}%')</if> " +
            " <if test='parentChannelId != null'> AND dc1.parentId=#{parentChannelId} </if> " +
            " <if test='online == true' > AND dc1.status=1</if>" +
            " <if test='online == false' > AND dc1.status=0</if>" +
            " <if test='hasSubChannel == true' >  AND subCount >0</if>" +
            " <if test='hasSubChannel == false' >  AND subCount=0</if>" +
            "GROUP BY dc1.channelId " +
            "ORDER BY dc1.channelId ASC " +
            "Limit #{limit} OFFSET #{start}" +
            " </script>"})
    List<DeviceChannel> queryChannelsByDeviceIdWithStartAndLimit(String deviceId, String parentChannelId, String query,
                                                                 Boolean hasSubChannel, Boolean online, int start, int limit);

    @Select("SELECT * FROM device_channel WHERE deviceId=#{deviceId} AND status=1")
    List<DeviceChannel> queryOnlineChannelsByDeviceId(String deviceId);
}
