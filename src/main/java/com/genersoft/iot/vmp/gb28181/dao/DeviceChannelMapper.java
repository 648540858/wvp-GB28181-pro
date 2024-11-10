package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.controller.bean.ChannelReduce;
import com.genersoft.iot.vmp.gb28181.dao.provider.DeviceChannelProvider;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.streamPush.bean.StreamPush;
import com.genersoft.iot.vmp.web.gb28181.dto.DeviceChannelExtend;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 用于存储设备通道信息
 */
@Mapper
@Repository
public interface DeviceChannelMapper {


    @Insert("<script> " +
            "insert into wvp_device_channel " +
            "(device_id, device_db_id, name, manufacturer, model, owner, civil_code, block, " +
            "address, parental, parent_id, safety_way, register_way, cert_num, certifiable, err_code, end_time, secrecy, " +
            "ip_address, port, password, status, longitude, latitude, ptz_type, position_type, room_type, use_type, " +
            "supply_light_type, direction_type, resolution, business_group_id, download_speed, svc_space_support_mod, " +
            "svc_time_support_mode, create_time, update_time, sub_count, stream_id, has_audio, gps_time, stream_identification, channel_type) " +
            "values " +
            "(#{deviceId}, #{deviceDbId}, #{name}, #{manufacturer}, #{model}, #{owner}, #{civilCode}, #{block}, " +
            "#{address}, #{parental}, #{parentId}, #{safetyWay}, #{registerWay}, #{certNum}, #{certifiable}, #{errCode}, #{endTime}, #{secrecy}, " +
            "#{ipAddress}, #{port}, #{password}, #{status}, #{longitude}, #{latitude}, #{ptzType}, #{positionType}, #{roomType}, #{useType}, " +
            "#{supplyLightType}, #{directionType}, #{resolution}, #{businessGroupId}, #{downloadSpeed}, #{svcSpaceSupportMod}," +
            " #{svcTimeSupportMode}, #{createTime}, #{updateTime}, #{subCount}, #{streamId}, #{hasAudio}, #{gpsTime}, #{streamIdentification}, #{channelType}) " +
            "</script>")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int add(DeviceChannel channel);

    @Update(value = {" <script>" +
            "UPDATE wvp_device_channel " +
            "SET update_time=#{updateTime}" +
            ", device_id=#{deviceId}" +
            ", device_db_id=#{deviceDbId}" +
            ", name=#{name}" +
            ", manufacturer=#{manufacturer}" +
            ", model=#{model}" +
            ", owner=#{owner}" +
            ", civil_code=#{civilCode}" +
            ", block=#{block}" +
            ", address=#{address}" +
            ", parental=#{parental}" +
            ", parent_id=#{parentId}" +
            ", safety_way=#{safetyWay}" +
            ", register_way=#{registerWay}" +
            ", cert_num=#{certNum}" +
            ", certifiable=#{certifiable}" +
            ", err_code=#{errCode}" +
            ", end_time=#{endTime}" +
            ", secrecy=#{secrecy}" +
            ", ip_address=#{ipAddress}" +
            ", port=#{port}" +
            ", password=#{password}" +
            ", status=#{status}" +
            ", longitude=#{longitude}" +
            ", latitude=#{latitude}" +
            ", ptz_type=#{ptzType}" +
            ", position_type=#{positionType}" +
            ", room_type=#{roomType}" +
            ", use_type=#{useType}" +
            ", supply_light_type=#{supplyLightType}" +
            ", direction_type=#{directionType}" +
            ", resolution=#{resolution}" +
            ", business_group_id=#{businessGroupId}" +
            ", download_speed=#{downloadSpeed}" +
            ", svc_space_support_mod=#{svcSpaceSupportMod}" +
            ", svc_time_support_mode=#{svcTimeSupportMode}" +
            ", sub_count=#{subCount}" +
            ", stream_id=#{streamId}" +
            ", has_audio=#{hasAudio}" +
            ", gps_time=#{gpsTime}" +
            ", stream_identification=#{streamIdentification}" +
            ", channel_type=#{channelType}" +
            " WHERE id=#{id}" +
            " </script>"})
    int update(DeviceChannel channel);

    @SelectProvider(type = DeviceChannelProvider.class, method = "queryChannels")
    List<DeviceChannel> queryChannels(@Param("deviceDbId") int deviceDbId, @Param("civilCode") String civilCode,
                                      @Param("businessGroupId") String businessGroupId, @Param("parentChannelId") String parentChannelId,
                                      @Param("query") String query, @Param("hasSubChannel") Boolean hasSubChannel,
                                      @Param("online") Boolean online, @Param("channelIds") List<String> channelIds);

