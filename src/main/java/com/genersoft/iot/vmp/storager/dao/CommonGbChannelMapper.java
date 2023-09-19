package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface CommonGbChannelMapper {

    @Select(value = "select * from wvp_common_gb_channel where common_gb_business_group_id = '#{commonBusinessGroupPath}'")
    List<CommonGbChannel> getChannels(String commonBusinessGroupPath);

    @Update(value = "<script>" +
            "<foreach collection='channels' item='item' separator=';'>" +
            "UPDATE wvp_common_gb_channel SET " +
            "updateTime= #{ item.updateTime} " +
            " <if test='item.commonGbDeviceID != null' > ,common_gb_device_id= #{ item.commonGbDeviceID} </if>" +
            " <if test='item.commonGbName != null' > ,common_gb_name= #{ item.commonGbName} </if>" +
            " <if test='item.commonGbManufacturer != null' > ,common_gb_manufacturer= #{ item.commonGbManufacturer} </if>" +
            " <if test='item.commonGbModel != null' > ,common_gb_model= #{ item.commonGbModel} </if>" +
            " <if test='item.commonGbOwner != null' > ,common_gb_owner= #{ item.commonGbOwner} </if>" +
            " <if test='item.commonGbCivilCode != null' > ,common_gb_civilCode= #{ item.commonGbCivilCode} </if>" +
            " <if test='item.commonGbBlock != null' > ,common_gb_block= #{ item.commonGbBlock} </if>" +
            " <if test='item.commonGbAddress != null' > ,common_gb_address= #{ item.commonGbAddress} </if>" +
            " <if test='item.common_gb_parental != null' > ,common_gb_parental= #{ item.commonGbParental} </if>" +
            " <if test='item.commonGbParentID != null' > ,common_gb_parent_id= #{ item.commonGbParentID} </if>" +
            " <if test='item.commonGbSafetyWay != null' > ,common_gb_safety_way= #{ item.commonGbSafetyWay} </if>" +
            " <if test='item.commonGbRegisterWay != null' > ,common_gb_register_way= #{ item.commonGbRegisterWay} </if>" +
            " <if test='item.commonGbCertNum != null' > ,common_gb_cert_num= #{ item.commonGbCertNum} </if>" +
            " <if test='item.commonGbCertifiable != null' > ,common_gb_certifiable= #{ item.commonGbCertifiable} </if>" +
            " <if test='item.commonGbErrCode != null' > ,common_gb_err_code= #{ item.commonGbErrCode} </if>" +
            " <if test='item.commonGbEndTime != null' > ,common_gb_end_time= #{ item.commonGbEndTime} </if>" +
            " <if test='item.commonGbSecrecy != null' > ,common_gb_secrecy= #{ item.commonGbSecrecy} </if>" +
            " <if test='item.commonGbIPAddress != null' > ,common_gb_ip_address= #{ item.commonGbIPAddress} </if>" +
            " <if test='item.commonGbPort != null' > ,common_gb_port= #{ item.commonGbPort} </if>" +
            " <if test='item.commonGbPassword != null' > ,common_gb_password= #{ item.commonGbPassword} </if>" +
            " <if test='item.commonGbStatus != null' > ,common_gb_status= #{ item.commonGbStatus} </if>" +
            " <if test='item.commonGbLongitude != null' > ,common_gb_longitude= #{ item.commonGbLongitude} </if>" +
            " <if test='item.commonGbLatitude != null' > ,common_gb_latitude= #{ item.commonGbLatitude} </if>" +
            " <if test='item.commonGbPtzType != null' > ,common_gb_ptz_type= #{ item.commonGbPtzType} </if>" +
            " <if test='item.commonGbPositionType != null' > ,common_gb_position_type= #{ item.commonGbPositionType} </if>" +
            " <if test='item.commonGbRoomType != null' > ,common_gb_room_type= #{ item.commonGbRoomType} </if>" +
            " <if test='item.commonGbUseType != null' > ,common_gb_use_type= #{ item.commonGbUseType} </if>" +
            " <if test='item.commonGbEndTime != null' > ,common_gb_supply_light_type= #{ item.commonGbSupplyLightType} </if>" +
            " <if test='item.commonGbSupplyLightType != null' > ,common_gb_direction_type= #{ item.commonGbDirectionType} </if>" +
            " <if test='item.commonGbResolution != null' > ,common_gb_resolution= #{ item.commonGbResolution} </if>" +
            " <if test='item.commonGbBusinessGroupID != null' > ,common_gb_business_group_id= #{ item.commonGbBusinessGroupID} </if>" +
            " <if test='item.commonGbDownloadSpeed != null' > ,common_gb_download_speed= #{ item.commonGbDownloadSpeed} </if>" +
            " <if test='item.commonGbSVCTimeSupportMode != null' > ,common_gb_svc_time_support_mode= #{ item.commonGbSVCTimeSupportMode} </if>" +
            " <if test='item.type != null' > ,type= #{ item.type} </if>" +
            " WHERE common_gb_id=#{item.commonGbId}" +
            "</foreach>" +
            "</script>")
    int updateChanelForBusinessGroup(List<CommonGbChannel> channels);


    @Delete(value = "<script>" +
            "<foreach collection='channels' item='item' separator=';'>" +
            "delete from wvp_common_gb_channel WHERE common_gb_id=#{item.commonGbId}" +
            "</foreach>" +
            "</script>")
    int removeChannelsForBusinessGroup(List<CommonGbChannel> channels);

    @Update("update wvp_common_gb_channel set common_gb_business_group_id = #{newPath} where common_gb_business_group_id = #{oldPath}")
    int updateBusinessGroupPath(String oldPath, String newPath);

    @Select("select * from wvp_common_gb_channel where common_gb_device_id=#{channelId}")
    CommonGbChannel queryByDeviceID(String channelId);

    @Insert(value = "<script>" +
            "insert into wvp_common_gb_channel ( " +
            "common_gb_device_id" +
            " <if test='common_gb_name != null' > ,common_gb_name </if>" +
            " <if test='common_gb_manufacturer != null' > ,common_gb_manufacturer </if>" +
            " <if test='common_gb_model != null' > ,common_gb_model </if>" +
            " <if test='common_gb_owner != null' > ,common_gb_owner </if>" +
            " <if test='common_gb_civilCode != null' > ,common_gb_civilCode </if>" +
            " <if test='common_gb_block != null' > ,common_gb_block </if>" +
            " <if test='common_gb_address != null' > ,common_gb_address </if>" +
            " <if test='common_gb_parental != null' > ,common_gb_parental </if>" +
            " <if test='common_gb_parent_id != null' > ,common_gb_parent_id </if>" +
            " <if test='common_gb_safety_way != null' > ,common_gb_safety_way </if>" +
            " <if test='common_gb_register_way != null' > ,common_gb_register_way </if>" +
            " <if test='common_gb_cert_num != null' > ,common_gb_cert_num </if>" +
            " <if test='common_gb_certifiable != null' > ,common_gb_certifiable </if>" +
            " <if test='common_gb_err_code != null' > ,common_gb_err_code </if>" +
            " <if test='common_gb_end_time != null' > ,common_gb_end_time </if>" +
            " <if test='common_gb_secrecy != null' > ,common_gb_secrecy </if>" +
            " <if test='common_gb_ip_address != null' > ,common_gb_ip_address </if>" +
            " <if test='common_gb_port != null' > ,common_gb_port </if>" +
            " <if test='common_gb_password != null' > ,common_gb_password </if>" +
            " <if test='common_gb_status != null' > ,common_gb_status </if>" +
            " <if test='common_gb_longitude != null' > ,common_gb_longitude </if>" +
            " <if test='common_gb_latitude != null' > ,common_gb_latitude </if>" +
            " <if test='common_gb_ptz_type != null' > ,common_gb_ptz_type </if>" +
            " <if test='common_gb_position_type != null' > ,common_gb_position_type </if>" +
            " <if test='common_gb_room_type != null' > ,common_gb_room_type </if>" +
            " <if test='common_gb_use_type != null' > ,common_gb_use_type </if>" +
            " <if test='common_gb_supply_light_type != null' > ,common_gb_supply_light_type </if>" +
            " <if test='common_gb_direction_type != null' > ,common_gb_direction_type </if>" +
            " <if test='common_gb_resolution != null' > ,common_gb_resolution </if>" +
            " <if test='common_gb_business_group_id != null' > ,common_gb_business_group_id </if>" +
            " <if test='common_gb_download_speed != null' > ,common_gb_download_speed </if>" +
            " <if test='common_gb_svc_time_support_mode != null' > ,common_gb_svc_time_support_mode </if>" +
            " <if test='type != null' > ,type </if>" +
            " <if test='updateTime != null' > ,updateTime </if>" +
            " <if test='createTime != null' > ,createTime </if>" +
            ") values (" +
            "#{commonGbDeviceID}" +
            " <if test='common_gb_name != null' > ,#{commonGbName}</if>" +
            " <if test='common_gb_manufacturer != null' > ,#{commonGbManufacturer}</if>" +
            " <if test='common_gb_model != null' > ,#{commonGbModel}</if>" +
            " <if test='common_gb_owner != null' > ,#{commonGbOwner}</if>" +
            " <if test='common_gb_civilCode != null' > ,#{commonGbCivilCode}</if>" +
            " <if test='common_gb_block != null' > ,#{commonGbBlock}</if>" +
            " <if test='common_gb_address != null' > ,#{commonGbAddress}</if>" +
            " <if test='common_gb_parental != null' > ,#{commonGbParental}</if>" +
            " <if test='common_gb_parent_id != null' > ,#{commonGbParentID}</if>" +
            " <if test='common_gb_safety_way != null' > ,#{commonGbSafetyWay}</if>" +
            " <if test='common_gb_register_way != null' > ,#{commonGbRegisterWay}</if>" +
            " <if test='common_gb_cert_num != null' > ,#{commonGbCertNum}</if>" +
            " <if test='common_gb_certifiable != null' > ,#{commonGbCertifiable}</if>" +
            " <if test='common_gb_err_code != null' > ,#{commonGbErrCode}</if>" +
            " <if test='common_gb_end_time != null' > ,#{commonGbEndTime}</if>" +
            " <if test='common_gb_secrecy != null' > ,#{commonGbSecrecy}</if>" +
            " <if test='common_gb_ip_address != null' > ,#{commonGbIPAddress}</if>" +
            " <if test='common_gb_port != null' > ,#{commonGbPort}</if>" +
            " <if test='common_gb_password != null' > ,#{commonGbPassword}</if>" +
            " <if test='common_gb_status != null' > ,#{commonGbStatus}</if>" +
            " <if test='common_gb_longitude != null' > ,#{commonGbLongitude}</if>" +
            " <if test='common_gb_latitude != null' > ,#{commonGbLatitude}</if>" +
            " <if test='common_gb_ptz_type != null' > ,#{commonGbPtzType}</if>" +
            " <if test='common_gb_position_type != null' > ,#{commonGbPositionType}</if>" +
            " <if test='common_gb_room_type != null' > ,#{commonGbRoomType}</if>" +
            " <if test='common_gb_use_type != null' > ,#{commonGbUseType}</if>" +
            " <if test='common_gb_supply_light_type != null' > ,#{commonGbSupplyLightType}</if>" +
            " <if test='common_gb_direction_type != null' > ,#{commonGbDirectionType}</if>" +
            " <if test='common_gb_resolution != null' > ,#{commonGbResolution}</if>" +
            " <if test='common_gb_business_group_id != null' > ,#{commonGbBusinessGroupID}</if>" +
            " <if test='common_gb_download_speed != null' > ,#{commonGbDownloadSpeed}</if>" +
            " <if test='common_gb_svc_time_support_mode != null' > ,#{commonGbSVCTimeSupportMode}</if>" +
            " <if test='type != null' > ,#{type}</if>" +
            " <if test='updateTime != null' > ,#{updateTime}</if>" +
            " <if test='createTime != null' > ,#{createTime}</if>" +
            ")" +
            "</script>")
    int add(CommonGbChannel channel);

    @Delete("delete from wvp_common_gb_channel where common_gb_device_id = #{channelId}")
    int deleteByDeviceID(String channelId);

    @Update(value = "<script>" +
            "UPDATE wvp_common_gb_channel SET " +
            "updateTime= #{ updateTime} " +
            " <if test='commonGbDeviceID != null' > ,common_gb_device_id= #{ commonGbDeviceID} </if>" +
            " <if test='commonGbName != null' > ,common_gb_name= #{ commonGbName} </if>" +
            " <if test='commonGbManufacturer != null' > ,common_gb_manufacturer= #{ commonGbManufacturer} </if>" +
            " <if test='commonGbModel != null' > ,common_gb_model= #{ commonGbModel} </if>" +
            " <if test='commonGbOwner != null' > ,common_gb_owner= #{ commonGbOwner} </if>" +
            " <if test='commonGbCivilCode != null' > ,common_gb_civilCode= #{ commonGbCivilCode} </if>" +
            " <if test='commonGbBlock != null' > ,common_gb_block= #{ commonGbBlock} </if>" +
            " <if test='commonGbAddress != null' > ,common_gb_address= #{ commonGbAddress} </if>" +
            " <if test='common_gb_parental != null' > ,common_gb_parental= #{ commonGbParental} </if>" +
            " <if test='commonGbParentID != null' > ,common_gb_parent_id= #{ commonGbParentID} </if>" +
            " <if test='commonGbSafetyWay != null' > ,common_gb_safety_way= #{ commonGbSafetyWay} </if>" +
            " <if test='commonGbRegisterWay != null' > ,common_gb_register_way= #{ commonGbRegisterWay} </if>" +
            " <if test='commonGbCertNum != null' > ,common_gb_cert_num= #{ commonGbCertNum} </if>" +
            " <if test='commonGbCertifiable != null' > ,common_gb_certifiable= #{ commonGbCertifiable} </if>" +
            " <if test='commonGbErrCode != null' > ,common_gb_err_code= #{ commonGbErrCode} </if>" +
            " <if test='commonGbEndTime != null' > ,common_gb_end_time= #{ commonGbEndTime} </if>" +
            " <if test='commonGbSecrecy != null' > ,common_gb_secrecy= #{ commonGbSecrecy} </if>" +
            " <if test='commonGbIPAddress != null' > ,common_gb_ip_address= #{ commonGbIPAddress} </if>" +
            " <if test='commonGbPort != null' > ,common_gb_port= #{ commonGbPort} </if>" +
            " <if test='commonGbPassword != null' > ,common_gb_password= #{ commonGbPassword} </if>" +
            " <if test='commonGbStatus != null' > ,common_gb_status= #{ commonGbStatus} </if>" +
            " <if test='commonGbLongitude != null' > ,common_gb_longitude= #{ commonGbLongitude} </if>" +
            " <if test='commonGbLatitude != null' > ,common_gb_latitude= #{ commonGbLatitude} </if>" +
            " <if test='commonGbPtzType != null' > ,common_gb_ptz_type= #{ commonGbPtzType} </if>" +
            " <if test='commonGbPositionType != null' > ,common_gb_position_type= #{ commonGbPositionType} </if>" +
            " <if test='commonGbRoomType != null' > ,common_gb_room_type= #{ commonGbRoomType} </if>" +
            " <if test='commonGbUseType != null' > ,common_gb_use_type= #{ commonGbUseType} </if>" +
            " <if test='commonGbEndTime != null' > ,common_gb_supply_light_type= #{ commonGbSupplyLightType} </if>" +
            " <if test='commonGbSupplyLightType != null' > ,common_gb_direction_type= #{ commonGbDirectionType} </if>" +
            " <if test='commonGbResolution != null' > ,common_gb_resolution= #{ commonGbResolution} </if>" +
            " <if test='commonGbBusinessGroupID != null' > ,common_gb_business_group_id= #{ commonGbBusinessGroupID} </if>" +
            " <if test='commonGbDownloadSpeed != null' > ,common_gb_download_speed= #{ commonGbDownloadSpeed} </if>" +
            " <if test='commonGbSVCTimeSupportMode != null' > ,common_gb_svc_time_support_mode= #{ commonGbSVCTimeSupportMode} </if>" +
            " <if test='type != null' > ,type= #{ type} </if>" +
            " WHERE common_gb_id=#{commonGbId}" +
            "</script>")
    int update(CommonGbChannel channel);

    @Select("select count(1)\n" +
            "from wvp_common_gb_channel gc " +
            "right join wvp_common_platform_channel pc " +
            "on gc.common_gb_device_id = pc.common_gb_channel_id" +
            "where gc.common_gb_device_id=#{channelId} and pc.platform_id=#{platformServerId}")
    int checkChannelInPlatform(String channelId, String platformServerId);

    @Insert(value = "<script>" +
            "insert into wvp_common_gb_channel ( " +
            "common_gb_device_id, " +
            "common_gb_name, " +
            "common_gb_manufacturer, " +
            "common_gb_model, " +
            "common_gb_owner, " +
            "common_gb_civilCode, " +
            "common_gb_block, " +
            "common_gb_address, " +
            "common_gb_parental, " +
            "common_gb_parent_id, " +
            "common_gb_safety_way, " +
            "common_gb_register_way, " +
            "common_gb_cert_num, " +
            "common_gb_certifiable, " +
            "common_gb_err_code, " +
            "common_gb_end_time, " +
            "common_gb_secrecy, " +
            "common_gb_ip_address, " +
            "common_gb_port, " +
            "common_gb_password, " +
            "common_gb_status, " +
            "common_gb_longitude, " +
            "common_gb_latitude, " +
            "common_gb_ptz_type, " +
            "common_gb_position_type, " +
            "common_gb_room_type, " +
            "common_gb_use_type, " +
            "common_gb_supply_light_type, " +
            "common_gb_direction_type, " +
            "common_gb_resolution, " +
            "common_gb_business_group_id, " +
            "common_gb_download_speed, " +
            "common_gb_svc_time_support_mode, " +
            "type, " +
            "updateTime, " +
            "createTime " +
            ") values " +
            "<foreach collection='commonGbChannelList' index='index' item='item' separator=','> " +
            "( " +
            "#{item.commonGbDeviceID}, " +
            "#{item.commonGbName}, " +
            "#{item.commonGbManufacturer}, " +
            "#{item.commonGbModel}, " +
            "#{item.commonGbOwner}, " +
            "#{item.commonGbCivilCode}, " +
            "#{item.commonGbBlock}," +
            "#{item.commonGbAddress}," +
            "#{item.commonGbParental}," +
            "#{item.commonGbParentID}," +
            "#{item.commonGbSafetyWay}," +
            "#{item.commonGbRegisterWay}," +
            "#{item.commonGbCertNum}," +
            "#{item.commonGbCertifiable}," +
            "#{item.commonGbErrCode}," +
            "#{item.commonGbEndTime}," +
            "#{item.commonGbSecrecy}," +
            "#{item.commonGbIPAddress}," +
            "#{item.commonGbPort}," +
            "#{item.commonGbPassword}," +
            "#{item.commonGbStatus}," +
            "#{item.commonGbLongitude}," +
            "#{item.commonGbLatitude}," +
            "#{item.commonGbPtzType}," +
            "#{item.commonGbPositionType}," +
            "#{item.commonGbRoomType}," +
            "#{item.commonGbUseType}," +
            "#{item.commonGbSupplyLightType}," +
            "#{item.commonGbDirectionType}," +
            "#{item.commonGbResolution}," +
            "#{item.commonGbBusinessGroupID}," +
            "#{item.commonGbDownloadSpeed}," +
            "#{item.commonGbSVCTimeSupportMode}," +
            "#{item.type}," +
            "#{item.updateTime}," +
            "#{item.createTime}" +
            ")" +
            "</foreach>" +
            "</script>")
    int addAll(List<CommonGbChannel> commonGbChannelList);

    @Delete("<script> "+
            "DELETE from wvp_common_gb_channel WHERE common_gb_id in" +
            "<foreach collection='clearChannels'  item='item'  open='(' separator=',' close=')' > #{item.commonGbChannelId}</foreach>" +
            "</script>")
    int deleteByDeviceIDs(List<DeviceChannel> clearChannels);
}
