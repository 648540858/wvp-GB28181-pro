package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannelInPlatform;
import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.ChannelReduce;
import com.genersoft.iot.vmp.web.gb28181.dto.DeviceChannelExtend;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用于存储设备通道信息
 */
@Mapper
@Repository
public interface DeviceChannelMapper {

    @Insert("INSERT INTO wvp_device_channel (channel_id, device_id, name, manufacture, model, owner, civil_code, block, " +
            "address, parental, parent_id, safety_way, register_way, cert_num, certifiable, err_code, secrecy, " +
            "ip_address, port, password, ptz_type, status, stream_id, longitude, latitude, longitude_gcj02, latitude_gcj02, " +
            "longitude_wgs84, latitude_wgs84, has_audio, create_time, update_time, business_group_id, gps_time) " +
            "VALUES (#{channelId}, #{deviceId}, #{name}, #{manufacture}, #{model}, #{owner}, #{civilCode}, #{block}," +
            "#{address}, #{parental}, #{parentId}, #{safetyWay}, #{registerWay}, #{certNum}, #{certifiable}, #{errCode}, #{secrecy}, " +
            "#{ipAddress}, #{port}, #{password}, #{PTZType}, #{status}, #{streamId}, #{longitude}, #{latitude}, #{longitudeGcj02}, " +
            "#{latitudeGcj02}, #{longitudeWgs84}, #{latitudeWgs84}, #{hasAudio}, #{createTime}, #{updateTime}, #{businessGroupId}, #{gpsTime})")
    int add(DeviceChannel channel);

    @Update(value = {" <script>" +
            "UPDATE wvp_device_channel " +
            "SET update_time=#{updateTime}" +
            "<if test='name != null'>, name=#{name}</if>" +
            "<if test='manufacture != null'>, manufacture=#{manufacture}</if>" +
            "<if test='model != null'>, model=#{model}</if>" +
            "<if test='owner != null'>, owner=#{owner}</if>" +
            "<if test='civilCode != null'>, civil_code=#{civilCode}</if>" +
            "<if test='block != null'>, block=#{block}</if>" +
            "<if test='address != null'>, address=#{address}</if>" +
            "<if test='parental != null'>, parental=#{parental}</if>" +
            "<if test='parentId != null'>, parent_id=#{parentId}</if>" +
            "<if test='safetyWay != null'>, safety_way=#{safetyWay}</if>" +
            "<if test='registerWay != null'>, register_way=#{registerWay}</if>" +
            "<if test='certNum != null'>, cert_num=#{certNum}</if>" +
            "<if test='certifiable != null'>, certifiable=#{certifiable}</if>" +
            "<if test='errCode != null'>, err_code=#{errCode}</if>" +
            "<if test='secrecy != null'>, secrecy=#{secrecy}</if>" +
            "<if test='ipAddress != null'>, ip_address=#{ipAddress}</if>" +
            "<if test='port != null'>, port=#{port}</if>" +
            "<if test='password != null'>, password=#{password}</if>" +
            "<if test='PTZType != null'>, ptz_type=#{PTZType}</if>" +
            "<if test='status != null'>, status=#{status}</if>" +
            "<if test='streamId != null'>, stream_id=#{streamId}</if>" +
            "<if test='hasAudio != null'>, has_audio=#{hasAudio}</if>" +
            "<if test='longitude != null'>, longitude=#{longitude}</if>" +
            "<if test='latitude != null'>, latitude=#{latitude}</if>" +
            "<if test='longitudeGcj02 != null'>, longitude_gcj02=#{longitudeGcj02}</if>" +
            "<if test='latitudeGcj02 != null'>, latitude_gcj02=#{latitudeGcj02}</if>" +
            "<if test='longitudeWgs84 != null'>, longitude_wgs84=#{longitudeWgs84}</if>" +
            "<if test='latitudeWgs84 != null'>, latitude_wgs84=#{latitudeWgs84}</if>" +
            "<if test='businessGroupId != null'>, business_group_id=#{businessGroupId}</if>" +
            "<if test='gpsTime != null'>, gps_time=#{gpsTime}</if>" +
            "WHERE device_id=#{deviceId} AND channel_id=#{channelId}"+
            " </script>"})
    int update(DeviceChannel channel);