    @SelectProvider(type = DeviceChannelProvider.class, method = "queryChannelsByDeviceDbId")
    List<DeviceChannel> queryChannelsByDeviceDbId(@Param("deviceDbId") int deviceDbId);

    @Delete("DELETE FROM wvp_device_channel WHERE device_db_id=#{deviceId}")
    int cleanChannelsByDeviceId(@Param("deviceId") int deviceId);

    @Delete("DELETE FROM wvp_device_channel WHERE id=#{id}")
    int del(@Param("id") int id);

    @Select(value = {" <script>" +
            " SELECT " +
            " dc.id,\n" +
            " dc.create_time,\n" +
            " dc.update_time,\n" +
            " dc.sub_count,\n" +
            " coalesce(dc.gb_device_id, dc.device_id) as channel_id,\n" +
            " de.device_id as device_id,\n" +
            " coalesce(dc.gb_name, dc.name) as name,\n" +
            " de.name as device_name,\n" +
            " de.on_line as device_online,\n" +
            " coalesce(dc.gb_manufacturer, dc.manufacturer) as manufacture,\n" +
            " coalesce(dc.gb_model, dc.model) as model,\n" +
            " coalesce(dc.gb_owner, dc.owner) as owner,\n" +
            " coalesce(dc.gb_civil_code, dc.civil_code) as civil_code,\n" +
            " coalesce(dc.gb_block, dc.block) as block,\n" +
            " coalesce(dc.gb_address, dc.address) as address,\n" +
            " coalesce(dc.gb_parental, dc.parental) as parental,\n" +
            " coalesce(dc.gb_parent_id, dc.parent_id) as parent_id,\n" +
            " coalesce(dc.gb_safety_way, dc.safety_way) as safety_way,\n" +
            " coalesce(dc.gb_register_way, dc.register_way) as register_way,\n" +
            " coalesce(dc.gb_cert_num, dc.cert_num) as cert_num,\n" +
            " coalesce(dc.gb_certifiable, dc.certifiable) as certifiable,\n" +
            " coalesce(dc.gb_err_code, dc.err_code) as err_code,\n" +
            " coalesce(dc.gb_end_time, dc.end_time) as end_time,\n" +
            " coalesce(dc.gb_secrecy, dc.secrecy) as secrecy,\n" +
            " coalesce(dc.gb_ip_address, dc.ip_address) as ip_address,\n" +
            " coalesce(dc.gb_port, dc.port) as port,\n" +
            " coalesce(dc.gb_password, dc.password) as password,\n" +
            " coalesce(dc.gb_ptz_type, dc.ptz_type) as ptz_type,\n" +
            " coalesce(dc.gb_status, dc.status) as status,\n" +
            " coalesce(dc.gb_longitude, dc.longitude) as longitude,\n" +
            " coalesce(dc.gb_latitude, dc.latitude) as latitude,\n" +
            " coalesce(dc.gb_business_group_id, dc.business_group_id) as business_group_id " +
            " from " +
            " wvp_device_channel dc " +
            " LEFT JOIN wvp_device de ON dc.device_db_id = de.id " +
            " WHERE 1=1" +
            " <if test='deviceId != null'> AND de.device_id = #{deviceId} </if> " +
            " <if test='query != null'> AND (dc.device_id LIKE '%${query}%' OR dc.name LIKE '%${query}%' OR dc.name LIKE '%${query}%')</if> " +
            " <if test='parentChannelId != null'> AND dc.parent_id=#{parentChannelId} </if> " +
            " <if test='online == true' > AND dc.status='ON'</if>" +
            " <if test='online == false' > AND dc.status='OFF'</if>" +
            " <if test='hasSubChannel == true' >  AND dc.sub_count > 0 </if>" +
            " <if test='hasSubChannel == false' >  AND dc.sub_count = 0 </if>" +
            "<if test='channelIds != null'> AND dc.device_id in <foreach item='item' index='index' collection='channelIds' open='(' separator=',' close=')'>" +
            "#{item} " +
            "</foreach> </if>" +
            "ORDER BY dc.device_id ASC" +
            " </script>"})
    List<DeviceChannelExtend> queryChannelsWithDeviceInfo(@Param("deviceId") String deviceId, @Param("parentChannelId") String parentChannelId, @Param("query") String query, @Param("hasSubChannel") Boolean hasSubChannel, @Param("online") Boolean online, @Param("channelIds") List<String> channelIds);

