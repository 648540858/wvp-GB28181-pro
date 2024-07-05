package com.genersoft.iot.vmp.gb28181.dao;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

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

    @Delete("delete from wvp_device_channel where id = #{gbId} ")
    void delete(int gbId);

    @Update(value = {" <script>" +
            "UPDATE wvp_device_channel " +
            "SET update_time=#{updateTime}" +
            "<if test='streamPushId != null'>, stream_push_id  = #{streamPushId}" +
            "<if test='streamPushId  == null'>, stream_push_id = null" +
            "<if test='streamProxyId != null'>,  stream_proxy_id = #{streamProxyId}" +
            "<if test='streamProxyId  == null'>, stream_proxy_id = null" +
            "<if test='subCount != null'>,  sub_count = #{subCount}" +
            "<if test='subCount  == null'>, sub_count = null" +
            "<if test='streamId != null'>,  stream_id = #{streamId}" +
            "<if test='streamId  == null'>, stream_id = null" +
            "<if test='hasAudio != null'>,  has_audio = #{hasAudio}" +
            "<if test='hasAudio  == null'>, has_audio = null" +
            "<if test='gpsTime != null'>,  gps_time = #{gpsTime}" +
            "<if test='gpsTime  == null'>, gps_time = null" +
            "<if test='streamIdentification != null'>,  stream_identification = #{streamIdentification}" +
            "<if test='streamIdentification  == null'>, stream_identification = null" +
            "<if test='gbDeviceId != null'>, gb_device_id = #{gbDeviceId}" +
            "<if test='gbDeviceId  == null'>, gb_device_id = null" +
            "<if test='gbName != null'>,  gb_name = #{gbName}" +
            "<if test='gbName == null'>, gb_name = null" +
            "<if test='gbManufacturer != null'>,  gb_manufacturer = #{gbManufacturer}" +
            "<if test='gbManufacturer == null'>, gb_manufacturer = null" +
            "<if test='gbModel != null'>,  gb_model = #{gbModel}" +
            "<if test='gbModel  == null'>,   gb_model = null" +
            "<if test='gbOwner != null' >, gb_owner = #{gbOwner}" +
            "<if test='gbOwner  == null'>, gb_owner = null" +
            "<if test='gbCivilCode, != null' >, gb_civil_code = #{gbCivilCode}" +
            "<if test='gbCivilCode,  == null'>, gb_civil_code = null" +
            "<if test='gbBlock != null' >, gb_block = #{gbBlock}" +
            "<if test='gbBlock  == null'>, gb_block = null" +
            "<if test='gbAddress != null' >, gb_address = #{gbAddress}" +
            "<if test='gbAddress  == null'>, gb_address = null" +
            "<if test='gbParental != null' >, gb_parental = #{gbParental}" +
            "<if test='gbParental  == null'>, gb_parental = null" +
            "<if test='gbParentId != null' >, gb_parent_id = #{gbParentId}" +
            "<if test='gbParentId  == null'>, gb_parent_id = null" +
            "<if test='gbSafetyWay != null' >, gb_safety_way = #{gbSafetyWay}" +
            "<if test='gbSafetyWay  == null'>, gb_safety_way = null" +
            "<if test='gbRegisterWay != null' >, gb_register_way = #{gbRegisterWay}" +
            "<if test='gbRegisterWay  == null'>, gb_register_way = null" +
            "<if test='gbCertNum != null' >, gb_cert_num = #{gbCertNum}" +
            "<if test='gbCertNum  == null'>, gb_cert_num = null" +
            "<if test='gbCertifiable != null' >, gb_certifiable = #{gbCertifiable}" +
            "<if test='gbCertifiable  == null'>, gb_certifiable = null" +
            "<if test='gbErrCode != null' >, gb_err_code = #{gbErrCode}" +
            "<if test='gbErrCode  == null'>, gb_err_code = null" +
            "<if test='gbEndTime != null' >, gb_end_time = #{gbEndTime}" +
            "<if test='gbEndTime  == null'>, gb_end_time = null" +
            "<if test='gbSecrecy != null' >, gb_secrecy = #{gbSecrecy}" +
            "<if test='gbSecrecy  == null'>, gb_secrecy = null" +
            "<if test='gbIpAddress != null' >, gb_ip_address = #{gbIpAddress}" +
            "<if test='gbIpAddress  == null'>, gb_ip_address = null" +
            "<if test='gbPort != null' >, gb_ip_address = #{gbPort}" +
            "<if test='gbPort  == null'>, gb_port = null" +
            "<if test='gbPassword != null' >, gb_password = #{gbPassword}" +
            "<if test='gbPassword  == null'>, gb_password = null" +
            "<if test='gbStatus != null' >, gb_status = #{gbStatus}" +
            "<if test='gbStatus  == null'>, gb_status = null" +
            "<if test='gbLongitude != null' >, gb_longitude = #{gbLongitude}" +
            "<if test='gbLongitude  == null'>, gb_longitude = null" +
            "<if test='gbLatitude != null' >, gb_latitude = #{gbLatitude}" +
            "<if test='gbLatitude  == null'>, gb_latitude = null" +
            "<if test='gbPtzType != null' >, gb_ptz_type = #{gbPtzType}" +
            "<if test='gbPtzType  == null'>, gb_ptz_type = null" +
            "<if test='gbPositionType != null' >, gb_position_type = #{gbPositionType}" +
            "<if test='gbPositionType  == null'>, gb_position_type = null" +
            "<if test='gbRoomType != null' >, gb_room_type = #{gbRoomType}" +
            "<if test='gbRoomType  == null'>, gb_room_type = null" +
            "<if test='gbUseType != null' >, gb_use_type = #{gbUseType}" +
            "<if test='gbUseType  == null'>, gb_use_type = null" +
            "<if test='gbSupplyLightType != null' >, gb_supply_light_type = #{gbSupplyLightType}" +
            "<if test='gbSupplyLightType  == null'>, gb_supply_light_type = null" +
            "<if test='gbDirectionType != null' >, gb_direction_type = #{gbDirectionType}" +
            "<if test='gbDirectionType  == null'>, gb_direction_type = null" +
            "<if test='gbResolution != null' >, gb_resolution = #{gbResolution}" +
            "<if test='gbResolution  == null'>, gb_resolution = null" +
            "<if test='gbBusinessGroupId != null' >, gb_business_group_id = #{gbBusinessGroupId}" +
            "<if test='gbBusinessGroupId  == null'>, gb_business_group_id = null" +
            "<if test='gbDownloadSpeed != null' >, gb_download_speed = #{gbDownloadSpeed}" +
            "<if test='gbDownloadSpeed  == null'>, gb_download_speed = null" +
            "<if test='gbSvcSpaceSupportMod != null' >, gb_svc_space_support_mod = #{gbSvcSpaceSupportMod}" +
            "<if test='gbSvcSpaceSupportMod  == null'>, gb_svc_space_support_mod = null" +
            "<if test='gbSvcTimeSupportMode != null' >, gb_svc_time_support_mode = #{gbSvcTimeSupportMode}" +
            "<if test='gbSvcTimeSupportMode  == null'>, gb_svc_time_support_mode = null" +
            "WHERE id = #{gbId}"+
            " </script>"})
    int update(CommonGBChannel commonGBChannel);

    int updateStatus(@Param("gbId") int gbId, @Param("status") int status);
}