    @Select(value = {" <script>" +
            "SELECT " +
            "dc.* " +
            "from " +
            "wvp_device_channel dc " +
            "WHERE " +
            "dc.device_id = #{deviceId} " +
" <if test='query != null'> AND (dc.channel_id LIKE concat('%',#{query},'%') OR dc.name LIKE concat('%',#{query},'%') OR dc.name LIKE concat('%',#{query},'%'))</if> " +
            " <if test='parentChannelId != null'> AND (dc.parent_id=#{parentChannelId} OR dc.civil_code = #{parentChannelId}) </if> " +
            " <if test='online == true' > AND dc.status= true</if>" +
            " <if test='online == false' > AND dc.status= false</if>" +
            " <if test='hasSubChannel == true' >  AND dc.sub_count > 0 </if>" +
            " <if test='hasSubChannel == false' >  AND dc.sub_count = 0 </if>" +
            "<if test='channelIds != null'> AND dc.channel_id in <foreach item='item' index='index' collection='channelIds' open='(' separator=',' close=')'>" +
            "#{item} " +
            "</foreach> </if>" +
            "ORDER BY dc.channel_id " +
            " </script>"})
    List<DeviceChannel> queryChannels(@Param("deviceId") String deviceId, @Param("parentChannelId") String parentChannelId, @Param("query") String query, @Param("hasSubChannel") Boolean hasSubChannel, @Param("online") Boolean online, @Param("channelIds") List<String> channelIds);

    @Select(value = {" <script>" +
            "SELECT " +
            "dc.*, " +
            "de.name as device_name, " +
            "de.on_line as device_online " +
            "from " +
            "wvp_device_channel dc " +
            "LEFT JOIN wvp_device de ON dc.device_id = de.device_id " +
            "WHERE 1=1" +
            " <if test='deviceId != null'> AND dc.device_id = #{deviceId} </if> " +
            " <if test='query != null'> AND (dc.channel_id LIKE '%${query}%' OR dc.name LIKE '%${query}%' OR dc.name LIKE '%${query}%')</if> " +
            " <if test='parentChannelId != null'> AND dc.parent_id=#{parentChannelId} </if> " +
            " <if test='online == true' > AND dc.status=true</if>" +
            " <if test='online == false' > AND dc.status=false</if>" +
            " <if test='hasSubChannel == true' >  AND dc.sub_count > 0 </if>" +
            " <if test='hasSubChannel == false' >  AND dc.sub_count = 0 </if>" +
            "<if test='channelIds != null'> AND dc.channel_id in <foreach item='item' index='index' collection='channelIds' open='(' separator=',' close=')'>" +
            "#{item} " +
            "</foreach> </if>" +
            "ORDER BY dc.channel_id ASC" +
            " </script>"})
    List<DeviceChannelExtend> queryChannelsWithDeviceInfo(@Param("deviceId") String deviceId, @Param("parentChannelId") String parentChannelId, @Param("query") String query, @Param("hasSubChannel") Boolean hasSubChannel, @Param("online") Boolean online, @Param("channelIds") List<String> channelIds);


    @Select(value = {" <script>" +
            "SELECT " +
            "dc.*, " +
            "de.name as device_name, " +
            "de.on_line as device_online " +
            "from " +
            "wvp_device_channel dc " +
            "LEFT JOIN wvp_device de ON dc.device_id = de.device_id " +
            "WHERE 1=1" +
            " <if test='deviceId != null'> AND dc.device_id = #{deviceId} </if> " +
            " <if test='query != null'> AND (dc.channel_id LIKE '%${query}%' OR dc.name LIKE '%${query}%' OR dc.name LIKE '%${query}%')</if> " +
            " <if test='parentChannelId != null'> AND dc.parent_id=#{parentChannelId} </if> " +
            " <if test='online == true' > AND dc.status=true</if>" +
            " <if test='online == false' > AND dc.status=false</if>" +
            " <if test='hasSubChannel == true' >  AND dc.sub_count > 0 </if>" +
            " <if test='hasSubChannel == false' >  AND dc.sub_count = 0 </if>" +
            "<if test='channelIds != null'> AND dc.channel_id in <foreach item='item' index='index' collection='channelIds' open='(' separator=',' close=')'>" +
            "#{item} " +
            "</foreach> </if>" +
            "ORDER BY dc.channel_id ASC " +
            "Limit #{limit} OFFSET #{start}" +
            " </script>"})
    List<DeviceChannelExtend> queryChannelsByDeviceIdWithStartAndLimit(@Param("deviceId") String deviceId, @Param("channelIds") List<String> channelIds, @Param("parentChannelId") String parentChannelId, @Param("query") String query, @Param("hasSubChannel") Boolean hasSubChannel, @Param("online") Boolean online, @Param("start") int start, @Param("limit") int limit);

