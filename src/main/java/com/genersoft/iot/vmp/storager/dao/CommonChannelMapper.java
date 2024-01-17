package com.genersoft.iot.vmp.storager.dao;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.service.bean.Group;
import com.genersoft.iot.vmp.service.bean.Region;
import com.genersoft.iot.vmp.vmanager.bean.UpdateCommonChannelToGroup;
import com.genersoft.iot.vmp.vmanager.bean.UpdateCommonChannelToRegion;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Mapper
@Repository
public interface CommonChannelMapper {

    @Select(value = "select * from wvp_common_channel where common_gb_business_group_id = '#{commonGroupId}'")
    List<CommonGbChannel> getChannels(String commonGroupId);

    @Update(value = "<script>" +
            "<foreach collection='channels' item='item' separator=';'>" +
            "UPDATE wvp_common_channel SET " +
            "update_time= #{ item.updateTime} " +
            " <if test='item.commonGbDeviceID != null' > ,common_gb_device_id= #{ item.commonGbDeviceID} </if>" +
            " <if test='item.commonGbName != null' > ,common_gb_name= #{ item.commonGbName} </if>" +
            " <if test='item.commonGbManufacturer != null' > ,common_gb_manufacturer= #{ item.commonGbManufacturer} </if>" +
            " <if test='item.commonGbModel != null' > ,common_gb_model= #{ item.commonGbModel} </if>" +
            " <if test='item.commonGbOwner != null' > ,common_gb_owner= #{ item.commonGbOwner} </if>" +
            " <if test='item.commonGbCivilCode != null' > ,common_gb_civilCode= #{ item.commonGbCivilCode} </if>" +
            " <if test='item.commonGbBlock != null' > ,common_gb_block= #{ item.commonGbBlock} </if>" +
            " <if test='item.commonGbAddress != null' > ,common_gb_address= #{ item.commonGbAddress} </if>" +
            " <if test='item.commonGbParental != null' > ,common_gb_parental= #{ item.commonGbParental} </if>" +
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
    int updateChanelForGroup(List<CommonGbChannel> channels);


    @Delete(value = "<script>" +
            "<foreach collection='channels' item='item' separator=';'>" +
            "delete from wvp_common_channel WHERE common_gb_id=#{item.commonGbId}" +
            "</foreach>" +
            "</script>")
    int removeChannelsForGroup(List<CommonGbChannel> channels);

    @Select("select * from wvp_common_channel where common_gb_device_id=#{channelId}")
    CommonGbChannel queryByDeviceID(String channelId);

    @Insert(value = "<script>" +
            "insert into wvp_common_channel ( " +
            "common_gb_device_id" +
            " <if test='commonGbName != null' > ,common_gb_name </if>" +
            " <if test='commonGbManufacturer != null' > ,common_gb_manufacturer </if>" +
            " <if test='commonGbModel != null' > ,common_gb_model </if>" +
            " <if test='commonGbOwner != null' > ,common_gb_owner </if>" +
            " <if test='commonGbCivilCode != null' > ,common_gb_civilCode </if>" +
            " <if test='commonGbBlock != null' > ,common_gb_block </if>" +
            " <if test='commonGbAddress != null' > ,common_gb_address </if>" +
            " <if test='commonGbParental != null' > ,common_gb_parental </if>" +
            " <if test='commonGbParentID != null' > ,common_gb_parent_id </if>" +
            " <if test='commonGbSafetyWay != null' > ,common_gb_safety_way </if>" +
            " <if test='commonGbRegisterWay != null' > ,common_gb_register_way </if>" +
            " <if test='commonGbCertNum != null' > ,common_gb_cert_num </if>" +
            " <if test='commonGbCertifiable != null' > ,common_gb_certifiable </if>" +
            " <if test='commonGbErrCode != null' > ,common_gb_err_code </if>" +
            " <if test='commonGbEndTime != null' > ,common_gb_end_time </if>" +
            " <if test='commonGbSecrecy != null' > ,common_gb_secrecy </if>" +
            " <if test='commonGbIPAddress != null' > ,common_gb_ip_address </if>" +
            " <if test='commonGbPort != null' > ,common_gb_port </if>" +
            " <if test='commonGbPassword != null' > ,common_gb_password </if>" +
            " <if test='commonGbStatus != null' > ,common_gb_status </if>" +
            " <if test='commonGbLongitude != null' > ,common_gb_longitude </if>" +
            " <if test='commonGbLatitude != null' > ,common_gb_latitude </if>" +
            " <if test='commonGbPtzType != null' > ,common_gb_ptz_type </if>" +
            " <if test='commonGbPositionType != null' > ,common_gb_position_type </if>" +
            " <if test='commonGbRoomType != null' > ,common_gb_room_type </if>" +
            " <if test='commonGbUseType != null' > ,common_gb_use_type </if>" +
            " <if test='commonGbSupplyLightType != null' > ,common_gb_supply_light_type </if>" +
            " <if test='commonGbDirectionType != null' > ,common_gb_direction_type </if>" +
            " <if test='commonGbResolution != null' > ,common_gb_resolution </if>" +
            " <if test='commonGbBusinessGroupID != null' > ,common_gb_business_group_id </if>" +
            " <if test='commonGbDownloadSpeed != null' > ,common_gb_download_speed </if>" +
            " <if test='commonGbSVCTimeSupportMode != null' > ,common_gb_svc_time_support_mode </if>" +
            " <if test='commonGbSVCSpaceSupportMode != null' > ,common_gb_svc_space_support_mode </if>" +
            " <if test='type != null' > ,type </if>" +
            " <if test='updateTime != null' > ,update_time </if>" +
            " <if test='createTime != null' > ,create_time </if>" +
            ") values (" +
            "#{commonGbDeviceID}" +
            " <if test='commonGbName != null' > ,#{commonGbName}</if>" +
            " <if test='commonGbManufacturer != null' > ,#{commonGbManufacturer}</if>" +
            " <if test='commonGbModel != null' > ,#{commonGbModel}</if>" +
            " <if test='commonGbOwner != null' > ,#{commonGbOwner}</if>" +
            " <if test='commonGbCivilCode != null' > ,#{commonGbCivilCode}</if>" +
            " <if test='commonGbBlock != null' > ,#{commonGbBlock}</if>" +
            " <if test='commonGbAddress != null' > ,#{commonGbAddress}</if>" +
            " <if test='commonGbParental != null' > ,#{commonGbParental}</if>" +
            " <if test='commonGbParentID != null' > ,#{commonGbParentID}</if>" +
            " <if test='commonGbSafetyWay != null' > ,#{commonGbSafetyWay}</if>" +
            " <if test='commonGbRegisterWay != null' > ,#{commonGbRegisterWay}</if>" +
            " <if test='commonGbCertNum != null' > ,#{commonGbCertNum}</if>" +
            " <if test='commonGbCertifiable != null' > ,#{commonGbCertifiable}</if>" +
            " <if test='commonGbErrCode != null' > ,#{commonGbErrCode}</if>" +
            " <if test='commonGbEndTime != null' > ,#{commonGbEndTime}</if>" +
            " <if test='commonGbSecrecy != null' > ,#{commonGbSecrecy}</if>" +
            " <if test='commonGbIPAddress != null' > ,#{commonGbIPAddress}</if>" +
            " <if test='commonGbPort != null' > ,#{commonGbPort}</if>" +
            " <if test='commonGbPassword != null' > ,#{commonGbPassword}</if>" +
            " <if test='commonGbStatus != null' > ,#{commonGbStatus}</if>" +
            " <if test='commonGbLongitude != null' > ,#{commonGbLongitude}</if>" +
            " <if test='commonGbLatitude != null' > ,#{commonGbLatitude}</if>" +
            " <if test='commonGbPtzType != null' > ,#{commonGbPtzType}</if>" +
            " <if test='commonGbPositionType != null' > ,#{commonGbPositionType}</if>" +
            " <if test='commonGbRoomType != null' > ,#{commonGbRoomType}</if>" +
            " <if test='commonGbUseType != null' > ,#{commonGbUseType}</if>" +
            " <if test='commonGbSupplyLightType != null' > ,#{commonGbSupplyLightType}</if>" +
            " <if test='commonGbDirectionType != null' > ,#{commonGbDirectionType}</if>" +
            " <if test='commonGbResolution != null' > ,#{commonGbResolution}</if>" +
            " <if test='commonGbBusinessGroupID != null' > ,#{commonGbBusinessGroupID}</if>" +
            " <if test='commonGbDownloadSpeed != null' > ,#{commonGbDownloadSpeed}</if>" +
            " <if test='commonGbSVCTimeSupportMode != null' > ,#{commonGbSVCTimeSupportMode}</if>" +
            " <if test='commonGbSVCSpaceSupportMode != null' > ,#{commonGbSVCSpaceSupportMode}</if>" +
            " <if test='type != null' > ,#{type}</if>" +
            " <if test='updateTime != null' > ,#{updateTime}</if>" +
            " <if test='createTime != null' > ,#{createTime}</if>" +
            ")" +
            "</script>")
    @Options(useGeneratedKeys=true, keyProperty="commonGbId", keyColumn="common_gb_id")
    int add(CommonGbChannel commonGbChannel);

    @Delete("delete from wvp_common_channel where common_gb_device_id = #{channelId}")
    int deleteByDeviceID(String channelId);

    @Update(value = "<script>" +
            "UPDATE wvp_common_channel SET " +
            "update_time= #{ updateTime} " +
            " <if test='commonGbDeviceID != null' > ,common_gb_device_id= #{ commonGbDeviceID} </if>" +
            " <if test='commonGbName != null' > ,common_gb_name= #{ commonGbName} </if>" +
            " <if test='commonGbManufacturer != null' > ,common_gb_manufacturer= #{ commonGbManufacturer} </if>" +
            " <if test='commonGbModel != null' > ,common_gb_model= #{ commonGbModel} </if>" +
            " <if test='commonGbOwner != null' > ,common_gb_owner= #{ commonGbOwner} </if>" +
            " <if test='commonGbCivilCode != null' > ,common_gb_civilCode= #{ commonGbCivilCode} </if>" +
            " <if test='commonGbBlock != null' > ,common_gb_block= #{ commonGbBlock} </if>" +
            " <if test='commonGbAddress != null' > ,common_gb_address= #{ commonGbAddress} </if>" +
            " <if test='commonGbParental != null' > ,common_gb_parental= #{ commonGbParental} </if>" +
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
            " <if test='commonGbSVCSpaceSupportMode != null' > ,common_gb_svc_space_support_mode= #{commonGbSVCSpaceSupportMode} </if>" +
            " <if test='type != null' > ,type= #{ type} </if>" +
            " WHERE common_gb_id=#{commonGbId}" +
            "</script>")
    int update(CommonGbChannel channel);

    @Update(value = "<script>" +
            "UPDATE wvp_common_channel SET " +
            "update_time= #{ updateTime}" +
            ", common_gb_device_id= #{ commonGbDeviceID}" +
            ", common_gb_name= #{ commonGbName} " +
            ", common_gb_manufacturer= #{ commonGbManufacturer} " +
            ", common_gb_model= #{ commonGbModel} " +
            ", common_gb_owner= #{ commonGbOwner} " +
            ", common_gb_civilCode= #{ commonGbCivilCode} " +
            ", common_gb_block= #{ commonGbBlock} " +
            ", common_gb_address= #{ commonGbAddress} " +
            ", common_gb_parental= #{ commonGbParental} " +
            ", common_gb_parent_id= #{ commonGbParentID} " +
            ", common_gb_safety_way= #{ commonGbSafetyWay} " +
            ", common_gb_register_way= #{ commonGbRegisterWay} " +
            ", common_gb_cert_num= #{ commonGbCertNum} " +
            ", common_gb_certifiable= #{ commonGbCertifiable} " +
            ", common_gb_err_code= #{ commonGbErrCode} " +
            ", common_gb_end_time= #{ commonGbEndTime} " +
            ", common_gb_secrecy= #{ commonGbSecrecy} " +
            ", common_gb_ip_address= #{ commonGbIPAddress} " +
            ", common_gb_port= #{ commonGbPort} " +
            ", common_gb_password= #{ commonGbPassword} " +
            ", common_gb_status= #{ commonGbStatus} " +
            ", common_gb_longitude= #{ commonGbLongitude} " +
            ", common_gb_latitude= #{ commonGbLatitude} " +
            ", common_gb_ptz_type= #{ commonGbPtzType} " +
            ", common_gb_position_type= #{ commonGbPositionType} " +
            ", common_gb_room_type= #{ commonGbRoomType} " +
            ", common_gb_use_type= #{ commonGbUseType} " +
            ", common_gb_supply_light_type= #{ commonGbSupplyLightType} " +
            ", common_gb_direction_type= #{ commonGbDirectionType} " +
            ", common_gb_resolution= #{ commonGbResolution} " +
            ", common_gb_business_group_id= #{ commonGbBusinessGroupID} " +
            ", common_gb_download_speed= #{ commonGbDownloadSpeed} " +
            ", common_gb_svc_time_support_mode= #{ commonGbSVCTimeSupportMode} " +
            ", common_gb_svc_space_support_mode= #{commonGbSVCSpaceSupportMode} " +
            ", type= #{ type} " +
            " WHERE common_gb_id=#{commonGbId}" +
            " </script>")
    int updateForForm(CommonGbChannel channel);

    @Select("select count(1)\n" +
            "from wvp_common_channel gc " +
            "right join wvp_common_platform_channel pc " +
            "on gc.common_gb_device_id = pc.common_gb_channel_id" +
            "where gc.common_gb_device_id=#{channelId} and pc.platform_id=#{platformServerId}")
    int checkChannelInPlatform(String channelId, String platformServerId);

    @Insert(value = "<script>" +
            "insert into wvp_common_channel ( " +
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
            "update_time, " +
            "create_time " +
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
            "DELETE from wvp_common_channel WHERE common_gb_device_id in (" +
            "<foreach collection='clearChannels'  item='item'  separator=',' > #{item}</foreach>" +
            " )"+
            "</script>")
    int deleteByDeviceIDs(List<String> clearChannels);

    @Update("<script> "+
            "UPDATE wvp_common_channel SET common_gb_status = true  WHERE common_gb_id in" +
            "<foreach collection='channelList'  item='item'  open='(' separator=',' close=')' > #{item.commonGbId}</foreach>" +
            "</script>")
    void channelsOnlineFromList(List<CommonGbChannel> channelList);


    @Update("<script> "+
            "UPDATE wvp_common_channel SET common_gb_status = false  WHERE common_gb_id in" +
            "<foreach collection='channelList'  item='item'  open='(' separator=',' close=')' > #{item.commonGbId}</foreach>" +
            "</script>")
    void channelsOfflineFromList(List<CommonGbChannel> channelList);

    @Update("<script> "+
            "UPDATE wvp_common_channel SET common_gb_parent_id = null  WHERE common_gb_id in" +
            "<foreach collection='errorParentIdList'  item='item'  open='(' separator=',' close=')' > #{item}</foreach>" +
            "</script>")
    int clearParentIds(List<String> errorParentIdList);

    @Update("<script> "+
            "UPDATE wvp_common_channel SET common_gb_civilCode = null  WHERE common_gb_civilCode in" +
            "<foreach collection='errorCivilCodeList'  item='item'  open='(' separator=',' close=')' > #{item}</foreach>" +
            "</script>")
    void clearCivilCodes(List<String> errorCivilCodeList);

    @Select("<script> "+
            "SELECT * FROM wvp_common_channel WHERE common_gb_device_id in" +
            "<foreach collection='commonGbChannelList'  item='item'  open='(' separator=',' close=')' > #{item.commonGbDeviceID}</foreach>" +
            "</script>")
    List<CommonGbChannel> queryInList(List<CommonGbChannel> commonGbChannelList);

    @Update("<script> "+
            "UPDATE wvp_common_channel SET common_gb_civilCode = #{commonRegionDeviceIdForNew}  WHERE common_gb_civilCode = #{commonRegionDeviceIdForOld}" +
            "</script>")
    void updateChanelRegion(@Param("commonRegionDeviceIdForOld") String commonRegionDeviceIdForOld,
                            @Param("commonRegionDeviceIdForNew") String commonRegionDeviceIdForNew);

    @Update("<script> "+
            "UPDATE wvp_common_channel SET common_gb_business_group_id = #{groupDeviceIdForNew}  WHERE common_gb_business_group_id = #{groupDeviceIdForOld}" +
            "</script>")
    void updateChanelGroup(
            @Param("groupDeviceIdForOld") String groupDeviceIdForOld,
            @Param("groupDeviceIdForNew") String groupDeviceIdForNew);

    @Select("<script> "+
            "select * from wvp_common_channel where common_gb_civilCode = #{regionDeviceId}" +
            "<if test='query != null'> and ( common_gb_device_id LIKE concat('%',#{query},'%') or common_gb_name LIKE concat('%',#{query},'%') )  </if>" +
            "</script>")
    List<CommonGbChannel> getChannelsInRegion(@Param("regionDeviceId") String regionDeviceId,
                                              @Param("query") String query);

    @Select("<script> "+
            "select * from wvp_common_channel where 1=1 " +
            "<if test='groupDeviceId != null'> and common_gb_business_group_id = #{groupDeviceId} </if>" +
            "<if test='regionDeviceId != null'> and common_gb_civilCode = #{regionDeviceId} </if>" +
            "<if test='inGroup != null &amp; inGroup'> and common_gb_business_group_id is not null </if>" +
            "<if test='inGroup != null &amp; !inGroup'> and common_gb_business_group_id is null </if>" +
            "<if test='inRegion != null &amp; inRegion'> and common_gb_civilCode is not null </if>" +
            "<if test='inRegion != null &amp; !inRegion'> and common_gb_civilCode is null </if>" +
            "<if test='type != null'> and type = #{type} </if>" +
            "<if test='query != null'> and ( common_gb_device_id LIKE concat('%',#{query},'%') or common_gb_name LIKE concat('%',#{query},'%') )  </if>" +
            "</script>")
    List<CommonGbChannel> queryChannelListInGroup(@Param("query") String query,
                                                  @Param("groupDeviceId") String groupDeviceId,
                                                  @Param("regionDeviceId") String regionDeviceId,
                                                  @Param("inGroup") Boolean inGroup,
                                                  @Param("inRegion") Boolean inRegion,
                                                  @Param("type") String type
    );



    @Select("<script> "+
            "select * from wvp_common_channel where 1=1 " +
            "<if test='query != null'> and ( common_gb_device_id LIKE concat('%',#{query},'%') or common_gb_name LIKE concat('%',#{query},'%') )  </if>" +
            "</script>")
    List<CommonGbChannel> query(@Param("query") String query);

    @Select("<script> "+
            "UPDATE wvp_common_channel SET common_gb_business_group_id = null  WHERE common_gb_business_group_id  in" +
            "<foreach collection='groupList'  item='item'  open='(' separator=',' close=')' > #{item.commonGroupDeviceId}</foreach>" +
            "</script>")
    void removeGroupInfo(@Param("groupList") List<Group> groupList);

    @Update({"<script>" +
            "<foreach collection='param.commonGbIds' item='item' separator=';'>" +
            " UPDATE" +
            " wvp_common_channel" +
            " SET common_gb_business_group_id = #{param.commonGbBusinessGroupID}" +
            " WHERE common_gb_id = #{item}" +
            "</foreach>" +
            "</script>"})
    void updateChannelToGroup(@Param("param") UpdateCommonChannelToGroup param);

    @Update({"<script>" +
            "<foreach collection='commonGbIds' item='item' separator=';'>" +
            " UPDATE" +
            " wvp_common_channel" +
            " SET common_gb_business_group_id = null" +
            " WHERE common_gb_id = #{item}" +
            "</foreach>" +
            "</script>"})
    void removeFromGroupByIds(@Param("commonGbIds") List<Integer> commonGbIds);

    @Update({"<script>" +
            " UPDATE" +
            " wvp_common_channel" +
            " SET common_gb_business_group_id = null" +
            " WHERE common_gb_business_group_id = #{commonGbBusinessGroupID}" +
            "</script>"})
    void removeFromGroupByGroupId(@Param("commonGbBusinessGroupID") String commonGbBusinessGroupID);

    @Select("<script> "+
            "UPDATE wvp_common_channel SET common_gb_civilCode = null  WHERE common_gb_civilCode  in" +
            "<foreach collection='regionList'  item='item'  open='(' separator=',' close=')' > #{item.commonRegionDeviceId}</foreach>" +
            "</script>")
    void removeRegionInfo(@Param("regionList") List<Region> regionList);

    @Update({"<script>" +
            "<foreach collection='commonGbIds' item='item' separator=';'>" +
            " UPDATE" +
            " wvp_common_channel" +
            " SET common_gb_civilCode = null" +
            " WHERE common_gb_id = #{item}" +
            "</foreach>" +
            "</script>"})
    void removeRegionGroupByIds(@Param("commonGbIds") List<Integer> commonGbIds);

    @Update({"<script>" +
            " UPDATE" +
            " wvp_common_channel" +
            " SET common_gb_civilCode = null" +
            " WHERE common_gb_civilCode = #{commonGbCivilCode}" +
            "</script>"})
    void removeFromRegionByRegionId(@Param("commonGbCivilCode") String commonGbCivilCode);

    @Update({"<script>" +
            "<foreach collection='param.commonGbIds' item='item' separator=';'>" +
            " UPDATE" +
            " wvp_common_channel" +
            " SET common_gb_civilCode = #{param.commonGbCivilCode}" +
            " WHERE common_gb_id = #{item}" +
            "</foreach>" +
            "</script>"})
    void updateChannelToRegion(@Param("param") UpdateCommonChannelToRegion param);

    @Insert("<script> " +
            "insert into wvp_common_channel " +
            "( common_gb_device_id,  " +
            " common_gb_name,  " +
            " common_gb_manufacturer,  " +
            " common_gb_model,  " +
            " common_gb_owner,  " +
            " common_gb_civilCode,  " +
            " common_gb_block,  " +
            " common_gb_address,  " +
            " common_gb_parental,  " +
            " common_gb_parent_id,  " +
            " common_gb_safety_way,  " +
            " common_gb_register_way,  " +
            " common_gb_cert_num,  " +
            " common_gb_certifiable,  " +
            " common_gb_err_code,  " +
            " common_gb_end_time,  " +
            " common_gb_secrecy,  " +
            " common_gb_ip_address,  " +
            " common_gb_port,  " +
            " common_gb_password,  " +
            " common_gb_status,  " +
            " common_gb_longitude,  " +
            " common_gb_latitude,  " +
            " common_gb_ptz_type,  " +
            " common_gb_position_type,  " +
            " common_gb_room_type,  " +
            " common_gb_use_type,  " +
            " common_gb_supply_light_type,  " +
            " common_gb_direction_type,  " +
            " common_gb_resolution,  " +
            " common_gb_business_group_id,  " +
            " common_gb_download_speed,  " +
            " common_gb_svc_time_support_mode,  " +
            " type,  " +
            " update_time,  " +
            " create_time  )"+
            "values " +
            "<foreach collection='commonGbChannels' index='index' item='item' separator=','> " +
            "(" +
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
            ")</foreach> " +
    "</script>")
    @Options(useGeneratedKeys=true, keyProperty="commonGbId", keyColumn="common_gb_id")
    int batchAdd(@Param("commonGbChannels") List<CommonGbChannel> commonGbChannels);

    @Update({"<script>" +
            "<foreach collection='commonGbChannels' item='item' separator=';'>" +
            " UPDATE" +
            " wvp_common_channel" +
            " SET update_time=#{item.updateTime}" +
            " <if test='item.commonGbDeviceID != null' > ,common_gb_device_id = #{item.commonGbDeviceID} </if>" +
            " <if test='item.commonGbName != null' > ,common_gb_name = #{item.commonGbName} </if>" +
            " <if test='item.commonGbManufacturer != null' > ,common_gb_manufacturer = #{item.commonGbManufacturer} </if>" +
            " <if test='item.commonGbModel != null' > ,common_gb_model = #{item.commonGbModel} </if>" +
            " <if test='item.commonGbOwner != null' > ,common_gb_owner = #{item.commonGbOwner} </if>" +
            " <if test='item.commonGbCivilCode != null' > ,common_gb_civilCode = #{item.commonGbCivilCode} </if>" +
            " <if test='item.commonGbBlock != null' > ,common_gb_block = #{item.commonGbBlock} </if>" +
            " <if test='item.commonGbAddress != null' > ,common_gb_address = #{item.commonGbAddress} </if>" +
            " <if test='item.commonGbParental != null' > ,common_gb_parental = #{item.commonGbParental} </if>" +
            " <if test='item.commonGbParentID != null' > ,common_gb_parent_id = #{item.commonGbParentID} </if>" +
            " <if test='item.commonGbSafetyWay != null' > ,common_gb_safety_way = #{item.commonGbSafetyWay} </if>" +
            " <if test='item.commonGbRegisterWay != null' > ,common_gb_register_way = #{item.commonGbRegisterWay} </if>" +
            " <if test='item.commonGbCertNum != null' > ,common_gb_cert_num = #{item.commonGbCertNum} </if>" +
            " <if test='item.commonGbCertifiable != null' > ,common_gb_certifiable = #{item.commonGbCertifiable} </if>" +
            " <if test='item.commonGbErrCode != null' > ,common_gb_err_code = #{item.commonGbErrCode} </if>" +
            " <if test='item.commonGbEndTime != null' > ,common_gb_end_time = #{item.commonGbEndTime} </if>" +
            " <if test='item.commonGbSecrecy != null' > ,common_gb_secrecy = #{item.commonGbSecrecy} </if>" +
            " <if test='item.commonGbIPAddress != null' > ,common_gb_ip_address = #{item.commonGbIPAddress} </if>" +
            " <if test='item.commonGbPort != null' > ,common_gb_port = #{item.commonGbPort} </if>" +
            " <if test='item.commonGbPassword != null' > ,common_gb_password = #{item.commonGbPassword} </if>" +
            " <if test='item.commonGbStatus != null' > ,common_gb_status = #{item.commonGbStatus} </if>" +
            " <if test='item.commonGbLongitude != null' > ,common_gb_longitude = #{item.commonGbLongitude} </if>" +
            " <if test='item.commonGbLatitude != null' > ,common_gb_latitude = #{item.commonGbLatitude} </if>" +
            " <if test='item.commonGbPtzType != null' > ,common_gb_ptz_type = #{item.commonGbPtzType} </if>" +
            " <if test='item.commonGbPositionType != null' > ,common_gb_position_type = #{item.commonGbPositionType} </if>" +
            " <if test='item.commonGbRoomType != null' > ,common_gb_room_type = #{item.commonGbRoomType} </if>" +
            " <if test='item.commonGbUseType != null' > ,common_gb_use_type = #{item.commonGbUseType} </if>" +
            " <if test='item.commonGbEndTime != null' > ,common_gb_supply_light_type = #{item.commonGbSupplyLightType} </if>" +
            " <if test='item.commonGbSupplyLightType != null' > ,common_gb_direction_type = #{item.commonGbDirectionType} </if>" +
            " <if test='item.commonGbResolution != null' > ,common_gb_resolution = #{item.commonGbResolution} </if>" +
            " <if test='item.commonGbBusinessGroupID != null' > ,common_gb_business_group_id = #{item.commonGbBusinessGroupID} </if>" +
            " <if test='item.commonGbDownloadSpeed != null' > ,common_gb_download_speed = #{item.commonGbDownloadSpeed} </if>" +
            " <if test='item.commonGbSVCTimeSupportMode != null' > ,common_gb_svc_time_support_mode = #{item.commonGbSVCTimeSupportMode} </if>" +
            " <if test='item.type != null' > ,type = #{item.type} </if>" +
            " WHERE common_gb_id=#{item.commonGbId}" +
            "</foreach>" +
            "</script>"})
    @Options(useGeneratedKeys=true, keyProperty="commonGbId", keyColumn="common_gb_id")
    int batchUpdate(@Param("commonGbChannels") List<CommonGbChannel> commonGbChannels);


    @Delete(value = {" <script>" +
                "DELETE " +
                "from " +
                "wvp_common_channel " +
                "WHERE common_gb_id IN " +
                "<foreach collection='commonGbChannels'  item='item'  open='(' separator=',' close=')' >#{item.commonGbId}</foreach>" +
            " </script>"})
    int batchDelete(@Param("commonGbChannels") List<CommonGbChannel> commonGbChannels);

    @MapKey("commonGbDeviceID")
    @Select("select * from wvp_common_channel")
    Map<String, CommonGbChannel> queryAllChannelsForMap();

    @Select("<script> "+
            "SELECT * FROM wvp_common_channel WHERE common_gb_id in" +
            "<foreach collection='channelIds'  item='item'  open='(' separator=',' close=')' > #{item}</foreach>" +
            "</script>")
    List<CommonGbChannel> queryInIdList(@Param("channelIds") List<Integer> channelIds);

    @Select("<script> "+
            "SELECT common_gb_id FROM wvp_common_channel WHERE common_gb_id in" +
            "<foreach collection='channelIds'  item='item'  open='(' separator=',' close=')' > #{item}</foreach>" +
            "</script>")
    List<Integer> getChannelIdsByIds(@Param("channelIds") List<Integer> channelIds);

    @Delete("<script> "+
            "delete from wvp_common_channel WHERE common_gb_id = #{commonGbChannelId}" +
            "</script>")
    void delete(@Param("commonGbChannelId") int commonGbChannelId);

    @Delete("<script> "+
            "delete from wvp_common_channel WHERE common_gb_id in" +
            "<foreach collection='commonChannelIdList'  item='item'  open='(' separator=',' close=')' > #{item}</foreach>" +
            "</script>")
    void deleteByIdList(@Param("commonChannelIdList") List<Integer> commonChannelIdList);

    @Select("<script> "+
            "SELECT * FROM wvp_common_channel" +
            "</script>")
    List<CommonGbChannel> getAll();

    @Select("SELECT common_gb_id FROM wvp_common_channel WHERE common_gb_id = #{commonGbChannelId}")
    CommonGbChannel getOne(@Param("commonGbChannelId") int commonGbChannelId);
}
