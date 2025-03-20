package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.gb28181.bean.*;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Mapper
@Repository
public interface PlatformChannelMapper {


    @Insert("<script> "+
            "INSERT INTO wvp_platform_channel (platform_id, device_channel_id) VALUES" +
            "<foreach collection='channelList'  item='item' separator=','>" +
            " (#{platformId}, #{item.gbId} )" +
            "</foreach>" +
            "</script>")
    int addChannels(@Param("platformId") Integer platformId, @Param("channelList") List<CommonGBChannel> channelList);

    @Delete("<script> "+
            "DELETE from wvp_platform_channel WHERE device_channel_id in " +
            "( select  temp.device_channel_id from " +
            "(select pgc.device_channel_id from wvp_platform_channel pgc " +
            "left join wvp_device_channel dc on dc.id = pgc.device_channel_id where dc.channel_type = 0 and dc.device_id  =#{deviceId} " +
            ") temp)" +
            "</script>")
    int delChannelForDeviceId(String deviceId);

    @Select("select d.*\n" +
            "from wvp_platform_channel pgc\n" +
            "         left join wvp_device_channel dc on dc.id = pgc.device_channel_id\n" +
            "         left join wvp_device d on dc.device_id = d.device_id\n" +
            "where  dc.channel_type = 0 and dc.channel_id = #{channelId} and pgc.platform_id=#{platformId}")
    List<Device> queryDeviceByPlatformIdAndChannelId(@Param("platformId") String platformId, @Param("channelId") String channelId);

    @Select("<script> " +
            " SELECT " +
            " wp.* " +
            " FROM " +
            " wvp_platform wp " +
            " left join wvp_platform_channel wpgc on " +
            " wp.id = wpgc.platform_id " +
            " WHERE " +
            " wpgc.device_channel_id = #{channelId} and wp.status = true " +
            " AND wp.server_gb_id in " +
            "<foreach collection='platforms' item='item'  open='(' separator=',' close=')' > #{item}</foreach>" +
            "</script> ")
    List<Platform> queryPlatFormListForGBWithGBId(@Param("channelId") Integer channelId, List<String> platforms);

    @Select("select dc.channel_id, dc.device_id,dc.name,d.manufacturer,d.model,d.firmware\n" +
            "from wvp_platform_channel pgc\n" +
            "         left join wvp_device_channel dc on dc.id = pgc.device_channel_id\n" +
            "         left join wvp_device d on dc.device_id = d.device_id\n" +
            "where dc.channel_type = 0 and dc.channel_id = #{channelId} and pgc.platform_id=#{platformId}")
    List<Device> queryDeviceInfoByPlatformIdAndChannelId(@Param("platformId") String platformId, @Param("channelId") String channelId);

    @Select(" SELECT wp.* from wvp_platform_channel pgc " +
            " left join wvp_device_channel dc on dc.id = pgc.device_channel_id " +
            " left join  wvp_platform wp on wp.id = pgc.platform_id" +
            " WHERE  dc.channel_type = 0 and dc.device_id=#{channelId}")
    List<Platform> queryParentPlatformByChannelId(@Param("channelId") String channelId);