    @Select("SELECT * FROM wvp_device_channel WHERE device_id=#{deviceId} AND channel_id=#{channelId}")
    DeviceChannel queryChannel(@Param("deviceId") String deviceId,@Param("channelId") String channelId);

    @Delete("DELETE FROM wvp_device_channel WHERE device_id=#{deviceId}")
    int cleanChannelsByDeviceId(@Param("deviceId") String deviceId);

    @Delete("DELETE FROM wvp_device_channel WHERE device_id=#{deviceId} AND channel_id=#{channelId}")
    int del(@Param("deviceId") String deviceId, @Param("channelId") String channelId);

    @Update(value = {"UPDATE wvp_device_channel SET stream_id=null WHERE device_id=#{deviceId} AND channel_id=#{channelId}"})
    void stopPlay(@Param("deviceId") String deviceId, @Param("channelId") String channelId);

    @Update(value = {"UPDATE wvp_device_channel SET stream_id=#{streamId} WHERE device_id=#{deviceId} AND channel_id=#{channelId}"})
    void startPlay(@Param("deviceId") String deviceId, @Param("channelId") String channelId, @Param("streamId") String streamId);


    @Select(value = {" <script>" +
            "SELECT " +
            "    dc.id,\n" +
            "    dc.channel_id,\n" +
            "    dc.device_id,\n" +
            "    dc.name,\n" +
            "    de.manufacturer,\n" +
            "    de.host_address,\n" +
            "    dc.sub_count,\n" +
            "    pgc.platform_id as platform_id,\n" +
            "    pgc.catalog_id as catalog_id " +
            " FROM wvp_device_channel dc " +
            " LEFT JOIN wvp_device de ON dc.device_id = de.device_id " +
            " LEFT JOIN wvp_platform_gb_channel pgc on pgc.device_channel_id = dc.id " +
            " WHERE 1=1 " +
            " <if test='query != null'> AND (dc.channel_id LIKE concat('%',#{query},'%') OR dc.name LIKE concat('%',#{query},'%') OR dc.name LIKE concat('%',#{query},'%'))</if> " +
            " <if test='online == true' > AND dc.status=true</if> " +
            " <if test='online == false' > AND dc.status=false</if> " +
            " <if test='hasSubChannel!= null and has_sub_channel == true' >  AND dc.sub_count > 0</if> " +
            " <if test='hasSubChannel!= null and has_sub_channel == false' >  AND dc.sub_count = 0</if> " +
            " <if test='catalogId == null ' >  AND dc.id not in (select device_channel_id from wvp_platform_gb_channel where platform_id=#{platformId} ) </if> " +
            " <if test='catalogId != null ' >  AND pgc.platform_id = #{platformId} and pgc.catalog_id=#{catalogId} </if> " +
            " ORDER BY dc.device_id, dc.channel_id ASC" +
            " </script>"})
    List<ChannelReduce> queryChannelListInAll(@Param("query") String query, @Param("online") Boolean online, @Param("hasSubChannel") Boolean hasSubChannel, @Param("platformId") String platformId, @Param("catalogId") String catalogId);

    @Select(value = {" <script>" +
            "SELECT " +
            "    dc.*,\n" +
            "    pgc.platform_id as platform_id,\n" +
            "    pgc.catalog_id as catalog_id " +
            " FROM wvp_device_channel dc " +
            " LEFT JOIN wvp_platform_gb_channel pgc on pgc.device_channel_id = dc.id " +
            " WHERE pgc.platform_id = #{platformId} " +
            " ORDER BY dc.device_id, dc.channel_id ASC" +
            " </script>"})
    List<DeviceChannelInPlatform> queryChannelByPlatformId(String platformId);


    @Select("SELECT * FROM wvp_device_channel WHERE channel_id=#{channelId}")
    List<DeviceChannel> queryChannelByChannelId( String channelId);

