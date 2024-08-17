package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.controller.bean.ChannelReduce;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface PlatformChannelMapper {

    /**
     * 查询列表里已经关联的
     */
    @Select("<script> "+
            "SELECT device_channel_id from wvp_platform_gb_channel WHERE platform_id=#{platformId} AND device_channel_id in" +
            "<foreach collection='channelReduces' open='(' item='item' separator=',' close=')'> #{item.id}</foreach>" +
            "</script>")
    List<Integer> findChannelRelatedPlatform(@Param("platformId") String platformId, @Param("channelReduces") List<ChannelReduce> channelReduces);

    @Insert("<script> "+
            "INSERT INTO wvp_platform_gb_channel (platform_id, device_channel_id) VALUES" +
            "<foreach collection='channelList'  item='item' separator=','>" +
            " (#{platformId}, #{item.gbId} )" +
            "</foreach>" +
            "</script>")
    int addChannels(@Param("platformId") Integer platformId, @Param("channelList") List<CommonGBChannel> channelList);

    @Delete("<script> "+
            "DELETE from wvp_platform_gb_channel WHERE platform_id=#{platformId} AND device_channel_id in" +
            "<foreach collection='channelReducesToDel'  item='item'  open='(' separator=',' close=')' > #{item.id}</foreach>" +
            "</script>")
    int delChannelForGB(@Param("platformId") String platformId, @Param("channelReducesToDel") List<ChannelReduce> channelReducesToDel);

    @Delete("<script> "+
            "DELETE from wvp_platform_gb_channel WHERE device_channel_id in " +
            "( select  temp.device_channel_id from " +
            "(select pgc.device_channel_id from wvp_platform_gb_channel pgc " +
            "left join wvp_device_channel dc on dc.id = pgc.device_channel_id where dc.device_id  =#{deviceId} " +
            ") temp)" +
            "</script>")
    int delChannelForDeviceId(String deviceId);

    @Delete("<script> "+
            "DELETE from wvp_platform_gb_channel WHERE platform_id=#{platformId}"  +
            "</script>")
    int cleanChannelForGB(String platformId);

    @Select("SELECT dc.* from wvp_platform_gb_channel pgc left join wvp_device_channel dc on dc.id = pgc.device_channel_id WHERE dc.channel_id=#{channelId} and pgc.platform_id=#{platformId}")
    List<DeviceChannel> queryChannelInParentPlatform(@Param("platformId") String platformId, @Param("channelId") String channelId);

    @Select("<script> "+
            "SELECT dc.* from wvp_platform_gb_channel pgc left join wvp_device_channel dc on dc.id = pgc.device_channel_id WHERE pgc.platform_id=#{platformId} " +
            " <if test='catalogId != null' > and pgc.catalog_id=#{catalogId}</if>" +
            "</script>")
    List<CommonGBChannel> queryAllChannelInCatalog(@Param("platformId") String platformId, @Param("catalogId") String catalogId);

    @Select(" select dc.channel_id as id, dc.name as name, pgc.platform_id as platform_id, pgc.catalog_id as parent_id, 0 as children_count, 1 as type " +
            " from wvp_device_channel dc left join wvp_platform_gb_channel pgc on dc.id = pgc.device_channel_id " +
            " where pgc.platform_id=#{platformId} and pgc.catalog_id=#{catalogId}")
    List<PlatformCatalog> queryChannelInParentPlatformAndCatalog(@Param("platformId") String platformId, @Param("catalogId") String catalogId);

    @Select("select d.*\n" +
            "from wvp_platform_gb_channel pgc\n" +
            "         left join wvp_device_channel dc on dc.id = pgc.device_channel_id\n" +
            "         left join wvp_device d on dc.device_id = d.device_id\n" +
            "where dc.channel_id = #{channelId} and pgc.platform_id=#{platformId}")
    List<Device> queryVideoDeviceByPlatformIdAndChannelId(@Param("platformId") String platformId, @Param("channelId") String channelId);

    @Delete("<script> "+
            "DELETE from wvp_platform_gb_channel WHERE platform_id=#{platformId} and catalog_id=#{id}"  +
            "</script>")
    int delByCatalogId(@Param("platformId") String platformId, @Param("id") String id);

    @Delete("<script> "+
           "DELETE from wvp_platform_gb_channel  WHERE catalog_id=#{parentId} AND platform_id=#{platformId} AND channel_id=#{id}"  +
           "</script>")
    int delByCatalogIdAndChannelIdAndPlatformId(PlatformCatalog platformCatalog);

    @Select("<script> " +
            "SELECT " +
            "pp.* " +
            "FROM " +
            "wvp_platform pp " +
            "left join wvp_platform_gb_channel pgc on " +
            "pp.server_gb_id = pgc.platform_id " +
            "left join wvp_device_channel dc on " +
            "dc.id = pgc.device_channel_id " +
            "WHERE " +
            "dc.device_id = #{channelId} and pp.status = true " +
            "AND pp.server_gb_id IN" +
            "<foreach collection='platforms' item='item'  open='(' separator=',' close=')' > #{item}</foreach>" +
            "</script> ")
    List<Platform> queryPlatFormListForGBWithGBId(@Param("channelId") String channelId, @Param("platforms") List<String> platforms);

    @Delete("<script> " +
           "DELETE from wvp_platform_gb_channel WHERE platform_id=#{serverGBId}"  +
           "</script>")
    void delByPlatformId(String serverGBId);

    @Delete("<script> " +
            "DELETE from wvp_platform_gb_channel WHERE platform_id=#{platformId} " +
            " <if test=\"catalogId != null\" >  and catalog_id=#{catalogId}</if>" +
            "</script>")
    int delChannelForGBByCatalogId(@Param("platformId") String platformId, @Param("catalogId") String catalogId);

    @Select("select dc.channel_id dc.device_id,dc.name,d.manufacturer,d.model,d.firmware\n" +
            "from wvp_platform_gb_channel pgc\n" +
            "         left join wvp_device_channel dc on dc.id = pgc.device_channel_id\n" +
            "         left join wvp_device d on dc.device_id = d.device_id\n" +
            "where dc.channel_id = #{channelId} and pgc.platform_id=#{platformId}")
    List<Device> queryDeviceInfoByPlatformIdAndChannelId(@Param("platformId") String platformId, @Param("channelId") String channelId);

    @Select("SELECT pgc.platform_id from wvp_platform_gb_channel pgc left join wvp_device_channel dc on dc.id = pgc.device_channel_id WHERE dc.device_id=#{channelId}")
    List<Integer> queryParentPlatformByChannelId(@Param("channelId") String channelId);



    @Select("<script>" +
            " select " +
            "    wdc.id as gb_id,\n" +
            "    wdc.device_db_id as gb_device_db_id,\n" +
            "    wdc.stream_push_id,\n" +
            "    wdc.stream_proxy_id,\n" +
            "    wdc.create_time,\n" +
            "    wdc.update_time,\n" +
            "    coalesce(wpgc.device_id, wdc.gb_device_id, wdc.device_id) as gb_device_id,\n" +
            "    coalesce(wpgc.name, wdc.gb_name, wdc.name) as gb_name,\n" +
            "    coalesce(wpgc.manufacturer, wdc.gb_manufacturer, wdc.manufacturer) as gb_manufacturer,\n" +
            "    coalesce(wpgc.model, wdc.gb_model, wdc.model) as gb_model,\n" +
            "    coalesce(wpgc.owner, wdc.gb_owner, wdc.owner) as gb_owner,\n" +
            "    coalesce(wpgc.civil_code, wdc.gb_civil_code, wdc.civil_code),\n" +
            "    coalesce(wpgc.block, wdc.gb_block, wdc.block) as gb_block,\n" +
            "    coalesce(wpgc.address, wdc.gb_address, wdc.address) as gb_address,\n" +
            "    coalesce(wpgc.parental, wdc.gb_parental, wdc.parental) as gb_parental,\n" +
            "    coalesce(wpgc.parent_id, wdc.gb_parent_id, wdc.parent_id) as gb_parent_id,\n" +
            "    coalesce(wpgc.safety_way, wdc.gb_safety_way, wdc.safety_way) as gb_safety_way,\n" +
            "    coalesce(wpgc.register_way, wdc.gb_register_way, wdc.register_way) as gb_register_way,\n" +
            "    coalesce(wpgc.cert_num, wdc.gb_cert_num, wdc.cert_num) as gb_cert_num,\n" +
            "    coalesce(wpgc.certifiable, wdc.gb_certifiable, wdc.certifiable) as gb_certifiable,\n" +
            "    coalesce(wpgc.err_code, wdc.gb_err_code, wdc.err_code) as gb_err_code,\n" +
            "    coalesce(wpgc.end_time, wdc.gb_end_time, wdc.end_time) as gb_end_time,\n" +
            "    coalesce(wpgc.secrecy, wdc.gb_secrecy, wdc.secrecy) as gb_secrecy,\n" +
            "    coalesce(wpgc.ip_address, wdc.gb_ip_address, wdc.ip_address) as gb_ip_address,\n" +
            "    coalesce(wpgc.port, wdc.gb_port, wdc.port) as gb_port,\n" +
            "    coalesce(wpgc.password, wdc.gb_password, wdc.password) as gb_password,\n" +
            "    coalesce(wpgc.status, wdc.gb_status, wdc.status) as gb_status,\n" +
            "    coalesce(wpgc.longitude, wdc.gb_longitude, wdc.longitude) as gb_longitude,\n" +
            "    coalesce(wpgc.latitude, wdc.gb_latitude, wdc.latitude) as gb_latitude,\n" +
            "    coalesce(wpgc.ptz_type, wdc.gb_ptz_type, wdc.ptz_type) as gb_ptz_type,\n" +
            "    coalesce(wpgc.position_type, wdc.gb_position_type, wdc.position_type) as gb_position_type,\n" +
            "    coalesce(wpgc.room_type, wdc.gb_room_type, wdc.room_type) as gb_room_type,\n" +
            "    coalesce(wpgc.use_type, wdc.gb_use_type, wdc.use_type) as gb_use_type,\n" +
            "    coalesce(wpgc.supply_light_type, wdc.gb_supply_light_type, wdc.supply_light_type) as gb_supply_light_type,\n" +
            "    coalesce(wpgc.direction_type, wdc.gb_direction_type, wdc.direction_type) as gb_direction_type,\n" +
            "    coalesce(wpgc.resolution, wdc.gb_resolution, wdc.resolution) as gb_resolution,\n" +
            "    coalesce(wpgc.business_group_id, wdc.gb_business_group_id, wdc.business_group_id) as gb_business_group_id,\n" +
            "    coalesce(wpgc.download_speed, wdc.gb_download_speed, wdc.download_speed) as gb_download_speed,\n" +
            "    coalesce(wpgc.svc_space_support_mod, wdc.gb_svc_space_support_mod, wdc.svc_space_support_mod) as gb_svc_space_support_mod,\n" +
            "    coalesce(wpgc.svc_time_support_mode, wdc.gb_svc_time_support_mode, wdc.svc_time_support_mode) as gb_svc_time_support_mode, \n" +
            "    wpgc.platform_id " +
            " from wvp_device_channel wdc" +
            " left join wvp_platform_gb_channel wpgc on wdc.id = wpgc.device_channel_id and wpgc.platform_id = #{platformId}" +
            " where  1=1" +
            " <if test='query != null'> AND (coalesce(wpgc.device_id, wdc.gb_device_id, wdc.device_id) LIKE concat('%',#{query},'%') " +
            " OR coalesce(wpgc.name, wdc.gb_name, wdc.name)  LIKE concat('%',#{query},'%'))</if> " +
            " <if test='online == true'> AND coalesce(wpgc.status, wdc.gb_status, wdc.status) = 'ON'</if> " +
            " <if test='online == false'> AND coalesce(wpgc.status, wdc.gb_status, wdc.status) = 'OFF'</if> " +
            " <if test='hasShare == true'> AND wpgc.platform_id = #{platformId}</if> " +
            " <if test='hasShare == false'> AND wpgc.platform_id is null</if> " +

            "</script>")
    List<PlatformChannel> queryForPlatformSearch(@Param("platformId") Integer platformId, @Param("query") String query,
                                                 @Param("online") Boolean online, @Param("hasShare") Boolean hasShare);

    @Select("select\n" +
            "    wdc.id as gb_id,\n" +
            "    wdc.device_db_id as gb_device_db_id,\n" +
            "    wdc.stream_push_id,\n" +
            "    wdc.stream_proxy_id,\n" +
            "    wdc.create_time,\n" +
            "    wdc.update_time,\n" +
            "    coalesce(wpgc.device_id, wdc.gb_device_id, wdc.device_id) as gb_device_id,\n" +
            "    coalesce(wpgc.name, wdc.gb_name, wdc.name) as gb_name,\n" +
            "    coalesce(wpgc.manufacturer, wdc.gb_manufacturer, wdc.manufacturer) as gb_manufacturer,\n" +
            "    coalesce(wpgc.model, wdc.gb_model, wdc.model) as gb_model,\n" +
            "    coalesce(wpgc.owner, wdc.gb_owner, wdc.owner) as gb_owner,\n" +
            "    coalesce(wpgc.civil_code, wdc.gb_civil_code, wdc.civil_code),\n" +
            "    coalesce(wpgc.block, wdc.gb_block, wdc.block) as gb_block,\n" +
            "    coalesce(wpgc.address, wdc.gb_address, wdc.address) as gb_address,\n" +
            "    coalesce(wpgc.parental, wdc.gb_parental, wdc.parental) as gb_parental,\n" +
            "    coalesce(wpgc.parent_id, wdc.gb_parent_id, wdc.parent_id) as gb_parent_id,\n" +
            "    coalesce(wpgc.safety_way, wdc.gb_safety_way, wdc.safety_way) as gb_safety_way,\n" +
            "    coalesce(wpgc.register_way, wdc.gb_register_way, wdc.register_way) as gb_register_way,\n" +
            "    coalesce(wpgc.cert_num, wdc.gb_cert_num, wdc.cert_num) as gb_cert_num,\n" +
            "    coalesce(wpgc.certifiable, wdc.gb_certifiable, wdc.certifiable) as gb_certifiable,\n" +
            "    coalesce(wpgc.err_code, wdc.gb_err_code, wdc.err_code) as gb_err_code,\n" +
            "    coalesce(wpgc.end_time, wdc.gb_end_time, wdc.end_time) as gb_end_time,\n" +
            "    coalesce(wpgc.secrecy, wdc.gb_secrecy, wdc.secrecy) as gb_secrecy,\n" +
            "    coalesce(wpgc.ip_address, wdc.gb_ip_address, wdc.ip_address) as gb_ip_address,\n" +
            "    coalesce(wpgc.port, wdc.gb_port, wdc.port) as gb_port,\n" +
            "    coalesce(wpgc.password, wdc.gb_password, wdc.password) as gb_password,\n" +
            "    coalesce(wpgc.status, wdc.gb_status, wdc.status) as gb_status,\n" +
            "    coalesce(wpgc.longitude, wdc.gb_longitude, wdc.longitude) as gb_longitude,\n" +
            "    coalesce(wpgc.latitude, wdc.gb_latitude, wdc.latitude) as gb_latitude,\n" +
            "    coalesce(wpgc.ptz_type, wdc.gb_ptz_type, wdc.ptz_type) as gb_ptz_type,\n" +
            "    coalesce(wpgc.position_type, wdc.gb_position_type, wdc.position_type) as gb_position_type,\n" +
            "    coalesce(wpgc.room_type, wdc.gb_room_type, wdc.room_type) as gb_room_type,\n" +
            "    coalesce(wpgc.use_type, wdc.gb_use_type, wdc.use_type) as gb_use_type,\n" +
            "    coalesce(wpgc.supply_light_type, wdc.gb_supply_light_type, wdc.supply_light_type) as gb_supply_light_type,\n" +
            "    coalesce(wpgc.direction_type, wdc.gb_direction_type, wdc.direction_type) as gb_direction_type,\n" +
            "    coalesce(wpgc.resolution, wdc.gb_resolution, wdc.resolution) as gb_resolution,\n" +
            "    coalesce(wpgc.business_group_id, wdc.gb_business_group_id, wdc.business_group_id) as gb_business_group_id,\n" +
            "    coalesce(wpgc.download_speed, wdc.gb_download_speed, wdc.download_speed) as gb_download_speed,\n" +
            "    coalesce(wpgc.svc_space_support_mod, wdc.gb_svc_space_support_mod, wdc.svc_space_support_mod) as gb_svc_space_support_mod,\n" +
            "    coalesce(wpgc.svc_time_support_mode, wdc.gb_svc_time_support_mode, wdc.svc_time_support_mode) as gb_svc_time_support_mode\n" +
            " from wvp_device_channel wdc" +
            " left join wvp_platform_gb_channel wpgc on wdc.id = wpgc.device_channel_id" +
            " where wpgc.platform_id = #{platformId} and coalesce(wpgc.device_id, wdc.gb_device_id, wdc.device_id) = #{channelDeviceId}"

    )
    CommonGBChannel queryOneWithPlatform(@Param("platformId") Integer platformId, @Param("channelDeviceId") String channelDeviceId);


    @Select("<script>" +
            " select " +
            "    wdc.id as gb_id,\n" +
            "    wdc.device_db_id as gb_device_db_id,\n" +
            "    wdc.stream_push_id,\n" +
            "    wdc.stream_proxy_id,\n" +
            "    wdc.create_time,\n" +
            "    wdc.update_time,\n" +
            "    coalesce(wpgc.device_id, wdc.gb_device_id, wdc.device_id) as gb_device_id,\n" +
            "    coalesce(wpgc.name, wdc.gb_name, wdc.name) as gb_name,\n" +
            "    coalesce(wpgc.manufacturer, wdc.gb_manufacturer, wdc.manufacturer) as gb_manufacturer,\n" +
            "    coalesce(wpgc.model, wdc.gb_model, wdc.model) as gb_model,\n" +
            "    coalesce(wpgc.owner, wdc.gb_owner, wdc.owner) as gb_owner,\n" +
            "    coalesce(wpgc.civil_code, wdc.gb_civil_code, wdc.civil_code),\n" +
            "    coalesce(wpgc.block, wdc.gb_block, wdc.block) as gb_block,\n" +
            "    coalesce(wpgc.address, wdc.gb_address, wdc.address) as gb_address,\n" +
            "    coalesce(wpgc.parental, wdc.gb_parental, wdc.parental) as gb_parental,\n" +
            "    coalesce(wpgc.parent_id, wdc.gb_parent_id, wdc.parent_id) as gb_parent_id,\n" +
            "    coalesce(wpgc.safety_way, wdc.gb_safety_way, wdc.safety_way) as gb_safety_way,\n" +
            "    coalesce(wpgc.register_way, wdc.gb_register_way, wdc.register_way) as gb_register_way,\n" +
            "    coalesce(wpgc.cert_num, wdc.gb_cert_num, wdc.cert_num) as gb_cert_num,\n" +
            "    coalesce(wpgc.certifiable, wdc.gb_certifiable, wdc.certifiable) as gb_certifiable,\n" +
            "    coalesce(wpgc.err_code, wdc.gb_err_code, wdc.err_code) as gb_err_code,\n" +
            "    coalesce(wpgc.end_time, wdc.gb_end_time, wdc.end_time) as gb_end_time,\n" +
            "    coalesce(wpgc.secrecy, wdc.gb_secrecy, wdc.secrecy) as gb_secrecy,\n" +
            "    coalesce(wpgc.ip_address, wdc.gb_ip_address, wdc.ip_address) as gb_ip_address,\n" +
            "    coalesce(wpgc.port, wdc.gb_port, wdc.port) as gb_port,\n" +
            "    coalesce(wpgc.password, wdc.gb_password, wdc.password) as gb_password,\n" +
            "    coalesce(wpgc.status, wdc.gb_status, wdc.status) as gb_status,\n" +
            "    coalesce(wpgc.longitude, wdc.gb_longitude, wdc.longitude) as gb_longitude,\n" +
            "    coalesce(wpgc.latitude, wdc.gb_latitude, wdc.latitude) as gb_latitude,\n" +
            "    coalesce(wpgc.ptz_type, wdc.gb_ptz_type, wdc.ptz_type) as gb_ptz_type,\n" +
            "    coalesce(wpgc.position_type, wdc.gb_position_type, wdc.position_type) as gb_position_type,\n" +
            "    coalesce(wpgc.room_type, wdc.gb_room_type, wdc.room_type) as gb_room_type,\n" +
            "    coalesce(wpgc.use_type, wdc.gb_use_type, wdc.use_type) as gb_use_type,\n" +
            "    coalesce(wpgc.supply_light_type, wdc.gb_supply_light_type, wdc.supply_light_type) as gb_supply_light_type,\n" +
            "    coalesce(wpgc.direction_type, wdc.gb_direction_type, wdc.direction_type) as gb_direction_type,\n" +
            "    coalesce(wpgc.resolution, wdc.gb_resolution, wdc.resolution) as gb_resolution,\n" +
            "    coalesce(wpgc.business_group_id, wdc.gb_business_group_id, wdc.business_group_id) as gb_business_group_id,\n" +
            "    coalesce(wpgc.download_speed, wdc.gb_download_speed, wdc.download_speed) as gb_download_speed,\n" +
            "    coalesce(wpgc.svc_space_support_mod, wdc.gb_svc_space_support_mod, wdc.svc_space_support_mod) as gb_svc_space_support_mod,\n" +
            "    coalesce(wpgc.svc_time_support_mode, wdc.gb_svc_time_support_mode, wdc.svc_time_support_mode) as gb_svc_time_support_mode\n" +
            " from wvp_device_channel wdc" +
            " left join wvp_platform_gb_channel wpgc on wdc.id = wpgc.device_channel_id and wpgc.platform_id = #{platformId}" +
            " where  wpgc.platform_id is null" +
            "<if test='channelIds != null'> AND wdc.id in <foreach item='item' index='index' collection='channelIds' open='(' separator=',' close=')'>" +
            "#{item} " +
            "</foreach> </if>" +

            "</script>")
    List<CommonGBChannel> queryNotShare(@Param("platformId") Integer platformId, List<Integer> channelIds);

    @Select("<script>" +
            " select " +
            "    wdc.id as gb_id,\n" +
            "    wdc.device_db_id as gb_device_db_id,\n" +
            "    wdc.stream_push_id,\n" +
            "    wdc.stream_proxy_id,\n" +
            "    wdc.create_time,\n" +
            "    wdc.update_time,\n" +
            "    coalesce(wpgc.device_id, wdc.gb_device_id, wdc.device_id) as gb_device_id,\n" +
            "    coalesce(wpgc.name, wdc.gb_name, wdc.name) as gb_name,\n" +
            "    coalesce(wpgc.manufacturer, wdc.gb_manufacturer, wdc.manufacturer) as gb_manufacturer,\n" +
            "    coalesce(wpgc.model, wdc.gb_model, wdc.model) as gb_model,\n" +
            "    coalesce(wpgc.owner, wdc.gb_owner, wdc.owner) as gb_owner,\n" +
            "    coalesce(wpgc.civil_code, wdc.gb_civil_code, wdc.civil_code),\n" +
            "    coalesce(wpgc.block, wdc.gb_block, wdc.block) as gb_block,\n" +
            "    coalesce(wpgc.address, wdc.gb_address, wdc.address) as gb_address,\n" +
            "    coalesce(wpgc.parental, wdc.gb_parental, wdc.parental) as gb_parental,\n" +
            "    coalesce(wpgc.parent_id, wdc.gb_parent_id, wdc.parent_id) as gb_parent_id,\n" +
            "    coalesce(wpgc.safety_way, wdc.gb_safety_way, wdc.safety_way) as gb_safety_way,\n" +
            "    coalesce(wpgc.register_way, wdc.gb_register_way, wdc.register_way) as gb_register_way,\n" +
            "    coalesce(wpgc.cert_num, wdc.gb_cert_num, wdc.cert_num) as gb_cert_num,\n" +
            "    coalesce(wpgc.certifiable, wdc.gb_certifiable, wdc.certifiable) as gb_certifiable,\n" +
            "    coalesce(wpgc.err_code, wdc.gb_err_code, wdc.err_code) as gb_err_code,\n" +
            "    coalesce(wpgc.end_time, wdc.gb_end_time, wdc.end_time) as gb_end_time,\n" +
            "    coalesce(wpgc.secrecy, wdc.gb_secrecy, wdc.secrecy) as gb_secrecy,\n" +
            "    coalesce(wpgc.ip_address, wdc.gb_ip_address, wdc.ip_address) as gb_ip_address,\n" +
            "    coalesce(wpgc.port, wdc.gb_port, wdc.port) as gb_port,\n" +
            "    coalesce(wpgc.password, wdc.gb_password, wdc.password) as gb_password,\n" +
            "    coalesce(wpgc.status, wdc.gb_status, wdc.status) as gb_status,\n" +
            "    coalesce(wpgc.longitude, wdc.gb_longitude, wdc.longitude) as gb_longitude,\n" +
            "    coalesce(wpgc.latitude, wdc.gb_latitude, wdc.latitude) as gb_latitude,\n" +
            "    coalesce(wpgc.ptz_type, wdc.gb_ptz_type, wdc.ptz_type) as gb_ptz_type,\n" +
            "    coalesce(wpgc.position_type, wdc.gb_position_type, wdc.position_type) as gb_position_type,\n" +
            "    coalesce(wpgc.room_type, wdc.gb_room_type, wdc.room_type) as gb_room_type,\n" +
            "    coalesce(wpgc.use_type, wdc.gb_use_type, wdc.use_type) as gb_use_type,\n" +
            "    coalesce(wpgc.supply_light_type, wdc.gb_supply_light_type, wdc.supply_light_type) as gb_supply_light_type,\n" +
            "    coalesce(wpgc.direction_type, wdc.gb_direction_type, wdc.direction_type) as gb_direction_type,\n" +
            "    coalesce(wpgc.resolution, wdc.gb_resolution, wdc.resolution) as gb_resolution,\n" +
            "    coalesce(wpgc.business_group_id, wdc.gb_business_group_id, wdc.business_group_id) as gb_business_group_id,\n" +
            "    coalesce(wpgc.download_speed, wdc.gb_download_speed, wdc.download_speed) as gb_download_speed,\n" +
            "    coalesce(wpgc.svc_space_support_mod, wdc.gb_svc_space_support_mod, wdc.svc_space_support_mod) as gb_svc_space_support_mod,\n" +
            "    coalesce(wpgc.svc_time_support_mode, wdc.gb_svc_time_support_mode, wdc.svc_time_support_mode) as gb_svc_time_support_mode\n" +
            " from wvp_device_channel wdc" +
            " left join wvp_platform_gb_channel wpgc on wdc.id = wpgc.device_channel_id" +
            " where  wpgc.platform_id = #{platformId}" +
            "<if test='channelIds != null'> AND wdc.id in " +
            "   <foreach item='item' index='index' collection='channelIds' open='(' separator=',' close=')'>" +
            "   #{item} " +
            "   </foreach> " +
            "</if>" +
            "</script>")
    List<CommonGBChannel> queryShare(@Param("platformId") Integer platformId, List<Integer> channelIds);

    @Delete("<script> " +
            "DELETE from wvp_platform_gb_channel WHERE platform_id=#{platformId} " +
            "<if test='channelList != null'> AND device_channel_id in " +
            "   <foreach item='item' index='index' collection='channelList' open='(' separator=',' close=')'>" +
            "   #{item.gbId} " +
            "   </foreach> " +
            "</if>" +
            "</script>")
    int removeChannels(@Param("platformId") Integer platformId, List<CommonGBChannel> channelList);

}
