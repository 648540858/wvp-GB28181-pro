package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannelInPlatform;
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
            "ipAddress, port, password, PTZType, status, streamId, longitude, latitude, longitudeGcj02, latitudeGcj02, longitudeWgs84, latitudeWgs84, createTime, updateTime) " +
            "VALUES ('${channelId}', '${deviceId}', '${name}', '${manufacture}', '${model}', '${owner}', '${civilCode}', '${block}'," +
            "'${address}', ${parental}, '${parentId}', ${safetyWay}, ${registerWay}, '${certNum}', ${certifiable}, ${errCode}, '${secrecy}', " +
            "'${ipAddress}', ${port}, '${password}', ${PTZType}, ${status}, '${streamId}', ${longitude}, ${latitude}, ${longitudeGcj02}, ${latitudeGcj02}, ${longitudeWgs84}, ${latitudeWgs84},'${createTime}', '${updateTime}')")
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
            "<if test='longitudeGcj02 != null'>, longitudeGcj02=${longitudeGcj02}</if>" +
            "<if test='latitudeGcj02 != null'>, latitudeGcj02=${latitudeGcj02}</if>" +
            "<if test='longitudeWgs84 != null'>, longitudeWgs84=${longitudeWgs84}</if>" +
            "<if test='latitudeWgs84 != null'>, latitudeWgs84=${latitudeWgs84}</if>" +
            "WHERE deviceId='${deviceId}' AND channelId='${channelId}'"+
            " </script>"})
    int update(DeviceChannel channel);

    @Select(value = {" <script>" +
            "SELECT " +
            "dc.* " +
            "from " +
            "device_channel dc " +
            "WHERE " +
            "dc.deviceId = #{deviceId} " +
            " <if test='query != null'> AND (dc.channelId LIKE '%${query}%' OR dc.name LIKE '%${query}%' OR dc.name LIKE '%${query}%')</if> " +
            " <if test='parentChannelId != null'> AND dc.parentId=#{parentChannelId} </if> " +
            " <if test='online == true' > AND dc.status=1</if>" +
            " <if test='online == false' > AND dc.status=0</if>" +
            " <if test='hasSubChannel == true' >  AND dc.subCount > 0 </if>" +
            " <if test='hasSubChannel == false' >  AND dc.subCount = 0 </if>" +
            "ORDER BY dc.channelId " +
            " </script>"})
    List<DeviceChannel> queryChannels(String deviceId, String parentChannelId, String query, Boolean hasSubChannel, Boolean online);

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
            "SELECT " +
            "    dc.id,\n" +
            "    dc.channelId,\n" +
            "    dc.deviceId,\n" +
            "    dc.name,\n" +
            "    de.manufacturer,\n" +
            "    de.hostAddress,\n" +
            "    dc.subCount,\n" +
            "    pgc.platformId as platformId,\n" +
            "    pgc.catalogId as catalogId " +
            " FROM device_channel dc " +
            " LEFT JOIN device de ON dc.deviceId = de.deviceId " +
            " LEFT JOIN platform_gb_channel pgc on pgc.deviceChannelId = dc.id " +
            " WHERE 1=1 " +
            " <if test='query != null'> AND (dc.channelId LIKE '%${query}%' OR dc.name LIKE '%${query}%' OR dc.name LIKE '%${query}%')</if> " +
            " <if test='online == true' > AND dc.status=1</if> " +
            " <if test='online == false' > AND dc.status=0</if> " +
            " <if test='hasSubChannel!= null and hasSubChannel == true' >  AND dc.subCount > 0</if> " +
            " <if test='hasSubChannel!= null and hasSubChannel == false' >  AND dc.subCount = 0</if> " +
            " <if test='catalogId == null ' >  AND dc.id not in (select deviceChannelId from platform_gb_channel where platformId=#{platformId} ) </if> " +
            " <if test='catalogId != null ' >  AND pgc.platformId = #{platformId} and pgc.catalogId=#{catalogId} </if> " +
            " ORDER BY dc.deviceId, dc.channelId ASC" +
            " </script>"})
    List<ChannelReduce> queryChannelListInAll(String query, Boolean online, Boolean hasSubChannel, String platformId, String catalogId);

    @Select(value = {" <script>" +
            "SELECT " +
            "    dc.*,\n" +
            "    pgc.platformId as platformId,\n" +
            "    pgc.catalogId as catalogId " +
            " FROM device_channel dc " +
            " LEFT JOIN platform_gb_channel pgc on pgc.deviceChannelId = dc.id " +
            " WHERE pgc.platformId = #{platformId} " +
            " ORDER BY dc.deviceId, dc.channelId ASC" +
            " </script>"})
    List<DeviceChannelInPlatform> queryChannelByPlatformId(String platformId);


    @Select("SELECT * FROM device_channel WHERE channelId=#{channelId}")
    List<DeviceChannel> queryChannelByChannelId( String channelId);

    @Update(value = {"UPDATE device_channel SET status=0 WHERE deviceId=#{deviceId} AND channelId=#{channelId}"})
    void offline(String deviceId,  String channelId);

    @Update(value = {"UPDATE device_channel SET status=1 WHERE deviceId=#{deviceId} AND channelId=#{channelId}"})
    void online(String deviceId,  String channelId);

    @Insert("<script> " +
            "insert into device_channel " +
            "(channelId, deviceId, name, manufacture, model, owner, civilCode, block, subCount, " +
            "  address, parental, parentId, safetyWay, registerWay, certNum, certifiable, errCode, secrecy, " +
            "  ipAddress, port, password, PTZType, status, streamId, longitude, latitude, longitudeGcj02, latitudeGcj02, " +
            "  longitudeWgs84, latitudeWgs84, createTime, updateTime) " +
            "values " +
            "<foreach collection='addChannels' index='index' item='item' separator=','> " +
            "('${item.channelId}', '${item.deviceId}', '${item.name}', '${item.manufacture}', '${item.model}', " +
            "'${item.owner}', '${item.civilCode}', '${item.block}',${item.subCount}," +
            "'${item.address}', ${item.parental}, '${item.parentId}', ${item.safetyWay}, ${item.registerWay}, " +
            "'${item.certNum}', ${item.certifiable}, ${item.errCode}, '${item.secrecy}', " +
            "'${item.ipAddress}', ${item.port}, '${item.password}', ${item.PTZType}, ${item.status}, " +
            "'${item.streamId}', ${item.longitude}, ${item.latitude},${item.longitudeGcj02}, " +
            "${item.latitudeGcj02},${item.longitudeWgs84}, ${item.latitudeWgs84},'${item.createTime}', '${item.updateTime}')" +
            "</foreach> " +
            "ON DUPLICATE KEY UPDATE " +
            "updateTime=VALUES(updateTime), " +
            "name=VALUES(name), " +
            "manufacture=VALUES(manufacture), " +
            "model=VALUES(model), " +
            "owner=VALUES(owner), " +
            "civilCode=VALUES(civilCode), " +
            "block=VALUES(block), " +
            "subCount=VALUES(subCount), " +
            "address=VALUES(address), " +
            "parental=VALUES(parental), " +
            "parentId=VALUES(parentId), " +
            "safetyWay=VALUES(safetyWay), " +
            "registerWay=VALUES(registerWay), " +
            "certNum=VALUES(certNum), " +
            "certifiable=VALUES(certifiable), " +
            "errCode=VALUES(errCode), " +
            "secrecy=VALUES(secrecy), " +
            "ipAddress=VALUES(ipAddress), " +
            "port=VALUES(port), " +
            "password=VALUES(password), " +
            "PTZType=VALUES(PTZType), " +
            "status=VALUES(status), " +
            "streamId=VALUES(streamId), " +
            "longitude=VALUES(longitude), " +
            "latitude=VALUES(latitude), " +
            "longitudeGcj02=VALUES(longitudeGcj02), " +
            "latitudeGcj02=VALUES(latitudeGcj02), " +
            "longitudeWgs84=VALUES(longitudeWgs84), " +
            "latitudeWgs84=VALUES(latitudeWgs84) " +
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
            "<if test='item.subCount != null'>, block=${item.subCount}</if>" +
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
            "<if test='item.longitudeGcj02 != null'>, longitudeGcj02=${item.longitudeGcj02}</if>" +
            "<if test='item.latitudeGcj02 != null'>, latitudeGcj02=${item.latitudeGcj02}</if>" +
            "<if test='item.longitudeWgs84 != null'>, longitudeWgs84=${item.longitudeWgs84}</if>" +
            "<if test='item.latitudeWgs84 != null'>, latitudeWgs84=${item.latitudeWgs84}</if>" +
            "WHERE deviceId='${item.deviceId}' AND channelId='${item.channelId}'"+
            "</foreach>" +
            "</script>"})
    int batchUpdate(List<DeviceChannel> updateChannels);


    @Select(value = {" <script>" +
            "SELECT " +
            "dc1.* " +
            "from " +
            "device_channel dc1 " +
            "WHERE " +
            "dc1.deviceId = #{deviceId} " +
            " <if test='query != null'> AND (dc1.channelId LIKE '%${query}%' OR dc1.name LIKE '%${query}%' OR dc1.name LIKE '%${query}%')</if> " +
            " <if test='parentChannelId != null'> AND dc1.parentId=#{parentChannelId} </if> " +
            " <if test='online == true' > AND dc1.status=1</if>" +
            " <if test='online == false' > AND dc1.status=0</if>" +
            " <if test='hasSubChannel == true' >  AND dc1.subCount >0</if>" +
            " <if test='hasSubChannel == false' >  AND dc1.subCount=0</if>" +
            "ORDER BY dc1.channelId ASC " +
            "Limit #{limit} OFFSET #{start}" +
            " </script>"})
    List<DeviceChannel> queryChannelsByDeviceIdWithStartAndLimit(String deviceId, String parentChannelId, String query,
                                                                 Boolean hasSubChannel, Boolean online, int start, int limit);

    @Select("SELECT * FROM device_channel WHERE deviceId=#{deviceId} AND status=1")
    List<DeviceChannel> queryOnlineChannelsByDeviceId(String deviceId);

    @Delete(value = {" <script>" +
            "DELETE " +
            "from " +
            "device_channel " +
            "WHERE " +
            "deviceId = #{deviceId} " +
            " AND channelId NOT IN " +
            "<foreach collection='channels'  item='item'  open='(' separator=',' close=')' > #{item.channelId}</foreach>" +
            " </script>"})
    int cleanChannelsNotInList(String deviceId, List<DeviceChannel> channels);

    @Update(" update device_channel" +
            " set subCount = (select *" +
            "                from (select count(0)" +
            "                      from device_channel" +
            "                      where deviceId = #{deviceId} and parentId = #{channelId}) as temp)" +
            " where deviceId = #{deviceId} " +
            " and channelId = #{channelId}")
    int updateChannelSubCount(String deviceId, String channelId);

    @Update(value = {"UPDATE device_channel SET latitude=${latitude}, longitude=${longitude} WHERE deviceId=#{deviceId} AND channelId=#{channelId}"})
    void updatePotion(String deviceId, String channelId, double longitude, double latitude);

    @Select("SELECT * FROM device_channel WHERE length(trim(streamId)) > 0")
    List<DeviceChannel> getAllChannelInPlay();

    @Select("select * from device_channel where longitude*latitude > 0 and deviceId = #{deviceId}")
    List<DeviceChannel> getAllChannelWithCoordinate(String deviceId);
}