    @Update(value = {"UPDATE wvp_device_channel SET status=false WHERE device_id=#{deviceId} AND channel_id=#{channelId}"})
    void offline(@Param("deviceId") String deviceId, @Param("channelId") String channelId);

    @Update(value = {"UPDATE wvp_device_channel SET status=false WHERE device_id=#{deviceId}"})
    void offlineByDeviceId(String deviceId);

    @Insert("<script> " +
            "insert into wvp_device_channel " +
            "(channel_id, device_id, name, manufacture, model, owner, civil_code, block, sub_count, " +
            "  address, parental, parent_id, safety_way, register_way, cert_num, certifiable, err_code, secrecy, " +
            "  ip_address,port,password,ptz_type,status,stream_id,longitude,latitude,longitude_gcj02,latitude_gcj02,"+
            "  longitude_wgs84,latitude_wgs84,has_audio,create_time,update_time,business_group_id,gps_time)"+
            "values " +
            "<foreach collection='addChannels' index='index' item='item' separator=','> " +
            "(#{item.channelId}, #{item.deviceId}, #{item.name}, #{item.manufacture}, #{item.model}, " +
            "#{item.owner}, #{item.civilCode}, #{item.block},#{item.subCount}," +
            "#{item.address}, #{item.parental}, #{item.parentId}, #{item.safetyWay}, #{item.registerWay}, " +
            "#{item.certNum}, #{item.certifiable}, #{item.errCode}, #{item.secrecy}, " +
            "#{item.ipAddress}, #{item.port}, #{item.password}, #{item.PTZType}, #{item.status}, " +
            "#{item.streamId}, #{item.longitude}, #{item.latitude},#{item.longitudeGcj02}, " +
            "#{item.latitudeGcj02},#{item.longitudeWgs84}, #{item.latitudeWgs84}, #{item.hasAudio}, now(), now(), " +
            "#{item.businessGroupId}, #{item.gpsTime}) " +
            "</foreach> " +
            "</script>")
    int batchAdd(@Param("addChannels") List<DeviceChannel> addChannels);


    @Insert("<script> " +
            "insert into wvp_device_channel " +
            "(channel_id,device_id,name,manufacture,model,owner,civil_code,block,sub_count,"+
            "  address,parental,parent_id,safety_way,register_way,cert_num,certifiable,err_code,secrecy,"+
            "  ip_address,port,password,ptz_type,status,stream_id,longitude,latitude,longitude_gcj02,latitude_gcj02,"+
            "  longitude_wgs84,latitude_wgs84,has_audio,create_time,update_time,business_group_id,gps_time)"+
            "values " +
            "<foreach collection='addChannels' index='index' item='item' separator=','> " +
            "(#{item.channelId}, #{item.deviceId}, #{item.name}, #{item.manufacture}, #{item.model}, " +
            "#{item.owner}, #{item.civilCode}, #{item.block},#{item.subCount}," +
            "#{item.address}, #{item.parental}, #{item.parentId}, #{item.safetyWay}, #{item.registerWay}, " +
            "#{item.certNum}, #{item.certifiable}, #{item.errCode}, #{item.secrecy}, " +
            "#{item.ipAddress}, #{item.port}, #{item.password}, #{item.PTZType}, #{item.status}, " +
            "#{item.streamId}, #{item.longitude}, #{item.latitude},#{item.longitudeGcj02}, " +
            "#{item.latitudeGcj02},#{item.longitudeWgs84}, #{item.latitudeWgs84}, #{item.hasAudio}, now(), now(), " +
            "#{item.businessGroupId}, #{item.gpsTime}) " +
            "</foreach> " +
            "ON DUPLICATE KEY UPDATE " +
            "update_time=VALUES(update_time), " +
            "name=VALUES(name), " +
            "manufacture=VALUES(manufacture), " +
            "model=VALUES(model), " +
            "owner=VALUES(owner), " +
            "civil_code=VALUES(civil_code), " +
            "block=VALUES(block), " +
            "sub_count=VALUES(sub_count), " +
            "address=VALUES(address), " +
            "parental=VALUES(parental), " +
            "parent_id=VALUES(parent_id), " +
            "safety_way=VALUES(safety_way), " +
            "register_way=VALUES(register_way), " +
            "cert_num=VALUES(cert_num), " +
            "certifiable=VALUES(certifiable), " +
            "err_code=VALUES(err_code), " +
            "secrecy=VALUES(secrecy), " +
            "ip_address=VALUES(ip_address), " +
            "port=VALUES(port), " +
            "password=VALUES(password), " +
            "ptz_type=VALUES(ptz_type), " +
            "status=VALUES(status), " +
            "stream_id=VALUES(stream_id), " +
            "longitude=VALUES(longitude), " +
            "latitude=VALUES(latitude), " +
            "longitude_gcj02=VALUES(longitude_gcj02), " +
            "latitude_gcj02=VALUES(latitude_gcj02), " +
            "longitude_wgs84=VALUES(longitude_wgs84), " +
            "latitude_wgs84=VALUES(latitude_wgs84), " +
            "has_audio=VALUES(has_audio), " +
            "business_group_id=VALUES(business_group_id), " +
            "gps_time=VALUES(gps_time)" +
            "</script>")
    int batchAddOrUpdate(List<DeviceChannel> addChannels);