    @Update(value = {"UPDATE wvp_device_channel SET stream_id=#{streamId} WHERE id=#{channelId}"})
    void startPlay(@Param("channelId") Integer channelId, @Param("streamId") String streamId);


    @Select(value = {" <script>" +
            "SELECT " +
            " dc.id,\n" +
            " COALESCE(dc.gb_device_id, dc.device_id) AS name,\n" +
            " COALESCE(dc.gb_name, dc.name) AS name,\n" +
            " COALESCE(dc.gb_manufacturer, dc.manufacturer) AS manufacturer,\n" +
            " COALESCE(dc.gb_ip_address, dc.ip_address) AS ip_address,\n" +
            " dc.sub_count,\n" +
            " pgc.platform_id as platform_id,\n" +
            " pgc.catalog_id as catalog_id " +
            " FROM wvp_device_channel dc " +
            " LEFT JOIN wvp_device de ON dc.device_db_id = de.id " +
            " LEFT JOIN wvp_platform_channel pgc on pgc.device_channel_id = dc.id " +
            " WHERE 1=1 " +
            " <if test='query != null'> " +
            "AND " +
            "(COALESCE(dc.gb_device_id, dc.device_id) LIKE concat('%',#{query},'%') " +
            " OR COALESCE(dc.gb_name, dc.name) LIKE concat('%',#{query},'%'))</if> " +
            " <if test='online == true' > AND dc.status='ON'</if> " +
            " <if test='online == false' > AND dc.status='OFF'</if> " +
            " <if test='hasSubChannel!= null and hasSubChannel == true' >  AND dc.sub_count > 0</if> " +
            " <if test='hasSubChannel!= null and hasSubChannel == false' >  AND dc.sub_count = 0</if> " +
            " <if test='catalogId == null ' >  AND dc.id not in (select device_channel_id from wvp_platform_channel where platform_id=#{platformId} ) </if> " +
            " <if test='catalogId != null ' >  AND pgc.platform_id = #{platformId} and pgc.catalog_id=#{catalogId} </if> " +
            " ORDER BY COALESCE(dc.gb_device_id, dc.device_id) ASC" +
            " </script>"})
    List<ChannelReduce> queryChannelListInAll(@Param("query") String query, @Param("online") Boolean online, @Param("hasSubChannel") Boolean hasSubChannel, @Param("platformId") String platformId, @Param("catalogId") String catalogId);


    @Update(value = {"UPDATE wvp_device_channel SET status='OFF' WHERE id=#{id}"})
    void offline(@Param("id") int id);

    @Insert("<script> " +
            "insert into wvp_device_channel " +
            "(device_id, device_db_id, name, manufacturer, model, owner, civil_code, block, " +
            "address, parental, parent_id, safety_way, register_way, cert_num, certifiable, err_code, end_time, secrecy, " +
            "ip_address, port, password, status, longitude, latitude, ptz_type, position_type, room_type, use_type, " +
            "supply_light_type, direction_type, resolution, business_group_id, download_speed, svc_space_support_mod, " +
            "svc_time_support_mode, create_time, update_time, sub_count, stream_id, has_audio, gps_time, stream_identification, channel_type) " +
            "values " +
            "<foreach collection='addChannels' index='index' item='item' separator=','> " +
            "(#{item.deviceId}, #{item.deviceDbId}, #{item.name}, #{item.manufacturer}, #{item.model}, #{item.owner}, #{item.civilCode}, #{item.block}, " +
            "#{item.address}, #{item.parental}, #{item.parentId}, #{item.safetyWay}, #{item.registerWay}, #{item.certNum}, #{item.certifiable}, #{item.errCode}, #{item.endTime}, #{item.secrecy}, " +
            "#{item.ipAddress}, #{item.port}, #{item.password}, #{item.status}, #{item.longitude}, #{item.latitude}, #{item.ptzType}, #{item.positionType}, #{item.roomType}, #{item.useType}, " +
            "#{item.supplyLightType}, #{item.directionType}, #{item.resolution}, #{item.businessGroupId}, #{item.downloadSpeed}, #{item.svcSpaceSupportMod}," +
            " #{item.svcTimeSupportMode}, #{item.createTime}, #{item.updateTime}, #{item.subCount}, #{item.streamId}, #{item.hasAudio}, #{item.gpsTime}, #{item.streamIdentification}, #{item.channelType}) " +
            "</foreach> " +
            "</script>")
    int batchAdd(@Param("addChannels") List<DeviceChannel> addChannels);