    @Select("<script>" +
            " select " +
            "    wpgc.id ,\n" +
            "    wdc.id as gb_id,\n" +
            "    wdc.data_type ,\n" +
            "    wdc.data_device_id,\n" +
            "    wdc.create_time,\n" +
            "    wdc.update_time,\n" +
            "    wpgc.custom_device_id, \n" +
            "    wpgc.custom_name, \n" +
            "    wpgc.custom_manufacturer, \n" +
            "    wpgc.custom_model, \n" +
            "    wpgc.custom_owner, \n" +
            "    wpgc.custom_civil_code,\n" +
            "    wpgc.custom_block, \n" +
            "    wpgc.custom_address,\n" +
            "    wpgc.custom_parental, \n" +
            "    wpgc.custom_parent_id, \n" +
            "    wpgc.custom_safety_way, \n" +
            "    wpgc.custom_register_way, \n" +
            "    wpgc.custom_cert_num, \n" +
            "    wpgc.custom_certifiable, \n" +
            "    wpgc.custom_err_code, \n" +
            "    wpgc.custom_end_time, \n" +
            "    wpgc.custom_secrecy, \n" +
            "    wpgc.custom_ip_address, \n" +
            "    wpgc.custom_port, \n" +
            "    wpgc.custom_password, \n" +
            "    wpgc.custom_status, \n" +
            "    wpgc.custom_longitude, \n" +
            "    wpgc.custom_latitude, \n" +
            "    wpgc.custom_ptz_type, \n" +
            "    wpgc.custom_position_type, \n" +
            "    wpgc.custom_room_type, \n" +
            "    wpgc.custom_use_type, \n" +
            "    wpgc.custom_supply_light_type, \n" +
            "    wpgc.custom_direction_type, \n" +
            "    wpgc.custom_resolution, \n" +
            "    wpgc.custom_business_group_id, \n" +
            "    wpgc.custom_download_speed, \n" +
            "    wpgc.custom_svc_space_support_mod,\n" +
            "    wpgc.custom_svc_time_support_mode," +
            "    coalesce( wdc.gb_device_id, wdc.device_id) as gb_device_id,\n" +
            "    coalesce( wdc.gb_name, wdc.name) as gb_name,\n" +
            "    coalesce( wdc.gb_manufacturer, wdc.manufacturer) as gb_manufacturer,\n" +
            "    coalesce( wdc.gb_model, wdc.model) as gb_model,\n" +
            "    coalesce( wdc.gb_owner, wdc.owner) as gb_owner,\n" +
            "    coalesce( wdc.gb_civil_code, wdc.civil_code) as gb_civil_code,\n" +
            "    coalesce( wdc.gb_block, wdc.block) as gb_block,\n" +
            "    coalesce( wdc.gb_address, wdc.address) as gb_address,\n" +
            "    coalesce( wdc.gb_parental, wdc.parental) as gb_parental,\n" +
            "    coalesce( wdc.gb_parent_id, wdc.parent_id) as gb_parent_id,\n" +
            "    coalesce( wdc.gb_safety_way, wdc.safety_way) as gb_safety_way,\n" +
            "    coalesce( wdc.gb_register_way, wdc.register_way) as gb_register_way,\n" +
            "    coalesce( wdc.gb_cert_num, wdc.cert_num) as gb_cert_num,\n" +
            "    coalesce( wdc.gb_certifiable, wdc.certifiable) as gb_certifiable,\n" +
            "    coalesce( wdc.gb_err_code, wdc.err_code) as gb_err_code,\n" +
            "    coalesce( wdc.gb_end_time, wdc.end_time) as gb_end_time,\n" +
            "    coalesce( wdc.gb_secrecy, wdc.secrecy) as gb_secrecy,\n" +
            "    coalesce( wdc.gb_ip_address, wdc.ip_address) as gb_ip_address,\n" +
            "    coalesce( wdc.gb_port, wdc.port) as gb_port,\n" +
            "    coalesce( wdc.gb_password, wdc.password) as gb_password,\n" +
            "    coalesce( wdc.gb_status, wdc.status) as gb_status,\n" +
            "    coalesce( wdc.gb_longitude, wdc.longitude) as gb_longitude,\n" +
            "    coalesce( wdc.gb_latitude, wdc.latitude) as gb_latitude,\n" +
            "    coalesce( wdc.gb_ptz_type, wdc.ptz_type) as gb_ptz_type,\n" +
            "    coalesce( wdc.gb_position_type, wdc.position_type) as gb_position_type,\n" +
            "    coalesce( wdc.gb_room_type, wdc.room_type) as gb_room_type,\n" +
            "    coalesce( wdc.gb_use_type, wdc.use_type) as gb_use_type,\n" +
            "    coalesce( wdc.gb_supply_light_type, wdc.supply_light_type) as gb_supply_light_type,\n" +
            "    coalesce( wdc.gb_direction_type, wdc.direction_type) as gb_direction_type,\n" +
            "    coalesce( wdc.gb_resolution, wdc.resolution) as gb_resolution,\n" +
            "    coalesce( wdc.gb_business_group_id, wdc.business_group_id) as gb_business_group_id,\n" +
            "    coalesce( wdc.gb_download_speed, wdc.download_speed) as gb_download_speed,\n" +
            "    coalesce( wdc.gb_svc_space_support_mod, wdc.svc_space_support_mod) as gb_svc_space_support_mod,\n" +
            "    coalesce( wdc.gb_svc_time_support_mode, wdc.svc_time_support_mode) as gb_svc_time_support_mode, \n" +
            "    wpgc.platform_id " +
            " from wvp_device_channel wdc" +
            " left join wvp_platform_channel wpgc on wdc.id = wpgc.device_channel_id and wpgc.platform_id = #{platformId}" +
            " where wdc.channel_type = 0 " +
            " <if test='query != null'> " +
            " AND (coalesce(wdc.gb_device_id, wdc.device_id) LIKE concat('%',#{query},'%') OR wpgc.custom_device_id LIKE concat('%',#{query},'%') " +
            "      OR coalesce(wdc.gb_name, wdc.name)  LIKE concat('%',#{query},'%') OR wpgc.custom_name LIKE concat('%',#{query},'%'))</if> " +
            " <if test='online == true'> AND coalesce(wpgc.status, wdc.gb_status, wdc.status) = 'ON'</if> " +
            " <if test='online == false'> AND coalesce(wpgc.status, wdc.gb_status, wdc.status) = 'OFF'</if> " +
            " <if test='hasShare == true'> AND wpgc.platform_id = #{platformId}</if> " +
            " <if test='hasShare == false'> AND wpgc.platform_id is null</if> " +
            " <if test='dataType != null'> AND wdc.data_type = #{dataType}</if> " +
            "</script>")
    List<PlatformChannel> queryForPlatformForWebList(@Param("platformId") Integer platformId, @Param("query") String query,
                                                     @Param("dataType") Integer dataType, @Param("online") Boolean online,
                                                     @Param("hasShare") Boolean hasShare);

