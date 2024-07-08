package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface CommonGBChannelMapper {

    @Select("select\n" +
            "    id as gb_id,\n" +
            "    device_db_id,\n" +
            "    stream_push_id,\n" +
            "    stream_proxy_id,\n" +
            "    create_time,\n" +
            "    update_time,\n" +
            "    sub_count,\n" +
            "    stream_id,\n" +
            "    has_audio,\n" +
            "    gps_time,\n" +
            "    stream_identification,\n" +
            "    coalesce(gb_device_id, device_id) as gb_device_id,\n" +
            "    coalesce(gb_name, name) as gb_name,\n" +
            "    coalesce(gb_manufacturer, manufacturer) as gb_manufacturer,\n" +
            "    coalesce(gb_model, model) as gb_model,\n" +
            "    coalesce(gb_owner, owner) as gb_owner,\n" +
            "    coalesce(gb_civil_code, civil_code) as gb_civil_code,\n" +
            "    coalesce(gb_block, block) as gb_block,\n" +
            "    coalesce(gb_address, address) as gb_address,\n" +
            "    coalesce(gb_parental, parental) as gb_parental,\n" +
            "    coalesce(gb_parent_id, parent_id) as gb_parent_id,\n" +
            "    coalesce(gb_safety_way, safety_way) as gb_safety_way,\n" +
            "    coalesce(gb_register_way, register_way) as gb_register_way,\n" +
            "    coalesce(gb_cert_num, cert_num) as gb_cert_num,\n" +
            "    coalesce(gb_certifiable, certifiable) as gb_certifiable,\n" +
            "    coalesce(gb_err_code, err_code) as gb_err_code,\n" +
            "    coalesce(gb_end_time, end_time) as gb_end_time,\n" +
            "    coalesce(gb_secrecy, secrecy) as gb_secrecy,\n" +
            "    coalesce(gb_ip_address, ip_address) as gb_ip_address,\n" +
            "    coalesce(gb_port, port) as gb_port,\n" +
            "    coalesce(gb_password, password) as gb_password,\n" +
            "    coalesce(gb_status, status) as gb_status,\n" +
            "    coalesce(gb_longitude, longitude) as gb_longitude,\n" +
            "    coalesce(gb_latitude, latitude) as gb_latitude,\n" +
            "    coalesce(gb_ptz_type, ptz_type) as gb_ptz_type,\n" +
            "    coalesce(gb_position_type, position_type) as gb_position_type,\n" +
            "    coalesce(gb_room_type, room_type) as gb_room_type,\n" +
            "    coalesce(gb_use_type, use_type) as gb_use_type,\n" +
            "    coalesce(gb_supply_light_type, supply_light_type) as gb_supply_light_type,\n" +
            "    coalesce(gb_direction_type, direction_type) as gb_direction_type,\n" +
            "    coalesce(gb_resolution, resolution) as gb_resolution,\n" +
            "    coalesce(gb_business_group_id, business_group_id) as gb_business_group_id,\n" +
            "    coalesce(gb_download_speed, download_speed) as gb_download_speed,\n" +
            "    coalesce(gb_svc_space_support_mod, svc_space_support_mod) as gb_svc_space_support_mod,\n" +
            "    coalesce(gb_svc_time_support_mode svc_time_support_mode) as gb_svc_time_support_mode\n" +
            "from wvp_device_channel\n" +
            "where gb_device_id = #{gbDeviceId} or device_id = #{gbDeviceId}")
    CommonGBChannel queryByDeviceId(@Param("gbDeviceId") String gbDeviceId);

    @Insert(" <script>" +
            "INSERT INTO wvp_device_channel (" +
            "gb_device_id," +
            " <if test='streamProxyId != null' > stream_proxy_id,</if>" +
            " <if test='streamPushId != null' > stream_push_id,</if>" +
            "create_time," +
            "update_time," +
            "sub_count," +
            "stream_id," +
            "has_audio," +
            "gps_time," +
            "stream_identification," +
            "gb_name," +
            "gb_manufacturer," +
            "gb_model," +
            "gb_owner," +
            "gb_civil_code," +
            "gb_block," +
            "gb_address," +
            "gb_parental," +
            "gb_parent_id ," +
            "gb_safety_way," +
            "gb_register_way," +
            "gb_cert_num," +
            "gb_certifiable," +
            "gb_err_code," +
            "gb_end_time," +
            "gb_secrecy," +
            "gb_ip_address," +
            "gb_port," +
            "gb_password," +
            "gb_status," +
            "gb_longitude," +
            "gb_latitude," +
            "gb_ptz_type," +
            "gb_position_type," +
            "gb_room_type," +
            "gb_use_type," +
            "gb_supply_light_type," +
            "gb_direction_type," +
            "gb_resolution," +
            "gb_business_group_id," +
            "gb_download_speed," +
            "gb_svc_space_support_mod," +
            "gb_svc_time_support_mode ) " +
            "VALUES (" +
            "#{gbDeviceId}, " +
            " <if test='streamProxyId != null' > #{streamProxyId},</if>" +
            " <if test='streamPushId != null' > #{streamPushId},</if>" +
            "#{createTime}, " +
            "#{updateTime}, " +
            "#{subCount}, " +
            "#{streamId}, " +
            "#{hasAudio}, " +
            "#{gpsTime}, " +
            "#{streamIdentification}, " +
            "#{gbName}, " +
            "#{gbManufacturer}, " +
            "#{gbModel}, " +
            "#{gbOwner}, " +
            "#{gbCivilCode}, " +
            "#{gbBlock}, " +
            "#{gbAddress}, " +
            "#{gbParental}, " +
            "#{gbParentId}, " +
            "#{gbSafetyWay}, " +
            "#{gbRegisterWay}, " +
            "#{gbCertNum}, " +
            "#{gbCertifiable}, " +
            "#{gbErrCode}, " +
            "#{gbEndTime}, " +
            "#{gbSecrecy},"+
            "#{gbIpAddress},"+
            "#{gbPort},"+
            "#{gbPassword},"+
            "#{gbStatus},"+
            "#{gbLongitude},"+
            "#{gbLatitude},"+
            "#{gbPtzType},"+
            "#{gbPositionType},"+
            "#{gbRoomType},"+
            "#{gbUseType},"+
            "#{gbSupplyLightType},"+
            "#{gbDirectionType},"+
            "#{gbResolution},"+
            "#{gbBusinessGroupId},"+
            "#{gbDownloadSpeed},"+
            "#{gbSvcSpaceSupportMod},"+
            "#{gbSvcTimeSupportMode}"+
            ")" +
            " </script>")
    int insert(CommonGBChannel commonGBChannel);

    @Select(" select\n" +
            "    id as gb_id,\n" +
            "    device_db_id,\n" +
            "    stream_push_id,\n" +
            "    stream_proxy_id,\n" +
            "    create_time,\n" +
            "    update_time,\n" +
            "    sub_count,\n" +
            "    stream_id,\n" +
            "    has_audio,\n" +
            "    gps_time,\n" +
            "    stream_identification,\n" +
            "    coalesce(gb_device_id, device_id) as gb_device_id,\n" +
            "    coalesce(gb_name, name) as gb_name,\n" +
            "    coalesce(gb_manufacturer, manufacturer) as gb_manufacturer,\n" +
            "    coalesce(gb_model, model) as gb_model,\n" +
            "    coalesce(gb_owner, owner) as gb_owner,\n" +
            "    coalesce(gb_civil_code, civil_code) as gb_civil_code,\n" +
            "    coalesce(gb_block, block) as gb_block,\n" +
            "    coalesce(gb_address, address) as gb_address,\n" +
            "    coalesce(gb_parental, parental) as gb_parental,\n" +
            "    coalesce(gb_parent_id, parent_id) as gb_parent_id,\n" +
            "    coalesce(gb_safety_way, safety_way) as gb_safety_way,\n" +
            "    coalesce(gb_register_way, register_way) as gb_register_way,\n" +
            "    coalesce(gb_cert_num, cert_num) as gb_cert_num,\n" +
            "    coalesce(gb_certifiable, certifiable) as gb_certifiable,\n" +
            "    coalesce(gb_err_code, err_code) as gb_err_code,\n" +
            "    coalesce(gb_end_time, end_time) as gb_end_time,\n" +
            "    coalesce(gb_secrecy, secrecy) as gb_secrecy,\n" +
            "    coalesce(gb_ip_address, ip_address) as gb_ip_address,\n" +
            "    coalesce(gb_port, port) as gb_port,\n" +
            "    coalesce(gb_password, password) as gb_password,\n" +
            "    coalesce(gb_status, status) as gb_status,\n" +
            "    coalesce(gb_longitude, longitude) as gb_longitude,\n" +
            "    coalesce(gb_latitude, latitude) as gb_latitude,\n" +
            "    coalesce(gb_ptz_type, ptz_type) as gb_ptz_type,\n" +
            "    coalesce(gb_position_type, position_type) as gb_position_type,\n" +
            "    coalesce(gb_room_type, room_type) as gb_room_type,\n" +
            "    coalesce(gb_use_type, use_type) as gb_use_type,\n" +
            "    coalesce(gb_supply_light_type, supply_light_type) as gb_supply_light_type,\n" +
            "    coalesce(gb_direction_type, direction_type) as gb_direction_type,\n" +
            "    coalesce(gb_resolution, resolution) as gb_resolution,\n" +
            "    coalesce(gb_business_group_id, business_group_id) as gb_business_group_id,\n" +
            "    coalesce(gb_download_speed, download_speed) as gb_download_speed,\n" +
            "    coalesce(gb_svc_space_support_mod, svc_space_support_mod) as gb_svc_space_support_mod,\n" +
            "    coalesce(gb_svc_time_support_mode svc_time_support_mode) as gb_svc_time_support_mode\n" +
            " from wvp_device_channel" +
            " where id = #{gbId}")
    CommonGBChannel queryById(@Param("gbId") int gbId);

    @Delete(value = {"delete from wvp_device_channel where id = #{gbId} "})
    void delete(int gbId);

    @Update(value = {" <script>" +
            "UPDATE wvp_device_channel " +
            "SET update_time=#{updateTime}" +
            "<if test='streamPushId != null'>, stream_push_id  = #{streamPushId}</if>" +
            "<if test='streamPushId  == null'>, stream_push_id = null</if>" +
            "<if test='streamProxyId != null'>,  stream_proxy_id = #{streamProxyId}</if>" +
            "<if test='streamProxyId  == null'>, stream_proxy_id = null</if>" +
            "<if test='subCount != null'>,  sub_count = #{subCount}</if>" +
            "<if test='subCount  == null'>, sub_count = null</if>" +
            "<if test='streamId != null'>,  stream_id = #{streamId}</if>" +
            "<if test='streamId  == null'>, stream_id = null</if>" +
            "<if test='hasAudio != null'>,  has_audio = #{hasAudio}</if>" +
            "<if test='hasAudio  == null'>, has_audio = null</if>" +
            "<if test='gpsTime != null'>,  gps_time = #{gpsTime}</if>" +
            "<if test='gpsTime  == null'>, gps_time = null</if>" +
            "<if test='streamIdentification != null'>,  stream_identification = #{streamIdentification}</if>" +
            "<if test='streamIdentification  == null'>, stream_identification = null</if>" +
            "<if test='gbDeviceId != null'>, gb_device_id = #{gbDeviceId}</if>" +
            "<if test='gbDeviceId  == null'>, gb_device_id = null</if>" +
            "<if test='gbName != null'>,  gb_name = #{gbName}</if>" +
            "<if test='gbName == null'>, gb_name = null</if>" +
            "<if test='gbManufacturer != null'>,  gb_manufacturer = #{gbManufacturer}</if>" +
            "<if test='gbManufacturer == null'>, gb_manufacturer = null</if>" +
            "<if test='gbModel != null'>,  gb_model = #{gbModel}</if>" +
            "<if test='gbModel  == null'>,   gb_model = null</if>" +
            "<if test='gbOwner != null' >, gb_owner = #{gbOwner}</if>" +
            "<if test='gbOwner  == null'>, gb_owner = null</if>" +
            "<if test='gbCivilCode, != null' >, gb_civil_code = #{gbCivilCode}</if>" +
            "<if test='gbCivilCode,  == null'>, gb_civil_code = null</if>" +
            "<if test='gbBlock != null' >, gb_block = #{gbBlock}</if>" +
            "<if test='gbBlock  == null'>, gb_block = null</if>" +
            "<if test='gbAddress != null' >, gb_address = #{gbAddress}</if>" +
            "<if test='gbAddress  == null'>, gb_address = null</if>" +
            "<if test='gbParental != null' >, gb_parental = #{gbParental}</if>" +
            "<if test='gbParental  == null'>, gb_parental = null</if>" +
            "<if test='gbParentId != null' >, gb_parent_id = #{gbParentId}</if>" +
            "<if test='gbParentId  == null'>, gb_parent_id = null</if>" +
            "<if test='gbSafetyWay != null' >, gb_safety_way = #{gbSafetyWay}</if>" +
            "<if test='gbSafetyWay  == null'>, gb_safety_way = null</if>" +
            "<if test='gbRegisterWay != null' >, gb_register_way = #{gbRegisterWay}</if>" +
            "<if test='gbRegisterWay  == null'>, gb_register_way = null</if>" +
            "<if test='gbCertNum != null' >, gb_cert_num = #{gbCertNum}</if>" +
            "<if test='gbCertNum  == null'>, gb_cert_num = null</if>" +
            "<if test='gbCertifiable != null' >, gb_certifiable = #{gbCertifiable}</if>" +
            "<if test='gbCertifiable  == null'>, gb_certifiable = null</if>" +
            "<if test='gbErrCode != null' >, gb_err_code = #{gbErrCode}</if>" +
            "<if test='gbErrCode  == null'>, gb_err_code = null</if>" +
            "<if test='gbEndTime != null' >, gb_end_time = #{gbEndTime}</if>" +
            "<if test='gbEndTime  == null'>, gb_end_time = null</if>" +
            "<if test='gbSecrecy != null' >, gb_secrecy = #{gbSecrecy}</if>" +
            "<if test='gbSecrecy  == null'>, gb_secrecy = null</if>" +
            "<if test='gbIpAddress != null' >, gb_ip_address = #{gbIpAddress}</if>" +
            "<if test='gbIpAddress  == null'>, gb_ip_address = null</if>" +
            "<if test='gbPort != null' >, gb_ip_address = #{gbPort}</if>" +
            "<if test='gbPort  == null'>, gb_port = null</if>" +
            "<if test='gbPassword != null' >, gb_password = #{gbPassword}</if>" +
            "<if test='gbPassword  == null'>, gb_password = null</if>" +
            "<if test='gbStatus != null' >, gb_status = #{gbStatus}</if>" +
            "<if test='gbStatus  == null'>, gb_status = null</if>" +
            "<if test='gbLongitude != null' >, gb_longitude = #{gbLongitude}</if>" +
            "<if test='gbLongitude  == null'>, gb_longitude = null</if>" +
            "<if test='gbLatitude != null' >, gb_latitude = #{gbLatitude}</if>" +
            "<if test='gbLatitude  == null'>, gb_latitude = null</if>" +
            "<if test='gbPtzType != null' >, gb_ptz_type = #{gbPtzType}</if>" +
            "<if test='gbPtzType  == null'>, gb_ptz_type = null</if>" +
            "<if test='gbPositionType != null' >, gb_position_type = #{gbPositionType}</if>" +
            "<if test='gbPositionType  == null'>, gb_position_type = null</if>" +
            "<if test='gbRoomType != null' >, gb_room_type = #{gbRoomType}</if>" +
            "<if test='gbRoomType  == null'>, gb_room_type = null</if>" +
            "<if test='gbUseType != null' >, gb_use_type = #{gbUseType}</if>" +
            "<if test='gbUseType  == null'>, gb_use_type = null</if>" +
            "<if test='gbSupplyLightType != null' >, gb_supply_light_type = #{gbSupplyLightType}</if>" +
            "<if test='gbSupplyLightType  == null'>, gb_supply_light_type = null</if>" +
            "<if test='gbDirectionType != null' >, gb_direction_type = #{gbDirectionType}</if>" +
            "<if test='gbDirectionType  == null'>, gb_direction_type = null</if>" +
            "<if test='gbResolution != null' >, gb_resolution = #{gbResolution}</if>" +
            "<if test='gbResolution  == null'>, gb_resolution = null</if>" +
            "<if test='gbBusinessGroupId != null' >, gb_business_group_id = #{gbBusinessGroupId}</if>" +
            "<if test='gbBusinessGroupId  == null'>, gb_business_group_id = null</if>" +
            "<if test='gbDownloadSpeed != null' >, gb_download_speed = #{gbDownloadSpeed}</if>" +
            "<if test='gbDownloadSpeed  == null'>, gb_download_speed = null</if>" +
            "<if test='gbSvcSpaceSupportMod != null' >, gb_svc_space_support_mod = #{gbSvcSpaceSupportMod}</if>" +
            "<if test='gbSvcSpaceSupportMod  == null'>, gb_svc_space_support_mod = null</if>" +
            "<if test='gbSvcTimeSupportMode != null' >, gb_svc_time_support_mode = #{gbSvcTimeSupportMode}</if>" +
            "<if test='gbSvcTimeSupportMode  == null'>, gb_svc_time_support_mode = null</if>" +
            "WHERE id = #{gbId}"+
            " </script>"})
    int update(CommonGBChannel commonGBChannel);

    @Update(value = {" <script>" +
            "UPDATE wvp_device_channel " +
            "SET gb_status = #{gbStatus}" +
            "WHERE id = #{gbId}"+
            " </script>"})
    int updateStatusById(@Param("gbId") int gbId, @Param("status") int status);

    @Update("<script> " +
            "<foreach collection='commonGBChannels' index='index' item='item' separator=';'> " +
            "UPDATE wvp_device_channel SET gb_status = #{gbStatus} WHERE id = #{item.gbId}" +
            "</foreach> " +
            "</script>")
    int updateStatusForListById(List<CommonGBChannel> commonGBChannels, @Param("status") int status);

    @Select(value = {" <script>" +
            " select\n" +
            "    id as gb_id,\n" +
            "    device_db_id,\n" +
            "    stream_push_id,\n" +
            "    stream_proxy_id,\n" +
            "    create_time,\n" +
            "    update_time,\n" +
            "    sub_count,\n" +
            "    stream_id,\n" +
            "    has_audio,\n" +
            "    gps_time,\n" +
            "    stream_identification,\n" +
            "    coalesce(gb_device_id, device_id) as gb_device_id,\n" +
            "    coalesce(gb_name, name) as gb_name,\n" +
            "    coalesce(gb_manufacturer, manufacturer) as gb_manufacturer,\n" +
            "    coalesce(gb_model, model) as gb_model,\n" +
            "    coalesce(gb_owner, owner) as gb_owner,\n" +
            "    coalesce(gb_civil_code, civil_code) as gb_civil_code,\n" +
            "    coalesce(gb_block, block) as gb_block,\n" +
            "    coalesce(gb_address, address) as gb_address,\n" +
            "    coalesce(gb_parental, parental) as gb_parental,\n" +
            "    coalesce(gb_parent_id, parent_id) as gb_parent_id,\n" +
            "    coalesce(gb_safety_way, safety_way) as gb_safety_way,\n" +
            "    coalesce(gb_register_way, register_way) as gb_register_way,\n" +
            "    coalesce(gb_cert_num, cert_num) as gb_cert_num,\n" +
            "    coalesce(gb_certifiable, certifiable) as gb_certifiable,\n" +
            "    coalesce(gb_err_code, err_code) as gb_err_code,\n" +
            "    coalesce(gb_end_time, end_time) as gb_end_time,\n" +
            "    coalesce(gb_secrecy, secrecy) as gb_secrecy,\n" +
            "    coalesce(gb_ip_address, ip_address) as gb_ip_address,\n" +
            "    coalesce(gb_port, port) as gb_port,\n" +
            "    coalesce(gb_password, password) as gb_password,\n" +
            "    coalesce(gb_status, status) as gb_status,\n" +
            "    coalesce(gb_longitude, longitude) as gb_longitude,\n" +
            "    coalesce(gb_latitude, latitude) as gb_latitude,\n" +
            "    coalesce(gb_ptz_type, ptz_type) as gb_ptz_type,\n" +
            "    coalesce(gb_position_type, position_type) as gb_position_type,\n" +
            "    coalesce(gb_room_type, room_type) as gb_room_type,\n" +
            "    coalesce(gb_use_type, use_type) as gb_use_type,\n" +
            "    coalesce(gb_supply_light_type, supply_light_type) as gb_supply_light_type,\n" +
            "    coalesce(gb_direction_type, direction_type) as gb_direction_type,\n" +
            "    coalesce(gb_resolution, resolution) as gb_resolution,\n" +
            "    coalesce(gb_business_group_id, business_group_id) as gb_business_group_id,\n" +
            "    coalesce(gb_download_speed, download_speed) as gb_download_speed,\n" +
            "    coalesce(gb_svc_space_support_mod, svc_space_support_mod) as gb_svc_space_support_mod,\n" +
            "    coalesce(gb_svc_time_support_mode svc_time_support_mode) as gb_svc_time_support_mode\n" +
            " from wvp_device_channel" +
            " where gb_status=#{status} and id in " +
            " <foreach collection='commonGBChannelList'  item='item'  open='(' separator=',' close=')' > #{item.gbId}</foreach>" +
            "</script>"})
    List<CommonGBChannel> queryInListByStatus(List<CommonGBChannel> commonGBChannelList, @Param("status") int status);


    @Insert(" <script>" +
            "INSERT INTO wvp_device_channel (" +
            "gb_device_id," +
            "stream_proxy_id, " +
            "stream_push_id," +
            "create_time," +
            "update_time," +
            "sub_count," +
            "stream_id," +
            "has_audio," +
            "gps_time," +
            "stream_identification," +
            "gb_name," +
            "gb_manufacturer," +
            "gb_model," +
            "gb_owner," +
            "gb_civil_code," +
            "gb_block," +
            "gb_address," +
            "gb_parental," +
            "gb_parent_id ," +
            "gb_safety_way," +
            "gb_register_way," +
            "gb_cert_num," +
            "gb_certifiable," +
            "gb_err_code," +
            "gb_end_time," +
            "gb_secrecy," +
            "gb_ip_address," +
            "gb_port," +
            "gb_password," +
            "gb_status," +
            "gb_longitude," +
            "gb_latitude," +
            "gb_ptz_type," +
            "gb_position_type," +
            "gb_room_type," +
            "gb_use_type," +
            "gb_supply_light_type," +
            "gb_direction_type," +
            "gb_resolution," +
            "gb_business_group_id," +
            "gb_download_speed," +
            "gb_svc_space_support_mod," +
            "gb_svc_time_support_mode ) " +
            "VALUES" +
            "<foreach collection='commonGBChannels' index='index' item='item' separator=','> " +
            "(#{item.gbDeviceId}, #{item.streamProxyId}, #{item.streamPushId},#{item.createTime},#{item.updateTime},#{item.subCount}," +
            "#{item.streamId},#{item.hasAudio},#{item.gpsTime},#{item.streamIdentification},#{item.gbName},#{item.gbManufacturer}, #{item.gbModel}," +
            "#{item.gbOwner},#{item.gbCivilCode},#{item.gbBlock}, #{item.gbAddress}, #{item.gbParental}, #{item.gbParentId},#{item.gbSafetyWay}, " +
            "#{item.gbRegisterWay},#{item.gbCertNum},#{item.gbCertifiable},#{item.gbErrCode},#{item.gbEndTime}, #{item.gbSecrecy},#{item.gbIpAddress}," +
            "#{item.gbPort},#{item.gbPassword},#{item.gbStatus},#{item.gbLongitude}, #{item.gbLatitude},#{item.gbPtzType},#{item.gbPositionType},#{item.gbRoomType}," +
            "#{item.gbUseType},#{item.gbSupplyLightType},#{item.gbDirectionType},#{item.gbResolution},#{item.gbBusinessGroupId},#{item.gbDownloadSpeed}," +
            "#{item.gbSvcSpaceSupportMod},#{item.gbSvcTimeSupportMode})" +
            "</foreach> " +
            " </script>")
    int batchAdd(List<CommonGBChannel> commonGBChannels);

    @Update("<script> " +
            "<foreach collection='commonGBChannels' index='index' item='item' separator=';'> " +
            "UPDATE wvp_device_channel SET gb_status = #{item.gbStatus} WHERE id = #{item.gbId}" +
            "</foreach> " +
            "</script>")
    int updateStatus(List<CommonGBChannel> commonGBChannels);

    @Select(value = {"select\n" +
            "    wdc.id as gb_id,\n" +
            "    wdc.device_db_id,\n" +
            "    wdc.create_time,\n" +
            "    wdc.update_time,\n" +
            "    wdc.sub_count,\n" +
            "    wdc.stream_id,\n" +
            "    wdc.has_audio,\n" +
            "    wdc.gps_time,\n" +
            "    wdc.stream_identification,\n" +
            "    coalesce(wpgc.device_id, wdc.gb_device_id, wdc.device_id) as gb_device_id,\n" +
            "    coalesce(wpgc.name, wdc.gb_name, wdc.name) as gb_name,\n" +
            "    coalesce(wdc.gb_manufacturer, wdc.manufacturer) as gb_manufacturer,\n" +
            "    coalesce(wdc.gb_model, wdc.model) as gb_model,\n" +
            "    coalesce(wdc.gb_owner, wdc.owner) as gb_owner,\n" +
            "    coalesce(wpgc.civil_code, wdc.gb_civil_code, wdc.civil_code) as gb_civil_code,\n" +
            "    coalesce(wdc.gb_block, wdc.block) as gb_block,\n" +
            "    coalesce(wdc.gb_address, wdc.address) as gb_address,\n" +
            "    coalesce(wpgc.parental, wdc.gb_parental, wdc.parental) as gb_parental,\n" +
            "    coalesce(wpgc.parent_id, wdc.gb_parent_id, wdc.parent_id) as gb_parent_id,\n" +
            "    coalesce(wdc.gb_safety_way, wdc.safety_way) as gb_safety_way,\n" +
            "    coalesce(wdc.gb_register_way, wdc.register_way) as gb_register_way,\n" +
            "    coalesce(wdc.gb_cert_num, wdc.cert_num) as gb_cert_num,\n" +
            "    coalesce(wdc.gb_certifiable, wdc.certifiable) as gb_certifiable,\n" +
            "    coalesce(wdc.gb_err_code, wdc.err_code) as gb_err_code,\n" +
            "    coalesce(wdc.gb_end_time, wdc.end_time) as gb_end_time,\n" +
            "    coalesce(wdc.gb_secrecy, wdc.secrecy) as gb_secrecy,\n" +
            "    coalesce(wdc.gb_ip_address, wdc.ip_address) as gb_ip_address,\n" +
            "    coalesce(wdc.gb_port, wdc.port) as gb_port,\n" +
            "    coalesce(wdc.gb_password, wdc.password) as gb_password,\n" +
            "    coalesce(wdc.gb_status, wdc.status) as gb_status,\n" +
            "    coalesce(wdc.gb_longitude, wdc.longitude) as gb_longitude,\n" +
            "    coalesce(wdc.gb_latitude, wdc.latitude) as gb_latitude,\n" +
            "    coalesce(wdc.gb_ptz_type, wdc.ptz_type) as gb_ptz_type,\n" +
            "    coalesce(wdc.gb_position_type, wdc.position_type) as gb_position_type,\n" +
            "    coalesce(wdc.gb_room_type, wdc.room_type) as gb_room_type,\n" +
            "    coalesce(wdc.gb_use_type, wdc.use_type) as gb_use_type,\n" +
            "    coalesce(wdc.gb_supply_light_type, wdc.supply_light_type) as gb_supply_light_type,\n" +
            "    coalesce(wdc.gb_direction_type, wdc.direction_type) as gb_direction_type,\n" +
            "    coalesce(wdc.gb_resolution, wdc.resolution) as gb_resolution,\n" +
            "    coalesce(wpgc.business_group_id, wdc.gb_business_group_id, wdc.business_group_id) as gb_business_group_id,\n" +
            "    coalesce(wdc.gb_download_speed, wdc.download_speed) as gb_download_speed,\n" +
            "    coalesce(wdc.gb_svc_space_support_mod, wdc.svc_space_support_mod) as gb_svc_space_support_mod,\n" +
            "    coalesce(wdc.gb_svc_time_support_mode wdc.svc_time_support_mode) as gb_svc_time_support_mode\n" +
            "from wvp_device_channel wdc left join wvp_platform_gb_channel wpgc on wdc.id = wpgc.device_channel_id\n" +
            "where wpgc.platform_id = #{platformId}"})
    List<CommonGBChannel> queryByPlatformId(@Param("platformId") Integer platformId);
}