    @Update(value = {"UPDATE wvp_device_channel SET status='ON' WHERE id=#{id}"})
    void online(@Param("id") int id);

    @Update({"<script>" +
            "<foreach collection='updateChannels' item='item' separator=';'>" +
            " UPDATE" +
            " wvp_device_channel" +
            " SET update_time=#{item.updateTime}" +
            ", device_id=#{item.deviceId}" +
            ", device_db_id=#{item.deviceDbId}" +
            ", name=#{item.name}" +
            ", manufacturer=#{item.manufacturer}" +
            ", model=#{item.model}" +
            ", owner=#{item.owner}" +
            ", civil_code=#{item.civilCode}" +
            ", block=#{item.block}" +
            ", address=#{item.address}" +
            ", parental=#{item.parental}" +
            ", parent_id=#{item.parentId}" +
            ", safety_way=#{item.safetyWay}" +
            ", register_way=#{item.registerWay}" +
            ", cert_num=#{item.certNum}" +
            ", certifiable=#{item.certifiable}" +
            ", err_code=#{item.errCode}" +
            ", end_time=#{item.endTime}" +
            ", secrecy=#{item.secrecy}" +
            ", ip_address=#{item.ipAddress}" +
            ", port=#{item.port}" +
            ", password=#{item.password}" +
            ", status=#{item.status}" +
            ", longitude=#{item.longitude}" +
            ", latitude=#{item.latitude}" +
            ", ptz_type=#{item.ptzType}" +
            ", position_type=#{item.positionType}" +
            ", room_type=#{item.roomType}" +
            ", use_type=#{item.useType}" +
            ", supply_light_type=#{item.supplyLightType}" +
            ", direction_type=#{item.directionType}" +
            ", resolution=#{item.resolution}" +
            ", business_group_id=#{item.businessGroupId}" +
            ", download_speed=#{item.downloadSpeed}" +
            ", svc_space_support_mod=#{item.svcSpaceSupportMod}" +
            ", svc_time_support_mode=#{item.svcTimeSupportMode}" +
            ", sub_count=#{item.subCount}" +
            ", stream_id=#{item.streamId}" +
            ", has_audio=#{item.hasAudio}" +
            ", gps_time=#{item.gpsTime}" +
            ", stream_identification=#{item.streamIdentification}" +
            ", channel_type=#{item.channelType}" +
            " WHERE id=#{item.id}" +
            "</foreach>" +
            "</script>"})
    int batchUpdate(List<DeviceChannel> updateChannels);


    @Update({"<script>" +
            "<foreach collection='updateChannels' item='item' separator=';'>" +
            " UPDATE" +
            " wvp_device_channel" +
            " SET update_time=#{item.updateTime}" +
            ", device_id=#{item.deviceId}" +
            ", device_db_id=#{item.deviceDbId}" +
            ", name=#{item.name}" +
            ", manufacturer=#{item.manufacturer}" +
            ", model=#{item.model}" +
            ", owner=#{item.owner}" +
            ", civil_code=#{item.civilCode}" +
            ", block=#{item.block}" +
            ", address=#{item.address}" +
            ", parental=#{item.parental}" +
            ", parent_id=#{item.parentId}" +
            ", safety_way=#{item.safetyWay}" +
            ", register_way=#{item.registerWay}" +
            ", cert_num=#{item.certNum}" +
            ", certifiable=#{item.certifiable}" +
            ", err_code=#{item.errCode}" +
            ", end_time=#{item.endTime}" +
            ", secrecy=#{item.secrecy}" +
            ", ip_address=#{item.ipAddress}" +
            ", port=#{item.port}" +
            ", password=#{item.password}" +
            ", status=#{item.status}" +
            ", longitude=#{item.longitude}" +
            ", latitude=#{item.latitude}" +
            ", ptz_type=#{item.ptzType}" +
            ", position_type=#{item.positionType}" +
            ", room_type=#{item.roomType}" +
            ", use_type=#{item.useType}" +
            ", supply_light_type=#{item.supplyLightType}" +
            ", direction_type=#{item.directionType}" +
            ", resolution=#{item.resolution}" +
            ", business_group_id=#{item.businessGroupId}" +
            ", download_speed=#{item.downloadSpeed}" +
            ", svc_space_support_mod=#{item.svcSpaceSupportMod}" +
            ", svc_time_support_mode=#{item.svcTimeSupportMode}" +
            ", sub_count=#{item.subCount}" +
            ", stream_id=#{item.streamId}" +
            ", has_audio=#{item.hasAudio}" +
            ", gps_time=#{item.gpsTime}" +
            ", stream_identification=#{item.streamIdentification}" +
            ", channel_type=#{item.channelType}" +
            " WHERE device_db_id = #{item.deviceDbId} and device_id=#{item.deviceId}" +
            "</foreach>" +
            "</script>"})
    int batchUpdateForNotify(List<DeviceChannel> updateChannels);