    @Select("select\n" +
            "    wdc.id as gb_id,\n" +
            "    wdc.data_type,\n" +
            "    wdc.data_device_id,\n" +
            "    wdc.create_time,\n" +
            "    wdc.update_time,\n" +
            "    coalesce(wpgc.custom_device_id, wdc.gb_device_id, wdc.device_id) as gb_device_id,\n" +
            "    coalesce(wpgc.custom_name, wdc.gb_name, wdc.name) as gb_name,\n" +
            "    coalesce(wpgc.custom_manufacturer, wdc.gb_manufacturer, wdc.manufacturer) as gb_manufacturer,\n" +
            "    coalesce(wpgc.custom_model, wdc.gb_model, wdc.model) as gb_model,\n" +
            "    coalesce(wpgc.custom_owner, wdc.gb_owner, wdc.owner) as gb_owner,\n" +
            "    coalesce(wpgc.custom_civil_code, wdc.gb_civil_code, wdc.civil_code) as gb_civil_code,\n" +
            "    coalesce(wpgc.custom_block, wdc.gb_block, wdc.block) as gb_block,\n" +
            "    coalesce(wpgc.custom_address, wdc.gb_address, wdc.address) as gb_address,\n" +
            "    coalesce(wpgc.custom_parental, wdc.gb_parental, wdc.parental) as gb_parental,\n" +
            "    coalesce(wpgc.custom_parent_id, wdc.gb_parent_id, wdc.parent_id) as gb_parent_id,\n" +
            "    coalesce(wpgc.custom_safety_way, wdc.gb_safety_way, wdc.safety_way) as gb_safety_way,\n" +
            "    coalesce(wpgc.custom_register_way, wdc.gb_register_way, wdc.register_way) as gb_register_way,\n" +
            "    coalesce(wpgc.custom_cert_num, wdc.gb_cert_num, wdc.cert_num) as gb_cert_num,\n" +
            "    coalesce(wpgc.custom_certifiable, wdc.gb_certifiable, wdc.certifiable) as gb_certifiable,\n" +
            "    coalesce(wpgc.custom_err_code, wdc.gb_err_code, wdc.err_code) as gb_err_code,\n" +
            "    coalesce(wpgc.custom_end_time, wdc.gb_end_time, wdc.end_time) as gb_end_time,\n" +
            "    coalesce(wpgc.custom_secrecy, wdc.gb_secrecy, wdc.secrecy) as gb_secrecy,\n" +
            "    coalesce(wpgc.custom_ip_address, wdc.gb_ip_address, wdc.ip_address) as gb_ip_address,\n" +
            "    coalesce(wpgc.custom_port, wdc.gb_port, wdc.port) as gb_port,\n" +
            "    coalesce(wpgc.custom_password, wdc.gb_password, wdc.password) as gb_password,\n" +
            "    coalesce(wpgc.custom_status, wdc.gb_status, wdc.status) as gb_status,\n" +
            "    coalesce(wpgc.custom_longitude, wdc.gb_longitude, wdc.longitude) as gb_longitude,\n" +
            "    coalesce(wpgc.custom_latitude, wdc.gb_latitude, wdc.latitude) as gb_latitude,\n" +
            "    coalesce(wpgc.custom_ptz_type, wdc.gb_ptz_type, wdc.ptz_type) as gb_ptz_type,\n" +
            "    coalesce(wpgc.custom_position_type, wdc.gb_position_type, wdc.position_type) as gb_position_type,\n" +
            "    coalesce(wpgc.custom_room_type, wdc.gb_room_type, wdc.room_type) as gb_room_type,\n" +
            "    coalesce(wpgc.custom_use_type, wdc.gb_use_type, wdc.use_type) as gb_use_type,\n" +
            "    coalesce(wpgc.custom_supply_light_type, wdc.gb_supply_light_type, wdc.supply_light_type) as gb_supply_light_type,\n" +
            "    coalesce(wpgc.custom_direction_type, wdc.gb_direction_type, wdc.direction_type) as gb_direction_type,\n" +
            "    coalesce(wpgc.custom_resolution, wdc.gb_resolution, wdc.resolution) as gb_resolution,\n" +
            "    coalesce(wpgc.custom_business_group_id, wdc.gb_business_group_id, wdc.business_group_id) as gb_business_group_id,\n" +
            "    coalesce(wpgc.custom_download_speed, wdc.gb_download_speed, wdc.download_speed) as gb_download_speed,\n" +
            "    coalesce(wpgc.custom_svc_space_support_mod, wdc.gb_svc_space_support_mod, wdc.svc_space_support_mod) as gb_svc_space_support_mod,\n" +
            "    coalesce(wpgc.custom_svc_time_support_mode, wdc.gb_svc_time_support_mode, wdc.svc_time_support_mode) as gb_svc_time_support_mode\n" +
            " from wvp_device_channel wdc" +
            " left join wvp_platform_channel wpgc on wdc.id = wpgc.device_channel_id" +
            " where wdc.channel_type = 0 and wpgc.platform_id = #{platformId} and coalesce(wpgc.custom_device_id, wdc.gb_device_id, wdc.device_id) = #{channelDeviceId} order by wdc.id "

    )
    List<CommonGBChannel> queryOneWithPlatform(@Param("platformId") Integer platformId, @Param("channelDeviceId") String channelDeviceId);