    @Update(value = {"UPDATE wvp_device_channel SET status=true WHERE device_id=#{deviceId} AND channel_id=#{channelId}"})
    void online(@Param("deviceId") String deviceId, @Param("channelId") String channelId);

    @Update({"<script>" +
            "<foreach collection='updateChannels' item='item' separator=';'>" +
            " UPDATE" +
            " wvp_device_channel" +
            " SET update_time=#{item.updateTime}" +
            "<if test='item.name != null'>, name=#{item.name}</if>" +
            "<if test='item.manufacture != null'>, manufacture=#{item.manufacture}</if>" +
            "<if test='item.model != null'>, model=#{item.model}</if>" +
            "<if test='item.owner != null'>, owner=#{item.owner}</if>" +
            "<if test='item.civilCode != null'>, civil_code=#{item.civilCode}</if>" +
            "<if test='item.block != null'>, block=#{item.block}</if>" +
            "<if test='item.subCount != null'>, sub_count=#{item.subCount}</if>" +
            "<if test='item.address != null'>, address=#{item.address}</if>" +
            "<if test='item.parental != null'>, parental=#{item.parental}</if>" +
            "<if test='item.parentId != null'>, parent_id=#{item.parentId}</if>" +
            "<if test='item.safetyWay != null'>, safety_way=#{item.safetyWay}</if>" +
            "<if test='item.registerWay != null'>, register_way=#{item.registerWay}</if>" +
            "<if test='item.certNum != null'>, cert_num=#{item.certNum}</if>" +
            "<if test='item.certifiable != null'>, certifiable=#{item.certifiable}</if>" +
            "<if test='item.errCode != null'>, err_code=#{item.errCode}</if>" +
            "<if test='item.secrecy != null'>, secrecy=#{item.secrecy}</if>" +
            "<if test='item.ipAddress != null'>, ip_address=#{item.ipAddress}</if>" +
            "<if test='item.port != null'>, port=#{item.port}</if>" +
            "<if test='item.password != null'>, password=#{item.password}</if>" +
            "<if test='item.PTZType != null'>, ptz_type=#{item.PTZType}</if>" +
            "<if test='item.status != null'>, status=#{item.status}</if>" +
            "<if test='item.streamId != null'>, stream_id=#{item.streamId}</if>" +
            "<if test='item.hasAudio != null'>, has_audio=#{item.hasAudio}</if>" +
            "<if test='item.longitude != null'>, longitude=#{item.longitude}</if>" +
            "<if test='item.latitude != null'>, latitude=#{item.latitude}</if>" +
            "<if test='item.longitudeGcj02 != null'>, longitude_gcj02=#{item.longitudeGcj02}</if>" +
            "<if test='item.latitudeGcj02 != null'>, latitude_gcj02=#{item.latitudeGcj02}</if>" +
            "<if test='item.longitudeWgs84 != null'>, longitude_wgs84=#{item.longitudeWgs84}</if>" +
            "<if test='item.latitudeWgs84 != null'>, latitude_wgs84=#{item.latitudeWgs84}</if>" +
            "<if test='item.businessGroupId != null'>, business_group_id=#{item.businessGroupId}</if>" +
            "<if test='item.gpsTime != null'>, gps_time=#{item.gpsTime}</if>" +
            "<if test='item.id > 0'>WHERE id=#{item.id}</if>" +
            "<if test='item.id == 0'>WHERE device_id=#{item.deviceId} AND channel_id=#{item.channelId}</if>" +
            "</foreach>" +
            "</script>"})
    int batchUpdate(List<DeviceChannel> updateChannels);