    @Update(" update wvp_device_channel" +
            " set sub_count = (select *" +
            "             from (select count(0)" +
            "                   from wvp_device_channel" +
            "                   where device_db_id = #{deviceDbId} and parent_id = #{channelId}) as temp)" +
            " where device_db_id = #{deviceDbId} and device_id = #{channelId}")
    int updateChannelSubCount(@Param("deviceDbId") int deviceDbId, @Param("channelId") String channelId);

    @Update(value = {" <script>" +
            " UPDATE wvp_device_channel " +
            " SET " +
            " latitude=#{latitude}, " +
            " longitude=#{longitude}, " +
            " gps_time=#{gpsTime} " +
            " WHERE id=#{id} " +
            " </script>"})
    int updatePosition(DeviceChannel deviceChannel);

    @Select("select " +
            " id,\n" +
            " device_db_id,\n" +
            " create_time,\n" +
            " update_time,\n" +
            " sub_count,\n" +
            " stream_id,\n" +
            " has_audio,\n" +
            " gps_time,\n" +
            " stream_identification,\n" +
            " channel_type,\n" +
            " device_id,\n" +
            " name,\n" +
            " manufacturer,\n" +
            " model,\n" +
            " owner,\n" +
            " civil_code,\n" +
            " block,\n" +
            " address,\n" +
            " parental,\n" +
            " parent_id,\n" +
            " safety_way,\n" +
            " register_way,\n" +
            " cert_num,\n" +
            " certifiable,\n" +
            " err_code,\n" +
            " end_time,\n" +
            " secrecy,\n" +
            " ip_address,\n" +
            " port,\n" +
            " password,\n" +
            " status,\n" +
            " longitude,\n" +
            " latitude,\n" +
            " ptz_type,\n" +
            " position_type,\n" +
            " room_type,\n" +
            " use_type,\n" +
            " supply_light_type,\n" +
            " direction_type,\n" +
            " resolution,\n" +
            " business_group_id,\n" +
            " download_speed,\n" +
            " svc_space_support_mod,\n" +
            " svc_time_support_mode\n" +
            " from wvp_device_channel where device_db_id = #{deviceDbId}")
    List<DeviceChannel> queryAllChannelsForRefresh(@Param("deviceDbId") int deviceDbId);

    @Select("select de.* from wvp_device de left join wvp_device_channel dc on de.device_id = dc.device_id where dc.device_id=#{channelId}")
    List<Device> getDeviceByChannelDeviceId(String channelId);


    @Delete({"<script>" +
            "<foreach collection='deleteChannelList' item='item' separator=';'>" +
            "DELETE FROM wvp_device_channel WHERE id=#{item.id}" +
            "</foreach>" +
            "</script>"})
    int batchDel(List<DeviceChannel> deleteChannelList);

    @Update({"<script>" +
            "<foreach collection='channels' item='item' separator=';'>" +
            "UPDATE wvp_device_channel SET status=#{item.status} WHERE device_id=#{item.deviceId}" +
            "</foreach>" +
            "</script>"})
    int batchUpdateStatus(List<DeviceChannel> channels);

    @Select("select count(1) from wvp_device_channel where status = 'ON'")
    int getOnlineCount();

    @Select("select count(1) from wvp_device_channel")
    int getAllChannelCount();

    @Update("<script>" +
            "UPDATE wvp_device_channel SET stream_identification=#{streamIdentification} WHERE id=#{id}" +
            "</script>")
    void updateChannelStreamIdentification(DeviceChannel channel);