    @Select("<script>" +
            " select " +
            "    wdc.id as gb_id,\n" +
            "    wdc.data_type,\n" +
            "    wdc.data_device_id,\n" +
            "    wdc.create_time,\n" +
            "    wdc.update_time,\n" +
            "    coalesce(wpgc.custom_device_id, wdc.gb_device_id, wdc.device_id) as gb_device_id,\n" +
            "    coalesce(wpgc.custom_name, wdc.gb_name, wdc.name) as gb_name,\n" +
            "    coalesce(wpgc.custom_manufacturer, wdc.gb_manufacturer, wdc.manufacturer) as gb_manufacturer,\n" +
            "    coalesce(wpgc.custom_model, wdc.gb_model, wdc.model) as gb_model,\n" +
            "    coalesce(wpgc.custom_owner, wdc.gb_owner, wdc.owner) as gb_owner,\n" +
            "    coalesce(wpgc.custom_civil_code, wdc.gb_civil_code, wdc.civil_code) as gb_civil_code,\n" +
            "    coalesce(wpgc.custom_block, wdc.gb_block, wdc.block) as gb_block,\n" +
            "    coalesce(wpgc.custom_address, wdc.gb_address, wdc.address) as gb_address,\n" +
            "    coalesce(wpgc.custom_parental, wdc.gb_parental, wdc.parental) as gb_parental,\n" +
            "    coalesce(wpgc.custom_parent_id, wdc.gb_parent_id, wdc.parent_id) as gb_parent_id,\n" +
            "    coalesce(wpgc.custom_safety_way, wdc.gb_safety_way, wdc.safety_way) as gb_safety_way,\n" +
            "    coalesce(wpgc.custom_register_way, wdc.gb_register_way, wdc.register_way) as gb_register_way,\n" +
            "    coalesce(wpgc.custom_cert_num, wdc.gb_cert_num, wdc.cert_num) as gb_cert_num,\n" +
            "    coalesce(wpgc.custom_certifiable, wdc.gb_certifiable, wdc.certifiable) as gb_certifiable,\n" +
            "    coalesce(wpgc.custom_err_code, wdc.gb_err_code, wdc.err_code) as gb_err_code,\n" +
            "    coalesce(wpgc.custom_end_time, wdc.gb_end_time, wdc.end_time) as gb_end_time,\n" +
            "    coalesce(wpgc.custom_secrecy, wdc.gb_secrecy, wdc.secrecy) as gb_secrecy,\n" +
            "    coalesce(wpgc.custom_ip_address, wdc.gb_ip_address, wdc.ip_address) as gb_ip_address,\n" +
            "    coalesce(wpgc.custom_port, wdc.gb_port, wdc.port) as gb_port,\n" +
            "    coalesce(wpgc.custom_password, wdc.gb_password, wdc.password) as gb_password,\n" +
            "    coalesce(wpgc.custom_status, wdc.gb_status, wdc.status) as gb_status,\n" +
            "    coalesce(wpgc.custom_longitude, wdc.gb_longitude, wdc.longitude) as gb_longitude,\n" +
            "    coalesce(wpgc.custom_latitude, wdc.gb_latitude, wdc.latitude) as gb_latitude,\n" +
            "    coalesce(wpgc.custom_ptz_type, wdc.gb_ptz_type, wdc.ptz_type) as gb_ptz_type,\n" +
            "    coalesce(wpgc.custom_position_type, wdc.gb_position_type, wdc.position_type) as gb_position_type,\n" +
            "    coalesce(wpgc.custom_room_type, wdc.gb_room_type, wdc.room_type) as gb_room_type,\n" +
            "    coalesce(wpgc.custom_use_type, wdc.gb_use_type, wdc.use_type) as gb_use_type,\n" +
            "    coalesce(wpgc.custom_supply_light_type, wdc.gb_supply_light_type, wdc.supply_light_type) as gb_supply_light_type,\n" +
            "    coalesce(wpgc.custom_direction_type, wdc.gb_direction_type, wdc.direction_type) as gb_direction_type,\n" +
            "    coalesce(wpgc.custom_resolution, wdc.gb_resolution, wdc.resolution) as gb_resolution,\n" +
            "    coalesce(wpgc.custom_business_group_id, wdc.gb_business_group_id, wdc.business_group_id) as gb_business_group_id,\n" +
            "    coalesce(wpgc.custom_download_speed, wdc.gb_download_speed, wdc.download_speed) as gb_download_speed,\n" +
            "    coalesce(wpgc.custom_svc_space_support_mod, wdc.gb_svc_space_support_mod, wdc.svc_space_support_mod) as gb_svc_space_support_mod,\n" +
            "    coalesce(wpgc.custom_svc_time_support_mode, wdc.gb_svc_time_support_mode, wdc.svc_time_support_mode) as gb_svc_time_support_mode\n" +
            " from wvp_device_channel wdc" +
            " left join wvp_platform_channel wpgc on wdc.id = wpgc.device_channel_id and wpgc.platform_id = #{platformId}" +
            " where wdc.channel_type = 0 and wpgc.platform_id is null" +
            "<if test='channelIds != null'> AND wdc.id in " +
            "<foreach item='item' index='index' collection='channelIds' open='(' separator=',' close=')'> #{item} </foreach> " +
            "</if>" +
            "</script>")
    List<CommonGBChannel> queryNotShare(@Param("platformId") Integer platformId, List<Integer> channelIds);