    @Select("SELECT * FROM wvp_device_channel WHERE device_id=#{deviceId} AND status=true")
    List<DeviceChannel> queryOnlineChannelsByDeviceId(String deviceId);

    @Delete(value = {" <script>" +
            "DELETE " +
            "from " +
            "wvp_device_channel " +
            "WHERE " +
            "device_id = #{deviceId} " +
            " AND channel_id NOT IN " +
            "<foreach collection='channels'  item='item'  open='(' separator=',' close=')' > #{item.channelId}</foreach>" +
            " </script>"})
    int cleanChannelsNotInList(@Param("deviceId") String deviceId, @Param("channels") List<DeviceChannel> channels);

    @Update(" update wvp_device_channel" +
            " set sub_count = (select *" +
            "                from (select count(0)" +
            "                      from wvp_device_channel" +
            "                      where device_id = #{deviceId} and parent_id = #{channelId}) as temp)" +
            " where device_id = #{deviceId} " +
            " and channel_id = #{channelId}")
    int updateChannelSubCount(@Param("deviceId") String deviceId, @Param("channelId") String channelId);

    @Update(value = {" <script>" +
            "UPDATE wvp_device_channel " +
            "SET " +
            "latitude=#{latitude}, " +
            "longitude=#{longitude}, " +
            "longitude_gcj02=#{longitudeGcj02}, " +
            "latitude_gcj02=#{latitudeGcj02}, " +
            "longitude_wgs84=#{longitudeWgs84}, " +
            "latitude_wgs84=#{latitudeWgs84}, " +
            "gps_time=#{gpsTime} " +
            "WHERE device_id=#{deviceId} " +
            " <if test='channelId != null' >  AND channel_id=#{channelId}</if>" +
            " </script>"})
    void updatePosition(DeviceChannel deviceChannel);

    @Select("SELECT * FROM wvp_device_channel WHERE length(trim(stream_id)) > 0")
    List<DeviceChannel> getAllChannelInPlay();

    @Select("select * from wvp_device_channel where longitude*latitude > 0 and device_id = #{deviceId}")
    List<DeviceChannel> getAllChannelWithCoordinate(String deviceId);


    @Select(value = {" <script>" +
            "select * " +
            "from wvp_device_channel " +
            "where device_id=#{deviceId}" +
            " <if test='parentId != null and length != null' > and parent_id= #{parentId} or left(channel_id, LENGTH(#{parentId})) = #{parentId} and length(channel_id)=#{length} </if>" +
            " <if test='parentId == null and length != null' > and parent_id= #{parentId} or length(channel_id)=#{length} </if>" +
            " <if test='parentId == null and length == null' > and parent_id= #{parentId} </if>" +
            " <if test='parentId != null and length == null' > and parent_id= #{parentId} or left(channel_id, LENGTH(#{parentId})) = #{parentId} </if>" +
            " </script>"})
    List<DeviceChannel> getChannelsWithCivilCodeAndLength(@Param("deviceId") String deviceId, @Param("parentId") String parentId, @Param("length") Integer length);

    @Select(value = {" <script>" +
            "select * " +
            "from wvp_device_channel " +
            "where device_id=#{deviceId} and length(channel_id)>14 and civil_code=#{parentId}" +
            " </script>"})
    List<DeviceChannel> getChannelsByCivilCode(@Param("deviceId") String deviceId, @Param("parentId") String parentId);

    @Select("select min(length(channel_id)) as minLength " +
            "from wvp_device_channel " +
            "where device_id=#{deviceId}")
    Integer getChannelMinLength(String deviceId);

    @Select("select * from wvp_device_channel where device_id=#{deviceId} and civil_code not in " +
            "(select civil_code from wvp_device_channel where device_id=#{deviceId} group by civil_code)")
    List<DeviceChannel> getChannelWithoutCivilCode(String deviceId);