    @Update({"<script>" +
            "<foreach collection='channelList' item='item' separator=';'>" +
            " UPDATE" +
            " wvp_device_channel" +
            " SET update_time=#{item.updateTime}" +
            "<if test='item.longitude != null'>, longitude=#{item.longitude}</if>" +
            "<if test='item.latitude != null'>, latitude=#{item.latitude}</if>" +
            "<if test='item.gpsTime != null'>, gps_time=#{item.gpsTime}</if>" +
            "<if test='item.id > 0'>WHERE id=#{item.id}</if>" +
            "<if test='item.id == 0'>WHERE device_db_id=#{item.deviceDbId} AND device_id=#{item.deviceId}</if>" +
            "</foreach>" +
            "</script>"})
    void batchUpdatePosition(List<DeviceChannel> channelList);

    @SelectProvider(type = DeviceChannelProvider.class, method = "getOne")
    DeviceChannel getOne(@Param("id") int id);

    @Select(value = {" <script>" +
            " SELECT " +
            " id,\n" +
            " device_db_id,\n" +
            " create_time,\n" +
            " update_time,\n" +
            " sub_count,\n" +
            " stream_id,\n" +
            " has_audio,\n" +
            " gps_time,\n" +
            " stream_identification,\n" +
            " channel_type,\n" +
            " device_id,\n" +
            " name,\n" +
            " manufacturer,\n" +
            " model,\n" +
            " owner,\n" +
            " civil_code,\n" +
            " block,\n" +
            " address,\n" +
            " parental,\n" +
            " parent_id,\n" +
            " safety_way,\n" +
            " register_way,\n" +
            " cert_num,\n" +
            " certifiable,\n" +
            " err_code,\n" +
            " end_time,\n" +
            " secrecy,\n" +
            " ip_address,\n" +
            " port,\n" +
            " password,\n" +
            " status,\n" +
            " longitude,\n" +
            " latitude,\n" +
            " ptz_type,\n" +
            " position_type,\n" +
            " room_type,\n" +
            " use_type,\n" +
            " supply_light_type,\n" +
            " direction_type,\n" +
            " resolution,\n" +
            " business_group_id,\n" +
            " download_speed,\n" +
            " svc_space_support_mod,\n" +
            " svc_time_support_mode\n" +
            " from wvp_device_channel " +
            " where id=#{id}" +
            " </script>"})
    DeviceChannel getOneForSource(@Param("id") int id);

    @SelectProvider(type = DeviceChannelProvider.class, method = "getOneByDeviceId")
    DeviceChannel getOneByDeviceId(@Param("deviceDbId") int deviceDbId, @Param("channelId") String channelId);


    @Select(value = {" <script>" +
            " SELECT " +
            " id,\n" +
            " device_db_id,\n" +
            " create_time,\n" +
            " update_time,\n" +
            " sub_count,\n" +
            " stream_id,\n" +
            " has_audio,\n" +
            " gps_time,\n" +
            " stream_identification,\n" +
            " channel_type,\n" +
            " device_id,\n" +
            " name,\n" +
            " manufacturer,\n" +
            " model,\n" +
            " owner,\n" +
            " civil_code,\n" +
            " block,\n" +
            " address,\n" +
            " parental,\n" +
            " parent_id,\n" +
            " safety_way,\n" +
            " register_way,\n" +
            " cert_num,\n" +
            " certifiable,\n" +
            " err_code,\n" +
            " end_time,\n" +
            " secrecy,\n" +
            " ip_address,\n" +
            " port,\n" +
            " password,\n" +
            " status,\n" +
            " longitude,\n" +
            " latitude,\n" +
            " ptz_type,\n" +
            " position_type,\n" +
            " room_type,\n" +
            " use_type,\n" +
            " supply_light_type,\n" +
            " direction_type,\n" +
            " resolution,\n" +
            " business_group_id,\n" +
            " download_speed,\n" +
            " svc_space_support_mod,\n" +
            " svc_time_support_mode\n" +
            " from wvp_device_channel " +
            " where device_db_id=#{deviceDbId} and coalesce(gb_device_id, device_id) = #{channelId}" +
            " </script>"})
    DeviceChannel getOneByDeviceIdForSource(@Param("deviceDbId") int deviceDbId, @Param("channelId") String channelId);


    @Update(value = {"UPDATE wvp_device_channel SET stream_id=null WHERE id=#{channelId}"})
    void stopPlayById(@Param("channelId") Integer channelId);