    @Select("<script>" +
            " select " +
            "    wdc.id as gb_id,\n" +
            "    wdc.data_type,\n" +
            "    wdc.data_device_id,\n" +
            "    wdc.create_time,\n" +
            "    wdc.update_time,\n" +
            "    wdc.gps_altitude,\n" +
            "    wdc.gps_speed,\n" +
            "    wdc.gps_direction,\n" +
            "    wdc.gps_time,\n" +
            "    coalesce(wpgc.custom_device_id, wdc.gb_device_id, wdc.device_id) as gb_device_id,\n" +
            "    coalesce(wpgc.custom_name, wdc.gb_name, wdc.name) as gb_name,\n" +
            "    coalesce(wpgc.custom_manufacturer, wdc.gb_manufacturer, wdc.manufacturer) as gb_manufacturer,\n" +
            "    coalesce(wpgc.custom_model, wdc.gb_model, wdc.model) as gb_model,\n" +
            "    coalesce(wpgc.custom_owner, wdc.gb_owner, wdc.owner) as gb_owner,\n" +
            "    coalesce(wpgc.custom_civil_code, wdc.gb_civil_code, wdc.civil_code) as gb_civil_code,\n" +
            "    coalesce(wpgc.custom_block, wdc.gb_block, wdc.block) as gb_block,\n" +
            "    coalesce(wpgc.custom_address, wdc.gb_address, wdc.address) as gb_address,\n" +
            "    coalesce(wpgc.custom_parental, wdc.gb_parental, wdc.parental) as gb_parental,\n" +
            "    coalesce(wpgc.custom_parent_id, wdc.gb_parent_id, wdc.parent_id) as gb_parent_id,\n" +
            "    coalesce(wpgc.custom_safety_way, wdc.gb_safety_way, wdc.safety_way) as gb_safety_way,\n" +
            "    coalesce(wpgc.custom_register_way, wdc.gb_register_way, wdc.register_way) as gb_register_way,\n" +
            "    coalesce(wpgc.custom_cert_num, wdc.gb_cert_num, wdc.cert_num) as gb_cert_num,\n" +
            "    coalesce(wpgc.custom_certifiable, wdc.gb_certifiable, wdc.certifiable) as gb_certifiable,\n" +
            "    coalesce(wpgc.custom_err_code, wdc.gb_err_code, wdc.err_code) as gb_err_code,\n" +
            "    coalesce(wpgc.custom_end_time, wdc.gb_end_time, wdc.end_time) as gb_end_time,\n" +
            "    coalesce(wpgc.custom_secrecy, wdc.gb_secrecy, wdc.secrecy) as gb_secrecy,\n" +
            "    coalesce(wpgc.custom_ip_address, wdc.gb_ip_address, wdc.ip_address) as gb_ip_address,\n" +
            "    coalesce(wpgc.custom_port, wdc.gb_port, wdc.port) as gb_port,\n" +
            "    coalesce(wpgc.custom_password, wdc.gb_password, wdc.password) as gb_password,\n" +
            "    coalesce(wpgc.custom_status, wdc.gb_status, wdc.status) as gb_status,\n" +
            "    coalesce(wpgc.custom_longitude, wdc.gb_longitude, wdc.longitude) as gb_longitude,\n" +
            "    coalesce(wpgc.custom_latitude, wdc.gb_latitude, wdc.latitude) as gb_latitude,\n" +
            "    coalesce(wpgc.custom_ptz_type, wdc.gb_ptz_type, wdc.ptz_type) as gb_ptz_type,\n" +
            "    coalesce(wpgc.custom_position_type, wdc.gb_position_type, wdc.position_type) as gb_position_type,\n" +
            "    coalesce(wpgc.custom_room_type, wdc.gb_room_type, wdc.room_type) as gb_room_type,\n" +
            "    coalesce(wpgc.custom_use_type, wdc.gb_use_type, wdc.use_type) as gb_use_type,\n" +
            "    coalesce(wpgc.custom_supply_light_type, wdc.gb_supply_light_type, wdc.supply_light_type) as gb_supply_light_type,\n" +
            "    coalesce(wpgc.custom_direction_type, wdc.gb_direction_type, wdc.direction_type) as gb_direction_type,\n" +
            "    coalesce(wpgc.custom_resolution, wdc.gb_resolution, wdc.resolution) as gb_resolution,\n" +
            "    coalesce(wpgc.custom_business_group_id, wdc.gb_business_group_id, wdc.business_group_id) as gb_business_group_id,\n" +
            "    coalesce(wpgc.custom_download_speed, wdc.gb_download_speed, wdc.download_speed) as gb_download_speed,\n" +
            "    coalesce(wpgc.custom_svc_space_support_mod, wdc.gb_svc_space_support_mod, wdc.svc_space_support_mod) as gb_svc_space_support_mod,\n" +
            "    coalesce(wpgc.custom_svc_time_support_mode, wdc.gb_svc_time_support_mode, wdc.svc_time_support_mode) as gb_svc_time_support_mode\n" +
            " from wvp_device_channel wdc" +
            " left join wvp_platform_channel wpgc on wdc.id = wpgc.device_channel_id" +
            " where wdc.channel_type = 0 and wpgc.platform_id = #{platformId}" +
            "<if test='channelIds != null'> AND wdc.id in " +
            "   <foreach item='item' index='index' collection='channelIds' open='(' separator=',' close=')'>" +
            "   #{item} " +
            "   </foreach> " +
            "</if>" +
            " order by wdc.id" +
            "</script>")
    List<CommonGBChannel> queryShare(@Param("platformId") Integer platformId, List<Integer> channelIds);

