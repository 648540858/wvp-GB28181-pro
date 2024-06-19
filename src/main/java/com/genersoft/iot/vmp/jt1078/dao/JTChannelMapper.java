package com.genersoft.iot.vmp.jt1078.dao;

import com.genersoft.iot.vmp.jt1078.bean.JTChannel;
import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface JTChannelMapper {

    @Select(value = {" <script>" +
            "SELECT * " +
            "from " +
            "wvp_jt_channel jc " +
            "WHERE " +
            "terminal_db_id = #{terminalDbId}" +
            " <if test='query != null'> AND " +
            "jc.name LIKE concat('%',#{query},'%') " +
            "</if> " +
            "ORDER BY jc.channel_id " +
            " </script>"})
    List<JTChannel> getAll(@Param("terminalDbId") int terminalDbId, @Param("query") String query);

    @Update(value = {" <script>" +
            "UPDATE wvp_jt_channel " +
            "SET update_time=#{updateTime}" +
            "<if test=\"terminalDbId != null\">, terminal_db_id=#{terminalDbId}</if>" +
            "<if test=\"hasAudio != null\">, has_audio=#{hasAudio}</if>" +
            "<if test=\"name != null\">, name=#{name}</if>" +
            "<if test=\"channelId != null\">, channel_id=#{channelId}</if>" +
            "<if test='gbManufacturer != null'>, gb_manufacturer=#{gbManufacturer}</if>" +
            "<if test='gbModel != null'>, gb_model=#{gbModel}</if>" +
            "<if test='gbCivilCode != null'>, gb_civil_code=#{gbCivilCode}</if>" +
            "<if test='gbBlock != null'>, gb_block=#{gbBlock}</if>" +
            "<if test='gbAddress != null'>, gb_address=#{gbAddress}</if>" +
            "<if test='gbParental != null'>, gb_parental=#{gbParental}</if>" +
            "<if test='gbParentId != null'>, gb_parent_id=#{gbParentId}</if>" +
            "<if test='gbRegisterWay != null'>, gb_register_way=#{gbRegisterWay}</if>" +
            "<if test='gbSecurityLevelCode != null'>, gb_security_level_code=#{gbSecurityLevelCode}</if>" +
            "<if test='gbSecrecy != null'>, gb_secrecy=#{gbSecrecy}</if>" +
            "<if test='gbIpAddress != null'>, gb_ip_address=#{gbIpAddress}</if>" +
            "<if test='gbPort != null'>, gb_port=#{gbPort}</if>" +
            "<if test='gbPassword != null'>, gb_password=#{gbPassword}</if>" +
            "<if test='gbStatus != null'>, gb_status=#{gbStatus}</if>" +
            "<if test='gbLongitude != null'>, gb_longitude=#{gbLongitude}</if>" +
            "<if test='gbLatitude != null'>, gb_latitude=#{gbLatitude}</if>" +
            "<if test='gbBusinessGroupId != null'>, gb_business_group_id=#{gbBusinessGroupId}</if>" +
            "<if test='gbPtzType != null'>, gb_ptz_type=#{gbPtzType}</if>" +
            "<if test='gbPhotoelectricImagingTyp != null'>, gb_photoelectric_imaging_typ=#{gbPhotoelectricImagingTyp}</if>" +
            "<if test='gbCapturePositionType != null'>, gb_capture_position_type=#{gbCapturePositionType}</if>" +
            "<if test='gbRoomType != null'>, gb_room_type=#{gbRoomType}</if>" +
            "<if test='gbSupplyLightType != null'>, gb_supply_light_type=#{gbSupplyLightType}</if>" +
            "<if test='gbDirectionType != null'>, gb_direction_type=#{gbDirectionType}</if>" +
            "<if test='gbResolution != null'>, gb_resolution=#{gbResolution}</if>" +
            "<if test='gbStreamNumberList != null'>, gb_stream_number_list=#{gbStreamNumberList}</if>" +
            "<if test='gbDownloadSpeed != null'>, gb_download_speed=#{gbDownloadSpeed}</if>" +
            "<if test='gbSvcSpaceSupportMod != null'>, gb_svc_space_support_mod=#{gbSvcSpaceSupportMod}</if>" +
            "<if test='gbSvcTimeSupportMode != null'>, gb_svc_time_support_mode=#{gbSvcTimeSupportMode}</if>" +
            "<if test='gbSsvcRatioSupportList != null'>, gb_ssvc_ratio_support_list=#{gbSsvcRatioSupportList}</if>" +
            "<if test='gbMobileDeviceType != null'>, gb_mobile_device_type=#{gbMobileDeviceType}</if>" +
            "<if test='gbHorizontalFieldAngle != null'>, gb_horizontal_field_angle=#{gbHorizontalFieldAngle}</if>" +
            "<if test='gbVerticalFieldAngle != null'>, gb_vertical_field_angle=#{gbVerticalFieldAngle}</if>" +
            "<if test='gbMaxViewDistance != null'>, gb_max_view_distance=#{gbMaxViewDistance}</if>" +
            "<if test='gbGrassrootsCode != null'>, gb_grassroots_code=#{gbGrassrootsCode}</if>" +
            "<if test='gbPoType != null'>, gb_po_type=#{gbPoType}</if>" +
            "<if test='gbPoCommonName != null'>, gb_po_common_name=#{gbPoCommonName}</if>" +
            "<if test='gbMac != null'>, gb_mac=#{gbMac}</if>" +
            "<if test='gbFunctionType != null'>, gb_function_type=#{gbFunctionType}</if>" +
            "<if test='gbEncodeType != null'>, gb_encode_type=#{gbEncodeType}</if>" +
            "<if test='gbInstallTime != null'>, gb_install_time=#{gbInstallTime}</if>" +
            "<if test='gbManagementUnit != null'>, gb_management_unit=#{gbManagementUnit}</if>" +
            "<if test='gbContactInfo != null'>, gb_contact_info=#{gbContactInfo}</if>" +
            "<if test='gbRecordSaveDays != null'>, gb_record_save_days=#{gbRecordSaveDays}</if>" +
            "<if test='gbIndustrialClassification != null'>, gb_industrial_classification=#{gbIndustrialClassification}</if>" +
            "WHERE id=#{id}"+
            " </script>"})
    void update(JTChannel channel);

    @Insert(value = {" <script>" +
            "INSERT INTO wvp_jt_channel (" +
            "terminal_db_id,"+
            "channel_id,"+
            "name,"+
            "has_audio,"+
            "create_time,"+
            "update_time"+
            "<if test='gbDeviceId != null'>, gb_device_id</if>" +
            "<if test='gbName != null'>, gb_name</if>" +
            "<if test='gbManufacturer != null'>, gb_manufacturer</if>" +
            "<if test='gbModel != null'>, gb_model</if>" +
            "<if test='gbCivilCode != null'>, gb_civil_code</if>" +
            "<if test='gbBlock != null'>, gb_block</if>" +
            "<if test='gbAddress != null'>, gb_address</if>" +
            "<if test='gbParental != null'>, gb_parental</if>" +
            "<if test='gbParentId != null'>, gb_parent_id</if>" +
            "<if test='gbRegisterWay != null'>, gb_register_way</if>" +
            "<if test='gbSecurityLevelCode != null'>, gb_security_level_code</if>" +
            "<if test='gbSecrecy != null'>, gb_secrecy</if>" +
            "<if test='gbIpAddress != null'>, gb_ip_address</if>" +
            "<if test='gbPort != null'>, gb_port</if>" +
            "<if test='gbPassword != null'>, gb_password</if>" +
            "<if test='gbStatus != null'>, gb_status</if>" +
            "<if test='gbLongitude != null'>, gb_longitude</if>" +
            "<if test='gbLatitude != null'>, gb_latitude</if>" +
            "<if test='gbBusinessGroupId != null'>, gb_business_group_id</if>" +
            "<if test='gbPtzType != null'>, gb_ptz_type</if>" +
            "<if test='gbPhotoelectricImagingTyp != null'>, gb_photoelectric_imaging_typ</if>" +
            "<if test='gbCapturePositionType != null'>, gb_capture_position_type</if>" +
            "<if test='gbRoomType != null'>, gb_room_type</if>" +
            "<if test='gbSupplyLightType != null'>, gb_supply_light_type</if>" +
            "<if test='gbDirectionType != null'>, gb_direction_type</if>" +
            "<if test='gbResolution != null'>, gb_resolution</if>" +
            "<if test='gbStreamNumberList != null'>, gb_stream_number_list</if>" +
            "<if test='gbDownloadSpeed != null'>, gb_download_speed</if>" +
            "<if test='gbSvcSpaceSupportMod != null'>, gb_svc_space_support_mod</if>" +
            "<if test='gbSvcTimeSupportMode != null'>, gb_svc_time_support_mode</if>" +
            "<if test='gbSsvcRatioSupportList != null'>, gb_ssvc_ratio_support_list</if>" +
            "<if test='gbMobileDeviceType != null'>, gb_mobile_device_type</if>" +
            "<if test='gbHorizontalFieldAngle != null'>, gb_horizontal_field_angle</if>" +
            "<if test='gbVerticalFieldAngle != null'>, gb_vertical_field_angle</if>" +
            "<if test='gbMaxViewDistance != null'>, gb_max_view_distance</if>" +
            "<if test='gbGrassrootsCode != null'>, gb_grassroots_code</if>" +
            "<if test='gbPoType != null'>, gb_po_type</if>" +
            "<if test='gbPoCommonName != null'>, gb_po_common_name</if>" +
            "<if test='gbMac != null'>, gb_mac</if>" +
            "<if test='gbFunctionType != null'>, gb_function_type</if>" +
            "<if test='gbEncodeType != null'>, gb_encode_type</if>" +
            "<if test='gbInstallTime != null'>, gb_install_time</if>" +
            "<if test='gbManagementUnit != null'>, gb_management_unit</if>" +
            "<if test='gbContactInfo != null'>, gb_contact_info</if>" +
            "<if test='gbRecordSaveDays != null'>, gb_record_save_days</if>" +
            "<if test='gbIndustrialClassification != null'>, gb_industrial_classification</if>" +
            ") VALUES (" +
            "#{terminalDbId}," +
            "#{channelId}," +
            "#{name}," +
            "#{hasAudio}," +
            "#{createTime}," +
            "#{updateTime}" +
            "<if test='gbDeviceId != null'>, #{gbDeviceId}</if>" +
            "<if test='gbName != null'>, #{gbName}</if>" +
            "<if test='gbManufacturer != null'>, #{gbManufacturer}</if>" +
            "<if test='gbModel != null'>, #{gbModel}</if>" +
            "<if test='gbCivilCode != null'>, #{gbCivilCode}</if>" +
            "<if test='gbBlock != null'>, #{gbBlock}</if>" +
            "<if test='gbAddress != null'>, #{gbAddress}</if>" +
            "<if test='gbParental != null'>, #{gbParental}</if>" +
            "<if test='gbParentId != null'>, #{gbParentId}</if>" +
            "<if test='gbRegisterWay != null'>, #{gbRegisterWay}</if>" +
            "<if test='gbSecurityLevelCode != null'>, #{gbSecurityLevelCode}</if>" +
            "<if test='gbSecrecy != null'>, #{gbSecrecy}</if>" +
            "<if test='gbIpAddress != null'>, #{gbIpAddress}</if>" +
            "<if test='gbPort != null'>, #{gbPort}</if>" +
            "<if test='gbPassword != null'>, #{gbPassword}</if>" +
            "<if test='gbStatus != null'>, #{gbStatus}</if>" +
            "<if test='gbLongitude != null'>, #{gbLongitude}</if>" +
            "<if test='gbLatitude != null'>, #{gbLatitude}</if>" +
            "<if test='gbBusinessGroupId != null'>, #{gbBusinessGroupId}</if>" +
            "<if test='gbPtzType != null'>, #{gbPtzType}</if>" +
            "<if test='gbPhotoelectricImagingTyp != null'>, #{gbPhotoelectricImagingTyp}</if>" +
            "<if test='gbCapturePositionType != null'>, #{gbCapturePositionType}</if>" +
            "<if test='gbRoomType != null'>, #{gbRoomType}</if>" +
            "<if test='gbSupplyLightType != null'>, #{gbSupplyLightType}</if>" +
            "<if test='gbDirectionType != null'>, #{gbDirectionType}</if>" +
            "<if test='gbResolution != null'>, #{gbResolution}</if>" +
            "<if test='gbStreamNumberList != null'>, #{gbStreamNumberList}</if>" +
            "<if test='gbDownloadSpeed != null'>, #{gbDownloadSpeed}</if>" +
            "<if test='gbSvcSpaceSupportMod != null'>, #{gbSvcSpaceSupportMod}</if>" +
            "<if test='gbSvcTimeSupportMode != null'>, #{gbSvcTimeSupportMode}</if>" +
            "<if test='gbSsvcRatioSupportList != null'>, #{gbSsvcRatioSupportList}</if>" +
            "<if test='gbMobileDeviceType != null'>, #{gbMobileDeviceType}</if>" +
            "<if test='gbHorizontalFieldAngle != null'>, #{gbHorizontalFieldAngle}</if>" +
            "<if test='gbVerticalFieldAngle != null'>, #{gbVerticalFieldAngle}</if>" +
            "<if test='gbMaxViewDistance != null'>, #{gbMaxViewDistance}</if>" +
            "<if test='gbGrassrootsCode != null'>, #{gbGrassrootsCode}</if>" +
            "<if test='gbPoType != null'>, #{gbPoType}</if>" +
            "<if test='gbPoCommonName != null'>, #{gbPoCommonName}</if>" +
            "<if test='gbMac != null'>, #{gbMac}</if>" +
            "<if test='gbFunctionType != null'>, #{gbFunctionType}</if>" +
            "<if test='gbEncodeType != null'>, #{gbEncodeType}</if>" +
            "<if test='gbInstallTime != null'>, #{gbInstallTime}</if>" +
            "<if test='gbManagementUnit != null'>, #{gbManagementUnit}</if>" +
            "<if test='gbContactInfo != null'>, #{gbContactInfo}</if>" +
            "<if test='gbRecordSaveDays != null'>, #{gbRecordSaveDays}</if>" +
            "<if test='gbIndustrialClassification != null'>, #{gbIndustrialClassification}</if>" +
            " )</script>"})
    void add(JTChannel channel);

    @Delete("delete from wvp_jt_channel where id = #{id}")
    void delete(@Param("id") int id);

    @Select(value = {" <script>" +
            "SELECT * " +
            "from " +
            "wvp_jt_channel jc " +
            "WHERE " +
            "terminal_db_id = #{terminalDbId} and channel_id = #{channelId}" +
            " </script>"})
    JTChannel getChannel(@Param("terminalDbId") int terminalDbId, @Param("channelId") Integer channelId);

    @Select(value = {" <script>" +
            " SELECT * " +
            " from " +
            " wvp_jt_channel" +
            " WHERE " +
            " id = #{id}" +
            " </script>"})
    JTChannel getChannelByDbId(@Param("id") Integer id);
}