    @Update(value = {" <script>" +
            "UPDATE wvp_device_channel " +
            "SET has_audio=#{audio}" +
            " WHERE id=#{channelId}" +
            " </script>"})
    void changeAudio(@Param("channelId") int channelId, @Param("audio") boolean audio);

    @Update("<script> " +
            "<foreach collection='gpsMsgInfoList' index='index' item='item' separator=';'> " +
            "UPDATE wvp_device_channel SET gb_longitude = #{item.lng}, gb_latitude=#{item.lat} WHERE id = #{item.channelId}" +
            "</foreach> " +
            "</script>")
    void updateStreamGPS(List<GPSMsgInfo> gpsMsgInfoList);

    @Update("UPDATE wvp_device_channel SET status=#{status} WHERE device_id=#{deviceId} AND channel_id=#{channelId}")
    void updateStatus(DeviceChannel channel);


    @Update({"<script>" +
            " UPDATE" +
            " wvp_device_channel" +
            " SET update_time=#{updateTime}" +
            ", device_id=#{deviceId}" +
            ", device_db_id=#{deviceDbId}" +
            ", name=#{name}" +
            ", manufacturer=#{manufacturer}" +
            ", model=#{model}" +
            ", owner=#{owner}" +
            ", civil_code=#{civilCode}" +
            ", block=#{block}" +
            ", address=#{address}" +
            ", parental=#{parental}" +
            ", parent_id=#{parentId}" +
            ", safety_way=#{safetyWay}" +
            ", register_way=#{registerWay}" +
            ", cert_num=#{certNum}" +
            ", certifiable=#{certifiable}" +
            ", err_code=#{errCode}" +
            ", end_time=#{endTime}" +
            ", secrecy=#{secrecy}" +
            ", ip_address=#{ipAddress}" +
            ", port=#{port}" +
            ", password=#{password}" +
            ", status=#{status}" +
            ", longitude=#{longitude}" +
            ", latitude=#{latitude}" +
            ", ptz_type=#{ptzType}" +
            ", position_type=#{positionType}" +
            ", room_type=#{roomType}" +
            ", use_type=#{useType}" +
            ", supply_light_type=#{supplyLightType}" +
            ", direction_type=#{directionType}" +
            ", resolution=#{resolution}" +
            ", business_group_id=#{businessGroupId}" +
            ", download_speed=#{downloadSpeed}" +
            ", svc_space_support_mod=#{svcSpaceSupportMod}" +
            ", svc_time_support_mode=#{svcTimeSupportMode}" +
            ", sub_count=#{subCount}" +
            ", stream_id=#{streamId}" +
            ", has_audio=#{hasAudio}" +
            ", gps_time=#{gpsTime}" +
            ", stream_identification=#{streamIdentification}" +
            ", channel_type=#{channelType}" +
            " WHERE id = #{id}" +
            "</script>"})
    void updateChannelForNotify(DeviceChannel channel);


    @Select(value = {" <script>" +
            " SELECT " +
            " id,\n" +
            " device_db_id,\n" +
            " create_time,\n" +
            " update_time,\n" +
            " sub_count,\n" +
            " stream_id,\n" +
            " has_audio,\n" +
            " gps_time,\n" +
            " stream_identification,\n" +
            " channel_type,\n" +
            " device_id,\n" +
            " name,\n" +
            " manufacturer,\n" +
            " model,\n" +
            " owner,\n" +
            " civil_code,\n" +
            " block,\n" +
            " address,\n" +
            " parental,\n" +
            " parent_id,\n" +
            " safety_way,\n" +
            " register_way,\n" +
            " cert_num,\n" +
            " certifiable,\n" +
            " err_code,\n" +
            " end_time,\n" +
            " secrecy,\n" +
            " ip_address,\n" +
            " port,\n" +
            " password,\n" +
            " status,\n" +
            " longitude,\n" +
            " latitude,\n" +
            " ptz_type,\n" +
            " position_type,\n" +
            " room_type,\n" +
            " use_type,\n" +
            " supply_light_type,\n" +
            " direction_type,\n" +
            " resolution,\n" +
            " business_group_id,\n" +
            " download_speed,\n" +
            " svc_space_support_mod,\n" +
            " svc_time_support_mode\n" +
            " from wvp_device_channel " +
            " where device_db_id=#{deviceDbId} and device_id = #{channelId}" +
            " </script>"})
    DeviceChannel getOneBySourceChannelId(int deviceDbId, String channelId);
}