    @Delete("<script> " +
            "DELETE from wvp_platform_channel WHERE platform_id=#{platformId} " +
            "<if test='channelList != null'> AND device_channel_id in " +
            "   <foreach item='item' index='index' collection='channelList' open='(' separator=',' close=')'>" +
            "   #{item.gbId} " +
            "   </foreach> " +
            "</if>" +
            "</script>")
    int removeChannelsWithPlatform(@Param("platformId") Integer platformId, List<CommonGBChannel> channelList);

    @Delete("<script> " +
            "DELETE from wvp_platform_channel WHERE " +
            "<if test='channelList != null'> AND device_channel_id in " +
            "   <foreach item='item' index='index' collection='channelList' open='(' separator=',' close=')'>" +
            "   #{item.gbId} " +
            "   </foreach> " +
            "</if>" +
            "</script>")
    int removeChannels(List<CommonGBChannel> channelList);

    @Insert("<script> "+
            "INSERT INTO wvp_platform_group (platform_id, group_id) VALUES " +
            "<foreach collection='groupListNotShare'  item='item' separator=','>" +
            " (#{platformId}, #{item.id} )" +
            "</foreach>" +
            "</script>")
    int addPlatformGroup(Collection<Group> groupListNotShare, @Param("platformId") Integer platformId);

    @Insert("<script> "+
            "INSERT INTO wvp_platform_region (platform_id, region_id) VALUES " +
            "<foreach collection='regionListNotShare'  item='item' separator=','>" +
            " (#{platformId}, #{item.id} )" +
            "</foreach>" +
            "</script>")
    int addPlatformRegion(List<Region> regionListNotShare, @Param("platformId") Integer platformId);

    @Delete("<script> "+
            "DELETE from wvp_platform_group WHERE platform_id=#{platformId} AND group_id in" +
            "<foreach collection='groupList'  item='item'  open='(' separator=',' close=')' > #{item.id}</foreach>" +
            "</script>")
    int removePlatformGroup(List<Group> groupList, @Param("platformId") Integer platformId);

    @Delete("<script> "+
            "DELETE from wvp_platform_group WHERE platform_id=#{platformId} AND group_id  =#{id}" +
            "</script>")
    void removePlatformGroupById(@Param("id") int id, @Param("platformId") Integer platformId);

    @Delete("<script> "+
            "DELETE from wvp_platform_region WHERE platform_id=#{platformId} AND region_id  =#{id}" +
            "</script>")
    void removePlatformRegionById(@Param("id") int id, @Param("platformId") Integer platformId);

    @Select(" <script>" +
            " SELECT wcg.* " +
            " from wvp_common_group wcg" +
            " left join wvp_platform_group wpg on wpg.group_id = wcg.id and wpg.platform_id = #{platformId}" +
            " where wpg.platform_id IS NOT NULL and wcg.parent_id = #{parentId} " +
            " </script>")
    Set<Group> queryShareChildrenGroup(@Param("parentId") Integer parentId, @Param("platformId") Integer platformId);

    @Select(" <script>" +
            " SELECT wcr.* " +
            " from wvp_common_region wcr" +
            " left join wvp_platform_region wpr on wpr.region_id = wcr.id and wpr.platform_id = #{platformId}" +
            " where wpr.platform_id IS NOT NULL and wcr.parent_device_id = #{parentId} " +
            " </script>")
    Set<Region> queryShareChildrenRegion(@Param("parentId") String parentId, @Param("platformId") Integer platformId);

    @Select(" <script>" +
            " SELECT wcg.* " +
            " from wvp_common_group wcg" +
            " left join wvp_platform_group wpg on wpg.group_id = wcg.id and wpg.platform_id = #{platformId}" +
            " where wpg.platform_id is not null and wcg.id in " +
            "<foreach collection='groupSet'  item='item'  open='(' separator=',' close=')' > #{item.parentId}</foreach>" +
            " </script>")
    Set<Group> queryShareParentGroupByGroupSet(Set<Group> groupSet, @Param("platformId") Integer platformId);

