package com.genersoft.iot.vmp.gb28181.dao.provider;

import java.util.Map;

/**
 * 达梦(DM8) upsert 实现（wvp_device_channel 的 gb_ 字段），使用 MERGE 语法。
 */
public class DmCommonChannelUpsertProvider {

    public String batchUpsert(Map<String, Object> params) {
        return "<script>" +
                "MERGE INTO wvp_device_channel t " +
                "USING ( " +
                " <foreach collection='channels' item='item' separator=' UNION ALL '> " +
                " SELECT " +
                " #{item.gbDeviceId} AS gb_device_id, #{item.dataType} AS data_type, #{item.dataDeviceId} AS data_device_id, " +
                " #{item.createTime} AS create_time, #{item.updateTime} AS update_time, " +
                " #{item.gbName} AS gb_name, #{item.gbManufacturer} AS gb_manufacturer, #{item.gbModel} AS gb_model, " +
                " #{item.gbOwner} AS gb_owner, #{item.gbCivilCode} AS gb_civil_code, #{item.gbBlock} AS gb_block, " +
                " #{item.gbAddress} AS gb_address, #{item.gbParental} AS gb_parental, #{item.gbParentId} AS gb_parent_id, " +
                " #{item.gbSafetyWay} AS gb_safety_way, #{item.gbRegisterWay} AS gb_register_way, #{item.gbCertNum} AS gb_cert_num, " +
                " #{item.gbCertifiable} AS gb_certifiable, #{item.gbErrCode} AS gb_err_code, #{item.gbEndTime} AS gb_end_time, " +
                " #{item.gbSecrecy} AS gb_secrecy, #{item.gbIpAddress} AS gb_ip_address, #{item.gbPort} AS gb_port, " +
                " #{item.gbPassword} AS gb_password, #{item.gbStatus} AS gb_status, #{item.gbLongitude} AS gb_longitude, " +
                " #{item.gbLatitude} AS gb_latitude, #{item.gbPtzType} AS gb_ptz_type, #{item.gbPositionType} AS gb_position_type, " +
                " #{item.gbRoomType} AS gb_room_type, #{item.gbUseType} AS gb_use_type, #{item.gbSupplyLightType} AS gb_supply_light_type, " +
                " #{item.gbDirectionType} AS gb_direction_type, #{item.gbResolution} AS gb_resolution, " +
                " #{item.gbBusinessGroupId} AS gb_business_group_id, #{item.gbDownloadSpeed} AS gb_download_speed, " +
                " #{item.gbSvcSpaceSupportMod} AS gb_svc_space_support_mod, #{item.gbSvcTimeSupportMode} AS gb_svc_time_support_mode, " +
                " #{item.enableBroadcast} AS enable_broadcast " +
                " FROM DUAL " +
                " </foreach> " +
                " ) s ON (t.gb_device_id = s.gb_device_id) " +
                " WHEN MATCHED THEN UPDATE SET " +
                " t.gb_name = s.gb_name, t.gb_manufacturer = s.gb_manufacturer, t.gb_model = s.gb_model, " +
                " t.gb_owner = s.gb_owner, t.gb_civil_code = s.gb_civil_code, t.gb_block = s.gb_block, " +
                " t.gb_address = s.gb_address, t.gb_parental = s.gb_parental, t.gb_parent_id = s.gb_parent_id, " +
                " t.gb_safety_way = s.gb_safety_way, t.gb_register_way = s.gb_register_way, " +
                " t.gb_cert_num = s.gb_cert_num, t.gb_certifiable = s.gb_certifiable, " +
                " t.gb_err_code = s.gb_err_code, t.gb_end_time = s.gb_end_time, t.gb_secrecy = s.gb_secrecy, " +
                " t.gb_ip_address = s.gb_ip_address, t.gb_port = s.gb_port, t.gb_password = s.gb_password, " +
                " t.gb_status = s.gb_status, t.gb_longitude = s.gb_longitude, t.gb_latitude = s.gb_latitude, " +
                " t.gb_ptz_type = s.gb_ptz_type, t.gb_position_type = s.gb_position_type, " +
                " t.gb_room_type = s.gb_room_type, t.gb_use_type = s.gb_use_type, " +
                " t.gb_supply_light_type = s.gb_supply_light_type, t.gb_direction_type = s.gb_direction_type, " +
                " t.gb_resolution = s.gb_resolution, t.gb_business_group_id = s.gb_business_group_id, " +
                " t.gb_download_speed = s.gb_download_speed, " +
                " t.gb_svc_space_support_mod = s.gb_svc_space_support_mod, " +
                " t.gb_svc_time_support_mode = s.gb_svc_time_support_mode, " +
                " t.enable_broadcast = s.enable_broadcast, t.update_time = s.update_time " +
                " WHEN NOT MATCHED THEN INSERT " +
                " (gb_device_id, data_type, data_device_id, create_time, update_time, " +
                " gb_name, gb_manufacturer, gb_model, gb_owner, gb_civil_code, gb_block, gb_address, " +
                " gb_parental, gb_parent_id, gb_safety_way, gb_register_way, gb_cert_num, gb_certifiable, " +
                " gb_err_code, gb_end_time, gb_secrecy, gb_ip_address, gb_port, gb_password, gb_status, " +
                " gb_longitude, gb_latitude, gb_ptz_type, gb_position_type, gb_room_type, gb_use_type, " +
                " gb_supply_light_type, gb_direction_type, gb_resolution, gb_business_group_id, " +
                " gb_download_speed, gb_svc_space_support_mod, gb_svc_time_support_mode, enable_broadcast) " +
                " VALUES " +
                " (s.gb_device_id, s.data_type, s.data_device_id, s.create_time, s.update_time, " +
                " s.gb_name, s.gb_manufacturer, s.gb_model, s.gb_owner, s.gb_civil_code, s.gb_block, s.gb_address, " +
                " s.gb_parental, s.gb_parent_id, s.gb_safety_way, s.gb_register_way, s.gb_cert_num, s.gb_certifiable, " +
                " s.gb_err_code, s.gb_end_time, s.gb_secrecy, s.gb_ip_address, s.gb_port, s.gb_password, s.gb_status, " +
                " s.gb_longitude, s.gb_latitude, s.gb_ptz_type, s.gb_position_type, s.gb_room_type, s.gb_use_type, " +
                " s.gb_supply_light_type, s.gb_direction_type, s.gb_resolution, s.gb_business_group_id, " +
                " s.gb_download_speed, s.gb_svc_space_support_mod, s.gb_svc_time_support_mode, s.enable_broadcast) " +
                "</script>";
    }
}