    @Select("select * from wvp_device_channel where device_id=#{deviceId} and SUBSTRING(channel_id, 11, 3)=#{typeCode}")
    List<DeviceChannel> getBusinessGroups(@Param("deviceId") String deviceId, @Param("typeCode") String typeCode);

    @Select("select dc.id, dc.channel_id, dc.device_id, dc.name, dc.manufacture,dc.model,dc.owner, pc.civil_code,dc.block, " +
            " dc.address, '0' as parental,'0' as channel_type, pc.id as parent_id, dc.safety_way, dc.register_way,dc.cert_num, dc.certifiable,  " +
            " dc.err_code,dc.end_time, dc.secrecy,   dc.ip_address,  dc.port,  dc.ptz_type,  dc.password, dc.status, " +
            " dc.longitude_wgs84 as longitude, dc.latitude_wgs84 as latitude,  pc.business_group_id " +
            " from wvp_device_channel dc" +
            " LEFT JOIN wvp_platform_gb_channel pgc on  dc.id = pgc.device_channel_id" +
            " LEFT JOIN wvp_platform_catalog pc on pgc.catalog_id = pc.id and pgc.platform_id = pc.platform_id" +
            " where pgc.platform_id=#{serverGBId}")
    List<DeviceChannel> queryChannelWithCatalog(String serverGBId);

    @Select("select * from wvp_device_channel where device_id = #{deviceId}")
    List<DeviceChannel> queryAllChannels(String deviceId);


    @Select("select channelId" +
            ", device_id" +
            ", latitude" +
            ", longitude"+
            ",latitude_wgs84"+
            ",longitude_wgs84"+
            ",latitude_gcj02"+
            ",longitude_gcj02"+
            "from wvp_device_channel where device_id = #{deviceId} " +
            "and latitude != 0 " +
            "and  longitude != 0 " +
            "and(latitude_gcj02=0 or latitude_wgs84=0 or longitude_wgs84= 0 or longitude_gcj02 = 0)")
    List<DeviceChannel> getChannelsWithoutTransform(String deviceId);

    @Select("select de.* from wvp_device de left join wvp_device_channel dc on de.device_id = dc.deviceId where dc.channel_id=#{channelId}")
    List<Device> getDeviceByChannelId(String channelId);


    @Delete({"<script>" +
            "<foreach collection='deleteChannelList' item='item' separator=';'>" +
            "DELETE FROM wvp_device_channel WHERE device_id=#{item.deviceId} AND channel_id=#{item.channelId}" +
            "</foreach>" +
            "</script>"})
    int batchDel(@Param("deleteChannelList") List<DeviceChannel> deleteChannelList);

    @Update({"<script>" +
            "<foreach collection='channels' item='item' separator=';'>" +
            "UPDATE wvp_device_channel SET status=true WHERE device_id=#{item.deviceId} AND channel_id=#{item.channelId}" +
            "</foreach>" +
            "</script>"})
    int batchOnline(@Param("channels") List<DeviceChannel> channels);

    @Update({"<script>" +
            "<foreach collection='channels' item='item' separator=';'>" +
            "UPDATE wvp_device_channel SET status= false WHERE device_id=#{item.deviceId} AND channel_id=#{item.channelId}" +
            "</foreach>" +
            "</script>"})
    int batchOffline(List<DeviceChannel> channels);


    @Select("select count(1) from wvp_device_channel where status = true")
    int getOnlineCount();

    @Select("select count(1) from wvp_device_channel")
    int getAllChannelCount();

    // 设备主子码流逻辑START
    @Update(value = {"UPDATE wvp_device_channel SET stream_id=null WHERE device_id=#{deviceId}"})
    void clearPlay(String deviceId);
    // 设备主子码流逻辑END
    @Select(value = {" <script>" +
            "select * " +
            "from wvp_device_channel " +
            "where device_id=#{deviceId}" +
            " <if test='parentId != null and parentId != deviceId'> and parent_id = #{parentId} </if>" +
            " <if test='parentId == null or parentId == deviceId'> and parent_id is null or parent_id = #{deviceId}</if>" +
            " <if test='onlyCatalog == true '> and parental = 1 </if>" +
            " </script>"})
    List<DeviceChannel> getSubChannelsByDeviceId(@Param("deviceId") String deviceId, @Param("parentId") String parentId, @Param("onlyCatalog") boolean onlyCatalog);

}