    @Select(" <script>" +
            " SELECT wcr.* " +
            " from wvp_common_region wcr" +
            " left join wvp_platform_region wpr on wpr.region_id = wcr.id and wpr.platform_id = #{platformId}" +
            " where wpr.platform_id is not null and wcr.id in " +
            "<foreach collection='regionSet'  item='item'  open='(' separator=',' close=')' > #{item.parentId}</foreach>" +
            " </script>")
    Set<Region> queryShareParentRegionByRegionSet(Set<Region> regionSet, @Param("platformId") Integer platformId);

    @Select("<script> " +
            " SELECT " +
            " pp.* " +
            " FROM " +
            " wvp_platform pp " +
            " left join wvp_platform_channel pgc on " +
            " pp.id = pgc.platform_id " +
            " left join wvp_device_channel dc on " +
            " dc.id = pgc.device_channel_id " +
            " WHERE " +
            "  pgc.device_channel_id IN" +
            "<foreach collection='ids' item='item'  open='(' separator=',' close=')' > #{item}</foreach>" +
            "</script> ")
    List<Platform> queryPlatFormListByChannelList(Collection<Integer> ids);

    @Select("<script> " +
            " SELECT " +
            " pp.* " +
            " FROM " +
            " wvp_platform pp " +
            " left join wvp_platform_channel pgc on " +
            " pp.id = pgc.platform_id " +
            " left join wvp_device_channel dc on " +
            " dc.id = pgc.device_channel_id " +
            " WHERE " +
            "  pgc.device_channel_id = #{channelId}" +
            "</script> ")
    List<Platform> queryPlatFormListByChannelId(@Param("channelId") int channelId);

    @Delete("<script> "+
            "DELETE from wvp_platform_channel WHERE platform_id=#{platformId}" +
            "</script>")
    void removeChannelsByPlatformId(@Param("platformId") Integer platformId);

    @Delete("<script> "+
            "DELETE from wvp_platform_group WHERE platform_id=#{platformId}" +
            "</script>")
    void removePlatformGroupsByPlatformId(@Param("platformId") Integer platformId);

    @Delete("<script> "+
            "DELETE from wvp_platform_region WHERE platform_id=#{platformId}" +
            "</script>")
    void removePlatformRegionByPlatformId(@Param("platformId") Integer platformId);

    @Update(value = {" <script>" +
            " UPDATE wvp_platform_channel " +
            " SET custom_device_id =#{customDeviceId}" +
            " ,custom_name =#{customName}" +
            " ,custom_manufacturer =#{customManufacturer}" +
            " ,custom_model =#{customModel}" +
            " ,custom_owner =#{customOwner}" +
            " ,custom_civil_code =#{customCivilCode}" +
            " ,custom_block =#{customBlock}" +
            " ,custom_address =#{customAddress}" +
            " ,custom_parental =#{customParental}" +
            " ,custom_parent_id =#{customParentId}" +
            " ,custom_safety_way =#{customSafetyWay}" +
            " ,custom_register_way =#{customRegisterWay}" +
            " ,custom_cert_num =#{customCertNum}" +
            " ,custom_certifiable =#{customCertifiable}" +
            " ,custom_err_code =#{customErrCode}" +
            " ,custom_end_time =#{customEndTime}" +
            " ,custom_secrecy =#{customSecrecy}" +
            " ,custom_ip_address =#{customIpAddress}" +
            " ,custom_port =#{customPort}" +
            " ,custom_password =#{customPassword}" +
            " ,custom_status =#{customStatus}" +
            " ,custom_longitude =#{customLongitude}" +
            " ,custom_latitude =#{customLatitude}" +
            " ,custom_ptz_type =#{customPtzType}" +
            " ,custom_position_type =#{customPositionType}" +
            " ,custom_room_type =#{customRoomType}" +
            " ,custom_use_type =#{customUseType}" +
            " ,custom_supply_light_type =#{customSupplyLightType}" +
            " ,custom_direction_type =#{customDirectionType}" +
            " ,custom_resolution =#{customResolution}" +
            " ,custom_business_group_id =#{customBusinessGroupId}" +
            " ,custom_download_speed =#{customDownloadSpeed}" +
            " ,custom_svc_space_support_mod =#{customSvcSpaceSupportMod}" +
            " ,custom_svc_time_support_mode = #{customSvcTimeSupportMode}" +
            " WHERE id = #{id}"+
            " </script>"})
    void updateCustomChannel(PlatformChannel channel);


    @Select("<script>" +
            " select " +
            "    wdc.id as gb_id,\n" +
            "    wdc.data_type,\n" +
            "    wdc.data_device_id,\n" +
            "    wdc.create_time,\n" +
            "    wdc.update_time,\n" +
            "    coalesce(wpgc.custom_device_id, wdc.gb_device_id, wdc.device_id) as gb_device_id,\n" +
            "    coalesce(wpgc.custom_name, wdc.gb_name, wdc.name) as gb_name,\n" +
            "    coalesce(wpgc.custom_manufacturer, wdc.gb_manufacturer, wdc.manufacturer) as gb_manufacturer,\n" +
            "    coalesce(wpgc.custom_model, wdc.gb_model, wdc.model) as gb_model,\n" +
            "    coalesce(wpgc.custom_owner, wdc.gb_owner, wdc.owner) as gb_owner,\n" +
            "    coalesce(wpgc.custom_civil_code, wdc.gb_civil_code, wdc.civil_code) as gb_civil_code,\n" +
            "    coalesce(wpgc.custom_block, wdc.gb_block, wdc.block) as gb_block,\n" +
            "    coalesce(wpgc.custom_address, wdc.gb_address, wdc.address) as gb_address,\n" +
            "    coalesce(wpgc.custom_parental, wdc.gb_parental, wdc.parental) as gb_parental,\n" +
            "    coalesce(wpgc.custom_parent_id, wdc.gb_parent_id, wdc.parent_id) as gb_parent_id,\n" +
            "    coalesce(wpgc.custom_safety_way, wdc.gb_safety_way, wdc.safety_way) as gb_safety_way,\n" +
            "    coalesce(wpgc.custom_register_way, wdc.gb_register_way, wdc.register_way) as gb_register_way,\n" +
            "    coalesce(wpgc.custom_cert_num, wdc.gb_cert_num, wdc.cert_num) as gb_cert_num,\n" +
            "    coalesce(wpgc.custom_certifiable, wdc.gb_certifiable, wdc.certifiable) as gb_certifiable,\n" +
            "    coalesce(wpgc.custom_err_code, wdc.gb_err_code, wdc.err_code) as gb_err_code,\n" +
            "    coalesce(wpgc.custom_end_time, wdc.gb_end_time, wdc.end_time) as gb_end_time,\n" +
            "    coalesce(wpgc.custom_secrecy, wdc.gb_secrecy, wdc.secrecy) as gb_secrecy,\n" +
            "    coalesce(wpgc.custom_ip_address, wdc.gb_ip_address, wdc.ip_address) as gb_ip_address,\n" +
            "    coalesce(wpgc.custom_port, wdc.gb_port, wdc.port) as gb_port,\n" +
            "    coalesce(wpgc.custom_password, wdc.gb_password, wdc.password) as gb_password,\n" +
            "    coalesce(wpgc.custom_status, wdc.gb_status, wdc.status) as gb_status,\n" +
            "    coalesce(wpgc.custom_longitude, wdc.gb_longitude, wdc.longitude) as gb_longitude,\n" +
            "    coalesce(wpgc.custom_latitude, wdc.gb_latitude, wdc.latitude) as gb_latitude,\n" +
            "    coalesce(wpgc.custom_ptz_type, wdc.gb_ptz_type, wdc.ptz_type) as gb_ptz_type,\n" +
            "    coalesce(wpgc.custom_position_type, wdc.gb_position_type, wdc.position_type) as gb_position_type,\n" +
            "    coalesce(wpgc.custom_room_type, wdc.gb_room_type, wdc.room_type) as gb_room_type,\n" +
            "    coalesce(wpgc.custom_use_type, wdc.gb_use_type, wdc.use_type) as gb_use_type,\n" +
            "    coalesce(wpgc.custom_supply_light_type, wdc.gb_supply_light_type, wdc.supply_light_type) as gb_supply_light_type,\n" +
            "    coalesce(wpgc.custom_direction_type, wdc.gb_direction_type, wdc.direction_type) as gb_direction_type,\n" +
            "    coalesce(wpgc.custom_resolution, wdc.gb_resolution, wdc.resolution) as gb_resolution,\n" +
            "    coalesce(wpgc.custom_business_group_id, wdc.gb_business_group_id, wdc.business_group_id) as gb_business_group_id,\n" +
            "    coalesce(wpgc.custom_download_speed, wdc.gb_download_speed, wdc.download_speed) as gb_download_speed,\n" +
            "    coalesce(wpgc.custom_svc_space_support_mod, wdc.gb_svc_space_support_mod, wdc.svc_space_support_mod) as gb_svc_space_support_mod,\n" +
            "    coalesce(wpgc.custom_svc_time_support_mode, wdc.gb_svc_time_support_mode, wdc.svc_time_support_mode) as gb_svc_time_support_mode\n" +
            " from wvp_device_channel wdc" +
            " left join wvp_platform_channel wpgc on wdc.id = wpgc.device_channel_id" +
            " where wdc.channel_type = 0 and wpgc.platform_id = #{platformId} and wdc.id = #{gbId}" +
            "</script>")
    CommonGBChannel queryShareChannel(@Param("platformId") int platformId, @Param("gbId") int gbId);


    @Select(" <script>" +
            " SELECT wcg.* " +
            " from wvp_common_group wcg" +
            " left join wvp_platform_group wpg on wpg.group_id = wcg.id " +
            " where wpg.platform_id = #{platformId}" +
            " order by wcg.id DESC" +
            " </script>")
    Set<Group> queryShareGroup(@Param("platformId") Integer platformId);

    @Select(" <script>" +
            " SELECT wcr.* " +
            " from wvp_common_region wcr" +
            " left join wvp_platform_region wpr on wpr.region_id = wcr.id " +
            " where wpr.platform_id = #{platformId}" +
            " order by wcr.id DESC" +
            " </script>")
    Set<Region> queryShareRegion(Integer id);
}
